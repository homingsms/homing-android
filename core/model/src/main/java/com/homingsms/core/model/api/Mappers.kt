package com.homingsms.core.model.api

import com.homingsms.core.model.InboxRecord
import com.homingsms.core.model.MessageError
import com.homingsms.core.model.MessageRecord
import com.homingsms.core.model.WebhookDelivery
import com.homingsms.core.model.WebhookEndpoint

fun MessageError.toDto(): MessageErrorDto = MessageErrorDto(code, message)

fun MessageRecord.toDto(deviceId: String?): MessageDto = MessageDto(
    id = id,
    type = type,
    to = to,
    text = text,
    data = data,
    port = port,
    subject = subject,
    attachments = attachments.map { MessageAttachmentDto(it.id, it.contentType, it.name, it.size) },
    state = state,
    priority = priority,
    deviceId = deviceId,
    simSlot = simSlot,
    idempotencyKey = idempotencyKey,
    scheduleAt = Iso8601.formatOrNull(scheduleAt),
    validUntil = Iso8601.formatOrNull(validUntil),
    deliveryReport = deliveryReport,
    metadata = metadata,
    attempts = attempts,
    partsCount = partsCount,
    error = error?.toDto(),
    createdAt = Iso8601.format(createdAt),
    updatedAt = Iso8601.format(updatedAt),
    sentAt = Iso8601.formatOrNull(sentAt),
    deliveredAt = Iso8601.formatOrNull(deliveredAt),
    failedAt = Iso8601.formatOrNull(failedAt)
)

fun MessageRecord.toDetailDto(deviceId: String?): MessageDetailDto = MessageDetailDto(
    message = toDto(deviceId),
    events = events.map {
        MessageEventDto(
            state = it.state,
            at = Iso8601.format(it.at),
            deviceId = deviceId,
            simSlot = it.simSlot,
            error = it.error?.toDto()
        )
    }
)

fun InboxRecord.toDto(deviceId: String?): InboxMessageDto = InboxMessageDto(
    id = id,
    type = type,
    from = from,
    to = to,
    deviceId = deviceId,
    simSlot = simSlot,
    text = text,
    data = data,
    port = port,
    subject = subject,
    attachments = attachments.map { InboxAttachmentDto(it.id, it.contentType, it.name, it.size) },
    receivedAt = Iso8601.format(receivedAt),
    createdAt = Iso8601.format(createdAt)
)

fun WebhookEndpoint.toDto(): WebhookEndpointDto = WebhookEndpointDto(
    id = id,
    url = url,
    description = description,
    events = events.sortedBy { it.ordinal },
    enabled = enabled,
    createdAt = Iso8601.format(createdAt),
    updatedAt = Iso8601.format(updatedAt)
)

fun WebhookEndpoint.toDtoWithSecret(secret: String): WebhookEndpointWithSecretDto = WebhookEndpointWithSecretDto(
    id = id,
    url = url,
    description = description,
    events = events.sortedBy { it.ordinal },
    enabled = enabled,
    createdAt = Iso8601.format(createdAt),
    updatedAt = Iso8601.format(updatedAt),
    secret = secret
)

fun WebhookDelivery.toDto(): WebhookDeliveryDto = WebhookDeliveryDto(
    id = id,
    endpointId = endpointId,
    eventId = eventId,
    eventType = eventType,
    status = status,
    attempts = attempts,
    lastStatusCode = lastStatusCode,
    lastResponseBody = lastResponseBody,
    lastError = lastError,
    nextAttemptAt = Iso8601.formatOrNull(nextAttemptAt),
    deliveredAt = Iso8601.formatOrNull(deliveredAt),
    createdAt = Iso8601.format(createdAt),
    updatedAt = Iso8601.format(updatedAt)
)
