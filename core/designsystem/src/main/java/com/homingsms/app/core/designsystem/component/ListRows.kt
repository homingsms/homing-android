package com.homingsms.app.core.designsystem.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.homingsms.app.core.designsystem.theme.HomingTheme

@Composable
fun HomingListRow(
    headline: String,
    modifier: Modifier = Modifier,
    supporting: String? = null,
    overline: String? = null,
    headlineStyle: TextStyle = MaterialTheme.typography.titleSmall,
    supportingMaxLines: Int = 2,
    enabled: Boolean = true,
    leading: (@Composable () -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null,
    onClickLabel: String? = null,
    onClick: (() -> Unit)? = null
) {
    val colors = HomingTheme.colors
    val clickModifier = if (onClick != null) {
        Modifier.clickable(enabled = enabled, onClickLabel = onClickLabel, role = Role.Button, onClick = onClick)
    } else {
        Modifier.semantics(mergeDescendants = true) {}
    }
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .then(clickModifier)
            .padding(horizontal = HomingTheme.spacing.gutter, vertical = HomingTheme.spacing.md),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (leading != null) {
            Box(contentAlignment = Alignment.Center) { leading() }
            Spacer(Modifier.width(HomingTheme.spacing.lg))
        }
        Column(Modifier.weight(1f)) {
            if (overline != null) {
                Text(text = overline, style = MaterialTheme.typography.labelSmall, color = colors.mutedForeground, maxLines = 1)
            }
            Text(
                text = headline,
                style = headlineStyle,
                color = if (enabled) colors.foreground else colors.mutedForeground,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            if (supporting != null) {
                Text(
                    text = supporting,
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.mutedForeground,
                    maxLines = supportingMaxLines,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        if (trailing != null) {
            Spacer(Modifier.width(HomingTheme.spacing.md))
            trailing()
        }
    }
}

@Composable
fun HomingNavigationRow(
    title: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    value: String? = null,
    leading: (@Composable () -> Unit)? = null
) {
    HomingListRow(
        headline = title,
        supporting = value,
        modifier = modifier,
        leading = leading,
        trailing = {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = HomingTheme.colors.mutedForeground
            )
        },
        onClick = onClick
    )
}

@Composable
fun HomingSwitchRow(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    description: String? = null,
    enabled: Boolean = true,
    leading: (@Composable () -> Unit)? = null
) {
    val colors = HomingTheme.colors
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .toggleable(value = checked, enabled = enabled, role = Role.Switch, onValueChange = onCheckedChange)
            .padding(horizontal = HomingTheme.spacing.gutter, vertical = HomingTheme.spacing.md),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (leading != null) {
            Box(contentAlignment = Alignment.Center) { leading() }
            Spacer(Modifier.width(HomingTheme.spacing.lg))
        }
        Column(Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                color = if (enabled) colors.foreground else colors.mutedForeground
            )
            if (description != null) {
                Text(text = description, style = MaterialTheme.typography.bodyMedium, color = colors.mutedForeground)
            }
        }
        Spacer(Modifier.width(HomingTheme.spacing.md))
        Switch(checked = checked, onCheckedChange = null, enabled = enabled)
    }
}

@Composable
fun HomingDivider(modifier: Modifier = Modifier, inset: Boolean = false) {
    HorizontalDivider(
        modifier = if (inset) modifier.padding(start = HomingTheme.spacing.gutter) else modifier,
        thickness = 1.dp,
        color = HomingTheme.colors.border
    )
}
