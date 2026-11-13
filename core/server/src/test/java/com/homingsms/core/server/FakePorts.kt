package com.homingsms.core.server

import com.homingsms.core.model.ApiKeyPrincipal
import com.homingsms.core.model.Cursor
import com.homingsms.core.model.InboxAttachmentContent
import com.homingsms.core.model.InboxAttachmentInfo
import com.homingsms.core.model.InboxFilter
import com.homingsms.core.model.InboxRecord
import com.homingsms.core.model.MessageEventRecord
import com.homingsms.core.model.MessageFilter
import com.homingsms.core.model.MessagePriority
import com.homingsms.core.model.MessageRecord
import com.homingsms.core.model.MessageState
import com.homingsms.core.model.MessageType
import com.homingsms.core.model.Page
import com.homingsms.core.model.Scope
import com.homingsms.core.model.WebhookDelivery
import com.homingsms.core.model.WebhookDeliveryStatus
import com.homingsms.core.model.WebhookEndpoint
import com.homingsms.core.model.WebhookEventType
import com.homingsms.core.model.api.HealthDto
import com.homingsms.core.model.api.ValidatedSend

const val NOW = 1_794_478_530_000L
const val ADMIN_KEY = "hk_live_admin000000000000000000000000000"
const val SEND_KEY = "hk_live_send0000000000000000000000000000"
const val READ_KEY = "hk_live_read0000000000000000000000000000"

class FakeMessages : MessagesPort {
    val records = linkedMapOf<String, MessageRecord>()
    private val keys = mutableMapOf<String, Pair<String, String>>()
    var lastRequest: ValidatedSend? = null

    override suspend fun send(request: ValidatedSend, requestHash: String): SendResult {
        request.idempotencyKey?.let { key ->
            keys[key]?.let { (id, hash) ->
                return if (hash == requestHash) SendResult.Replayed(records.getValue(id)) else SendResult.Conflict("key reused")
            }
        }
        lastRequest = request
        val id = "msg_${records.size + 1}"
        val record = record(id, request.to, request.text, MessageState.Pending, NOW + records.size)
            .copy(idempotencyKey = request.idempotencyKey, metadata = request.metadata, priority = request.priority)
        records[id] = record
        request.idempotencyKey?.let { keys[it] = id to requestHash }
        return SendResult.Created(record)
    }

    override suspend fun list(filter: MessageFilter, cursor: Cursor?, limit: Int): Page<MessageRecord> {
        val sorted = records.values
            .filter { filter.state == null || it.state == filter.state }
            .filter { filter.to == null || it.to == filter.to }
            .sortedWith(compareByDescending<MessageRecord> { it.createdAt }.thenByDescending { it.id })
            .filter { cursor == null || it.createdAt < cursor.timestamp }
        val items = sorted.take(limit)
        return Page(items, if (sorted.size > limit) Cursor(items.last().createdAt, items.last().id).encode() else null)
    }

    override suspend fun get(id: String): MessageRecord? = records[id]

    override suspend fun cancel(id: String): CancelResult {
        val record = records[id] ?: return CancelResult.NotFound
        if (record.state != MessageState.Pending) return CancelResult.Conflict("Message in state ${record.state.value} cannot be cancelled")
        return CancelResult.Cancelled(record.copy(state = MessageState.Cancelled).also { records[id] = it })
    }

    companion object {
        fun record(id: String, to: String, text: String?, state: MessageState, createdAt: Long) = MessageRecord(
            id = id, type = MessageType.Sms, to = to, text = text, data = null, port = null, subject = null,
            attachments = emptyList(), state = state, priority = MessagePriority.Normal, simSlot = null, idempotencyKey = null,
            scheduleAt = null, validUntil = null, deliveryReport = true, metadata = emptyMap(), attempts = 0, partsCount = null,
            error = null, createdAt = createdAt, updatedAt = createdAt, sentAt = null, deliveredAt = null, failedAt = null,
            events = listOf(MessageEventRecord(MessageState.Pending, createdAt, null, null))
        )
    }
}

