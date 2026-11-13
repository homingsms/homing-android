package com.homingsms.app.feature.onboarding

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.homingsms.app.core.designsystem.theme.StatusTone

@Composable
internal fun OnboardingPermission.title(): String = stringResource(
    when (this) {
        OnboardingPermission.SendSms -> R.string.onboarding_perm_send_title
        OnboardingPermission.ReceiveSms -> R.string.onboarding_perm_receive_title
        OnboardingPermission.PhoneState -> R.string.onboarding_perm_phone_title
        OnboardingPermission.Notifications -> R.string.onboarding_perm_notifications_title
    }
)

@Composable
internal fun OnboardingPermission.why(): String = stringResource(
    when (this) {
        OnboardingPermission.SendSms -> R.string.onboarding_perm_send_why
        OnboardingPermission.ReceiveSms -> R.string.onboarding_perm_receive_why
        OnboardingPermission.PhoneState -> R.string.onboarding_perm_phone_why
        OnboardingPermission.Notifications -> R.string.onboarding_perm_notifications_why
    }
)

@Composable
internal fun OnboardingPermission.without(): String = stringResource(
    when (this) {
        OnboardingPermission.SendSms -> R.string.onboarding_perm_send_without
        OnboardingPermission.ReceiveSms -> R.string.onboarding_perm_receive_without
        OnboardingPermission.PhoneState -> R.string.onboarding_perm_phone_without
        OnboardingPermission.Notifications -> R.string.onboarding_perm_notifications_without
    }
)

@Composable
internal fun PermissionStatus.label(): String = stringResource(
    when (this) {
        PermissionStatus.NotRequested -> R.string.onboarding_status_not_requested
        PermissionStatus.Granted -> R.string.onboarding_status_granted
        PermissionStatus.Denied -> R.string.onboarding_status_denied
        PermissionStatus.PermanentlyDenied -> R.string.onboarding_status_blocked
    }
)

internal fun PermissionStatus.tone(): StatusTone = when (this) {
    PermissionStatus.NotRequested -> StatusTone.Neutral
    PermissionStatus.Granted -> StatusTone.Success
    PermissionStatus.Denied -> StatusTone.Warning
    PermissionStatus.PermanentlyDenied -> StatusTone.Danger
}

@Composable
internal fun DeviceMaker.title(): String = stringResource(
    when (this) {
        DeviceMaker.Xiaomi -> R.string.onboarding_oem_xiaomi
        DeviceMaker.Samsung -> R.string.onboarding_oem_samsung
        DeviceMaker.Huawei -> R.string.onboarding_oem_huawei
        DeviceMaker.OppoRealme -> R.string.onboarding_oem_oppo
        DeviceMaker.TecnoInfinix -> R.string.onboarding_oem_tecno
        DeviceMaker.Other -> R.string.onboarding_oem_other
    }
)

internal fun DeviceMaker.steps(): List<Int> = when (this) {
    DeviceMaker.Xiaomi -> listOf(
        R.string.onboarding_oem_xiaomi_1,
        R.string.onboarding_oem_xiaomi_2,
        R.string.onboarding_oem_xiaomi_3,
        R.string.onboarding_oem_lock_recents
    )

    DeviceMaker.Samsung -> listOf(
        R.string.onboarding_oem_samsung_1,
        R.string.onboarding_oem_samsung_2,
        R.string.onboarding_oem_samsung_3
    )

    DeviceMaker.Huawei -> listOf(
        R.string.onboarding_oem_huawei_1,
        R.string.onboarding_oem_huawei_2,
        R.string.onboarding_oem_huawei_3
    )

    DeviceMaker.OppoRealme -> listOf(
        R.string.onboarding_oem_oppo_1,
        R.string.onboarding_oem_oppo_2,
        R.string.onboarding_oem_lock_recents
    )

    DeviceMaker.TecnoInfinix -> listOf(
        R.string.onboarding_oem_tecno_1,
        R.string.onboarding_oem_tecno_2,
        R.string.onboarding_oem_lock_recents
    )

    DeviceMaker.Other -> listOf(
        R.string.onboarding_oem_other_1,
        R.string.onboarding_oem_other_2
    )
}

@Composable
internal fun GatewayMode.title(): String = stringResource(
    when (this) {
        GatewayMode.Local -> R.string.onboarding_mode_local
        GatewayMode.Cloud -> R.string.onboarding_mode_cloud
        GatewayMode.Both -> R.string.onboarding_mode_both
    }
)

@Composable
internal fun GatewayMode.description(): String = stringResource(
    when (this) {
        GatewayMode.Local -> R.string.onboarding_mode_local_body
        GatewayMode.Cloud -> R.string.onboarding_mode_cloud_body
        GatewayMode.Both -> R.string.onboarding_mode_both_body
    }
)
