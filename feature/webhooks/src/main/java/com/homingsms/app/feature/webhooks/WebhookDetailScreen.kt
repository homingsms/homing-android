package com.homingsms.app.feature.webhooks

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.homingsms.app.core.designsystem.component.CenteredContent
import com.homingsms.app.core.designsystem.component.CodeBlock
import com.homingsms.app.core.designsystem.component.ConfirmationDialog
import com.homingsms.app.core.designsystem.component.ErrorState
import com.homingsms.app.core.designsystem.component.HomingBottomSheet
import com.homingsms.app.core.designsystem.component.HomingButton
import com.homingsms.app.core.designsystem.component.HomingButtonStyle
import com.homingsms.app.core.designsystem.component.HomingDivider
import com.homingsms.app.core.designsystem.component.HomingListRow
import com.homingsms.app.core.designsystem.component.HomingPanel
import com.homingsms.app.core.designsystem.component.HomingScaffold
import com.homingsms.app.core.designsystem.component.HomingTopAppBar
import com.homingsms.app.core.designsystem.component.ListAppendFooter
import com.homingsms.app.core.designsystem.component.LoadingState
import com.homingsms.app.core.designsystem.component.SecretField
import com.homingsms.app.core.designsystem.component.SectionHeader
import com.homingsms.app.core.designsystem.component.StatusBadge
import com.homingsms.app.core.designsystem.icon.HomingIcons
import com.homingsms.app.core.designsystem.theme.HomingTheme
import com.homingsms.app.core.designsystem.theme.StatusTone
import com.homingsms.app.core.designsystem.util.formatCount
import com.homingsms.app.core.designsystem.util.formatDateTime
import com.homingsms.app.core.designsystem.util.formatFullDateTime
import kotlinx.coroutines.launch

private const val SECRET_PREFIX_LENGTH = 6

@Composable
fun WebhookDetailScreen(state: WebhookDetailUiState, actions: WebhookDetailActions, modifier: Modifier = Modifier) {
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val copied = stringResource(com.homingsms.app.core.designsystem.R.string.homing_action_copied)
    WebhookNoticeEffect(state.notice, snackbar, actions.onNoticeShown)
    HomingScaffold(
        modifier = modifier,
        snackbarHostState = snackbar,
        topBar = {
            HomingTopAppBar(
                title = stringResource(R.string.webhooks_detail_title),
                onBack = actions.onBack,
                actions = {
                    if (state.endpoint != null) {
                        IconButton(onClick = actions.onEdit) {
                            Icon(HomingIcons.Edit, contentDescription = stringResource(R.string.webhooks_edit))
                        }
                    }
                }
            )
        }
    ) { padding ->
        CenteredContent(Modifier.padding(padding)) {
            val endpoint = state.endpoint
            when {
                endpoint != null -> DetailContent(state, endpoint, actions) { scope.launch { snackbar.showSnackbar(copied) } }

                state.loadState == WebhooksLoadState.Error -> ErrorState(
                    icon = HomingIcons.CloudOff,
                    title = stringResource(R.string.webhooks_error_title),
                    onRetry = actions.onRetryLoad
                )

                else -> LoadingState(rows = 3)
            }
        }
    }
}

