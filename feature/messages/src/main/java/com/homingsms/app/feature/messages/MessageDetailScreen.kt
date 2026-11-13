package com.homingsms.app.feature.messages

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import com.homingsms.app.core.designsystem.component.CenteredContent
import com.homingsms.app.core.designsystem.component.ConfirmationDialog
import com.homingsms.app.core.designsystem.component.CopyableValue
import com.homingsms.app.core.designsystem.component.ErrorState
import com.homingsms.app.core.designsystem.component.HomingButton
import com.homingsms.app.core.designsystem.component.HomingButtonStyle
import com.homingsms.app.core.designsystem.component.HomingPanel
import com.homingsms.app.core.designsystem.component.HomingScaffold
import com.homingsms.app.core.designsystem.component.HomingTimeline
import com.homingsms.app.core.designsystem.component.HomingTopAppBar
import com.homingsms.app.core.designsystem.component.LoadingState
import com.homingsms.app.core.designsystem.component.SectionHeader
import com.homingsms.app.core.designsystem.component.StatusBadge
import com.homingsms.app.core.designsystem.component.TimelineEntry
import com.homingsms.app.core.designsystem.icon.HomingIcons
import com.homingsms.app.core.designsystem.theme.HomingTheme
import com.homingsms.app.core.designsystem.theme.StatusTone
import com.homingsms.app.core.designsystem.util.formatBytes
import com.homingsms.app.core.designsystem.util.formatCount
import com.homingsms.app.core.designsystem.util.formatFullDateTime
import com.homingsms.app.core.designsystem.util.simLabel
import kotlinx.coroutines.launch

@Composable
fun MessageDetailScreen(state: MessageDetailUiState, actions: MessageDetailActions, modifier: Modifier = Modifier) {
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val copied = stringResource(com.homingsms.app.core.designsystem.R.string.homing_action_copied)
    NoticeEffect(state.notice, snackbar, actions.onNoticeShown)
    HomingScaffold(
        modifier = modifier,
        snackbarHostState = snackbar,
        topBar = { HomingTopAppBar(title = stringResource(R.string.messages_detail_title), onBack = actions.onBack) }
    ) { padding ->
        CenteredContent(Modifier.padding(padding)) {
            val message = state.message
            when {
                message != null -> MessageDetailContent(
                    state = state,
                    message = message,
                    actions = actions,
                    onShowCopiedConfirmation = { scope.launch { snackbar.showSnackbar(copied) } }
                )

                state.loadState == LoadState.Error -> ErrorState(
                    icon = HomingIcons.CloudOff,
                    title = stringResource(R.string.messages_detail_error),
                    onRetry = actions.onRetryLoad
                )

                else -> LoadingState(rows = 3)
            }
        }
    }
}

@Composable
private fun NoticeEffect(notice: MessageNotice?, snackbar: SnackbarHostState, onShown: () -> Unit) {
    val text = when (notice) {
        MessageNotice.RetryQueued -> stringResource(R.string.messages_notice_retry)
        MessageNotice.Cancelled -> stringResource(R.string.messages_notice_cancelled)
        MessageNotice.ActionFailed -> stringResource(R.string.messages_notice_failed)
        null -> null
    }
    LaunchedEffect(notice) {
        if (text != null) {
            snackbar.showSnackbar(text)
            onShown()
        }
    }
}

@Composable
private fun MessageDetailContent(
    state: MessageDetailUiState,
    message: MessageDetail,
    actions: MessageDetailActions,
    onShowCopiedConfirmation: () -> Unit
) {
    var confirmCancel by rememberSaveable { mutableStateOf(false) }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(HomingTheme.spacing.gutter),
        verticalArrangement = Arrangement.spacedBy(HomingTheme.spacing.lg)
    ) {
        Summary(message)
        if (message.error != null) CarrierError(message.error)
        DetailActions(state, actions, onCancelClick = { confirmCancel = true })
        MessageBody(message)
        HomingPanel {
            SectionHeader(title = stringResource(R.string.messages_timeline))
            Spacer(Modifier.padding(top = HomingTheme.spacing.xs))
            HomingTimeline(entries = timelineEntries(message))
        }
        Properties(message, onShowCopiedConfirmation)
    }
    if (confirmCancel) {
        ConfirmationDialog(
            title = stringResource(R.string.messages_cancel_title),
            message = stringResource(R.string.messages_cancel_body),
            confirmLabel = stringResource(R.string.messages_cancel_confirm),
            destructive = true,
            onConfirm = {
                confirmCancel = false
                actions.onCancel()
            },
            onDismiss = { confirmCancel = false }
        )
    }
}

@Composable
private fun Summary(message: MessageDetail) {
    Column(verticalArrangement = Arrangement.spacedBy(HomingTheme.spacing.sm)) {
        Text(text = message.recipient, style = HomingTheme.text.mono.copy(fontSize = MaterialTheme.typography.titleLarge.fontSize))
        Row(verticalAlignment = Alignment.CenterVertically) {
            StatusBadge(label = message.state.label(), tone = message.state.tone())
            Spacer(Modifier.width(HomingTheme.spacing.sm))
            Text(
                text = listOfNotNull(
                    message.type.label(),
                    message.simSlot?.let { simLabel(it) },
                    if (message.priority == MessagePriority.Otp) stringResource(R.string.messages_priority_otp) else null
                ).joinToString(" · "),
                style = MaterialTheme.typography.bodyMedium,
                color = HomingTheme.colors.mutedForeground
            )
        }
    }
}

