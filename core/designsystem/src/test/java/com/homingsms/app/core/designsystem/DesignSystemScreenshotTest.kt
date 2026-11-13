package com.homingsms.app.core.designsystem

import androidx.compose.ui.test.junit4.createComposeRule
import com.homingsms.app.core.designsystem.component.HomingTopAppBar
import com.homingsms.app.core.designsystem.icon.HomingIcons
import com.homingsms.app.core.designsystem.preview.DesignSystemCatalog
import com.homingsms.app.core.designsystem.shell.HomingAppShell
import com.homingsms.app.core.designsystem.shell.ShellDestination
import com.homingsms.app.core.testing.ScreenshotVariant
import com.homingsms.app.core.testing.captureScreen
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.ParameterizedRobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(ParameterizedRobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class DesignSystemScreenshotTest(private val variant: ScreenshotVariant) {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun catalog() = composeRule.captureScreen("designsystem_catalog", variant) {
        DesignSystemCatalog()
    }

    @Test
    fun shellPhone() = composeRule.captureScreen("designsystem_shell_phone", variant) {
        Shell()
    }

    @Test
    @Config(qualifiers = "w840dp-h900dp-xhdpi")
    fun shellTablet() = composeRule.captureScreen("designsystem_shell_tablet", variant) {
        Shell()
    }

    @androidx.compose.runtime.Composable
    private fun Shell() {
        HomingAppShell(
            destinations = listOf(
                ShellDestination("home", "Home", HomingIcons.Home),
                ShellDestination("settings", "Settings", HomingIcons.Settings, badgeCount = 2)
            ),
            selectedKey = "home",
            onDestinationSelected = {}
        ) {
            HomingTopAppBar(title = "Home")
        }
    }

    companion object {
        @JvmStatic
        @ParameterizedRobolectricTestRunner.Parameters(name = "{0}")
        fun variants() = ScreenshotVariant.entries.toList()
    }
}
