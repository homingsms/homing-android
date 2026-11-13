package com.homingsms.app.feature.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BatteryChargingFull
import androidx.compose.material.icons.outlined.BatteryStd
import androidx.compose.material.icons.outlined.Cloud
import androidx.compose.material.icons.outlined.Dns
import androidx.compose.material.icons.outlined.NetworkCheck
import androidx.compose.material.icons.outlined.SimCard
import androidx.compose.material.icons.outlined.Sync
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.homingsms.app.core.designsystem.component.CenteredContent
import com.homingsms.app.core.designsystem.component.CodeBlock
import com.homingsms.app.core.designsystem.component.EmptyState
import com.homingsms.app.core.designsystem.component.HomingButton
import com.homingsms.app.core.designsystem.component.HomingButtonStyle
import com.homingsms.app.core.designsystem.component.HomingDivider
import com.homingsms.app.core.designsystem.component.HomingListRow
import com.homingsms.app.core.designsystem.component.HomingPanel
import com.homingsms.app.core.designsystem.component.HomingScaffold
import com.homingsms.app.core.designsystem.component.HomingSwitchRow
import com.homingsms.app.core.designsystem.component.HomingWordmark
import com.homingsms.app.core.designsystem.component.HourlyActivityChart
import com.homingsms.app.core.designsystem.component.LoadingState
import com.homingsms.app.core.designsystem.component.SectionHeader
import com.homingsms.app.core.designsystem.component.SimCard
import com.homingsms.app.core.designsystem.component.StatTile
import com.homingsms.app.core.designsystem.component.StatusHero
import com.homingsms.app.core.designsystem.theme.HomingTheme
import com.homingsms.app.core.designsystem.theme.StatusTone
import com.homingsms.app.core.designsystem.util.formatCount
import com.homingsms.app.core.designsystem.util.formatRelativeTime
import kotlinx.coroutines.launch

@Composable
fun HomeScreen(state: HomeUiState, actions: HomeActions, modifier: Modifier = Modifier) {
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val copied = stringResource(com.homingsms.app.core.designsystem.R.string.homing_action_copied)
    val confirmCopy: () -> Unit = { scope.launch { snackbar.showSnackbar(copied) } }
    HomingScaffold(modifier = modifier, snackbarHostState = snackbar) { padding ->
        CenteredContent(Modifier.padding(padding)) {
            if (state.isLoading) {
                Column {
                    HomingWordmark(Modifier.padding(HomingTheme.spacing.gutter))
                    LoadingState()
                }
            } else {
                HomeContent(state = state, actions = actions, onShowCopiedConfirmation = confirmCopy)
            }
        }
    }
}

@Composable
private fun HomeContent(state: HomeUiState, actions: HomeActions, onShowCopiedConfirmation: () -> Unit) {
    val gap = HomingTheme.spacing.lg
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(HomingTheme.spacing.gutter),
        verticalArrangement = Arrangement.spacedBy(gap)
    ) {
        item(key = "wordmark") { HomingWordmark(Modifier.padding(vertical = HomingTheme.spacing.xs)) }
        item(key = "hero") { HomeStatusHero(state, actions, onShowCopiedConfirmation) }
        if (state.issues.size > 1) {
            item(key = "more-issues") { MoreIssues(state.issues.drop(1), actions) }
        }
        item(key = "today") { TodayPanel(state, actions) }
        item(key = "sims-header") { SectionHeader(title = stringResource(R.string.home_sims_title)) }
        if (state.sims.isEmpty()) {
            item(key = "sims-empty") {
                HomingPanel(contentPadding = PaddingValues()) {
                    EmptyState(
                        icon = Icons.Outlined.SimCard,
                        title = stringResource(R.string.home_sims_empty_title),
                        body = stringResource(R.string.home_sims_empty_body)
                    )
                }
            }
        } else {
            items(state.sims, key = { "sim-${it.slot}" }) { sim ->
                SimCard(
                    slot = sim.slot,
                    carrier = sim.carrier,
                    phoneNumber = sim.phoneNumber,
                    signalLevel = sim.signalLevel,
                    enabled = sim.enabled,
                    sentToday = sim.sentToday,
                    dailyCap = sim.dailyCap,
                    onClick = { actions.onSimClick(sim.slot) }
                )
            }
        }
        item(key = "device") { DevicePanel(state.device) }
        item(key = "connections") { ConnectionsPanel(state, actions) }
    }
}

