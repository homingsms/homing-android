package com.homingsms.app.core.designsystem.component

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.homingsms.app.core.designsystem.R
import com.homingsms.app.core.designsystem.theme.HomingTheme
import com.homingsms.app.core.designsystem.util.formatCount

@Composable
fun HourlyActivityChart(bars: List<HourlyBar>, modifier: Modifier = Modifier, chartHeight: Dp = 88.dp) {
    val colors = HomingTheme.colors
    val total = bars.sumOf { it.total }
    if (total == 0) {
        Text(
            text = stringResource(R.string.homing_chart_empty),
            style = MaterialTheme.typography.bodyMedium,
            color = colors.mutedForeground,
            modifier = modifier
        )
        return
    }
    val description = stringResource(
        R.string.homing_chart_description,
        formatCount(bars.sumOf { it.delivered }),
        formatCount(bars.sumOf { it.sentWithoutReport }),
        formatCount(bars.sumOf { it.failed })
    )
    val rtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    val max = bars.maxOf { it.total }.coerceAtLeast(1)
    Column(
        modifier = modifier.clearAndSetSemantics { contentDescription = description },
        verticalArrangement = Arrangement.spacedBy(HomingTheme.spacing.sm)
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(chartHeight)
        ) {
            drawBars(bars, max, rtl, ChartColors(colors.success, colors.chartNeutral, colors.danger, colors.muted))
        }
        Row(Modifier.fillMaxWidth()) {
            Text(
                text = stringResource(R.string.homing_chart_start),
                style = MaterialTheme.typography.bodySmall,
                color = colors.mutedForeground,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = stringResource(R.string.homing_chart_end),
                style = MaterialTheme.typography.bodySmall,
                color = colors.mutedForeground
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(HomingTheme.spacing.lg)) {
            LegendItem(color = colors.success, label = stringResource(R.string.homing_chart_delivered))
            LegendItem(color = colors.chartNeutral, label = stringResource(R.string.homing_chart_sent))
            LegendItem(color = colors.danger, label = stringResource(R.string.homing_chart_failed))
        }
    }
}

private class ChartColors(val delivered: Color, val sent: Color, val failed: Color, val track: Color)

private fun DrawScope.drawBars(bars: List<HourlyBar>, max: Int, rtl: Boolean, palette: ChartColors) {
    val gap = 3.dp.toPx()
    val barWidth = ((size.width - gap * (bars.size - 1)) / bars.size).coerceAtLeast(1f)
    val radius = CornerRadius(2.dp.toPx())
    bars.forEachIndexed { index, bar ->
        val position = if (rtl) bars.lastIndex - index else index
        val x = position * (barWidth + gap)
        if (bar.total == 0) {
            drawRoundRect(palette.track, Offset(x, size.height - 2.dp.toPx()), Size(barWidth, 2.dp.toPx()), radius)
            return@forEachIndexed
        }
        var bottom = size.height
        val segments = listOf(bar.delivered to palette.delivered, bar.sentWithoutReport to palette.sent, bar.failed to palette.failed)
        segments.forEach { (count, color) ->
            if (count > 0) {
                val h = size.height * count / max
                drawRoundRect(color, Offset(x, bottom - h), Size(barWidth, h), radius)
                bottom -= h
            }
        }
    }
}

@Composable
private fun LegendItem(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier
                .size(8.dp)
                .background(color, RoundedCornerShape(2.dp))
        )
        Spacer(Modifier.width(6.dp))
        Text(text = label, style = MaterialTheme.typography.bodySmall, color = HomingTheme.colors.mutedForeground)
    }
}
