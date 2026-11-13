package com.homingsms.app.feature.onboarding

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.homingsms.app.core.testing.HomingTestContent
import com.homingsms.app.feature.connect.CloudPairing
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class OnboardingScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun progressesThroughStepsAndAsksPermissionsOneAtATime() {
        var state by mutableStateOf(OnboardingUiState())
        val actions = OnboardingActions(
            onNext = { OnboardingFlow.next(state)?.let { state = state.copy(step = it) } },
            onBack = { OnboardingFlow.previous(state)?.let { state = state.copy(step = it) } },
            onRequestPermission = { requested ->
                state = state.copy(
                    permissions = state.permissions.map {
                        if (it.permission == requested) it.copy(status = PermissionStatus.Granted) else it
                    }
                )
            },
            onModeSelected = { state = state.copy(mode = it) }
        )
        composeRule.setContent { HomingTestContent { OnboardingScreen(state, actions) } }

        composeRule.onNodeWithText("Turn this phone into an SMS gateway").assertIsDisplayed()
        composeRule.onNodeWithText("Get started").performClick()
        composeRule.onNodeWithText("Step 1 of 4").assertIsDisplayed()

        composeRule.onNodeWithText("Permission 1 of 4").assertIsDisplayed()
        composeRule.onNodeWithText("Continue").assertIsNotEnabled()
        composeRule.onNodeWithText("Allow").performClick()
        composeRule.onNodeWithText("Permission 2 of 4").assertIsDisplayed()
        composeRule.onNodeWithText("Allow").performClick()
        composeRule.onNodeWithText("Allow").performClick()
        composeRule.onNodeWithText("Permission 4 of 4").assertIsDisplayed()
        composeRule.onNodeWithText("Not now").assertIsDisplayed()
        composeRule.onNodeWithText("Continue").assertIsEnabled().performClick()

        assertEquals(OnboardingStep.Battery, state.step)
        composeRule.onNodeWithText("Continue").performClick()

        composeRule.onNodeWithText("Continue").assertIsNotEnabled()
        composeRule.onNodeWithText("Homing Cloud").performClick()
        composeRule.onNodeWithText("Homing Cloud").assertIsSelected()
        composeRule.onNodeWithText("Step 3 of 5").assertIsDisplayed()
        composeRule.onNodeWithText("Continue").performClick()

        assertEquals(OnboardingStep.CloudPairing, state.step)
        composeRule.onNodeWithText("Continue").assertIsNotEnabled()
        state = state.copy(cloud = CloudPairing.Paired("Pixel", null, 0L, "https://api.homingsms.com"))
        composeRule.onNodeWithText("Continue").assertIsEnabled().performClick()

        assertEquals(OnboardingStep.TestMessage, state.step)
        composeRule.onNodeWithText("Finish").assertIsNotEnabled()
        composeRule.onNodeWithText("Back").performClick()
        assertEquals(OnboardingStep.CloudPairing, state.step)
    }

    @Test
    fun blockedPermissionOffersSettings() {
        var opened = 0
        val state = OnboardingSamples.permissions.copy(
            permissions = OnboardingSamples.permissions.permissions.map {
                if (it.permission == OnboardingPermission.ReceiveSms) it.copy(status = PermissionStatus.PermanentlyDenied) else it
            }
        )
        composeRule.setContent { HomingTestContent { OnboardingScreen(state, OnboardingActions(onOpenAppSettings = { opened++ })) } }
        composeRule.onNodeWithText("Android won't ask again. Turn it on in the app settings.").assertIsDisplayed()
        composeRule.onNodeWithText("Open app settings").performClick()
        assertEquals(1, opened)
    }

    @Test
    fun oemGuidanceMatchesMaker() {
        var oem = 0
        composeRule.setContent {
            HomingTestContent { OnboardingScreen(OnboardingSamples.battery, OnboardingActions(onOpenOemSettings = { oem++ })) }
        }
        composeRule.onNodeWithText("Also on Xiaomi, Redmi and POCO").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("Turn on Autostart.").assertExists()
        composeRule.onNodeWithText("Open phone settings").performScrollTo().performClick()
        assertEquals(1, oem)
    }

    @Test
    fun testMessageShowsArrivalAndEnablesFinish() {
        var finished = 0
        composeRule.setContent {
            HomingTestContent { OnboardingScreen(OnboardingSamples.testArrived, OnboardingActions(onFinish = { finished++ })) }
        }
        composeRule.onNodeWithText("It works. Homing is ready.").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("Finish").assertIsEnabled().performClick()
        assertEquals(1, finished)
    }
}
