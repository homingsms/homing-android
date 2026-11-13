package com.homingsms.app.core.designsystem.theme

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

object HomingSpacing {
    val xxs = 4.dp
    val xs = 6.dp
    val sm = 8.dp
    val md = 12.dp
    val lg = 16.dp
    val xl = 20.dp
    val xxl = 24.dp
    val xxxl = 32.dp
    val huge = 40.dp
    val gutter = 16.dp
    val panelPadding = 16.dp
    val minTouchTarget = 48.dp
    val contentMaxWidth = 840.dp
}

object HomingRadius {
    val sm = 6.dp
    val md = 8.dp
    val lg = 12.dp
    val xl = 16.dp
}

val HomingShapes = Shapes(
    extraSmall = RoundedCornerShape(HomingRadius.sm),
    small = RoundedCornerShape(HomingRadius.md),
    medium = RoundedCornerShape(HomingRadius.lg),
    large = RoundedCornerShape(HomingRadius.xl),
    extraLarge = RoundedCornerShape(HomingRadius.xl)
)

object HomingMotion {
    const val FAST_MILLIS = 150
    const val STANDARD_MILLIS = 160
    const val SHEET_MILLIS = 220
    const val PULSE_MILLIS = 1600
    val Easing: Easing = CubicBezierEasing(0.25f, 1f, 0.5f, 1f)
}
