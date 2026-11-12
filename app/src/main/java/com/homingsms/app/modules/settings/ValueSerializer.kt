package com.homingsms.app.modules.settings

import com.homingsms.app.serialization.AppJson
import com.homingsms.app.serialization.toJsonElement
import kotlin.reflect.KType
import kotlinx.serialization.KSerializer
import kotlinx.serialization.builtins.nullable
import kotlinx.serialization.serializer

class ValueSerializer {
    fun <T> serialize(value: T): String = AppJson.default.encodeToString(value.toJsonElement())

    @Suppress("UNCHECKED_CAST")
    fun <T> deserialize(value: String, type: KType): T? {
        if (value.isBlank()) {
            return null
        }

        val serializer = (AppJson.lenient.serializersModule.serializer(type) as KSerializer<Any>).nullable
        return AppJson.lenient.decodeFromString(serializer, value) as T?
    }
}
