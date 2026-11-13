package com.homingsms.core.data.cloud

import com.homingsms.core.model.CloudSettings

interface CloudConfigStore {
    val cloud: CloudSettings

    suspend fun update(transform: (CloudSettings) -> CloudSettings)
}

data class DeviceIdentity(val name: String?, val model: String, val manufacturer: String, val osVersion: String, val appVersion: String)

interface CloudDeviceInfo {
    fun identity(): DeviceIdentity

    fun sims(): List<SimInfoDto>

    suspend fun heartbeat(): HeartbeatRequest
}

data class PendingReport(
    val messageId: String,
    val state: String,
    val at: Long,
    val simSlot: Int? = null,
    val partsCount: Int? = null,
    val errorCode: String? = null,
    val errorMessage: String? = null
)

sealed interface LeaseAcceptance {
    data object Enqueued : LeaseAcceptance

    data class Known(val reports: List<PendingReport>) : LeaseAcceptance

    data class Rejected(val errorCode: String, val message: String) : LeaseAcceptance
}

interface CloudMessageSink {
    suspend fun accept(message: DeviceMessageDto): LeaseAcceptance

    suspend fun cancel(messageId: String)
}

fun interface InboxUploadSource {
    suspend fun load(localIds: List<String>): Map<String, InboxUploadDto>
}

sealed interface SyncOutcome {
    data object Success : SyncOutcome

    data object NotPaired : SyncOutcome

    data object Unpaired : SyncOutcome

    data class Retry(val message: String) : SyncOutcome
}
