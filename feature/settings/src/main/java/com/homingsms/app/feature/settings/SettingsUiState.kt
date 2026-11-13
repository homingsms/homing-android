package com.homingsms.app.feature.settings

import androidx.compose.runtime.Immutable

enum class SimRoutingMode { DefaultSim, RoundRobin, LeastUsed, CarrierMatch }

@Immutable
data class SimCapUi(val slot: Int, val carrier: String?, val dailyCap: Int?)

@Immutable
data class WorkingHours(
    val enabled: Boolean = false,
    val startMinutes: Int = DEFAULT_WORK_START_MINUTES,
    val endMinutes: Int = DEFAULT_WORK_END_MINUTES
)

const val DEFAULT_WORK_START_MINUTES = 540
const val DEFAULT_WORK_END_MINUTES = 1080

@Immutable
data class SendingSettings(
    val rateLimitPerMinute: Int? = null,
    val delayBetweenSeconds: Int? = null,
    val dailyCaps: List<SimCapUi> = emptyList(),
    val workingHours: WorkingHours = WorkingHours(),
    val routing: SimRoutingMode = SimRoutingMode.DefaultSim,
    val failoverToOtherSim: Boolean = false,
    val deliveryReports: Boolean = true
)

@Immutable
data class ReceivingSettings(
    val forwardSms: Boolean = true,
    val forwardDataSms: Boolean = false,
    val forwardMms: Boolean = true,
    val dataSmsPort: Int? = null,
    val optOutEnabled: Boolean = true,
    val optOutKeywords: List<String> = listOf("STOP", "STOPALL", "UNSUBSCRIBE", "CANCEL", "END", "QUIT")
)

@Immutable
data class ServerSettings(
    val localEnabled: Boolean = false,
    val localPort: Int = 8080,
    val cloudPaired: Boolean = false,
    val cloudServerUrl: String = "",
    val pullIntervalSeconds: Int = 60
)

@Immutable
data class SecuritySettings(
    val apiKeyCount: Int = 0,
    val webhookCount: Int = 0,
    val encryptionEnabled: Boolean = false,
    val encryptionPassphraseSet: Boolean = false
)

@Immutable
data class AdvancedSettings(
    val keepAliveEnabled: Boolean = true,
    val batteryOptimizationIgnored: Boolean = false,
    val verboseLogging: Boolean = false
)

@Immutable
data class SimRateUi(val slot: Int, val carrier: String?, val rateMicros: Long?)

@Immutable
data class SpendSettingsUi(val currencyCode: String = "USD", val rates: List<SimRateUi> = listOf(SimRateUi(0, null, null)))

@Immutable
data class SettingsUiState(
    val sending: SendingSettings = SendingSettings(),
    val spend: SpendSettingsUi = SpendSettingsUi(),
    val receiving: ReceivingSettings = ReceivingSettings(),
    val server: ServerSettings = ServerSettings(),
    val security: SecuritySettings = SecuritySettings(),
    val advanced: AdvancedSettings = AdvancedSettings(),
    val appVersion: String = ""
)

sealed interface SettingsChange {
    data class RateLimit(val perMinute: Int?) : SettingsChange
    data class SendDelay(val seconds: Int?) : SettingsChange
    data class DailyCap(val slot: Int, val cap: Int?) : SettingsChange
    data class WorkingHoursEnabled(val enabled: Boolean) : SettingsChange
    data class WorkingHoursRange(val startMinutes: Int, val endMinutes: Int) : SettingsChange
    data class Routing(val mode: SimRoutingMode) : SettingsChange
    data class Failover(val enabled: Boolean) : SettingsChange
    data class DeliveryReports(val enabled: Boolean) : SettingsChange
    data class ForwardSms(val enabled: Boolean) : SettingsChange
    data class ForwardDataSms(val enabled: Boolean) : SettingsChange
    data class ForwardMms(val enabled: Boolean) : SettingsChange
    data class DataSmsPort(val port: Int?) : SettingsChange
    data class OptOutEnabled(val enabled: Boolean) : SettingsChange
    data class OptOutKeywords(val keywords: List<String>) : SettingsChange
    data class LocalServer(val enabled: Boolean) : SettingsChange
    data class LocalPort(val port: Int) : SettingsChange
    data class CloudServerUrl(val url: String) : SettingsChange
    data class PullInterval(val seconds: Int) : SettingsChange
    data class Encryption(val enabled: Boolean) : SettingsChange
    data class EncryptionPassphrase(val passphrase: String) : SettingsChange
    data class KeepAlive(val enabled: Boolean) : SettingsChange
    data class VerboseLogging(val enabled: Boolean) : SettingsChange
    data class SpendCurrency(val code: String) : SettingsChange
    data class SmsRate(val slot: Int, val micros: Long?) : SettingsChange
}

enum class SettingsDestination {
    ApiKeys,
    Webhooks,
    CloudPairing,
    BatteryOptimization,
    OutgoingSmsLimit,
    ExportLogs,
    Onboarding,
    About
}

@Immutable
data class SettingsActions(val onChange: (SettingsChange) -> Unit = {}, val onNavigate: (SettingsDestination) -> Unit = {})