@Composable
private fun HomeStatusHero(state: HomeUiState, actions: HomeActions, onShowCopiedConfirmation: () -> Unit) {
    val primary = state.issues.firstOrNull()
    StatusHero(
        health = state.health,
        reason = primary?.reason() ?: stringResource(R.string.home_status_all_good),
        fixLabel = primary?.fixLabel(),
        onFix = primary?.let { { actions.onFixIssue(it) } },
        detail = if (primary is HomeIssue.OutgoingSmsLimit) {
            {
                Text(
                    text = stringResource(R.string.home_issue_sms_limit_fix),
                    style = MaterialTheme.typography.bodyMedium,
                    color = HomingTheme.colors.foreground
                )
                CodeBlock(
                    code = primary.adbCommand,
                    copyLabel = stringResource(R.string.home_command_label),
                    onShowCopiedConfirmation = onShowCopiedConfirmation
                )
            }
        } else {
            null
        }
    )
}

@Composable
private fun MoreIssues(issues: List<HomeIssue>, actions: HomeActions) {
    HomingPanel(contentPadding = PaddingValues(vertical = HomingTheme.spacing.xs)) {
        Text(
            text = stringResource(R.string.home_more_issues),
            style = MaterialTheme.typography.labelMedium,
            color = HomingTheme.colors.mutedForeground,
            modifier = Modifier.padding(horizontal = HomingTheme.spacing.gutter, vertical = HomingTheme.spacing.sm)
        )
        issues.forEachIndexed { index, issue ->
            if (index > 0) HomingDivider(inset = true)
            val label = issue.fixLabel()
            HomingListRow(
                headline = issue.reason(),
                headlineStyle = MaterialTheme.typography.bodyMedium,
                trailing = if (label != null) {
                    { HomingButton(text = label, onClick = { actions.onFixIssue(issue) }, style = HomingButtonStyle.Text) }
                } else {
                    null
                }
            )
        }
    }
}

@Composable
private fun TodayPanel(state: HomeUiState, actions: HomeActions) {
    val today = state.today
    HomingPanel(verticalArrangement = Arrangement.spacedBy(HomingTheme.spacing.lg)) {
        SectionHeader(
            title = stringResource(R.string.home_today_title),
            trailing = {
                HomingButton(
                    text = stringResource(R.string.home_open_messages),
                    onClick = actions.onOpenMessages,
                    style = HomingButtonStyle.Text
                )
            }
        )
        Row(Modifier.fillMaxWidth()) {
            StatTile(stringResource(R.string.home_stat_sent), today?.sent, Modifier.weight(1f), StatusTone.Brand)
            StatTile(stringResource(R.string.home_stat_delivered), today?.delivered, Modifier.weight(1f), StatusTone.Success)
        }
        Row(Modifier.fillMaxWidth()) {
            StatTile(stringResource(R.string.home_stat_failed), today?.failed, Modifier.weight(1f), StatusTone.Danger)
            StatTile(stringResource(R.string.home_stat_queued), today?.queued, Modifier.weight(1f), StatusTone.Info)
        }
        HomingDivider()
        Text(
            text = stringResource(R.string.home_chart_title),
            style = MaterialTheme.typography.labelMedium,
            color = HomingTheme.colors.mutedForeground
        )
        HourlyActivityChart(bars = state.hourly, modifier = Modifier.fillMaxWidth())
    }
}

