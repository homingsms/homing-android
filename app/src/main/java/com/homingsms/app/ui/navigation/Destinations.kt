package com.homingsms.app.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.homingsms.app.R
import com.homingsms.app.core.designsystem.component.ConfirmationDialog
import com.homingsms.app.core.designsystem.util.rememberCopyToClipboard
import com.homingsms.app.feature.about.AboutScreen
import com.homingsms.app.feature.connect.ConnectScreen
import com.homingsms.app.feature.home.HomeScreen
import com.homingsms.app.feature.inbox.InboxDetailScreen
import com.homingsms.app.feature.inbox.InboxScreen
import com.homingsms.app.feature.messages.MessageDetailScreen
import com.homingsms.app.feature.messages.MessagesScreen
import com.homingsms.app.feature.onboarding.OnboardingScreen
import com.homingsms.app.feature.onboarding.UpdatedTermsScreen
import com.homingsms.app.feature.settings.SettingsScreen
import com.homingsms.app.feature.webhooks.WebhookDetailScreen
import com.homingsms.app.feature.webhooks.WebhookEditorScreen
import com.homingsms.app.feature.webhooks.WebhooksScreen
import com.homingsms.app.ui.EffectHandler
import com.homingsms.app.ui.Navigator
import com.homingsms.app.ui.about.AboutViewModel
import com.homingsms.app.ui.connect.ConnectViewModel
import com.homingsms.app.ui.connect.PendingPairLink
import com.homingsms.app.ui.home.HomeViewModel
import com.homingsms.app.ui.inbox.InboxDetailViewModel
import com.homingsms.app.ui.inbox.InboxViewModel
import com.homingsms.app.ui.messages.MessageDetailViewModel
import com.homingsms.app.ui.messages.MessagesViewModel
import com.homingsms.app.ui.onboarding.OnboardingViewModel
import com.homingsms.app.ui.onboarding.UpdatedTermsViewModel
import com.homingsms.app.ui.settings.SettingsViewModel
import com.homingsms.app.ui.webhooks.WebhookDetailViewModel
import com.homingsms.app.ui.webhooks.WebhookEditorViewModel
import com.homingsms.app.ui.webhooks.WebhooksViewModel
import com.homingsms.core.model.StatusReason
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

@Composable
private fun OnResume(action: () -> Unit) {
    LifecycleResumeEffect(Unit) {
        action()
        onPauseOrDispose { }
    }
}

@Composable
fun OnboardingDestination(navigator: Navigator) {
    val viewModel: OnboardingViewModel = koinViewModel()
    val state by viewModel.state.collectAsStateWithLifecycle()
    val pending by viewModel.pendingLink.collectAsStateWithLifecycle()
    EffectHandler(viewModel.effects, navigator, viewModel::onPermissionsResult)
    OnResume(viewModel::onResume)
    OnboardingScreen(state, viewModel.actions)
    PairLinkDialog(pending, viewModel::confirmPairLink, viewModel::dismissPairLink)
}

@Composable
fun TermsDestination(navigator: Navigator) {
    val viewModel: UpdatedTermsViewModel = koinViewModel()
    val state by viewModel.state.collectAsStateWithLifecycle()
    EffectHandler(viewModel.effects, navigator)
    UpdatedTermsScreen(state, viewModel.actions)
}

@Composable
fun HomeDestination(navigator: Navigator) {
    val viewModel: HomeViewModel = koinViewModel()
    val state by viewModel.state.collectAsStateWithLifecycle()
    EffectHandler(viewModel.effects, navigator, viewModel::onPermissionsResult)
    OnResume(viewModel::onResume)
    HomeScreen(state, viewModel.actions)
}

@Composable
fun MessagesDestination(navigator: Navigator) {
    val viewModel: MessagesViewModel = koinViewModel()
    val state by viewModel.state.collectAsStateWithLifecycle()
    EffectHandler(viewModel.effects, navigator)
    MessagesScreen(state, viewModel.actions)
}

@Composable
fun MessageDetailDestination(id: String, navigator: Navigator) {
    val viewModel: MessageDetailViewModel = koinViewModel { parametersOf(id) }
    val state by viewModel.state.collectAsStateWithLifecycle()
    EffectHandler(viewModel.effects, navigator)
    MessageDetailScreen(state, viewModel.actions)
}

