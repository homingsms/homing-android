package com.homingsms.app.ui

import com.homingsms.app.feature.about.OpenSourceComponent
import com.homingsms.app.feature.onboarding.DeviceMaker
import com.homingsms.app.ui.about.LicenseSource
import com.homingsms.app.ui.data.AppStatus
import com.homingsms.app.ui.data.CloudUpload
import com.homingsms.app.ui.data.FileSharing
import com.homingsms.app.ui.data.GatewayControl
import com.homingsms.app.ui.data.InboxStore
import com.homingsms.app.ui.data.MessageStore
import com.homingsms.app.ui.data.SharedFile
import com.homingsms.app.ui.data.StoredMessage
import com.homingsms.app.ui.data.WebhookStore
import com.homingsms.app.ui.system.PlatformState
import com.homingsms.core.model.ApiKeyInfo
import com.homingsms.core.model.AppSettings
import com.homingsms.core.model.CloudConnectionState
import com.homingsms.core.model.CloudPairingState
import com.homingsms.core.model.CreatedApiKey
import com.homingsms.core.model.Cursor
import com.homingsms.core.model.DeviceHealth
import com.homingsms.core.model.GatewayState
import com.homingsms.core.model.HourlyBucket
import com.homingsms.core.model.InboxAttachmentContent
import com.homingsms.core.model.InboxAttachmentInfo
import com.homingsms.core.model.InboxFilter
import com.homingsms.core.model.InboxRecord
import com.homingsms.core.model.LocalServerInfo
import com.homingsms.core.model.MessageError
import com.homingsms.core.model.MessageEventRecord
import com.homingsms.core.model.MessageFilter
import com.homingsms.core.model.MessagePriority
import com.homingsms.core.model.MessageRecord
import com.homingsms.core.model.MessageState
import com.homingsms.core.model.MessageType
import com.homingsms.core.model.Page
import com.homingsms.core.model.Scope
import com.homingsms.core.model.SimStats
import com.homingsms.core.model.SmsPartsUsage
import com.homingsms.core.model.TodayCounts
import com.homingsms.core.model.WebhookDelivery
import com.homingsms.core.model.WebhookDeliveryStatus
import com.homingsms.core.model.WebhookEndpoint
import com.homingsms.core.model.WebhookEventType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update

class FakeAppStatus(initial: AppSettings = AppSettings()) : AppStatus {
    override val settings = MutableStateFlow(initial)
    override val serviceRunning = MutableStateFlow(true)
    override val gateway = MutableStateFlow(GatewayState.Stopped)
    override val health = MutableStateFlow(DeviceHealth.Unknown)
    override val today = MutableStateFlow(TodayCounts.Empty)
    override val last24Hours = MutableStateFlow<List<HourlyBucket>>(emptyList())
    override val sims = MutableStateFlow<List<SimStats>>(emptyList())
    override val smsParts = MutableStateFlow<SmsPartsUsage?>(null)
    override val localServerInfo = MutableStateFlow(LocalServerInfo.Stopped)
    override val cloudPairing = MutableStateFlow<CloudPairingState>(CloudPairingState.Unpaired)
    override val cloudConnection = MutableStateFlow(CloudConnectionState.Idle)
    override val webhookEndpoints = MutableStateFlow<List<WebhookEndpoint>>(emptyList())
    var refreshes = 0

    override suspend fun refresh() {
        refreshes++
    }
}

class FakeGatewayControl(private val status: FakeAppStatus) : GatewayControl {
    override val defaultCloudUrl: String = "https://api.example.test"
    override val allowCleartext: Boolean = false
    val calls = mutableListOf<String>()
    var pairResult: CloudPairingState = CloudPairingState.Paired("dev_1", "acc_1", "https://api.example.test", 1L)
    var storedPassphrase: String? = null
    var createdKeys = 0

    override fun start() {
        calls += "start"
        status.serviceRunning.value = true
    }

    override fun stop() {
        calls += "stop"
        status.serviceRunning.value = false
    }

    override suspend fun setLocalEnabled(enabled: Boolean) {
        calls += "local:$enabled"
        status.settings.update { it.copy(localServer = it.localServer.copy(enabled = enabled)) }
    }

    override suspend fun setLocalPort(port: Int) {
        calls += "port:$port"
        status.settings.update { it.copy(localServer = it.localServer.copy(port = port)) }
    }

    override suspend fun setCloudEnabled(enabled: Boolean) {
        calls += "cloud:$enabled"
        status.settings.update { it.copy(cloud = it.cloud.copy(enabled = enabled)) }
    }

    override suspend fun createApiKey(name: String, scopes: Set<Scope>): CreatedApiKey {
        createdKeys++
        calls += "createKey:$name:${scopes.joinToString(",") { it.value }}"
        val info = ApiKeyInfo("key_$createdKeys", name, "hk_live_ab", scopes, 10L, null)
        status.localServerInfo.update { it.copy(keys = it.keys + info) }
        return CreatedApiKey(info, "hk_live_secret$createdKeys")
    }

    override suspend fun revokeApiKey(id: String): Boolean {
        calls += "revoke:$id"
        status.localServerInfo.update { info -> info.copy(keys = info.keys.filterNot { it.id == id }) }
        return true
    }

