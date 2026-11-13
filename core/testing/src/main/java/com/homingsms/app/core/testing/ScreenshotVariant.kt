package com.homingsms.app.core.testing

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import com.github.takahirom.roborazzi.captureRoboImage
import com.homingsms.app.core.designsystem.preview.PREVIEW_NOW_MILLIS
import com.homingsms.app.core.designsystem.theme.HomingTheme
import com.homingsms.app.core.designsystem.theme.LocalHomingClock
import java.util.Locale
import java.util.TimeZone

enum class ScreenshotVariant(val fileSuffix: String, val dark: Boolean, val localeTag: String?, val fontScale: Float?) {
    Light("light", dark = false, localeTag = null, fontScale = null),
    Dark("dark", dark = true, localeTag = null, fontScale = null),
    ArabicRtl("ar_rtl", dark = false, localeTag = "ar", fontScale = null),
    FontScale200("font_200", dark = false, localeTag = null, fontScale = 2f)
}
