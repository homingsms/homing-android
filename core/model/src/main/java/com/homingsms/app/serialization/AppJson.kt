package com.homingsms.app.serialization

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json

@OptIn(ExperimentalSerializationApi::class)
object AppJson {
    val default: Json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
        encodeDefaults = true
        coerceInputValues = true
    }

    val withNulls: Json = Json(default) {
        explicitNulls = true
    }

    val pretty: Json = Json(default) {
        prettyPrint = true
    }

    val lenient: Json = Json(default) {
        isLenient = true
    }
}
