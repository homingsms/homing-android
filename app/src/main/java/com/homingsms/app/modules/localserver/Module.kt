package com.homingsms.app.modules.localserver

import com.homingsms.app.modules.settings.APP_SCOPE
import com.homingsms.core.server.LocalApiConfig
import org.koin.core.qualifier.named
import org.koin.dsl.module

val localserverModule = module {
    single { LocalServerService(get(), get(), get(named(APP_SCOPE))) }
    single { LocalMessagesPort(get(), get()) }
    single { LocalInboxPort(get()) }
    single { LocalWebhooksPort(get(), get()) }
    single { LocalHealthPort(get(), get(), get(), get()) }
    single<LocalApiConfig> {
        localApiConfig(
            messages = get<LocalMessagesPort>(),
            inbox = get<LocalInboxPort>(),
            webhooks = get<LocalWebhooksPort>(),
            keys = get(),
            health = get<LocalHealthPort>(),
            settings = get()
        )
    }
}
