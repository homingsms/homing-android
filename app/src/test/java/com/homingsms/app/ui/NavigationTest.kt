package com.homingsms.app.ui

import android.app.Application
import android.content.Intent
import android.net.Uri
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isDialog
import androidx.compose.ui.test.isToggleable
import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performSemanticsAction
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import com.homingsms.app.MainActivity
import com.homingsms.app.ui.about.BuildInfo
import com.homingsms.app.ui.about.LicenseSource
import com.homingsms.app.ui.connect.PairLinkInbox
import com.homingsms.app.ui.data.AppStatus
import com.homingsms.app.ui.data.FileSharing
import com.homingsms.app.ui.data.GatewayControl
import com.homingsms.app.ui.data.InboxStore
import com.homingsms.app.ui.data.MessageStore
import com.homingsms.app.ui.data.WebhookStore
import com.homingsms.app.ui.system.PlatformState
import com.homingsms.core.model.DeviceHealth
import com.homingsms.core.model.LegalSettings
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

class NavigationTestApplication : Application()

@RunWith(RobolectricTestRunner::class)
@Config(application = NavigationTestApplication::class)
class NavigationTest {
    @get:Rule
    val compose = createEmptyComposeRule()

    private val status = FakeAppStatus()
    private val control = FakeGatewayControl(status)
    private val links = PairLinkInbox()

    @Before
    fun startDependencies() {
        val fakes = module {
            single<AppStatus> { status }
            single<GatewayControl> { control }
            single<MessageStore> { FakeMessageStore() }
            single<InboxStore> { FakeInboxStore() }
            single<WebhookStore> { FakeWebhookStore() }
            single<FileSharing> { FakeFileSharing() }
            single<PlatformState> { FakePlatformState(batteryIgnored = true) }
            single<LicenseSource> { FakeLicenseSource(emptyList()) }
            single { BuildInfo("1.0.0", 1L, "debug") }
            single { links }
        }
        startKoin { modules(fakes, uiModule) }
    }

    @After
    fun stopDependencies() {
        stopKoin()
    }

    private fun launch(
        onboardingCompleted: Boolean,
        link: String? = null,
        termsAccepted: Boolean = onboardingCompleted
    ): ActivityScenario<MainActivity> {
        status.settings.value = status.settings.value.let {
            it.copy(
                general = it.general.copy(onboardingCompleted = onboardingCompleted),
                legal = if (termsAccepted) it.legal.accept(1L) else LegalSettings("2025-01-01", 1L)
            )
        }
        status.health.value = DeviceHealth.Unknown.copy(collectedAt = 1L)
        val context = ApplicationProvider.getApplicationContext<Application>()
        val intent = if (link == null) {
            Intent(context, MainActivity::class.java).setAction(Intent.ACTION_MAIN)
        } else {
            Intent(Intent.ACTION_VIEW, Uri.parse(link)).setClass(context, MainActivity::class.java)
        }
        return ActivityScenario.launch<MainActivity>(intent).also { compose.waitForIdle() }
    }

    @Test
    fun firstRunOpensOnboardingWithoutTheNavigationBar() {
        launch(onboardingCompleted = false).use {
            compose.onNodeWithText("Turn this phone into an SMS gateway").assertIsDisplayed()
            compose.onNodeWithText("Inbox").assertDoesNotExist()
        }
    }

    @Test
    fun laterLaunchesOpenHomeInsideTheShell() {
        launch(onboardingCompleted = true).use {
            compose.onNodeWithText("Today").assertIsDisplayed()
            compose.onNodeWithText("Inbox").performClick()
            compose.waitForIdle()
            compose.onNodeWithText("Nothing received yet").assertIsDisplayed()
            compose.onNodeWithText("Turn this phone into an SMS gateway").assertDoesNotExist()
        }
    }

    @Test
    fun outdatedTermsBlockTheShellUntilAccepted() {
        launch(onboardingCompleted = true, termsAccepted = false).use {
            compose.onNodeWithText("Updated terms").assertIsDisplayed()
            compose.onNodeWithText("Inbox").assertDoesNotExist()
            compose.onNodeWithText("Accept").performScrollTo().assertIsNotEnabled()
            compose.onNode(isToggleable()).performSemanticsAction(SemanticsActions.OnClick)
            compose.onNodeWithText("Accept").assertIsEnabled().performClick()
            compose.waitForIdle()
            compose.onNodeWithText("Today").assertIsDisplayed()
            compose.onNodeWithText("Updated terms").assertDoesNotExist()
            assertTrue(status.settings.value.legal.acceptedCurrentTerms)
            assertFalse("stop" in control.calls)
        }
    }

    @Test
    fun pairLinkOpensConnectWithPrefilledCodeAndConfirmation() {
        launch(onboardingCompleted = true, link = PAIR_LINK).use {
            compose.onNodeWithText("Pair with Homing Cloud?").assertIsDisplayed()
            compose.onNodeWithText("cloud.example.test", substring = true).assertIsDisplayed()
            compose.onNode(hasText("Pair") and hasAnyAncestor(isDialog())).performClick()
            compose.waitForIdle()
            assertTrue(control.calls.contains("pairLink:$PAIR_LINK"))
            compose.onNodeWithText("Pair with Homing Cloud?").assertDoesNotExist()
        }
    }

    @Test
    fun pairLinkDuringFirstRunJumpsToCloudPairingStep() {
        launch(onboardingCompleted = false, link = PAIR_LINK).use {
            compose.onNodeWithText("Pair with Homing Cloud?").assertIsDisplayed()
            compose.onNodeWithText("Inbox").assertDoesNotExist()
        }
    }

    private companion object {
        const val PAIR_LINK = "homing://pair?server=https://cloud.example.test&code=ABCD12"
    }
}
