package com.homingsms.app.feature.home

import androidx.compose.runtime.Immutable
import com.homingsms.app.core.designsystem.component.GatewayHealth
import com.homingsms.app.core.designsystem.component.HourlyBar

@Immutable
data class HomeUiState(
    val isLoading: Boolean = false,
    val health: GatewayHealth = GatewayHealth.Offline,
    val issues: List<HomeIssue> = emptyList(),
    val today: TodayCounts? = null,
    val hourly: List<HourlyBar> = emptyList(),
    val sims: List<SimStatus> = emptyList(),
    val device: DeviceHealth? = null,
    val localServer: LocalServerStatus = LocalServerStatus(),
    val cloud: CloudStatus = CloudStatus()
)

@Immutable
data class TodayCounts(val sent: Long, val delivered: Long, val failed: Long, val queued: Long)

@Immutable
data class SimStatus(
    val slot: Int,
    val carrier: String?,
    val phoneNumber: String?,
    val signalLevel: Int?,
    val enabled: Boolean,
    val sentToday: Int,
    val dailyCap: Int?
)

enum class NetworkKind { Wifi, Cellular, Ethernet, None, Unknown }

@Immutable
data class DeviceHealth(val batteryPercent: Int?, val charging: Boolean, val network: NetworkKind, val lastCloudContactMillis: Long?)

@Immutable
data class LocalServerStatus(val enabled: Boolean = false, val running: Boolean = false, val address: String? = null)

@Immutable
data class CloudStatus(
    val enabled: Boolean = false,
    val paired: Boolean = false,
    val connected: Boolean = false,
    val lastContactMillis: Long? = null
)

enum class MissingPermission { SendSms, ReceiveSms, PhoneState, Notifications }

const val DEFAULT_OUTGOING_SMS_LIMIT = 1000

sealed interface HomeIssue {
    data object NotConfigured : HomeIssue
    data object ServiceStopped : HomeIssue
    data object BatteryOptimized : HomeIssue
    data class OutgoingSmsLimit(val suggestedLimit: Int = DEFAULT_OUTGOING_SMS_LIMIT) : HomeIssue {
        val adbCommand: String get() = "adb shell settings put global sms_outgoing_check_max_count $suggestedLimit"
    }
    data class PermissionMissing(val permission: MissingPermission) : HomeIssue
    data class SimNoSignal(val slot: Int) : HomeIssue
    data class SimCapReached(val slot: Int) : HomeIssue
    data object DeviceCapReached : HomeIssue
    data object AllSimsDisabled : HomeIssue
    data object NoNetwork : HomeIssue
    data object LowBattery : HomeIssue
    data object Paused : HomeIssue
    data object CloudUnreachable : HomeIssue
    data object NoSim : HomeIssue
    data object AirplaneMode : HomeIssue
    data object BatterySaver : HomeIssue
    data object CloudNotPaired : HomeIssue
    data object LocalServerDown : HomeIssue
}

@Immutable
data class HomeActions(
    val onFixIssue: (HomeIssue) -> Unit = {},
    val onLocalServerToggle: (Boolean) -> Unit = {},
    val onCloudToggle: (Boolean) -> Unit = {},
    val onSimClick: (Int) -> Unit = {},
    val onOpenMessages: () -> Unit = {}
)
