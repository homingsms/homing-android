package com.homingsms.app.feature.connect

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
class ConnectScreenshotTest(private val variant: ScreenshotVariant) {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun connected() = composeRule.captureScreen("connect_connected", variant) { ConnectScreen(ConnectSamples.connected, ConnectActions()) }

    @Test
    fun notPaired() = composeRule.captureScreen("connect_not_paired", variant) { ConnectScreen(ConnectSamples.notPaired, ConnectActions()) }

    @Test
    fun scannerPermission() = composeRule.captureScreen("connect_scanner_permission", variant) {
        QrScannerContent(permission = CameraPermissionState.Unknown, onRequestPermission = {}, onOpenSettings = {}, onClose = {})
    }

    companion object {
        @JvmStatic
        @ParameterizedRobolectricTestRunner.Parameters(name = "{0}")
        fun variants() = ScreenshotVariant.entries.toList()
    }
}
