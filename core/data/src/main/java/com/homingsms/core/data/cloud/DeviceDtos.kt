package com.homingsms.core.data.cloud

import com.homingsms.core.model.NetworkType
import com.homingsms.core.model.RemoteDeviceSettings
import com.homingsms.core.model.RemoteSimSettings
import kotlinx.serialization.Serializable

@Serializable
data class SimInfoDto(val slot: Int, val carrier: String? = null, val phoneNumber: String? = null, val iccid: String? = null)

@Serializable
data class PairRequest(
    val code: String,
    val name: String? = null,
    val model: String? = null,
    val manufacturer: String? = null,
    val osVersion: String? = null,
    val appVersion: String,
    val sims: List<SimInfoDto>,
    val fcmToken: String? = null
)

@Serializable
data class SimSettingsDto(val slot: Int, val enabled: Boolean, val dailyLimit: Int? = null)

@Serializable
data class DeviceSettingsDto(
    val version: Int,
    val paused: Boolean,
    val heartbeatIntervalSeconds: Int,
    val pullIntervalSeconds: Int,
    val leaseSeconds: Int,
    val dailyLimit: Int? = null,
    val sims: List<SimSettingsDto> = emptyList(),
    val optOutKeywords: List<String> = emptyList()
) {
    fun toModel(): RemoteDeviceSettings = RemoteDeviceSettings(
        version = version,
        paused = paused,
        heartbeatIntervalSeconds = heartbeatIntervalSeconds,
        pullIntervalSeconds = pullIntervalSeconds,
        leaseSeconds = leaseSeconds,
        dailyLimit = dailyLimit,
        sims = sims.map { RemoteSimSettings(it.slot, it.enabled, it.dailyLimit) },
        optOutKeywords = optOutKeywords.map { it.uppercase() }
    )
}

@Serializable
data class PairResponse(val deviceId: String, val deviceToken: String, val accountId: String, val settings: DeviceSettingsDto)

@Serializable
data class BatteryDto(val level: Int, val charging: Boolean)

@Serializable
data class NetworkDto(val type: NetworkType)

@Serializable
data class SimHealthDto(
    val slot: Int,
    val carrier: String? = null,
    val phoneNumber: String? = null,
    val signalLevel: Int? = null,
    val sentToday: Int
)

@Serializable
data class HeartbeatRequest(
    val appVersion: String,
    val battery: BatteryDto,
    val network: NetworkDto,
    val sims: List<SimHealthDto>,
    val localQueueSize: Int? = null
)

@Serializable
data class HeartbeatResponse(val serverTime: String, val pendingMessages: Int, val settingsVersion: Int)

@Serializable
data class PullRequest(val limit: Int)

@Serializable
data class DeviceAttachmentDto(val contentType: String, val name: String? = null, val data: String)

@Serializable
data class DeviceMessageDto(
    val id: String,
    val type: String,
    val to: String,
    val text: String? = null,
    val data: String? = null,
    val port: Int? = null,
    val subject: String? = null,
    val attachments: List<DeviceAttachmentDto> = emptyList(),
    val simSlot: Int? = null,
    val priority: String = "normal",
    val deliveryReport: Boolean = true,
    val validUntil: String? = null,
    val attempt: Int = 1,
    val leaseExpiresAt: String
)

@Serializable
data class PullResponse(val messages: List<DeviceMessageDto>)

@Serializable
data class StateReportErrorDto(val code: String, val message: String? = null)

@Serializable
data class StateReportDto(
    val messageId: String,
    val state: String,
    val at: String,
    val simSlot: Int? = null,
    val partsCount: Int? = null,
    val error: StateReportErrorDto? = null
)

@Serializable
data class StateReportRequest(val reports: List<StateReportDto>)

@Serializable
data class StateReportResultDto(val messageId: String, val status: String, val reason: String? = null)

@Serializable
data class StateReportResponse(val results: List<StateReportResultDto>)

@Serializable
data class InboxAttachmentUploadDto(val contentType: String, val name: String? = null, val data: String)

@Serializable
data class InboxUploadDto(
    val localId: String,
    val type: String,
    val from: String,
    val to: String? = null,
    val simSlot: Int? = null,
    val receivedAt: String,
    val text: String? = null,
    val data: String? = null,
    val port: Int? = null,
    val subject: String? = null,
    val attachments: List<InboxAttachmentUploadDto>? = null
)

@Serializable
data class InboxPushRequest(val messages: List<InboxUploadDto>)

@Serializable
data class InboxPushResultDto(val localId: String, val id: String, val duplicate: Boolean)

@Serializable
data class InboxPushResponse(val results: List<InboxPushResultDto>)

object DeviceReportState {
    const val QUEUED = "queued"
    const val SENDING = "sending"
    const val SENT = "sent"
    const val DELIVERED = "delivered"
    const val FAILED = "failed"
}

object WakeReason {
    const val KIND = "homing.wake"
    const val MESSAGES_PENDING = "messages_pending"
    const val SETTINGS_CHANGED = "settings_changed"
    const val DEVICE_REMOVED = "device_removed"
}
