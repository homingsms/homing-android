package com.homingsms.app.feature.settings

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import com.homingsms.app.core.designsystem.component.CenteredContent
import com.homingsms.app.core.designsystem.component.GroupLabel
import com.homingsms.app.core.designsystem.component.HomingDivider
import com.homingsms.app.core.designsystem.component.HomingNavigationRow
import com.homingsms.app.core.designsystem.component.HomingPanel
import com.homingsms.app.core.designsystem.component.HomingScaffold
import com.homingsms.app.core.designsystem.component.HomingSwitchRow
import com.homingsms.app.core.designsystem.component.HomingTopAppBar
import com.homingsms.app.core.designsystem.theme.CAP_WARNING_PERCENT
import com.homingsms.app.core.designsystem.theme.HomingTheme
import com.homingsms.app.core.designsystem.util.formatClockTime
import com.homingsms.app.core.designsystem.util.formatCount
import com.homingsms.app.core.designsystem.util.formatPercent
import com.homingsms.app.core.designsystem.util.simLabel

@Composable
fun SettingsScreen(state: SettingsUiState, actions: SettingsActions, modifier: Modifier = Modifier) {
    var editor by remember { mutableStateOf<SettingsEditor?>(null) }
    val open: (SettingsEditor) -> Unit = { editor = it }
    HomingScaffold(
        modifier = modifier,
        topBar = { HomingTopAppBar(title = stringResource(R.string.settings_title)) }
    ) { padding ->
        CenteredContent(Modifier.padding(padding)) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    start = HomingTheme.spacing.gutter,
                    end = HomingTheme.spacing.gutter,
                    bottom = HomingTheme.spacing.xxl
                )
            ) {
                sendingSection(state.sending, actions, open)
                receivingSection(state.receiving, actions, open)
                serverSection(state.server, actions, open)
                securitySection(state.security, actions, open)
                advancedSection(state.advanced, actions)
                section("about", R.string.settings_group_about) {
                    HomingNavigationRow(
                        title = stringResource(R.string.settings_about),
                        value = stringResource(R.string.settings_version, state.appVersion),
                        onClick = { actions.onNavigate(SettingsDestination.About) }
                    )
                }
            }
        }
    }
    editor?.let { SettingsEditorHost(it, state, actions, onClose = { editor = null }) }
}

private fun LazyListScope.section(key: String, title: Int, content: @Composable () -> Unit) {
    item(key = "$key-label") { GroupLabel(text = stringResource(title), modifier = Modifier.padding(top = HomingTheme.spacing.sm)) }
    item(key = key) {
        HomingPanel(contentPadding = PaddingValues(vertical = HomingTheme.spacing.xs)) { content() }
    }
}

@Composable
private fun RowDivider() = HomingDivider(inset = true)

private fun LazyListScope.sendingSection(sending: SendingSettings, actions: SettingsActions, open: (SettingsEditor) -> Unit) {
    section("sending", R.string.settings_group_sending) {
        HomingNavigationRow(
            title = stringResource(R.string.settings_rate_limit),
            value = sending.rateLimitPerMinute?.let { stringResource(R.string.settings_per_minute, formatCount(it)) }
                ?: stringResource(R.string.settings_no_limit),
            onClick = { open(SettingsEditor.RateLimit) }
        )
        RowDivider()
        HomingNavigationRow(
            title = stringResource(R.string.settings_send_delay),
            value = sending.delayBetweenSeconds?.let {
                pluralStringResource(R.plurals.settings_seconds, it, formatCount(it))
            } ?: stringResource(R.string.settings_no_delay),
            onClick = { open(SettingsEditor.SendDelay) }
        )
        sending.dailyCaps.forEach { sim ->
            RowDivider()
            HomingNavigationRow(
                title = stringResource(R.string.settings_daily_cap, simLabel(sim.slot)),
                value = sim.dailyCap?.let { stringResource(R.string.settings_per_day, formatCount(it)) }
                    ?: stringResource(R.string.settings_no_cap),
                onClick = { open(SettingsEditor.DailyCap(sim.slot, sim.dailyCap)) }
            )
        }
        RowDivider()
        WorkingHoursRows(sending.workingHours, actions, open)
        RowDivider()
        HomingNavigationRow(
            title = stringResource(R.string.settings_routing),
            value = sending.routing.label(),
            onClick = { open(SettingsEditor.Routing) }
        )
        RowDivider()
        HomingSwitchRow(
            title = stringResource(R.string.settings_failover),
            description = stringResource(R.string.settings_failover_body),
            checked = sending.failoverToOtherSim,
            onCheckedChange = { actions.onChange(SettingsChange.Failover(it)) }
        )
        RowDivider()
        HomingSwitchRow(
            title = stringResource(R.string.settings_delivery_reports),
            description = stringResource(R.string.settings_delivery_reports_body),
            checked = sending.deliveryReports,
            onCheckedChange = { actions.onChange(SettingsChange.DeliveryReports(it)) }
        )
    }
}

