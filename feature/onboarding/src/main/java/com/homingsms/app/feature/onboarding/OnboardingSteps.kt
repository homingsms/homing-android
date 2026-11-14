package com.homingsms.app.feature.onboarding

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.homingsms.app.core.designsystem.component.HomingButton
import com.homingsms.app.core.designsystem.component.HomingButtonStyle
import com.homingsms.app.core.designsystem.component.HomingDivider
import com.homingsms.app.core.designsystem.component.HomingLogoMark
import com.homingsms.app.core.designsystem.component.HomingPanel
import com.homingsms.app.core.designsystem.component.StatusBadge
import com.homingsms.app.core.designsystem.icon.HomingIcons
import com.homingsms.app.core.designsystem.theme.HomingTheme
import com.homingsms.app.core.designsystem.theme.StatusTone
import com.homingsms.app.core.designsystem.util.formatCount

@Composable
internal fun WelcomeStep(state: OnboardingUiState, actions: OnboardingActions) {
    Column(
        modifier = Modifier.padding(top = HomingTheme.spacing.xxxl),
        verticalArrangement = Arrangement.spacedBy(HomingTheme.spacing.xxl)
    ) {
        HomingLogoMark(size = 56.dp)
        StepHeading(title = stringResource(R.string.onboarding_welcome_title), body = stringResource(R.string.onboarding_welcome_body))
        Column(verticalArrangement = Arrangement.spacedBy(HomingTheme.spacing.lg)) {
            WelcomePoint(HomingIcons.Messages, R.string.onboarding_welcome_send)
            WelcomePoint(HomingIcons.Inbox, R.string.onboarding_welcome_receive)
            WelcomePoint(HomingIcons.Transfer, R.string.onboarding_welcome_modes)
        }
        LegalSummary(stringResource(R.string.onboarding_legal_summary_title))
        Column(verticalArrangement = Arrangement.spacedBy(HomingTheme.spacing.lg)) {
            LegalAgreementRow(
                checked = state.termsAccepted,
                onCheckedChange = actions.onTermsAcceptedChange,
                onOpenLegal = actions.onOpenLegal
            )
            HomingButton(
                text = stringResource(R.string.onboarding_get_started),
                onClick = actions.onNext,
                enabled = state.canContinue,
                modifier = Modifier.fillMaxWidth()
            )
        }
        Text(
            text = stringResource(R.string.onboarding_welcome_time),
            style = MaterialTheme.typography.bodySmall,
            color = HomingTheme.colors.mutedForeground
        )
    }
}

@Composable
private fun WelcomePoint(icon: ImageVector, @StringRes text: Int) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .background(HomingTheme.colors.brandSoft, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(imageVector = icon, contentDescription = null, tint = HomingTheme.colors.brand)
        }
        Spacer(Modifier.width(HomingTheme.spacing.lg))
        Text(text = stringResource(text), style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
    }
}

@Composable
internal fun PermissionsStep(state: OnboardingUiState, actions: OnboardingActions) {
    StepHeading(title = stringResource(R.string.onboarding_permissions_title), body = stringResource(R.string.onboarding_permissions_body))
    val pending = state.pendingPermission
    if (pending != null) {
        PermissionCard(pending, actions)
    } else {
        HomingPanel(color = HomingTheme.colors.successSoft, borderColor = HomingTheme.colors.successSoft) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(HomingIcons.CheckCircle, contentDescription = null, tint = HomingTheme.colors.success)
                Spacer(Modifier.width(HomingTheme.spacing.md))
                Text(text = stringResource(R.string.onboarding_permissions_done), style = MaterialTheme.typography.bodyLarge)
            }
        }
    }
    HomingPanel(contentPadding = PaddingValues(vertical = HomingTheme.spacing.xs)) {
        state.permissions.forEachIndexed { index, item ->
            if (index > 0) HomingDivider(inset = true)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp)
                    .padding(horizontal = HomingTheme.spacing.gutter, vertical = HomingTheme.spacing.sm),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = item.permission.title(), style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                Spacer(Modifier.width(HomingTheme.spacing.sm))
                StatusBadge(label = item.status.label(), tone = item.status.tone())
            }
        }
    }
}

