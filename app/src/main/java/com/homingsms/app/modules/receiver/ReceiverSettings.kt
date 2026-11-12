package com.homingsms.app.modules.receiver

import com.homingsms.app.modules.settings.KeyValueStorage
import com.homingsms.app.modules.settings.get

class ReceiverSettings(
    private val storage: KeyValueStorage,
) {
    var contentProviderEnabled: Boolean
        get() = storage.get<Boolean>(CONTENT_PROVIDER_ENABLED) ?: true
        set(value) = storage.set(CONTENT_PROVIDER_ENABLED, value)

    companion object {
        private const val CONTENT_PROVIDER_ENABLED = "content_provider_enabled"
    }
}
