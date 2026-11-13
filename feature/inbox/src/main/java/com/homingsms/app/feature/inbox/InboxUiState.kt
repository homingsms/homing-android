package com.homingsms.app.feature.inbox

import androidx.compose.runtime.Immutable
import com.homingsms.app.core.designsystem.component.AppendState

enum class InboxKind { Sms, Data, Mms }

enum class InboxLoadState { Loading, Error, Ready }

class AttachmentThumbnail(val bytes: ByteArray)

@Immutable
data class InboxSim(val slot: Int, val carrier: String?)

@Immutable
data class InboxFilters(val kinds: Set<InboxKind> = emptySet(), val simSlots: Set<Int> = emptySet()) {
    val isActive: Boolean get() = kinds.isNotEmpty() || simSlots.isNotEmpty()
}

@Immutable
data class InboxListItem(
    val id: String,
    val sender: String,
    val kind: InboxKind,
    val preview: String?,
    val receivedAtMillis: Long,
    val simSlot: Int? = null,
    val attachmentCount: Int = 0,
    val dataPort: Int? = null,
    val thumbnail: AttachmentThumbnail? = null,
    val optOutKeyword: String? = null
)

@Immutable
data class InboxUiState(
    val query: String = "",
    val filters: InboxFilters = InboxFilters(),
    val availableSims: List<InboxSim> = emptyList(),
    val items: List<InboxListItem> = emptyList(),
    val loadState: InboxLoadState = InboxLoadState.Loading,
    val appendState: AppendState = AppendState.Idle
)

@Immutable
data class InboxActions(
    val onQueryChange: (String) -> Unit = {},
    val onToggleKind: (InboxKind) -> Unit = {},
    val onToggleSim: (Int) -> Unit = {},
    val onClearFilters: () -> Unit = {},
    val onItemClick: (String) -> Unit = {},
    val onLoadMore: () -> Unit = {},
    val onRetryLoad: () -> Unit = {}
)

@Immutable
data class InboxAttachment(
    val id: String,
    val name: String?,
    val contentType: String,
    val sizeBytes: Long,
    val thumbnail: AttachmentThumbnail? = null
) {
    val isImage: Boolean get() = contentType.startsWith("image/")
}

enum class ForwardStatus { Forwarded, Pending, Failed, NotForwarded }

@Immutable
data class InboxDetail(
    val id: String,
    val sender: String,
    val kind: InboxKind,
    val receivedAtMillis: Long,
    val simSlot: Int? = null,
    val text: String? = null,
    val subject: String? = null,
    val dataBase64: String? = null,
    val dataPort: Int? = null,
    val attachments: List<InboxAttachment> = emptyList(),
    val forwardStatus: ForwardStatus = ForwardStatus.NotForwarded,
    val optOutKeyword: String? = null
)

@Immutable
data class InboxDetailUiState(val loadState: InboxLoadState = InboxLoadState.Loading, val message: InboxDetail? = null)

@Immutable
data class InboxDetailActions(
    val onBack: () -> Unit = {},
    val onOpenAttachment: (String) -> Unit = {},
    val onShareAttachment: (String) -> Unit = {},
    val onRetryLoad: () -> Unit = {}
)
