package com.homingsms.app.modules.status

import android.Manifest
import android.os.Build
import com.homingsms.app.modules.cloud.CloudController
import com.homingsms.app.modules.localserver.LocalServerService
import com.homingsms.app.modules.messages.SmsLimitMonitor
import com.homingsms.core.data.repository.InboxRecords
import com.homingsms.core.data.repository.MessageRecords
import com.homingsms.core.data.repository.StatsRepository
import com.homingsms.core.data.settings.SettingsRepository
import com.homingsms.core.data.webhooks.WebhookRepository
import com.homingsms.core.model.AppSettings
import com.homingsms.core.model.CloudConnectionState
import com.homingsms.core.model.CloudPairingState
import com.homingsms.core.model.Cursor
import com.homingsms.core.model.DeviceHealth
import com.homingsms.core.model.GatewayInputs
import com.homingsms.core.model.GatewayState
import com.homingsms.core.model.GatewayStatusDeriver
import com.homingsms.core.model.HourlyBucket
import com.homingsms.core.model.InboxFilter
import com.homingsms.core.model.InboxRecord
import com.homingsms.core.model.LocalServerInfo
import com.homingsms.core.model.MessageFilter
import com.homingsms.core.model.MessageRecord
import com.homingsms.core.model.Page
import com.homingsms.core.model.SimStats
import com.homingsms.core.model.SmsLimitState
import com.homingsms.core.model.TodayCounts
import com.homingsms.core.model.WebhookDelivery
import com.homingsms.core.model.WebhookEndpoint
import com.homingsms.core.telephony.DeviceHealthReader
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class AppStateRepository(
    private val settingsRepository: SettingsRepository,
    private val stats: StatsRepository,
    private val healthReader: DeviceHealthReader,
    private val smsLimitMonitor: SmsLimitMonitor,
    private val localServer: LocalServerService,
    private val cloud: CloudController,
    private val webhooks: WebhookRepository,
    private val messageRecords: MessageRecords,
    private val inboxRecords: InboxRecords
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val running = MutableStateFlow(false)
    private val healthState = MutableStateFlow(DeviceHealth.Unknown)
    private val permissionsState = MutableStateFlow<List<String>>(emptyList())

    val settings: StateFlow<AppSettings> = settingsRepository.settings
    val serviceRunning: StateFlow<Boolean> = running.asStateFlow()
    val health: StateFlow<DeviceHealth> = healthState.asStateFlow()
    val missingPermissions: StateFlow<List<String>> = permissionsState.asStateFlow()
    val smsLimit: StateFlow<SmsLimitState> = smsLimitMonitor.limitState
    val localServerInfo: StateFlow<LocalServerInfo> = localServer.info
    val cloudPairing: StateFlow<CloudPairingState> = cloud.pairing
    val cloudConnection: StateFlow<CloudConnectionState> = cloud.connection

    val today: StateFlow<TodayCounts> = stats.today.stateIn(scope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), TodayCounts.Empty)

    val last24Hours: StateFlow<List<HourlyBucket>> =
        stats.last24Hours.stateIn(scope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), emptyList())

    val sims: StateFlow<List<SimStats>> = combine(healthState, settingsRepository.settings) { health, current ->
        health.sims.map {
            SimStats(
                slot = it.slot,
                carrier = it.carrier,
                phoneNumber = it.phoneNumber,
                signalLevel = it.signalLevel,
                sentToday = it.sentToday,
                dailyLimit = current.dailyLimitFor(it.slot),
                enabled = current.simEnabled(it.slot)
            )
        }
    }.stateIn(scope, SharingStarted.Eagerly, emptyList())

    val webhookEndpoints: StateFlow<List<WebhookEndpoint>> =
        webhooks.endpointsFlow.stateIn(scope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), emptyList())

    val gateway: StateFlow<GatewayState> = combine(
        combine(running, settingsRepository.settings, localServer.info) { isRunning, current, local -> Triple(isRunning, current, local) },
        combine(cloud.pairing, cloud.connection) { pairing, connection -> pairing to connection },
        combine(healthState, permissionsState, smsLimitMonitor.limitState, sims) { health, permissions, limit, simList ->
            DeviceInputs(health, permissions, limit, simList)
        }
    ) { base, cloudState, device ->
        GatewayStatusDeriver.derive(
            GatewayInputs(
                running = base.first,
                localEnabled = base.second.localServer.enabled,
                localRunning = base.third.running,
                localError = base.third.lastError,
                cloudEnabled = base.second.cloud.enabled,
                cloudPairing = cloudState.first,
                cloudConnection = cloudState.second,
                health = device.health,
                missingPermissions = device.permissions,
                smsLimit = device.limit,
                sims = device.sims,
                now = System.currentTimeMillis()
            )
        )
    }.stateIn(scope, SharingStarted.Eagerly, GatewayState.Stopped)

    init {
        scope.launch(Dispatchers.IO) {
            while (true) {
                refresh()
                delay(REFRESH_MS)
            }
        }
    }

    fun webhookDeliveries(endpointId: String): Flow<List<WebhookDelivery>> = webhooks.recentDeliveries(endpointId)

    suspend fun messages(filter: MessageFilter, cursor: Cursor?, limit: Int): Page<MessageRecord> =
        messageRecords.list(filter, cursor, limit, source = null)

    suspend fun message(id: String): MessageRecord? = messageRecords.get(id)

    suspend fun inbox(filter: InboxFilter, cursor: Cursor?, limit: Int): Page<InboxRecord> = inboxRecords.list(filter, cursor, limit)

    internal fun setRunning(value: Boolean) {
        running.value = value
        scope.launch(Dispatchers.IO) { refresh() }
    }

    suspend fun refresh() {
        healthState.value = healthReader.read(stats.sentToday(), stats.queued())
        permissionsState.value = healthReader.missingPermissions(requiredPermissions())
        smsLimitMonitor.refresh()
        localServer.refreshAddresses()
    }

    private data class DeviceInputs(
        val health: DeviceHealth,
        val permissions: List<String>,
        val limit: SmsLimitState,
        val sims: List<SimStats>
    )

    companion object {
        private const val REFRESH_MS = 30_000L
        private const val READ_PHONE_NUMBERS = "android.permission.READ_PHONE_NUMBERS"
        private const val POST_NOTIFICATIONS = "android.permission.POST_NOTIFICATIONS"
        private const val STOP_TIMEOUT_MS = 5_000L

        fun requiredPermissions(): List<String> = listOfNotNull(
            Manifest.permission.SEND_SMS,
            Manifest.permission.RECEIVE_SMS,
            Manifest.permission.READ_SMS,
            Manifest.permission.RECEIVE_MMS,
            Manifest.permission.READ_PHONE_STATE,
            READ_PHONE_NUMBERS.takeIf { Build.VERSION.SDK_INT >= Build.VERSION_CODES.O },
            POST_NOTIFICATIONS.takeIf { Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU }
        )
    }
}
