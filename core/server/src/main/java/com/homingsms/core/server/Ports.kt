package com.homingsms.core.server

import com.homingsms.core.model.ApiKeyPrincipal
import com.homingsms.core.model.Cursor
import com.homingsms.core.model.InboxAttachmentContent
import com.homingsms.core.model.InboxFilter
import com.homingsms.core.model.InboxRecord
import com.homingsms.core.model.MessageFilter
import com.homingsms.core.model.MessageRecord
import com.homingsms.core.model.Page
import com.homingsms.core.model.WebhookDelivery
import com.homingsms.core.model.WebhookDeliveryStatus
import com.homingsms.core.model.WebhookEndpoint
import com.homingsms.core.model.WebhookEventType
import com.homingsms.core.model.api.HealthDto
import com.homingsms.core.model.api.ValidatedSend

sealed interface SendResult {
    data class Created(val record: MessageRecord) : SendResult

    data class Replayed(val record: MessageRecord) : SendResult

    data class Conflict(val message: String) : SendResult

    data class Rejected(val message: String) : SendResult
}

sealed interface CancelResult {
    data class Cancelled(val record: MessageRecord) : CancelResult

    data object NotFound : CancelResult

    data class Conflict(val message: String) : CancelResult
}

interface MessagesPort {
    suspend fun send(request: ValidatedSend, requestHash: String): SendResult

    suspend fun list(filter: MessageFilter, cursor: Cursor?, limit: Int): Page<MessageRecord>

    suspend fun get(id: String): MessageRecord?

    suspend fun cancel(id: String): CancelResult
}

interface InboxPort {
    suspend fun list(filter: InboxFilter, cursor: Cursor?, limit: Int): Page<InboxRecord>

    suspend fun get(id: String): InboxRecord?

    suspend fun attachment(id: String, attachmentId: String): InboxAttachmentContent?
}

data class NewWebhook(val url: String, val events: Set<WebhookEventType>, val description: String?, val enabled: Boolean)

data class WebhookPatch(
    val url: String? = null,
    val events: Set<WebhookEventType>? = null,
    val descriptionSet: Boolean = false,
    val description: String? = null,
    val enabled: Boolean? = null
)

data class CreatedWebhook(val endpoint: WebhookEndpoint, val secret: String)

interface WebhooksPort {
    suspend fun list(): List<WebhookEndpoint>

    suspend fun create(webhook: NewWebhook): CreatedWebhook

    suspend fun get(id: String): WebhookEndpoint?

    suspend fun update(id: String, patch: WebhookPatch): WebhookEndpoint?

    suspend fun delete(id: String): Boolean

    suspend fun rotateSecret(id: String): String?

    suspend fun test(id: String): WebhookDelivery?

    suspend fun deliveries(id: String, status: WebhookDeliveryStatus?, cursor: Cursor?, limit: Int): Page<WebhookDelivery>?

    suspend fun redeliver(id: String, deliveryId: String): WebhookDelivery?
}

fun interface ApiKeyAuthenticator {
    suspend fun authenticate(key: String): ApiKeyPrincipal?
}

fun interface HealthPort {
    suspend fun health(): HealthDto
}

class LocalApiConfig(
    val messages: MessagesPort,
    val inbox: InboxPort,
    val webhooks: WebhooksPort,
    val authenticator: ApiKeyAuthenticator,
    val health: HealthPort,
    val deviceIds: () -> List<String>,
    val allowCleartextWebhooks: Boolean = false,
    val prettyJson: Boolean = false,
    val clock: () -> Long = System::currentTimeMillis
)
