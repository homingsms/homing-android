package com.homingsms.app.ui.data

import com.homingsms.core.model.AppSettings
import com.homingsms.core.model.CloudConnectionState
import com.homingsms.core.model.CloudPairingState
import com.homingsms.core.model.CreatedApiKey
import com.homingsms.core.model.Cursor
import com.homingsms.core.model.DeviceHealth
import com.homingsms.core.model.GatewayState
import com.homingsms.core.model.HourlyBucket
import com.homingsms.core.model.InboxAttachmentContent
import com.homingsms.core.model.InboxFilter
import com.homingsms.core.model.InboxRecord
import com.homingsms.core.model.LocalServerInfo
import com.homingsms.core.model.MessageFilter
import com.homingsms.core.model.MessageRecord
import com.homingsms.core.model.Page
import com.homingsms.core.model.Scope
import com.homingsms.core.model.SimStats
import com.homingsms.core.model.TodayCounts
import com.homingsms.core.model.WebhookDelivery
import com.homingsms.core.model.WebhookEndpoint
import com.homingsms.core.model.WebhookEventType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

interface AppStatus {
    val settings: StateFlow<AppSettings>
    val serviceRunning: StateFlow<Boolean>
    val gateway: StateFlow<GatewayState>
    val health: StateFlow<DeviceHealth>
    val today: StateFlow<TodayCounts>
    val last24Hours: StateFlow<List<HourlyBucket>>
    val sims: StateFlow<List<SimStats>>
    val localServerInfo: StateFlow<LocalServerInfo>
    val cloudPairing: StateFlow<CloudPairingState>
    val cloudConnection: StateFlow<CloudConnectionState>
    val webhookEndpoints: StateFlow<List<WebhookEndpoint>>

    suspend fun refresh()
}

interface CloudPairingControl {
    val defaultCloudUrl: String
    val allowCleartext: Boolean

    suspend fun pair(code: String, serverUrl: String): CloudPairingState

    suspend fun pairWithLink(link: String): CloudPairingState

    suspend fun unpair()
}

interface GatewayControl : CloudPairingControl {
    fun start()

    fun stop()

    suspend fun setLocalEnabled(enabled: Boolean)

    suspend fun setLocalPort(port: Int)

    suspend fun setCloudEnabled(enabled: Boolean)

    suspend fun createApiKey(name: String, scopes: Set<Scope>): CreatedApiKey

    suspend fun revokeApiKey(id: String): Boolean

    fun retryCloud()

    suspend fun updateSettings(transform: (AppSettings) -> AppSettings)

    fun hasPassphrase(): Boolean

    fun setPassphrase(value: String?)

    suspend fun setAutostart(enabled: Boolean)
}

data class StoredMessage(val record: MessageRecord, val local: Boolean)

interface MessageStore {
    val changes: Flow<Unit>

    suspend fun list(filter: MessageFilter, cursor: Cursor?, limit: Int): Page<MessageRecord>

    suspend fun get(id: String): StoredMessage?

    suspend fun retry(id: String): String?

    suspend fun cancel(id: String): Boolean

    suspend fun sendTest(to: String, text: String): String
}

data class CloudUpload(val attempts: Int, val lastError: String?)

interface InboxStore {
    val changes: Flow<Unit>

    suspend fun list(filter: InboxFilter, cursor: Cursor?, limit: Int): Page<InboxRecord>

    suspend fun get(id: String): InboxRecord?

    suspend fun attachment(id: String, attachmentId: String): InboxAttachmentContent?

    suspend fun pendingCloudUpload(localId: String): CloudUpload?
}

interface WebhookStore {
    val endpoints: Flow<List<WebhookEndpoint>>

    fun recentDeliveries(endpointId: String, limit: Int): Flow<List<WebhookDelivery>>

    suspend fun get(id: String): WebhookEndpoint?

    suspend fun create(url: String, events: Set<WebhookEventType>, enabled: Boolean): WebhookEndpoint

    suspend fun update(id: String, url: String? = null, events: Set<WebhookEventType>? = null, enabled: Boolean? = null): WebhookEndpoint?

    suspend fun delete(id: String): Boolean

    suspend fun secret(id: String): String

    suspend fun rotateSecret(id: String): String?

    suspend fun deliveries(endpointId: String, cursor: Cursor?, limit: Int): Page<WebhookDelivery>?

    suspend fun sendTest(id: String): WebhookDelivery?

    suspend fun redeliver(endpointId: String, deliveryId: String): WebhookDelivery?
}

data class SharedFile(val uri: String, val mimeType: String)

interface FileSharing {
    suspend fun exportLogs(): SharedFile?

    suspend fun attachmentFile(name: String?, contentType: String, bytes: ByteArray): SharedFile
}
