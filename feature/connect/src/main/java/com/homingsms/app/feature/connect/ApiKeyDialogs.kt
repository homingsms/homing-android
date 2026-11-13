package com.homingsms.app.feature.connect

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.homingsms.app.core.designsystem.component.HomingBottomSheet
import com.homingsms.app.core.designsystem.component.HomingButton
import com.homingsms.app.core.designsystem.component.HomingDialog
import com.homingsms.app.core.designsystem.component.HomingTextField
import com.homingsms.app.core.designsystem.component.SecretField
import com.homingsms.app.core.designsystem.theme.HomingTheme

@Composable
internal fun ApiScope.description(): String = stringResource(
    when (this) {
        ApiScope.MessagesSend -> R.string.connect_scope_messages_send
        ApiScope.MessagesRead -> R.string.connect_scope_messages_read
        ApiScope.InboxRead -> R.string.connect_scope_inbox_read
        ApiScope.WebhooksManage -> R.string.connect_scope_webhooks_manage
        ApiScope.DevicesManage -> R.string.connect_scope_devices_manage
        ApiScope.Admin -> R.string.connect_scope_admin
    }
)

@Composable
internal fun CreateKeySheet(draft: CreateKeyDraft, actions: ConnectActions) {
    val admin = ApiScope.Admin in draft.scopes
    HomingBottomSheet(onDismissRequest = actions.onCreateKeyDismiss, title = stringResource(R.string.connect_create_key_title)) {
        HomingTextField(
            value = draft.name,
            onValueChange = actions.onCreateKeyNameChange,
            label = stringResource(R.string.connect_key_name),
            placeholder = stringResource(R.string.connect_key_name_placeholder),
            enabled = !draft.isCreating
        )
        Text(text = stringResource(R.string.connect_key_scopes), style = MaterialTheme.typography.labelMedium)
        Column {
            ApiScope.entries.forEach { scope ->
                ScopeRow(
                    scope = scope,
                    checked = scope in draft.scopes || admin,
                    enabled = !draft.isCreating && (scope == ApiScope.Admin || !admin),
                    onToggle = { actions.onCreateKeyScopeToggle(scope) }
                )
            }
        }
        HomingButton(
            text = stringResource(R.string.connect_create_key_confirm),
            onClick = actions.onCreateKeyConfirm,
            enabled = draft.canCreate,
            loading = draft.isCreating,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun ScopeRow(scope: ApiScope, checked: Boolean, enabled: Boolean, onToggle: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .toggleable(value = checked, enabled = enabled, role = Role.Checkbox, onValueChange = { onToggle() }),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(
            checked = checked,
            onCheckedChange = null,
            enabled = enabled,
            colors = CheckboxDefaults.colors(
                checkedColor = HomingTheme.colors.brand,
                checkmarkColor = HomingTheme.colors.brandForeground
            )
        )
        Spacer(Modifier.width(HomingTheme.spacing.md))
        Column(Modifier.weight(1f)) {
            Text(text = scope.wireName, style = HomingTheme.text.mono)
            Text(
                text = scope.description(),
                style = MaterialTheme.typography.bodySmall,
                color = HomingTheme.colors.mutedForeground
            )
        }
    }
}

@Composable
internal fun SecretOnceDialog(secret: CreatedKeySecret, onSaved: () -> Unit, onShowCopiedConfirmation: () -> Unit) {
    HomingDialog(
        title = stringResource(R.string.connect_secret_title),
        onDismiss = {},
        confirmLabel = stringResource(R.string.connect_secret_saved),
        onConfirm = onSaved,
        dismissLabel = null,
        dismissOnOutsideClick = false
    ) {
        Text(text = stringResource(R.string.connect_secret_body, secret.name), style = MaterialTheme.typography.bodyMedium)
        SecretField(
            label = stringResource(R.string.connect_secret_label),
            secret = secret.secret,
            initiallyRevealed = true,
            onShowCopiedConfirmation = onShowCopiedConfirmation
        )
    }
}
