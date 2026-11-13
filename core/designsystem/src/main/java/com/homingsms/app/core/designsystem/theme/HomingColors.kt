package com.homingsms.app.core.designsystem.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

@Immutable
data class HomingColors(
    val background: Color,
    val surface: Color,
    val muted: Color,
    val subtle: Color,
    val border: Color,
    val borderStrong: Color,
    val foreground: Color,
    val mutedForeground: Color,
    val brand: Color,
    val brandHover: Color,
    val brandForeground: Color,
    val brandSoft: Color,
    val success: Color,
    val successSoft: Color,
    val danger: Color,
    val dangerSoft: Color,
    val dangerHover: Color,
    val warning: Color,
    val warningSoft: Color,
    val info: Color,
    val infoSoft: Color,
    val chartNeutral: Color,
    val overlay: Color,
    val isDark: Boolean
)

val LightHomingColors = HomingColors(
    background = Color(0xFFF4F5F2),
    surface = Color(0xFFFFFFFF),
    muted = Color(0xFFEBEDE8),
    subtle = Color(0xFFE2E5DF),
    border = Color(0xFFD9DDD6),
    borderStrong = Color(0xFFC3C9C1),
    foreground = Color(0xFF141A18),
    mutedForeground = Color(0xFF58625D),
    brand = Color(0xFF0A6870),
    brandHover = Color(0xFF08575E),
    brandForeground = Color(0xFFFFFFFF),
    brandSoft = Color(0xFFDCEDEC),
    success = Color(0xFF1B7346),
    successSoft = Color(0xFFDEF0E4),
    danger = Color(0xFFB8322B),
    dangerSoft = Color(0xFFF8E2DF),
    dangerHover = Color(0xFF9C2A24),
    warning = Color(0xFF8C5A00),
    warningSoft = Color(0xFFF6EBD3),
    info = Color(0xFF2A5BBE),
    infoSoft = Color(0xFFE2E9F8),
    chartNeutral = Color(0xFFB9C0B8),
    overlay = Color(0x6B0C1110),
    isDark = false
)

val DarkHomingColors = HomingColors(
    background = Color(0xFF0C1110),
    surface = Color(0xFF131918),
    muted = Color(0xFF1A211F),
    subtle = Color(0xFF222A28),
    border = Color(0xFF263030),
    borderStrong = Color(0xFF34403E),
    foreground = Color(0xFFE8EDEA),
    mutedForeground = Color(0xFF94A19B),
    brand = Color(0xFF53BDC1),
    brandHover = Color(0xFF6FCBCE),
    brandForeground = Color(0xFF04201F),
    brandSoft = Color(0xFF11302F),
    success = Color(0xFF4FC28A),
    successSoft = Color(0xFF13291D),
    danger = Color(0xFFF2756A),
    dangerSoft = Color(0xFF341816),
    dangerHover = Color(0xFFF48B82),
    warning = Color(0xFFE3A642),
    warningSoft = Color(0xFF2E2311),
    info = Color(0xFF82A9F4),
    infoSoft = Color(0xFF17223A),
    chartNeutral = Color(0xFF3B4744),
    overlay = Color(0x99000000),
    isDark = true
)

private val DarkOnError = Color(0xFF2A0D0A)
private val LightOnError = Color(0xFFFFFFFF)

fun HomingColors.toColorScheme(): ColorScheme {
    val onError = if (isDark) DarkOnError else LightOnError
    val inversePrimary = if (isDark) LightHomingColors.brand else DarkHomingColors.brand
    val base = if (isDark) darkColorScheme() else lightColorScheme()
    return base.copy(
        primary = brand,
        onPrimary = brandForeground,
        primaryContainer = brandSoft,
        onPrimaryContainer = foreground,
        inversePrimary = inversePrimary,
        secondary = brand,
        onSecondary = brandForeground,
        secondaryContainer = brandSoft,
        onSecondaryContainer = foreground,
        tertiary = info,
        onTertiary = surface,
        tertiaryContainer = infoSoft,
        onTertiaryContainer = foreground,
        background = background,
        onBackground = foreground,
        surface = surface,
        onSurface = foreground,
        surfaceVariant = muted,
        onSurfaceVariant = mutedForeground,
        surfaceTint = surface,
        inverseSurface = foreground,
        inverseOnSurface = background,
        error = danger,
        onError = onError,
        errorContainer = dangerSoft,
        onErrorContainer = danger,
        outline = borderStrong,
        outlineVariant = border,
        scrim = overlay,
        surfaceBright = surface,
        surfaceDim = subtle,
        surfaceContainerLowest = surface,
        surfaceContainerLow = surface,
        surfaceContainer = surface,
        surfaceContainerHigh = surface,
        surfaceContainerHighest = muted
    )
}
