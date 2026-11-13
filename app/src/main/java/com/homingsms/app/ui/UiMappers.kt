package com.homingsms.app.ui

import android.Manifest
import com.homingsms.app.core.designsystem.component.GatewayHealth
import com.homingsms.app.feature.connect.ApiScope
import com.homingsms.app.feature.connect.CloudPairing
import com.homingsms.app.feature.connect.PairingError
import com.homingsms.app.feature.home.HomeIssue
import com.homingsms.app.feature.home.MissingPermission
import com.homingsms.app.feature.home.NetworkKind
import com.homingsms.app.feature.messages.MessageState as UiMessageState
import com.homingsms.app.feature.messages.MessageType as UiMessageType
import com.homingsms.app.feature.webhooks.DeliveryStatus
import com.homingsms.app.feature.webhooks.WebhookEvent
import com.homingsms.core.model.CloudPairingState
import com.homingsms.core.model.GatewayState
import com.homingsms.core.model.GatewayStatus
import com.homingsms.core.model.MessageState
import com.homingsms.core.model.MessageType
import com.homingsms.core.model.NetworkType
import com.homingsms.core.model.PairingFailure
import com.homingsms.core.model.Scope
import com.homingsms.core.model.StatusReason
import com.homingsms.core.model.WebhookDeliveryStatus
import com.homingsms.core.model.WebhookEventType

object UiMappers {
    fun health(status: GatewayStatus): GatewayHealth = when (status) {
        GatewayStatus.Online -> GatewayHealth.Online
        GatewayStatus.Degraded -> GatewayHealth.Degraded
        GatewayStatus.Offline -> GatewayHealth.Offline
    }

    fun issues(gateway: GatewayState): List<HomeIssue> {
        val allSimsCapped = gateway.issues.any { it.blocking && it.reason is StatusReason.SimDailyLimitReached }
        return gateway.reasons.flatMap { reason -> issues(reason, allSimsCapped) }.distinct()
    }

    fun issues(reason: StatusReason, allSimsCapped: Boolean = false): List<HomeIssue> = when (reason) {
        is StatusReason.PermissionsMissing -> reason.permissions.mapNotNull(::missingPermission).distinct()
            .sortedBy { it.ordinal }
            .map { HomeIssue.PermissionMissing(it) }

        is StatusReason.SimDailyLimitReached -> listOf(
            if (allSimsCapped) HomeIssue.DeviceCapReached else HomeIssue.SimCapReached(reason.simSlot)
        )

        is StatusReason.OutgoingSmsLimit -> listOf(HomeIssue.OutgoingSmsLimit(StatusReason.OutgoingSmsLimit.SUGGESTED_MAX_COUNT))

        else -> listOfNotNull(deviceIssue(reason) ?: serviceIssue(reason))
    }

    private fun deviceIssue(reason: StatusReason): HomeIssue? = when (reason) {
        StatusReason.NoSim -> HomeIssue.NoSim
        StatusReason.AirplaneMode -> HomeIssue.AirplaneMode
        StatusReason.NoNetwork -> HomeIssue.NoNetwork
        is StatusReason.NoSignal -> HomeIssue.SimNoSignal(reason.simSlot)
        StatusReason.BatterySaver -> HomeIssue.BatterySaver
        is StatusReason.LowBattery -> HomeIssue.LowBattery
        else -> null
    }

    private fun serviceIssue(reason: StatusReason): HomeIssue? = when (reason) {
        StatusReason.Stopped -> HomeIssue.ServiceStopped
        StatusReason.NoModeEnabled -> HomeIssue.NotConfigured
        is StatusReason.CloudUnreachable -> HomeIssue.CloudUnreachable
        StatusReason.CloudPaused -> HomeIssue.Paused
        StatusReason.CloudNotPaired, StatusReason.CloudDeviceRemoved -> HomeIssue.CloudNotPaired
        is StatusReason.LocalServerDown -> HomeIssue.LocalServerDown
        else -> null
    }

    fun missingPermission(permission: String): MissingPermission? = when (permission) {
        Manifest.permission.SEND_SMS -> MissingPermission.SendSms
        Manifest.permission.RECEIVE_SMS, Manifest.permission.READ_SMS, Manifest.permission.RECEIVE_MMS -> MissingPermission.ReceiveSms
        Manifest.permission.READ_PHONE_STATE, READ_PHONE_NUMBERS -> MissingPermission.PhoneState
        POST_NOTIFICATIONS -> MissingPermission.Notifications
        else -> null
    }

    fun network(type: NetworkType): NetworkKind = when (type) {
        NetworkType.Wifi -> NetworkKind.Wifi
        NetworkType.Cellular -> NetworkKind.Cellular
        NetworkType.Ethernet -> NetworkKind.Ethernet
        NetworkType.None -> NetworkKind.None
        NetworkType.Unknown -> NetworkKind.Unknown
    }

    fun pairing(state: CloudPairingState, deviceName: String?): CloudPairing = when (state) {
        CloudPairingState.Unpaired, is CloudPairingState.Removed -> CloudPairing.NotPaired

        is CloudPairingState.Pairing -> CloudPairing.Pairing

        is CloudPairingState.Failed -> CloudPairing.Failed(pairingError(state.reason))

        is CloudPairingState.Paired -> CloudPairing.Paired(
            deviceName = deviceName?.takeIf { it.isNotBlank() } ?: state.deviceId,
            accountName = state.accountId.takeIf { it.isNotBlank() },
            pairedAtMillis = state.pairedAt,
            serverUrl = state.serverUrl
        )
    }

    fun pairingError(reason: PairingFailure): PairingError = when (reason) {
        PairingFailure.InvalidCode, PairingFailure.InvalidLink -> PairingError.InvalidCode
        PairingFailure.Network -> PairingError.Network
        PairingFailure.RateLimited, PairingFailure.Server -> PairingError.Unknown
    }

    fun scope(scope: Scope): ApiScope = ApiScope.entries.first { it.wireName == scope.value }

    fun scope(scope: ApiScope): Scope = Scope.entries.first { it.value == scope.wireName }

    fun event(type: WebhookEventType): WebhookEvent? = WebhookEvent.entries.firstOrNull { it.wireName == type.value }

    fun event(event: WebhookEvent): WebhookEventType = WebhookEventType.entries.first { it.value == event.wireName }

    fun delivery(status: WebhookDeliveryStatus): DeliveryStatus = when (status) {
        WebhookDeliveryStatus.Pending -> DeliveryStatus.Pending
        WebhookDeliveryStatus.Retrying -> DeliveryStatus.Retrying
        WebhookDeliveryStatus.Succeeded -> DeliveryStatus.Succeeded
        WebhookDeliveryStatus.Failed -> DeliveryStatus.Failed
    }

    fun messageState(state: MessageState): UiMessageState = UiMessageState.valueOf(state.name)

    fun messageState(state: UiMessageState): MessageState = MessageState.valueOf(state.name)

    fun messageType(type: MessageType): UiMessageType = UiMessageType.valueOf(type.name)

    fun messageType(type: UiMessageType): MessageType = MessageType.valueOf(type.name)

    private const val READ_PHONE_NUMBERS = "android.permission.READ_PHONE_NUMBERS"
    private const val POST_NOTIFICATIONS = "android.permission.POST_NOTIFICATIONS"
}
