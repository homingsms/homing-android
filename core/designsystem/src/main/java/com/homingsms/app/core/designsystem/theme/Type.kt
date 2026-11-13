package com.homingsms.app.core.designsystem.theme

import androidx.compose.material3.Typography
import androidx.compose.runtime.Immutable
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.homingsms.app.core.designsystem.R

private const val TABULAR = "tnum"

@OptIn(ExperimentalTextApi::class)
val AspektaFontFamily = FontFamily(
    Font(
        resId = R.font.aspekta,
        weight = FontWeight.Normal,
        variationSettings = FontVariation.Settings(FontVariation.weight(400))
    ),
    Font(
        resId = R.font.aspekta,
        weight = FontWeight.Medium,
        variationSettings = FontVariation.Settings(FontVariation.weight(500))
    ),
    Font(
        resId = R.font.aspekta,
        weight = FontWeight.SemiBold,
        variationSettings = FontVariation.Settings(FontVariation.weight(600))
    )
)

val JetBrainsMonoFontFamily = FontFamily(
    Font(R.font.jetbrains_mono_regular, FontWeight.Normal),
    Font(R.font.jetbrains_mono_medium, FontWeight.Medium)
)

private fun aspekta(size: Int, lineHeight: Int, weight: FontWeight, tracking: Double = 0.0, tabular: Boolean = false) = TextStyle(
    fontFamily = AspektaFontFamily,
    fontWeight = weight,
    fontSize = size.sp,
    lineHeight = lineHeight.sp,
    letterSpacing = tracking.em,
    fontFeatureSettings = if (tabular) TABULAR else null
)

val HomingTypography = Typography(
    displayLarge = aspekta(36, 44, FontWeight.Medium, -0.02, tabular = true),
    displayMedium = aspekta(32, 40, FontWeight.Medium, -0.02, tabular = true),
    displaySmall = aspekta(30, 36, FontWeight.Medium, -0.02, tabular = true),
    headlineLarge = aspekta(28, 36, FontWeight.Medium, -0.02),
    headlineMedium = aspekta(26, 32, FontWeight.Medium, -0.02),
    headlineSmall = aspekta(24, 32, FontWeight.Medium, -0.02),
    titleLarge = aspekta(20, 28, FontWeight.Medium, -0.01),
    titleMedium = aspekta(15, 22, FontWeight.Medium),
    titleSmall = aspekta(14, 20, FontWeight.Medium),
    bodyLarge = aspekta(16, 24, FontWeight.Normal),
    bodyMedium = aspekta(14, 20, FontWeight.Normal),
    bodySmall = aspekta(12, 16, FontWeight.Normal),
    labelLarge = aspekta(14, 20, FontWeight.Medium),
    labelMedium = aspekta(13, 18, FontWeight.Medium),
    labelSmall = aspekta(12, 16, FontWeight.Medium)
)

@Immutable
data class HomingTextStyles(
    val heroNumber: TextStyle,
    val number: TextStyle,
    val mono: TextStyle,
    val monoSmall: TextStyle,
    val wordmark: TextStyle
)

val DefaultHomingTextStyles = HomingTextStyles(
    heroNumber = aspekta(30, 36, FontWeight.Medium, -0.02, tabular = true),
    number = aspekta(20, 28, FontWeight.Medium, -0.01, tabular = true),
    mono = TextStyle(
        fontFamily = JetBrainsMonoFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 13.sp,
        lineHeight = 20.sp,
        textDirection = TextDirection.Ltr
    ),
    monoSmall = TextStyle(
        fontFamily = JetBrainsMonoFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        textDirection = TextDirection.Ltr
    ),
    wordmark = aspekta(17, 24, FontWeight.SemiBold, -0.02)
)
