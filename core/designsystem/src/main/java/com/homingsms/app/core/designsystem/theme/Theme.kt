package com.homingsms.app.core.designsystem.theme

import android.content.Context
import android.os.Build
import android.provider.Settings
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalContext

val LocalHomingColors = staticCompositionLocalOf { LightHomingColors }
val LocalHomingTextStyles = staticCompositionLocalOf { DefaultHomingTextStyles }
val LocalReducedMotion = staticCompositionLocalOf { false }
val LocalHomingClock = staticCompositionLocalOf<() -> Long> { { System.currentTimeMillis() } }

@Composable
fun HomingTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    reducedMotion: Boolean? = null,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val colors = if (darkTheme) DarkHomingColors else LightHomingColors
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ->
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)

        else -> colors.toColorScheme()
    }
    val systemReducedMotion = remember(context) { context.animationsDisabled() }
    CompositionLocalProvider(
        LocalHomingColors provides colors,
        LocalHomingTextStyles provides DefaultHomingTextStyles,
        LocalReducedMotion provides (reducedMotion ?: systemReducedMotion)
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = HomingTypography,
            shapes = HomingShapes
        ) {
            CompositionLocalProvider(LocalContentColor provides colorScheme.onBackground, content = content)
        }
    }
}

object HomingTheme {
    val colors: HomingColors
        @Composable
        @ReadOnlyComposable
        get() = LocalHomingColors.current

    val text: HomingTextStyles
        @Composable
        @ReadOnlyComposable
        get() = LocalHomingTextStyles.current

    val reducedMotion: Boolean
        @Composable
        @ReadOnlyComposable
        get() = LocalReducedMotion.current

    val spacing = HomingSpacing
    val radius = HomingRadius
}

private fun Context.animationsDisabled(): Boolean = runCatching {
    Settings.Global.getFloat(contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f
}.getOrDefault(false)
