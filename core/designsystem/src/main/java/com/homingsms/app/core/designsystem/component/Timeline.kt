package com.homingsms.app.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.homingsms.app.core.designsystem.theme.HomingTheme
import com.homingsms.app.core.designsystem.theme.StatusTone
import com.homingsms.app.core.designsystem.theme.colors

@Composable
fun HomingTimeline(entries: List<TimelineEntry>, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        entries.forEachIndexed { index, entry ->
            TimelineRow(entry = entry, isLast = index == entries.lastIndex)
        }
    }
}

@Composable
private fun TimelineRow(entry: TimelineEntry, isLast: Boolean) {
    val colors = HomingTheme.colors
    val toneColors = entry.tone.colors()
    Row(
        modifier = Modifier
            .height(IntrinsicSize.Min)
            .semantics(mergeDescendants = true) {}
    ) {
        Column(
            modifier = Modifier
                .width(20.dp)
                .fillMaxHeight(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(5.dp))
            val dot = Modifier.size(10.dp)
            if (entry.pending) {
                Box(dot.border(2.dp, colors.borderStrong, CircleShape))
            } else {
                Box(dot.background(toneColors.content, CircleShape))
            }
            if (!isLast) {
                Box(
                    Modifier
                        .padding(top = 4.dp)
                        .width(2.dp)
                        .weight(1f)
                        .background(colors.border)
                )
            }
        }
        Spacer(Modifier.width(HomingTheme.spacing.md))
        Column(Modifier.padding(bottom = if (isLast) 0.dp else HomingTheme.spacing.lg)) {
            Text(
                text = entry.title,
                style = MaterialTheme.typography.titleSmall,
                color = if (entry.pending) colors.mutedForeground else colors.foreground
            )
            if (entry.time != null) {
                Text(text = entry.time, style = MaterialTheme.typography.bodySmall, color = colors.mutedForeground)
            }
            if (entry.detail != null) {
                Spacer(Modifier.height(2.dp))
                Text(
                    text = entry.detail,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (entry.tone == StatusTone.Danger) colors.danger else colors.foreground
                )
            }
        }
    }
}
