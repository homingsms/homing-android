package com.homingsms.app.modules.messages

import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module

val messagesModule = module {
    singleOf(::MessagesRepository)
    singleOf(::MessagesService)
    singleOf(::MessageStateService)
    single { SmsLimitMonitor(get(), get()) }
}

val MODULE_NAME = "messages"
