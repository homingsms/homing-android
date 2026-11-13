package com.homingsms.app.core.designsystem.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.homingsms.app.core.designsystem.R
import com.homingsms.app.core.designsystem.theme.HomingTheme

@Composable
fun ConfirmationDialog(
    title: String,
    message: String,
    confirmLabel: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    destructive: Boolean = false,
    dismissLabel: String = stringResource(R.string.homing_action_cancel),
    icon: ImageVector? = null
) {
    val colors = HomingTheme.colors
    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = modifier,
        icon = icon?.let { { Icon(imageVector = it, contentDescription = null) } },
        title = { Text(text = title, style = MaterialTheme.typography.titleLarge) },
        text = { Text(text = message, style = MaterialTheme.typography.bodyMedium) },
        confirmButton = {
            TextButton(
                onClick = onConfirm,
                colors = ButtonDefaults.textButtonColors(contentColor = if (destructive) colors.danger else colors.brand)
            ) { Text(confirmLabel) }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                colors = ButtonDefaults.textButtonColors(contentColor = colors.foreground)
            ) { Text(dismissLabel) }
        },
        shape = MaterialTheme.shapes.extraLarge,
        containerColor = colors.surface,
        iconContentColor = if (destructive) colors.danger else colors.brand,
        titleContentColor = colors.foreground,
        textContentColor = colors.mutedForeground
    )
}

@Composable
fun HomingDialog(
    title: String,
    onDismiss: () -> Unit,
    confirmLabel: String,
    onConfirm: () -> Unit,
    modifier: Modifier = Modifier,
    confirmEnabled: Boolean = true,
    dismissLabel: String? = stringResource(R.string.homing_action_cancel),
    dismissOnOutsideClick: Boolean = true,
    content: @Composable ColumnScope.() -> Unit
) {
    val colors = HomingTheme.colors
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            dismissOnBackPress = dismissOnOutsideClick,
            dismissOnClickOutside = dismissOnOutsideClick
        )
    ) {
        Surface(
            modifier = modifier.widthIn(min = 280.dp, max = 560.dp),
            shape = MaterialTheme.shapes.extraLarge,
            color = colors.surface,
            contentColor = colors.foreground
        ) {
            Column(Modifier.padding(top = 24.dp, bottom = 12.dp)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier
                        .padding(horizontal = 24.dp)
                        .semantics { heading() }
                )
                Column(
                    modifier = Modifier
                        .weight(1f, fill = false)
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 24.dp, vertical = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(HomingTheme.spacing.md),
                    content = content
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp),
                    horizontalArrangement = Arrangement.End
                ) {
                    if (dismissLabel != null) {
                        TextButton(
                            onClick = onDismiss,
                            colors = ButtonDefaults.textButtonColors(contentColor = colors.foreground)
                        ) { Text(dismissLabel) }
                    }
                    TextButton(
                        onClick = onConfirm,
                        enabled = confirmEnabled,
                        colors = ButtonDefaults.textButtonColors(contentColor = colors.brand)
                    ) { Text(confirmLabel) }
                }
            }
        }
    }
}