@Composable
private fun PermissionCard(item: PermissionItem, actions: OnboardingActions) {
    val colors = HomingTheme.colors
    HomingPanel(verticalArrangement = Arrangement.spacedBy(HomingTheme.spacing.md)) {
        Text(
            text = stringResource(
                R.string.onboarding_permission_progress,
                formatCount(item.permission.ordinal + 1),
                formatCount(OnboardingPermission.entries.size)
            ),
            style = MaterialTheme.typography.labelMedium,
            color = colors.mutedForeground
        )
        Text(text = item.permission.title(), style = MaterialTheme.typography.titleLarge, modifier = Modifier.semanticsHeading())
        LabeledText(label = stringResource(R.string.onboarding_permission_why), text = item.permission.why())
        LabeledText(label = stringResource(R.string.onboarding_permission_without), text = item.permission.without())
        when (item.status) {
            PermissionStatus.PermanentlyDenied -> {
                Text(
                    text = stringResource(R.string.onboarding_permission_blocked),
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.danger
                )
                HomingButton(
                    text = stringResource(R.string.onboarding_open_settings),
                    onClick = actions.onOpenAppSettings,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            else -> HomingButton(
                text = stringResource(
                    if (item.status == PermissionStatus.Denied) {
                        R.string.onboarding_permission_retry
                    } else {
                        R.string.onboarding_permission_allow
                    }
                ),
                onClick = { actions.onRequestPermission(item.permission) },
                modifier = Modifier.fillMaxWidth()
            )
        }
        if (!item.permission.required) {
            HomingButton(
                text = stringResource(R.string.onboarding_permission_skip),
                onClick = { actions.onSkipPermission(item.permission) },
                style = HomingButtonStyle.Text,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
internal fun LabeledText(label: String, text: String) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(text = label, style = MaterialTheme.typography.labelMedium, color = HomingTheme.colors.mutedForeground)
        Text(text = text, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
internal fun NumberedSteps(steps: List<Int>) {
    Column(verticalArrangement = Arrangement.spacedBy(HomingTheme.spacing.md)) {
        steps.forEachIndexed { index, res ->
            Row {
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .background(HomingTheme.colors.brandSoft, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = formatCount(index + 1), style = MaterialTheme.typography.labelSmall, color = HomingTheme.colors.brand)
                }
                Spacer(Modifier.width(HomingTheme.spacing.md))
                Text(text = stringResource(res), style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
            }
        }
    }
}

@Composable
internal fun BatteryStep(battery: BatteryUi, actions: OnboardingActions) {
    StepHeading(title = stringResource(R.string.onboarding_battery_title), body = stringResource(R.string.onboarding_battery_body))
    HomingPanel(verticalArrangement = Arrangement.spacedBy(HomingTheme.spacing.md)) {
        StatusBadge(
            label = stringResource(
                if (battery.ignoringOptimizations) R.string.onboarding_battery_ok else R.string.onboarding_battery_restricted
            ),
            tone = if (battery.ignoringOptimizations) StatusTone.Success else StatusTone.Warning
        )
        if (!battery.ignoringOptimizations) {
            HomingButton(
                text = stringResource(R.string.onboarding_battery_allow),
                onClick = actions.onRequestBatteryExemption,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
    HomingPanel(verticalArrangement = Arrangement.spacedBy(HomingTheme.spacing.md)) {
        Text(text = battery.maker.title(), style = MaterialTheme.typography.titleMedium, modifier = Modifier.semanticsHeading())
        NumberedSteps(battery.maker.steps())
        if (battery.maker != DeviceMaker.Other) {
            HomingButton(
                text = stringResource(R.string.onboarding_oem_open),
                onClick = actions.onOpenOemSettings,
                style = HomingButtonStyle.Outline,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
internal fun RestrictedSettingsStep(actions: OnboardingActions) {
    StepHeading(title = stringResource(R.string.onboarding_restricted_title), body = stringResource(R.string.onboarding_restricted_body))
    HomingPanel(verticalArrangement = Arrangement.spacedBy(HomingTheme.spacing.lg)) {
        NumberedSteps(
            listOf(
                R.string.onboarding_restricted_step_1,
                R.string.onboarding_restricted_step_2,
                R.string.onboarding_restricted_step_3,
                R.string.onboarding_restricted_step_4
            )
        )
        HomingButton(
            text = stringResource(R.string.onboarding_restricted_open),
            onClick = actions.onOpenAppInfo,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
internal fun ModeStep(selected: GatewayMode?, onSelect: (GatewayMode) -> Unit) {
    StepHeading(title = stringResource(R.string.onboarding_mode_title), body = stringResource(R.string.onboarding_mode_body))
    Column(Modifier.selectableGroup(), verticalArrangement = Arrangement.spacedBy(HomingTheme.spacing.md)) {
        GatewayMode.entries.forEach { mode ->
            val isSelected = mode == selected
            HomingPanel(
                modifier = Modifier
                    .fillMaxWidth()
                    .selectable(selected = isSelected, role = Role.RadioButton, onClick = { onSelect(mode) }),
                color = if (isSelected) HomingTheme.colors.brandSoft else HomingTheme.colors.surface,
                borderColor = if (isSelected) HomingTheme.colors.brand else HomingTheme.colors.border
            ) {
                Row(verticalAlignment = Alignment.Top) {
                    RadioButton(
                        selected = isSelected,
                        onClick = null,
                        colors = RadioButtonDefaults.colors(selectedColor = HomingTheme.colors.brand)
                    )
                    Spacer(Modifier.width(HomingTheme.spacing.md))
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = if (mode == GatewayMode.Local) HomingIcons.Server else HomingIcons.Cloud,
                                contentDescription = null,
                                tint = HomingTheme.colors.mutedForeground,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(Modifier.width(HomingTheme.spacing.sm))
                            Text(text = mode.title(), style = MaterialTheme.typography.titleMedium)
                        }
                        Text(
                            text = mode.description(),
                            style = MaterialTheme.typography.bodyMedium,
                            color = HomingTheme.colors.mutedForeground
                        )
                    }
                }
            }
        }
    }
}
