package com.homingsms.app.modules.settings

import androidx.preference.PreferenceManager
import com.homingsms.app.helpers.SettingsHelper
import com.homingsms.app.modules.device.DeviceSettings
import com.homingsms.app.modules.encryption.EncryptionSettings
import com.homingsms.app.modules.gateway.GatewaySettings
import com.homingsms.app.modules.incoming.IncomingMessagesSettings
import com.homingsms.app.modules.localserver.LocalServerSettings
import com.homingsms.app.modules.logs.LogsSettings
import com.homingsms.app.modules.messages.MessagesSettings
import com.homingsms.app.modules.ping.PingSettings
import com.homingsms.app.modules.receiver.ReceiverSettings
import com.homingsms.app.modules.receiver.StateStorage
import com.homingsms.app.modules.webhooks.TemporaryStorage
import com.homingsms.app.modules.webhooks.WebhooksSettings
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module


val settingsModule = module {
    singleOf(::SettingsService)
    factory { PreferenceManager.getDefaultSharedPreferences(get()) }
    factory { SettingsHelper(get()) }

    factory {
        EncryptionSettings(
            PreferencesStorage(get(), "encryption")
        )
    }
    factory {
        GatewaySettings(
            PreferencesStorage(get(), "gateway")
        )
    }
    factory {
        MessagesSettings(
            PreferencesStorage(get(), "messages")
        )
    }
    factory {
        LocalServerSettings(
            PreferencesStorage(get(), "localserver")
        )
    }
    factory {
        PingSettings(
            PreferencesStorage(get(), "ping")
        )
    }
    factory {
        IncomingMessagesSettings(
            PreferencesStorage(get(), "incoming")
        )
    }
    factory {
        LogsSettings(
            PreferencesStorage(get(), "logs")
        )
    }
    factory {
        WebhooksSettings(
            PreferencesStorage(get(), "webhooks")
        )
    }
    factory {
        ReceiverSettings(
            PreferencesStorage(get(), "receiver")
        )
    }
    factory {
        DeviceSettings(
            PreferencesStorage(get(), "device")
        )
    }
    //
    single {
        TemporaryStorage(
            PreferencesStorage(get(), "webhooks")
        )
    }
    single {
        StateStorage(
            PreferencesStorage(get(), "receiver")
        )
    }
}
