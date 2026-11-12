package com.homingsms.app.serialization

import com.homingsms.app.BuildConfig
import com.homingsms.app.domain.HealthResponse
import com.homingsms.app.modules.health.domain.CheckResult
import com.homingsms.app.modules.health.domain.Status
import com.homingsms.app.modules.webhooks.domain.WebHookEvent
import com.homingsms.app.modules.webhooks.domain.WebHookEventDTO
import com.homingsms.app.modules.webhooks.payload.SmsBatchReceivedPayload
import com.homingsms.app.modules.webhooks.payload.SmsEventPayload
import java.util.Date
import java.util.TimeZone
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class WireShapeTest {
    private lateinit var defaultTimeZone: TimeZone

    @Before
    fun pinTimeZone() {
        defaultTimeZone = TimeZone.getDefault()
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"))
    }

    @After
    fun restoreTimeZone() {
        TimeZone.setDefault(defaultTimeZone)
    }

    private fun assertJson(expected: String, actual: JsonElement) {
        assertEquals(Json.parseToJsonElement(expected), actual)
    }

    private fun received(recipient: String? = null) = SmsEventPayload.SmsReceived(
        messageId = "m1",
        sender = "+1000",
        recipient = recipient,
        simNumber = 1,
        message = "hello",
        receivedAt = Date(0)
    )

    @Test
    fun smsReceivedPayloadKeepsLegacyPhoneNumberAndOmitsNulls() {
        assertJson(
            """{
                "messageId": "m1",
                "sender": "+1000",
                "simNumber": 1,
                "phoneNumber": "+1000",
                "message": "hello",
                "receivedAt": "1970-01-01T00:00:00.000Z"
            }""",
            received().toJsonElement()
        )
    }

    @Test
    fun smsSentPayloadUsesRecipientAsPhoneNumber() {
        val payload = SmsEventPayload.SmsSent(
            messageId = "m2",
            sender = null,
            recipient = "+2000",
            simNumber = null,
            partsCount = 2,
            sentAt = Date(1_000)
        )

        assertJson(
            """{
                "messageId": "m2",
                "recipient": "+2000",
                "phoneNumber": "+2000",
                "partsCount": 2,
                "sentAt": "1970-01-01T00:00:01.000Z"
            }""",
            payload.toJsonElement()
        )
    }

    @Test
    fun batchPayloadHasNoTypeDiscriminator() {
        val batch = SmsBatchReceivedPayload(messages = listOf(received(recipient = "+3000")))

        assertJson(
            """{
                "messages": [{
                    "messageId": "m1",
                    "sender": "+1000",
                    "recipient": "+3000",
                    "simNumber": 1,
                    "phoneNumber": "+1000",
                    "message": "hello",
                    "receivedAt": "1970-01-01T00:00:00.000Z"
                }]
            }""",
            batch.toJsonElement()
        )
    }

    @Test
    fun webhookEventEnvelope() {
        val event = WebHookEventDTO(
            id = "e1",
            webhookId = "w1",
            event = WebHookEvent.SmsReceived,
            deviceId = "d1",
            payload = received().toJsonElement()
        )

        val encoded = AppJson.default.encodeToString(WebHookEventDTO.serializer(), event)

        assertJson(
            """{
                "id": "e1",
                "webhookId": "w1",
                "event": "sms:received",
                "deviceId": "d1",
                "payload": {
                    "messageId": "m1",
                    "sender": "+1000",
                    "simNumber": 1,
                    "phoneNumber": "+1000",
                    "message": "hello",
                    "receivedAt": "1970-01-01T00:00:00.000Z"
                }
            }""",
            Json.parseToJsonElement(encoded)
        )
    }

    @Test
    fun healthResponseShape() {
        val health = HealthResponse(
            status = Status.WARN,
            checks = mapOf(
                "battery:level" to CheckResult(Status.PASS, 80, "percent", "Battery level")
            )
        )

        assertJson(
            """{
                "status": "warn",
                "checks": {
                    "battery:level": {
                        "status": "pass",
                        "observedValue": 80,
                        "observedUnit": "percent",
                        "description": "Battery level"
                    }
                },
                "version": "${BuildConfig.VERSION_NAME}",
                "releaseId": ${BuildConfig.VERSION_CODE}
            }""",
            AppJson.default.encodeToJsonElement(HealthResponse.serializer(), health)
        )
    }

    @Test
    fun pingWebhookPayloadWrapsHealth() {
        val health = HealthResponse(status = Status.PASS, checks = emptyMap())

        assertJson(
            """{
                "health": {
                    "status": "pass",
                    "checks": {},
                    "version": "${BuildConfig.VERSION_NAME}",
                    "releaseId": ${BuildConfig.VERSION_CODE}
                }
            }""",
            mapOf("health" to health).toJsonElement()
        )
    }

    @Test
    fun mapsDropNullEntriesAndKeepNesting() {
        val settings = mapOf(
            "messages" to mapOf("send_interval_min" to 5, "limit_period" to "Disabled", "log_lifetime_days" to null),
            "gateway" to null,
            "ping" to mapOf("interval_seconds" to null)
        )

        assertJson(
            """{
                "messages": {"send_interval_min": 5, "limit_period": "Disabled"},
                "ping": {}
            }""",
            settings.toJsonElement()
        )
    }

    @Test
    fun plainValuesUseDoublesForNumbers() {
        val parsed = Json.parseToJsonElement(
            """{"n": 30, "f": 1.5, "b": true, "s": "x", "z": null, "o": {"a": [1, "2"]}}"""
        ) as kotlinx.serialization.json.JsonObject

        assertEquals(
            mapOf(
                "n" to 30.0,
                "f" to 1.5,
                "b" to true,
                "s" to "x",
                "z" to null,
                "o" to mapOf("a" to listOf(1.0, "2"))
            ),
            parsed.toPlainMap()
        )
    }
}