private fun LazyListScope.receivingSection(receiving: ReceivingSettings, actions: SettingsActions, open: (SettingsEditor) -> Unit) {
    section("receiving", R.string.settings_group_receiving) {
        HomingSwitchRow(
            title = stringResource(R.string.settings_forward_sms),
            checked = receiving.forwardSms,
            onCheckedChange = { actions.onChange(SettingsChange.ForwardSms(it)) }
        )
        RowDivider()
        HomingSwitchRow(
            title = stringResource(R.string.settings_forward_mms),
            checked = receiving.forwardMms,
            onCheckedChange = { actions.onChange(SettingsChange.ForwardMms(it)) }
        )
        RowDivider()
        HomingSwitchRow(
            title = stringResource(R.string.settings_forward_data),
            checked = receiving.forwardDataSms,
            onCheckedChange = { actions.onChange(SettingsChange.ForwardDataSms(it)) }
        )
        if (receiving.forwardDataSms) {
            HomingNavigationRow(
                title = stringResource(R.string.settings_data_port),
                value = receiving.dataSmsPort?.toString() ?: stringResource(R.string.settings_any_port),
                onClick = { open(SettingsEditor.DataPort) }
            )
        }
        RowDivider()
        HomingSwitchRow(
            title = stringResource(R.string.settings_opt_out),
            description = stringResource(R.string.settings_opt_out_body),
            checked = receiving.optOutEnabled,
            onCheckedChange = { actions.onChange(SettingsChange.OptOutEnabled(it)) }
        )
        if (receiving.optOutEnabled) {
            HomingNavigationRow(
                title = stringResource(R.string.settings_opt_out_keywords),
                value = receiving.optOutKeywords.joinToString(", "),
                onClick = { open(SettingsEditor.OptOutKeywords) }
            )
        }
    }
}

private fun LazyListScope.serverSection(server: ServerSettings, actions: SettingsActions, open: (SettingsEditor) -> Unit) {
    section("server", R.string.settings_group_server) {
        HomingSwitchRow(
            title = stringResource(R.string.settings_local_server),
            checked = server.localEnabled,
            onCheckedChange = { actions.onChange(SettingsChange.LocalServer(it)) }
        )
        RowDivider()
        HomingNavigationRow(
            title = stringResource(R.string.settings_local_port),
            value = server.localPort.toString(),
            onClick = { open(SettingsEditor.LocalPort) }
        )
        RowDivider()
        HomingNavigationRow(
            title = stringResource(R.string.settings_cloud),
            value = stringResource(if (server.cloudPaired) R.string.settings_cloud_paired else R.string.settings_cloud_not_paired),
            onClick = { actions.onNavigate(SettingsDestination.CloudPairing) }
        )
        RowDivider()
        HomingNavigationRow(
            title = stringResource(R.string.settings_cloud_url),
            value = server.cloudServerUrl,
            onClick = { open(SettingsEditor.CloudUrl) }
        )
        RowDivider()
        HomingNavigationRow(
            title = stringResource(R.string.settings_pull_interval),
            value = pluralStringResource(R.plurals.settings_seconds, server.pullIntervalSeconds, formatCount(server.pullIntervalSeconds)),
            onClick = { open(SettingsEditor.PullInterval) }
        )
    }
}

private fun LazyListScope.securitySection(security: SecuritySettings, actions: SettingsActions, open: (SettingsEditor) -> Unit) {
    section("security", R.string.settings_group_security) {
        HomingNavigationRow(
            title = stringResource(R.string.settings_api_keys),
            value = pluralStringResource(R.plurals.settings_key_count, security.apiKeyCount, formatCount(security.apiKeyCount)),
            onClick = { actions.onNavigate(SettingsDestination.ApiKeys) }
        )
        RowDivider()
        HomingNavigationRow(
            title = stringResource(R.string.settings_webhooks),
            value = pluralStringResource(R.plurals.settings_webhook_count, security.webhookCount, formatCount(security.webhookCount)),
            onClick = { actions.onNavigate(SettingsDestination.Webhooks) }
        )
        RowDivider()
        HomingSwitchRow(
            title = stringResource(R.string.settings_encryption),
            description = stringResource(R.string.settings_encryption_body),
            checked = security.encryptionEnabled,
            onCheckedChange = { actions.onChange(SettingsChange.Encryption(it)) }
        )
        if (security.encryptionEnabled) {
            HomingNavigationRow(
                title = stringResource(R.string.settings_passphrase),
                value = stringResource(
                    if (security.encryptionPassphraseSet) R.string.settings_passphrase_set else R.string.settings_passphrase_missing
                ),
                onClick = { open(SettingsEditor.Passphrase) }
            )
        }
    }
}

