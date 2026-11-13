package com.homingsms.core.model.api

import com.homingsms.core.model.MessagePriority
import com.homingsms.core.model.MessageState
import com.homingsms.core.model.MessageType
import com.homingsms.core.model.WebhookDeliveryStatus
import com.homingsms.core.model.WebhookEventType
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonEncoder
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject

@Serializable
data class ApiError(val statusCode: Int, val message: JsonElement, val error: String, val timestamp: String, val path: String)

@Serializable
data class MessageErrorDto(val code: String, val message: String)

@Serializable
data class MessageAttachmentDto(val id: String, val contentType: String, val name: String?, val size: Long)

@Serializable
data class MessageDto(
    val id: String,
    val type: MessageType,
    val to: String,
    val text: String?,
    val data: String?,
    val port: Int?,
    val subject: String?,
    val attachments: List<MessageAttachmentDto>,
    val state: MessageState,
    val priority: MessagePriority,
    val deviceId: String?,
    val simSlot: Int?,
    val idempotencyKey: String?,
    val scheduleAt: String?,
    val validUntil: String?,
    val deliveryReport: Boolean,
    val metadata: Map<String, String>,
    val attempts: Int,
    val partsCount: Int?,
    val error: MessageErrorDto?,
    val createdAt: String,
    val updatedAt: String,
    val sentAt: String?,
    val deliveredAt: String?,
    val failedAt: String?
)

@Serializable
data class MessageEventDto(val state: MessageState, val at: String, val deviceId: String?, val simSlot: Int?, val error: MessageErrorDto?)

@Serializable(with = MessageDetailSerializer::class)
data class MessageDetailDto(val message: MessageDto, val events: List<MessageEventDto>)

object MessageDetailSerializer : KSerializer<MessageDetailDto> {
    private const val EVENTS = "events"
    private val eventsSerializer = ListSerializer(MessageEventDto.serializer())

    override val descriptor: SerialDescriptor = SerialDescriptor("MessageDetail", JsonObject.serializer().descriptor)

    override fun serialize(encoder: Encoder, value: MessageDetailDto) {
        val json = (encoder as JsonEncoder).json
        val message = json.encodeToJsonElement(MessageDto.serializer(), value.message).jsonObject
        val events = json.encodeToJsonElement(eventsSerializer, value.events)
        encoder.encodeJsonElement(JsonObject(message + (EVENTS to events)))
    }

    override fun deserialize(decoder: Decoder): MessageDetailDto {
        val jsonDecoder = decoder as JsonDecoder
        val json = jsonDecoder.json
        val element = jsonDecoder.decodeJsonElement().jsonObject
        val events = element[EVENTS]?.let { json.decodeFromJsonElement(eventsSerializer, it) }.orEmpty()
        val message = json.decodeFromJsonElement(MessageDto.serializer(), JsonObject(element - EVENTS))
        return MessageDetailDto(message, events)
    }
}

@Serializable
data class MessagePage(val data: List<MessageDto>, val nextCursor: String?)

@Serializable
data class InboxAttachmentDto(val id: String, val contentType: String, val name: String?, val size: Long)

@Serializable
data class InboxMessageDto(
    val id: String,
    val type: MessageType,
    val from: String,
    val to: String?,
    val deviceId: String?,
    val simSlot: Int?,
    val text: String?,
    val data: String?,
    val port: Int?,
    val subject: String?,
    val attachments: List<InboxAttachmentDto>,
    val receivedAt: String,
    val createdAt: String
)

@Serializable
data class InboxPage(val data: List<InboxMessageDto>, val nextCursor: String?)

@Serializable
data class WebhookEndpointDto(
    val id: String,
    val url: String,
    val description: String?,
    val events: List<WebhookEventType>,
    val enabled: Boolean,
    val createdAt: String,
    val updatedAt: String
)

@Serializable
data class WebhookEndpointWithSecretDto(
    val id: String,
    val url: String,
    val description: String?,
    val events: List<WebhookEventType>,
    val enabled: Boolean,
    val createdAt: String,
    val updatedAt: String,
    val secret: String
)

@Serializable
data class WebhookEndpointList(val data: List<WebhookEndpointDto>)

@Serializable
data class WebhookSecretDto(val secret: String)

@Serializable
data class WebhookDeliveryDto(
    val id: String,
    val endpointId: String,
    val eventId: String,
    val eventType: WebhookEventType,
    val status: WebhookDeliveryStatus,
    val attempts: Int,
    val lastStatusCode: Int?,
    val lastResponseBody: String?,
    val lastError: String?,
    val nextAttemptAt: String?,
    val deliveredAt: String?,
    val createdAt: String,
    val updatedAt: String
)

@Serializable
data class WebhookDeliveryPage(val data: List<WebhookDeliveryDto>, val nextCursor: String?)

@Serializable
data class HealthDto(val status: String, val version: String, val time: String, val checks: Map<String, String>)

@Serializable
data class WebhookEnvelope(val id: String, val type: WebhookEventType, val createdAt: String, val accountId: String, val data: JsonElement)

@Serializable
data class MessageEventData(val message: MessageDto)

@Serializable
data class InboxEventData(val message: InboxMessageDto)

@Serializable
data class OptOutEventData(
    val phoneNumber: String,
    val keyword: String,
    val inboxMessageId: String,
    val deviceId: String?,
    val receivedAt: String
)

@Serializable
data class TestEventData(val endpointId: String, val message: String)
