package com.homingsms.app.data

import androidx.room.TypeConverter
import com.homingsms.app.serialization.AppJson
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.JsonElement

class Converters {
    @TypeConverter
    fun listToString(value: List<String>?): String? {
        return value?.let { AppJson.default.encodeToString(STRING_LIST, it) }
    }

    @TypeConverter
    fun listFromString(value: String?): List<String>? {
        return value?.let { AppJson.default.decodeFromString(STRING_LIST, it) }
    }

    @TypeConverter
    fun dateToString(value: Date?): String? {
        return value?.let { DATE_FORMAT.format(it) }
    }

    @TypeConverter
    fun dateFromString(value: String?): Date? {
        if (value == null) return null
        // Try the canonical format first, then a tolerant fallback list.
        // Never let a ParseException/RuntimeException escape into LiveData.
        val parsers = listOf(DATE_FORMAT) + FALLBACK_FORMATS
        for (format in parsers) {
            try {
                format.parse(value)?.let { return it }
            } catch (_: Exception) {
                // try next format
            }
        }
        return null
    }

    @TypeConverter
    fun jsonToString(value: JsonElement?): String? {
        return value?.let { AppJson.default.encodeToString(JsonElement.serializer(), it) }
    }

    @TypeConverter
    fun stringToJson(value: String?): JsonElement? {
        return value?.let { AppJson.default.decodeFromString(JsonElement.serializer(), it) }
    }

    companion object {
        private val STRING_LIST = ListSerializer(String.serializer())

        private val DATE_FORMAT =
            SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US).apply {
                timeZone = TimeZone.getTimeZone("GMT")
            }

        // Accepted legacy/alternate formats (all interpreted as GMT to match the
        // canonical writer). Used only after DATE_FORMAT fails, so new rows keep
        // the exact .SSS'Z' round-trip while old rows (e.g. produced by MIGRATION_7_8
        // via strftime without milliseconds) are parsed without crashing.
        private val FALLBACK_FORMATS = listOf(
            SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ", Locale.US).apply {
                timeZone = TimeZone.getTimeZone("GMT")
                isLenient = true
            },
            SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US).apply {
                timeZone = TimeZone.getTimeZone("GMT")
                isLenient = true
            },
            SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).apply {
                timeZone = TimeZone.getTimeZone("GMT")
                isLenient = true
            },
            SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS", Locale.US).apply {
                timeZone = TimeZone.getTimeZone("GMT")
                isLenient = true
            },
            SimpleDateFormat("yyyy-MM-dd", Locale.US).apply {
                timeZone = TimeZone.getTimeZone("GMT")
                isLenient = true
            },
        )
    }
}
