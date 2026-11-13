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

const val SCREENSHOT_DIR = "src/test/screenshots"

@Composable
fun HomingTestContent(
    variant: ScreenshotVariant = ScreenshotVariant.Light,
    nowMillis: Long = PREVIEW_NOW_MILLIS,
    content: @Composable () -> Unit
) {
    WithLocale(variant.localeTag) {
        WithFontScale(variant.fontScale) {
            HomingTheme(darkTheme = variant.dark, reducedMotion = true) {
                CompositionLocalProvider(LocalHomingClock provides { nowMillis }) {
                    Box(
                        Modifier
                            .fillMaxSize()
                            .background(HomingTheme.colors.background)
                    ) { content() }
                }
            }
        }
    }
}

@Composable
private fun WithLocale(localeTag: String?, content: @Composable () -> Unit) {
    if (localeTag == null) {
        content()
        return
    }
    val baseContext = LocalContext.current
    val baseConfiguration = LocalConfiguration.current
    val localized = remember(localeTag, baseConfiguration) {
        val locale = Locale.forLanguageTag(localeTag)
        val configuration = Configuration(baseConfiguration).apply {
            setLocale(locale)
            setLayoutDirection(locale)
        }
        configuration to baseContext.createConfigurationContext(configuration)
    }
    val direction = if (localized.first.layoutDirection == android.view.View.LAYOUT_DIRECTION_RTL) {
        LayoutDirection.Rtl
    } else {
        LayoutDirection.Ltr
    }
    CompositionLocalProvider(
        LocalConfiguration provides localized.first,
        LocalContext provides localized.second,
        LocalResources provides localized.second.resources,
        LocalLayoutDirection provides direction,
        content = content
    )
}

@Composable
private fun WithFontScale(fontScale: Float?, content: @Composable () -> Unit) {
    if (fontScale == null) {
        content()
        return
    }
    val density = LocalDensity.current
    CompositionLocalProvider(LocalDensity provides Density(density.density, fontScale), content = content)
}

fun ComposeContentTestRule.captureScreen(
    name: String,
    variant: ScreenshotVariant,
    nowMillis: Long = PREVIEW_NOW_MILLIS,
    content: @Composable () -> Unit
) {
    TimeZone.setDefault(TimeZone.getTimeZone("UTC"))
    setContent { HomingTestContent(variant = variant, nowMillis = nowMillis, content = content) }
    waitForIdle()
    onRoot().captureRoboImage("$SCREENSHOT_DIR/${name}_${variant.fileSuffix}.png")
}
