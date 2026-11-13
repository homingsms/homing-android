package com.homingsms.core.data

import com.homingsms.core.data.cloud.CloudConfigStore
import com.homingsms.core.data.db.CloudInboxOutboxEntity
import com.homingsms.core.data.db.CloudOutboxDao
import com.homingsms.core.data.db.CloudStateReportEntity
import com.homingsms.core.data.secrets.SecretStore
import com.homingsms.core.model.CloudSettings
import java.util.ArrayDeque
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import mockwebserver3.Dispatcher
import mockwebserver3.MockResponse
import mockwebserver3.RecordedRequest

class FakeSecrets : SecretStore {
    val values = mutableMapOf<String, String>()

    override fun get(key: String): String? = values[key]

    override fun put(key: String, value: String) {
        values[key] = value
    }

    override fun remove(key: String) {
        values.remove(key)
    }
}

class FakeConfig(var value: CloudSettings = CloudSettings()) : CloudConfigStore {
    override val cloud: CloudSettings
        get() = value

    override suspend fun update(transform: (CloudSettings) -> CloudSettings) {
        value = transform(value)
    }
}

class FakeOutbox : CloudOutboxDao {
    val states = mutableListOf<CloudStateReportEntity>()
    val inbox = linkedMapOf<String, CloudInboxOutboxEntity>()
    private var nextId = 1L

    override suspend fun insertState(entity: CloudStateReportEntity): Long {
        val id = nextId++
        states += entity.copy(id = id)
        return id
    }

    override suspend fun states(limit: Int): List<CloudStateReportEntity> = states.sortedBy { it.id }.take(limit)

    override suspend fun deleteStates(ids: List<Long>) {
        states.removeAll { it.id in ids }
    }

    override suspend fun incrementStateAttempts(ids: List<Long>) {
        states.replaceAll { if (it.id in ids) it.copy(attempts = it.attempts + 1) else it }
    }

    override fun observeStateCount(): Flow<Int> = MutableStateFlow(states.size)

    override suspend fun insertInbox(entity: CloudInboxOutboxEntity): Long {
        if (entity.localId in inbox) return -1
        inbox[entity.localId] = entity
        return 1
    }

    override suspend fun inbox(limit: Int): List<CloudInboxOutboxEntity> = inbox.values.take(limit)

    override suspend fun deleteInbox(localIds: List<String>) {
        localIds.forEach { inbox.remove(it) }
    }

    override suspend fun markInboxFailed(localIds: List<String>, error: String?) {
        localIds.forEach { id -> inbox[id]?.let { inbox[id] = it.copy(attempts = it.attempts + 1, lastError = error) } }
    }

    override fun observeInboxCount(): Flow<Int> = MutableStateFlow(inbox.size)

    override suspend fun clearStates() {
        states.clear()
    }

    override suspend fun clearInbox() {
        inbox.clear()
    }
}

class RoutingDispatcher : Dispatcher() {
    private val routes = mutableMapOf<String, ArrayDeque<MockResponse>>()
    val requests = mutableListOf<RecordedRequest>()

    fun on(method: String, path: String, code: Int, body: String = "", headers: Map<String, String> = emptyMap()) {
        val builder = MockResponse.Builder().code(code).body(body).addHeader("Content-Type", "application/json")
        headers.forEach { (key, value) -> builder.addHeader(key, value) }
        routes.getOrPut("$method $path") { ArrayDeque() }.add(builder.build())
    }

    fun requests(method: String, path: String): List<RecordedRequest> =
        synchronized(requests) { requests.filter { it.method == method && it.url.encodedPath == path } }

    override fun dispatch(request: RecordedRequest): MockResponse {
        synchronized(requests) { requests += request }
        val queue = routes["${request.method} ${request.url.encodedPath}"]
        val response = if (queue != null && queue.size > 1) queue.poll() else queue?.peek()
        return response ?: MockResponse.Builder().code(NOT_FOUND).body("""{"statusCode":404,"message":"no route"}""").build()
    }

    private companion object {
        const val NOT_FOUND = 404
    }
}
