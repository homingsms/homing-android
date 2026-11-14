package com.homingsms.app.ui.settings

import androidx.lifecycle.viewModelScope
import com.homingsms.app.BuildConfig
import com.homingsms.app.feature.settings.AdvancedSettings
import com.homingsms.app.feature.settings.LegalSettingsUi
import com.homingsms.app.feature.settings.ReceivingSettings
import com.homingsms.app.feature.settings.SecuritySettings
import com.homingsms.app.feature.settings.SendingSettings
import com.homingsms.app.feature.settings.ServerSettings
import com.homingsms.app.feature.settings.SettingsActions
import com.homingsms.app.feature.settings.SettingsChange
import com.homingsms.app.feature.settings.SettingsDestination
import com.homingsms.app.feature.settings.SettingsUiState
import com.homingsms.app.feature.settings.SimCapUi
import com.homingsms.app.feature.settings.SimRateUi
import com.homingsms.app.feature.settings.SimRoutingMode
import com.homingsms.app.feature.settings.SpendSettingsUi
import com.homingsms.app.feature.settings.WorkingHours
import com.homingsms.app.ui.EffectViewModel
import com.homingsms.app.ui.attempt
import com.homingsms.app.ui.data.AppStatus
import com.homingsms.app.ui.data.FileSharing
import com.homingsms.app.ui.data.GatewayControl
import com.homingsms.app.ui.navigation.AboutRoute
import com.homingsms.app.ui.navigation.ConnectRoute
import com.homingsms.app.ui.navigation.OnboardingRoute
import com.homingsms.app.ui.navigation.WebhooksRoute
import com.homingsms.app.ui.onboarding.LegalLinks
import com.homingsms.app.ui.system.PlatformState
import com.homingsms.app.ui.system.SystemAction
import com.homingsms.core.model.AppSettings
import com.homingsms.core.model.CloudPairingState
import com.homingsms.core.model.LegalDocument
import com.homingsms.core.model.LimitPeriod
import com.homingsms.core.model.PairingLink
import com.homingsms.core.model.RemoteDeviceSettings
import com.homingsms.core.model.SimSelectionMode
import com.homingsms.core.model.SimStats
import com.homingsms.core.model.SpendMath
import java.util.Locale
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val status: AppStatus,
    private val control: GatewayControl,
    private val platform: PlatformState,
    private val sharing: FileSharing,
    private val appVersion: String,
    private val legalBaseUrl: String = BuildConfig.LEGAL_BASE_URL
) : EffectViewModel() {
    private data class Local(val passphraseSet: Boolean, val encryptionPending: Boolean = false, val batteryIgnored: Boolean)

    private val local = MutableStateFlow(Local(control.hasPassphrase(), batteryIgnored = platform.isIgnoringBatteryOptimizations()))
    private val smsLimitHelp = MutableStateFlow(false)

    val showSmsLimitHelp: StateFlow<Boolean> = smsLimitHelp.asStateFlow()

    val state: StateFlow<SettingsUiState> = combine(
        status.settings,
        status.sims,
        status.localServerInfo,
        combine(status.webhookEndpoints, status.cloudPairing) { endpoints, pairing ->
            endpoints.size to (pairing is CloudPairingState.Paired)
        },
        local
    ) { settings, sims, info, (webhookCount, paired), extra ->
        SettingsUiState(
            sending = SettingsMapping.sending(settings, sims),
            spend = SettingsMapping.spend(settings, sims),
            receiving = SettingsMapping.receiving(settings),
            server = ServerSettings(
                localEnabled = settings.localServer.enabled,
                localPort = settings.localServer.port,
                cloudPaired = paired,
                cloudServerUrl = settings.cloud.serverUrl ?: control.defaultCloudUrl,
                pullIntervalSeconds = settings.cloud.effectivePullIntervalSeconds
            ),
            security = SecuritySettings(
                apiKeyCount = info.keys.size,
                webhookCount = webhookCount,
                encryptionEnabled = extra.passphraseSet || extra.encryptionPending,
                encryptionPassphraseSet = extra.passphraseSet
            ),
            advanced = AdvancedSettings(
                keepAliveEnabled = settings.general.autostart,
                batteryOptimizationIgnored = extra.batteryIgnored,
                verboseLogging = settings.logs.verbose
            ),
            legal = LegalSettingsUi(termsAcceptedAtMillis = settings.legal.acceptedAt),
            appVersion = appVersion
        )
    }.stateIn(viewModelScope, SharingStarted.Eagerly, SettingsUiState(appVersion = appVersion))

    val actions = SettingsActions(onChange = ::change, onNavigate = ::open)

    fun onResume() {
        local.update { it.copy(passphraseSet = control.hasPassphrase(), batteryIgnored = platform.isIgnoringBatteryOptimizations()) }
    }

    fun dismissSmsLimitHelp() {
        smsLimitHelp.value = false
    }

    fun change(change: SettingsChange) {
        viewModelScope.launch {
            attempt {
                when (change) {
                    is SettingsChange.LocalServer -> control.setLocalEnabled(change.enabled)
                    is SettingsChange.LocalPort -> if (change.port in MIN_PORT..MAX_PORT) control.setLocalPort(change.port)
                    is SettingsChange.Encryption -> setEncryption(change.enabled)
                    is SettingsChange.EncryptionPassphrase -> setPassphrase(change.passphrase)
                    is SettingsChange.KeepAlive -> control.setAutostart(change.enabled)
                    is SettingsChange.CloudServerUrl -> setCloudUrl(change.url)
                    else -> control.updateSettings { SettingsMapping.apply(it, change) }
                }
            }
        }
    }

    private fun setEncryption(enabled: Boolean) {
        if (!enabled) control.setPassphrase(null)
        local.update { it.copy(passphraseSet = control.hasPassphrase(), encryptionPending = enabled) }
    }

    private fun setPassphrase(value: String) {
        if (value.isBlank()) return
        control.setPassphrase(value)
        local.update { it.copy(passphraseSet = control.hasPassphrase(), encryptionPending = false) }
    }

    private suspend fun setCloudUrl(value: String) {
        if (status.cloudPairing.value is CloudPairingState.Paired) return
        val normalized = PairingLink.normalizeServerUrl(value, control.allowCleartext) ?: return
        control.updateSettings { it.copy(cloud = it.cloud.copy(serverUrl = normalized)) }
    }

    fun open(destination: SettingsDestination) {
        when (destination) {
            SettingsDestination.ApiKeys, SettingsDestination.CloudPairing -> navigate(ConnectRoute)

            SettingsDestination.Webhooks -> navigate(WebhooksRoute)

            SettingsDestination.BatteryOptimization -> system(
                if (local.value.batteryIgnored) SystemAction.OpenBatteryOptimizationSettings else SystemAction.RequestBatteryExemption
            )

            SettingsDestination.OutgoingSmsLimit -> smsLimitHelp.value = true

            SettingsDestination.ExportLogs -> viewModelScope.launch {
                attempt { sharing.exportLogs() }.getOrNull()?.let { system(SystemAction.ShareFile(it.uri, it.mimeType)) }
            }

            SettingsDestination.Onboarding -> navigate(OnboardingRoute)

            SettingsDestination.TermsOfService -> system(LegalLinks.open(LegalDocument.Terms, legalBaseUrl))

            SettingsDestination.PrivacyPolicy -> system(LegalLinks.open(LegalDocument.Privacy, legalBaseUrl))

            SettingsDestination.AcceptableUsePolicy -> system(LegalLinks.open(LegalDocument.AcceptableUse, legalBaseUrl))

            SettingsDestination.About -> navigate(AboutRoute)
        }
    }

    private companion object {
        const val MIN_PORT = 1024
        const val MAX_PORT = 65535
    }
}

