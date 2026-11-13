package com.homingsms.core.server

import com.homingsms.core.model.MessagePriority
import com.homingsms.core.model.MessageState
import com.homingsms.core.model.MessageType
import com.homingsms.core.model.Scope
import com.homingsms.core.model.WebhookDeliveryStatus
import com.homingsms.core.model.WebhookEventType
import com.homingsms.core.model.api.ApiError
import com.homingsms.core.model.api.AttachmentInput
import com.homingsms.core.model.api.HealthDto
import com.homingsms.core.model.api.InboxAttachmentDto
import com.homingsms.core.model.api.InboxMessageDto
import com.homingsms.core.model.api.InboxPage
import com.homingsms.core.model.api.MessageAttachmentDto
import com.homingsms.core.model.api.MessageDetailDto
import com.homingsms.core.model.api.MessageDto
import com.homingsms.core.model.api.MessageErrorDto
import com.homingsms.core.model.api.MessageEventDto
import com.homingsms.core.model.api.MessagePage
import com.homingsms.core.model.api.SendMessageRequest
import com.homingsms.core.model.api.WebhookDeliveryDto
import com.homingsms.core.model.api.WebhookDeliveryPage
import com.homingsms.core.model.api.WebhookEndpointDto
import com.homingsms.core.model.api.WebhookEndpointList
import com.homingsms.core.model.api.WebhookEndpointWithSecretDto
import com.homingsms.core.model.api.WebhookSecretDto
import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PolymorphicKind
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.SerialKind
import kotlinx.serialization.descriptors.StructureKind
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject

object OpenApiDocument {
    const val SERVER_URL = "/api/v1"

    internal val components: Map<String, KSerializer<*>> = linkedMapOf(
        "Error" to ApiError.serializer(),
        "SendMessageRequest" to SendMessageRequest.serializer(),
        "MessageAttachmentInput" to AttachmentInput.serializer(),
        "Message" to MessageDto.serializer(),
        "MessageDetail" to MessageDetailDto.serializer(),
        "MessageEvent" to MessageEventDto.serializer(),
        "MessageError" to MessageErrorDto.serializer(),
        "MessageAttachment" to MessageAttachmentDto.serializer(),
        "MessagePage" to MessagePage.serializer(),
        "InboxMessage" to InboxMessageDto.serializer(),
        "InboxAttachment" to InboxAttachmentDto.serializer(),
        "InboxPage" to InboxPage.serializer(),
        "WebhookEndpoint" to WebhookEndpointDto.serializer(),
        "WebhookEndpointWithSecret" to WebhookEndpointWithSecretDto.serializer(),
        "WebhookEndpointList" to WebhookEndpointList.serializer(),
        "CreateWebhookRequest" to CreateWebhookRequest.serializer(),
        "UpdateWebhookRequest" to UpdateWebhookRequest.serializer(),
        "WebhookSecret" to WebhookSecretDto.serializer(),
        "WebhookDelivery" to WebhookDeliveryDto.serializer(),
        "WebhookDeliveryPage" to WebhookDeliveryPage.serializer(),
        "Health" to HealthDto.serializer(),
        "MessageState" to MessageState.serializer(),
        "MessageType" to MessageType.serializer(),
        "MessagePriority" to MessagePriority.serializer(),
        "WebhookEventType" to WebhookEventType.serializer(),
        "WebhookDeliveryStatus" to WebhookDeliveryStatus.serializer()
    )

    fun build(routes: List<RouteSpec>, version: String): JsonObject {
        val generator = SchemaGenerator(components)
        val paths = routes.groupBy { it.path }.toSortedMap().mapValues { (_, specs) ->
            JsonObject(specs.associate { it.method.value.lowercase() to operation(it) })
        }
        return buildJsonObject {
            put("openapi", "3.1.0")
            putJsonObject("info") {
                put("title", "Homing Local API")
                put("version", version)
                put(
                    "description",
                    "The API served by the Homing app in local mode. It mirrors the Homing Cloud public API for messages, " +
                        "inbox, webhooks and health. Authenticate with Authorization: Bearer hk_live_... keys created in the app."
                )
            }
            putJsonArray("servers") { add(buildJsonObject { put("url", SERVER_URL) }) }
            putJsonArray("security") { add(buildJsonObject { putJsonArray("apiKey") {} }) }
            putJsonArray("tags") {
                routes.map { it.tag }.distinct().forEach { tag -> add(buildJsonObject { put("name", tag) }) }
            }
            put("paths", JsonObject(paths))
            putJsonObject("components") {
                putJsonObject("securitySchemes") {
                    putJsonObject("apiKey") {
                        put("type", "http")
                        put("scheme", "bearer")
                        put("bearerFormat", "hk_live_<secret>")
                        put("description", "API key created on the phone. Scopes: ${Scope.entries.joinToString { it.value }}.")
                    }
                }
                put("schemas", generator.all())
            }
        }
    }

