package com.homingsms.core.data.webhooks

import androidx.sqlite.db.SimpleSQLiteQuery
import com.homingsms.core.data.db.WebhookDeliveriesDao
import com.homingsms.core.data.db.WebhookDeliveryEntity
import com.homingsms.core.data.db.WebhookEndpointEntity
import com.homingsms.core.data.db.WebhookEndpointsDao
import com.homingsms.core.data.secrets.SecretStore
import com.homingsms.core.model.Cursor
import com.homingsms.core.model.Ids
import com.homingsms.core.model.Page
import com.homingsms.core.model.WebhookDelivery
import com.homingsms.core.model.WebhookDeliveryStatus
import com.homingsms.core.model.WebhookEndpoint
import com.homingsms.core.model.WebhookEventType
import com.homingsms.core.model.api.ApiJson
import com.homingsms.core.model.api.Iso8601
import com.homingsms.core.model.api.TestEventData
import com.homingsms.core.model.api.WebhookEnvelope
import com.homingsms.core.model.api.WebhookSecretFormat
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.JsonElement

class WebhookRepository(
    private val endpoints: WebhookEndpointsDao,
    private val deliveries: WebhookDeliveriesDao,
    private val secrets: SecretStore,
    private val accountId: () -> String,
    private val clock: () -> Long = System::currentTimeMillis
) {
    val endpointsFlow: Flow<List<WebhookEndpoint>> = endpoints.observeAll().map { list -> list.map { it.toModel() } }

    fun recentDeliveries(endpointId: String, limit: Int = RECENT_LIMIT): Flow<List<WebhookDelivery>> =
        deliveries.observeRecent(endpointId, limit).map { list -> list.map { it.toModel() } }

    suspend fun list(): List<WebhookEndpoint> = endpoints.all().map { it.toModel() }

    suspend fun get(id: String): WebhookEndpoint? = endpoints.get(id)?.toModel()

    suspend fun create(url: String, events: Set<WebhookEventType>, description: String?, enabled: Boolean): Pair<WebhookEndpoint, String> {
        val now = clock()
        val entity = WebhookEndpointEntity(
            id = Ids.webhook(),
            url = url,
            description = description,
            events = encodeEvents(events),
            enabled = enabled,
            createdAt = now,
            updatedAt = now
        )
        val secret = newSecret()
        secrets.put(SecretStore.webhookSecret(entity.id), secret)
        endpoints.insert(entity)
        return entity.toModel() to secret
    }

    suspend fun update(
        id: String,
        url: String? = null,
        events: Set<WebhookEventType>? = null,
        description: String? = null,
        descriptionSet: Boolean = false,
        enabled: Boolean? = null
    ): WebhookEndpoint? {
        val current = endpoints.get(id) ?: return null
        val updated = current.copy(
            url = url ?: current.url,
            events = events?.let(::encodeEvents) ?: current.events,
            description = if (descriptionSet) description else current.description,
            enabled = enabled ?: current.enabled,
            updatedAt = clock()
        )
        endpoints.update(updated)
        return updated.toModel()
    }

    suspend fun delete(id: String): Boolean {
        val deleted = endpoints.delete(id) > 0
        if (deleted) secrets.remove(SecretStore.webhookSecret(id))
        return deleted
    }

    suspend fun rotateSecret(id: String): String? {
        endpoints.get(id) ?: return null
        return newSecret().also { secrets.put(SecretStore.webhookSecret(id), it) }
    }

    fun secretFor(id: String): String = secrets.get(SecretStore.webhookSecret(id))
        ?: newSecret().also { secrets.put(SecretStore.webhookSecret(id), it) }

    fun secretPreview(id: String): String? = secrets.get(SecretStore.webhookSecret(id))?.let(WebhookSecretFormat::preview)

    suspend fun emit(type: WebhookEventType, data: JsonElement): Int {
        val targets = endpoints.all().filter { it.enabled && type in decodeEvents(it.events) }
        if (targets.isEmpty()) return 0
        val envelope = envelope(type, data)
        deliveries.insert(targets.map { newDelivery(it.id, envelope) })
        return targets.size
    }

    suspend fun test(id: String): WebhookDelivery? {
        val endpoint = endpoints.get(id) ?: return null
        val data = ApiJson.default.encodeToJsonElement(
            TestEventData.serializer(),
            TestEventData(endpoint.id, "This is a test event from Homing.")
        )
        val delivery = newDelivery(endpoint.id, envelope(WebhookEventType.WebhookTest, data))
        deliveries.insert(listOf(delivery))
        return delivery.toModel()
    }

    suspend fun redeliver(endpointId: String, deliveryId: String): WebhookDelivery? {
        val original = deliveries.get(deliveryId)?.takeIf { it.endpointId == endpointId } ?: return null
        val now = clock()
        val reset = original.copy(
            status = WebhookDeliveryStatus.Pending.value,
            attempts = 0,
            nextAttemptAt = now,
            deliveredAt = null,
            updatedAt = now
        )
        deliveries.update(reset)
        return reset.toModel()
    }

    suspend fun deliveries(endpointId: String, status: WebhookDeliveryStatus?, cursor: Cursor?, limit: Int): Page<WebhookDelivery>? {
        endpoints.get(endpointId) ?: return null
        val conditions = mutableListOf("endpointId = ?")
        val args = mutableListOf<Any>(endpointId)
        status?.let {
            conditions += "status = ?"
            args += it.value
        }
        cursor?.let {
            conditions += "(createdAt < ? OR (createdAt = ? AND id < ?))"
            args += it.timestamp
            args += it.timestamp
            args += it.id
        }
        val sql = "SELECT * FROM webhook_deliveries WHERE ${conditions.joinToString(" AND ")} ORDER BY createdAt DESC, id DESC LIMIT ?"
        args += limit + 1
        val rows = deliveries.query(SimpleSQLiteQuery(sql, args.toTypedArray()))
        val items = rows.take(limit)
        val next = if (rows.size > limit) items.last().let { Cursor(it.createdAt, it.id).encode() } else null
        return Page(items.map { it.toModel() }, next)
    }

    private fun envelope(type: WebhookEventType, data: JsonElement): String = ApiJson.default.encodeToString(
        WebhookEnvelope.serializer(),
        WebhookEnvelope(Ids.event(), type, Iso8601.format(clock()), accountId(), data)
    )

    private fun newDelivery(endpointId: String, payload: String): WebhookDeliveryEntity {
        val now = clock()
        val envelope = ApiJson.default.decodeFromString(WebhookEnvelope.serializer(), payload)
        return WebhookDeliveryEntity(
            id = Ids.delivery(),
            endpointId = endpointId,
            eventId = envelope.id,
            eventType = envelope.type.value,
            payload = payload,
            status = WebhookDeliveryStatus.Pending.value,
            attempts = 0,
            lastStatusCode = null,
            lastResponseBody = null,
            lastError = null,
            nextAttemptAt = now,
            deliveredAt = null,
            createdAt = now,
            updatedAt = now
        )
    }

    private fun newSecret(): String = WebhookSecretFormat.PREFIX + Ids.random(WebhookSecretFormat.SECRET_LENGTH)

    companion object {
        const val RECENT_LIMIT = 50

        fun encodeEvents(events: Set<WebhookEventType>): String = events.sortedBy { it.ordinal }.joinToString(",") { it.value }

        fun decodeEvents(value: String): Set<WebhookEventType> =
            value.split(',').mapNotNull { WebhookEventType.fromValue(it.trim()) }.toSet()
    }
}

fun WebhookEndpointEntity.toModel(): WebhookEndpoint = WebhookEndpoint(
    id = id,
    url = url,
    description = description,
    events = WebhookRepository.decodeEvents(events),
    enabled = enabled,
    createdAt = createdAt,
    updatedAt = updatedAt
)

fun WebhookDeliveryEntity.toModel(): WebhookDelivery = WebhookDelivery(
    id = id,
    endpointId = endpointId,
    eventId = eventId,
    eventType = WebhookEventType.fromValue(eventType) ?: WebhookEventType.WebhookTest,
    status = WebhookDeliveryStatus.fromValue(status) ?: WebhookDeliveryStatus.Pending,
    attempts = attempts,
    lastStatusCode = lastStatusCode,
    lastResponseBody = lastResponseBody,
    lastError = lastError,
    nextAttemptAt = nextAttemptAt,
    deliveredAt = deliveredAt,
    createdAt = createdAt,
    updatedAt = updatedAt
)
