package com.homingsms.app.feature.connect

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.homingsms.app.core.designsystem.component.CenteredContent
import com.homingsms.app.core.designsystem.component.CodeBlock
import com.homingsms.app.core.designsystem.component.ConfirmationDialog
import com.homingsms.app.core.designsystem.component.CopyableValue
import com.homingsms.app.core.designsystem.component.HomingButton
import com.homingsms.app.core.designsystem.component.HomingButtonStyle
import com.homingsms.app.core.designsystem.component.HomingDivider
import com.homingsms.app.core.designsystem.component.HomingListRow
import com.homingsms.app.core.designsystem.component.HomingNavigationRow
import com.homingsms.app.core.designsystem.component.HomingPanel
import com.homingsms.app.core.designsystem.component.HomingScaffold
import com.homingsms.app.core.designsystem.component.HomingSwitchRow
import com.homingsms.app.core.designsystem.component.HomingTopAppBar
import com.homingsms.app.core.designsystem.component.LoadingState
import com.homingsms.app.core.designsystem.component.QrCode
import com.homingsms.app.core.designsystem.component.SectionHeader
import com.homingsms.app.core.designsystem.icon.HomingIcons
import com.homingsms.app.core.designsystem.theme.HomingTheme
import com.homingsms.app.core.designsystem.util.formatRelativeTime
import kotlinx.coroutines.launch

private const val KEY_PREFIX_SUFFIX = "…"

@Composable
fun ConnectScreen(state: ConnectUiState, actions: ConnectActions, modifier: Modifier = Modifier) {
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val copied = stringResource(com.homingsms.app.core.designsystem.R.string.homing_action_copied)
    val confirmCopy: () -> Unit = { scope.launch { snackbar.showSnackbar(copied) } }
    ConnectNoticeEffect(state.notice, snackbar, actions.onNoticeShown)
    HomingScaffold(
        modifier = modifier,
        snackbarHostState = snackbar,
        topBar = { HomingTopAppBar(title = stringResource(R.string.connect_title)) }
    ) { padding ->
        CenteredContent(Modifier.padding(padding)) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(HomingTheme.spacing.gutter),
                verticalArrangement = Arrangement.spacedBy(HomingTheme.spacing.lg)
            ) {
                LocalApiPanel(state.local, actions, confirmCopy)
                ApiKeysPanel(state, actions)
                CloudPairingPanel(cloud = state.cloud, pairingCode = state.pairingCode, actions = actions.pairing)
                HomingPanel(contentPadding = PaddingValues()) {
                    HomingNavigationRow(
                        title = stringResource(R.string.connect_webhooks),
                        value = stringResource(R.string.connect_webhooks_body),
                        onClick = actions.onOpenWebhooks,
                        leading = { Icon(HomingIcons.Webhook, contentDescription = null, tint = HomingTheme.colors.mutedForeground) }
                    )
                }
            }
        }
    }
    state.createKey?.let { CreateKeySheet(it, actions) }
    state.createdSecret?.let { SecretOnceDialog(it, actions.onSecretSaved, confirmCopy) }
}

@Composable
private fun ConnectNoticeEffect(notice: ConnectNotice?, snackbar: SnackbarHostState, onShown: () -> Unit) {
    val text = when (notice) {
        ConnectNotice.KeyRevoked -> stringResource(R.string.connect_notice_revoked)
        ConnectNotice.Unpaired -> stringResource(R.string.connect_notice_unpaired)
        ConnectNotice.Paired -> stringResource(R.string.connect_notice_paired)
        ConnectNotice.ActionFailed -> stringResource(R.string.connect_notice_failed)
        null -> null
    }
    LaunchedEffect(notice) {
        if (text != null) {
            snackbar.showSnackbar(text)
            onShown()
        }
    }
}

