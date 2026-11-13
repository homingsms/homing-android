package com.homingsms.app.feature.inbox

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.homingsms.app.core.designsystem.component.CenteredContent
import com.homingsms.app.core.designsystem.component.CopyableValue
import com.homingsms.app.core.designsystem.component.ErrorState
import com.homingsms.app.core.designsystem.component.HomingPanel
import com.homingsms.app.core.designsystem.component.HomingScaffold
import com.homingsms.app.core.designsystem.component.HomingTopAppBar
import com.homingsms.app.core.designsystem.component.LoadingState
import com.homingsms.app.core.designsystem.component.SectionHeader
import com.homingsms.app.core.designsystem.component.StatusBadge
import com.homingsms.app.core.designsystem.icon.HomingIcons
import com.homingsms.app.core.designsystem.theme.HomingTheme
import com.homingsms.app.core.designsystem.theme.StatusTone
import com.homingsms.app.core.designsystem.util.formatBytes
import com.homingsms.app.core.designsystem.util.formatFullDateTime
import com.homingsms.app.core.designsystem.util.simLabel
import kotlinx.coroutines.launch

@Composable
fun InboxDetailScreen(state: InboxDetailUiState, actions: InboxDetailActions, modifier: Modifier = Modifier) {
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val copied = stringResource(com.homingsms.app.core.designsystem.R.string.homing_action_copied)
    HomingScaffold(
        modifier = modifier,
        snackbarHostState = snackbar,
        topBar = { HomingTopAppBar(title = stringResource(R.string.inbox_detail_title), onBack = actions.onBack) }
    ) { padding ->
        CenteredContent(Modifier.padding(padding)) {
            val message = state.message
            when {
                message != null -> InboxDetailContent(message, actions) { scope.launch { snackbar.showSnackbar(copied) } }

                state.loadState == InboxLoadState.Error -> ErrorState(
                    icon = HomingIcons.CloudOff,
                    title = stringResource(R.string.inbox_detail_error),
                    onRetry = actions.onRetryLoad
                )

                else -> LoadingState(rows = 3)
            }
        }
    }
}

@Composable
private fun InboxDetailContent(message: InboxDetail, actions: InboxDetailActions, onCopied: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(HomingTheme.spacing.gutter),
        verticalArrangement = Arrangement.spacedBy(HomingTheme.spacing.lg)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(HomingTheme.spacing.sm)) {
            Text(text = message.sender, style = HomingTheme.text.mono.copy(fontSize = MaterialTheme.typography.titleLarge.fontSize))
            Text(
                text = listOfNotNull(
                    message.kind.label(),
                    message.simSlot?.let { simLabel(it) },
                    formatFullDateTime(message.receivedAtMillis)
                ).joinToString(" · "),
                style = MaterialTheme.typography.bodyMedium,
                color = HomingTheme.colors.mutedForeground
            )
            Row(horizontalArrangement = Arrangement.spacedBy(HomingTheme.spacing.sm)) {
                ForwardBadge(message.forwardStatus)
                if (message.optOutKeyword != null) {
                    StatusBadge(label = stringResource(R.string.inbox_opt_out_keyword, message.optOutKeyword), tone = StatusTone.Warning)
                }
            }
        }
        HomingPanel(verticalArrangement = Arrangement.spacedBy(HomingTheme.spacing.sm)) {
            message.subject?.let { Text(text = it, style = MaterialTheme.typography.titleSmall) }
            message.text?.let { Text(text = it, style = MaterialTheme.typography.bodyLarge) }
            message.dataBase64?.let {
                Text(
                    text = stringResource(R.string.inbox_data_port, message.dataPort?.toString().orEmpty()),
                    style = MaterialTheme.typography.labelMedium,
                    color = HomingTheme.colors.mutedForeground
                )
                CopyableValue(label = stringResource(R.string.inbox_data_label), value = it, onShowCopiedConfirmation = onCopied)
            }
        }
        if (message.attachments.isNotEmpty()) {
            HomingPanel(verticalArrangement = Arrangement.spacedBy(HomingTheme.spacing.md)) {
                SectionHeader(title = stringResource(R.string.inbox_attachments_title))
                message.attachments.forEach { attachment ->
                    AttachmentRow(attachment, actions)
                }
            }
        }
        CopyableValue(label = stringResource(R.string.inbox_id), value = message.id, onShowCopiedConfirmation = onCopied)
    }
}

@Composable
private fun ForwardBadge(status: ForwardStatus) {
    val (label, tone) = when (status) {
        ForwardStatus.Forwarded -> stringResource(R.string.inbox_forwarded) to StatusTone.Success
        ForwardStatus.Pending -> stringResource(R.string.inbox_forward_pending) to StatusTone.Neutral
        ForwardStatus.Failed -> stringResource(R.string.inbox_forward_failed) to StatusTone.Danger
        ForwardStatus.NotForwarded -> stringResource(R.string.inbox_not_forwarded) to StatusTone.Neutral
    }
    StatusBadge(label = label, tone = tone)
}

@Composable
private fun AttachmentRow(attachment: InboxAttachment, actions: InboxDetailActions) {
    val name = attachment.name ?: attachment.contentType
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .clickable(
                role = Role.Button,
                onClickLabel = stringResource(R.string.inbox_open_attachment),
                onClick = { actions.onOpenAttachment(attachment.id) }
            ),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AttachmentThumb(thumbnail = attachment.thumbnail, isImage = attachment.isImage, size = 72.dp)
        Spacer(Modifier.width(HomingTheme.spacing.md))
        Column(Modifier.weight(1f)) {
            Text(text = name, style = MaterialTheme.typography.titleSmall, maxLines = 2)
            Text(
                text = stringResource(R.string.inbox_attachment_meta, attachment.contentType, formatBytes(attachment.sizeBytes)),
                style = MaterialTheme.typography.bodySmall,
                color = HomingTheme.colors.mutedForeground
            )
        }
        IconButton(onClick = { actions.onShareAttachment(attachment.id) }) {
            Icon(
                imageVector = HomingIcons.Share,
                contentDescription = stringResource(R.string.inbox_share_attachment, name),
                tint = HomingTheme.colors.mutedForeground
            )
        }
    }
}
