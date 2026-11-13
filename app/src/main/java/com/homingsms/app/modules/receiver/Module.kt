package com.homingsms.app.modules.receiver

import com.homingsms.app.modules.settings.APP_SCOPE
import org.koin.core.module.dsl.singleOf
import org.koin.core.qualifier.named
import org.koin.dsl.module

val receiverModule = module {
    singleOf(::ReceiverService)
    single { InboxDispatcher(get(), get(), get(), get(named(APP_SCOPE))) }
}

val MODULE_NAME = "receiver"
