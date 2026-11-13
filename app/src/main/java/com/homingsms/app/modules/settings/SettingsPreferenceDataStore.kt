package com.homingsms.app.modules.settings

import android.content.SharedPreferences
import androidx.core.content.edit
import androidx.preference.PreferenceDataStore
import com.homingsms.core.data.secrets.SecretStore
import com.homingsms.core.data.settings.SettingsRepository
import com.homingsms.core.model.AppSettings
import com.homingsms.core.model.LimitPeriod
import com.homingsms.core.model.ProcessingOrder
import com.homingsms.core.model.SimSelectionMode

class SettingsPreferenceDataStore(
    private val repository: SettingsRepository,
    private val secrets: SecretStore,
    private val fallback: SharedPreferences
) : PreferenceDataStore() {
    private val current: AppSettings
        get() = repository.current

    override fun getString(key: String, defValue: String?): String? = when (key) {
        PASSPHRASE -> null
        in strings -> strings.getValue(key).first(current) ?: defValue
        else -> fallback.getString(key, defValue)
    }

    override fun putString(key: String, value: String?) {
        if (key == PASSPHRASE) {
            if (value.isNullOrEmpty()) secrets.remove(SecretStore.PASSPHRASE) else secrets.put(SecretStore.PASSPHRASE, value)
            return
        }
        val setter = strings[key]?.second
        if (setter == null) {
            fallback.edit { putString(key, value) }
            return
        }
        repository.updateBlocking { setter(it, value?.trim()) }
    }

    override fun getBoolean(key: String, defValue: Boolean): Boolean =
        booleans[key]?.first?.invoke(current) ?: fallback.getBoolean(key, defValue)

    override fun putBoolean(key: String, value: Boolean) {
        val setter = booleans[key]?.second
        if (setter == null) {
            fallback.edit { putBoolean(key, value) }
            return
        }
        repository.updateBlocking { setter(it, value) }
    }

    fun hasPassphrase(): Boolean = !secrets.get(SecretStore.PASSPHRASE).isNullOrEmpty()

    companion object {
        const val PASSPHRASE = "encryption.passphrase"

        private val strings: Map<String, Pair<(AppSettings) -> String?, (AppSettings, String?) -> AppSettings>> = mapOf(
            "messages.sim_selection_mode" to Pair(
                { it.messages.simSelection.name },
                { s, v -> s.copy(messages = s.messages.copy(simSelection = enumOr(v, SimSelectionMode.OSDefault))) }
            ),
            "messages.processing_order" to Pair(
                { it.messages.processingOrder.name },
                { s, v -> s.copy(messages = s.messages.copy(processingOrder = enumOr(v, ProcessingOrder.LIFO))) }
            ),
            "messages.log_lifetime_days" to Pair(
                { it.messages.logLifetimeDays?.toString() },
                { s, v -> s.copy(messages = s.messages.copy(logLifetimeDays = v.positiveInt())) }
            ),
            "messages.send_interval_min" to Pair(
                { it.messages.sendIntervalMinSeconds.toString() },
                { s, v -> s.copy(messages = s.messages.copy(sendIntervalMinSeconds = v?.toIntOrNull()?.coerceAtLeast(0) ?: 0)) }
            ),
            "messages.send_interval_max" to Pair(
                { it.messages.sendIntervalMaxSeconds?.toString() },
                { s, v -> s.copy(messages = s.messages.copy(sendIntervalMaxSeconds = v?.toIntOrNull()?.coerceAtLeast(0))) }
            ),
            "messages.limit_period" to Pair(
                { it.messages.limitPeriod.name },
                { s, v -> s.copy(messages = s.messages.copy(limitPeriod = enumOr(v, LimitPeriod.Disabled))) }
            ),
            "messages.limit_value" to Pair(
                { it.messages.limitValue.toString() },
                { s, v -> s.copy(messages = s.messages.copy(limitValue = v?.toIntOrNull()?.coerceAtLeast(0) ?: 0)) }
            ),
            "messages.work_hours_start" to Pair(
                { it.messages.workHours.start },
                { s, v -> s.copy(messages = s.messages.copy(workHours = s.messages.workHours.copy(start = v ?: "09:00"))) }
            ),
            "messages.work_hours_end" to Pair(
                { it.messages.workHours.end },
                { s, v -> s.copy(messages = s.messages.copy(workHours = s.messages.workHours.copy(end = v ?: "19:00"))) }
            ),
            "localserver.PORT" to Pair(
                { it.localServer.port.toString() },
                { s, v -> s.copy(localServer = s.localServer.copy(port = v?.toIntOrNull() ?: s.localServer.port)) }
            ),
            "incoming.lifetime_days" to Pair(
                { it.inbox.lifetimeDays?.toString() },
                { s, v -> s.copy(inbox = s.inbox.copy(lifetimeDays = v.positiveInt())) }
            ),
            "logs.lifetime_days" to Pair(
                { it.logs.lifetimeDays?.toString() },
                { s, v -> s.copy(logs = s.logs.copy(lifetimeDays = v.positiveInt())) }
            )
        )

        private val booleans: Map<String, Pair<(AppSettings) -> Boolean, (AppSettings, Boolean) -> AppSettings>> = mapOf(
            "messages.work_hours_enabled" to Pair(
                { it.messages.workHours.enabled },
                { s, v -> s.copy(messages = s.messages.copy(workHours = s.messages.workHours.copy(enabled = v))) }
            ),
            "webhooks.internet_required" to Pair(
                { it.webhooks.internetRequired },
                { s, v -> s.copy(webhooks = s.webhooks.copy(internetRequired = v)) }
            ),
            "receiver.content_provider_enabled" to Pair(
                { it.inbox.contentProviderEnabled },
                { s, v -> s.copy(inbox = s.inbox.copy(contentProviderEnabled = v)) }
            )
        )

        private fun String?.positiveInt(): Int? = this?.toIntOrNull()?.takeIf { it > 0 }

        private inline fun <reified T : Enum<T>> enumOr(value: String?, fallback: T): T =
            enumValues<T>().firstOrNull { it.name == value } ?: fallback
    }
}
