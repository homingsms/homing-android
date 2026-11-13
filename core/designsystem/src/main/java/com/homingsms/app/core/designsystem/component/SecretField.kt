package com.homingsms.app.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.homingsms.app.core.designsystem.R
import com.homingsms.app.core.designsystem.theme.HomingTheme
import com.homingsms.app.core.designsystem.util.rememberCopyToClipboard

private const val MASK_LENGTH = 16
private const val MASK_CHAR = '•'

fun maskSecret(secret: String, visiblePrefixLength: Int): String {
    val prefix = secret.take(visiblePrefixLength.coerceIn(0, secret.length))
    val hiddenLength = (secret.length - prefix.length).coerceIn(0, MASK_LENGTH)
    return prefix + MASK_CHAR.toString().repeat(hiddenLength)
}

@Composable
fun SecretField(
    label: String,
    secret: String,
    modifier: Modifier = Modifier,
    visiblePrefixLength: Int = 0,
    initiallyRevealed: Boolean = false,
    onShowCopiedConfirmation: () -> Unit = {}
) {
    var revealed by rememberSaveable { mutableStateOf(initiallyRevealed) }
    val copy = rememberCopyToClipboard(onShowCopiedConfirmation)
    val colors = HomingTheme.colors
    val hiddenDescription = stringResource(R.string.homing_secret_hidden, label)
    Column(modifier = modifier.fillMaxWidth()) {
        Text(text = label, style = MaterialTheme.typography.labelMedium, color = colors.mutedForeground)
        Spacer(Modifier.height(HomingTheme.spacing.xs))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 48.dp)
                .background(colors.muted, MaterialTheme.shapes.small)
                .padding(start = HomingTheme.spacing.md),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = if (revealed) secret else maskSecret(secret, visiblePrefixLength),
                style = HomingTheme.text.mono.copy(textAlign = TextAlign.Left),
                color = colors.foreground,
                modifier = Modifier
                    .weight(1f)
                    .padding(vertical = HomingTheme.spacing.sm)
                    .then(if (revealed) Modifier else Modifier.semantics { contentDescription = hiddenDescription })
            )
            IconButton(onClick = { revealed = !revealed }) {
                Icon(
                    imageVector = if (revealed) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                    contentDescription = if (revealed) {
                        stringResource(R.string.homing_secret_hide, label)
                    } else {
                        stringResource(R.string.homing_secret_show, label)
                    },
                    tint = colors.mutedForeground
                )
            }
            IconButton(onClick = { copy.copy(label, secret, true) }) {
                Icon(
                    imageVector = Icons.Outlined.ContentCopy,
                    contentDescription = stringResource(R.string.homing_secret_copy, label),
                    tint = colors.mutedForeground
                )
            }
        }
    }
}

@Composable
fun CodeBlock(
    code: String,
    copyLabel: String,
    modifier: Modifier = Modifier,
    sensitive: Boolean = false,
    onShowCopiedConfirmation: () -> Unit = {}
) {
    val copy = rememberCopyToClipboard(onShowCopiedConfirmation)
    val colors = HomingTheme.colors
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
        Row(
            modifier = modifier
                .fillMaxWidth()
                .background(colors.muted, MaterialTheme.shapes.medium)
                .padding(start = HomingTheme.spacing.md),
            verticalAlignment = Alignment.Top
        ) {
            Text(
                text = code,
                style = HomingTheme.text.mono.copy(textAlign = TextAlign.Left),
                color = colors.foreground,
                softWrap = false,
                modifier = Modifier
                    .weight(1f)
                    .horizontalScroll(rememberScrollState())
                    .padding(vertical = HomingTheme.spacing.md)
            )
            IconButton(onClick = { copy.copy(copyLabel, code, sensitive) }) {
                Icon(
                    imageVector = Icons.Outlined.ContentCopy,
                    contentDescription = stringResource(R.string.homing_secret_copy, copyLabel),
                    tint = colors.mutedForeground
                )
            }
        }
    }
}

@Composable
fun CopyableValue(label: String, value: String, modifier: Modifier = Modifier, onShowCopiedConfirmation: () -> Unit = {}) {
    val copy = rememberCopyToClipboard(onShowCopiedConfirmation)
    val colors = HomingTheme.colors
    Column(modifier = modifier.fillMaxWidth()) {
        Text(text = label, style = MaterialTheme.typography.labelMedium, color = colors.mutedForeground)
        Spacer(Modifier.height(HomingTheme.spacing.xs))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 48.dp)
                .background(colors.muted, MaterialTheme.shapes.small)
                .padding(start = HomingTheme.spacing.md),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = value,
                style = HomingTheme.text.mono.copy(textAlign = TextAlign.Left),
                color = colors.foreground,
                modifier = Modifier
                    .weight(1f)
                    .padding(vertical = HomingTheme.spacing.sm)
            )
            IconButton(onClick = { copy.copy(label, value, false) }) {
                Icon(
                    imageVector = Icons.Outlined.ContentCopy,
                    contentDescription = stringResource(R.string.homing_secret_copy, label),
                    tint = colors.mutedForeground
                )
            }
        }
    }
}
