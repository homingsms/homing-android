package com.homingsms.app.core.designsystem.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckBox
import androidx.compose.material.icons.filled.CheckBoxOutlineBlank
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.toggleableState
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.unit.dp
import com.homingsms.app.core.designsystem.R
import com.homingsms.app.core.designsystem.theme.HomingTheme
import com.homingsms.app.core.designsystem.util.formatCount

@Composable
fun FilterChipRow(
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(horizontal = HomingTheme.spacing.gutter),
    content: @Composable RowScope.() -> Unit
) {
    Row(
        modifier = modifier
            .horizontalScroll(rememberScrollState())
            .padding(contentPadding),
        horizontalArrangement = Arrangement.spacedBy(HomingTheme.spacing.sm),
        verticalAlignment = Alignment.CenterVertically,
        content = content
    )
}

@Composable
fun HomingFilterChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    trailingIcon: (@Composable () -> Unit)? = null
) {
    val colors = HomingTheme.colors
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(text = label, style = MaterialTheme.typography.labelLarge) },
        modifier = modifier,
        shape = CircleShape,
        leadingIcon = if (selected) {
            { Icon(imageVector = Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(18.dp)) }
        } else {
            null
        },
        trailingIcon = trailingIcon,
        colors = FilterChipDefaults.filterChipColors(
            containerColor = colors.surface,
            labelColor = colors.foreground,
            iconColor = colors.mutedForeground,
            selectedContainerColor = colors.brandSoft,
            selectedLabelColor = colors.foreground,
            selectedLeadingIconColor = colors.brand,
            selectedTrailingIconColor = colors.foreground
        ),
        border = FilterChipDefaults.filterChipBorder(
            enabled = true,
            selected = selected,
            borderColor = colors.border,
            selectedBorderColor = colors.brand.copy(alpha = 0.4f)
        )
    )
}

@Composable
fun <T> FilterMenuChip(
    label: String,
    options: List<FilterOption<T>>,
    selected: Set<T>,
    onToggle: (T) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    val chipLabel = when (selected.size) {
        0 -> label
        1 -> options.firstOrNull { it.value in selected }?.label ?: label
        else -> stringResource(R.string.homing_filter_count, label, formatCount(selected.size))
    }
    Box(modifier = modifier) {
        HomingFilterChip(
            label = chipLabel,
            selected = selected.isNotEmpty(),
            onClick = { expanded = true },
            trailingIcon = {
                Icon(imageVector = Icons.Filled.ArrowDropDown, contentDescription = null, modifier = Modifier.size(18.dp))
            }
        )
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            shape = MaterialTheme.shapes.small,
            containerColor = HomingTheme.colors.surface,
            border = BorderStroke(1.dp, HomingTheme.colors.border)
        ) {
            options.forEach { option ->
                val checked = option.value in selected
                DropdownMenuItem(
                    text = { Text(text = option.label, style = MaterialTheme.typography.bodyLarge) },
                    onClick = { onToggle(option.value) },
                    leadingIcon = {
                        Icon(
                            imageVector = if (checked) Icons.Filled.CheckBox else Icons.Filled.CheckBoxOutlineBlank,
                            contentDescription = null,
                            tint = if (checked) HomingTheme.colors.brand else HomingTheme.colors.mutedForeground
                        )
                    },
                    modifier = Modifier.semantics {
                        role = Role.Checkbox
                        toggleableState = ToggleableState(checked)
                    }
                )
            }
        }
    }
}
