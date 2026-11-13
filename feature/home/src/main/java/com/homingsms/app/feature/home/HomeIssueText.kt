package com.homingsms.app.feature.home

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.homingsms.app.core.designsystem.util.formatPercent
import com.homingsms.app.core.designsystem.util.simLabel

private const val LOW_BATTERY_FRACTION = 0.15f

private val plainReasons: Map<HomeIssue, Int> = mapOf(
    HomeIssue.NotConfigured to R.string.home_issue_not_configured,
    HomeIssue.ServiceStopped to R.string.home_issue_service_stopped,
    HomeIssue.BatteryOptimized to R.string.home_issue_battery_optimized,
    HomeIssue.DeviceCapReached to R.string.home_issue_device_cap,
    HomeIssue.AllSimsDisabled to R.string.home_issue_all_sims_disabled,
    HomeIssue.NoNetwork to R.string.home_issue_no_network,
    HomeIssue.Paused to R.string.home_issue_paused,
    HomeIssue.CloudUnreachable to R.string.home_issue_cloud_unreachable,
    HomeIssue.NoSim to R.string.home_issue_no_sim,
    HomeIssue.AirplaneMode to R.string.home_issue_airplane_mode,
    HomeIssue.BatterySaver to R.string.home_issue_battery_saver,
    HomeIssue.CloudNotPaired to R.string.home_issue_cloud_not_paired,
    HomeIssue.LocalServerDown to R.string.home_issue_local_server_down
)

private val plainFixes: Map<HomeIssue, Int> = mapOf(
    HomeIssue.NotConfigured to R.string.home_fix_set_up,
    HomeIssue.ServiceStopped to R.string.home_fix_restart,
    HomeIssue.BatteryOptimized to R.string.home_fix_battery,
    HomeIssue.DeviceCapReached to R.string.home_fix_change_cap,
    HomeIssue.AllSimsDisabled to R.string.home_fix_sim_settings,
    HomeIssue.NoNetwork to R.string.home_fix_network_settings,
    HomeIssue.CloudUnreachable to R.string.home_fix_retry,
    HomeIssue.AirplaneMode to R.string.home_fix_network_settings,
    HomeIssue.BatterySaver to R.string.home_fix_battery_saver,
    HomeIssue.CloudNotPaired to R.string.home_fix_set_up,
    HomeIssue.LocalServerDown to R.string.home_fix_restart
)

@Composable
internal fun HomeIssue.reason(): String = when (this) {
    is HomeIssue.OutgoingSmsLimit -> stringResource(R.string.home_issue_sms_limit)
    is HomeIssue.PermissionMissing -> permission.reason()
    is HomeIssue.SimNoSignal -> stringResource(R.string.home_issue_sim_no_signal, simLabel(slot))
    is HomeIssue.SimCapReached -> stringResource(R.string.home_issue_sim_cap, simLabel(slot))
    HomeIssue.LowBattery -> stringResource(R.string.home_issue_low_battery, formatPercent(LOW_BATTERY_FRACTION))
    else -> stringResource(plainReasons.getValue(this))
}

@Composable
internal fun HomeIssue.fixLabel(): String? = fixRes()?.let { stringResource(it) }

@StringRes
private fun HomeIssue.fixRes(): Int? = when (this) {
    is HomeIssue.PermissionMissing -> R.string.home_fix_permission
    is HomeIssue.SimNoSignal -> R.string.home_fix_network_settings
    is HomeIssue.SimCapReached -> R.string.home_fix_change_cap
    else -> plainFixes[this]
}

@Composable
private fun MissingPermission.reason(): String = stringResource(
    when (this) {
        MissingPermission.SendSms -> R.string.home_issue_permission_send
        MissingPermission.ReceiveSms -> R.string.home_issue_permission_receive
        MissingPermission.PhoneState -> R.string.home_issue_permission_phone
        MissingPermission.Notifications -> R.string.home_issue_permission_notifications
    }
)
