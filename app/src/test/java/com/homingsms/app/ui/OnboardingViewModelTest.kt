package com.homingsms.app.ui

import android.Manifest
import app.cash.turbine.test
import com.homingsms.app.feature.connect.CloudPairing
import com.homingsms.app.feature.onboarding.GatewayMode
import com.homingsms.app.feature.onboarding.LegalLink
import com.homingsms.app.feature.onboarding.OnboardingPermission
import com.homingsms.app.feature.onboarding.OnboardingStep
import com.homingsms.app.feature.onboarding.PermissionStatus
import com.homingsms.app.feature.onboarding.TestMessageStatus
import com.homingsms.app.ui.connect.PairLinkInbox
import com.homingsms.app.ui.navigation.HomeRoute
import com.homingsms.app.ui.navigation.TermsRoute
import com.homingsms.app.ui.onboarding.OnboardingViewModel
import com.homingsms.app.ui.system.PermissionOutcome
import com.homingsms.app.ui.system.SystemAction
import com.homingsms.core.model.AppSettings
import com.homingsms.core.model.LegalSettings
import com.homingsms.core.model.LegalTerms
import com.homingsms.core.model.MessageError
import com.homingsms.core.model.MessageState
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.currentTime
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class OnboardingViewModelTest {
    @get:Rule
    val main = MainDispatcherRule(StandardTestDispatcher())

    private val status = FakeAppStatus().apply { serviceRunning.value = false }
    private val control = FakeGatewayControl(status)
    private val platform = FakePlatformState(batteryIgnored = false)
    private val messages = FakeMessageStore()
    private val inbox = FakeInboxStore()
    private val links = PairLinkInbox()

    private fun kotlinx.coroutines.test.TestScope.viewModel(sdkInt: Int = 34) =
        OnboardingViewModel(status, control, platform, messages, inbox, links, sdkInt, clock = { currentTime })

    private val smsPermissions = listOf(
        Manifest.permission.SEND_SMS,
        Manifest.permission.RECEIVE_SMS,
        Manifest.permission.READ_SMS,
        Manifest.permission.RECEIVE_MMS,
        Manifest.permission.RECEIVE_WAP_PUSH,
        Manifest.permission.READ_PHONE_STATE,
        "android.permission.READ_PHONE_NUMBERS"
    )

    @Test
    fun walksThroughStepsAndAppliesMode() = runTest {
        val viewModel = viewModel()
        runCurrent()
        assertEquals(OnboardingStep.Welcome, viewModel.state.value.step)
        assertFalse(viewModel.state.value.needsRestrictedSettings)
        assertFalse(viewModel.state.value.termsAccepted)

        viewModel.actions.onNext()
        runCurrent()
        assertEquals(OnboardingStep.Welcome, viewModel.state.value.step)
        assertEquals(LegalSettings(), status.settings.value.legal)

        advanceTimeBy(5_000)
        viewModel.actions.onTermsAcceptedChange(true)
        runCurrent()
        assertTrue(viewModel.state.value.canContinue)
        viewModel.actions.onNext()
        runCurrent()
        assertEquals(OnboardingStep.Permissions, viewModel.state.value.step)
        assertEquals(LegalSettings(LegalTerms.CURRENT_VERSION, 5_000L), status.settings.value.legal)
        viewModel.actions.onNext()
        runCurrent()
        assertEquals(OnboardingStep.Permissions, viewModel.state.value.step)

        platform.granted += smsPermissions
        viewModel.onResume()
        runCurrent()
        assertTrue(viewModel.state.value.requiredPermissionsGranted)
        viewModel.actions.onNext()
        runCurrent()
        assertEquals(OnboardingStep.Battery, viewModel.state.value.step)
        viewModel.actions.onNext()
        runCurrent()
        assertEquals(OnboardingStep.Mode, viewModel.state.value.step)
        viewModel.actions.onModeSelected(GatewayMode.Local)
        runCurrent()
        viewModel.actions.onNext()
        advanceUntilIdle()
        assertEquals(OnboardingStep.TestMessage, viewModel.state.value.step)
        assertEquals(listOf("local:true", "cloud:false"), control.calls)
    }

    @Test
    fun restrictedSettingsStepForSideloadedAndroid13() = runTest {
        platform.sideloaded = true
        val viewModel = viewModel(sdkInt = 33)
        runCurrent()
        assertTrue(viewModel.state.value.needsRestrictedSettings)
        viewModel.actions.onTermsAcceptedChange(true)
        runCurrent()
        viewModel.actions.onNext()
        runCurrent()
        assertEquals(OnboardingStep.RestrictedSettings, viewModel.state.value.step)
    }

    @Test
    fun permissionStatusesFollowRequestOutcomes() = runTest {
        val viewModel = viewModel()
        viewModel.effects.test {
            viewModel.actions.onRequestPermission(OnboardingPermission.SendSms)
            assertEquals(UiEffect.System(SystemAction.RequestPermissions(listOf(Manifest.permission.SEND_SMS))), awaitItem())
            viewModel.actions.onOpenOemSettings()
            assertEquals(UiEffect.System(SystemAction.OpenOemSettings(platform.deviceMaker)), awaitItem())
            viewModel.actions.onRequestBatteryExemption()
            assertEquals(UiEffect.System(SystemAction.RequestBatteryExemption), awaitItem())
        }
        runCurrent()
        assertEquals(PermissionStatus.NotRequested, status(viewModel, OnboardingPermission.SendSms))

        viewModel.onPermissionsResult(listOf(PermissionOutcome(Manifest.permission.SEND_SMS, granted = false, canAskAgain = true)))
        runCurrent()
        assertEquals(PermissionStatus.Denied, status(viewModel, OnboardingPermission.SendSms))

        viewModel.onPermissionsResult(listOf(PermissionOutcome(Manifest.permission.READ_SMS, granted = false, canAskAgain = false)))
        runCurrent()
        assertEquals(PermissionStatus.PermanentlyDenied, status(viewModel, OnboardingPermission.ReceiveSms))

        platform.granted += Manifest.permission.SEND_SMS
        viewModel.onResume()
        runCurrent()
        assertEquals(PermissionStatus.Granted, status(viewModel, OnboardingPermission.SendSms))

        viewModel.actions.onSkipPermission(OnboardingPermission.Notifications)
        runCurrent()
        assertEquals(PermissionStatus.Denied, status(viewModel, OnboardingPermission.Notifications))
    }

    @Test
    fun deepLinkJumpsToCloudPairingWithCodeAndConfirmation() = runTest {
        val viewModel = viewModel()
        links.offer("homing://pair?server=https://cloud.example.test&code=ABCD12")
        runCurrent()

        val state = viewModel.state.value
        assertEquals(OnboardingStep.CloudPairing, state.step)
        assertEquals(GatewayMode.Cloud, state.mode)
        assertEquals("ABCD12", state.pairingCode)
        assertEquals("cloud.example.test", viewModel.pendingLink.value?.serverHost)

        viewModel.confirmPairLink()
        runCurrent()
        assertTrue(viewModel.state.value.cloud is CloudPairing.Paired)
        assertTrue(viewModel.state.value.canContinue)
    }

    @Test
    fun testMessageReportsSentDeliveredAndReceived() = runTest {
        val viewModel = viewModel()
        runCurrent()
        assertEquals("+15550100", viewModel.state.value.test.phoneNumber)

        viewModel.actions.onSendTest()
        runCurrent()
        assertTrue("start" in control.calls)
        val (to, text) = messages.sent.single()
        assertEquals("+15550100", to)
        assertEquals(TestMessageStatus.Sending, viewModel.state.value.test.status)

        messages.setState("msg_test") { it.copy(state = MessageState.Sent, sentAt = 10L) }
        advanceTimeBy(2_100)
        assertEquals(TestMessageStatus.Sent(10L), viewModel.state.value.test.status)

        messages.setState("msg_test") { it.copy(state = MessageState.Delivered, deliveredAt = 20L) }
        advanceTimeBy(2_100)
        assertEquals(TestMessageStatus.Delivered(10L, 20L), viewModel.state.value.test.status)

        inbox.records.value = listOf(inbox("inb_t", text = text, receivedAt = 30L))
        advanceTimeBy(2_100)
        assertEquals(TestMessageStatus.Received(10L, 20L, 30L, text), viewModel.state.value.test.status)
        assertTrue(viewModel.state.value.test.arrived)
    }

    @Test
    fun testMessageFailureShowsCarrierError() = runTest {
        val viewModel = viewModel()
        runCurrent()
        viewModel.actions.onSendTest()
        runCurrent()
        messages.setState("msg_test") { it.copy(state = MessageState.Failed, error = MessageError("SEND_FAILED", "No service")) }
        advanceTimeBy(2_100)
        assertEquals(TestMessageStatus.Failed("No service"), viewModel.state.value.test.status)
    }

    @Test
    fun finishMarksOnboardingDoneAndGoesHome() = runTest {
        val viewModel = viewModel()
        viewModel.actions.onModeSelected(GatewayMode.Both)
        viewModel.actions.onTermsAcceptedChange(true)
        runCurrent()
        viewModel.effects.test {
            viewModel.actions.onFinish()
            assertEquals(UiEffect.ResetTo(HomeRoute), awaitItem())
        }
        val settings = status.settings.value
        assertTrue(settings.general.onboardingCompleted)
        assertTrue(settings.general.autostart)
        assertTrue(settings.localServer.enabled && settings.cloud.enabled)
        assertTrue("start" in control.calls)
        assertTrue(settings.legal.acceptedCurrentTerms)
    }

    @Test
    fun finishingWithoutAcceptingTheTermsOpensTheTermsGate() = runTest {
        val viewModel = viewModel()
        links.offer("homing://pair?server=https://cloud.example.test&code=ABCD12")
        runCurrent()
        assertEquals(OnboardingStep.CloudPairing, viewModel.state.value.step)
        viewModel.effects.test {
            viewModel.actions.onFinish()
            assertEquals(UiEffect.ResetTo(TermsRoute), awaitItem())
        }
        assertTrue(status.settings.value.general.onboardingCompleted)
        assertEquals(LegalSettings(), status.settings.value.legal)
    }

    @Test
    fun rerunningSetupKeepsCurrentAcceptanceCheckedAndOpensDocuments() = runTest {
        status.settings.value = AppSettings(legal = LegalSettings().accept(42L))
        val viewModel = OnboardingViewModel(
            status, control, platform, messages, inbox, links, 34,
            clock = { currentTime },
            legalBaseUrl = "https://legal.example.test"
        )
        runCurrent()
        assertTrue(viewModel.state.value.termsAccepted)
        assertTrue(viewModel.state.value.canContinue)
        viewModel.effects.test {
            viewModel.actions.onOpenLegal(LegalLink.Privacy)
            assertEquals(UiEffect.System(SystemAction.OpenUrl("https://legal.example.test/legal/privacy")), awaitItem())
            viewModel.actions.onOpenLegal(LegalLink.AcceptableUse)
            assertEquals(UiEffect.System(SystemAction.OpenUrl("https://legal.example.test/legal/acceptable-use")), awaitItem())
        }
        viewModel.actions.onNext()
        runCurrent()
        assertEquals(42L, status.settings.value.legal.acceptedAt)
    }

    private fun status(viewModel: OnboardingViewModel, permission: OnboardingPermission) =
        viewModel.state.value.permissions.first { it.permission == permission }.status
}