private fun LazyListScope.advancedSection(advanced: AdvancedSettings, actions: SettingsActions) {
    section("advanced", R.string.settings_group_advanced) {
        HomingSwitchRow(
            title = stringResource(R.string.settings_keep_alive),
            description = stringResource(R.string.settings_keep_alive_body),
            checked = advanced.keepAliveEnabled,
            onCheckedChange = { actions.onChange(SettingsChange.KeepAlive(it)) }
        )
        RowDivider()
        HomingNavigationRow(
            title = stringResource(R.string.settings_battery),
            value = stringResource(
                if (advanced.batteryOptimizationIgnored) R.string.settings_battery_ok else R.string.settings_battery_restricted
            ),
            onClick = { actions.onNavigate(SettingsDestination.BatteryOptimization) }
        )
        RowDivider()
        HomingNavigationRow(
            title = stringResource(R.string.settings_sms_limit),
            value = stringResource(R.string.settings_sms_limit_body),
            onClick = { actions.onNavigate(SettingsDestination.OutgoingSmsLimit) }
        )
        RowDivider()
        HomingSwitchRow(
            title = stringResource(R.string.settings_verbose_logs),
            checked = advanced.verboseLogging,
            onCheckedChange = { actions.onChange(SettingsChange.VerboseLogging(it)) }
        )
        RowDivider()
        HomingNavigationRow(
            title = stringResource(R.string.settings_export_logs),
            onClick = { actions.onNavigate(SettingsDestination.ExportLogs) }
        )
        RowDivider()
        HomingNavigationRow(
            title = stringResource(R.string.settings_run_setup),
            onClick = { actions.onNavigate(SettingsDestination.Onboarding) }
        )
    }
}

@Composable
private fun formatMinutes(minutes: Int): String {
    val calendar = java.util.Calendar.getInstance().apply {
        set(java.util.Calendar.HOUR_OF_DAY, minutes / 60)
        set(java.util.Calendar.MINUTE, minutes % 60)
    }
    return formatClockTime(calendar.timeInMillis)
}

@Composable
private fun SettingsEditorHost(editor: SettingsEditor, state: SettingsUiState, actions: SettingsActions, onClose: () -> Unit) {
    val apply: (SettingsChange) -> Unit = {
        actions.onChange(it)
        onClose()
    }
    when (editor) {
        SettingsEditor.RateLimit -> NumberEditorDialog(
            title = stringResource(R.string.settings_rate_limit),
            label = stringResource(R.string.settings_rate_limit_label),
            initial = state.sending.rateLimitPerMinute,
            range = 1..600,
            offLabel = stringResource(R.string.settings_no_limit),
            onDismiss = onClose,
            onConfirm = { apply(SettingsChange.RateLimit(it)) }
        )

        SettingsEditor.SendDelay -> NumberEditorDialog(
            title = stringResource(R.string.settings_send_delay),
            label = stringResource(R.string.settings_seconds_label),
            initial = state.sending.delayBetweenSeconds,
            range = 1..3600,
            offLabel = stringResource(R.string.settings_no_delay),
            onDismiss = onClose,
            onConfirm = { apply(SettingsChange.SendDelay(it)) }
        )

        is SettingsEditor.DailyCap -> NumberEditorDialog(
            title = stringResource(R.string.settings_daily_cap, simLabel(editor.slot)),
            label = stringResource(R.string.settings_daily_cap_label),
            initial = editor.current,
            range = 1..100_000,
            offLabel = stringResource(R.string.settings_no_cap),
            help = stringResource(R.string.settings_daily_cap_help, formatPercent(CAP_WARNING_PERCENT / 100f)),
            onDismiss = onClose,
            onConfirm = { apply(SettingsChange.DailyCap(editor.slot, it)) }
        )

        SettingsEditor.WorkStart -> TimeEditorDialog(
            title = stringResource(R.string.settings_working_start),
            initialMinutes = state.sending.workingHours.startMinutes,
            onDismiss = onClose,
            onConfirm = { apply(SettingsChange.WorkingHoursRange(it, state.sending.workingHours.endMinutes)) }
        )

        SettingsEditor.WorkEnd -> TimeEditorDialog(
            title = stringResource(R.string.settings_working_end),
            initialMinutes = state.sending.workingHours.endMinutes,
            onDismiss = onClose,
            onConfirm = { apply(SettingsChange.WorkingHoursRange(state.sending.workingHours.startMinutes, it)) }
        )

        SettingsEditor.Routing -> RoutingDialog(
            current = state.sending.routing,
            onDismiss = onClose,
            onConfirm = { apply(SettingsChange.Routing(it)) }
        )

        else -> SecondaryEditorHost(editor, state, apply, onClose)
    }
}

