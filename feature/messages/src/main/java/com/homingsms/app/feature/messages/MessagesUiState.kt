package com.homingsms.app.feature.messages

import androidx.compose.runtime.Immutable
import com.homingsms.app.core.designsystem.component.AppendState

enum class MessageState { Scheduled, Pending, Leased, Queued, Sending, Sent, Delivered, Failed, Cancelled, Expired }

enum class MessageType { Sms, Data, Mms }

enum class MessagePriority { Normal, Otp }

enum class MessageSource { Local, Cloud }

enum class LoadState { Loading, Error, Ready }

@Immutable
data class SimOption(val slot: Int, val carrier: String?)

@Immutable
data class MessageFilters(
    val states: Set<MessageState> = emptySet(),
    val types: Set<MessageType> = emptySet(),
    val simSlots: Set<Int> = emptySet()
) {
    val isActive: Boolean get() = states.isNotEmpty() || types.isNotEmpty() || simSlots.isNotEmpty()
}

@Immutable
data class MessageListItem(
    val id: String,
    val recipient: String,
    val type: MessageType,
    val state: MessageState,
    val preview: String?,
    val updatedAtMillis: Long,
    val simSlot: Int? = null,
    val priority: MessagePriority = MessagePriority.Normal,
    val attachmentCount: Int = 0,
    val dataPort: Int? = null,
    val errorCode: String? = null
)

@Immutable
data class MessagesUiState(
    val query: String = "",
    val filters: MessageFilters = MessageFilters(),
    val availableSims: List<SimOption> = emptyList(),
    val items: List<MessageListItem> = emptyList(),
    val loadState: LoadState = LoadState.Loading,
    val appendState: AppendState = AppendState.Idle
)

@Immutable
data class MessagesActions(
    val onQueryChange: (String) -> Unit = {},
    val onToggleState: (MessageState) -> Unit = {},
    val onToggleType: (MessageType) -> Unit = {},
    val onToggleSim: (Int) -> Unit = {},
    val onClearFilters: () -> Unit = {},
    val onMessageClick: (String) -> Unit = {},
    val onLoadMore: () -> Unit = {},
    val onRetryLoad: () -> Unit = {}
)

@Immutable
data class MessageAttachmentUi(val id: String, val name: String?, val contentType: String, val sizeBytes: Long)

@Immutable
data class MessageError(val code: String, val message: String)

@Immutable
data class MessageEvent(val state: MessageState, val atMillis: Long, val detail: String? = null)

@Immutable
data class MessageDetail(
    val id: String,
    val recipient: String,
    val type: MessageType,
    val state: MessageState,
    val createdAtMillis: Long,
    val events: List<MessageEvent>,
    val source: MessageSource = MessageSource.Local,
    val priority: MessagePriority = MessagePriority.Normal,
    val text: String? = null,
    val dataBase64: String? = null,
    val dataPort: Int? = null,
    val subject: String? = null,
    val attachments: List<MessageAttachmentUi> = emptyList(),
    val simSlot: Int? = null,
    val partsCount: Int = 1,
    val attempts: Int = 0,
    val deliveryReport: Boolean = true,
    val scheduleAtMillis: Long? = null,
    val validUntilMillis: Long? = null,
    val idempotencyKey: String? = null,
    val error: MessageError? = null
)

enum class MessageNotice { RetryQueued, Cancelled, ActionFailed }

@Immutable
data class MessageDetailUiState(
    val loadState: LoadState = LoadState.Loading,
    val message: MessageDetail? = null,
    val canRetry: Boolean = false,
    val canCancel: Boolean = false,
    val isRetrying: Boolean = false,
    val isCancelling: Boolean = false,
    val notice: MessageNotice? = null
)

@Immutable
data class MessageDetailActions(
    val onBack: () -> Unit = {},
    val onRetry: () -> Unit = {},
    val onCancel: () -> Unit = {},
    val onRetryLoad: () -> Unit = {},
    val onNoticeShown: () -> Unit = {}
)