@Composable
private fun WebhookNoticeEffect(notice: WebhookNotice?, snackbar: SnackbarHostState, onShown: () -> Unit) {
    val text = when (notice) {
        WebhookNotice.SecretRotated -> stringResource(R.string.webhooks_notice_rotated)
        WebhookNotice.RetryQueued -> stringResource(R.string.webhooks_notice_retry)
        WebhookNotice.ActionFailed -> stringResource(R.string.webhooks_notice_failed)
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
private fun DetailContent(state: WebhookDetailUiState, endpoint: WebhookEndpointUi, actions: WebhookDetailActions, onCopied: () -> Unit) {
    var openDeliveryId by rememberSaveable { mutableStateOf<String?>(null) }
    var confirmRotate by rememberSaveable { mutableStateOf(false) }
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(HomingTheme.spacing.gutter),
        verticalArrangement = Arrangement.spacedBy(HomingTheme.spacing.lg)
    ) {
        item(key = "endpoint") { EndpointSummary(endpoint) }
        item(key = "secret") { SecretPanel(state, onRotate = { confirmRotate = true }, onCopied = onCopied) }
        item(key = "test") { TestPanel(state.testState, actions.onSendTest) }
        item(key = "deliveries-header") { SectionHeader(title = stringResource(R.string.webhooks_deliveries_title)) }
        if (state.deliveries.isEmpty()) {
            item(key = "deliveries-empty") {
                Text(
                    text = stringResource(R.string.webhooks_deliveries_empty),
                    style = MaterialTheme.typography.bodyMedium,
                    color = HomingTheme.colors.mutedForeground
                )
            }
        } else {
            item(key = "deliveries") {
                HomingPanel(contentPadding = PaddingValues()) {
                    state.deliveries.forEachIndexed { index, delivery ->
                        if (index > 0) HomingDivider(inset = true)
                        DeliveryRow(delivery, onClick = { openDeliveryId = delivery.id })
                    }
                }
            }
            item(key = "deliveries-footer") {
                ListAppendFooter(state = state.deliveriesAppendState, onRetry = actions.onLoadMoreDeliveries)
            }
        }
    }
    state.deliveries.firstOrNull { it.id == openDeliveryId }?.let { delivery ->
        DeliverySheet(
            delivery = delivery,
            onRetry = { actions.onRetryDelivery(delivery.id) },
            onDismiss = { openDeliveryId = null },
            onCopied = onCopied
        )
    }
    if (confirmRotate) {
        ConfirmationDialog(
            title = stringResource(R.string.webhooks_rotate_title),
            message = stringResource(R.string.webhooks_rotate_body),
            confirmLabel = stringResource(R.string.webhooks_rotate_confirm),
            destructive = true,
            onConfirm = {
                confirmRotate = false
                actions.onRotateSecret()
            },
            onDismiss = { confirmRotate = false }
        )
    }
}

@Composable
private fun EndpointSummary(endpoint: WebhookEndpointUi) {
    Column(verticalArrangement = Arrangement.spacedBy(HomingTheme.spacing.sm)) {
        Text(text = endpoint.url, style = HomingTheme.text.mono)
        StatusBadge(
            label = stringResource(if (endpoint.enabled) R.string.webhooks_enabled else R.string.webhooks_disabled),
            tone = if (endpoint.enabled) StatusTone.Success else StatusTone.Neutral
        )
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(HomingTheme.spacing.xs),
            verticalArrangement = Arrangement.spacedBy(HomingTheme.spacing.xs)
        ) {
            WebhookEvent.entries.filter { it in endpoint.events }.forEach { event ->
                StatusBadge(label = event.title(), tone = StatusTone.Brand)
            }
        }
    }
}

@Composable
private fun SecretPanel(state: WebhookDetailUiState, onRotate: () -> Unit, onCopied: () -> Unit) {
    HomingPanel(verticalArrangement = Arrangement.spacedBy(HomingTheme.spacing.md)) {
        val secret = state.signingSecret
        if (secret != null) {
            SecretField(
                label = stringResource(R.string.webhooks_secret_label),
                secret = secret,
                visiblePrefixLength = SECRET_PREFIX_LENGTH,
                onShowCopiedConfirmation = onCopied
            )
        }
        Text(
            text = stringResource(R.string.webhooks_secret_help),
            style = MaterialTheme.typography.bodySmall,
            color = HomingTheme.colors.mutedForeground
        )
        HomingButton(
            text = stringResource(R.string.webhooks_rotate),
            onClick = onRotate,
            style = HomingButtonStyle.Outline,
            loading = state.isRotatingSecret
        )
    }
}