@Composable
private fun SecondaryEditorHost(editor: SettingsEditor, state: SettingsUiState, apply: (SettingsChange) -> Unit, onClose: () -> Unit) {
    when (editor) {
        SettingsEditor.DataPort -> NumberEditorDialog(
            title = stringResource(R.string.settings_data_port),
            label = stringResource(R.string.settings_port_label),
            initial = state.receiving.dataSmsPort,
            range = 1..65_535,
            offLabel = stringResource(R.string.settings_any_port),
            onDismiss = onClose,
            onConfirm = { apply(SettingsChange.DataSmsPort(it)) }
        )

        SettingsEditor.OptOutKeywords -> TextEditorDialog(
            title = stringResource(R.string.settings_opt_out_keywords),
            label = stringResource(R.string.settings_opt_out_keywords_label),
            initial = state.receiving.optOutKeywords.joinToString(", "),
            help = stringResource(R.string.settings_opt_out_keywords_help),
            onDismiss = onClose,
            onConfirm = { text ->
                apply(SettingsChange.OptOutKeywords(text.split(',').map { it.trim().uppercase() }.filter { it.isNotEmpty() }))
            }
        )

        SettingsEditor.LocalPort -> NumberEditorDialog(
            title = stringResource(R.string.settings_local_port),
            label = stringResource(R.string.settings_port_label),
            initial = state.server.localPort,
            range = 1024..65_535,
            onDismiss = onClose,
            onConfirm = { it?.let { port -> apply(SettingsChange.LocalPort(port)) } }
        )

        SettingsEditor.CloudUrl -> TextEditorDialog(
            title = stringResource(R.string.settings_cloud_url),
            label = stringResource(R.string.settings_cloud_url_label),
            initial = state.server.cloudServerUrl,
            help = stringResource(R.string.settings_cloud_url_help),
            mono = true,
            keyboardType = KeyboardType.Uri,
            onDismiss = onClose,
            onConfirm = { apply(SettingsChange.CloudServerUrl(it)) }
        )

        SettingsEditor.PullInterval -> NumberEditorDialog(
            title = stringResource(R.string.settings_pull_interval),
            label = stringResource(R.string.settings_seconds_label),
            initial = state.server.pullIntervalSeconds,
            range = 15..3600,
            onDismiss = onClose,
            onConfirm = { it?.let { seconds -> apply(SettingsChange.PullInterval(seconds)) } }
        )

        SettingsEditor.Passphrase -> TextEditorDialog(
            title = stringResource(R.string.settings_passphrase),
            label = stringResource(R.string.settings_passphrase_label),
            initial = "",
            help = stringResource(R.string.settings_passphrase_help),
            secret = true,
            onDismiss = onClose,
            onConfirm = { apply(SettingsChange.EncryptionPassphrase(it)) }
        )

        else -> Unit
    }
}

@Composable
private fun WorkingHoursRows(hours: WorkingHours, actions: SettingsActions, open: (SettingsEditor) -> Unit) {
    HomingSwitchRow(
        title = stringResource(R.string.settings_working_hours),
        description = stringResource(R.string.settings_working_hours_body),
        checked = hours.enabled,
        onCheckedChange = { actions.onChange(SettingsChange.WorkingHoursEnabled(it)) }
    )
    if (hours.enabled) {
        HomingNavigationRow(
            title = stringResource(R.string.settings_working_start),
            value = formatMinutes(hours.startMinutes),
            onClick = { open(SettingsEditor.WorkStart) }
        )
        HomingNavigationRow(
            title = stringResource(R.string.settings_working_end),
            value = formatMinutes(hours.endMinutes),
            onClick = { open(SettingsEditor.WorkEnd) }
        )
    }
}
