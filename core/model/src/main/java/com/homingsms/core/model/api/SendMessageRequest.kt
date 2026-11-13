package com.homingsms.core.model.api

import com.homingsms.core.model.MessagePriority
import com.homingsms.core.model.MessageType
import kotlin.io.encoding.Base64
import kotlinx.serialization.Serializable

@Serializable
data class AttachmentInput(val contentType: String? = null, val name: String? = null, val data: String? = null)

@Serializable
data class SendMessageRequest(
    val type: String? = null,
    val to: String? = null,
    val text: String? = null,
    val data: String? = null,
    val port: Int? = null,
    val subject: String? = null,
    val attachments: List<AttachmentInput>? = null,
    val idempotencyKey: String? = null,
    val deviceId: String? = null,
    val simSlot: Int? = null,
    val scheduleAt: String? = null,
    val validUntil: String? = null,
    val priority: String? = null,
    val deliveryReport: Boolean? = null,
    val metadata: Map<String, String>? = null
)

class ValidatedAttachment(val contentType: String, val name: String?, val base64: String, val size: Long)

data class ValidatedSend(
    val type: MessageType,
    val to: String,
    val text: String?,
    val data: String?,
    val port: Int?,
    val subject: String?,
    val attachments: List<ValidatedAttachment>,
    val idempotencyKey: String?,
    val deviceId: String?,
    val simSlot: Int?,
    val scheduleAt: Long?,
    val validUntil: Long?,
    val priority: MessagePriority,
    val deliveryReport: Boolean,
    val metadata: Map<String, String>
)

sealed interface Validation<out T> {
    data class Valid<T>(val value: T) : Validation<T>

    data class Invalid(val errors: List<String>) : Validation<Nothing>
}

object SendMessageValidator {
    const val MAX_TEXT = 1600
    const val MAX_DATA_BASE64 = 188
    const val MAX_DATA_BYTES = 140
    const val MAX_PORT = 65535
    const val MAX_SUBJECT = 80
    const val MAX_ATTACHMENTS = 10
    const val MAX_CONTENT_TYPE = 100
    const val MAX_NAME = 255
    const val MAX_ATTACHMENT_BYTES = 1_048_576L
    const val MAX_IDEMPOTENCY_KEY = 128
    const val MAX_SIM_SLOT = 7
    const val MAX_METADATA = 20
    const val MAX_METADATA_VALUE = 500
    val phonePattern = Regex("^\\+?[0-9]{3,15}$")

    fun validate(request: SendMessageRequest): Validation<ValidatedSend> {
        val errors = mutableListOf<String>()
        val type = request.type?.let { MessageType.fromValue(it) }
        if (type == null) errors += "type must be one of the following values: sms, data, mms"
        val to = request.to
        if (to == null || !phonePattern.matches(to)) errors += "to must match /^\\+?[0-9]{3,15}$/ regular expression"
        val attachments = validateContent(type, request, errors)
        validateOptions(request, errors)
        val scheduleAt = parseTime("scheduleAt", request.scheduleAt, errors)
        val validUntil = parseTime("validUntil", request.validUntil, errors)
        val priority = parsePriority(request.priority, errors)
        if (errors.isNotEmpty() || type == null || to == null) return Validation.Invalid(errors)
        return Validation.Valid(
            ValidatedSend(
                type = type,
                to = to,
                text = request.text.takeIf { type != MessageType.Data },
                data = request.data.takeIf { type == MessageType.Data },
                port = request.port.takeIf { type == MessageType.Data },
                subject = request.subject.takeIf { type == MessageType.Mms },
                attachments = attachments,
                idempotencyKey = request.idempotencyKey,
                deviceId = request.deviceId,
                simSlot = request.simSlot,
                scheduleAt = scheduleAt,
                validUntil = validUntil,
                priority = priority,
                deliveryReport = request.deliveryReport ?: true,
                metadata = request.metadata.orEmpty()
            )
        )
    }

    private fun validateContent(type: MessageType?, request: SendMessageRequest, errors: MutableList<String>): List<ValidatedAttachment> =
        when (type) {
            MessageType.Sms -> emptyList<ValidatedAttachment>().also { validateSms(request, errors) }
            MessageType.Data -> emptyList<ValidatedAttachment>().also { validateData(request, errors) }
            MessageType.Mms -> validateMms(request, errors)
            null -> emptyList()
        }

