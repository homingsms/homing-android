package com.homingsms.core.data.webhooks

import com.homingsms.core.data.db.WebhookDeliveriesDao
import com.homingsms.core.data.db.WebhookDeliveryEntity
import com.homingsms.core.data.db.WebhookEndpointsDao
import com.homingsms.core.model.WebhookDeliveryStatus
import com.homingsms.core.model.api.WebhookSignature
import java.io.IOException
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody

class WebhookDispatcher(
    private val endpoints: WebhookEndpointsDao,
    private val deliveries: WebhookDeliveriesDao,
    private val repository: WebhookRepository,
    private val client: OkHttpClient = defaultClient(),
    private val userAgent: String = DEFAULT_USER_AGENT,
    private val clock: () -> Long = System::currentTimeMillis
) {
    suspend fun processDue(limit: Int = BATCH): Long? {
        while (true) {
            val due = deliveries.due(clock(), limit)
            due.forEach { attempt(it) }
            if (due.size < limit) break
        }
        return deliveries.nextAttemptAt()
    }

    suspend fun attempt(delivery: WebhookDeliveryEntity): WebhookDeliveryEntity {
        val endpoint = endpoints.get(delivery.endpointId)
        if (endpoint == null || !endpoint.enabled) {
            val skipped = delivery.copy(
                status = WebhookDeliveryStatus.Failed.value,
                lastError = "Endpoint disabled",
                nextAttemptAt = null,
                updatedAt = clock()
            )
            deliveries.update(skipped)
            return skipped
        }
        val secret = repository.secretFor(endpoint.id)
        val attempts = delivery.attempts + 1
        val outcome = send(endpoint.url, delivery, secret)
        val now = clock()
        val updated = if (outcome.success) {
            delivery.copy(
                status = WebhookDeliveryStatus.Succeeded.value,
                attempts = attempts,
                lastStatusCode = outcome.statusCode,
                lastResponseBody = outcome.body,
                lastError = null,
                nextAttemptAt = null,
                deliveredAt = now,
                updatedAt = now
            )
        } else {
            val exhausted = attempts >= MAX_ATTEMPTS
            delivery.copy(
                status = if (exhausted) WebhookDeliveryStatus.Failed.value else WebhookDeliveryStatus.Retrying.value,
                attempts = attempts,
                lastStatusCode = outcome.statusCode,
                lastResponseBody = outcome.body,
                lastError = outcome.error,
                nextAttemptAt = if (exhausted) null else now + backoffMs(attempts),
                updatedAt = now
            )
        }
        deliveries.update(updated)
        return updated
    }

    private suspend fun send(url: String, delivery: WebhookDeliveryEntity, secret: String): Outcome = withContext(Dispatchers.IO) {
        val timestamp = clock() / MILLIS
        val request = Request.Builder()
            .url(url)
            .post(delivery.payload.toRequestBody(JSON))
            .header("User-Agent", userAgent)
            .header(WebhookSignature.EVENT_ID_HEADER, delivery.eventId)
            .header(WebhookSignature.EVENT_TYPE_HEADER, delivery.eventType)
            .header(WebhookSignature.HEADER, WebhookSignature.sign(secret, timestamp, delivery.payload))
            .build()
        try {
            client.newCall(request).execute().use { response ->
                val body = response.body.source().let { source ->
                    source.request(MAX_BODY.toLong())
                    source.buffer.clone().readUtf8(minOf(source.buffer.size, MAX_BODY.toLong()))
                }
                Outcome(response.isSuccessful, response.code, body, if (response.isSuccessful) null else "HTTP ${response.code}")
            }
        } catch (e: IOException) {
            Outcome(false, null, null, e.message ?: e.javaClass.simpleName)
        } catch (e: IllegalArgumentException) {
            Outcome(false, null, null, e.message ?: "Invalid URL")
        }
    }

    private data class Outcome(val success: Boolean, val statusCode: Int?, val body: String?, val error: String?)

    companion object {
        const val MAX_ATTEMPTS = 8
        const val BASE_BACKOFF_MS = 60_000L
        const val DEFAULT_USER_AGENT = "Homing-Android-Webhooks/1.0"
        private const val BATCH = 20
        private const val MAX_BODY = 1024
        private const val MILLIS = 1000L
        private const val TIMEOUT_SECONDS = 10L
        private val JSON = "application/json".toMediaType()

        fun backoffMs(attempts: Int): Long = BASE_BACKOFF_MS shl (attempts - 1).coerceIn(0, MAX_ATTEMPTS)

        fun defaultClient(): OkHttpClient = OkHttpClient.Builder()
            .followRedirects(false)
            .followSslRedirects(false)
            .callTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .build()
    }
}
