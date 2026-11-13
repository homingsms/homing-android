package com.homingsms.app.feature.webhooks

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.homingsms.app.core.designsystem.component.CenteredContent
import com.homingsms.app.core.designsystem.component.EmptyState
import com.homingsms.app.core.designsystem.component.ErrorState
import com.homingsms.app.core.designsystem.component.HomingDivider
import com.homingsms.app.core.designsystem.component.HomingListRow
import com.homingsms.app.core.designsystem.component.HomingPanel
import com.homingsms.app.core.designsystem.component.HomingScaffold
import com.homingsms.app.core.designsystem.component.HomingTopAppBar
import com.homingsms.app.core.designsystem.component.LoadingState
import com.homingsms.app.core.designsystem.component.StatusBadge
import com.homingsms.app.core.designsystem.icon.HomingIcons
import com.homingsms.app.core.designsystem.theme.HomingTheme
import com.homingsms.app.core.designsystem.util.formatCount
import com.homingsms.app.core.designsystem.util.formatRelativeTime

@Composable
fun WebhooksScreen(state: WebhooksUiState, actions: WebhooksActions, modifier: Modifier = Modifier) {
    HomingScaffold(
        modifier = modifier,
        topBar = { HomingTopAppBar(title = stringResource(R.string.webhooks_title), onBack = actions.onBack) },
        floatingActionButton = {
            if (state.endpoints.isNotEmpty()) {
                ExtendedFloatingActionButton(
                    onClick = actions.onAdd,
                    icon = { Icon(HomingIcons.Add, contentDescription = null) },
                    text = { Text(stringResource(R.string.webhooks_add)) },
                    containerColor = HomingTheme.colors.brand,
                    contentColor = HomingTheme.colors.brandForeground,
                    shape = MaterialTheme.shapes.medium
                )
            }
        }
    ) { padding ->
        CenteredContent(Modifier.padding(padding)) {
            when {
                state.loadState == WebhooksLoadState.Loading && state.endpoints.isEmpty() -> LoadingState(rows = 3)

                state.loadState == WebhooksLoadState.Error && state.endpoints.isEmpty() -> ErrorState(
                    icon = HomingIcons.CloudOff,
                    title = stringResource(R.string.webhooks_error_title),
                    onRetry = actions.onRetryLoad
                )

                state.endpoints.isEmpty() -> EmptyState(
                    icon = HomingIcons.Webhook,
                    title = stringResource(R.string.webhooks_empty_title),
                    body = stringResource(R.string.webhooks_empty_body),
                    actionLabel = stringResource(R.string.webhooks_add),
                    onAction = actions.onAdd
                )

                else -> EndpointList(state, actions)
            }
        }
    }
}

@Composable
private fun EndpointList(state: WebhooksUiState, actions: WebhooksActions) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(HomingTheme.spacing.md)
    ) {
        items(state.endpoints, key = { it.id }) { endpoint ->
            EndpointCard(endpoint, actions)
        }
    }
}

@Composable
private fun EndpointCard(endpoint: WebhookEndpointUi, actions: WebhooksActions) {
    HomingPanel(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium),
        contentPadding = PaddingValues()
    ) {
        HomingListRow(
            headline = endpoint.url,
            headlineStyle = HomingTheme.text.mono,
            supporting = pluralStringResource(R.plurals.webhooks_event_count, endpoint.events.size, formatCount(endpoint.events.size)),
            onClick = { actions.onEndpointClick(endpoint.id) },
            onClickLabel = stringResource(R.string.webhooks_open_endpoint)
        )
        HomingDivider()
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .toggleable(
                    value = endpoint.enabled,
                    role = Role.Switch,
                    onValueChange = { actions.onEnabledChange(endpoint.id, it) }
                )
                .padding(horizontal = HomingTheme.spacing.gutter, vertical = HomingTheme.spacing.sm),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    text = stringResource(if (endpoint.enabled) R.string.webhooks_enabled else R.string.webhooks_disabled),
                    style = MaterialTheme.typography.titleSmall
                )
                LastDeliveryLine(endpoint.lastDelivery)
            }
            Spacer(Modifier.width(HomingTheme.spacing.md))
            Switch(checked = endpoint.enabled, onCheckedChange = null)
        }
    }
}

@Composable
private fun LastDeliveryLine(summary: DeliverySummary?) {
    if (summary == null) {
        Text(
            text = stringResource(R.string.webhooks_no_deliveries_yet),
            style = MaterialTheme.typography.bodySmall,
            color = HomingTheme.colors.mutedForeground
        )
        return
    }
    Row(verticalAlignment = Alignment.CenterVertically) {
        StatusBadge(label = summary.status.label(), tone = summary.status.tone())
        Spacer(Modifier.width(HomingTheme.spacing.sm))
        Text(
            text = listOfNotNull(summary.statusCode?.toString(), formatRelativeTime(summary.atMillis)).joinToString(" · "),
            style = MaterialTheme.typography.bodySmall,
            color = HomingTheme.colors.mutedForeground,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
