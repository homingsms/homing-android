package com.homingsms.core.model.api

import com.homingsms.app.serialization.DateSerializer
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json

@OptIn(ExperimentalSerializationApi::class)
object ApiJson {
    val default: Json = Json {
        ignoreUnknownKeys = true
        explicitNulls = true
        encodeDefaults = true
    }

    val pretty: Json = Json(default) {
        prettyPrint = true
    }
}

object Iso8601 {
    private const val PATTERN = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'"

    fun format(timestamp: Long): String = SimpleDateFormat(PATTERN, Locale.US).apply {
        timeZone = TimeZone.getTimeZone("UTC")
    }.format(Date(timestamp))

    fun formatOrNull(timestamp: Long?): String? = timestamp?.let(::format)

    fun parse(value: String): Long? = DateSerializer.parse(value)?.time
}
