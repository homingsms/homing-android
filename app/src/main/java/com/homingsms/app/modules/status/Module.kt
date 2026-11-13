package com.homingsms.app.modules.status

import com.homingsms.app.BuildConfig
import com.homingsms.core.telephony.DeviceHealthReader
import org.koin.dsl.module

val statusModule = module {
    single { DeviceHealthReader(get(), BuildConfig.VERSION_NAME) }
    single {
        AppStateRepository(
            settingsRepository = get(),
            stats = get(),
            healthReader = get(),
            smsLimitMonitor = get(),
            localServer = get(),
            cloud = get(),
            webhooks = get(),
            messageRecords = get(),
            inboxRecords = get()
        )
    }
}
