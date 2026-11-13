package com.homingsms.app.core.designsystem.component

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.homingsms.app.core.designsystem.R
import com.homingsms.app.core.designsystem.theme.HomingTheme
import com.homingsms.app.core.designsystem.theme.StatusTone
import com.homingsms.app.core.designsystem.util.formatCount

@Composable
fun StatTile(label: String, value: Long?, modifier: Modifier = Modifier, tone: StatusTone? = null) {
    Column(modifier = modifier.semantics(mergeDescendants = true) {}) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (tone != null) {
                StatusDot(tone = tone, size = 8.dp)
                Spacer(Modifier.width(6.dp))
            }
            Text(text = label, style = MaterialTheme.typography.labelMedium, color = HomingTheme.colors.mutedForeground)
        }
        Spacer(Modifier.height(2.dp))
        if (value != null) {
            Text(text = formatCount(value), style = HomingTheme.text.number, color = HomingTheme.colors.foreground)
        } else {
            Text(
                text = stringResource(R.string.homing_value_unavailable),
                style = MaterialTheme.typography.bodyMedium,
                color = HomingTheme.colors.mutedForeground
            )
        }
    }
}