@Composable
private fun DevicePanel(device: DeviceHealth?) {
    HomingPanel(contentPadding = PaddingValues(vertical = HomingTheme.spacing.xs)) {
        SectionHeader(
            title = stringResource(R.string.home_device_title),
            modifier = Modifier.padding(horizontal = HomingTheme.spacing.gutter)
        )
        if (device == null) {
            Text(
                text = stringResource(com.homingsms.app.core.designsystem.R.string.homing_value_unavailable),
                style = MaterialTheme.typography.bodyMedium,
                color = HomingTheme.colors.mutedForeground,
                modifier = Modifier.padding(HomingTheme.spacing.gutter)
            )
            return@HomingPanel
        }
        val battery = device.batteryPercent?.let { stringResource(R.string.home_battery_percent, formatCount(it)) }
            ?: stringResource(com.homingsms.app.core.designsystem.R.string.homing_value_unavailable)
        HomingListRow(
            headline = stringResource(R.string.home_battery),
            supporting = if (device.charging) stringResource(R.string.home_battery_charging, battery) else battery,
            leading = {
                Icon(
                    imageVector = if (device.charging) Icons.Outlined.BatteryChargingFull else Icons.Outlined.BatteryStd,
                    contentDescription = null,
                    tint = HomingTheme.colors.mutedForeground
                )
            }
        )
        HomingDivider(inset = true)
        HomingListRow(
            headline = stringResource(R.string.home_network),
            supporting = device.network.label(),
            leading = { Icon(Icons.Outlined.NetworkCheck, contentDescription = null, tint = HomingTheme.colors.mutedForeground) }
        )
        HomingDivider(inset = true)
        HomingListRow(
            headline = stringResource(R.string.home_last_cloud_contact),
            supporting = formatRelativeTime(device.lastCloudContactMillis),
            leading = { Icon(Icons.Outlined.Sync, contentDescription = null, tint = HomingTheme.colors.mutedForeground) }
        )
    }
}

@Composable
private fun NetworkKind.label(): String = when (this) {
    NetworkKind.Wifi -> stringResource(R.string.home_network_wifi)
    NetworkKind.Cellular -> stringResource(R.string.home_network_cellular)
    NetworkKind.Ethernet -> stringResource(R.string.home_network_ethernet)
    NetworkKind.None -> stringResource(R.string.home_network_none)
    NetworkKind.Unknown -> stringResource(R.string.home_network_unknown)
}

@Composable
private fun ConnectionsPanel(state: HomeUiState, actions: HomeActions) {
    val local = state.localServer
    val cloud = state.cloud
    val localDescription = when {
        !local.enabled -> stringResource(R.string.home_local_off)
        local.running && local.address != null -> stringResource(R.string.home_local_running, local.address)
        else -> stringResource(R.string.home_local_starting)
    }
    val cloudDescription = when {
        !cloud.paired -> stringResource(R.string.home_cloud_not_paired)
        !cloud.enabled -> stringResource(R.string.home_cloud_off)
        cloud.connected -> stringResource(R.string.home_cloud_connected, formatRelativeTime(cloud.lastContactMillis))
        else -> stringResource(R.string.home_cloud_disconnected)
    }
    HomingPanel(contentPadding = PaddingValues(vertical = HomingTheme.spacing.xs)) {
        SectionHeader(
            title = stringResource(R.string.home_connections_title),
            modifier = Modifier.padding(horizontal = HomingTheme.spacing.gutter)
        )
        HomingSwitchRow(
            title = stringResource(R.string.home_local_server),
            description = localDescription,
            checked = local.enabled,
            onCheckedChange = actions.onLocalServerToggle,
            leading = { Icon(Icons.Outlined.Dns, contentDescription = null, tint = HomingTheme.colors.mutedForeground) }
        )
        HomingDivider(inset = true)
        HomingSwitchRow(
            title = stringResource(R.string.home_cloud),
            description = cloudDescription,
            checked = cloud.enabled && cloud.paired,
            enabled = cloud.paired,
            onCheckedChange = actions.onCloudToggle,
            leading = { Icon(Icons.Outlined.Cloud, contentDescription = null, tint = HomingTheme.colors.mutedForeground) }
        )
    }
}
