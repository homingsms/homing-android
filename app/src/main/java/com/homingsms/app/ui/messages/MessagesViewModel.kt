package com.homingsms.app.ui.messages

import androidx.lifecycle.viewModelScope
import com.homingsms.app.feature.messages.LoadState
import com.homingsms.app.feature.messages.MessageFilters
import com.homingsms.app.feature.messages.MessageListItem
import com.homingsms.app.feature.messages.MessagePriority
import com.homingsms.app.feature.messages.MessagesActions
import com.homingsms.app.feature.messages.MessagesUiState
import com.homingsms.app.feature.messages.SimOption
import com.homingsms.app.ui.CursorPager
import com.homingsms.app.ui.EffectViewModel
import com.homingsms.app.ui.InitialLoad
import com.homingsms.app.ui.UiMappers
import com.homingsms.app.ui.data.AppStatus
import com.homingsms.app.ui.data.MessageStore
import com.homingsms.app.ui.navigation.MessageDetailRoute
import com.homingsms.core.model.MessageFilter
import com.homingsms.core.model.MessagePriority as CorePriority
import com.homingsms.core.model.MessageRecord
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update

@OptIn(FlowPreview::class)
class MessagesViewModel(status: AppStatus, private val store: MessageStore) : EffectViewModel() {
    private val query = MutableStateFlow("")
    private val filters = MutableStateFlow(MessageFilters())
    private val pager = CursorPager(viewModelScope) { cursor, limit -> store.list(currentFilter(), cursor, limit) }

    val state: StateFlow<MessagesUiState> = combine(
        query,
        filters,
        status.sims.map { sims -> sims.map { SimOption(it.slot, it.carrier) } }.distinctUntilChanged(),
        pager.state
    ) { text, selected, sims, page ->
        MessagesUiState(
            query = text,
            filters = selected,
            availableSims = sims,
            items = page.items.map(::item),
            loadState = when (page.initial) {
                InitialLoad.Loading -> LoadState.Loading
                InitialLoad.Error -> LoadState.Error
                InitialLoad.Ready -> LoadState.Ready
            },
            appendState = page.append
        )
    }.stateIn(viewModelScope, SharingStarted.Eagerly, MessagesUiState())

    val actions = MessagesActions(
        onQueryChange = { query.value = it },
        onToggleState = { value -> filters.update { it.copy(states = it.states.toggle(value)) } },
        onToggleType = { value -> filters.update { it.copy(types = it.types.toggle(value)) } },
        onToggleSim = { value -> filters.update { it.copy(simSlots = it.simSlots.toggle(value)) } },
        onClearFilters = { filters.value = MessageFilters() },
        onMessageClick = { navigate(MessageDetailRoute(it)) },
        onLoadMore = pager::loadMore,
        onRetryLoad = { pager.refresh() }
    )

    init {
        pager.refresh()
        filters.drop(1).onEach { pager.refresh() }.launchIn(viewModelScope)
        query.drop(1).debounce(SEARCH_DEBOUNCE_MS).distinctUntilChanged().onEach { pager.refresh() }.launchIn(viewModelScope)
        store.changes.debounce(CHANGES_DEBOUNCE_MS).onEach { pager.refresh(keepLoaded = true) }.launchIn(viewModelScope)
    }

    private fun currentFilter(): MessageFilter {
        val selected = filters.value
        return MessageFilter(
            states = selected.states.map(UiMappers::messageState).toSet(),
            types = selected.types.map(UiMappers::messageType).toSet(),
            simSlots = selected.simSlots,
            search = query.value.trim().takeIf { it.isNotEmpty() }
        )
    }

    private companion object {
        const val SEARCH_DEBOUNCE_MS = 300L
        const val CHANGES_DEBOUNCE_MS = 500L

        fun item(record: MessageRecord) = MessageListItem(
            id = record.id,
            recipient = record.to,
            type = UiMappers.messageType(record.type),
            state = UiMappers.messageState(record.state),
            preview = record.text ?: record.subject,
            updatedAtMillis = record.updatedAt,
            simSlot = record.simSlot,
            priority = if (record.priority == CorePriority.Otp) MessagePriority.Otp else MessagePriority.Normal,
            attachmentCount = record.attachments.size,
            dataPort = record.port,
            errorCode = record.error?.code
        )
    }
}

fun <T> Set<T>.toggle(value: T): Set<T> = if (value in this) this - value else this + value
