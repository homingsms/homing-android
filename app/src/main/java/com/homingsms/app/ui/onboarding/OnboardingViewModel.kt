package com.homingsms.app.ui.onboarding

import android.os.Build
import androidx.lifecycle.viewModelScope
import com.homingsms.app.feature.onboarding.BatteryUi
import com.homingsms.app.feature.onboarding.GatewayMode
import com.homingsms.app.feature.onboarding.OnboardingActions
import com.homingsms.app.feature.onboarding.OnboardingFlow
import com.homingsms.app.feature.onboarding.OnboardingPermission
import com.homingsms.app.feature.onboarding.OnboardingStep
import com.homingsms.app.feature.onboarding.OnboardingUiState
import com.homingsms.app.feature.onboarding.PermissionItem
import com.homingsms.app.feature.onboarding.PermissionStatus
import com.homingsms.app.feature.onboarding.TestMessageStatus
import com.homingsms.app.feature.onboarding.TestMessageUi
import com.homingsms.app.ui.EffectViewModel
import com.homingsms.app.ui.UiMappers
import com.homingsms.app.ui.attempt
import com.homingsms.app.ui.connect.PairLinkInbox
import com.homingsms.app.ui.connect.PairingCoordinator
import com.homingsms.app.ui.connect.PendingPairLink
import com.homingsms.app.ui.data.AppStatus
import com.homingsms.app.ui.data.GatewayControl
import com.homingsms.app.ui.data.InboxStore
import com.homingsms.app.ui.data.MessageStore
import com.homingsms.app.ui.navigation.HomeRoute
import com.homingsms.app.ui.system.AppPermissions
import com.homingsms.app.ui.system.PermissionOutcome
import com.homingsms.app.ui.system.PlatformState
import com.homingsms.app.ui.system.SystemAction
import com.homingsms.core.model.CloudPairingState
import com.homingsms.core.model.InboxFilter
import com.homingsms.core.model.MessageRecord
import com.homingsms.core.model.MessageState
import kotlin.random.Random
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class OnboardingViewModel(
    private val status: AppStatus,
    private val control: GatewayControl,
    private val platform: PlatformState,
    private val messages: MessageStore,
    private val inbox: InboxStore,
    links: PairLinkInbox,
    private val sdkInt: Int = Build.VERSION.SDK_INT,
    private val clock: () -> Long = System::currentTimeMillis
) : EffectViewModel() {
    private data class Progress(
        val step: OnboardingStep = OnboardingStep.Welcome,
        val mode: GatewayMode? = null,
        val denied: Set<OnboardingPermission> = emptySet(),
        val blocked: Set<OnboardingPermission> = emptySet(),
        val batteryIgnored: Boolean = false,
        val test: TestMessageUi = TestMessageUi(),
        val revision: Int = 0
    )

    private val needsRestrictedSettings = sdkInt >= Build.VERSION_CODES.TIRAMISU &&
        platform.isSideloaded() &&
        !permissionGranted(OnboardingPermission.SendSms)

    private val progress = MutableStateFlow(
        Progress(
            mode = initialMode(),
            batteryIgnored = platform.isIgnoringBatteryOptimizations(),
            test = TestMessageUi(phoneNumber = platform.ownPhoneNumber().orEmpty())
        )
    )
    private val pairing = PairingCoordinator(viewModelScope, control, onResult = {})
    private var testJob: Job? = null

    val pendingLink: StateFlow<PendingPairLink?> = pairing.pendingLink

    val state: StateFlow<OnboardingUiState> = combine(
        progress,
        status.cloudPairing,
        status.settings,
        pairing.pairingCode
    ) { current, cloud, settings, code ->
        OnboardingUiState(
            step = current.step,
            needsRestrictedSettings = needsRestrictedSettings,
            permissions = OnboardingPermission.entries.map { PermissionItem(it, permissionStatus(it, current)) },
            battery = BatteryUi(current.batteryIgnored, platform.deviceMaker),
            mode = current.mode,
            cloud = UiMappers.pairing(cloud, settings.cloud.deviceName),
            pairingCode = code,
            test = current.test
        )
    }.stateIn(viewModelScope, SharingStarted.Eagerly, OnboardingUiState(needsRestrictedSettings = needsRestrictedSettings))

    val actions = OnboardingActions(
        onNext = ::next,
        onBack = ::previous,
        onFinish = ::finish,
        onRequestPermission = { permission ->
            val requested = AppPermissions.forOnboarding(permission, sdkInt)
            if (requested.isEmpty()) refresh() else system(SystemAction.RequestPermissions(requested))
        },
        onSkipPermission = { permission -> progress.update { it.copy(denied = it.denied + permission) } },
        onOpenAppSettings = { system(SystemAction.OpenAppSettings) },
        onRequestBatteryExemption = { system(SystemAction.RequestBatteryExemption) },
        onOpenOemSettings = { system(SystemAction.OpenOemSettings(platform.deviceMaker)) },
        onOpenAppInfo = { system(SystemAction.OpenAppSettings) },
        onModeSelected = { mode -> progress.update { it.copy(mode = mode) } },
        pairing = pairing.actions,
        onTestNumberChange = { number -> progress.update { it.copy(test = it.test.copy(phoneNumber = number)) } },
        onSendTest = ::sendTest
    )

    init {
        links.links.filterNotNull()
            .filter { !status.settings.value.general.onboardingCompleted }
            .onEach { links.consume()?.let(::offerLink) }
            .launchIn(viewModelScope)
    }

    fun onResume() = refresh()

    fun onPermissionsResult(outcomes: List<PermissionOutcome>) {
        val refused = outcomes.filterNot { it.granted }
        progress.update { current ->
            val owners = refused.mapNotNull { AppPermissions.ownerOf(it.permission) }.toSet()
            val blocked = refused.filterNot { it.canAskAgain }.mapNotNull { AppPermissions.ownerOf(it.permission) }.toSet()
            current.copy(denied = current.denied + owners, blocked = current.blocked + blocked)
        }
        refresh()
    }

    fun confirmPairLink() = pairing.confirmLink()

    fun dismissPairLink() = pairing.dismissLink()

    private fun refresh() {
        progress.update { current ->
            val number = current.test.phoneNumber.ifBlank { platform.ownPhoneNumber().orEmpty() }
            current.copy(
                batteryIgnored = platform.isIgnoringBatteryOptimizations(),
                test = current.test.copy(phoneNumber = number),
                revision = current.revision + 1
            )
        }
        viewModelScope.launch { attempt { status.refresh() } }
    }

    private fun offerLink(link: String) {
        if (!pairing.offerLink(link)) return
        progress.update { current ->
            val mode = when (current.mode) {
                null, GatewayMode.Cloud -> GatewayMode.Cloud
                GatewayMode.Local, GatewayMode.Both -> GatewayMode.Both
            }
            current.copy(mode = mode, step = OnboardingStep.CloudPairing)
        }
    }

    private fun next() {
        val current = state.value
        if (!current.canContinue) return
        val nextStep = OnboardingFlow.next(current) ?: return finish()
        if (current.step == OnboardingStep.Mode) current.mode?.let(::applyMode)
        progress.update { it.copy(step = nextStep) }
    }

    private fun previous() {
        val step = OnboardingFlow.previous(state.value) ?: return
        progress.update { it.copy(step = step) }
    }

    private fun applyMode(mode: GatewayMode) {
        viewModelScope.launch {
            attempt {
                control.setLocalEnabled(mode != GatewayMode.Cloud)
                control.setCloudEnabled(mode.usesCloud)
            }
        }
    }

    private fun finish() {
        viewModelScope.launch {
            state.value.mode?.let { mode ->
                attempt {
                    control.setLocalEnabled(mode != GatewayMode.Cloud)
                    control.setCloudEnabled(mode.usesCloud)
                }
            }
            attempt {
                control.setAutostart(true)
                control.updateSettings { it.copy(general = it.general.copy(onboardingCompleted = true)) }
            }
            if (!status.serviceRunning.value) control.start()
            resetTo(HomeRoute)
        }
    }

    private fun sendTest() {
        val number = progress.value.test.phoneNumber.trim()
        if (number.isEmpty() || testJob?.isActive == true) return
        val text = "Homing test " + Random.nextInt(TOKEN_MIN, TOKEN_MAX)
        setTest(TestMessageStatus.Sending)
        testJob = viewModelScope.launch {
            if (!status.serviceRunning.value) control.start()
            val id = attempt { messages.sendTest(number, text) }.getOrElse {
                setTest(TestMessageStatus.Failed(it.message))
                return@launch
            }
            watchTest(id, text)
        }
    }

    private suspend fun watchTest(id: String, text: String) {
        val deadline = clock() + TEST_TIMEOUT_MS
        var progressSoFar = TestProgress()
        while (clock() < deadline) {
            val record = attempt { messages.get(id) }.getOrNull()?.record
            if (record != null && record.state in failedStates) {
                setTest(TestMessageStatus.Failed(record.error?.message))
                return
            }
            progressSoFar = progressSoFar.advance(record, clock())
            val received = attempt { inbox.list(InboxFilter(search = text), null, 1) }.getOrNull()?.items
                ?.firstOrNull { it.text == text }
            setTest(progressSoFar.status(received?.receivedAt, text))
            if (received != null) return
            delay(POLL_MS)
        }
        if (progressSoFar.sentAt == null) setTest(TestMessageStatus.Failed(null))
    }

    private data class TestProgress(val sentAt: Long? = null, val deliveredAt: Long? = null) {
        fun advance(record: MessageRecord?, now: Long): TestProgress = when (record?.state) {
            MessageState.Sent -> copy(sentAt = sentAt ?: record.sentAt ?: now)
            MessageState.Delivered -> TestProgress(sentAt ?: record.sentAt ?: now, deliveredAt ?: record.deliveredAt ?: now)
            else -> this
        }

        fun status(receivedAt: Long?, text: String): TestMessageStatus = when {
            receivedAt != null -> TestMessageStatus.Received(sentAt ?: receivedAt, deliveredAt, receivedAt, text)
            deliveredAt != null -> TestMessageStatus.Delivered(sentAt ?: deliveredAt, deliveredAt)
            sentAt != null -> TestMessageStatus.Sent(sentAt)
            else -> TestMessageStatus.Sending
        }
    }

    private fun setTest(status: TestMessageStatus) = progress.update { it.copy(test = it.test.copy(status = status)) }

    private fun initialMode(): GatewayMode? {
        val settings = status.settings.value
        if (!settings.general.onboardingCompleted) return null
        val cloud = settings.cloud.enabled || status.cloudPairing.value is CloudPairingState.Paired
        return when {
            settings.localServer.enabled && cloud -> GatewayMode.Both
            cloud -> GatewayMode.Cloud
            settings.localServer.enabled -> GatewayMode.Local
            else -> null
        }
    }

    private fun permissionGranted(permission: OnboardingPermission): Boolean =
        AppPermissions.forOnboarding(permission, sdkInt).all(platform::isGranted)

    private fun permissionStatus(permission: OnboardingPermission, current: Progress): PermissionStatus = when {
        permissionGranted(permission) -> PermissionStatus.Granted
        permission in current.blocked -> PermissionStatus.PermanentlyDenied
        permission in current.denied -> PermissionStatus.Denied
        else -> PermissionStatus.NotRequested
    }

    private companion object {
        const val TEST_TIMEOUT_MS = 180_000L
        const val POLL_MS = 2_000L
        const val TOKEN_MIN = 1000
        const val TOKEN_MAX = 9999
        val failedStates = setOf(MessageState.Failed, MessageState.Expired, MessageState.Cancelled)
    }
}
