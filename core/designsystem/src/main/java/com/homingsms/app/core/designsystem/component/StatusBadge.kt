package com.homingsms.app.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.homingsms.app.core.designsystem.theme.HomingTheme
import com.homingsms.app.core.designsystem.theme.StatusTone
import com.homingsms.app.core.designsystem.theme.colors

@Composable
fun StatusBadge(label: String, tone: StatusTone, modifier: Modifier = Modifier) {
    val toneColors = tone.colors()
    Row(
        modifier = modifier
            .background(toneColors.container, RoundedCornerShape(HomingTheme.radius.sm))
            .padding(horizontal = 8.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        StatusDot(tone = tone, size = 6.dp)
        Spacer(Modifier.width(6.dp))
        Text(text = label, style = MaterialTheme.typography.labelSmall, color = toneColors.content, maxLines = 1)
    }
}

@Composable
fun StatusDot(tone: StatusTone, modifier: Modifier = Modifier, size: Dp = 8.dp) {
    Box(
        modifier = modifier
            .size(size)
            .background(tone.colors().content, CircleShape)
    )
}
