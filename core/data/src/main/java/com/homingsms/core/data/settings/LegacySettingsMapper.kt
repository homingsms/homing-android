package com.homingsms.core.data.settings

import com.homingsms.core.model.AppSettings
import com.homingsms.core.model.InboxSettings
import com.homingsms.core.model.LocalServerSettings
import com.homingsms.core.model.LogSettings
import com.homingsms.core.model.MessageSettings
import com.homingsms.core.model.WorkHours

data class LegacyMigrationResult(val settings: AppSettings, val secrets: Map<String, String>, val consumedKeys: Set<String>)

object LegacySettingsMapper {
    const val PASSPHRASE_SECRET = "encryption.passphrase"

    private val prefixes = listOf(
        "messages.",
        "localserver.",
        "gateway.",
        "webhooks.",
        "incoming.",
        "logs.",
        "ping.",
        "encryption.",
        "device."
    )
    private val exactKeys = setOf("autostart", "receiver.content_provider_enabled", "receiver.version")

    fun isLegacyKey(key: String): Boolean = key in exactKeys || prefixes.any { key.startsWith(it) }

    fun hasLegacyValues(values: Map<String, *>): Boolean = values.keys.any(::isLegacyKey)

    fun map(values: Map<String, *>, current: AppSettings): LegacyMigrationResult {
        val reader = Reader(values)
        val settings = current.copy(
            general = current.general.copy(autostart = reader.boolean("autostart") ?: current.general.autostart),
            messages = messages(reader, current.messages),
            localServer = localServer(reader, current.localServer),
            webhooks = current.webhooks.copy(
                internetRequired = reader.boolean("webhooks.internet_required") ?: current.webhooks.internetRequired
            ),
            inbox = inbox(reader, current.inbox),
            logs = LogSettings(lifetimeDays = reader.positiveInt("logs.lifetime_days") ?: current.logs.lifetimeDays)
        )
        val secrets = buildMap {
            reader.string("encryption.passphrase")?.takeIf { it.isNotEmpty() }?.let { put(PASSPHRASE_SECRET, it) }
        }
        return LegacyMigrationResult(settings, secrets, values.keys.filter(::isLegacyKey).toSet())
    }

    private fun messages(reader: Reader, current: MessageSettings): MessageSettings = current.copy(
        simSelection = reader.enum("messages.sim_selection_mode", current.simSelection),
        processingOrder = reader.enum("messages.processing_order", current.processingOrder),
        sendIntervalMinSeconds = reader.int("messages.send_interval_min") ?: current.sendIntervalMinSeconds,
        sendIntervalMaxSeconds = reader.int("messages.send_interval_max")
            ?: reader.int("messages.SECONDS_BETWEEN_MESSAGES")
            ?: current.sendIntervalMaxSeconds,
        limitPeriod = reader.enum("messages.limit_period", current.limitPeriod),
        limitValue = reader.int("messages.limit_value")?.coerceAtLeast(0) ?: current.limitValue,
        logLifetimeDays = reader.positiveInt("messages.log_lifetime_days") ?: current.logLifetimeDays,
        workHours = WorkHours(
            enabled = reader.boolean("messages.work_hours_enabled") ?: current.workHours.enabled,
            start = reader.time("messages.work_hours_start") ?: current.workHours.start,
            end = reader.time("messages.work_hours_end") ?: current.workHours.end
        )
    )

    private fun localServer(reader: Reader, current: LocalServerSettings): LocalServerSettings = current.copy(
        enabled = reader.boolean("localserver.ENABLED") ?: current.enabled,
        port = reader.int("localserver.PORT")?.takeIf { it in MIN_PORT..MAX_PORT } ?: current.port,
        deviceId = reader.string("localserver.DEVICE_ID") ?: current.deviceId
    )

    private fun inbox(reader: Reader, current: InboxSettings): InboxSettings = current.copy(
        contentProviderEnabled = reader.boolean("receiver.content_provider_enabled") ?: current.contentProviderEnabled,
        lifetimeDays = reader.positiveInt("incoming.lifetime_days") ?: current.lifetimeDays
    )

    private class Reader(private val values: Map<String, *>) {
        fun string(key: String): String? = values[key]?.toString()?.trim()?.takeIf { it.isNotEmpty() }

        fun int(key: String): Int? = when (val value = values[key]) {
            is Number -> value.toInt()
            is String -> value.trim().toFloatOrNull()?.toInt()
            else -> null
        }

        fun positiveInt(key: String): Int? = int(key)?.takeIf { it > 0 }

        fun boolean(key: String): Boolean? = when (val value = values[key]) {
            is Boolean -> value
            is String -> value.trim().toBooleanStrictOrNull()
            else -> null
        }

        fun time(key: String): String? = string(key)?.takeIf { timePattern.matches(it) }

        inline fun <reified T : Enum<T>> enum(key: String, fallback: T): T {
            val raw = string(key)?.trim('"') ?: return fallback
            return enumValues<T>().firstOrNull { it.name == raw } ?: fallback
        }
    }

    private const val MIN_PORT = 1024
    private const val MAX_PORT = 65535
    private val timePattern = Regex("^([01]\\d|2[0-3]):[0-5]\\d$")
}
