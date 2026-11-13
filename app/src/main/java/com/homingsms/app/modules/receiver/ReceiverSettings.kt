package com.homingsms.app.modules.receiver

import com.homingsms.core.data.settings.SettingsRepository

class ReceiverSettings(private val repository: SettingsRepository) {
    val contentProviderEnabled: Boolean
        get() = repository.current.inbox.contentProviderEnabled
}