    private fun parsePriority(value: String?, errors: MutableList<String>): MessagePriority {
        if (value == null) return MessagePriority.Normal
        return MessagePriority.fromValue(value) ?: MessagePriority.Normal.also {
            errors += "priority must be one of the following values: normal, otp"
        }
    }

    fun decodedSize(base64: String): Long? = runCatching { Base64.Default.decode(base64).size.toLong() }.getOrNull()

    private fun validateSms(request: SendMessageRequest, errors: MutableList<String>) {
        val text = request.text
        if (text.isNullOrEmpty() || text.length > MAX_TEXT) {
            errors += "text must be longer than or equal to 1 and shorter than or equal to $MAX_TEXT characters"
        }
    }

    private fun validateData(request: SendMessageRequest, errors: MutableList<String>) {
        val data = request.data
        val size = data?.let(::decodedSize)
        when {
            data.isNullOrEmpty() || data.length > MAX_DATA_BASE64 -> errors += "data must be base64 of at most $MAX_DATA_BYTES bytes"
            size == null -> errors += "data must be base64 encoded"
            size > MAX_DATA_BYTES -> errors += "data must be base64 of at most $MAX_DATA_BYTES bytes"
        }
        val port = request.port
        if (port == null || port !in 1..MAX_PORT) errors += "port must not be less than 1 and not greater than $MAX_PORT"
    }

    private fun validateMms(request: SendMessageRequest, errors: MutableList<String>): List<ValidatedAttachment> {
        if ((request.subject?.length ?: 0) > MAX_SUBJECT) errors += "subject must be shorter than or equal to $MAX_SUBJECT characters"
        if ((request.text?.length ?: 0) > MAX_TEXT) errors += "text must be shorter than or equal to $MAX_TEXT characters"
        val inputs = request.attachments.orEmpty()
        if (inputs.size > MAX_ATTACHMENTS) errors += "attachments must contain no more than $MAX_ATTACHMENTS elements"
        if (request.text.isNullOrEmpty() && inputs.isEmpty()) errors += "mms needs text or at least one attachment"
        val attachments = inputs.mapIndexedNotNull { index, input -> validateAttachment(index, input, errors) }
        if (attachments.sumOf { it.size } > MAX_ATTACHMENT_BYTES) errors += "attachments must be at most 1 MiB in total"
        return attachments
    }

    private fun validateAttachment(index: Int, input: AttachmentInput, errors: MutableList<String>): ValidatedAttachment? {
        val contentType = input.contentType
        val size = input.data?.let(::decodedSize)
        var valid = true
        if (contentType.isNullOrBlank() || contentType.length > MAX_CONTENT_TYPE) {
            errors += "attachments.$index.contentType must be a string of at most $MAX_CONTENT_TYPE characters"
            valid = false
        }
        if ((input.name?.length ?: 0) > MAX_NAME) {
            errors += "attachments.$index.name must be shorter than or equal to $MAX_NAME characters"
            valid = false
        }
        if (size == null) {
            errors += "attachments.$index.data must be base64 encoded"
            valid = false
        }
        if (!valid || contentType == null || size == null) return null
        return ValidatedAttachment(contentType, input.name, requireNotNull(input.data), size)
    }

    private fun validateOptions(request: SendMessageRequest, errors: MutableList<String>) {
        request.idempotencyKey?.let {
            if (it.isEmpty() || it.length > MAX_IDEMPOTENCY_KEY) {
                errors += "idempotencyKey must be longer than or equal to 1 and shorter than or equal to $MAX_IDEMPOTENCY_KEY characters"
            }
        }
        request.simSlot?.let {
            if (it !in 0..MAX_SIM_SLOT) errors += "simSlot must not be less than 0 and not greater than $MAX_SIM_SLOT"
        }
        request.metadata?.let { metadata ->
            if (metadata.size > MAX_METADATA) errors += "metadata must have at most $MAX_METADATA properties"
            if (metadata.values.any { it.length > MAX_METADATA_VALUE }) {
                errors += "metadata values must be shorter than or equal to $MAX_METADATA_VALUE characters"
            }
        }
    }

    private fun parseTime(name: String, value: String?, errors: MutableList<String>): Long? {
        if (value == null) return null
        val parsed = Iso8601.parse(value)
        if (parsed == null) errors += "$name must be a valid ISO 8601 date string"
        return parsed
    }
}
