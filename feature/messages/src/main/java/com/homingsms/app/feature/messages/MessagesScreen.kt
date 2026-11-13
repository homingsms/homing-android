package com.homingsms.app.feature.messages

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
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
import com.homingsms.app.core.designsystem.component.HomingListRow
import com.homingsms.app.core.designsystem.component.HomingScaffold
import com.homingsms.app.core.designsystem.component.HomingSearchField
import com.homingsms.app.core.designsystem.component.HomingTopAppBar
import com.homingsms.app.core.designsystem.component.ListAppendFooter
import com.homingsms.app.core.designsystem.component.LoadingState
import com.homingsms.app.core.designsystem.component.StatusBadge
import com.homingsms.app.core.designsystem.icon.HomingIcons
import com.homingsms.app.core.designsystem.theme.HomingTheme
import com.homingsms.app.core.designsystem.util.formatCount
import com.homingsms.app.core.designsystem.util.formatDateTime
import com.homingsms.app.core.designsystem.util.simLabel
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter

private const val LOAD_MORE_THRESHOLD = 5

@Composable
fun MessagesScreen(state: MessagesUiState, actions: MessagesActions, modifier: Modifier = Modifier) {
    HomingScaffold(
        modifier = modifier,
        topBar = { HomingTopAppBar(title = stringResource(R.string.messages_title)) }
    ) { padding ->
        CenteredContent(Modifier.padding(padding)) {
            Column(Modifier.fillMaxSize()) {
                HomingSearchField(
                    query = state.query,
                    onQueryChange = actions.onQueryChange,
                    label = stringResource(R.string.messages_search),
                    modifier = Modifier.padding(horizontal = HomingTheme.spacing.gutter)
                )
                MessageFilterChips(state, actions)
                MessagesBody(state, actions)
            }
        }
    }
}

@Composable
private fun MessageFilterChips(state: MessagesUiState, actions: MessagesActions) {
    FilterChipRow(contentPadding = PaddingValues(horizontal = HomingTheme.spacing.gutter, vertical = HomingTheme.spacing.sm)) {
        FilterMenuChip(
            label = stringResource(R.string.messages_filter_state),
            options = MessageState.entries.map { FilterOption(it, it.label()) },
            selected = state.filters.states,
            onToggle = actions.onToggleState
        )
        FilterMenuChip(
            label = stringResource(R.string.messages_filter_type),
            options = MessageType.entries.map { FilterOption(it, it.label()) },
            selected = state.filters.types,
            onToggle = actions.onToggleType
        )
        if (state.availableSims.size > 1) {
            FilterMenuChip(
                label = stringResource(R.string.messages_filter_sim),
                options = state.availableSims.map { sim ->
                    val label = simLabel(sim.slot)
                    FilterOption(sim.slot, sim.carrier?.let { stringResource(R.string.messages_sim_option, label, it) } ?: label)
                },
                selected = state.filters.simSlots,
                onToggle = actions.onToggleSim
            )
        }
        if (state.filters.isActive) {
            HomingButton(
                text = stringResource(R.string.messages_filter_clear),
                onClick = actions.onClearFilters,
                style = HomingButtonStyle.Text
            )
        }
    }
}

@Composable
private fun MessagesBody(state: MessagesUiState, actions: MessagesActions) {
    when {
        state.loadState == LoadState.Loading && state.items.isEmpty() -> LoadingState()

        state.loadState == LoadState.Error && state.items.isEmpty() -> ErrorState(
            icon = HomingIcons.CloudOff,
            title = stringResource(R.string.messages_error_title),
            body = stringResource(R.string.messages_error_body),
            onRetry = actions.onRetryLoad
        )

        state.items.isEmpty() && (state.filters.isActive || state.query.isNotBlank()) -> EmptyState(
            icon = HomingIcons.FilterOff,
            title = stringResource(R.string.messages_no_matches_title),
            body = stringResource(R.string.messages_no_matches_body),
            actionLabel = stringResource(R.string.messages_filter_clear),
            onAction = {
                actions.onQueryChange("")
                actions.onClearFilters()
            }
        )

        state.items.isEmpty() -> EmptyState(
            icon = HomingIcons.Messages,
            title = stringResource(R.string.messages_empty_title),
            body = stringResource(R.string.messages_empty_body)
        )

        else -> MessageList(state, actions)
    }
}

@Composable
private fun MessageList(state: MessagesUiState, actions: MessagesActions) {
    val listState = rememberLazyListState()
    LoadMoreEffect(listState, state, actions.onLoadMore)
    LazyColumn(state = listState, modifier = Modifier.fillMaxSize()) {
        items(state.items, key = { it.id }) { item ->
            MessageRow(item = item, showSim = state.availableSims.size > 1, onClick = { actions.onMessageClick(item.id) })
            HomingDivider(inset = true)
        }
        item(key = "footer") { ListAppendFooter(state = state.appendState, onRetry = actions.onLoadMore) }
    }
}

@Composable
private fun LoadMoreEffect(listState: LazyListState, state: MessagesUiState, onLoadMore: () -> Unit) {
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
internal fun MessageRow(item: MessageListItem, showSim: Boolean, onClick: () -> Unit) {
    val preview = when {
        item.type == MessageType.Mms && item.preview.isNullOrBlank() ->
            pluralStringResource(R.plurals.messages_attachments, item.attachmentCount, formatCount(item.attachmentCount))

        item.type == MessageType.Data -> item.dataPort?.let { stringResource(R.string.messages_data_port, it.toString()) }
            ?: item.type.label()

        else -> item.preview.orEmpty()
    }
    val meta = buildList {
        add(formatDateTime(item.updatedAtMillis))
        if (showSim && item.simSlot != null) add(simLabel(item.simSlot))
        if (item.priority == MessagePriority.Otp) add(stringResource(R.string.messages_priority_otp))
        if (item.type != MessageType.Sms) add(item.type.label())
    }.joinToString(separator = " · ")
    HomingListRow(
        headline = item.recipient,
        headlineStyle = HomingTheme.text.mono,
        overline = meta,
        supporting = preview,
        supportingMaxLines = 1,
        onClick = onClick,
        onClickLabel = stringResource(R.string.messages_open_detail),
        trailing = { StatusBadge(label = item.state.label(), tone = item.state.tone()) }
    )
}
