package com.homingsms.core.server

import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

internal object Schemas {
    fun string(format: String? = null, maxLength: Int? = null): JsonObject = buildJsonObject {
        put("type", "string")
        format?.let { put("format", it) }
        maxLength?.let { put("maxLength", it) }
    }

    fun integer(minimum: Int? = null, maximum: Int? = null, default: Int? = null): JsonObject = buildJsonObject {
        put("type", "integer")
        minimum?.let { put("minimum", it) }
        maximum?.let { put("maximum", it) }
        default?.let { put("default", it) }
    }

    fun enumOf(values: List<String>): JsonObject = buildJsonObject {
        put("type", "string")
        put("enum", buildJsonArray { values.forEach { add(JsonPrimitive(it)) } })
    }

    fun ref(name: String): JsonObject = buildJsonObject { put("\$ref", "#/components/schemas/$name") }

    fun dateTime(): JsonElement = string(format = "date-time")
}
