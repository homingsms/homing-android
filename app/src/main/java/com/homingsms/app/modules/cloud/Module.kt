package com.homingsms.app.modules.cloud

import com.homingsms.app.modules.settings.APP_SCOPE
import com.homingsms.core.data.cloud.CloudSyncEngine
import com.homingsms.core.data.cloud.DeviceApi
import com.homingsms.core.data.cloud.InboxUploadSource
import com.homingsms.core.data.repository.InboxRecords
import com.homingsms.core.data.secrets.SecretStore
import com.homingsms.core.data.settings.SettingsRepository
import org.koin.core.qualifier.named
import org.koin.dsl.module

val cloudModule = module {
    single {
        val settings = get<SettingsRepository>()
        val secrets = get<SecretStore>()
        DeviceApi(DeviceApi.createClient(), { settings.current.cloud.serverUrl }, { secrets.get(SecretStore.DEVICE_TOKEN) })
    }
    single {
        val inbox = get<InboxRecords>()
        CloudSyncEngine(
            api = get(),
            outbox = get(),
            config = SettingsCloudConfigStore(get()),
            secrets = get(),
            device = AndroidCloudDeviceInfo(get(), get(), get()),
            sink = MessagesCloudSink(get()),
            inboxSource = InboxUploadSource { inbox.uploads(it) }
        )
    }
    single { CloudController(get(), get(), get(), get(), get(named(APP_SCOPE))) }
}
