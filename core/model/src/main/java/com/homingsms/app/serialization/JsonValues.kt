package com.homingsms.app.serialization

import java.util.Date
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.serializer

fun Any?.toJsonElement(json: Json = AppJson.default): JsonElement = when (this) {
    null -> JsonNull

    is JsonElement -> this

    is String -> JsonPrimitive(this)

    is Char -> JsonPrimitive(this.toString())

    is Boolean -> JsonPrimitive(this)

    is Number -> JsonPrimitive(this)

    is Date -> JsonPrimitive(DateSerializer.format(this))

    is Map<*, *> -> JsonObject(
        this.entries
            .filter { it.value != null }
            .associate { it.key.toString() to it.value.toJsonElement(json) }
    )

    is Iterable<*> -> JsonArray(this.map { it.toJsonElement(json) })

    is Array<*> -> JsonArray(this.map { it.toJsonElement(json) })

    is Pair<*, *> -> mapOf("first" to first, "second" to second).toJsonElement(json)

    is Triple<*, *, *> -> mapOf("first" to first, "second" to second, "third" to third).toJsonElement(json)

    else -> encodeWithSerializer(this, json)
}

private fun encodeWithSerializer(value: Any, json: Json): JsonElement = try {
    json.encodeToJsonElement(serializer(value.javaClass), value)
} catch (_: IllegalArgumentException) {
    JsonPrimitive(value.toString())
}

fun JsonElement.toPlainValue(): Any? = when (this) {
    is JsonNull -> null

    is JsonObject -> this.entries.associateTo(LinkedHashMap()) { it.key to it.value.toPlainValue() }

    is JsonArray -> this.mapTo(ArrayList()) { it.toPlainValue() }

    is JsonPrimitive -> when {
        isString -> content
        booleanOrNull != null -> booleanOrNull
        else -> doubleOrNull ?: content
    }
}

@Suppress("UNCHECKED_CAST")
fun JsonObject.toPlainMap(): Map<String, Any?> = toPlainValue() as Map<String, Any?>
