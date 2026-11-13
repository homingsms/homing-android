package com.homingsms.core.model

data class MessageAttachmentInfo(val id: String, val contentType: String, val name: String?, val size: Long)

data class MessageEventRecord(val state: MessageState, val at: Long, val simSlot: Int?, val error: MessageError?)

data class MessageRecord(
    val id: String,
    val type: MessageType,
    val to: String,
    val text: String?,
    val data: String?,
    val port: Int?,
    val subject: String?,
    val attachments: List<MessageAttachmentInfo>,
    val state: MessageState,
    val priority: MessagePriority,
    val simSlot: Int?,
    val idempotencyKey: String?,
    val scheduleAt: Long?,
    val validUntil: Long?,
    val deliveryReport: Boolean,
    val metadata: Map<String, String>,
    val attempts: Int,
    val partsCount: Int?,
    val error: MessageError?,
    val createdAt: Long,
    val updatedAt: Long,
    val sentAt: Long?,
    val deliveredAt: Long?,
    val failedAt: Long?,
    val events: List<MessageEventRecord>
)

data class InboxAttachmentInfo(val id: String, val contentType: String, val name: String?, val size: Long)

data class InboxRecord(
    val id: String,
    val localId: String,
    val type: MessageType,
    val from: String,
    val to: String?,
    val simSlot: Int?,
    val text: String?,
    val data: String?,
    val port: Int?,
    val subject: String?,
    val attachments: List<InboxAttachmentInfo>,
    val receivedAt: Long,
    val createdAt: Long
)

data class InboxAttachmentContent(val contentType: String, val name: String?, val bytes: ByteArray) {
    override fun equals(other: Any?): Boolean = other is InboxAttachmentContent &&
        other.contentType == contentType &&
        other.name == name &&
        other.bytes.contentEquals(bytes)

    override fun hashCode(): Int = (contentType.hashCode() * 31 + name.hashCode()) * 31 + bytes.contentHashCode()
}

data class WebhookEndpoint(
    val id: String,
    val url: String,
    val description: String?,
    val events: Set<WebhookEventType>,
    val enabled: Boolean,
    val createdAt: Long,
    val updatedAt: Long
)

data class WebhookDelivery(
    val id: String,
    val endpointId: String,
    val eventId: String,
    val eventType: WebhookEventType,
    val status: WebhookDeliveryStatus,
    val attempts: Int,
    val lastStatusCode: Int?,
    val lastResponseBody: String?,
    val lastError: String?,
    val nextAttemptAt: Long?,
    val deliveredAt: Long?,
    val createdAt: Long,
    val updatedAt: Long,
    val payload: String? = null
)

data class Page<T>(val items: List<T>, val nextCursor: String?)