@Composable
fun InboxDestination(navigator: Navigator) {
    val viewModel: InboxViewModel = koinViewModel()
    val state by viewModel.state.collectAsStateWithLifecycle()
    EffectHandler(viewModel.effects, navigator)
    InboxScreen(state, viewModel.actions)
}

@Composable
fun InboxDetailDestination(id: String, navigator: Navigator) {
    val viewModel: InboxDetailViewModel = koinViewModel { parametersOf(id) }
    val state by viewModel.state.collectAsStateWithLifecycle()
    EffectHandler(viewModel.effects, navigator)
    InboxDetailScreen(state, viewModel.actions)
}

@Composable
fun ConnectDestination(navigator: Navigator) {
    val viewModel: ConnectViewModel = koinViewModel()
    val state by viewModel.state.collectAsStateWithLifecycle()
    val pending by viewModel.pendingLink.collectAsStateWithLifecycle()
    EffectHandler(viewModel.effects, navigator)
    ConnectScreen(state, viewModel.actions)
    PairLinkDialog(pending, viewModel::confirmPairLink, viewModel::dismissPairLink)
}

@Composable
fun WebhooksDestination(navigator: Navigator) {
    val viewModel: WebhooksViewModel = koinViewModel()
    val state by viewModel.state.collectAsStateWithLifecycle()
    EffectHandler(viewModel.effects, navigator)
    WebhooksScreen(state, viewModel.actions)
}

@Composable
fun WebhookEditorDestination(id: String?, navigator: Navigator) {
    val viewModel: WebhookEditorViewModel = koinViewModel { parametersOf(id) }
    val state by viewModel.state.collectAsStateWithLifecycle()
    EffectHandler(viewModel.effects, navigator)
    WebhookEditorScreen(state, viewModel.actions)
}

@Composable
fun WebhookDetailDestination(id: String, navigator: Navigator) {
    val viewModel: WebhookDetailViewModel = koinViewModel { parametersOf(id) }
    val state by viewModel.state.collectAsStateWithLifecycle()
    EffectHandler(viewModel.effects, navigator)
    WebhookDetailScreen(state, viewModel.actions)
}

@Composable
fun SettingsDestination(navigator: Navigator) {
    val viewModel: SettingsViewModel = koinViewModel()
    val state by viewModel.state.collectAsStateWithLifecycle()
    val showSmsLimitHelp by viewModel.showSmsLimitHelp.collectAsStateWithLifecycle()
    EffectHandler(viewModel.effects, navigator)
    OnResume(viewModel::onResume)
    SettingsScreen(state, viewModel.actions)
    if (showSmsLimitHelp) SmsLimitDialog(onDismiss = viewModel::dismissSmsLimitHelp)
}

@Composable
fun AboutDestination(navigator: Navigator) {
    val viewModel: AboutViewModel = koinViewModel()
    val state by viewModel.state.collectAsStateWithLifecycle()
    EffectHandler(viewModel.effects, navigator)
    AboutScreen(state, viewModel.actions)
}

@Composable
fun PairLinkDialog(link: PendingPairLink?, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    if (link == null) return
    ConfirmationDialog(
        title = stringResource(R.string.pair_link_title),
        message = stringResource(R.string.pair_link_message, link.serverHost, link.code),
        confirmLabel = stringResource(R.string.pair_link_confirm),
        onConfirm = onConfirm,
        onDismiss = onDismiss
    )
}

@Composable
private fun SmsLimitDialog(onDismiss: () -> Unit) {
    val command = "adb shell settings put global ${StatusReason.OutgoingSmsLimit.SETTING_MAX_COUNT} " +
        StatusReason.OutgoingSmsLimit.SUGGESTED_MAX_COUNT
    val copy = rememberCopyToClipboard()
    val label = stringResource(R.string.sms_limit_title)
    ConfirmationDialog(
        title = label,
        message = stringResource(R.string.sms_limit_message, command),
        confirmLabel = stringResource(R.string.sms_limit_copy),
        onConfirm = {
            copy.copy(label, command, sensitive = false)
            onDismiss()
        },
        onDismiss = onDismiss
    )
}
