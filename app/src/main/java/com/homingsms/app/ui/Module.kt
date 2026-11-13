package com.homingsms.app.ui

import com.homingsms.app.BuildConfig
import com.homingsms.app.modules.status.AppStateRepository
import com.homingsms.app.ui.about.AboutViewModel
import com.homingsms.app.ui.about.BuildInfo
import com.homingsms.app.ui.about.LicenseSource
import com.homingsms.app.ui.about.RawLicenseSource
import com.homingsms.app.ui.connect.ConnectViewModel
import com.homingsms.app.ui.connect.PairLinkInbox
import com.homingsms.app.ui.data.AndroidFileSharing
import com.homingsms.app.ui.data.AndroidGatewayControl
import com.homingsms.app.ui.data.AndroidInboxStore
import com.homingsms.app.ui.data.AndroidMessageStore
import com.homingsms.app.ui.data.AndroidWebhookStore
import com.homingsms.app.ui.data.AppStatus
import com.homingsms.app.ui.data.FileSharing
import com.homingsms.app.ui.data.GatewayControl
import com.homingsms.app.ui.data.InboxStore
import com.homingsms.app.ui.data.MessageStore
import com.homingsms.app.ui.data.WebhookStore
import com.homingsms.app.ui.home.HomeViewModel
import com.homingsms.app.ui.inbox.InboxDetailViewModel
import com.homingsms.app.ui.inbox.InboxViewModel
import com.homingsms.app.ui.messages.MessageDetailViewModel
import com.homingsms.app.ui.messages.MessagesViewModel
import com.homingsms.app.ui.navigation.AppViewModel
import com.homingsms.app.ui.onboarding.OnboardingViewModel
import com.homingsms.app.ui.settings.SettingsViewModel
import com.homingsms.app.ui.system.AndroidPlatformState
import com.homingsms.app.ui.system.PlatformState
import com.homingsms.app.ui.webhooks.WebhookDetailViewModel
import com.homingsms.app.ui.webhooks.WebhookEditorViewModel
import com.homingsms.app.ui.webhooks.WebhooksViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val uiDataModule = module {
    single<AppStatus> { get<AppStateRepository>() }
    single<GatewayControl> { AndroidGatewayControl(get(), get(), get(), get(), get(), get(), get()) }
    single<MessageStore> { AndroidMessageStore(get(), get(), get()) }
    single<InboxStore> { AndroidInboxStore(get(), get(), get()) }
    single<WebhookStore> { AndroidWebhookStore(get(), get()) }
    single<FileSharing> { AndroidFileSharing(get(), get()) }
    single<PlatformState> { AndroidPlatformState(get()) }
    single<LicenseSource> { RawLicenseSource(get()) }
    single { BuildInfo(BuildConfig.VERSION_NAME, BuildConfig.VERSION_CODE.toLong(), BuildConfig.BUILD_TYPE) }
    single { PairLinkInbox() }
}

val uiModule = module {
    viewModel { AppViewModel(get(), get()) }
    viewModel { OnboardingViewModel(get(), get(), get(), get(), get(), get()) }
    viewModel { HomeViewModel(get(), get(), get()) }
    viewModel { MessagesViewModel(get(), get()) }
    viewModel { params -> MessageDetailViewModel(params.get(), get()) }
    viewModel { InboxViewModel(get(), get()) }
    viewModel { params -> InboxDetailViewModel(params.get(), get(), get(), get()) }
    viewModel { ConnectViewModel(get(), get(), get()) }
    viewModel { WebhooksViewModel(get()) }
    viewModel { params -> WebhookEditorViewModel(params.getOrNull(), get(), get()) }
    viewModel { params -> WebhookDetailViewModel(params.get(), get()) }
    viewModel { SettingsViewModel(get(), get(), get(), get(), get<BuildInfo>().versionName) }
    viewModel { AboutViewModel(get(), get()) }
}
