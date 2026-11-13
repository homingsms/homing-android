package com.homingsms.app.feature.home

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
class HomeScreenshotTest(private val variant: ScreenshotVariant) {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun online() = composeRule.captureScreen("home_online", variant) { HomeScreen(HomeSamples.online, HomeActions()) }

    @Test
    fun degradedSmsLimit() = composeRule.captureScreen("home_degraded_sms_limit", variant) {
        HomeScreen(HomeSamples.smsLimit, HomeActions())
    }

    @Test
    fun offline() = composeRule.captureScreen("home_offline", variant) { HomeScreen(HomeSamples.offline, HomeActions()) }

    @Test
    fun loading() = composeRule.captureScreen("home_loading", variant) { HomeScreen(HomeSamples.loading, HomeActions()) }

    companion object {
        @JvmStatic
        @ParameterizedRobolectricTestRunner.Parameters(name = "{0}")
        fun variants() = ScreenshotVariant.entries.toList()
    }
}