    private fun operation(spec: RouteSpec): JsonObject = buildJsonObject {
        put("operationId", spec.operationId)
        put("summary", spec.summary)
        putJsonArray("tags") { add(JsonPrimitive(spec.tag)) }
        putJsonArray("security") {
            spec.scope?.let { scope ->
                add(buildJsonObject { putJsonArray("apiKey") { add(JsonPrimitive(scope.value)) } })
            }
        }
        if (spec.params.isNotEmpty()) {
            putJsonArray("parameters") {
                spec.params.forEach { param ->
                    add(
                        buildJsonObject {
                            put("name", param.name)
                            put("in", param.location)
                            put("required", param.required)
                            param.description?.let { put("description", it) }
                            put("schema", param.schema)
                        }
                    )
                }
            }
        }
        spec.requestSchema?.let { schema ->
            putJsonObject("requestBody") {
                put("required", true)
                putJsonObject("content") { putJsonObject("application/json") { put("schema", Schemas.ref(schema)) } }
            }
        }
        putJsonObject("responses") {
            spec.responses.sortedBy { it.status.value }.forEach { response -> put(response.status.value.toString(), response(response)) }
        }
    }

    private fun response(response: ResponseSpec): JsonObject = buildJsonObject {
        put("description", response.description)
        if (response.headers.isNotEmpty()) {
            putJsonObject("headers") {
                response.headers.forEach { header -> putJsonObject(header) { put("schema", Schemas.string()) } }
            }
        }
        response.schema?.let { schema ->
            val body = if (schema == "Binary") Schemas.string(format = "binary") else Schemas.ref(schema)
            putJsonObject("content") { putJsonObject(response.contentType) { put("schema", body) } }
        }
    }
}

internal class SchemaGenerator(private val roots: Map<String, KSerializer<*>>) {
    private val namesBySerialName: Map<String, String> = roots.entries.associate { (name, serializer) ->
        serializer.descriptor.serialName.removeSuffix("?") to name
    }

    fun all(): JsonObject = JsonObject(roots.mapValues { (name, serializer) -> definition(name, serializer.descriptor) })

    private fun definition(name: String, descriptor: SerialDescriptor): JsonObject = when {
        name == "MessageDetail" -> buildJsonObject {
            putJsonArray("allOf") {
                add(Schemas.ref("Message"))
                add(
                    buildJsonObject {
                        put("type", "object")
                        putJsonArray("required") { add(JsonPrimitive("events")) }
                        putJsonObject("properties") {
                            putJsonObject("events") {
                                put("type", "array")
                                put("description", "State timeline, oldest first.")
                                put("items", Schemas.ref("MessageEvent"))
                            }
                        }
                    }
                )
            }
        }

        descriptor.kind == SerialKind.ENUM -> enumSchema(descriptor)

        else -> objectSchema(descriptor)
    }

    private fun enumSchema(descriptor: SerialDescriptor): JsonObject = buildJsonObject {
        put("type", "string")
        put("enum", JsonArray((0 until descriptor.elementsCount).map { JsonPrimitive(descriptor.getElementName(it)) }))
    }

    private fun objectSchema(descriptor: SerialDescriptor): JsonObject = buildJsonObject {
        put("type", "object")
        val required = (0 until descriptor.elementsCount).filter { !descriptor.isElementOptional(it) }
        if (required.isNotEmpty()) {
            put("required", JsonArray(required.map { JsonPrimitive(descriptor.getElementName(it)) }))
        }
        putJsonObject("properties") {
            for (index in 0 until descriptor.elementsCount) {
                put(descriptor.getElementName(index), schemaFor(descriptor.getElementDescriptor(index)))
            }
        }
    }

    private fun schemaFor(descriptor: SerialDescriptor): JsonElement {
        val base = baseSchema(descriptor)
        return if (descriptor.isNullable) nullable(base) else base
    }

    private fun baseSchema(descriptor: SerialDescriptor): JsonObject {
        val serialName = descriptor.serialName.removeSuffix("?")
        namesBySerialName[serialName]?.let { return Schemas.ref(it) }
        return when (val kind = descriptor.kind) {
            is PrimitiveKind -> primitive(kind)

            SerialKind.ENUM -> enumSchema(descriptor)

            StructureKind.LIST -> buildJsonObject {
                put("type", "array")
                put("items", schemaFor(descriptor.getElementDescriptor(0)))
            }

            StructureKind.MAP -> buildJsonObject {
                put("type", "object")
                put("additionalProperties", schemaFor(descriptor.getElementDescriptor(1)))
            }

            StructureKind.CLASS, StructureKind.OBJECT -> objectSchema(descriptor)

            is PolymorphicKind, SerialKind.CONTEXTUAL -> JsonObject(emptyMap())
        }
    }

    private fun primitive(kind: PrimitiveKind): JsonObject = buildJsonObject {
        put(
            "type",
            when (kind) {
                PrimitiveKind.BOOLEAN -> "boolean"
                PrimitiveKind.BYTE, PrimitiveKind.SHORT, PrimitiveKind.INT, PrimitiveKind.LONG -> "integer"
                PrimitiveKind.FLOAT, PrimitiveKind.DOUBLE -> "number"
                PrimitiveKind.CHAR, PrimitiveKind.STRING -> "string"
            }
        )
    }

    private fun nullable(schema: JsonObject): JsonObject {
        val type = schema["type"]
        return if (type is JsonPrimitive) {
            JsonObject(
                schema + (
                    "type" to buildJsonArray {
                        add(type)
                        add(JsonPrimitive("null"))
                    }
                    )
            )
        } else {
            buildJsonObject {
                putJsonArray("oneOf") {
                    add(schema)
                    add(buildJsonObject { put("type", "null") })
                }
            }
        }
    }
}
