package com.homingsms.core.model.api

import com.homingsms.core.model.MessagePriority
import com.homingsms.core.model.MessageType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SendMessageValidatorTest {
    private fun valid(request: SendMessageRequest): ValidatedSend {
        val result = SendMessageValidator.validate(request)
        assertTrue("expected valid but got $result", result is Validation.Valid)
        return (result as Validation.Valid).value
    }

    private fun errors(request: SendMessageRequest): List<String> {
        val result = SendMessageValidator.validate(request)
        assertTrue("expected invalid", result is Validation.Invalid)
        return (result as Validation.Invalid).errors
    }

    @Test
    fun acceptsSmsWithDefaults() {
        val send = valid(SendMessageRequest(type = "sms", to = "+2348012345678", text = "Hello"))
        assertEquals(MessageType.Sms, send.type)
        assertEquals(MessagePriority.Normal, send.priority)
        assertTrue(send.deliveryReport)
        assertEquals(emptyMap<String, String>(), send.metadata)
    }

    @Test
    fun acceptsShortCodesAndOptions() {
        val send = valid(
            SendMessageRequest(
                type = "sms",
                to = "12345",
                text = "STOP",
                idempotencyKey = "key-1",
                simSlot = 1,
                priority = "otp",
                deliveryReport = false,
                scheduleAt = "2026-11-12T10:15:30.000Z",
                validUntil = "2026-11-13T10:15:30Z",
                metadata = mapOf("order" to "42")
            )
        )
        assertEquals(MessagePriority.Otp, send.priority)
        assertEquals(1, send.simSlot)
        assertEquals(1_794_478_530_000, send.scheduleAt)
        assertEquals(mapOf("order" to "42"), send.metadata)
    }

    @Test
    fun rejectsBadRecipientTypeAndText() {
        val list = errors(SendMessageRequest(type = "fax", to = "+12", text = ""))
        assertTrue(list.any { it.startsWith("type must be") })
        assertTrue(list.any { it.startsWith("to must match") })
    }

    @Test
    fun smsNeedsTextWithinLimit() {
        assertTrue(errors(SendMessageRequest(type = "sms", to = "+15550001")).any { it.startsWith("text must be") })
        assertTrue(errors(SendMessageRequest(type = "sms", to = "+15550001", text = "x".repeat(1601))).isNotEmpty())
    }

    @Test
    fun dataNeedsBase64PayloadAndPort() {
        valid(SendMessageRequest(type = "data", to = "+15550001", data = "aGVsbG8=", port = 53739))
        assertTrue(errors(SendMessageRequest(type = "data", to = "+15550001", data = "aGVsbG8=")).any { it.startsWith("port") })
        assertTrue(errors(SendMessageRequest(type = "data", to = "+15550001", data = "***", port = 1)).any { it.startsWith("data") })
        val tooBig = kotlin.io.encoding.Base64.Default.encode(ByteArray(141))
        assertTrue(errors(SendMessageRequest(type = "data", to = "+15550001", data = tooBig, port = 1)).any { it.startsWith("data") })
    }

    @Test
    fun mmsNeedsTextOrAttachments() {
        assertTrue(errors(SendMessageRequest(type = "mms", to = "+15550001")).any { it.startsWith("mms needs") })
        val send = valid(
            SendMessageRequest(
                type = "mms",
                to = "+15550001",
                subject = "Photo",
                attachments = listOf(AttachmentInput("image/png", "a.png", "aGVsbG8="))
            )
        )
        assertEquals(5L, send.attachments.single().size)
        assertTrue(
            errors(SendMessageRequest(type = "mms", to = "+15550001", attachments = listOf(AttachmentInput(data = "aGk=")))).isNotEmpty()
        )
    }

    @Test
    fun rejectsOutOfRangeOptions() {
        val list = errors(
            SendMessageRequest(
                type = "sms",
                to = "+15550001",
                text = "x",
                simSlot = 8,
                priority = "urgent",
                scheduleAt = "tomorrow",
                idempotencyKey = "",
                metadata = (1..21).associate { "k$it" to "v" }
            )
        )
        assertEquals(5, list.size)
    }
}