object SettingsMapping {
    private const val MINUTE_MS = 60_000L
    private const val MINUTES_PER_HOUR = 60
    private const val HOURS_PER_DAY = 24
    private const val MIN_PULL_SECONDS = 15
    private const val MAX_PORT = 65535

    fun sending(settings: AppSettings, sims: List<SimStats>): SendingSettings {
        val messages = settings.messages
        val hours = messages.workHours
        return SendingSettings(
            rateLimitPerMinute = ratePerMinute(messages.limitPeriod, messages.limitValue),
            delayBetweenSeconds = messages.sendIntervalMinSeconds.takeIf { it > 0 } ?: messages.sendIntervalMaxSeconds?.takeIf { it > 0 },
            dailyCaps = sims.map { SimCapUi(it.slot, it.carrier, settings.dailyLimitFor(it.slot)) },
            workingHours = WorkingHours(hours.enabled, minutes(hours.start), minutes(hours.end)),
            routing = routing(messages.simSelection),
            failoverToOtherSim = messages.failoverToOtherSim,
            deliveryReports = messages.deliveryReports
        )
    }

    fun spend(settings: AppSettings, sims: List<SimStats>, locale: Locale = Locale.getDefault()): SpendSettingsUi {
        val rates = settings.spend.simRatesMicros
        val rows = sims.map { SimRateUi(it.slot, it.carrier, rates[it.slot]) }.ifEmpty { listOf(SimRateUi(0, null, rates[0])) }
        return SpendSettingsUi(currencyCode = settings.spend.effectiveCurrency(locale), rates = rows)
    }

