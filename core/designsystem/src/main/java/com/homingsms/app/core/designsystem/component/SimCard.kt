package com.homingsms.app.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.homingsms.app.core.designsystem.R
import com.homingsms.app.core.designsystem.theme.HomingTheme
import com.homingsms.app.core.designsystem.theme.StatusTone
import com.homingsms.app.core.designsystem.theme.colors
import com.homingsms.app.core.designsystem.theme.usageTone
import com.homingsms.app.core.designsystem.util.formatCount
import com.homingsms.app.core.designsystem.util.simLabel

private const val MAX_SIGNAL = 4

@Composable
fun SimCard(
    slot: Int,
    carrier: String?,
    phoneNumber: String?,
    signalLevel: Int?,
    enabled: Boolean,
    sentToday: Int,
    dailyCap: Int?,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null
) {
    val colors = HomingTheme.colors
    val clickModifier = if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier
    HomingPanel(
        modifier = modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .then(clickModifier),
        verticalArrangement = Arrangement.spacedBy(HomingTheme.spacing.sm)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(text = simLabel(slot), style = MaterialTheme.typography.titleMedium, color = colors.foreground)
                Text(
                    text = carrier ?: stringResource(R.string.homing_sim_no_carrier),
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.mutedForeground,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            SignalBars(level = signalLevel)
        }
        if (phoneNumber != null) {
            Text(text = phoneNumber, style = HomingTheme.text.mono, color = colors.foreground)
        }
        if (enabled) {
            SimUsage(sentToday = sentToday, dailyCap = dailyCap)
        } else {
            Text(
                text = stringResource(R.string.homing_sim_disabled),
                style = MaterialTheme.typography.bodyMedium,
                color = colors.mutedForeground
            )
        }
    }
}

@Composable
private fun SimUsage(sentToday: Int, dailyCap: Int?) {
    val tone = usageTone(sentToday, dailyCap)
    val usageText = if (dailyCap != null) {
        stringResource(R.string.homing_sim_usage, formatCount(sentToday), formatCount(dailyCap))
    } else {
        stringResource(R.string.homing_sim_usage_uncapped, formatCount(sentToday))
    }
    Text(text = usageText, style = MaterialTheme.typography.bodyMedium, color = HomingTheme.colors.foreground)
    if (dailyCap != null && dailyCap > 0) {
        UsageBar(fraction = sentToday.toFloat() / dailyCap, tone = tone)
        when (tone) {
            StatusTone.Danger -> Text(
                text = stringResource(R.string.homing_sim_at_cap),
                style = MaterialTheme.typography.bodySmall,
                color = HomingTheme.colors.danger
            )

            StatusTone.Warning -> Text(
                text = stringResource(R.string.homing_sim_near_cap),
                style = MaterialTheme.typography.bodySmall,
                color = HomingTheme.colors.warning
            )

            else -> Unit
        }
    }
}

@Composable
fun UsageBar(fraction: Float, tone: StatusTone, modifier: Modifier = Modifier) {
    val clamped = fraction.coerceIn(0f, 1f)
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(6.dp)
            .clip(RoundedCornerShape(3.dp))
            .background(HomingTheme.colors.muted)
            .semantics { progressBarRangeInfo = ProgressBarRangeInfo(clamped, 0f..1f) }
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(clamped)
                .fillMaxHeight()
                .clip(RoundedCornerShape(3.dp))
                .background(tone.colors().content)
        )
    }
}

@Composable
fun SignalBars(level: Int?, modifier: Modifier = Modifier) {
    val description = when {
        level == null -> stringResource(R.string.homing_sim_signal_unknown)
        level <= 0 -> stringResource(R.string.homing_sim_no_signal)
        else -> stringResource(R.string.homing_sim_signal, level.coerceAtMost(MAX_SIGNAL))
    }
    val colors = HomingTheme.colors
    val active = if (level != null && level <= 0) colors.danger else colors.foreground
    Row(
        modifier = modifier
            .height(16.dp)
            .semantics { contentDescription = description },
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalAlignment = Alignment.Bottom
    ) {
        for (bar in 1..MAX_SIGNAL) {
            val filled = level != null && bar <= level
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .fillMaxHeight(bar / MAX_SIGNAL.toFloat())
                    .clip(RoundedCornerShape(1.dp))
                    .background(if (filled) active else colors.borderStrong)
            )
        }
    }
}
