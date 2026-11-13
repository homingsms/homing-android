package com.homingsms.app.modules.incoming

import com.homingsms.app.modules.incoming.repositories.IncomingMessagesRepository
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module

val incomingModule = module {
    singleOf(::IncomingMessagesRepository)
    singleOf(::IncomingMessagesService)
}

const val MODULE_NAME = "incoming"
