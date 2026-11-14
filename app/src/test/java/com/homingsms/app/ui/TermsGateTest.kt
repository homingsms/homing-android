package com.homingsms.app.ui

import app.cash.turbine.test
import com.homingsms.app.feature.onboarding.LegalLink
import com.homingsms.app.ui.connect.PairLinkInbox
import com.homingsms.app.ui.navigation.AppViewModel
import com.homingsms.app.ui.navigation.ConnectRoute
import com.homingsms.app.ui.navigation.HomeRoute
import com.homingsms.app.ui.navigation.OnboardingRoute
import com.homingsms.app.ui.navigation.TermsRoute
import com.homingsms.app.ui.onboarding.UpdatedTermsViewModel
import com.homingsms.app.ui.system.SystemAction
import com.homingsms.core.model.AppSettings
import com.homingsms.core.model.GeneralSettings
import com.homingsms.core.model.LegalSettings
import com.homingsms.core.model.LegalTerms
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class TermsGateTest {
    @get:Rule
    val main = MainDispatcherRule()

    private val onboarded = AppSettings(general = GeneralSettings(onboardingCompleted = true))

    @Test
    fun startRouteAsksForTheCurrentTermsAfterOnboarding() {
        assertEquals(OnboardingRoute, AppViewModel.startRoute(AppSettings()))
        assertEquals(OnboardingRoute, AppViewModel.startRoute(AppSettings(legal = LegalSettings().accept(1L))))
        assertEquals(TermsRoute, AppViewModel.startRoute(onboarded))
        assertEquals(TermsRoute, AppViewModel.startRoute(onboarded.copy(legal = LegalSettings("2025-01-01", 1L))))
        assertEquals(HomeRoute, AppViewModel.startRoute(onboarded.copy(legal = LegalSettings(LegalTerms.CURRENT_VERSION, 1L))))
        assertEquals(TermsRoute, AppViewModel(FakeAppStatus(onboarded), PairLinkInbox()).startRoute)
    }

    @Test
    fun pairLinksWaitBehindTheTermsGate() = runTest {
        val links = PairLinkInbox()
        val viewModel = AppViewModel(FakeAppStatus(onboarded), links)
        viewModel.routes.test {
            viewModel.onLink(PAIR_LINK)
            expectNoEvents()
        }
        assertEquals(PAIR_LINK, links.links.value)
    }

    @Test
    fun acceptStaysDisabledUntilCheckedThenStoresVersionAndTime() = runTest {
        val status = FakeAppStatus(onboarded).apply { serviceRunning.value = true }
        val control = FakeGatewayControl(status)
        val viewModel = UpdatedTermsViewModel(control, PairLinkInbox(), "https://legal.example.test", clock = { 1_234L })

        viewModel.effects.test {
            viewModel.actions.onAccept()
            expectNoEvents()
            assertEquals(LegalSettings(), status.settings.value.legal)

            viewModel.actions.onOpenLegal(LegalLink.Terms)
            assertEquals(UiEffect.System(SystemAction.OpenUrl("https://legal.example.test/legal/terms")), awaitItem())

            viewModel.actions.onAcceptedChange(true)
            assertTrue(viewModel.state.value.accepted)
            viewModel.actions.onAccept()
            assertEquals(UiEffect.ResetTo(HomeRoute), awaitItem())
            expectNoEvents()
        }
        assertEquals(LegalSettings(LegalTerms.CURRENT_VERSION, 1_234L), status.settings.value.legal)
        assertEquals(HomeRoute, AppViewModel.startRoute(status.settings.value))
        assertFalse("stop" in control.calls)
        assertTrue(status.serviceRunning.value)
    }

    @Test
    fun acceptingWithAPendingPairLinkContinuesToConnect() = runTest {
        val status = FakeAppStatus(onboarded)
        val links = PairLinkInbox().apply { offer(PAIR_LINK) }
        val viewModel = UpdatedTermsViewModel(FakeGatewayControl(status), links, "https://legal.example.test", clock = { 1L })
        viewModel.effects.test {
            viewModel.actions.onAcceptedChange(true)
            viewModel.actions.onAccept()
            assertEquals(UiEffect.ResetTo(HomeRoute), awaitItem())
            assertEquals(UiEffect.Navigate(ConnectRoute), awaitItem())
        }
    }

    private companion object {
        const val PAIR_LINK = "homing://pair?server=https://cloud.example.test&code=ABCD12"
    }
}
