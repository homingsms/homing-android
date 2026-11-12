package com.homingsms.app.modules.messages

import com.homingsms.app.modules.messages.vm.MessageDetailsViewModel
import com.homingsms.app.modules.messages.vm.MessagesListViewModel
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module

val messagesModule = module {
    singleOf(::MessagesRepository)
    singleOf(::MessagesService)
    singleOf(::MessageStateService)
    viewModel { MessagesListViewModel(get()) }
    viewModel { MessageDetailsViewModel(get()) }
}

val MODULE_NAME = "messages"
