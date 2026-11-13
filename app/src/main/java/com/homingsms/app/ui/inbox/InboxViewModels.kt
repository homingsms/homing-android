package com.homingsms.app.ui.inbox

import androidx.lifecycle.viewModelScope
import com.homingsms.app.feature.inbox.AttachmentThumbnail
import com.homingsms.app.feature.inbox.ForwardStatus
import com.homingsms.app.feature.inbox.InboxActions
import com.homingsms.app.feature.inbox.InboxAttachment
import com.homingsms.app.feature.inbox.InboxDetail
import com.homingsms.app.feature.inbox.InboxDetailActions
import com.homingsms.app.feature.inbox.InboxDetailUiState
import com.homingsms.app.feature.inbox.InboxFilters
import com.homingsms.app.feature.inbox.InboxKind
import com.homingsms.app.feature.inbox.InboxListItem
import com.homingsms.app.feature.inbox.InboxLoadState
import com.homingsms.app.feature.inbox.InboxSim
import com.homingsms.app.feature.inbox.InboxUiState
import com.homingsms.app.ui.CursorPager
import com.homingsms.app.ui.EffectViewModel
import com.homingsms.app.ui.InitialLoad
import com.homingsms.app.ui.attempt
import com.homingsms.app.ui.data.AppStatus
import com.homingsms.app.ui.data.FileSharing
import com.homingsms.app.ui.data.InboxStore
import com.homingsms.app.ui.messages.toggle
import com.homingsms.app.ui.navigation.InboxDetailRoute
import com.homingsms.app.ui.system.SystemAction
import com.homingsms.core.model.AppSettings
import com.homingsms.core.model.CloudPairingState
import com.homingsms.core.model.InboxFilter
import com.homingsms.core.model.InboxRecord
import com.homingsms.core.model.MessageType
import com.homingsms.core.model.WebhookEventType
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

object InboxMapping {
    fun kind(type: MessageType): InboxKind = InboxKind.valueOf(type.name)

    fun type(kind: InboxKind): MessageType = MessageType.valueOf(kind.name)

    fun optOutKeyword(record: InboxRecord, keywords: List<String>): String? {
        if (record.type != MessageType.Sms) return null
        val text = record.text?.trim()?.uppercase() ?: return null
        return keywords.firstOrNull { it.uppercase() == text }
    }

    fun item(record: InboxRecord, keywords: List<String>) = InboxListItem(
        id = record.id,
        sender = record.from,
        kind = kind(record.type),
        preview = record.text ?: record.subject,
        receivedAtMillis = record.receivedAt,
        simSlot = record.simSlot,
        attachmentCount = record.attachments.size,
        dataPort = record.port,
        optOutKeyword = optOutKeyword(record, keywords)
    )

    fun forwardTargets(settings: AppSettings, paired: Boolean, webhookSubscribed: Boolean): Boolean {
        val cloud = settings.cloud.enabled && paired
        val webhooks = settings.localServer.enabled && webhookSubscribed
        return cloud || webhooks
    }
}

@OptIn(FlowPreview::class)
class InboxViewModel(private val status: AppStatus, private val store: InboxStore) : EffectViewModel() {
    private val query = MutableStateFlow("")
    private val filters = MutableStateFlow(InboxFilters())
    private val pager = CursorPager(viewModelScope) { cursor, limit -> store.list(currentFilter(), cursor, limit) }

    val state: StateFlow<InboxUiState> = combine(
        combine(query, filters) { text, selected -> text to selected },
        status.sims.map { sims -> sims.map { InboxSim(it.slot, it.carrier) } }.distinctUntilChanged(),
        status.settings.map { it.optOutKeywords }.distinctUntilChanged(),
        pager.state
    ) { (text, selected), sims, keywords, page ->
        InboxUiState(
            query = text,
            filters = selected,
            availableSims = sims,
            items = page.items.map { InboxMapping.item(it, keywords) },
            loadState = when (page.initial) {
                InitialLoad.Loading -> InboxLoadState.Loading
                InitialLoad.Error -> InboxLoadState.Error
                InitialLoad.Ready -> InboxLoadState.Ready
            },
            appendState = page.append
        )
    }.stateIn(viewModelScope, SharingStarted.Eagerly, InboxUiState())

    val actions = InboxActions(
        onQueryChange = { query.value = it },
        onToggleKind = { value -> filters.update { it.copy(kinds = it.kinds.toggle(value)) } },
        onToggleSim = { value -> filters.update { it.copy(simSlots = it.simSlots.toggle(value)) } },
        onClearFilters = { filters.value = InboxFilters() },
        onItemClick = { navigate(InboxDetailRoute(it)) },
        onLoadMore = pager::loadMore,
        onRetryLoad = { pager.refresh() }
    )

