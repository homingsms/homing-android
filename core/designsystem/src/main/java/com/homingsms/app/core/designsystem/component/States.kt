package com.homingsms.app.core.designsystem.component

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.homingsms.app.core.designsystem.R
import com.homingsms.app.core.designsystem.theme.HomingMotion
import com.homingsms.app.core.designsystem.theme.HomingTheme

@Composable
fun EmptyState(
    icon: ImageVector,
    title: String,
    modifier: Modifier = Modifier,
    body: String? = null,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null
) {
    MessageState(
        icon = icon,
        title = title,
        body = body,
        actionLabel = actionLabel,
        onAction = onAction,
        modifier = modifier
    )
}

@Composable
fun ErrorState(icon: ImageVector, title: String, onRetry: () -> Unit, modifier: Modifier = Modifier, body: String? = null) {
    MessageState(
        icon = icon,
        title = title,
        body = body,
        actionLabel = stringResource(R.string.homing_action_retry),
        onAction = onRetry,
        modifier = modifier
    )
}

@Composable
private fun MessageState(
    icon: ImageVector,
    title: String,
    body: String?,
    actionLabel: String?,
    onAction: (() -> Unit)?,
    modifier: Modifier
) {
    val colors = HomingTheme.colors
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = HomingTheme.spacing.xxl, vertical = HomingTheme.spacing.huge),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(HomingTheme.spacing.md)
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .background(colors.muted, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(imageVector = icon, contentDescription = null, tint = colors.mutedForeground)
        }
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = colors.foreground,
            textAlign = TextAlign.Center,
            modifier = Modifier.semantics { heading() }
        )
        if (body != null) {
            Text(
                text = body,
                style = MaterialTheme.typography.bodyMedium,
                color = colors.mutedForeground,
                textAlign = TextAlign.Center,
                modifier = Modifier.widthIn(max = 360.dp)
            )
        }
        if (actionLabel != null && onAction != null) {
            HomingButton(text = actionLabel, onClick = onAction, style = HomingButtonStyle.Outline)
        }
    }
}

@Composable
fun LoadingState(modifier: Modifier = Modifier, rows: Int = 6) {
    val description = stringResource(R.string.homing_loading)
    val alpha = skeletonAlpha()
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clearAndSetSemantics {
                contentDescription = description
                progressBarRangeInfo = ProgressBarRangeInfo.Indeterminate
            }
            .alpha(alpha)
    ) {
        repeat(rows) { SkeletonRow() }
    }
}

@Composable
private fun SkeletonRow() {
    val fill = HomingTheme.colors.muted
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = HomingTheme.spacing.gutter, vertical = HomingTheme.spacing.md),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier
                .size(36.dp)
                .background(fill, CircleShape)
        )
        Spacer(Modifier.width(HomingTheme.spacing.lg))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(HomingTheme.spacing.sm)) {
            Box(
                Modifier
                    .fillMaxWidth(0.5f)
                    .height(12.dp)
                    .background(fill, MaterialTheme.shapes.extraSmall)
            )
            Box(
                Modifier
                    .fillMaxWidth(0.8f)
                    .height(10.dp)
                    .background(fill, MaterialTheme.shapes.extraSmall)
            )
        }
    }
}

@Composable
private fun skeletonAlpha(): Float {
    if (HomingTheme.reducedMotion) return 1f
    val transition = rememberInfiniteTransition(label = "skeleton")
    val alpha = transition.animateFloat(
        initialValue = 1f,
        targetValue = 0.55f,
        animationSpec = infiniteRepeatable(
            animation = tween(HomingMotion.PULSE_MILLIS / 2, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "skeletonAlpha"
    )
    return alpha.value
}

@Composable
fun ListAppendFooter(state: AppendState, onRetry: () -> Unit, modifier: Modifier = Modifier) {
    when (state) {
        AppendState.Loading -> Box(
            modifier = modifier
                .fillMaxWidth()
                .heightIn(min = 64.dp),
            contentAlignment = Alignment.Center
        ) {
            val description = stringResource(R.string.homing_loading)
            CircularProgressIndicator(
                modifier = Modifier
                    .size(24.dp)
                    .semantics { contentDescription = description },
                strokeWidth = 2.dp,
                color = HomingTheme.colors.brand
            )
        }

        AppendState.Error -> Row(
            modifier = modifier
                .fillMaxWidth()
                .padding(horizontal = HomingTheme.spacing.gutter, vertical = HomingTheme.spacing.sm),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(R.string.homing_list_load_more_failed),
                style = MaterialTheme.typography.bodyMedium,
                color = HomingTheme.colors.mutedForeground,
                modifier = Modifier.weight(1f)
            )
            HomingButton(
                text = stringResource(R.string.homing_action_retry),
                onClick = onRetry,
                style = HomingButtonStyle.Text
            )
        }

        AppendState.Idle, AppendState.EndReached -> Spacer(modifier.height(HomingTheme.spacing.lg))
    }
}
