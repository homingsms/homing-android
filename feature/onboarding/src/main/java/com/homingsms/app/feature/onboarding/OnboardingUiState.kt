package com.homingsms.app.feature.onboarding

import androidx.compose.runtime.Immutable
import com.homingsms.app.feature.connect.CloudPairing
import com.homingsms.app.feature.connect.PairingActions

enum class OnboardingStep { Welcome, Permissions, Battery, RestrictedSettings, Mode, CloudPairing, TestMessage }

enum class LegalLink { Terms, Privacy, AcceptableUse }

enum class GatewayMode(val usesCloud: Boolean) { Local(false), Cloud(true), Both(true) }

enum class OnboardingPermission(val required: Boolean) {
    SendSms(true),
    ReceiveSms(true),
    PhoneState(true),
    Notifications(false)
}

enum class PermissionStatus { NotRequested, Granted, Denied, PermanentlyDenied }

@Immutable
data class PermissionItem(val permission: OnboardingPermission, val status: PermissionStatus = PermissionStatus.NotRequested)

enum class DeviceMaker { Xiaomi, Samsung, Huawei, OppoRealme, TecnoInfinix, Other }

@Immutable
data class BatteryUi(val ignoringOptimizations: Boolean = false, val maker: DeviceMaker = DeviceMaker.Other)

sealed interface TestMessageStatus {
    data object Idle : TestMessageStatus
    data object Sending : TestMessageStatus
    data class Sent(val sentAtMillis: Long) : TestMessageStatus
    data class Delivered(val sentAtMillis: Long, val deliveredAtMillis: Long) : TestMessageStatus
    data class Received(val sentAtMillis: Long, val deliveredAtMillis: Long?, val receivedAtMillis: Long, val text: String) :
        TestMessageStatus
    data class Failed(val reason: String?) : TestMessageStatus
}

@Immutable
data class TestMessageUi(val phoneNumber: String = "", val status: TestMessageStatus = TestMessageStatus.Idle) {
    val arrived: Boolean get() = status is TestMessageStatus.Received
}

@Immutable
data class OnboardingUiState(
    val step: OnboardingStep = OnboardingStep.Welcome,
    val needsRestrictedSettings: Boolean = false,
    val permissions: List<PermissionItem> = OnboardingPermission.entries.map { PermissionItem(it) },
    val battery: BatteryUi = BatteryUi(),
    val mode: GatewayMode? = null,
    val cloud: CloudPairing = CloudPairing.NotPaired,
    val pairingCode: String = "",
    val test: TestMessageUi = TestMessageUi(),
    val termsAccepted: Boolean = false
) {
    val steps: List<OnboardingStep> get() = OnboardingFlow.steps(mode, needsRestrictedSettings)
    val stepIndex: Int get() = steps.indexOf(step).coerceAtLeast(0)
    val pendingPermission: PermissionItem? get() = permissions.firstOrNull { it.status != PermissionStatus.Granted }
    val requiredPermissionsGranted: Boolean
        get() = permissions.filter { it.permission.required }.all { it.status == PermissionStatus.Granted }
    val canContinue: Boolean
        get() = when (step) {
            OnboardingStep.Welcome -> termsAccepted
            OnboardingStep.Battery, OnboardingStep.RestrictedSettings -> true
            OnboardingStep.Permissions -> requiredPermissionsGranted
            OnboardingStep.Mode -> mode != null
            OnboardingStep.CloudPairing -> cloud is CloudPairing.Paired
            OnboardingStep.TestMessage -> true
        }
}

object OnboardingFlow {
    fun steps(mode: GatewayMode?, needsRestrictedSettings: Boolean): List<OnboardingStep> = buildList {
        add(OnboardingStep.Welcome)
        if (needsRestrictedSettings) add(OnboardingStep.RestrictedSettings)
        add(OnboardingStep.Permissions)
        add(OnboardingStep.Battery)
        add(OnboardingStep.Mode)
        if (mode?.usesCloud == true) add(OnboardingStep.CloudPairing)
        add(OnboardingStep.TestMessage)
    }

    fun next(state: OnboardingUiState): OnboardingStep? = state.steps.getOrNull(state.stepIndex + 1)

    fun previous(state: OnboardingUiState): OnboardingStep? = state.steps.getOrNull(state.stepIndex - 1)
}

@Immutable
data class OnboardingActions(
    val onNext: () -> Unit = {},
    val onBack: () -> Unit = {},
    val onFinish: () -> Unit = {},
    val onRequestPermission: (OnboardingPermission) -> Unit = {},
    val onSkipPermission: (OnboardingPermission) -> Unit = {},
    val onOpenAppSettings: () -> Unit = {},
    val onRequestBatteryExemption: () -> Unit = {},
    val onOpenOemSettings: () -> Unit = {},
    val onOpenAppInfo: () -> Unit = {},
    val onModeSelected: (GatewayMode) -> Unit = {},
    val pairing: PairingActions = PairingActions(),
    val onTestNumberChange: (String) -> Unit = {},
    val onSendTest: () -> Unit = {},
    val onTermsAcceptedChange: (Boolean) -> Unit = {},
    val onOpenLegal: (LegalLink) -> Unit = {}
)

@Immutable
data class UpdatedTermsUiState(val accepted: Boolean = false)

@Immutable
data class UpdatedTermsActions(
    val onAcceptedChange: (Boolean) -> Unit = {},
    val onOpenLegal: (LegalLink) -> Unit = {},
    val onAccept: () -> Unit = {}
)