@Composable
private fun CarrierError(error: MessageError) {
    HomingPanel(color = HomingTheme.colors.dangerSoft, borderColor = HomingTheme.colors.dangerSoft) {
        Text(
            text = stringResource(R.string.messages_error_heading),
            style = MaterialTheme.typography.titleSmall,
            color = HomingTheme.colors.danger
        )
        Text(text = error.message, style = MaterialTheme.typography.bodyMedium, color = HomingTheme.colors.foreground)
        Text(text = error.code, style = HomingTheme.text.monoSmall, color = HomingTheme.colors.danger)
    }
}

@Composable
private fun DetailActions(state: MessageDetailUiState, actions: MessageDetailActions, onCancelClick: () -> Unit) {
    if (!state.canRetry && !state.canCancel) return
    Row(horizontalArrangement = Arrangement.spacedBy(HomingTheme.spacing.sm)) {
        if (state.canRetry) {
            HomingButton(
                text = stringResource(R.string.messages_retry),
                onClick = actions.onRetry,
                icon = HomingIcons.Refresh,
                loading = state.isRetrying
            )
        }
        if (state.canCancel) {
            HomingButton(
                text = stringResource(R.string.messages_cancel),
                onClick = onCancelClick,
                style = HomingButtonStyle.Outline,
                loading = state.isCancelling
            )
        }
    }
}

@Composable
private fun MessageBody(message: MessageDetail) {
    val hasBody = message.text != null || message.subject != null || message.dataBase64 != null || message.attachments.isNotEmpty()
    if (!hasBody) return
    HomingPanel(verticalArrangement = Arrangement.spacedBy(HomingTheme.spacing.sm)) {
        SectionHeader(title = stringResource(R.string.messages_content))
        message.subject?.let { Text(text = it, style = MaterialTheme.typography.titleSmall) }
        message.text?.let { Text(text = it, style = MaterialTheme.typography.bodyLarge) }
        message.dataBase64?.let {
            Text(
                text = stringResource(R.string.messages_data_port, message.dataPort?.toString().orEmpty()),
                style = MaterialTheme.typography.labelMedium,
                color = HomingTheme.colors.mutedForeground
            )
            Text(text = it, style = HomingTheme.text.monoSmall)
        }
        message.attachments.forEach { attachment ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(HomingIcons.Attachment, contentDescription = null, tint = HomingTheme.colors.mutedForeground)
                Spacer(Modifier.width(HomingTheme.spacing.sm))
                Text(
                    text = stringResource(
                        R.string.messages_attachment_line,
                        attachment.name ?: attachment.contentType,
                        formatBytes(attachment.sizeBytes)
                    ),
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}

@Composable
private fun timelineEntries(message: MessageDetail): List<TimelineEntry> {
    val entries = message.events.map { event ->
        TimelineEntry(
            title = event.state.label(),
            time = formatFullDateTime(event.atMillis),
            tone = event.state.tone(),
            detail = event.detail
        )
    }
    val awaitingReport = message.state == MessageState.Sent && message.deliveryReport
    val inFlight = message.state in setOf(MessageState.Pending, MessageState.Leased, MessageState.Queued, MessageState.Sending)
    return when {
        awaitingReport -> entries + TimelineEntry(
            title = stringResource(R.string.messages_awaiting_report),
            time = null,
            tone = StatusTone.Neutral,
            pending = true
        )

        inFlight -> entries + TimelineEntry(
            title = MessageState.Sent.label(),
            time = null,
            tone = StatusTone.Neutral,
            pending = true
        )

        else -> entries
    }
}

@Composable
private fun Properties(message: MessageDetail, onShowCopiedConfirmation: () -> Unit) {
    HomingPanel(verticalArrangement = Arrangement.spacedBy(HomingTheme.spacing.md)) {
        SectionHeader(title = stringResource(R.string.messages_details))
        CopyableValue(
            label = stringResource(R.string.messages_id),
            value = message.id,
            onShowCopiedConfirmation = onShowCopiedConfirmation
        )
        message.idempotencyKey?.let {
            CopyableValue(
                label = stringResource(R.string.messages_idempotency_key),
                value = it,
                onShowCopiedConfirmation = onShowCopiedConfirmation
            )
        }
        Property(stringResource(R.string.messages_created), formatFullDateTime(message.createdAtMillis))
        message.scheduleAtMillis?.let { Property(stringResource(R.string.messages_scheduled_for), formatFullDateTime(it)) }
        message.validUntilMillis?.let { Property(stringResource(R.string.messages_valid_until), formatFullDateTime(it)) }
        Property(
            stringResource(R.string.messages_source),
            stringResource(if (message.source == MessageSource.Local) R.string.messages_source_local else R.string.messages_source_cloud)
        )
        Property(stringResource(R.string.messages_parts), formatCount(message.partsCount))
        Property(stringResource(R.string.messages_attempts), formatCount(message.attempts))
        Property(
            stringResource(R.string.messages_delivery_report),
            stringResource(if (message.deliveryReport) R.string.messages_yes else R.string.messages_no)
        )
    }
}

@Composable
private fun Property(label: String, value: String) {
    Row(Modifier.fillMaxWidth()) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = HomingTheme.colors.mutedForeground,
            modifier = Modifier.weight(1f)
        )
        Spacer(Modifier.width(HomingTheme.spacing.md))
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = HomingTheme.colors.foreground,
            textAlign = TextAlign.End,
            modifier = Modifier.weight(1f)
        )
    }
}
