package com.homingsms.app.core.designsystem.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.homingsms.app.core.designsystem.R
import com.homingsms.app.core.designsystem.theme.HomingTheme
import com.homingsms.app.core.designsystem.theme.StatusTone

@Composable
fun StatusHero(
    health: GatewayHealth,
    modifier: Modifier = Modifier,
    reason: String? = null,
    fixLabel: String? = null,
    onFix: (() -> Unit)? = null,
    detail: (@Composable ColumnScope.() -> Unit)? = null
) {
    val colors = HomingTheme.colors
    val container = when (health) {
        GatewayHealth.Online -> colors.surface
        GatewayHealth.Degraded -> colors.warningSoft
        GatewayHealth.Offline -> colors.muted
    }
    HomingPanel(
        modifier = modifier
            .fillMaxWidth()
            .semantics { liveRegion = LiveRegionMode.Polite },
        color = container,
        verticalArrangement = Arrangement.spacedBy(HomingTheme.spacing.md)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            StatusDot(tone = health.tone(), size = 10.dp)
            Spacer(Modifier.width(HomingTheme.spacing.md))
            Text(
                text = health.label(),
                style = MaterialTheme.typography.headlineSmall,
                color = colors.foreground,
                modifier = Modifier.semantics { heading() }
            )
        }
        if (reason != null) {
            Text(text = reason, style = MaterialTheme.typography.bodyMedium, color = colors.foreground)
        }
        detail?.invoke(this)
        if (fixLabel != null && onFix != null) {
            HomingButton(
                text = fixLabel,
                onClick = onFix,
                style = if (health == GatewayHealth.Online) HomingButtonStyle.Outline else HomingButtonStyle.Primary
            )
        }
    }
}
