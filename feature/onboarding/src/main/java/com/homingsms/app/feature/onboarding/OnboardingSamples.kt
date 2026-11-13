package com.homingsms.app.feature.onboarding

import com.homingsms.app.core.designsystem.preview.PREVIEW_NOW_MILLIS
import com.homingsms.app.feature.connect.CloudPairing

internal object OnboardingSamples {
    val welcome = OnboardingUiState()

    val permissions = OnboardingUiState(
        step = OnboardingStep.Permissions,
        permissions = listOf(
            PermissionItem(OnboardingPermission.SendSms, PermissionStatus.Granted),
            PermissionItem(OnboardingPermission.ReceiveSms, PermissionStatus.Denied),
            PermissionItem(OnboardingPermission.PhoneState),
            PermissionItem(OnboardingPermission.Notifications)
        )
    )

    private val granted = OnboardingPermission.entries.map { PermissionItem(it, PermissionStatus.Granted) }

    val battery = OnboardingUiState(
        step = OnboardingStep.Battery,
        permissions = granted,
        battery = BatteryUi(ignoringOptimizations = false, maker = DeviceMaker.Xiaomi)
    )

    val restricted = OnboardingUiState(step = OnboardingStep.RestrictedSettings, needsRestrictedSettings = true)

    val mode = OnboardingUiState(step = OnboardingStep.Mode, permissions = granted, mode = GatewayMode.Both)

    val pairing = OnboardingUiState(
        step = OnboardingStep.CloudPairing,
        permissions = granted,
        mode = GatewayMode.Cloud,
        cloud = CloudPairing.NotPaired
    )

    val testArrived = OnboardingUiState(
        step = OnboardingStep.TestMessage,
        permissions = granted,
        mode = GatewayMode.Local,
        test = TestMessageUi(
            phoneNumber = "+44 7700 900123",
            status = TestMessageStatus.Received(
                sentAtMillis = PREVIEW_NOW_MILLIS - 9_000,
                deliveredAtMillis = PREVIEW_NOW_MILLIS - 5_000,
                receivedAtMillis = PREVIEW_NOW_MILLIS - 4_000,
                text = "Homing test message"
            )
        )
    )
}
