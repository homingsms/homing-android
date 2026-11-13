package com.homingsms.core.server

import com.homingsms.core.model.MessageState
import com.homingsms.core.model.api.HealthDto
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.patch
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.client.statement.readRawBytes
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.server.testing.ApplicationTestBuilder
import io.ktor.server.testing.testApplication
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LocalApiTest {
    private val messages = FakeMessages()
    private val webhooks = FakeWebhooks()

    private fun api(
        config: LocalApiConfig = testConfig(messages = messages, webhooks = webhooks),
        block: suspend ApplicationTestBuilder.() -> Unit
    ) = testApplication {
        application { installLocalApi(config, "1.0.0") }
        block()
    }

    private fun HttpRequestBuilder.auth(key: String) = header(HttpHeaders.Authorization, "Bearer $key")

    private fun HttpRequestBuilder.json(body: String) {
        contentType(ContentType.Application.Json)
        setBody(body)
    }

    private suspend fun HttpResponse.json(): JsonObject = Json.parseToJsonElement(bodyAsText()).jsonObject

    private fun assertError(body: JsonObject, status: Int, path: String) {
        assertEquals(setOf("statusCode", "message", "error", "timestamp", "path"), body.keys)
        assertEquals(status, body["statusCode"]!!.jsonPrimitive.int)
        assertEquals(path, body["path"]!!.jsonPrimitive.content)
        assertEquals("2026-11-12T10:15:30.000Z", body["timestamp"]!!.jsonPrimitive.content)
    }

    @Test
    fun healthNeedsNoKey() = api {
        val response = client.get("/api/v1/health")
        assertEquals(HttpStatusCode.OK, response.status)
        assertEquals("ok", response.json()["status"]!!.jsonPrimitive.content)
    }

    @Test
    fun degradedHealthReturns503() = api(testConfig(health = HealthDto("degraded", "1", "t", mapOf("telephony" to "down")))) {
        val response = client.get("/api/v1/health")
        assertEquals(HttpStatusCode.ServiceUnavailable, response.status)
        assertEquals("degraded", response.json()["status"]!!.jsonPrimitive.content)
    }

    @Test
    fun missingKeyIs401WithErrorShape() = api {
        val response = client.get("/api/v1/messages")
        assertEquals(HttpStatusCode.Unauthorized, response.status)
        assertError(response.json(), 401, "/api/v1/messages")
    }

    @Test
    fun unknownKeyIs401() = api {
        val response = client.get("/api/v1/messages") { auth("hk_live_unknown") }
        assertEquals(HttpStatusCode.Unauthorized, response.status)
        assertEquals("Invalid API key", response.json()["message"]!!.jsonPrimitive.content)
    }

    @Test
    fun missingScopeIs403() = api {
        val response = client.get("/api/v1/messages") { auth(SEND_KEY) }
        assertEquals(HttpStatusCode.Forbidden, response.status)
        assertError(response.json(), 403, "/api/v1/messages")
        assertEquals(HttpStatusCode.Forbidden, client.get("/api/v1/webhooks") { auth(READ_KEY) }.status)
        assertEquals(HttpStatusCode.Forbidden, client.post("/api/v1/messages") { auth(READ_KEY) }.status)
    }

    @Test
    fun sendReturns202WithMessageDetail() = api {
        val response = client.post("/api/v1/messages") {
            auth(SEND_KEY)
            json("""{"type":"sms","to":"+15550001","text":"Hi","metadata":{"order":"7"}}""")
        }
        assertEquals(HttpStatusCode.Accepted, response.status)
        val body = response.json()
        assertEquals("msg_1", body["id"]!!.jsonPrimitive.content)
        assertEquals("pending", body["state"]!!.jsonPrimitive.content)
        assertEquals("dev_local", body["deviceId"]!!.jsonPrimitive.content)
        assertEquals(JsonNull, body["sentAt"])
        assertEquals("7", body["metadata"]!!.jsonObject["order"]!!.jsonPrimitive.content)
        assertEquals(1, body["events"]!!.jsonArray.size)
    }

    @Test
    fun idempotentReplayAndConflict() = api {
        val body = """{"type":"sms","to":"+15550001","text":"Hi","idempotencyKey":"k1"}"""
        val first = client.post("/api/v1/messages") {
            auth(SEND_KEY)
            json(body)
        }
        assertEquals(HttpStatusCode.Accepted, first.status)
        val replay = client.post("/api/v1/messages") {
            auth(SEND_KEY)
            json(body)
        }
        assertEquals(HttpStatusCode.OK, replay.status)
        assertEquals("true", replay.headers["Idempotent-Replayed"])
        assertEquals(first.json()["id"], replay.json()["id"])
        val conflict = client.post("/api/v1/messages") {
            auth(SEND_KEY)
            json("""{"type":"sms","to":"+15550001","text":"Other","idempotencyKey":"k1"}""")
        }
        assertEquals(HttpStatusCode.Conflict, conflict.status)
        assertEquals(1, messages.records.size)
    }

    @Test
    fun validationErrorsAreListed() = api {
        val response = client.post("/api/v1/messages") {
            auth(SEND_KEY)
            json("""{"type":"sms","to":"abc"}""")
        }
        assertEquals(HttpStatusCode.BadRequest, response.status)
        val message = response.json()["message"]
        assertTrue(message is JsonArray)
        assertEquals(2, message!!.jsonArray.size)
    }

    @Test
    fun malformedJsonIs400() = api {
        val response = client.post("/api/v1/messages") {
            auth(SEND_KEY)
            json("{not json")
        }
        assertEquals(HttpStatusCode.BadRequest, response.status)
        assertError(response.json(), 400, "/api/v1/messages")
    }

    @Test
    fun foreignDeviceIs404AndOwnCloudIdIsAccepted() = api {
        val foreign = client.post("/api/v1/messages") {
            auth(SEND_KEY)
            json("""{"type":"sms","to":"+15550001","text":"Hi","deviceId":"dev_other"}""")
        }
        assertEquals(HttpStatusCode.NotFound, foreign.status)
        val own = client.post("/api/v1/messages") {
            auth(SEND_KEY)
            json("""{"type":"sms","to":"+15550001","text":"Hi","deviceId":"dev_cloud","simSlot":1}""")
        }
        assertEquals(HttpStatusCode.Accepted, own.status)
        assertEquals(1, messages.lastRequest?.simSlot)
    }

    @Test
    fun oversizedBodyIs413() = api {
        val response = client.post("/api/v1/messages") {
            auth(SEND_KEY)
            json("""{"type":"sms","to":"+15550001","text":"${"x".repeat(2 * 1024 * 1024)}"}""")
        }
        assertEquals(HttpStatusCode.PayloadTooLarge, response.status)
    }

    @Test
    fun listsPagesWithCursor() = api {
        repeat(3) { index ->
            messages.records["msg_$index"] =
                FakeMessages.record("msg_$index", "+1555000$index", "t", MessageState.Pending, NOW + index)
        }
        val first = client.get("/api/v1/messages?limit=2") { auth(ADMIN_KEY) }.json()
        assertEquals(listOf("msg_2", "msg_1"), first["data"]!!.jsonArray.map { it.jsonObject["id"]!!.jsonPrimitive.content })
        val cursor = first["nextCursor"]!!.jsonPrimitive.content
        val second = client.get("/api/v1/messages?limit=2&cursor=$cursor") { auth(ADMIN_KEY) }.json()
        assertEquals(listOf("msg_0"), second["data"]!!.jsonArray.map { it.jsonObject["id"]!!.jsonPrimitive.content })
        assertEquals(JsonNull, second["nextCursor"])
        assertEquals("+15550001", first["data"]!!.jsonArray.last().jsonObject["to"]!!.jsonPrimitive.content)
    }

    @Test
    fun listRejectsBadQueryAndFiltersOtherDevices() = api {
        assertEquals(HttpStatusCode.BadRequest, client.get("/api/v1/messages?limit=101") { auth(READ_KEY) }.status)
        assertEquals(HttpStatusCode.BadRequest, client.get("/api/v1/messages?state=lost") { auth(READ_KEY) }.status)
        assertEquals(HttpStatusCode.BadRequest, client.get("/api/v1/messages?cursor=@@@") { auth(READ_KEY) }.status)
        assertEquals(HttpStatusCode.BadRequest, client.get("/api/v1/messages?createdFrom=yesterday") { auth(READ_KEY) }.status)
        messages.records["msg_9"] = FakeMessages.record("msg_9", "+15550009", "t", MessageState.Pending, NOW)
        val other = client.get("/api/v1/messages?deviceId=dev_other") { auth(READ_KEY) }.json()
        assertEquals(0, other["data"]!!.jsonArray.size)
    }

    @Test
    fun getAndCancel() = api {
        messages.records["msg_a"] = FakeMessages.record("msg_a", "+15550001", "t", MessageState.Pending, NOW)
        messages.records["msg_b"] = FakeMessages.record("msg_b", "+15550001", "t", MessageState.Sent, NOW)
        assertEquals(HttpStatusCode.OK, client.get("/api/v1/messages/msg_a") { auth(READ_KEY) }.status)
        val missing = client.get("/api/v1/messages/msg_x") { auth(READ_KEY) }
        assertEquals(HttpStatusCode.NotFound, missing.status)
        assertError(missing.json(), 404, "/api/v1/messages/msg_x")
        val cancelled = client.post("/api/v1/messages/msg_a/cancel") { auth(SEND_KEY) }
        assertEquals(HttpStatusCode.OK, cancelled.status)
        assertEquals("cancelled", cancelled.json()["state"]!!.jsonPrimitive.content)
        assertEquals(HttpStatusCode.Conflict, client.post("/api/v1/messages/msg_b/cancel") { auth(SEND_KEY) }.status)
        assertEquals(HttpStatusCode.NotFound, client.post("/api/v1/messages/msg_x/cancel") { auth(SEND_KEY) }.status)
    }

    @Test
    fun inboxListGetAndAttachment() = api {
        val list = client.get("/api/v1/inbox?type=mms") { auth(READ_KEY) }.json()
        assertEquals(listOf("inb_2"), list["data"]!!.jsonArray.map { it.jsonObject["id"]!!.jsonPrimitive.content })
        val item = client.get("/api/v1/inbox/inb_1") { auth(READ_KEY) }.json()
        assertEquals("Hello", item["text"]!!.jsonPrimitive.content)
        assertEquals("dev_local", item["deviceId"]!!.jsonPrimitive.content)
        val attachment = client.get("/api/v1/inbox/inb_2/attachments/1") { auth(READ_KEY) }
        assertEquals(HttpStatusCode.OK, attachment.status)
        assertEquals(ContentType.Image.PNG, attachment.contentType()?.withoutParameters())
        assertArrayEquals(byteArrayOf(1, 2, 3), attachment.readRawBytes())
        assertEquals(HttpStatusCode.NotFound, client.get("/api/v1/inbox/inb_2/attachments/9") { auth(READ_KEY) }.status)
        assertEquals(HttpStatusCode.NotFound, client.get("/api/v1/inbox/inb_9") { auth(READ_KEY) }.status)
        assertEquals(HttpStatusCode.Forbidden, client.get("/api/v1/inbox") { auth(SEND_KEY) }.status)
    }

    @Test
    fun webhookLifecycle() = api {
        val created = client.post("/api/v1/webhooks") {
            auth(ADMIN_KEY)
            json("""{"url":"https://hooks.example.com/homing","events":["message.sent","inbox.received"],"description":"Main"}""")
        }
        assertEquals(HttpStatusCode.Created, created.status)
        val body = created.json()
        val id = body["id"]!!.jsonPrimitive.content
        assertTrue(body["secret"]!!.jsonPrimitive.content.startsWith("whsec_"))

        val listed = client.get("/api/v1/webhooks") { auth(ADMIN_KEY) }.json()
        assertTrue(listed["data"]!!.jsonArray.single().jsonObject["secret"] == null)

        val patched = client.patch("/api/v1/webhooks/$id") {
            auth(ADMIN_KEY)
            json("""{"description":null,"enabled":false}""")
        }.json()
        assertEquals(JsonNull, patched["description"])
        assertEquals("false", patched["enabled"].toString())

        val rotated = client.post("/api/v1/webhooks/$id/rotate-secret") { auth(ADMIN_KEY) }.json()
        assertEquals("whsec_rotated", rotated["secret"]!!.jsonPrimitive.content)

        val test = client.post("/api/v1/webhooks/$id/test") { auth(ADMIN_KEY) }
        assertEquals(HttpStatusCode.Accepted, test.status)
        val deliveryId = test.json()["id"]!!.jsonPrimitive.content
        val deliveries = client.get("/api/v1/webhooks/$id/deliveries?status=pending") { auth(ADMIN_KEY) }.json()
        assertEquals(1, deliveries["data"]!!.jsonArray.size)
        assertEquals(
            HttpStatusCode.Accepted,
            client.post("/api/v1/webhooks/$id/deliveries/$deliveryId/redeliver") {
                auth(ADMIN_KEY)
            }.status
        )
        assertEquals(HttpStatusCode.NotFound, client.post("/api/v1/webhooks/$id/deliveries/whd_x/redeliver") { auth(ADMIN_KEY) }.status)

        assertEquals(HttpStatusCode.NoContent, client.delete("/api/v1/webhooks/$id") { auth(ADMIN_KEY) }.status)
        assertEquals(HttpStatusCode.NotFound, client.get("/api/v1/webhooks/$id") { auth(ADMIN_KEY) }.status)
    }

    @Test
    fun webhookValidation() = api {
        val cleartext = client.post("/api/v1/webhooks") {
            auth(ADMIN_KEY)
            json("""{"url":"http://example.com/hook","events":["message.sent"]}""")
        }
        assertEquals(HttpStatusCode.BadRequest, cleartext.status)
        val loopback = client.post("/api/v1/webhooks") {
            auth(ADMIN_KEY)
            json("""{"url":"http://127.0.0.1:9000/hook","events":["message.sent"]}""")
        }
        assertEquals(HttpStatusCode.Created, loopback.status)
        val badEvent = client.post("/api/v1/webhooks") {
            auth(ADMIN_KEY)
            json("""{"url":"https://example.com/hook","events":["webhook.test"]}""")
        }
        assertEquals(HttpStatusCode.BadRequest, badEvent.status)
        val duplicate = client.post("/api/v1/webhooks") {
            auth(ADMIN_KEY)
            json("""{"url":"https://example.com/hook","events":["message.sent","message.sent"]}""")
        }
        assertEquals(HttpStatusCode.BadRequest, duplicate.status)
    }

    @Test
    fun unknownRouteUsesErrorShape() = api {
        val response = client.get("/api/v1/nope") { auth(ADMIN_KEY) }
        assertEquals(HttpStatusCode.NotFound, response.status)
        assertError(response.json(), 404, "/api/v1/nope")
    }

    @Test
    fun legacyRoutesAreGone() = api {
        assertEquals(HttpStatusCode.NotFound, client.get("/message") { auth(ADMIN_KEY) }.status)
        assertEquals(HttpStatusCode.NotFound, client.post("/api/v1/auth/token") { auth(ADMIN_KEY) }.status)
    }
}