class FakeInbox : InboxPort {
    val records = listOf(
        InboxRecord(
            id = "inb_1", localId = "text:1", type = MessageType.Sms, from = "+15550002", to = null, simSlot = 0, text = "Hello",
            data = null, port = null, subject = null, attachments = emptyList(), receivedAt = NOW, createdAt = NOW
        ),
        InboxRecord(
            id = "inb_2", localId = "mms:2", type = MessageType.Mms, from = "+15550003", to = null, simSlot = 0, text = null,
            data = null, port = null, subject = "Pic", attachments = listOf(InboxAttachmentInfo("1", "image/png", "a.png", 3)),
            receivedAt = NOW - 1000, createdAt = NOW - 1000
        )
    )

    override suspend fun list(filter: InboxFilter, cursor: Cursor?, limit: Int): Page<InboxRecord> =
        Page(records.filter { filter.type == null || it.type == filter.type }.take(limit), null)

    override suspend fun get(id: String): InboxRecord? = records.firstOrNull { it.id == id }

    override suspend fun attachment(id: String, attachmentId: String): InboxAttachmentContent? =
        if (id == "inb_2" && attachmentId == "1") InboxAttachmentContent("image/png", "a.png", byteArrayOf(1, 2, 3)) else null
}

class FakeWebhooks : WebhooksPort {
    val endpoints = linkedMapOf<String, WebhookEndpoint>()
    val deliveries = linkedMapOf<String, WebhookDelivery>()
    private var counter = 0

    override suspend fun list(): List<WebhookEndpoint> = endpoints.values.toList()

    override suspend fun create(webhook: NewWebhook): CreatedWebhook {
        val id = "whk_${++counter}"
        val endpoint = WebhookEndpoint(id, webhook.url, webhook.description, webhook.events, webhook.enabled, NOW, NOW)
        endpoints[id] = endpoint
        return CreatedWebhook(endpoint, "whsec_secret$counter")
    }

    override suspend fun get(id: String): WebhookEndpoint? = endpoints[id]

    override suspend fun update(id: String, patch: WebhookPatch): WebhookEndpoint? {
        val current = endpoints[id] ?: return null
        val updated = current.copy(
            url = patch.url ?: current.url,
            events = patch.events ?: current.events,
            description = if (patch.descriptionSet) patch.description else current.description,
            enabled = patch.enabled ?: current.enabled
        )
        endpoints[id] = updated
        return updated
    }

    override suspend fun delete(id: String): Boolean = endpoints.remove(id) != null

    override suspend fun rotateSecret(id: String): String? = endpoints[id]?.let { "whsec_rotated" }

    override suspend fun test(id: String): WebhookDelivery? {
        endpoints[id] ?: return null
        val delivery = WebhookDelivery(
            "whd_${++counter}", id, "evt_$counter", WebhookEventType.WebhookTest, WebhookDeliveryStatus.Pending,
            0, null, null, null, NOW, null, NOW, NOW
        )
        deliveries[delivery.id] = delivery
        return delivery
    }

    override suspend fun deliveries(id: String, status: WebhookDeliveryStatus?, cursor: Cursor?, limit: Int): Page<WebhookDelivery>? {
        endpoints[id] ?: return null
        return Page(deliveries.values.filter { it.endpointId == id && (status == null || it.status == status) }.take(limit), null)
    }

    override suspend fun redeliver(id: String, deliveryId: String): WebhookDelivery? =
        deliveries[deliveryId]?.takeIf { it.endpointId == id }?.copy(attempts = 0)
}

fun testConfig(
    messages: FakeMessages = FakeMessages(),
    inbox: FakeInbox = FakeInbox(),
    webhooks: FakeWebhooks = FakeWebhooks(),
    health: HealthDto = HealthDto("ok", "1.0.0", "2026-11-12T10:15:30.000Z", mapOf("database" to "up"))
) = LocalApiConfig(
    messages = messages,
    inbox = inbox,
    webhooks = webhooks,
    authenticator = ApiKeyAuthenticator { key ->
        when (key) {
            ADMIN_KEY -> ApiKeyPrincipal("key_admin", setOf(Scope.Admin))
            SEND_KEY -> ApiKeyPrincipal("key_send", setOf(Scope.MessagesSend))
            READ_KEY -> ApiKeyPrincipal("key_read", setOf(Scope.MessagesRead, Scope.InboxRead))
            else -> null
        }
    },
    health = HealthPort { health },
    deviceIds = { listOf("dev_local", "dev_cloud") },
    clock = { NOW }
)
