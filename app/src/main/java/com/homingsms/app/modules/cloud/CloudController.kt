package com.homingsms.app.modules.cloud

import android.content.Context
import com.homingsms.app.BuildConfig
import com.homingsms.app.domain.EntitySource
import com.homingsms.app.domain.ProcessingState
import com.homingsms.app.modules.events.EventBus
import com.homingsms.app.modules.events.EventsReceiver
import com.homingsms.app.modules.messages.events.MessageStateChangedEvent
import com.homingsms.core.data.cloud.CloudSyncEngine
import com.homingsms.core.data.cloud.DeviceReportState
import com.homingsms.core.data.cloud.PendingReport
import com.homingsms.core.data.db.CloudOutboxDao
import com.homingsms.core.data.settings.SettingsRepository
import com.homingsms.core.model.CloudConnectionState
import com.homingsms.core.model.CloudPairingState
import com.homingsms.core.model.ErrorCodes
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import org.koin.core.component.get

class CloudController(
    private val context: Context,
    private val engine: CloudSyncEngine,
    private val settings: SettingsRepository,
    private val outbox: CloudOutboxDao,
    private val scope: CoroutineScope
) {
    private val collector by lazy { Collector() }

    val pairing: StateFlow<CloudPairingState> = engine.pairing
    val connection: StateFlow<CloudConnectionState> = engine.connection

    val allowCleartext: Boolean
        get() = BuildConfig.DEBUG

    init {
        scope.launch {
            combine(outbox.observeStateCount(), outbox.observeInboxCount()) { states, inbox -> states to inbox }
                .collect { (states, inbox) -> engine.setPendingCounts(states, inbox) }
        }
    }

    fun start() {
        engine.setPushAvailable(PushTokens.available(context))
        collector.start()
        if (!settings.current.cloud.enabled || !engine.isPaired) return
        CloudService.start(context)
        CloudSyncWorker.schedulePeriodic(context)
        CloudSyncWorker.flush(context)
        PushTokens.refresh(context)
    }

    fun stop() {
        collector.stop()
        CloudService.stop(context)
        CloudSyncWorker.cancelAll(context)
    }

    suspend fun pair(code: String, serverUrl: String): CloudPairingState =
        engine.pair(code, serverUrl, allowCleartext).also { if (it is CloudPairingState.Paired) start() }

    suspend fun pairWithLink(link: String): CloudPairingState =
        engine.pairWithLink(link, allowCleartext).also { if (it is CloudPairingState.Paired) start() }

    suspend fun unpair() {
        engine.unpair()
        stop()
    }

    suspend fun setEnabled(enabled: Boolean) {
        settings.update { it.copy(cloud = it.cloud.copy(enabled = enabled)) }
        if (enabled) start() else stop()
    }

    suspend fun queueInbox(localId: String) {
        if (!engine.isPaired) return
        engine.queueInbox(localId)
        CloudSyncWorker.flush(context)
    }

    suspend fun report(event: MessageStateChangedEvent) {
        if (event.source != EntitySource.Cloud || !engine.isPaired) return
        val state = when (event.state) {
            ProcessingState.Processed -> DeviceReportState.SENDING
            ProcessingState.Sent -> DeviceReportState.SENT
            ProcessingState.Delivered -> DeviceReportState.DELIVERED
            ProcessingState.Failed -> DeviceReportState.FAILED
            else -> return
        }
        val error = ErrorCodes.decode(event.error).takeIf { state == DeviceReportState.FAILED }
        engine.report(
            PendingReport(
                messageId = event.id,
                state = state,
                at = System.currentTimeMillis(),
                simSlot = event.simNumber?.minus(1),
                partsCount = event.partsCount,
                errorCode = error?.code,
                errorMessage = error?.message
            )
        )
        CloudSyncWorker.flush(context)
    }

    private inner class Collector : EventsReceiver() {
        override suspend fun collect(eventBus: EventBus) {
            eventBus.collect<MessageStateChangedEvent> { event -> get<CloudController>().report(event) }
        }
    }
}
