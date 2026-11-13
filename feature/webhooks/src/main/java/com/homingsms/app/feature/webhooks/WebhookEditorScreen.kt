package com.homingsms.app.feature.webhooks

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.homingsms.app.core.designsystem.component.CenteredContent
import com.homingsms.app.core.designsystem.component.ConfirmationDialog
import com.homingsms.app.core.designsystem.component.HomingButton
import com.homingsms.app.core.designsystem.component.HomingButtonStyle
import com.homingsms.app.core.designsystem.component.HomingDivider
import com.homingsms.app.core.designsystem.component.HomingPanel
import com.homingsms.app.core.designsystem.component.HomingScaffold
import com.homingsms.app.core.designsystem.component.HomingSwitchRow
import com.homingsms.app.core.designsystem.component.HomingTextField
import com.homingsms.app.core.designsystem.component.HomingTopAppBar
import com.homingsms.app.core.designsystem.component.SectionHeader
import com.homingsms.app.core.designsystem.theme.HomingTheme

@Composable
fun WebhookEditorScreen(state: WebhookEditorUiState, actions: WebhookEditorActions, modifier: Modifier = Modifier) {
    var confirmDelete by rememberSaveable { mutableStateOf(false) }
    HomingScaffold(
        modifier = modifier,
        topBar = {
            HomingTopAppBar(
                title = stringResource(if (state.isNew) R.string.webhooks_editor_add_title else R.string.webhooks_editor_edit_title),
                onBack = actions.onBack
            )
        }
    ) { padding ->
        CenteredContent(Modifier.padding(padding)) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(HomingTheme.spacing.gutter),
                verticalArrangement = Arrangement.spacedBy(HomingTheme.spacing.lg)
            ) {
                HomingTextField(
                    value = state.url,
                    onValueChange = actions.onUrlChange,
                    label = stringResource(R.string.webhooks_url_label),
                    placeholder = stringResource(R.string.webhooks_url_placeholder),
                    supportingText = stringResource(R.string.webhooks_url_help),
                    errorText = state.urlError?.let { urlErrorText(it) },
                    mono = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri, imeAction = ImeAction.Done)
                )
                EventPicker(selected = state.events, onToggle = actions.onToggleEvent)
                HomingPanel(contentPadding = PaddingValues()) {
                    HomingSwitchRow(
                        title = stringResource(R.string.webhooks_enabled_switch),
                        description = stringResource(R.string.webhooks_enabled_help),
                        checked = state.enabled,
                        onCheckedChange = actions.onEnabledChange
                    )
                }
                HomingButton(
                    text = stringResource(R.string.webhooks_save),
                    onClick = actions.onSave,
                    enabled = state.canSave,
                    loading = state.isSaving,
                    modifier = Modifier.fillMaxWidth()
                )
                if (!state.isNew) {
                    HomingButton(
                        text = stringResource(R.string.webhooks_delete),
                        onClick = { confirmDelete = true },
                        style = HomingButtonStyle.Outline,
                        loading = state.isDeleting,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
    if (confirmDelete) {
        ConfirmationDialog(
            title = stringResource(R.string.webhooks_delete_title),
            message = stringResource(R.string.webhooks_delete_body),
            confirmLabel = stringResource(R.string.webhooks_delete),
            destructive = true,
            onConfirm = {
                confirmDelete = false
                actions.onDelete()
            },
            onDismiss = { confirmDelete = false }
        )
    }
}

@Composable
private fun urlErrorText(error: WebhookUrlError): String = stringResource(
    when (error) {
        WebhookUrlError.Empty -> R.string.webhooks_url_error_empty
        WebhookUrlError.Invalid -> R.string.webhooks_url_error_invalid
        WebhookUrlError.NotHttps -> R.string.webhooks_url_error_https
    }
)

@Composable
private fun EventPicker(selected: Set<WebhookEvent>, onToggle: (WebhookEvent) -> Unit) {
    HomingPanel(contentPadding = PaddingValues(vertical = HomingTheme.spacing.xs)) {
        SectionHeader(
            title = stringResource(R.string.webhooks_events_title),
            modifier = Modifier.padding(horizontal = HomingTheme.spacing.gutter)
        )
        if (selected.isEmpty()) {
            Text(
                text = stringResource(R.string.webhooks_events_none),
                style = MaterialTheme.typography.bodySmall,
                color = HomingTheme.colors.warning,
                modifier = Modifier.padding(horizontal = HomingTheme.spacing.gutter)
            )
        }
        WebhookEvent.entries.forEachIndexed { index, event ->
            if (index > 0) HomingDivider(inset = true)
            EventRow(event = event, checked = event in selected, onToggle = { onToggle(event) })
        }
    }
}

@Composable
private fun EventRow(event: WebhookEvent, checked: Boolean, onToggle: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .toggleable(value = checked, role = Role.Checkbox, onValueChange = { onToggle() })
            .padding(horizontal = HomingTheme.spacing.gutter, vertical = HomingTheme.spacing.sm),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(
            checked = checked,
            onCheckedChange = null,
            colors = CheckboxDefaults.colors(checkedColor = HomingTheme.colors.brand, checkmarkColor = HomingTheme.colors.brandForeground)
        )
        Spacer(Modifier.width(HomingTheme.spacing.md))
        Column(Modifier.weight(1f)) {
            Text(text = event.title(), style = MaterialTheme.typography.titleSmall)
            Text(text = event.wireName, style = HomingTheme.text.monoSmall, color = HomingTheme.colors.mutedForeground)
        }
    }
}
