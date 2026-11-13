package com.homingsms.app.modules.webhooks

import com.homingsms.core.data.settings.SettingsRepository
import com.homingsms.core.data.webhooks.WebhookDispatcher
import com.homingsms.core.data.webhooks.WebhookRepository
import org.koin.dsl.module

val webhooksModule = module {
    single {
        val settings = get<SettingsRepository>()
        WebhookRepository(get(), get(), get(), { LOCAL_ACCOUNT_ID + settings.current.localServer.deviceId.orEmpty() })
    }
    single { WebhookDispatcher(get(), get(), get()) }
    single { WebhooksService(get(), get(), get(), get(), get()) }
}

const val LOCAL_ACCOUNT_ID = "local:"
