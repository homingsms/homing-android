package com.homingsms.app.feature.onboarding

import androidx.compose.ui.test.junit4.createComposeRule
import com.homingsms.app.core.testing.ScreenshotVariant
import com.homingsms.app.core.testing.captureScreen
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.ParameterizedRobolectricTestRunner
import org.robolectric.annotation.GraphicsMode

@RunWith(ParameterizedRobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class OnboardingScreenshotTest(private val variant: ScreenshotVariant) {

    @get:Rule
    val composeRule = createComposeRule()

    private fun capture(name: String, state: OnboardingUiState) =
        composeRule.captureScreen("onboarding_$name", variant) { OnboardingScreen(state, OnboardingActions()) }

    @Test
    fun welcome() = capture("welcome", OnboardingSamples.welcome)

    @Test
    fun updatedTerms() = composeRule.captureScreen("onboarding_updated_terms", variant) {
        UpdatedTermsScreen(OnboardingSamples.updatedTerms, UpdatedTermsActions())
    }

    @Test
    fun permissions() = capture("permissions", OnboardingSamples.permissions)

    @Test
    fun battery() = capture("battery", OnboardingSamples.battery)

    @Test
    fun restrictedSettings() = capture("restricted_settings", OnboardingSamples.restricted)

    @Test
    fun mode() = capture("mode", OnboardingSamples.mode)

    @Test
    fun pairing() = capture("pairing", OnboardingSamples.pairing)

    @Test
    fun testMessage() = capture("test_message", OnboardingSamples.testArrived)

    companion object {
        @JvmStatic
        @ParameterizedRobolectricTestRunner.Parameters(name = "{0}")
        fun variants() = ScreenshotVariant.entries.toList()
    }
}