    fun receiving(settings: AppSettings): ReceivingSettings {
        val inbox = settings.inbox
        return ReceivingSettings(
            forwardSms = inbox.forwardSms,
            forwardDataSms = inbox.forwardDataSms,
            forwardMms = inbox.forwardMms,
            dataSmsPort = inbox.dataSmsPort,
            optOutEnabled = inbox.optOutEnabled,
            optOutKeywords = inbox.optOutKeywords ?: settings.cloud.remote?.optOutKeywords ?: RemoteDeviceSettings.DEFAULT_OPT_OUT_KEYWORDS
        )
    }

    fun ratePerMinute(period: LimitPeriod, value: Int): Int? {
        if (period == LimitPeriod.Disabled || value <= 0) return null
        if (period == LimitPeriod.PerMinute) return value
        return (value * MINUTE_MS / period.durationMs).toInt().coerceAtLeast(1)
    }

    fun routing(mode: SimSelectionMode): SimRoutingMode = when (mode) {
        SimSelectionMode.OSDefault -> SimRoutingMode.DefaultSim
        SimSelectionMode.RoundRobin, SimSelectionMode.Random -> SimRoutingMode.RoundRobin
        SimSelectionMode.LeastUsed -> SimRoutingMode.LeastUsed
        SimSelectionMode.CarrierMatch -> SimRoutingMode.CarrierMatch
    }

    fun selection(mode: SimRoutingMode): SimSelectionMode = when (mode) {
        SimRoutingMode.DefaultSim -> SimSelectionMode.OSDefault
        SimRoutingMode.RoundRobin -> SimSelectionMode.RoundRobin
        SimRoutingMode.LeastUsed -> SimSelectionMode.LeastUsed
        SimRoutingMode.CarrierMatch -> SimSelectionMode.CarrierMatch
    }

    fun minutes(value: String): Int {
        val parts = value.split(":")
        val hours = parts.getOrNull(0)?.toIntOrNull() ?: return 0
        return hours * MINUTES_PER_HOUR + (parts.getOrNull(1)?.toIntOrNull() ?: 0)
    }

    fun clock(minutes: Int): String =
        String.format(Locale.US, "%02d:%02d", minutes / MINUTES_PER_HOUR % HOURS_PER_DAY, minutes % MINUTES_PER_HOUR)

