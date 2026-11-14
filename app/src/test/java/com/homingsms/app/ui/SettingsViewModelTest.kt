package com.homingsms.app.ui

import app.cash.turbine.test
import com.homingsms.app.feature.settings.SettingsChange
import com.homingsms.app.feature.settings.SettingsDestination
import com.homingsms.app.feature.settings.SimRateUi
import com.homingsms.app.feature.settings.SimRoutingMode
import com.homingsms.app.ui.navigation.AboutRoute
import com.homingsms.app.ui.navigation.ConnectRoute
import com.homingsms.app.ui.navigation.OnboardingRoute
import com.homingsms.app.ui.navigation.WebhooksRoute
import com.homingsms.app.ui.settings.SettingsMapping
import com.homingsms.app.ui.settings.SettingsViewModel
import com.homingsms.app.ui.system.SystemAction
import com.homingsms.core.model.AppSettings
import com.homingsms.core.model.CloudPairingState
import com.homingsms.core.model.LegalSettings
import com.homingsms.core.model.LimitPeriod
import com.homingsms.core.model.SimSelectionMode
import com.homingsms.core.model.SimStats
import com.homingsms.core.model.SpendSettings
import com.homingsms.core.model.WorkHours
import java.util.Locale
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class SettingsViewModelTest {
    @get:Rule
    val main = MainDispatcherRule()

    private val status = FakeAppStatus()
    private val control = FakeGatewayControl(status)
    private val platform = FakePlatformState(batteryIgnored = false)
    private val sharing = FakeFileSharing()

    private fun viewModel() = SettingsViewModel(status, control, platform, sharing, "1.2.3")

    @Test
    fun mapsStoredSettings() {
        status.settings.value = AppSettings().let { base ->
            base.copy(
                messages = base.messages.copy(
                    limitPeriod = LimitPeriod.PerHour,
                    limitValue = 120,
                    sendIntervalMinSeconds = 5,
                    workHours = WorkHours(true, "08:30", "20:15"),
                    simSelection = SimSelectionMode.LeastUsed,
                    simDailyLimits = mapOf(0 to 200),
                    deliveryReports = false
                ),
                inbox = base.inbox.copy(forwardDataSms = false, dataSmsPort = 9200, optOutKeywords = listOf("STOP")),
                cloud = base.cloud.copy(pullIntervalSeconds = 45),
                logs = base.logs.copy(verbose = true),
                general = base.general.copy(autostart = true)
            )
        }
        status.sims.value = listOf(SimStats(0, "Carrier", null, null, 0, 200, true))
        status.webhookEndpoints.value = listOf(endpoint("whk_1"))
        control.storedPassphrase = "secret"

        val state = viewModel().state.value

        assertEquals(2, state.sending.rateLimitPerMinute)
        assertEquals(5, state.sending.delayBetweenSeconds)
        assertEquals(200, state.sending.dailyCaps.single().dailyCap)
        assertEquals(510, state.sending.workingHours.startMinutes)
        assertEquals(1215, state.sending.workingHours.endMinutes)
        assertEquals(SimRoutingMode.LeastUsed, state.sending.routing)
        assertFalse(state.sending.deliveryReports)
        assertFalse(state.receiving.forwardDataSms)
        assertEquals(9200, state.receiving.dataSmsPort)
        assertEquals(listOf("STOP"), state.receiving.optOutKeywords)
        assertEquals(45, state.server.pullIntervalSeconds)
        assertEquals("https://api.example.test", state.server.cloudServerUrl)
        assertEquals(1, state.security.webhookCount)
        assertTrue(state.security.encryptionEnabled && state.security.encryptionPassphraseSet)
        assertTrue(state.advanced.keepAliveEnabled && state.advanced.verboseLogging)
        assertFalse(state.advanced.batteryOptimizationIgnored)
        assertEquals("1.2.3", state.appVersion)
    }

    @Test
    fun appliesEveryStoredChange() {
        val viewModel = viewModel()
        listOf(
            SettingsChange.RateLimit(20),
            SettingsChange.SendDelay(3),
            SettingsChange.DailyCap(1, 150),
            SettingsChange.WorkingHoursEnabled(true),
            SettingsChange.WorkingHoursRange(9 * 60, 17 * 60 + 30),
            SettingsChange.Routing(SimRoutingMode.CarrierMatch),
            SettingsChange.Failover(true),
            SettingsChange.DeliveryReports(false),
            SettingsChange.ForwardSms(false),
            SettingsChange.ForwardDataSms(true),
            SettingsChange.ForwardMms(false),
            SettingsChange.DataSmsPort(8901),
            SettingsChange.OptOutEnabled(false),
            SettingsChange.OptOutKeywords(listOf(" stop ", "Quit", "")),
            SettingsChange.PullInterval(5),
            SettingsChange.VerboseLogging(true)
        ).forEach(viewModel.actions.onChange)

        val settings = status.settings.value
        assertEquals(LimitPeriod.PerMinute, settings.messages.limitPeriod)
        assertEquals(20, settings.messages.limitValue)
        assertEquals(3, settings.messages.sendIntervalMinSeconds)
        assertEquals(3, settings.messages.sendIntervalMaxSeconds)
        assertEquals(mapOf(1 to 150), settings.messages.simDailyLimits)
        assertEquals(WorkHours(true, "09:00", "17:30"), settings.messages.workHours)
        assertEquals(SimSelectionMode.CarrierMatch, settings.messages.simSelection)
        assertTrue(settings.messages.failoverToOtherSim)
        assertFalse(settings.messages.deliveryReports)
        assertFalse(settings.inbox.forwardSms)
        assertTrue(settings.inbox.forwardDataSms)
        assertFalse(settings.inbox.forwardMms)
        assertEquals(8901, settings.inbox.dataSmsPort)
        assertFalse(settings.inbox.optOutEnabled)
        assertEquals(listOf("STOP", "QUIT"), settings.inbox.optOutKeywords)
        assertTrue(settings.optOutKeywords.isEmpty())
        assertEquals(15, settings.cloud.pullIntervalSeconds)
        assertTrue(settings.logs.verbose)

        viewModel.actions.onChange(SettingsChange.RateLimit(null))
        viewModel.actions.onChange(SettingsChange.DailyCap(1, null))
        assertEquals(LimitPeriod.Disabled, status.settings.value.messages.limitPeriod)
        assertTrue(status.settings.value.messages.simDailyLimits.isEmpty())
    }

    @Test
    fun serviceChangesGoThroughControl() {
        val viewModel = viewModel()
        viewModel.actions.onChange(SettingsChange.LocalServer(true))
        viewModel.actions.onChange(SettingsChange.LocalPort(9090))
        viewModel.actions.onChange(SettingsChange.LocalPort(80))
        viewModel.actions.onChange(SettingsChange.KeepAlive(false))
        assertEquals(listOf("local:true", "port:9090", "autostart:false"), control.calls)
    }

    @Test
    fun encryptionNeedsPassphraseAndCanBeTurnedOff() {
        val viewModel = viewModel()
        viewModel.actions.onChange(SettingsChange.Encryption(true))
        assertTrue(viewModel.state.value.security.encryptionEnabled)
        assertFalse(viewModel.state.value.security.encryptionPassphraseSet)

        viewModel.actions.onChange(SettingsChange.EncryptionPassphrase("correct horse"))
        assertEquals("correct horse", control.storedPassphrase)
        assertTrue(viewModel.state.value.security.encryptionPassphraseSet)

        viewModel.actions.onChange(SettingsChange.Encryption(false))
        assertNull(control.storedPassphrase)
        assertFalse(viewModel.state.value.security.encryptionEnabled)
    }

    @Test
    fun cloudUrlIsValidatedAndLockedWhilePaired() {
        val viewModel = viewModel()
        viewModel.actions.onChange(SettingsChange.CloudServerUrl("ftp://nope"))
        assertNull(status.settings.value.cloud.serverUrl)
        viewModel.actions.onChange(SettingsChange.CloudServerUrl("https://own.example.test/"))
        assertEquals("https://own.example.test", status.settings.value.cloud.serverUrl)

        status.cloudPairing.value = CloudPairingState.Paired("dev", "acc", "https://own.example.test", 1L)
        viewModel.actions.onChange(SettingsChange.CloudServerUrl("https://other.example.test"))
        assertEquals("https://own.example.test", status.settings.value.cloud.serverUrl)
    }

    @Test
    fun destinations() = runTest {
        val viewModel = viewModel()
        viewModel.effects.test {
            viewModel.actions.onNavigate(SettingsDestination.ApiKeys)
            assertEquals(UiEffect.Navigate(ConnectRoute), awaitItem())
            viewModel.actions.onNavigate(SettingsDestination.CloudPairing)
            assertEquals(UiEffect.Navigate(ConnectRoute), awaitItem())
            viewModel.actions.onNavigate(SettingsDestination.Webhooks)
            assertEquals(UiEffect.Navigate(WebhooksRoute), awaitItem())
            viewModel.actions.onNavigate(SettingsDestination.BatteryOptimization)
            assertEquals(UiEffect.System(SystemAction.RequestBatteryExemption), awaitItem())
            viewModel.actions.onNavigate(SettingsDestination.ExportLogs)
            assertEquals(UiEffect.System(SystemAction.ShareFile("content://logs", "text/plain")), awaitItem())
            viewModel.actions.onNavigate(SettingsDestination.Onboarding)
            assertEquals(UiEffect.Navigate(OnboardingRoute), awaitItem())
            viewModel.actions.onNavigate(SettingsDestination.About)
            assertEquals(UiEffect.Navigate(AboutRoute), awaitItem())
        }
        viewModel.actions.onNavigate(SettingsDestination.OutgoingSmsLimit)
        assertTrue(viewModel.showSmsLimitHelp.value)
        viewModel.dismissSmsLimitHelp()
        assertFalse(viewModel.showSmsLimitHelp.value)
    }

    @Test
    fun legalRowsOpenDocumentsAndShowAcceptance() = runTest {
        assertNull(viewModel().state.value.legal.termsAcceptedAtMillis)

        status.settings.value = AppSettings(legal = LegalSettings().accept(1_700_000_000_000L))
        val viewModel = SettingsViewModel(status, control, platform, sharing, "1.2.3", legalBaseUrl = "https://legal.example.test/")
        assertEquals(1_700_000_000_000L, viewModel.state.value.legal.termsAcceptedAtMillis)
        viewModel.effects.test {
            viewModel.actions.onNavigate(SettingsDestination.TermsOfService)
            assertEquals(UiEffect.System(SystemAction.OpenUrl("https://legal.example.test/legal/terms")), awaitItem())
            viewModel.actions.onNavigate(SettingsDestination.PrivacyPolicy)
            assertEquals(UiEffect.System(SystemAction.OpenUrl("https://legal.example.test/legal/privacy")), awaitItem())
            viewModel.actions.onNavigate(SettingsDestination.AcceptableUsePolicy)
            assertEquals(UiEffect.System(SystemAction.OpenUrl("https://legal.example.test/legal/acceptable-use")), awaitItem())
        }
    }

    @Test
    fun spendRowsFollowSimsAndFallBackToTheFirstSlot() {
        status.settings.value = AppSettings(spend = SpendSettings("KES", mapOf(1 to 800_000L)))
        status.sims.value = listOf(SimStats(0, "Safaricom", null, 3, 0, null, true), SimStats(1, "Airtel", null, 2, 0, null, true))
        val spend = viewModel().state.value.spend
        assertEquals("KES", spend.currencyCode)
        assertEquals(listOf(SimRateUi(0, "Safaricom", null), SimRateUi(1, "Airtel", 800_000L)), spend.rates)

        val noSims = SettingsMapping.spend(AppSettings(), emptyList(), Locale("en", "NG"))
        assertEquals("NGN", noSims.currencyCode)
        assertEquals(listOf(SimRateUi(0, null, null)), noSims.rates)
    }

    @Test
    fun spendChangesValidateAndPinTheCurrency() {
        val base = AppSettings()
        val rated = SettingsMapping.apply(base, SettingsChange.SmsRate(0, 7_500L), Locale.GERMANY)
        assertEquals(SpendSettings("EUR", mapOf(0 to 7_500L)), rated.spend)

        val renamed = SettingsMapping.apply(rated, SettingsChange.SpendCurrency("USD"))
        assertEquals("USD", renamed.spend.currencyCode)
        assertEquals(renamed, SettingsMapping.apply(renamed, SettingsChange.SpendCurrency("ABC")))
        assertEquals(renamed, SettingsMapping.apply(renamed, SettingsChange.SpendCurrency("usd")))

        val cleared = SettingsMapping.apply(renamed, SettingsChange.SmsRate(0, null))
        assertTrue(cleared.spend.simRatesMicros.isEmpty())
        assertEquals("USD", cleared.spend.currencyCode)
        assertTrue(SettingsMapping.apply(renamed, SettingsChange.SmsRate(0, -5L)).spend.simRatesMicros.isEmpty())
    }

    @Test
    fun spendChangesReachStoredSettings() {
        val viewModel = viewModel()
        viewModel.actions.onChange(SettingsChange.SpendCurrency("GBP"))
        viewModel.actions.onChange(SettingsChange.SmsRate(1, 12_000L))
        assertEquals(SpendSettings("GBP", mapOf(1 to 12_000L)), status.settings.value.spend)
    }

    @Test
    fun mappingHelpers() {
        assertEquals(1, SettingsMapping.ratePerMinute(LimitPeriod.PerDay, 100))
        assertNull(SettingsMapping.ratePerMinute(LimitPeriod.Disabled, 100))
        assertEquals(SimRoutingMode.RoundRobin, SettingsMapping.routing(SimSelectionMode.Random))
        assertEquals("07:05", SettingsMapping.clock(425))
        assertEquals(425, SettingsMapping.minutes("07:05"))
    }
}