    init {
        pager.refresh()
        filters.drop(1).onEach { pager.refresh() }.launchIn(viewModelScope)
        query.drop(1).debounce(SEARCH_DEBOUNCE_MS).distinctUntilChanged().onEach { pager.refresh() }.launchIn(viewModelScope)
        store.changes.debounce(CHANGES_DEBOUNCE_MS).onEach { pager.refresh(keepLoaded = true) }.launchIn(viewModelScope)
    }

    private fun currentFilter(): InboxFilter {
        val selected = filters.value
        return InboxFilter(
            types = selected.kinds.map(InboxMapping::type).toSet(),
            simSlots = selected.simSlots,
            search = query.value.trim().takeIf { it.isNotEmpty() }
        )
    }

    private companion object {
        const val SEARCH_DEBOUNCE_MS = 300L
        const val CHANGES_DEBOUNCE_MS = 500L
    }
}

@OptIn(FlowPreview::class)
class InboxDetailViewModel(
    private val id: String,
    private val status: AppStatus,
    private val store: InboxStore,
    private val sharing: FileSharing
) : EffectViewModel() {
    private val mutable = MutableStateFlow(InboxDetailUiState())

    val state: StateFlow<InboxDetailUiState> = mutable.asStateFlow()

    val actions = InboxDetailActions(
        onBack = ::back,
        onOpenAttachment = { attachmentId -> export(attachmentId, share = false) },
        onShareAttachment = { attachmentId -> export(attachmentId, share = true) },
        onRetryLoad = { load(showLoading = true) }
    )

    init {
        load(showLoading = true)
        store.changes.debounce(CHANGES_DEBOUNCE_MS).onEach { load(showLoading = false) }.launchIn(viewModelScope)
    }

    private fun load(showLoading: Boolean) {
        if (showLoading) mutable.update { it.copy(loadState = InboxLoadState.Loading) }
        viewModelScope.launch {
            attempt { store.get(id)?.let { detail(it) } }
                .onSuccess { detail ->
                    mutable.value = if (detail == null) {
                        InboxDetailUiState(InboxLoadState.Error)
                    } else {
                        InboxDetailUiState(InboxLoadState.Ready, detail)
                    }
                }
                .onFailure { if (showLoading) mutable.value = InboxDetailUiState(InboxLoadState.Error) }
        }
    }

    private suspend fun detail(record: InboxRecord): InboxDetail {
        val settings = status.settings.value
        val attachments = record.attachments.map { info ->
            val thumbnail = if (info.contentType.startsWith("image/") && info.size in 1..MAX_THUMBNAIL_BYTES) {
                store.attachment(record.id, info.id)?.bytes?.let(::AttachmentThumbnail)
            } else {
                null
            }
            InboxAttachment(info.id, info.name, info.contentType, info.size, thumbnail)
        }
        return InboxDetail(
            id = record.id,
            sender = record.from,
            kind = InboxMapping.kind(record.type),
            receivedAtMillis = record.receivedAt,
            simSlot = record.simSlot,
            text = record.text,
            subject = record.subject,
            dataBase64 = record.data,
            dataPort = record.port,
            attachments = attachments,
            forwardStatus = forwardStatus(record, settings),
            optOutKeyword = InboxMapping.optOutKeyword(record, settings.optOutKeywords)
        )
    }

    private suspend fun forwardStatus(record: InboxRecord, settings: AppSettings): ForwardStatus {
        if (!settings.inbox.forwards(record.type, record.port)) return ForwardStatus.NotForwarded
        val paired = status.cloudPairing.value is CloudPairingState.Paired
        val subscribed = status.webhookEndpoints.value.any { it.enabled && WebhookEventType.InboxReceived in it.events }
        if (!InboxMapping.forwardTargets(settings, paired, subscribed)) return ForwardStatus.NotForwarded
        val pending = store.pendingCloudUpload(record.localId) ?: return ForwardStatus.Forwarded
        return if (pending.attempts > 0 && pending.lastError != null) ForwardStatus.Failed else ForwardStatus.Pending
    }

    private fun export(attachmentId: String, share: Boolean) {
        viewModelScope.launch {
            val content = attempt { store.attachment(id, attachmentId) }.getOrNull() ?: return@launch
            val file = attempt { sharing.attachmentFile(content.name, content.contentType, content.bytes) }.getOrNull() ?: return@launch
            system(if (share) SystemAction.ShareFile(file.uri, file.mimeType) else SystemAction.ViewFile(file.uri, file.mimeType))
        }
    }

    private companion object {
        const val CHANGES_DEBOUNCE_MS = 300L
        const val MAX_THUMBNAIL_BYTES = 2_000_000L
    }
}