@Composable
private fun LocalApiPanel(local: LocalApiUi, actions: ConnectActions, onCopied: () -> Unit) {
    HomingPanel(contentPadding = PaddingValues(vertical = HomingTheme.spacing.xs)) {
        HomingSwitchRow(
            title = stringResource(R.string.connect_local_title),
            description = stringResource(
                when {
                    !local.enabled -> R.string.connect_local_off
                    local.running -> R.string.connect_local_on
                    else -> R.string.connect_local_starting
                }
            ),
            checked = local.enabled,
            onCheckedChange = actions.onLocalServerToggle,
            leading = { Icon(HomingIcons.Server, contentDescription = null, tint = HomingTheme.colors.mutedForeground) }
        )
        val baseUrl = local.apiBaseUrl
        if (local.enabled && baseUrl != null) {
            Column(
                modifier = Modifier.padding(HomingTheme.spacing.gutter),
                verticalArrangement = Arrangement.spacedBy(HomingTheme.spacing.lg)
            ) {
                CopyableValue(
                    label = stringResource(R.string.connect_base_url),
                    value = baseUrl,
                    onShowCopiedConfirmation = onCopied
                )
                Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    QrCode(
                        content = baseUrl,
                        contentDescription = stringResource(R.string.connect_base_url_qr, baseUrl),
                        size = HomingTheme.spacing.huge * 5
                    )
                }
                Text(
                    text = stringResource(R.string.connect_curl_title),
                    style = MaterialTheme.typography.labelMedium,
                    color = HomingTheme.colors.mutedForeground
                )
                CodeBlock(
                    code = curlSnippet(baseUrl),
                    copyLabel = stringResource(R.string.connect_curl_label),
                    onShowCopiedConfirmation = onCopied
                )
            }
        }
    }
}

@Composable
private fun ApiKeysPanel(state: ConnectUiState, actions: ConnectActions) {
    var revokeId by rememberSaveable { mutableStateOf<String?>(null) }
    HomingPanel(contentPadding = PaddingValues(vertical = HomingTheme.spacing.xs)) {
        SectionHeader(
            title = stringResource(R.string.connect_keys_title),
            modifier = Modifier.padding(start = HomingTheme.spacing.gutter, end = HomingTheme.spacing.xs),
            trailing = {
                HomingButton(
                    text = stringResource(R.string.connect_create_key),
                    onClick = actions.onCreateKeyClick,
                    style = HomingButtonStyle.Text
                )
            }
        )
        when {
            state.keysLoading -> LoadingState(rows = 2)

            state.keys.isEmpty() -> Text(
                text = stringResource(R.string.connect_keys_empty),
                style = MaterialTheme.typography.bodyMedium,
                color = HomingTheme.colors.mutedForeground,
                modifier = Modifier.padding(horizontal = HomingTheme.spacing.gutter, vertical = HomingTheme.spacing.md)
            )

            else -> state.keys.forEachIndexed { index, key ->
                if (index > 0) HomingDivider(inset = true)
                ApiKeyRow(key, onRevoke = { revokeId = key.id })
            }
        }
    }
    state.keys.firstOrNull { it.id == revokeId }?.let { key ->
        ConfirmationDialog(
            title = stringResource(R.string.connect_revoke_title, key.name),
            message = stringResource(R.string.connect_revoke_body),
            confirmLabel = stringResource(R.string.connect_revoke),
            destructive = true,
            onConfirm = {
                revokeId = null
                actions.onRevokeKey(key.id)
            },
            onDismiss = { revokeId = null }
        )
    }
}

@Composable
private fun ApiKeyRow(key: ApiKeyUi, onRevoke: () -> Unit) {
    val scopes = ApiScope.entries.filter { it in key.scopes }.joinToString(", ") { it.wireName }
    val lastUsed = key.lastUsedAtMillis?.let { stringResource(R.string.connect_key_last_used, formatRelativeTime(it)) }
        ?: stringResource(R.string.connect_key_never_used)
    HomingListRow(
        headline = key.name,
        overline = key.prefix + KEY_PREFIX_SUFFIX,
        supporting = "$scopes\n$lastUsed",
        supportingMaxLines = 4,
        trailing = {
            IconButton(onClick = onRevoke) {
                Icon(
                    imageVector = HomingIcons.Delete,
                    contentDescription = stringResource(R.string.connect_revoke_named, key.name),
                    tint = HomingTheme.colors.danger
                )
            }
        }
    )
}
