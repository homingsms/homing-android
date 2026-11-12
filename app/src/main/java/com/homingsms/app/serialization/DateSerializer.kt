package com.homingsms.app.serialization

import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.GregorianCalendar
import java.util.Locale
import java.util.TimeZone
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerializationException
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

object DateSerializer : KSerializer<Date> {
    private const val PATTERN = "yyyy-MM-dd'T'HH:mm:ss.SSSXXX"
    private const val MAX_SECOND = 59
    private const val MILLIS_DIGITS = 3
    private const val YEAR = 1
    private const val MONTH = 2
    private const val DAY = 3
    private const val HOUR = 4
    private const val MINUTE = 5
    private const val SECOND = 6
    private const val FRACTION = 7
    private const val ZONE = 8

    private val isoRegex = Regex(
        """^(\d{4})-?(\d{2})-?(\d{2})(?:T(\d{2}):?(\d{2})(?::?(\d{2})(?:[.,](\d+))?)?(Z|[+-]\d{2}(?::?\d{2})?))?$"""
    )

    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("com.homingsms.app.Date", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: Date) {
        encoder.encodeString(format(value))
    }

    override fun deserialize(decoder: Decoder): Date {
        val value = decoder.decodeString()
        return parse(value) ?: throw SerializationException("Failed to parse date: $value")
    }

    fun format(value: Date): String = SimpleDateFormat(PATTERN, Locale.US).format(value)

    fun parse(value: String): Date? {
        try {
            SimpleDateFormat(PATTERN, Locale.US).parse(value)?.let { return it }
        } catch (_: ParseException) {
        }

        return parseIso8601(value)
    }

    private fun parseIso8601(value: String): Date? {
        val groups = isoRegex.matchEntire(value)?.groupValues ?: return null
        val year = groups[YEAR].toInt()
        val month = groups[MONTH].toInt() - 1
        val day = groups[DAY].toInt()

        return try {
            if (groups[HOUR].isEmpty()) {
                GregorianCalendar(year, month, day).apply { isLenient = false }.time
            } else {
                val calendar = GregorianCalendar(parseTimeZone(groups[ZONE])).apply {
                    isLenient = false
                    clear()
                    set(
                        year,
                        month,
                        day,
                        groups[HOUR].toInt(),
                        groups[MINUTE].toInt(),
                        groups[SECOND].ifEmpty { "0" }.toInt().coerceAtMost(MAX_SECOND)
                    )
                    set(Calendar.MILLISECOND, parseMillis(groups[FRACTION]))
                }
                calendar.time
            }
        } catch (_: IllegalArgumentException) {
            null
        }
    }

    private fun parseMillis(fraction: String): Int = if (fraction.isEmpty()) {
        0
    } else {
        fraction.take(MILLIS_DIGITS).padEnd(MILLIS_DIGITS, '0').toInt()
    }

    private fun parseTimeZone(raw: String): TimeZone {
        if (raw == "Z") {
            return TimeZone.getTimeZone("UTC")
        }

        val digits = raw.substring(1).replace(":", "")
        val hours = digits.take(2)
        val minutes = digits.drop(2).ifEmpty { "00" }
        return TimeZone.getTimeZone("GMT${raw[0]}$hours:$minutes")
    }
}