    override suspend fun pair(code: String, serverUrl: String): CloudPairingState {
        calls += "pair:$code:$serverUrl"
        status.cloudPairing.value = pairResult
        return pairResult
    }

    override suspend fun pairWithLink(link: String): CloudPairingState {
        calls += "pairLink:$link"
        status.cloudPairing.value = pairResult
        return pairResult
    }

    override suspend fun unpair() {
        calls += "unpair"
        status.cloudPairing.value = CloudPairingState.Unpaired
    }

    override fun retryCloud() {
        calls += "retryCloud"
    }

    override suspend fun updateSettings(transform: (AppSettings) -> AppSettings) {
        status.settings.update(transform)
    }

    override fun hasPassphrase(): Boolean = !storedPassphrase.isNullOrEmpty()

    override fun setPassphrase(value: String?) {
        calls += "passphrase:${value != null}"
        storedPassphrase = value
    }

    override suspend fun setAutostart(enabled: Boolean) {
        calls += "autostart:$enabled"
        status.settings.update { it.copy(general = it.general.copy(autostart = enabled)) }
    }
}

class FakeMessageStore : MessageStore {
    val records = MutableStateFlow<List<StoredMessage>>(emptyList())
    override val changes = MutableSharedFlow<Unit>(extraBufferCapacity = 8)
    val filters = mutableListOf<MessageFilter>()
    var failList = false
    val retried = mutableListOf<String>()
    val cancelled = mutableListOf<String>()
    val sent = mutableListOf<Pair<String, String>>()

    override suspend fun list(filter: MessageFilter, cursor: Cursor?, limit: Int): Page<MessageRecord> {
        filters += filter
        check(!failList) { "boom" }
        return paged(records.value.map { it.record }, cursor, limit) { it.createdAt to it.id }
    }

    override suspend fun get(id: String): StoredMessage? = records.value.firstOrNull { it.record.id == id }

    override suspend fun retry(id: String): String? {
        retried += id
        return "msg_retry"
    }

    override suspend fun cancel(id: String): Boolean {
        cancelled += id
        return true
    }

    override suspend fun sendTest(to: String, text: String): String {
        sent += to to text
        records.update { it + StoredMessage(message("msg_test", to = to, text = text, state = MessageState.Pending), local = true) }
        return "msg_test"
    }

    fun setState(id: String, transform: (MessageRecord) -> MessageRecord) {
        records.update { list -> list.map { if (it.record.id == id) it.copy(record = transform(it.record)) else it } }
    }
}

class FakeInboxStore : InboxStore {
    val records = MutableStateFlow<List<InboxRecord>>(emptyList())
    override val changes = MutableSharedFlow<Unit>(extraBufferCapacity = 8)
    val filters = mutableListOf<InboxFilter>()
    val attachments = mutableMapOf<String, InboxAttachmentContent>()
    var pending: CloudUpload? = null

    override suspend fun list(filter: InboxFilter, cursor: Cursor?, limit: Int): Page<InboxRecord> {
        filters += filter
        val matching = records.value.filter { record -> filter.search == null || record.text?.contains(filter.search!!) == true }
        return paged(matching, cursor, limit) { it.receivedAt to it.id }
    }

    override suspend fun get(id: String): InboxRecord? = records.value.firstOrNull { it.id == id }

    override suspend fun attachment(id: String, attachmentId: String): InboxAttachmentContent? = attachments[attachmentId]

    override suspend fun pendingCloudUpload(localId: String): CloudUpload? = pending
}

class FakeWebhookStore : WebhookStore {
    val endpointList = MutableStateFlow<List<WebhookEndpoint>>(emptyList())
    val deliveryList = MutableStateFlow<List<WebhookDelivery>>(emptyList())
    override val endpoints: Flow<List<WebhookEndpoint>> = endpointList
    val secrets = mutableMapOf<String, String>()
    val calls = mutableListOf<String>()

    override fun recentDeliveries(endpointId: String, limit: Int): Flow<List<WebhookDelivery>> =
        deliveryList.map { list -> list.filter { it.endpointId == endpointId }.sortedByDescending { it.createdAt }.take(limit) }

    override suspend fun get(id: String): WebhookEndpoint? = endpointList.value.firstOrNull { it.id == id }

    override suspend fun create(url: String, events: Set<WebhookEventType>, enabled: Boolean): WebhookEndpoint {
        calls += "create:$url"
        val endpoint = endpoint("whk_new", url, events, enabled)
        endpointList.update { it + endpoint }
        return endpoint
    }

    override suspend fun update(id: String, url: String?, events: Set<WebhookEventType>?, enabled: Boolean?): WebhookEndpoint? {
        calls += "update:$id:$url:$enabled"
        var result: WebhookEndpoint? = null
        endpointList.update { list ->
            list.map {
                if (it.id != id) {
                    it
                } else {
                    it.copy(url = url ?: it.url, events = events ?: it.events, enabled = enabled ?: it.enabled).also { updated ->
                        result =
                            updated
                    }
                }
            }
        }
        return result
    }

