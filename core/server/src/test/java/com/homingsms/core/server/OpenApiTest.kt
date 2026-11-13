package com.homingsms.core.server

import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.plugin
import io.ktor.server.routing.RoutingNode
import io.ktor.server.routing.RoutingRoot
import io.ktor.server.routing.getAllRoutes
import io.ktor.server.testing.testApplication
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class OpenApiTest {
    @Test
    fun documentMatchesRegisteredRoutes() = testApplication {
        var routes: List<RoutingNode> = emptyList()
        application {
            installLocalApi(testConfig(), "9.9.9")
            routes = plugin(RoutingRoot).getAllRoutes()
        }
        val response = client.get("/api/v1/docs/openapi.json")
        assertEquals(HttpStatusCode.OK, response.status)
        val document = Json.parseToJsonElement(response.bodyAsText()).jsonObject

        val served = routes.mapNotNull { route ->
            val text = route.toString()
            val method = Regex("""\(method:([A-Z]+)\)$""").find(text)?.groupValues?.get(1) ?: return@mapNotNull null
            val path = text.substringBefore("/(method:").removePrefix(OpenApiDocument.SERVER_URL)
            if (path == DOCS_PATH) null else "$method $path"
        }.toSet()
        val documented = document["paths"]!!.jsonObject.flatMap { (path, operations) ->
            operations.jsonObject.keys.map { "${it.uppercase()} $path" }
        }.toSet()

        assertEquals(served, documented)
        assertEquals(EXPECTED_OPERATIONS, documented)
        assertEquals("9.9.9", document["info"]!!.jsonObject["version"]!!.jsonPrimitive.content)
    }

    @Test
    fun everyReferenceResolves() = testApplication {
        application { installLocalApi(testConfig(), "1") }
        val document = Json.parseToJsonElement(client.get("/api/v1/docs/openapi.json").bodyAsText()).jsonObject
        val schemas = document["components"]!!.jsonObject["schemas"]!!.jsonObject.keys
        val refs = mutableSetOf<String>()
        collectRefs(document, refs)
        val missing = refs.map { it.removePrefix("#/components/schemas/") }.filterNot { it in schemas }
        assertTrue("Unresolved refs: $missing", missing.isEmpty())
    }

    @Test
    fun schemasMatchTheContract() = testApplication {
        application { installLocalApi(testConfig(), "1") }
        val document = Json.parseToJsonElement(client.get("/api/v1/docs/openapi.json").bodyAsText()).jsonObject
        val schemas = document["components"]!!.jsonObject["schemas"]!!.jsonObject
        fun required(name: String) = schemas[name]!!.jsonObject["required"]!!.jsonArray.map { it.jsonPrimitive.content }.toSet()

        assertEquals(CONTRACT_MESSAGE, required("Message"))
        assertEquals(
            setOf(
                "id", "type", "from", "to", "deviceId", "simSlot", "text", "data", "port", "subject", "attachments",
                "receivedAt", "createdAt"
            ),
            required("InboxMessage")
        )
        assertEquals(setOf("statusCode", "message", "error", "timestamp", "path"), required("Error"))
        assertEquals(setOf("data", "nextCursor"), required("MessagePage"))
        assertEquals(setOf("id", "url", "description", "events", "enabled", "createdAt", "updatedAt"), required("WebhookEndpoint"))
        assertEquals(setOf("status", "version", "time", "checks"), required("Health"))
        val messageState = schemas["MessageState"]!!.jsonObject["enum"]!!.jsonArray.map { it.jsonPrimitive.content }
        assertEquals(
            listOf("scheduled", "pending", "leased", "queued", "sending", "sent", "delivered", "failed", "cancelled", "expired"),
            messageState
        )
        val sentAt = schemas["Message"]!!.jsonObject["properties"]!!.jsonObject["sentAt"]!!.jsonObject["type"]!!.jsonArray
        assertEquals(listOf("string", "null"), sentAt.map { it.jsonPrimitive.content })
        val detail = schemas["MessageDetail"]!!.jsonObject["allOf"]!!.jsonArray
        assertEquals("#/components/schemas/Message", detail.first().jsonObject["\$ref"]!!.jsonPrimitive.content)
    }

    @Test
    fun operationsDeclareScopes() = testApplication {
        application { installLocalApi(testConfig(), "1") }
        val document = Json.parseToJsonElement(client.get("/api/v1/docs/openapi.json").bodyAsText()).jsonObject
        val send = document["paths"]!!.jsonObject["/messages"]!!.jsonObject["post"]!!.jsonObject
        assertEquals("messages:send", send["security"]!!.jsonArray.single().jsonObject["apiKey"]!!.jsonArray.single().jsonPrimitive.content)
        val health = document["paths"]!!.jsonObject["/health"]!!.jsonObject["get"]!!.jsonObject
        assertEquals(0, health["security"]!!.jsonArray.size)
    }

    private fun collectRefs(element: JsonElement, refs: MutableSet<String>) {
        when (element) {
            is JsonObject -> element.forEach { (key, value) ->
                if (key == "\$ref") refs += value.jsonPrimitive.content else collectRefs(value, refs)
            }

            is JsonArray -> element.forEach { collectRefs(it, refs) }

            else -> Unit
        }
    }

    private companion object {
        val EXPECTED_OPERATIONS = setOf(
            "GET /health",
            "POST /messages",
            "GET /messages",
            "GET /messages/{messageId}",
            "POST /messages/{messageId}/cancel",
            "GET /inbox",
            "GET /inbox/{inboxMessageId}",
            "GET /inbox/{inboxMessageId}/attachments/{attachmentId}",
            "GET /webhooks",
            "POST /webhooks",
            "GET /webhooks/{webhookId}",
            "PATCH /webhooks/{webhookId}",
            "DELETE /webhooks/{webhookId}",
            "POST /webhooks/{webhookId}/rotate-secret",
            "POST /webhooks/{webhookId}/test",
            "GET /webhooks/{webhookId}/deliveries",
            "POST /webhooks/{webhookId}/deliveries/{deliveryId}/redeliver"
        )
        val CONTRACT_MESSAGE = setOf(
            "id", "type", "to", "text", "data", "port", "subject", "attachments", "state", "priority", "deviceId", "simSlot",
            "idempotencyKey", "scheduleAt", "validUntil", "deliveryReport", "metadata", "attempts", "partsCount", "error",
            "createdAt", "updatedAt", "sentAt", "deliveredAt", "failedAt"
        )
    }
}
