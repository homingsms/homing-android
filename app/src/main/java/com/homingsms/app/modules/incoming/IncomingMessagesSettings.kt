package com.homingsms.app.modules.incoming

import com.homingsms.core.data.settings.SettingsRepository

class IncomingMessagesSettings(private val repository: SettingsRepository) {
    val lifetimeDays: Int?
        get() = repository.current.inbox.lifetimeDays?.takeIf { it > 0 }
}
