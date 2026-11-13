package com.homingsms.core.data.cloud

import com.homingsms.core.data.db.CloudInboxOutboxEntity
import com.homingsms.core.data.db.CloudOutboxDao
import com.homingsms.core.data.db.CloudStateReportEntity
import com.homingsms.core.data.secrets.SecretStore
import com.homingsms.core.model.CloudConnectionState
import com.homingsms.core.model.CloudPairingState
import com.homingsms.core.model.ErrorCodes
import com.homingsms.core.model.PairingFailure
import com.homingsms.core.model.PairingLink
import com.homingsms.core.model.api.Iso8601
import java.io.IOException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class CloudSyncEngine(
    private val api: DeviceApi,
    private val outbox: CloudOutboxDao,
    private val config: CloudConfigStore,
    private val secrets: SecretStore,
    private val device: CloudDeviceInfo,
    private val sink: CloudMessageSink,
    private val inboxSource: InboxUploadSource,
    private val clock: () -> Long = System::currentTimeMillis
) {
    private val syncLock = Mutex()
    private val pairingState = MutableStateFlow(initialPairing())
    private val connectionState = MutableStateFlow(CloudConnectionState.Idle.copy(enabled = config.cloud.enabled))

    val pairing: StateFlow<CloudPairingState> = pairingState.asStateFlow()
    val connection: StateFlow<CloudConnectionState> = connectionState.asStateFlow()

    val isPaired: Boolean
        get() = config.cloud.deviceId != null && secrets.get(SecretStore.DEVICE_TOKEN) != null

    fun setPushAvailable(available: Boolean) {
        connectionState.update { it.copy(pushAvailable = available) }
    }

    fun setPendingCounts(states: Int, inbox: Int) {
        connectionState.update { it.copy(pendingStateReports = states, pendingInboxUploads = inbox) }
    }

    suspend fun pairWithLink(link: String, allowCleartext: Boolean): CloudPairingState {
        val parsed = PairingLink.parse(link, allowCleartext)
            ?: return CloudPairingState.Failed(PairingFailure.InvalidLink, "Not a valid Homing pairing link").also {
                pairingState.value = it
            }
        return pair(parsed.code, parsed.serverUrl, allowCleartext)
    }

    suspend fun pair(code: String, serverUrl: String, allowCleartext: Boolean = false): CloudPairingState {
        val server = PairingLink.normalizeServerUrl(serverUrl, allowCleartext)
        if (server == null || !PairingLink.isValidCode(code)) {
            return CloudPairingState.Failed(PairingFailure.InvalidLink, "Check the pairing code and server address")
                .also { pairingState.value = it }
        }
        pairingState.value = CloudPairingState.Pairing(server)
        val identity = device.identity()
        val fcmToken = config.cloud.fcmToken
        val request = PairRequest(
            code = code.trim().uppercase(),
            name = identity.name,
            model = identity.model.take(MAX_FIELD),
            manufacturer = identity.manufacturer.take(MAX_FIELD),
            osVersion = identity.osVersion.take(MAX_OS),
            appVersion = identity.appVersion.take(MAX_OS),
            sims = device.sims(),
            fcmToken = fcmToken
        )
        val result = try {
            api.pair(server, request)
        } catch (e: DeviceApiException) {
            val reason = when (e.status) {
                UNAUTHORIZED -> PairingFailure.InvalidCode
                TOO_MANY_REQUESTS -> PairingFailure.RateLimited
                else -> PairingFailure.Server
            }
            return CloudPairingState.Failed(reason, e.message).also { pairingState.value = it }
        } catch (e: IOException) {
            return CloudPairingState.Failed(PairingFailure.Network, e.message).also { pairingState.value = it }
        }
        secrets.put(SecretStore.DEVICE_TOKEN, result.deviceToken)
        outbox.clearStates()
        outbox.clearInbox()
        val now = clock()
        config.update {
            it.copy(
                enabled = true,
                serverUrl = server,
                deviceId = result.deviceId,
                accountId = result.accountId,
                pairedAt = now,
                reportedFcmToken = fcmToken,
                remote = result.settings.toModel()
            )
        }
        connectionState.value = CloudConnectionState.Idle.copy(
            enabled = true,
            lastSuccessAt = now,
            pushAvailable = connectionState.value.pushAvailable,
            remotePaused = result.settings.paused
        )
        return CloudPairingState.Paired(result.deviceId, result.accountId, server, now).also { pairingState.value = it }
    }

    suspend fun unpair(removedByServer: Boolean = false) {
        if (!removedByServer && isPaired) clearRemotePushToken()
        secrets.remove(SecretStore.DEVICE_TOKEN)
        outbox.clearStates()
        outbox.clearInbox()
        config.update {
            it.copy(enabled = false, deviceId = null, accountId = null, pairedAt = null, reportedFcmToken = null, remote = null)
        }
        connectionState.value = CloudConnectionState.Idle.copy(pushAvailable = connectionState.value.pushAvailable)
        pairingState.value = if (removedByServer) CloudPairingState.Removed(clock()) else CloudPairingState.Unpaired
    }

    suspend fun heartbeat(): SyncOutcome = authorized {
        val response = api.heartbeat(device.heartbeat())
        val now = clock()
        connectionState.update { it.copy(lastHeartbeatAt = now) }
        markSuccess()
        val remoteVersion = config.cloud.remote?.version ?: -1
        if (response.settingsVersion > remoteVersion) applySettings(api.settings())
        syncFcmToken()
        if (response.pendingMessages > 0) pullLocked()
        flushLocked()
        SyncOutcome.Success
    }

    suspend fun pull(): SyncOutcome = authorized {
        pullLocked()
        flushLocked()
        SyncOutcome.Success
    }

    suspend fun flush(): SyncOutcome = authorized {
        flushLocked()
        SyncOutcome.Success
    }

    suspend fun fetchSettings(): SyncOutcome = authorized {
        applySettings(api.settings())
        SyncOutcome.Success
    }

    suspend fun onFcmToken(token: String?): SyncOutcome {
        config.update { it.copy(fcmToken = token) }
        return authorized {
            syncFcmToken()
            SyncOutcome.Success
        }
    }

    suspend fun handleWake(data: Map<String, String>): SyncOutcome {
        if (data["kind"] != WakeReason.KIND) return SyncOutcome.Success
        val deviceId = config.cloud.deviceId ?: return SyncOutcome.NotPaired
        if (data["deviceId"] != null && data["deviceId"] != deviceId) return SyncOutcome.Success
        return when (data["reason"]) {
            WakeReason.MESSAGES_PENDING -> pull()
            WakeReason.SETTINGS_CHANGED -> fetchSettings()
            else -> heartbeat()
        }
    }

    suspend fun report(report: PendingReport) {
        if (!isPaired) return
        outbox.insertState(
            CloudStateReportEntity(
                messageId = report.messageId,
                state = report.state,
                at = report.at,
                simSlot = report.simSlot,
                partsCount = report.partsCount,
                errorCode = report.errorCode,
                errorMessage = report.errorMessage,
                attempts = 0,
                createdAt = clock()
            )
        )
    }

    suspend fun queueInbox(localId: String) {
        if (!isPaired) return
        outbox.insertInbox(CloudInboxOutboxEntity(localId = localId, createdAt = clock(), attempts = 0, lastError = null))
    }

    private suspend fun pullLocked() = syncLock.withLock {
        if (config.cloud.remote?.paused == true) return@withLock
        repeat(MAX_PULL_ROUNDS) {
            val messages = api.pull(PULL_LIMIT).messages
            messages.forEach { accept(it) }
            if (messages.size < PULL_LIMIT) return@withLock
        }
    }

    private suspend fun accept(message: DeviceMessageDto) {
        val now = clock()
        val validUntil = message.validUntil?.let(Iso8601::parse)
        if (validUntil != null && validUntil < now) {
            report(PendingReport(message.id, DeviceReportState.FAILED, now, errorCode = ErrorCodes.EXPIRED, errorMessage = "Expired"))
            return
        }
        when (val result = sink.accept(message)) {
            LeaseAcceptance.Enqueued -> report(PendingReport(message.id, DeviceReportState.QUEUED, now, simSlot = message.simSlot))

            is LeaseAcceptance.Known -> result.reports.forEach { report(it) }

            is LeaseAcceptance.Rejected -> report(
                PendingReport(message.id, DeviceReportState.FAILED, now, errorCode = result.errorCode, errorMessage = result.message)
            )
        }
    }

    private suspend fun flushLocked() = syncLock.withLock {
        flushStates()
        flushInbox()
    }

    private suspend fun flushStates() {
        while (true) {
            val batch = outbox.states(STATE_BATCH)
            if (batch.isEmpty()) return
            val response = try {
                api.reportStates(batch.map { it.toDto() })
            } catch (e: DeviceApiException) {
                outbox.incrementStateAttempts(batch.map { it.id })
                throw e
            } catch (e: IOException) {
                outbox.incrementStateAttempts(batch.map { it.id })
                throw e
            }
            response.results
                .filter { it.status == "rejected" && it.reason == "cancelled" }
                .forEach { sink.cancel(it.messageId) }
            outbox.deleteStates(batch.map { it.id })
            if (batch.size < STATE_BATCH) return
        }
    }

    private suspend fun flushInbox() {
        var batchSize = INBOX_BATCH
        var stripAttachments = false
        var batch = outbox.inbox(batchSize)
        while (batch.isNotEmpty()) {
            val sent = pushInboxBatch(batch, stripAttachments)
            if (sent) {
                if (batch.size < batchSize) return
                stripAttachments = false
            } else if (batch.size > 1) {
                batchSize = (batch.size / 2).coerceAtLeast(1)
            } else {
                stripAttachments = true
            }
            batch = outbox.inbox(batchSize)
        }
    }

    private suspend fun pushInboxBatch(batch: List<CloudInboxOutboxEntity>, stripAttachments: Boolean): Boolean {
        val ids = batch.map { it.localId }
        val uploads = inboxSource.load(ids)
        val missing = ids.filterNot(uploads::containsKey)
        if (missing.isNotEmpty()) outbox.deleteInbox(missing)
        val payload = ids.mapNotNull(uploads::get).map { if (stripAttachments) it.copy(attachments = null) else it }
        if (payload.isEmpty()) return true
        try {
            api.pushInbox(payload)
        } catch (e: DeviceApiException) {
            if (e.status == PAYLOAD_TOO_LARGE) return false
            outbox.markInboxFailed(ids, e.message)
            throw e
        } catch (e: IOException) {
            outbox.markInboxFailed(ids, e.message)
            throw e
        }
        outbox.deleteInbox(payload.map { it.localId })
        return true
    }

    private suspend fun clearRemotePushToken() {
        try {
            api.updateFcmToken(null)
        } catch (_: DeviceApiException) {
            return
        } catch (_: IOException) {
            return
        }
    }

    private suspend fun syncFcmToken() {
        val cloud = config.cloud
        if (cloud.fcmToken == cloud.reportedFcmToken) return
        api.updateFcmToken(cloud.fcmToken)
        config.update { it.copy(reportedFcmToken = cloud.fcmToken) }
    }

    private suspend fun applySettings(settings: DeviceSettingsDto) {
        val model = settings.toModel()
        config.update { it.copy(remote = model) }
        connectionState.update { it.copy(remotePaused = model.paused) }
    }

    private fun markSuccess() {
        connectionState.update {
            it.copy(enabled = true, lastSuccessAt = clock(), consecutiveFailures = 0, lastError = null)
        }
        if (pairingState.value !is CloudPairingState.Paired) pairingState.value = initialPairing()
    }

    private fun markFailure(message: String?) {
        connectionState.update {
            it.copy(lastFailureAt = clock(), consecutiveFailures = it.consecutiveFailures + 1, lastError = message)
        }
    }

    private suspend fun authorized(block: suspend () -> SyncOutcome): SyncOutcome {
        if (!isPaired) return SyncOutcome.NotPaired
        return try {
            block()
        } catch (_: DeviceUnauthorizedException) {
            unpair(removedByServer = true)
            SyncOutcome.Unpaired
        } catch (e: DeviceApiException) {
            markFailure(e.message)
            SyncOutcome.Retry(e.message ?: "HTTP ${e.status}")
        } catch (e: IOException) {
            markFailure(e.message ?: e.javaClass.simpleName)
            SyncOutcome.Retry(e.message ?: e.javaClass.simpleName)
        }
    }

    private fun initialPairing(): CloudPairingState {
        val cloud = config.cloud
        val deviceId = cloud.deviceId
        val token = secrets.get(SecretStore.DEVICE_TOKEN)
        return if (deviceId != null && token != null && cloud.serverUrl != null) {
            CloudPairingState.Paired(deviceId, cloud.accountId.orEmpty(), cloud.serverUrl.orEmpty(), cloud.pairedAt ?: 0)
        } else {
            CloudPairingState.Unpaired
        }
    }

    private fun CloudStateReportEntity.toDto(): StateReportDto = StateReportDto(
        messageId = messageId,
        state = state,
        at = Iso8601.format(at),
        simSlot = simSlot,
        partsCount = partsCount?.coerceIn(1, MAX_PARTS),
        error = errorCode?.let { StateReportErrorDto(it.take(MAX_ERROR_CODE), errorMessage?.take(MAX_ERROR_MESSAGE)) }
            ?: if (state ==
                DeviceReportState.FAILED
            ) {
                StateReportErrorDto(ErrorCodes.GENERIC_FAILURE, errorMessage?.take(MAX_ERROR_MESSAGE))
            } else {
                null
            }
    )

    companion object {
        const val PULL_LIMIT = 10
        const val MAX_PULL_ROUNDS = 5
        const val STATE_BATCH = 100
        const val INBOX_BATCH = 50
        private const val MAX_FIELD = 100
        private const val MAX_OS = 50
        private const val MAX_PARTS = 255
        private const val MAX_ERROR_CODE = 64
        private const val MAX_ERROR_MESSAGE = 500
        private const val UNAUTHORIZED = 401
        private const val TOO_MANY_REQUESTS = 429
        private const val PAYLOAD_TOO_LARGE = 413
    }
}
