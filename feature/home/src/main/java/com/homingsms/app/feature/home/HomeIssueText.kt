package com.homingsms.app.feature.home

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.homingsms.app.core.designsystem.util.formatPercent
import com.homingsms.app.core.designsystem.util.simLabel

private const val LOW_BATTERY_FRACTION = 0.15f

@Composable
internal fun HomeIssue.reason(): String = when (this) {
    HomeIssue.NotConfigured -> stringResource(R.string.home_issue_not_configured)
    HomeIssue.ServiceStopped -> stringResource(R.string.home_issue_service_stopped)
    HomeIssue.BatteryOptimized -> stringResource(R.string.home_issue_battery_optimized)
    is HomeIssue.OutgoingSmsLimit -> stringResource(R.string.home_issue_sms_limit)
    is HomeIssue.PermissionMissing -> permission.reason()
    is HomeIssue.SimNoSignal -> stringResource(R.string.home_issue_sim_no_signal, simLabel(slot))
    is HomeIssue.SimCapReached -> stringResource(R.string.home_issue_sim_cap, simLabel(slot))
    HomeIssue.DeviceCapReached -> stringResource(R.string.home_issue_device_cap)
    HomeIssue.AllSimsDisabled -> stringResource(R.string.home_issue_all_sims_disabled)
    HomeIssue.NoNetwork -> stringResource(R.string.home_issue_no_network)
    HomeIssue.LowBattery -> stringResource(R.string.home_issue_low_battery, formatPercent(LOW_BATTERY_FRACTION))
    HomeIssue.Paused -> stringResource(R.string.home_issue_paused)
    HomeIssue.CloudUnreachable -> stringResource(R.string.home_issue_cloud_unreachable)
}

@Composable
internal fun HomeIssue.fixLabel(): String? = when (this) {
    HomeIssue.NotConfigured -> stringResource(R.string.home_fix_set_up)
    HomeIssue.ServiceStopped -> stringResource(R.string.home_fix_restart)
    HomeIssue.BatteryOptimized -> stringResource(R.string.home_fix_battery)
    is HomeIssue.OutgoingSmsLimit -> null
    is HomeIssue.PermissionMissing -> stringResource(R.string.home_fix_permission)
    is HomeIssue.SimNoSignal -> stringResource(R.string.home_fix_network_settings)
    is HomeIssue.SimCapReached, HomeIssue.DeviceCapReached -> stringResource(R.string.home_fix_change_cap)
    HomeIssue.AllSimsDisabled -> stringResource(R.string.home_fix_sim_settings)
    HomeIssue.NoNetwork -> stringResource(R.string.home_fix_network_settings)
    HomeIssue.LowBattery -> null
    HomeIssue.Paused -> null
    HomeIssue.CloudUnreachable -> stringResource(R.string.home_fix_retry)
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
