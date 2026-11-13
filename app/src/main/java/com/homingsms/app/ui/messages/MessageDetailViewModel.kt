package com.homingsms.app.ui.messages

import androidx.lifecycle.viewModelScope
import com.homingsms.app.feature.messages.LoadState
import com.homingsms.app.feature.messages.MessageAttachmentUi
import com.homingsms.app.feature.messages.MessageDetail
import com.homingsms.app.feature.messages.MessageDetailActions
import com.homingsms.app.feature.messages.MessageDetailUiState
import com.homingsms.app.feature.messages.MessageError
import com.homingsms.app.feature.messages.MessageEvent
import com.homingsms.app.feature.messages.MessageNotice
import com.homingsms.app.feature.messages.MessagePriority
import com.homingsms.app.feature.messages.MessageSource
import com.homingsms.app.ui.EffectViewModel
import com.homingsms.app.ui.UiMappers
import com.homingsms.app.ui.attempt
import com.homingsms.app.ui.data.MessageStore
import com.homingsms.app.ui.data.StoredMessage
import com.homingsms.core.model.MessagePriority as CorePriority
import com.homingsms.core.model.MessageRecord
import com.homingsms.core.model.MessageState
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@OptIn(FlowPreview::class)
class MessageDetailViewModel(private val id: String, private val store: MessageStore) : EffectViewModel() {
    private val mutable = MutableStateFlow(MessageDetailUiState())

    val state: StateFlow<MessageDetailUiState> = mutable.asStateFlow()

    val actions = MessageDetailActions(
        onBack = ::back,
        onRetry = ::retry,
        onCancel = ::cancel,
        onRetryLoad = { load(showLoading = true) },
        onNoticeShown = { mutable.update { it.copy(notice = null) } }
    )

    init {
        load(showLoading = true)
        store.changes.debounce(CHANGES_DEBOUNCE_MS).onEach { load(showLoading = false) }.launchIn(viewModelScope)
    }

    private fun load(showLoading: Boolean) {
        if (showLoading) mutable.update { it.copy(loadState = LoadState.Loading) }
        viewModelScope.launch {
            attempt { store.get(id) }
                .onSuccess { stored ->
                    mutable.update { current ->
                        if (stored == null) {
                            current.copy(loadState = LoadState.Error, message = null, canRetry = false, canCancel = false)
                        } else {
                            current.copy(
                                loadState = LoadState.Ready,
                                message = detail(stored),
                                canRetry = stored.local && stored.record.state in retryable,
                                canCancel = stored.local && stored.record.state in cancellable
                            )
                        }
                    }
                }
                .onFailure { if (showLoading) mutable.update { it.copy(loadState = LoadState.Error) } }
        }
    }

    private fun retry() {
        if (mutable.value.isRetrying) return
        mutable.update { it.copy(isRetrying = true) }
        viewModelScope.launch {
            val created = attempt { store.retry(id) }.getOrNull()
            mutable.update {
                it.copy(
                    isRetrying = false,
                    notice = if (created !=
                        null
                    ) {
                        MessageNotice.RetryQueued
                    } else {
                        MessageNotice.ActionFailed
                    }
                )
            }
        }
    }

    private fun cancel() {
        if (mutable.value.isCancelling) return
        mutable.update { it.copy(isCancelling = true) }
        viewModelScope.launch {
            val cancelled = attempt { store.cancel(id) }.getOrDefault(false)
            mutable.update {
                it.copy(isCancelling = false, notice = if (cancelled) MessageNotice.Cancelled else MessageNotice.ActionFailed)
            }
            load(showLoading = false)
        }
    }

    companion object {
        private const val CHANGES_DEBOUNCE_MS = 300L
        private val retryable = setOf(MessageState.Failed, MessageState.Expired, MessageState.Cancelled)
        private val cancellable = setOf(MessageState.Pending, MessageState.Scheduled)

        fun detail(stored: StoredMessage): MessageDetail {
            val record: MessageRecord = stored.record
            return MessageDetail(
                id = record.id,
                recipient = record.to,
                type = UiMappers.messageType(record.type),
                state = UiMappers.messageState(record.state),
                createdAtMillis = record.createdAt,
                events = record.events.map { event ->
                    MessageEvent(UiMappers.messageState(event.state), event.at, event.error?.message)
                },
                source = if (stored.local) MessageSource.Local else MessageSource.Cloud,
                priority = if (record.priority == CorePriority.Otp) MessagePriority.Otp else MessagePriority.Normal,
                text = record.text,
                dataBase64 = record.data,
                dataPort = record.port,
                subject = record.subject,
                attachments = record.attachments.map { MessageAttachmentUi(it.id, it.name, it.contentType, it.size) },
                simSlot = record.simSlot,
                partsCount = record.partsCount ?: 1,
                attempts = record.attempts,
                deliveryReport = record.deliveryReport,
                scheduleAtMillis = record.scheduleAt,
                validUntilMillis = record.validUntil,
                idempotencyKey = record.idempotencyKey,
                error = record.error?.let { MessageError(it.code, it.message) }
            )
        }
    }
}
