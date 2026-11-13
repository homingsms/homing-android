package com.homingsms.app.modules.logs

import com.homingsms.core.data.settings.SettingsRepository

class LogsSettings(private val repository: SettingsRepository) {
    val lifetimeDays: Int?
        get() = repository.current.logs.lifetimeDays?.takeIf { it > 0 }

    val verbose: Boolean
        get() = repository.current.logs.verbose
}
