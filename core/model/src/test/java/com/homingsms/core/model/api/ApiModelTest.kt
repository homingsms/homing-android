package com.homingsms.core.model.api

import com.homingsms.core.model.MessagePriority
import com.homingsms.core.model.MessageState
import com.homingsms.core.model.MessageType
import com.homingsms.core.model.WebhookDeliveryStatus
import com.homingsms.core.model.WebhookEventType
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ApiModelTest {
    @Test
    fun signatureMatchesTheDocumentedScheme() {
        val header = WebhookSignature.sign("whsec_test", 1_700_000_000, """{"id":"evt_1"}""")
        assertEquals("t=1700000000,v1=c89214b5b5da833daed6f0b8c5bb6bd58cea9022bd80ccc78230f3942d632925", header)
    }

    @Test
    fun signatureVerificationChecksBodyTimeAndSecret() {
        val body = """{"id":"evt_1","type":"webhook.test"}"""
        val header = WebhookSignature.sign("whsec_a", 1_000, body)
        assertTrue(WebhookSignature.verify(header, body, "whsec_a", 1_100))
        assertFalse(WebhookSignature.verify(header, body, "whsec_b", 1_100))
        assertFalse(WebhookSignature.verify(header, "$body ", "whsec_a", 1_100))
        assertFalse(WebhookSignature.verify(header, body, "whsec_a", 1_000 + WebhookSignature.TOLERANCE_SECONDS + 1))
        assertTrue(WebhookSignature.verify("t=1000,v1=deadbeef,${header.substringAfter(',')}", body, "whsec_a", 1_000))
    }

    @Test
    fun apiKeyHashUsesPepperedHmac() {
        val pepper = ByteArray(32) { it.toByte() }
        val key = "hk_live_abcdefghijklmnopqrstuvwxyz012345"
        assertTrue(ApiKeyFormat.looksValid(key))
        assertEquals("63a8ee2c1cc8cc5fa42770847a095a4df9ae660a514206f7594316c26f714d65", ApiKeyFormat.hash(pepper, key))
        assertTrue(ApiKeyFormat.matches(pepper, key, ApiKeyFormat.hash(pepper, key)))
        assertFalse(ApiKeyFormat.matches(ByteArray(32), key, ApiKeyFormat.hash(pepper, key)))
        assertEquals("hk_live_abcd", ApiKeyFormat.displayPrefix(key))
        assertFalse(ApiKeyFormat.looksValid("hk_test_abcdefghijklmnopqrstuvwxyz012345"))
        assertFalse(ApiKeyFormat.looksValid("hk_live_short"))
    }

    @Test
    fun enumsUseContractValues() {
        assertEquals(
            listOf("scheduled", "pending", "leased", "queued", "sending", "sent", "delivered", "failed", "cancelled", "expired"),
            MessageState.entries.map { it.value }
        )
        assertEquals(listOf("sms", "data", "mms"), MessageType.entries.map { it.value })
        assertEquals(listOf("normal", "otp"), MessagePriority.entries.map { it.value })
        assertEquals(listOf("pending", "retrying", "succeeded", "failed"), WebhookDeliveryStatus.entries.map { it.value })
        assertEquals(
            listOf(
                "message.sent",
                "message.delivered",
                "message.failed",
                "inbox.received",
                "device.online",
                "device.offline",
                "opt_out.received"
            ),
            WebhookEventType.entries.filter { it.subscribable }.map { it.value }
        )
    }

    @Test
    fun messageJsonHasEveryRequiredContractKeyWithExplicitNulls() {
        val json = ApiJson.default.encodeToJsonElement(MessageDto.serializer(), sampleMessage()).jsonObject
        assertEquals(MESSAGE_REQUIRED, json.keys)
        assertEquals("null", json["sentAt"].toString())
        assertEquals("\"sms\"", json["type"].toString())
    }

    @Test
    fun messageDetailFlattensEvents() {
        val detail = MessageDetailDto(
            sampleMessage(),
            listOf(MessageEventDto(MessageState.Pending, "2026-11-12T10:15:30.000Z", "dev_1", null, null))
        )
        val encoded = ApiJson.default.encodeToString(MessageDetailDto.serializer(), detail)
        val json = ApiJson.default.parseToJsonElement(encoded).jsonObject
        assertEquals(MESSAGE_REQUIRED + "events", json.keys)
        assertEquals(setOf("state", "at", "deviceId", "simSlot", "error"), json["events"]!!.jsonArray.first().jsonObject.keys)
        assertEquals(detail, ApiJson.default.decodeFromString(MessageDetailDto.serializer(), encoded))
    }

    @Test
    fun inboxWebhookAndErrorShapesMatchContract() {
        val inbox = InboxMessageDto("inb_1", MessageType.Sms, "+1555", null, "dev_1", 0, "hi", null, null, null, emptyList(), NOW, NOW)
        assertEquals(
            setOf(
                "id", "type", "from", "to", "deviceId", "simSlot", "text", "data", "port", "subject", "attachments",
                "receivedAt", "createdAt"
            ),
            keys(ApiJson.default.encodeToJsonElement(InboxMessageDto.serializer(), inbox))
        )
        val endpoint = WebhookEndpointDto("whk_1", "https://a.com", null, listOf(WebhookEventType.MessageSent), true, NOW, NOW)
        assertEquals(
            setOf("id", "url", "description", "events", "enabled", "createdAt", "updatedAt"),
            keys(ApiJson.default.encodeToJsonElement(WebhookEndpointDto.serializer(), endpoint))
        )
        val delivery =
            WebhookDeliveryDto(
                "whd_1", "whk_1", "evt_1", WebhookEventType.WebhookTest, WebhookDeliveryStatus.Pending, 0,
                null, null, null, null, null, NOW, NOW
            )
        assertEquals(
            setOf(
                "id", "endpointId", "eventId", "eventType", "status", "attempts", "lastStatusCode", "lastResponseBody", "lastError",
                "nextAttemptAt", "deliveredAt", "createdAt", "updatedAt"
            ),
            keys(ApiJson.default.encodeToJsonElement(WebhookDeliveryDto.serializer(), delivery))
        )
        val envelope = WebhookEnvelope("evt_1", WebhookEventType.InboxReceived, NOW, "acc_1", JsonObject(emptyMap()))
        val envelopeJson = ApiJson.default.encodeToJsonElement(WebhookEnvelope.serializer(), envelope).jsonObject
        assertEquals(setOf("id", "type", "createdAt", "accountId", "data"), envelopeJson.keys)
        assertEquals("\"inbox.received\"", envelopeJson["type"].toString())
    }

    @Test
    fun isoTimestampsAreUtcWithMillis() {
        assertEquals("2026-11-12T10:15:30.000Z", Iso8601.format(1_794_478_530_000))
        assertEquals(1_794_478_530_000, Iso8601.parse("2026-11-12T10:15:30.000Z"))
        assertEquals(1_794_478_530_000, Iso8601.parse("2026-11-12T11:15:30+01:00"))
    }

    private fun keys(element: kotlinx.serialization.json.JsonElement) = element.jsonObject.keys

    private fun sampleMessage() = MessageDto(
        id = "msg_1", type = MessageType.Sms, to = "+15550001", text = "hi", data = null, port = null, subject = null,
        attachments = emptyList(), state = MessageState.Pending, priority = MessagePriority.Normal, deviceId = "dev_1",
        simSlot = null, idempotencyKey = null, scheduleAt = null, validUntil = null, deliveryReport = true, metadata = emptyMap(),
        attempts = 0, partsCount = null, error = null, createdAt = NOW, updatedAt = NOW, sentAt = null, deliveredAt = null, failedAt = null
    )

    private companion object {
        const val NOW = "2026-11-12T10:15:30.000Z"
        val MESSAGE_REQUIRED = setOf(
            "id", "type", "to", "text", "data", "port", "subject", "attachments", "state", "priority", "deviceId", "simSlot",
            "idempotencyKey", "scheduleAt", "validUntil", "deliveryReport", "metadata", "attempts", "partsCount", "error",
            "createdAt", "updatedAt", "sentAt", "deliveredAt", "failedAt"
        )
    }
}
