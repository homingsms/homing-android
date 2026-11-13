package com.homingsms.app.feature.inbox

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.material.icons.outlined.FilterListOff
import androidx.compose.material.icons.outlined.Inbox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.homingsms.app.core.designsystem.component.AppendState
import com.homingsms.app.core.designsystem.component.CenteredContent
import com.homingsms.app.core.designsystem.component.EmptyState
import com.homingsms.app.core.designsystem.component.ErrorState
import com.homingsms.app.core.designsystem.component.FilterChipRow
import com.homingsms.app.core.designsystem.component.FilterMenuChip
import com.homingsms.app.core.designsystem.component.FilterOption
import com.homingsms.app.core.designsystem.component.HomingButton
import com.homingsms.app.core.designsystem.component.HomingButtonStyle
import com.homingsms.app.core.designsystem.component.HomingDivider
import com.homingsms.app.core.designsystem.component.HomingFilterChip
import com.homingsms.app.core.designsystem.component.HomingListRow
import com.homingsms.app.core.designsystem.component.HomingScaffold
import com.homingsms.app.core.designsystem.component.HomingSearchField
import com.homingsms.app.core.designsystem.component.HomingTopAppBar
import com.homingsms.app.core.designsystem.component.ListAppendFooter
import com.homingsms.app.core.designsystem.component.LoadingState
import com.homingsms.app.core.designsystem.component.StatusBadge
import com.homingsms.app.core.designsystem.theme.HomingTheme
import com.homingsms.app.core.designsystem.theme.StatusTone
import com.homingsms.app.core.designsystem.util.formatCount
import com.homingsms.app.core.designsystem.util.formatDateTime
import com.homingsms.app.core.designsystem.util.simLabel
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter

private const val LOAD_MORE_THRESHOLD = 5

@Composable
fun InboxScreen(state: InboxUiState, actions: InboxActions, modifier: Modifier = Modifier) {
    HomingScaffold(
        modifier = modifier,
        topBar = { HomingTopAppBar(title = stringResource(R.string.inbox_title)) }
    ) { padding ->
        CenteredContent(Modifier.padding(padding)) {
            Column(Modifier.fillMaxSize()) {
                HomingSearchField(
                    query = state.query,
                    onQueryChange = actions.onQueryChange,
                    label = stringResource(R.string.inbox_search),
                    modifier = Modifier.padding(horizontal = HomingTheme.spacing.gutter)
                )
                InboxFilterChips(state, actions)
                InboxBody(state, actions)
            }
        }
    }
}

@Composable
private fun InboxFilterChips(state: InboxUiState, actions: InboxActions) {
    FilterChipRow(contentPadding = PaddingValues(horizontal = HomingTheme.spacing.gutter, vertical = HomingTheme.spacing.sm)) {
        InboxKind.entries.forEach { kind ->
            HomingFilterChip(
                label = kind.label(),
                selected = kind in state.filters.kinds,
                onClick = { actions.onToggleKind(kind) }
            )
        }
        if (state.availableSims.size > 1) {
            FilterMenuChip(
                label = stringResource(R.string.inbox_filter_sim),
                options = state.availableSims.map { FilterOption(it.slot, simLabel(it.slot)) },
                selected = state.filters.simSlots,
                onToggle = actions.onToggleSim
            )
        }
        if (state.filters.isActive) {
            HomingButton(
                text = stringResource(R.string.inbox_filter_clear),
                onClick = actions.onClearFilters,
                style = HomingButtonStyle.Text
            )
        }
    }
}

@Composable
private fun InboxBody(state: InboxUiState, actions: InboxActions) {
    when {
        state.loadState == InboxLoadState.Loading && state.items.isEmpty() -> LoadingState()

        state.loadState == InboxLoadState.Error && state.items.isEmpty() -> ErrorState(
            icon = Icons.Outlined.CloudOff,
            title = stringResource(R.string.inbox_error_title),
            body = stringResource(R.string.inbox_error_body),
            onRetry = actions.onRetryLoad
        )

        state.items.isEmpty() && (state.filters.isActive || state.query.isNotBlank()) -> EmptyState(
            icon = Icons.Outlined.FilterListOff,
            title = stringResource(R.string.inbox_no_matches_title),
            body = stringResource(R.string.inbox_no_matches_body),
            actionLabel = stringResource(R.string.inbox_filter_clear),
            onAction = {
                actions.onQueryChange("")
                actions.onClearFilters()
            }
        )

        state.items.isEmpty() -> EmptyState(
            icon = Icons.Outlined.Inbox,
            title = stringResource(R.string.inbox_empty_title),
            body = stringResource(R.string.inbox_empty_body)
        )

        else -> InboxList(state, actions)
    }
}

@Composable
private fun InboxList(state: InboxUiState, actions: InboxActions) {
    val listState = rememberLazyListState()
    InboxLoadMoreEffect(listState, state, actions.onLoadMore)
    LazyColumn(state = listState, modifier = Modifier.fillMaxSize()) {
        items(state.items, key = { it.id }) { item ->
            InboxRow(item = item, showSim = state.availableSims.size > 1, onClick = { actions.onItemClick(item.id) })
            HomingDivider(inset = true)
        }
        item(key = "footer") { ListAppendFooter(state = state.appendState, onRetry = actions.onLoadMore) }
    }
}

@Composable
private fun InboxLoadMoreEffect(listState: LazyListState, state: InboxUiState, onLoadMore: () -> Unit) {
    val latestState by rememberUpdatedState(state)
    val latestOnLoadMore by rememberUpdatedState(onLoadMore)
    LaunchedEffect(listState) {
        snapshotFlow { listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0 }
            .distinctUntilChanged()
            .filter { last -> last >= latestState.items.size - LOAD_MORE_THRESHOLD && latestState.appendState == AppendState.Idle }
            .collect { latestOnLoadMore() }
    }
}

@Composable
private fun InboxRow(item: InboxListItem, showSim: Boolean, onClick: () -> Unit) {
    val preview = when {
        item.kind == InboxKind.Data -> item.dataPort?.let { stringResource(R.string.inbox_data_port, it.toString()) }
            ?: item.kind.label()

        item.kind == InboxKind.Mms && item.preview.isNullOrBlank() ->
            pluralStringResource(R.plurals.inbox_attachments, item.attachmentCount, formatCount(item.attachmentCount))

        else -> item.preview.orEmpty()
    }
    val meta = buildList {
        add(formatDateTime(item.receivedAtMillis))
        if (showSim && item.simSlot != null) add(simLabel(item.simSlot))
        if (item.kind != InboxKind.Sms) add(item.kind.label())
    }.joinToString(" · ")
    HomingListRow(
        headline = item.sender,
        headlineStyle = HomingTheme.text.mono,
        overline = meta,
        supporting = preview,
        supportingMaxLines = 2,
        onClick = onClick,
        onClickLabel = stringResource(R.string.inbox_open_detail),
        trailing = when {
            item.optOutKeyword != null -> {
                { StatusBadge(label = stringResource(R.string.inbox_opt_out), tone = StatusTone.Warning) }
            }

            item.kind == InboxKind.Mms && item.attachmentCount > 0 -> {
                { AttachmentThumb(thumbnail = item.thumbnail, isImage = item.thumbnail != null, size = 44.dp) }
            }

            else -> null
        }
    )
}

@Composable
internal fun InboxKind.label(): String = stringResource(
    when (this) {
        InboxKind.Sms -> R.string.inbox_kind_sms
        InboxKind.Data -> R.string.inbox_kind_data
        InboxKind.Mms -> R.string.inbox_kind_mms
    }
)
