package com.homingsms.app.feature.webhooks

import androidx.compose.runtime.Immutable
import com.homingsms.app.core.designsystem.component.AppendState

enum class WebhookEvent(val wireName: String) {
    MessageSent("message.sent"),
    MessageDelivered("message.delivered"),
    MessageFailed("message.failed"),
    InboxReceived("inbox.received"),
    OptOutReceived("opt_out.received"),
    DeviceOnline("device.online"),
    DeviceOffline("device.offline")
}

enum class DeliveryStatus { Succeeded, Retrying, Pending, Failed }

enum class WebhooksLoadState { Loading, Error, Ready }

@Immutable
data class DeliverySummary(val status: DeliveryStatus, val statusCode: Int?, val atMillis: Long)

@Immutable
data class WebhookEndpointUi(
    val id: String,
    val url: String,
    val events: Set<WebhookEvent>,
    val enabled: Boolean,
    val lastDelivery: DeliverySummary? = null
)

@Immutable
data class WebhooksUiState(
    val loadState: WebhooksLoadState = WebhooksLoadState.Loading,
    val endpoints: List<WebhookEndpointUi> = emptyList()
)

@Immutable
data class WebhooksActions(
    val onBack: (() -> Unit)? = null,
    val onAdd: () -> Unit = {},
    val onEndpointClick: (String) -> Unit = {},
    val onEnabledChange: (String, Boolean) -> Unit = { _, _ -> },
    val onRetryLoad: () -> Unit = {}
)

enum class WebhookUrlError { Empty, Invalid, NotHttps }

@Immutable
data class WebhookEditorUiState(
    val isNew: Boolean = true,
    val url: String = "",
    val urlError: WebhookUrlError? = null,
    val events: Set<WebhookEvent> = emptySet(),
    val enabled: Boolean = true,
    val isSaving: Boolean = false,
    val isDeleting: Boolean = false
) {
    val canSave: Boolean get() = url.isNotBlank() && events.isNotEmpty() && !isSaving
}

@Immutable
data class WebhookEditorActions(
    val onBack: () -> Unit = {},
    val onUrlChange: (String) -> Unit = {},
    val onToggleEvent: (WebhookEvent) -> Unit = {},
    val onEnabledChange: (Boolean) -> Unit = {},
    val onSave: () -> Unit = {},
    val onDelete: () -> Unit = {}
)

@Immutable
data class DeliveryUi(
    val id: String,
    val eventType: String,
    val status: DeliveryStatus,
    val atMillis: Long,
    val attempt: Int = 1,
    val statusCode: Int? = null,
    val durationMillis: Long? = null,
    val requestBody: String? = null,
    val responseBody: String? = null,
    val errorMessage: String? = null,
    val nextRetryAtMillis: Long? = null,
    val isRetrying: Boolean = false
)

sealed interface TestEventState {
    data object Idle : TestEventState
    data object Sending : TestEventState
    data class Delivered(val statusCode: Int) : TestEventState
    data class Failed(val statusCode: Int?) : TestEventState
}

enum class WebhookNotice { SecretRotated, RetryQueued, ActionFailed }

@Immutable
data class WebhookDetailUiState(
    val loadState: WebhooksLoadState = WebhooksLoadState.Loading,
    val endpoint: WebhookEndpointUi? = null,
    val signingSecret: String? = null,
    val deliveries: List<DeliveryUi> = emptyList(),
    val deliveriesAppendState: AppendState = AppendState.Idle,
    val testState: TestEventState = TestEventState.Idle,
    val isRotatingSecret: Boolean = false,
    val notice: WebhookNotice? = null
)

@Immutable
data class WebhookDetailActions(
    val onBack: () -> Unit = {},
    val onEdit: () -> Unit = {},
    val onSendTest: () -> Unit = {},
    val onRotateSecret: () -> Unit = {},
    val onRetryDelivery: (String) -> Unit = {},
    val onLoadMoreDeliveries: () -> Unit = {},
    val onRetryLoad: () -> Unit = {},
    val onNoticeShown: () -> Unit = {}
)