    fun keywords(values: List<String>): List<String> = values.map { it.trim().uppercase() }.filter { it.isNotEmpty() }.distinct()

    fun apply(settings: AppSettings, change: SettingsChange, locale: Locale = Locale.getDefault()): AppSettings = messages(settings, change)
        ?: inbox(settings, change)
        ?: spend(settings, change, locale)
        ?: other(settings, change)

    private fun spend(settings: AppSettings, change: SettingsChange, locale: Locale): AppSettings? {
        val spend = settings.spend
        val updated = when (change) {
            is SettingsChange.SpendCurrency -> if (SpendMath.isCurrencyCode(change.code)) spend.copy(currencyCode = change.code) else spend
            is SettingsChange.SmsRate -> spend.copy(currencyCode = spend.effectiveCurrency(locale)).withRate(change.slot, change.micros)
            else -> return null
        }
        return settings.copy(spend = updated)
    }

    private fun other(settings: AppSettings, change: SettingsChange): AppSettings = when (change) {
        is SettingsChange.PullInterval -> settings.copy(
            cloud = settings.cloud.copy(pullIntervalSeconds = change.seconds.coerceAtLeast(MIN_PULL_SECONDS))
        )

        is SettingsChange.VerboseLogging -> settings.copy(logs = settings.logs.copy(verbose = change.enabled))

        else -> settings
    }

    private fun messages(settings: AppSettings, change: SettingsChange): AppSettings? {
        val messages = settings.messages
        val updated = when (change) {
            is SettingsChange.RateLimit -> messages.copy(
                limitPeriod = if (change.perMinute == null) LimitPeriod.Disabled else LimitPeriod.PerMinute,
                limitValue = change.perMinute?.coerceAtLeast(0) ?: 0
            )

            is SettingsChange.SendDelay -> messages.copy(
                sendIntervalMinSeconds = change.seconds?.coerceAtLeast(0) ?: 0,
                sendIntervalMaxSeconds = change.seconds?.coerceAtLeast(0)
            )

            is SettingsChange.DailyCap -> messages.copy(simDailyLimits = dailyLimits(messages.simDailyLimits, change.slot, change.cap))

            is SettingsChange.WorkingHoursEnabled -> messages.copy(workHours = messages.workHours.copy(enabled = change.enabled))

            is SettingsChange.WorkingHoursRange -> messages.copy(
                workHours = messages.workHours.copy(start = clock(change.startMinutes), end = clock(change.endMinutes))
            )

            is SettingsChange.Routing -> messages.copy(simSelection = selection(change.mode))

            is SettingsChange.Failover -> messages.copy(failoverToOtherSim = change.enabled)

            is SettingsChange.DeliveryReports -> messages.copy(deliveryReports = change.enabled)

            else -> return null
        }
        return settings.copy(messages = updated)
    }

    private fun dailyLimits(current: Map<Int, Int>, slot: Int, cap: Int?): Map<Int, Int> =
        if (cap == null || cap <= 0) current - slot else current + (slot to cap)

    private fun inbox(settings: AppSettings, change: SettingsChange): AppSettings? {
        val inbox = settings.inbox
        val updated = when (change) {
            is SettingsChange.ForwardSms -> inbox.copy(forwardSms = change.enabled)
            is SettingsChange.ForwardDataSms -> inbox.copy(forwardDataSms = change.enabled)
            is SettingsChange.ForwardMms -> inbox.copy(forwardMms = change.enabled)
            is SettingsChange.DataSmsPort -> inbox.copy(dataSmsPort = change.port?.takeIf { it in 1..MAX_PORT })
            is SettingsChange.OptOutEnabled -> inbox.copy(optOutEnabled = change.enabled)
            is SettingsChange.OptOutKeywords -> inbox.copy(optOutKeywords = keywords(change.keywords))
            else -> return null
        }
        return settings.copy(inbox = updated)
    }
}
