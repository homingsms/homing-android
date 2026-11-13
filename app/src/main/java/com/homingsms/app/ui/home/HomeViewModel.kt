package com.homingsms.app.ui.home

import android.Manifest
import androidx.lifecycle.viewModelScope
import com.homingsms.app.core.designsystem.component.HourlyBar
import com.homingsms.app.feature.home.CloudStatus
import com.homingsms.app.feature.home.DeviceHealth
import com.homingsms.app.feature.home.HomeActions
import com.homingsms.app.feature.home.HomeIssue
import com.homingsms.app.feature.home.HomeUiState
import com.homingsms.app.feature.home.LocalServerStatus
import com.homingsms.app.feature.home.SimSpend
import com.homingsms.app.feature.home.SimStatus
import com.homingsms.app.feature.home.SpendUiState
import com.homingsms.app.feature.home.TodayCounts
import com.homingsms.app.ui.EffectViewModel
import com.homingsms.app.ui.UiMappers
import com.homingsms.app.ui.data.AppStatus
import com.homingsms.app.ui.data.GatewayControl
import com.homingsms.app.ui.navigation.ConnectRoute
import com.homingsms.app.ui.navigation.MessagesRoute
import com.homingsms.app.ui.navigation.SettingsRoute
import com.homingsms.app.ui.system.PermissionOutcome
import com.homingsms.app.ui.system.PlatformState
import com.homingsms.app.ui.system.SystemAction
import com.homingsms.core.model.AppSettings
import com.homingsms.core.model.CloudConnectionState
import com.homingsms.core.model.CloudPairingState
import com.homingsms.core.model.DeviceHealth as CoreHealth
import com.homingsms.core.model.HourlyBucket
import com.homingsms.core.model.LocalServerInfo
import com.homingsms.core.model.SimStats
import com.homingsms.core.model.SmsPartsUsage
import com.homingsms.core.model.SpendMath
import com.homingsms.core.model.SpendSettings
import com.homingsms.core.model.StatusReason
import com.homingsms.core.model.TodayCounts as CoreToday
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class HomeViewModel(private val status: AppStatus, private val control: GatewayControl, private val platform: PlatformState) :
    EffectViewModel() {
    private val batteryIgnored = MutableStateFlow(platform.isIgnoringBatteryOptimizations())

    private val connection = combine(
        status.settings,
        status.localServerInfo,
        status.cloudPairing,
        status.cloudConnection
    ) { settings, local, pairing, cloud -> Connection(settings, local, pairing, cloud) }

    private val activity = combine(status.health, status.today, status.last24Hours, status.sims) { health, today, hourly, sims ->
        val simStatus = sims.map { sim ->
            SimStatus(sim.slot, sim.carrier, sim.phoneNumber, sim.signalLevel, sim.enabled, sim.sentToday, sim.dailyLimit)
        }
        Activity(health, today, hourly, simStatus)
    }

    private val spend = combine(status.settings, status.smsParts, status.sims) { settings, usage, sims ->
        usage?.let { HomeSpend.map(it, settings.spend, sims, settings.spend.effectiveCurrency()) }
    }

    val state: StateFlow<HomeUiState> = combine(
        status.gateway,
        connection,
        activity,
        batteryIgnored,
        spend
    ) { gateway, link, device, ignored, spendState ->
        val issues = UiMappers.issues(gateway).toMutableList()
        if (device.sims.isNotEmpty() && device.sims.none { it.enabled }) issues += HomeIssue.AllSimsDisabled
        if (!ignored) issues += HomeIssue.BatteryOptimized
        HomeUiState(
            isLoading = device.health.collectedAt == 0L,
            health = UiMappers.health(gateway.status),
            issues = issues.distinct(),
            today = TodayCounts(
                device.today.sent.toLong(),
                device.today.delivered.toLong(),
                device.today.failed.toLong(),
                device.today.queued.toLong()
            ),
            hourly = device.hourly.map(::bar),
            sims = device.sims,
            device = deviceHealth(device.health, link.cloud),
            localServer = LocalServerStatus(
                enabled = link.settings.localServer.enabled,
                running = link.local.running,
                address = link.local.baseUrls.firstOrNull() ?: if (link.local.running) loopback(link.local.port) else null
            ),
            cloud = CloudStatus(
                enabled = link.settings.cloud.enabled,
                paired = link.pairing is CloudPairingState.Paired,
                connected = link.cloud.lastSuccessAt != null && link.cloud.reachable,
                lastContactMillis = link.cloud.lastSuccessAt
            ),
            spend = spendState
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), HomeUiState(isLoading = true))

    val actions = HomeActions(
        onFixIssue = ::fix,
        onLocalServerToggle = { enabled -> viewModelScope.launch { control.setLocalEnabled(enabled) } },
        onCloudToggle = { enabled -> viewModelScope.launch { control.setCloudEnabled(enabled) } },
        onSimClick = { navigate(SettingsRoute) },
        onOpenMessages = { navigate(MessagesRoute) },
        onSetSmsRate = { navigate(SettingsRoute) }
    )

    fun onResume() {
        batteryIgnored.value = platform.isIgnoringBatteryOptimizations()
        viewModelScope.launch { status.refresh() }
    }

    fun onPermissionsResult(outcomes: List<PermissionOutcome>) {
        if (outcomes.any { !it.granted && !it.canAskAgain }) system(SystemAction.OpenAppSettings)
        if (outcomes.any { it.granted } && !status.serviceRunning.value) control.start()
        onResume()
    }

    fun fix(issue: HomeIssue) {
        when (issue) {
            HomeIssue.NotConfigured, HomeIssue.CloudNotPaired -> navigate(ConnectRoute)
            HomeIssue.ServiceStopped, HomeIssue.LocalServerDown -> restart()
            HomeIssue.BatteryOptimized -> system(SystemAction.RequestBatteryExemption)
            is HomeIssue.PermissionMissing -> system(SystemAction.RequestPermissions(missingPermissions()))
            is HomeIssue.SimNoSignal, HomeIssue.NoNetwork, HomeIssue.AirplaneMode -> system(SystemAction.OpenNetworkSettings)
            is HomeIssue.SimCapReached, HomeIssue.DeviceCapReached -> navigate(SettingsRoute)
            HomeIssue.AllSimsDisabled -> system(SystemAction.OpenSimSettings)
            HomeIssue.BatterySaver -> system(SystemAction.OpenBatterySaverSettings)
            HomeIssue.CloudUnreachable -> control.retryCloud()
            is HomeIssue.OutgoingSmsLimit, HomeIssue.LowBattery, HomeIssue.Paused, HomeIssue.NoSim -> Unit
        }
    }

    private fun restart() {
        val missing = missingPermissions()
        if (missing.isNotEmpty()) {
            system(SystemAction.RequestPermissions(missing))
            return
        }
        if (status.serviceRunning.value) control.stop()
        control.start()
        onResume()
    }

    private fun missingPermissions(): List<String> {
        val gatewayMissing = status.gateway.value.reasons
            .filterIsInstance<StatusReason.PermissionsMissing>()
            .flatMap { it.permissions }
        return gatewayMissing.ifEmpty { REQUIRED.filterNot(platform::isGranted) }
    }

    private fun deviceHealth(health: CoreHealth, cloud: CloudConnectionState): DeviceHealth? {
        if (health.collectedAt == 0L) return null
        return DeviceHealth(
            batteryPercent = health.battery.level,
            charging = health.battery.charging,
            network = UiMappers.network(health.network),
            lastCloudContactMillis = cloud.lastSuccessAt
        )
    }

    private fun bar(bucket: HourlyBucket): HourlyBar = HourlyBar(
        delivered = bucket.delivered,
        sentWithoutReport = (bucket.sent - bucket.delivered).coerceAtLeast(0),
        failed = bucket.failed
    )

    private fun loopback(port: Int) = "http://127.0.0.1:$port/api/v1"

    private data class Connection(
        val settings: AppSettings,
        val local: LocalServerInfo,
        val pairing: CloudPairingState,
        val cloud: CloudConnectionState
    )

    private data class Activity(val health: CoreHealth, val today: CoreToday, val hourly: List<HourlyBucket>, val sims: List<SimStatus>)

    private companion object {
        const val STOP_TIMEOUT_MS = 5_000L
        val REQUIRED = listOf(
            Manifest.permission.SEND_SMS,
            Manifest.permission.RECEIVE_SMS,
            Manifest.permission.READ_SMS,
            Manifest.permission.READ_PHONE_STATE
        )
    }
}

object HomeSpend {
    fun map(usage: SmsPartsUsage, settings: SpendSettings, sims: List<SimStats>, currencyCode: String): SpendUiState {
        val estimate = SpendMath.estimate(usage, settings, sims.map { it.slot }, currencyCode)
        val carriers = sims.associate { it.slot to it.carrier }
        return SpendUiState(
            currencyCode = estimate.currencyCode,
            hasRate = estimate.hasRate,
            monthMicros = estimate.monthMicros,
            allTimeMicros = estimate.allTimeMicros,
            monthParts = estimate.monthParts,
            allTimeParts = estimate.allTimeParts,
            unpricedMonthParts = if (estimate.hasRate) estimate.unpricedMonthParts else 0L,
            sims = estimate.sims.map { SimSpend(it.slot, carriers[it.slot], it.monthParts, it.monthMicros) }
        )
    }
}
