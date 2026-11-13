package com.homingsms.app.feature.settings

import android.text.format.DateFormat
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TimeInput
import androidx.compose.material3.TimePickerDefaults
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.homingsms.app.core.designsystem.component.HomingEditorSheet
import com.homingsms.app.core.designsystem.component.HomingTextField
import com.homingsms.app.core.designsystem.theme.HomingTheme

@Composable
internal fun NumberEditorDialog(
    title: String,
    label: String,
    initial: Int?,
    range: IntRange,
    onDismiss: () -> Unit,
    onConfirm: (Int?) -> Unit,
    offLabel: String? = null,
    help: String? = null
) {
    var text by rememberSaveable { mutableStateOf(initial?.toString().orEmpty()) }
    var off by rememberSaveable { mutableStateOf(initial == null && offLabel != null) }
    val parsed = text.trim().toIntOrNull()
    val valid = off || (parsed != null && parsed in range)
    HomingEditorSheet(
        title = title,
        onDismissRequest = onDismiss,
        confirmLabel = stringResource(R.string.settings_save),
        confirmEnabled = valid,
        onConfirm = { onConfirm(if (off) null else parsed) }
    ) {
        if (offLabel != null) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp)
                    .toggleable(value = off, role = Role.Switch, onValueChange = { off = it }),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = offLabel, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                Switch(checked = off, onCheckedChange = null)
            }
        }
        if (!off) {
            HomingTextField(
                value = text,
                onValueChange = { value -> text = value.filter { it.isDigit() } },
                label = label,
                supportingText = help ?: stringResource(R.string.settings_range, range.first.toString(), range.last.toString()),
                errorText = if (text.isNotEmpty() && !valid) {
                    stringResource(R.string.settings_range, range.first.toString(), range.last.toString())
                } else {
                    null
                },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
            )
        }
    }
}

@Composable
internal fun TextEditorDialog(
    title: String,
    label: String,
    initial: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
    help: String? = null,
    secret: Boolean = false,
    mono: Boolean = false,
    keyboardType: KeyboardType = KeyboardType.Text
) {
    var text by rememberSaveable { mutableStateOf(initial) }
    HomingEditorSheet(
        title = title,
        onDismissRequest = onDismiss,
        confirmLabel = stringResource(R.string.settings_save),
        confirmEnabled = text.isNotBlank(),
        onConfirm = { onConfirm(text.trim()) }
    ) {
        if (help != null) Text(text = help, style = MaterialTheme.typography.bodyMedium)
        HomingTextField(
            value = text,
            onValueChange = { text = it },
            label = label,
            mono = mono,
            singleLine = !mono,
            visualTransformation = if (secret) PasswordVisualTransformation() else VisualTransformation.None,
            keyboardOptions = KeyboardOptions(keyboardType = if (secret) KeyboardType.Password else keyboardType)
        )
    }
}

@Composable
internal fun RoutingDialog(current: SimRoutingMode, onDismiss: () -> Unit, onConfirm: (SimRoutingMode) -> Unit) {
    var selected by rememberSaveable { mutableStateOf(current) }
    HomingEditorSheet(
        title = stringResource(R.string.settings_routing),
        onDismissRequest = onDismiss,
        confirmLabel = stringResource(R.string.settings_save),
        onConfirm = { onConfirm(selected) }
    ) {
        Column(Modifier.selectableGroup()) {
            SimRoutingMode.entries.forEach { mode ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 56.dp)
                        .selectable(selected = mode == selected, role = Role.RadioButton, onClick = { selected = mode }),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = mode == selected,
                        onClick = null,
                        colors = RadioButtonDefaults.colors(selectedColor = HomingTheme.colors.brand)
                    )
                    Spacer(Modifier.width(HomingTheme.spacing.md))
                    Column(Modifier.weight(1f)) {
                        Text(text = mode.label(), style = MaterialTheme.typography.titleSmall)
                        Text(
                            text = mode.description(),
                            style = MaterialTheme.typography.bodySmall,
                            color = HomingTheme.colors.mutedForeground
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun TimeEditorDialog(title: String, initialMinutes: Int, onDismiss: () -> Unit, onConfirm: (Int) -> Unit) {
    val is24Hour = DateFormat.is24HourFormat(LocalContext.current)
    val state = rememberTimePickerState(initialHour = initialMinutes / 60, initialMinute = initialMinutes % 60, is24Hour = is24Hour)
    HomingEditorSheet(
        title = title,
        onDismissRequest = onDismiss,
        confirmLabel = stringResource(R.string.settings_save),
        onConfirm = { onConfirm(state.hour * 60 + state.minute) }
    ) {
        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            TimeInput(
                state = state,
                colors = TimePickerDefaults.colors(
                    timeSelectorSelectedContainerColor = HomingTheme.colors.brandSoft,
                    timeSelectorSelectedContentColor = HomingTheme.colors.foreground,
                    timeSelectorUnselectedContainerColor = HomingTheme.colors.muted
                )
            )
        }
    }
}

@Composable
internal fun SimRoutingMode.label(): String = stringResource(
    when (this) {
        SimRoutingMode.DefaultSim -> R.string.settings_routing_default
        SimRoutingMode.RoundRobin -> R.string.settings_routing_round_robin
        SimRoutingMode.LeastUsed -> R.string.settings_routing_least_used
        SimRoutingMode.CarrierMatch -> R.string.settings_routing_carrier
    }
)

@Composable
internal fun SimRoutingMode.description(): String = stringResource(
    when (this) {
        SimRoutingMode.DefaultSim -> R.string.settings_routing_default_body
        SimRoutingMode.RoundRobin -> R.string.settings_routing_round_robin_body
        SimRoutingMode.LeastUsed -> R.string.settings_routing_least_used_body
        SimRoutingMode.CarrierMatch -> R.string.settings_routing_carrier_body
    }
)
