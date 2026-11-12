package com.homingsms.app.modules.settings

import android.content.Context
import com.homingsms.app.R
import com.homingsms.app.modules.device.DeviceSettings
import com.homingsms.app.modules.encryption.EncryptionSettings
import com.homingsms.app.modules.gateway.GatewaySettings
import com.homingsms.app.modules.incoming.IncomingMessagesSettings
import com.homingsms.app.modules.logs.LogsSettings
import com.homingsms.app.modules.messages.MessagesSettings
import com.homingsms.app.modules.notifications.NotificationsService
import com.homingsms.app.modules.ping.PingSettings
import com.homingsms.app.modules.webhooks.WebhooksSettings

class SettingsService(
    private val context: Context,
    private val notificationsService: NotificationsService,
    encryptionSettings: EncryptionSettings,
    gatewaySettings: GatewaySettings,
    incomingMessagesSettings: IncomingMessagesSettings,
    messagesSettings: MessagesSettings,
    pingSettings: PingSettings,
    logsSettings: LogsSettings,
    webhooksSettings: WebhooksSettings,
    deviceSettings: DeviceSettings,
) {
    private val settings = mapOf(
        "encryption" to encryptionSettings,
        "gateway" to gatewaySettings,
        "incoming" to incomingMessagesSettings,
        "messages" to messagesSettings,
        "ping" to pingSettings,
        "logs" to logsSettings,
        "webhooks" to webhooksSettings,
        "device" to deviceSettings,
    )

    fun getAll(): Map<String, *> {
        return settings.mapValues { (it.value as? Exporter)?.export() }
    }

    fun update(data: Map<String, *>) {
        if (data.isEmpty()) {
            return
        }

        val changed = data.map { (key, value) ->
            try {
                settings[key]?.let {
                    (it as? Importer)?.import(value as Map<String, *>)
                }
            } catch (e: IllegalArgumentException) {
                throw IllegalArgumentException("Failed to import $key: ${e.message}", e)
            }
        }

        if (changed.none { it == true }) {
            return
        }

        notificationsService.notify(
            context,
            NotificationsService.NOTIFICATION_ID_SETTINGS_CHANGED,
            context.getString(R.string.settings_changed_via_api_restart_the_app_to_apply_changes)
        )
    }
}
