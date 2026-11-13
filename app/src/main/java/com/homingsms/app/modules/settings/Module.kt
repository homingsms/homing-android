package com.homingsms.app.modules.settings

import android.content.Context
import android.content.SharedPreferences
import com.homingsms.app.modules.encryption.EncryptionSettings
import com.homingsms.app.modules.incoming.IncomingMessagesSettings
import com.homingsms.app.modules.logs.LogsSettings
import com.homingsms.app.modules.messages.MessagesSettings
import com.homingsms.app.modules.receiver.ReceiverSettings
import com.homingsms.app.modules.receiver.StateStorage
import com.homingsms.core.data.secrets.PreferencesSecretStore
import com.homingsms.core.data.secrets.SecretStore
import com.homingsms.core.data.settings.SettingsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import org.koin.android.ext.koin.androidContext
import org.koin.core.qualifier.named
import org.koin.dsl.module

val settingsModule = module {
    single(named(APP_SCOPE)) { CoroutineScope(SupervisorJob() + Dispatchers.IO) }
    single<SecretStore> { PreferencesSecretStore.create(get()) }
    single { SettingsRepository.create(get(), get(), get(named(APP_SCOPE))) }
    factory<SharedPreferences> {
        androidContext().getSharedPreferences(androidContext().packageName + "_preferences", Context.MODE_PRIVATE)
    }

    factory { EncryptionSettings { get<SecretStore>().get(SecretStore.PASSPHRASE) } }
    factory { MessagesSettings(get()) }
    factory { IncomingMessagesSettings(get()) }
    factory { LogsSettings(get()) }
    factory { ReceiverSettings(get()) }
    single { StateStorage(PreferencesStorage(get(), "receiver")) }
}

const val APP_SCOPE = "appScope"