@Composable
private fun TestPanel(testState: TestEventState, onSendTest: () -> Unit) {
    HomingPanel(verticalArrangement = Arrangement.spacedBy(HomingTheme.spacing.md)) {
        Text(text = stringResource(R.string.webhooks_test_help), style = MaterialTheme.typography.bodyMedium)
        Row(horizontalArrangement = Arrangement.spacedBy(HomingTheme.spacing.md)) {
            HomingButton(
                text = stringResource(R.string.webhooks_send_test),
                onClick = onSendTest,
                icon = HomingIcons.Send,
                style = HomingButtonStyle.Secondary,
                loading = testState == TestEventState.Sending
            )
        }
        when (testState) {
            is TestEventState.Delivered -> StatusBadge(
                label = stringResource(R.string.webhooks_test_delivered, testState.statusCode.toString()),
                tone = StatusTone.Success
            )

            is TestEventState.Failed -> StatusBadge(
                label = testState.statusCode?.let { stringResource(R.string.webhooks_test_failed_code, it.toString()) }
                    ?: stringResource(R.string.webhooks_test_failed),
                tone = StatusTone.Danger
            )

            else -> Unit
        }
    }
}

@Composable
private fun DeliveryRow(delivery: DeliveryUi, onClick: () -> Unit) {
    HomingListRow(
        headline = delivery.eventType,
        headlineStyle = HomingTheme.text.mono,
        supporting = listOfNotNull(
            delivery.statusCode?.let { stringResource(R.string.webhooks_http_status, it.toString()) },
            stringResource(R.string.webhooks_attempt, formatCount(delivery.attempt)),
            formatDateTime(delivery.atMillis)
        ).joinToString(" · "),
        trailing = { StatusBadge(label = delivery.status.label(), tone = delivery.status.tone()) },
        onClick = onClick,
        onClickLabel = stringResource(R.string.webhooks_open_delivery)
    )
}

@Composable
private fun DeliverySheet(delivery: DeliveryUi, onRetry: () -> Unit, onDismiss: () -> Unit, onCopied: () -> Unit) {
    HomingBottomSheet(onDismissRequest = onDismiss, title = delivery.eventType) {
        Row(horizontalArrangement = Arrangement.spacedBy(HomingTheme.spacing.sm)) {
            StatusBadge(label = delivery.status.label(), tone = delivery.status.tone())
            delivery.statusCode?.let {
                Text(
                    text = stringResource(R.string.webhooks_http_status, it.toString()),
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
        Text(
            text = listOfNotNull(
                stringResource(R.string.webhooks_attempt, formatCount(delivery.attempt)),
                formatFullDateTime(delivery.atMillis),
                delivery.durationMillis?.let { stringResource(R.string.webhooks_duration_ms, formatCount(it)) }
            ).joinToString(" · "),
            style = MaterialTheme.typography.bodySmall,
            color = HomingTheme.colors.mutedForeground
        )
        delivery.errorMessage?.let { Text(text = it, style = MaterialTheme.typography.bodyMedium, color = HomingTheme.colors.danger) }
        delivery.nextRetryAtMillis?.let {
            Text(
                text = stringResource(R.string.webhooks_next_retry, formatFullDateTime(it)),
                style = MaterialTheme.typography.bodyMedium
            )
        }
        Text(text = stringResource(R.string.webhooks_response_body), style = MaterialTheme.typography.labelMedium)
        CodeBlock(
            code = delivery.responseBody ?: stringResource(R.string.webhooks_empty_body_text),
            copyLabel = stringResource(R.string.webhooks_response_body),
            onShowCopiedConfirmation = onCopied
        )
        delivery.requestBody?.let {
            Text(text = stringResource(R.string.webhooks_request_body), style = MaterialTheme.typography.labelMedium)
            CodeBlock(code = it, copyLabel = stringResource(R.string.webhooks_request_body), onShowCopiedConfirmation = onCopied)
        }
        if (delivery.status == DeliveryStatus.Failed || delivery.status == DeliveryStatus.Retrying) {
            HomingButton(
                text = stringResource(R.string.webhooks_retry_now),
                onClick = onRetry,
                loading = delivery.isRetrying,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}
