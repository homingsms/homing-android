package com.homingsms.app.core.designsystem.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Color

enum class StatusTone { Neutral, Info, Brand, Success, Warning, Danger }

@Immutable
data class ToneColors(val content: Color, val container: Color)

@Composable
@ReadOnlyComposable
fun StatusTone.colors(): ToneColors {
    val c = HomingTheme.colors
    return when (this) {
        StatusTone.Neutral -> ToneColors(c.mutedForeground, c.muted)
        StatusTone.Info -> ToneColors(c.info, c.infoSoft)
        StatusTone.Brand -> ToneColors(c.brand, c.brandSoft)
        StatusTone.Success -> ToneColors(c.success, c.successSoft)
        StatusTone.Warning -> ToneColors(c.warning, c.warningSoft)
        StatusTone.Danger -> ToneColors(c.danger, c.dangerSoft)
    }
}

const val CAP_WARNING_PERCENT = 80
private const val PERCENT = 100

fun usageTone(used: Int, cap: Int?): StatusTone = when {
    cap == null || cap <= 0 -> StatusTone.Brand
    used >= cap -> StatusTone.Danger
    used * PERCENT >= cap * CAP_WARNING_PERCENT -> StatusTone.Warning
    else -> StatusTone.Brand
}