    override suspend fun delete(id: String): Boolean {
        calls += "delete:$id"
        endpointList.update { list -> list.filterNot { it.id == id } }
        return true
    }

    override suspend fun secret(id: String): String = secrets.getOrPut(id) { "whsec_first" }

    override suspend fun rotateSecret(id: String): String = "whsec_rotated".also { secrets[id] = it }

    override suspend fun deliveries(endpointId: String, cursor: Cursor?, limit: Int): Page<WebhookDelivery> =
        paged(deliveryList.value.filter { it.endpointId == endpointId }, cursor, limit) { it.createdAt to it.id }

    override suspend fun sendTest(id: String): WebhookDelivery {
        calls += "test:$id"
        val delivery = delivery("whd_test", id, WebhookDeliveryStatus.Pending, createdAt = 500L, type = WebhookEventType.WebhookTest)
        deliveryList.update { it + delivery }
        return delivery
    }

    override suspend fun redeliver(endpointId: String, deliveryId: String): WebhookDelivery? {
        calls += "redeliver:$deliveryId"
        return deliveryList.value.firstOrNull { it.id == deliveryId }
    }
}

class FakeFileSharing : FileSharing {
    var logs: SharedFile? = SharedFile("content://logs", "text/plain")
    val written = mutableListOf<String?>()

    override suspend fun exportLogs(): SharedFile? = logs

    override suspend fun attachmentFile(name: String?, contentType: String, bytes: ByteArray): SharedFile {
        written += name
        return SharedFile("content://shared/$name", contentType)
    }
}

class FakePlatformState(
    var granted: MutableSet<String> = mutableSetOf(),
    var batteryIgnored: Boolean = false,
    var sideloaded: Boolean = false,
    var phoneNumber: String? = "+15550100",
    override val deviceMaker: DeviceMaker = DeviceMaker.Samsung
) : PlatformState {
    override fun isGranted(permission: String): Boolean = permission in granted

    override fun isIgnoringBatteryOptimizations(): Boolean = batteryIgnored

    override fun isSideloaded(): Boolean = sideloaded

    override fun ownPhoneNumber(): String? = phoneNumber
}

class FakeLicenseSource(private val components: List<OpenSourceComponent>) : LicenseSource {
    override suspend fun libraries(): List<OpenSourceComponent> = components
}

fun <T> paged(items: List<T>, cursor: Cursor?, limit: Int, key: (T) -> Pair<Long, String>): Page<T> {
    val sorted = items.sortedWith(compareByDescending<T> { key(it).first }.thenByDescending { key(it).second })
    val start = cursor?.let { c -> sorted.indexOfFirst { key(it) == c.timestamp to c.id } + 1 } ?: 0
    val page = sorted.drop(start).take(limit)
    val next = if (start + limit < sorted.size) page.last().let { Cursor(key(it).first, key(it).second).encode() } else null
    return Page(page, next)
}

fun message(
    id: String,
    to: String = "+15550101",
    text: String? = "Hello",
    state: MessageState = MessageState.Sent,
    type: MessageType = MessageType.Sms,
    createdAt: Long = 100L,
    simSlot: Int? = 0,
    error: MessageError? = null
) = MessageRecord(
    id = id,
    type = type,
    to = to,
    text = text,
    data = null,
    port = null,
    subject = null,
    attachments = emptyList(),
    state = state,
    priority = MessagePriority.Normal,
    simSlot = simSlot,
    idempotencyKey = null,
    scheduleAt = null,
    validUntil = null,
    deliveryReport = true,
    metadata = emptyMap(),
    attempts = 1,
    partsCount = 1,
    error = error,
    createdAt = createdAt,
    updatedAt = createdAt + 1,
    sentAt = null,
    deliveredAt = null,
    failedAt = null,
    events = listOf(
        MessageEventRecord(MessageState.Pending, createdAt, simSlot, null),
        MessageEventRecord(state, createdAt + 1, simSlot, error)
    )
)

fun inbox(
    id: String,
    text: String? = "Hi",
    type: MessageType = MessageType.Sms,
    receivedAt: Long = 100L,
    attachments: List<InboxAttachmentInfo> = emptyList()
) = InboxRecord(
    id = id,
    localId = "local_$id",
    type = type,
    from = "+15550199",
    to = null,
    simSlot = 0,
    text = text,
    data = null,
    port = null,
    subject = null,
    attachments = attachments,
    receivedAt = receivedAt,
    createdAt = receivedAt
)

fun endpoint(
    id: String,
    url: String = "https://hooks.example.test/a",
    events: Set<WebhookEventType> = setOf(WebhookEventType.MessageSent),
    enabled: Boolean = true
) = WebhookEndpoint(id, url, null, events, enabled, 1L, 1L)

fun delivery(
    id: String,
    endpointId: String,
    status: WebhookDeliveryStatus,
    createdAt: Long = 100L,
    code: Int? = null,
    type: WebhookEventType = WebhookEventType.MessageSent
) = WebhookDelivery(id, endpointId, "evt_$id", type, status, 1, code, null, null, null, null, createdAt, createdAt, "{\"id\":\"evt_$id\"}")
