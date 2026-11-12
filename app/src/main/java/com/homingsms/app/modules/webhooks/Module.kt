package com.homingsms.app.modules.webhooks

import com.homingsms.app.modules.webhooks.db.WebhookQueueRepository
import com.homingsms.app.modules.webhooks.vm.WebhookQueueViewModel
import org.koin.androidx.viewmodel.dsl.viewModelOf
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module

val webhooksModule = module {
    singleOf(::WebHooksService)
    singleOf(::WebhookQueueRepository)
    singleOf(::WebhookPayloadStorage)
    viewModelOf(::WebhookQueueViewModel)
}

val NAME = "webhooks"
