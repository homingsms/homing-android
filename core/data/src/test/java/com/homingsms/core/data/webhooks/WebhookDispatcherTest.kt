package com.homingsms.core.data.webhooks

import androidx.sqlite.db.SupportSQLiteQuery
import com.homingsms.core.data.FakeSecrets
import com.homingsms.core.data.RoutingDispatcher
import com.homingsms.core.data.db.WebhookDeliveriesDao
import com.homingsms.core.data.db.WebhookDeliveryEntity
import com.homingsms.core.data.db.WebhookEndpointEntity
import com.homingsms.core.data.db.WebhookEndpointsDao
import com.homingsms.core.model.WebhookDeliveryStatus
import com.homingsms.core.model.WebhookEventType
import com.homingsms.core.model.api.ApiJson
import com.homingsms.core.model.api.WebhookEnvelope
import com.homingsms.core.model.api.WebhookSignature
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import mockwebserver3.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class WebhookDispatcherTest {
    private val server = MockWebServer()
    private val routes = RoutingDispatcher()
    private val endpoints = FakeEndpoints()
    private val deliveries = FakeDeliveries()
    private val secrets = FakeSecrets()
    private var now = 1_794_478_530_000L
    private lateinit var repository: WebhookRepository
    private lateinit var dispatcher: WebhookDispatcher

    @Before
    fun setUp() {
        server.dispatcher = routes
        server.start()
        repository = WebhookRepository(endpoints, deliveries, secrets, { "local:dev_1" }) { now }
        dispatcher = WebhookDispatcher(endpoints, deliveries, repository, clock = { now })
    }

    @After
    fun tearDown() {
        server.close()
    }

    private suspend fun endpoint(
        events: Set<WebhookEventType> = setOf(WebhookEventType.MessageSent),
        enabled: Boolean = true
    ): Pair<String, String> {
        val (created, secret) = repository.create(server.url("/hook").toString(), events, "Main", enabled)
        return created.id to secret
    }

    @Test
    fun deliversSignedEnvelopes() = runTest {
        val (id, secret) = endpoint()
        routes.on("POST", "/hook", 200, "ok")
        assertEquals(1, repository.emit(WebhookEventType.MessageSent, JsonObject(mapOf("message" to JsonPrimitive("x")))))
        assertEquals(0, repository.emit(WebhookEventType.InboxReceived, JsonObject(emptyMap())))

        assertNull(dispatcher.processDue())

        val request = routes.requests("POST", "/hook").single()
        val body = request.body!!.utf8()
        val envelope = ApiJson.default.decodeFromString(WebhookEnvelope.serializer(), body)
        assertEquals(WebhookEventType.MessageSent, envelope.type)
        assertEquals("local:dev_1", envelope.accountId)
        assertEquals(envelope.id, request.headers[WebhookSignature.EVENT_ID_HEADER])
        assertEquals("message.sent", request.headers[WebhookSignature.EVENT_TYPE_HEADER])
        assertTrue(WebhookSignature.verify(request.headers[WebhookSignature.HEADER]!!, body, secret, now / 1000))
        val delivery = deliveries.rows.values.single()
        assertEquals(id, delivery.endpointId)
        assertEquals(WebhookDeliveryStatus.Succeeded.value, delivery.status)
        assertEquals(1, delivery.attempts)
        assertEquals("ok", delivery.lastResponseBody)
        assertEquals(now, delivery.deliveredAt)
    }

    @Test
    fun failuresBackOffExponentiallyAndGiveUpAfterEightAttempts() = runTest {
        val (id, _) = endpoint()
        routes.on("POST", "/hook", 500, "boom")
        repository.emit(WebhookEventType.MessageSent, JsonObject(emptyMap()))

        val next = dispatcher.processDue()
        val delivery = deliveries.rows.values.single()
        assertEquals(WebhookDeliveryStatus.Retrying.value, delivery.status)
        assertEquals(500, delivery.lastStatusCode)
        assertEquals("HTTP 500", delivery.lastError)
        assertEquals(now + 60_000, next)

        repeat(7) {
            now = deliveries.rows.values.single().nextAttemptAt!!
            dispatcher.processDue()
        }
        val final = deliveries.rows.values.single()
        assertEquals(8, final.attempts)
        assertEquals(WebhookDeliveryStatus.Failed.value, final.status)
        assertNull(final.nextAttemptAt)
        assertEquals(listOf(60_000L, 120_000L, 240_000L), (1..3).map { WebhookDispatcher.backoffMs(it) })

        val redelivered = repository.redeliver(id, final.id)
        assertNotNull(redelivered)
        assertEquals(WebhookDeliveryStatus.Pending, redelivered!!.status)
        assertEquals(0, redelivered.attempts)
    }

    @Test
    fun redirectsAreNotFollowed() = runTest {
        endpoint()
        routes.on("POST", "/hook", 302, "", mapOf("Location" to server.url("/elsewhere").toString()))
        repository.emit(WebhookEventType.MessageSent, JsonObject(emptyMap()))
        dispatcher.processDue()
        assertTrue(routes.requests("POST", "/elsewhere").isEmpty())
        assertEquals(WebhookDeliveryStatus.Retrying.value, deliveries.rows.values.single().status)
    }

    @Test
    fun disabledEndpointsGetNothingAndTestEventsAreQueued() = runTest {
        val (id, _) = endpoint(enabled = false)
        assertEquals(0, repository.emit(WebhookEventType.MessageSent, JsonObject(emptyMap())))
        val test = repository.test(id)!!
        assertEquals(WebhookEventType.WebhookTest, test.eventType)
        dispatcher.processDue()
        assertEquals(WebhookDeliveryStatus.Failed.value, deliveries.rows.getValue(test.id).status)
        assertEquals("Endpoint disabled", deliveries.rows.getValue(test.id).lastError)
    }

    @Test
    fun rotatingReplacesTheSecretAndDeleteRemovesIt() = runTest {
        val (id, secret) = endpoint()
        val rotated = repository.rotateSecret(id)!!
        assertTrue(rotated.startsWith("whsec_"))
        assertTrue(rotated != secret)
        assertEquals(rotated, repository.secretFor(id))
        assertTrue(repository.delete(id))
        assertTrue(secrets.values.isEmpty())
    }

    private class FakeEndpoints : WebhookEndpointsDao {
        val rows = linkedMapOf<String, WebhookEndpointEntity>()

        override fun observeAll(): Flow<List<WebhookEndpointEntity>> = MutableStateFlow(rows.values.toList())

        override suspend fun all(): List<WebhookEndpointEntity> = rows.values.toList()

        override suspend fun get(id: String): WebhookEndpointEntity? = rows[id]

        override suspend fun insert(entity: WebhookEndpointEntity) {
            rows[entity.id] = entity
        }

        override suspend fun update(entity: WebhookEndpointEntity) {
            rows[entity.id] = entity
        }

        override suspend fun delete(id: String): Int = if (rows.remove(id) != null) 1 else 0
    }

    private class FakeDeliveries : WebhookDeliveriesDao {
        val rows = linkedMapOf<String, WebhookDeliveryEntity>()

        override suspend fun insert(entities: List<WebhookDeliveryEntity>) {
            entities.forEach { rows[it.id] = it }
        }

        override suspend fun update(entity: WebhookDeliveryEntity) {
            rows[entity.id] = entity
        }

        override suspend fun get(id: String): WebhookDeliveryEntity? = rows[id]

        override suspend fun query(query: SupportSQLiteQuery): List<WebhookDeliveryEntity> = rows.values.toList()

        override suspend fun due(now: Long, limit: Int): List<WebhookDeliveryEntity> = rows.values
            .filter { it.status in setOf("pending", "retrying") && (it.nextAttemptAt ?: 0) <= now }
            .take(limit)

        override suspend fun nextAttemptAt(): Long? = rows.values.filter {
            it.status in setOf("pending", "retrying")
        }.mapNotNull { it.nextAttemptAt }.minOrNull()

        override fun observeRecent(endpointId: String, limit: Int): Flow<List<WebhookDeliveryEntity>> =
            MutableStateFlow(rows.values.toList())

        override suspend fun truncate(until: Long) = Unit
    }
}
