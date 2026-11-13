package com.homingsms.app.modules.webhooks

import android.content.Context
import com.homingsms.app.domain.EntitySource
import com.homingsms.app.domain.ProcessingState
import com.homingsms.app.modules.events.EventBus
import com.homingsms.app.modules.events.EventsReceiver
import com.homingsms.app.modules.messages.events.MessageStateChangedEvent
import com.homingsms.core.data.repository.InboxRecords
import com.homingsms.core.data.repository.MessageRecords
import com.homingsms.core.data.settings.SettingsRepository
import com.homingsms.core.data.webhooks.WebhookRepository
import com.homingsms.core.model.InboxRecord
import com.homingsms.core.model.MessageType
import com.homingsms.core.model.WebhookEventType
import com.homingsms.core.model.api.ApiJson
import com.homingsms.core.model.api.InboxEventData
import com.homingsms.core.model.api.Iso8601
import com.homingsms.core.model.api.MessageEventData
import com.homingsms.core.model.api.OptOutEventData
import com.homingsms.core.model.api.toDto
import org.koin.core.component.get

class WebhooksService(
    private val context: Context,
    private val repository: WebhookRepository,
    private val settings: SettingsRepository,
    private val records: MessageRecords,
    private val inbox: InboxRecords
) {
    private val collector by lazy { Collector() }

    val localDeviceId: String
        get() = settings.current.localServer.deviceId.orEmpty()

    fun start() {
        collector.start()
        WebhookWorker.runNow(context, settings)
    }

    fun stop() {
        collector.stop()
    }

    fun wake() = WebhookWorker.runNow(context, settings)

    suspend fun emitMessage(messageId: String, state: ProcessingState) {
        if (!settings.current.localServer.enabled) return
        val type = when (state) {
            ProcessingState.Sent -> WebhookEventType.MessageSent
            ProcessingState.Delivered -> WebhookEventType.MessageDelivered
            ProcessingState.Failed -> WebhookEventType.MessageFailed
            else -> return
        }
        val record = records.get(messageId, EntitySource.Local) ?: return
        val data = ApiJson.default.encodeToJsonElement(MessageEventData.serializer(), MessageEventData(record.toDto(localDeviceId)))
        if (repository.emit(type, data) > 0) wake()
    }

    suspend fun emitInbox(localId: String) {
        if (!settings.current.localServer.enabled) return
        val record = inbox.getByLocalId(localId) ?: return
        val data = ApiJson.default.encodeToJsonElement(InboxEventData.serializer(), InboxEventData(record.toDto(localDeviceId)))
        var emitted = repository.emit(WebhookEventType.InboxReceived, data)
        optOutKeyword(record)?.let { keyword ->
            val optOut = OptOutEventData(record.from, keyword, record.id, localDeviceId, Iso8601.format(record.receivedAt))
            emitted +=
                repository.emit(WebhookEventType.OptOutReceived, ApiJson.default.encodeToJsonElement(OptOutEventData.serializer(), optOut))
        }
        if (emitted > 0) wake()
    }

    private fun optOutKeyword(record: InboxRecord): String? {
        if (record.type != MessageType.Sms) return null
        val text = record.text?.trim()?.uppercase() ?: return null
        return settings.current.optOutKeywords.firstOrNull { it.uppercase() == text }
    }

    private inner class Collector : EventsReceiver() {
        override suspend fun collect(eventBus: EventBus) {
            eventBus.collect<MessageStateChangedEvent> { event ->
                if (event.source != EntitySource.Local) return@collect
                runCatching { get<WebhooksService>().emitMessage(event.id, event.state) }
            }
        }
    }
}
