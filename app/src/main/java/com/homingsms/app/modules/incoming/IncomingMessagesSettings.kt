package com.homingsms.app.modules.incoming

import com.homingsms.app.modules.settings.Exporter
import com.homingsms.app.modules.settings.Importer
import com.homingsms.app.modules.settings.KeyValueStorage
import com.homingsms.app.modules.settings.get

class IncomingMessagesSettings(
    private val storage: KeyValueStorage,
) : Exporter, Importer {
    val lifetimeDays: Int?
        get() = storage.get<Int?>(LIFETIME_DAYS)?.takeIf { it > 0 }

    override fun export(): Map<String, *> {
        return mapOf(
            LIFETIME_DAYS to lifetimeDays,
        )
    }

    override fun import(data: Map<String, *>): Boolean {
        return data.map { (key, value) ->
            when (key) {
                LIFETIME_DAYS -> {
                    val lifetimeDays = value?.toString()?.toFloat()?.toInt()
                    if (lifetimeDays != null && lifetimeDays < 1) {
                        throw IllegalArgumentException("Log lifetime days must be >= 1")
                    }

                    val changed = this.lifetimeDays != lifetimeDays
                    storage.set(key, lifetimeDays?.toString())

                    changed
                }

                else -> false
            }
        }.any { it }
    }

    companion object {
        private const val LIFETIME_DAYS = "lifetime_days"
    }
}
