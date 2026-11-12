package com.homingsms.app.modules.gateway

import com.homingsms.app.modules.gateway.inbox.InboxUploadRepository
import com.homingsms.app.modules.gateway.inbox.InboxUploadService
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module

val gatewayModule = module {
    singleOf(::GatewayService)
    singleOf(::InboxUploadRepository)
    singleOf(::InboxUploadService)
}

internal const val MODULE_NAME = "gateway"
