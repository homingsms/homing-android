package com.homingsms.core.model

import kotlinx.serialization.Serializable

@Serializable
enum class SimSelectionMode {
    OSDefault,
    RoundRobin,
    Random
}

@Serializable
enum class ProcessingOrder {
    LIFO,
    FIFO
}

@Serializable
enum class LimitPeriod(val durationMs: Long) {
    Disabled(0L),
    PerMinute(60_000L),
    Per30Minutes(1_800_000L),
    PerHour(3_600_000L),
    PerDay(86_400_000L)
}

@Serializable
data class WorkHours(val enabled: Boolean = false, val start: String = "09:00", val end: String = "19:00")

@Serializable
data class MessageSettings(
    val simSelection: SimSelectionMode = SimSelectionMode.OSDefault,
    val processingOrder: ProcessingOrder = ProcessingOrder.LIFO,
    val sendIntervalMinSeconds: Int = 0,
    val sendIntervalMaxSeconds: Int? = null,
    val limitPeriod: LimitPeriod = LimitPeriod.Disabled,
    val limitValue: Int = 0,
    val logLifetimeDays: Int? = null,
    val workHours: WorkHours = WorkHours(),
    val simDailyLimits: Map<Int, Int> = emptyMap()
)

@Serializable
data class LocalServerSettings(val enabled: Boolean = false, val port: Int = LocalServerInfo.DEFAULT_PORT, val deviceId: String? = null)

@Serializable
data class CloudSettings(
    val enabled: Boolean = false,
    val serverUrl: String? = null,
    val deviceName: String? = null,
    val deviceId: String? = null,
    val accountId: String? = null,
    val pairedAt: Long? = null,
    val fcmToken: String? = null,
    val reportedFcmToken: String? = null,
    val remote: RemoteDeviceSettings? = null
)

@Serializable
data class RemoteSimSettings(val slot: Int, val enabled: Boolean, val dailyLimit: Int? = null)

@Serializable
data class RemoteDeviceSettings(
    val version: Int = 0,
    val paused: Boolean = false,
    val heartbeatIntervalSeconds: Int = DEFAULT_HEARTBEAT_SECONDS,
    val pullIntervalSeconds: Int = DEFAULT_PULL_SECONDS,
    val leaseSeconds: Int = DEFAULT_LEASE_SECONDS,
    val dailyLimit: Int? = null,
    val sims: List<RemoteSimSettings> = emptyList(),
    val optOutKeywords: List<String> = DEFAULT_OPT_OUT_KEYWORDS
) {
    companion object {
        const val DEFAULT_HEARTBEAT_SECONDS = 60
        const val DEFAULT_PULL_SECONDS = 60
        const val DEFAULT_LEASE_SECONDS = 120
        val DEFAULT_OPT_OUT_KEYWORDS = listOf("STOP", "STOPALL", "UNSUBSCRIBE", "CANCEL", "END", "QUIT")
    }
}

@Serializable
data class WebhookSettings(val internetRequired: Boolean = true)

@Serializable
data class InboxSettings(val contentProviderEnabled: Boolean = true, val lifetimeDays: Int? = null)

@Serializable
data class LogSettings(val lifetimeDays: Int? = DEFAULT_LIFETIME_DAYS) {
    companion object {
        const val DEFAULT_LIFETIME_DAYS = 30
    }
}

@Serializable
data class GeneralSettings(val autostart: Boolean = false)

@Serializable
data class AppSettings(
    val schemaVersion: Int = CURRENT_SCHEMA,
    val general: GeneralSettings = GeneralSettings(),
    val messages: MessageSettings = MessageSettings(),
    val localServer: LocalServerSettings = LocalServerSettings(),
    val cloud: CloudSettings = CloudSettings(),
    val webhooks: WebhookSettings = WebhookSettings(),
    val inbox: InboxSettings = InboxSettings(),
    val logs: LogSettings = LogSettings()
) {
    val optOutKeywords: List<String>
        get() = cloud.remote?.optOutKeywords ?: RemoteDeviceSettings.DEFAULT_OPT_OUT_KEYWORDS

    fun dailyLimitFor(slot: Int): Int? {
        val remote = cloud.remote?.takeIf { cloud.enabled }
        val remoteSim = remote?.sims?.firstOrNull { it.slot == slot }
        return remoteSim?.dailyLimit ?: messages.simDailyLimits[slot] ?: remote?.dailyLimit
    }

    fun simEnabled(slot: Int): Boolean {
        val remote = cloud.remote?.takeIf { cloud.enabled } ?: return true
        return remote.sims.firstOrNull { it.slot == slot }?.enabled ?: true
    }

    companion object {
        const val CURRENT_SCHEMA = 1
    }
}
