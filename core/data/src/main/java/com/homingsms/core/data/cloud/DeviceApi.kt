package com.homingsms.core.data.cloud

import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.header
import io.ktor.client.request.request
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import java.util.concurrent.TimeUnit
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import okhttp3.OkHttpClient

open class DeviceApiException(val status: Int, message: String, val retryAfterSeconds: Long? = null, cause: Throwable? = null) :
    RuntimeException(message, cause)

class DeviceUnauthorizedException(message: String) : DeviceApiException(UNAUTHORIZED, message)

class DeviceApi(private val client: HttpClient, private val serverUrl: () -> String?, private val token: () -> String?) {
    suspend fun pair(serverUrl: String, request: PairRequest): PairResponse =
        call(HttpMethod.Post, "/device/pair", PairResponse.serializer(), base = serverUrl, authorized = false) {
            json(PairRequest.serializer(), request)
        }

    suspend fun heartbeat(request: HeartbeatRequest): HeartbeatResponse =
        call(HttpMethod.Post, "/device/heartbeat", HeartbeatResponse.serializer()) {
            json(HeartbeatRequest.serializer(), request)
        }

    suspend fun updateFcmToken(fcmToken: String?) {
        val body = JsonObject(mapOf("fcmToken" to (fcmToken?.let(::JsonPrimitive) ?: JsonNull)))
        call<Unit>(HttpMethod.Put, "/device/fcm-token", null) { json(JsonObject.serializer(), body) }
    }

    suspend fun pull(limit: Int): PullResponse = call(HttpMethod.Post, "/device/messages/pull", PullResponse.serializer()) {
        json(PullRequest.serializer(), PullRequest(limit))
    }

    suspend fun reportStates(reports: List<StateReportDto>): StateReportResponse =
        call(HttpMethod.Post, "/device/messages/states", StateReportResponse.serializer()) {
            json(StateReportRequest.serializer(), StateReportRequest(reports))
        }

    suspend fun pushInbox(messages: List<InboxUploadDto>): InboxPushResponse =
        call(HttpMethod.Post, "/device/inbox", InboxPushResponse.serializer()) {
            json(InboxPushRequest.serializer(), InboxPushRequest(messages))
        }

    suspend fun settings(): DeviceSettingsDto = call(HttpMethod.Get, "/device/settings", DeviceSettingsDto.serializer())

    private fun <T> HttpRequestBuilder.json(serializer: KSerializer<T>, value: T) {
        contentType(ContentType.Application.Json)
        setBody(requestJson.encodeToString(serializer, value))
    }

    private suspend fun <T> call(
        method: HttpMethod,
        path: String,
        serializer: KSerializer<T>?,
        base: String? = null,
        authorized: Boolean = true,
        block: HttpRequestBuilder.() -> Unit = {}
    ): T {
        val root = base ?: serverUrl() ?: error("Cloud server is not configured")
        val bearer = if (authorized) token() ?: throw DeviceUnauthorizedException("Device is not paired") else null
        val response: HttpResponse = client.request(apiUrl(root) + path) {
            this.method = method
            header(HttpHeaders.Accept, ContentType.Application.Json.toString())
            bearer?.let { header(HttpHeaders.Authorization, "Bearer $it") }
            block()
        }
        val text = response.bodyAsText()
        if (!response.status.isSuccess()) throw failure(response, text)
        @Suppress("UNCHECKED_CAST")
        if (serializer == null) return Unit as T
        return decode(serializer, text, response.status.value, path)
    }

    private fun <T> decode(serializer: KSerializer<T>, text: String, status: Int, path: String): T = try {
        responseJson.decodeFromString(serializer, text)
    } catch (e: SerializationException) {
        throw DeviceApiException(status, "Unexpected response from $path: ${e.message}", cause = e)
    }

    private fun failure(response: HttpResponse, body: String): DeviceApiException {
        val message = runCatching {
            when (val element = responseJson.decodeFromString(ErrorBody.serializer(), body).message) {
                is JsonPrimitive -> element.content
                is JsonArray -> element.joinToString("; ") { (it as? JsonPrimitive)?.content ?: it.toString() }
                else -> element.toString()
            }
        }.getOrDefault(body.take(MAX_ERROR_TEXT))
        val status = response.status.value
        return if (status == UNAUTHORIZED) {
            DeviceUnauthorizedException(message)
        } else {
            DeviceApiException(status, message, response.headers[HttpHeaders.RetryAfter]?.toLongOrNull())
        }
    }

    @kotlinx.serialization.Serializable
    private data class ErrorBody(val message: kotlinx.serialization.json.JsonElement = JsonPrimitive(""))

    companion object {
        private const val MAX_ERROR_TEXT = 200
        private const val CONNECT_TIMEOUT_SECONDS = 15L
        private const val READ_TIMEOUT_SECONDS = 30L

        val requestJson = Json {
            explicitNulls = false
            encodeDefaults = true
        }

        val responseJson = Json {
            ignoreUnknownKeys = true
            coerceInputValues = true
            explicitNulls = false
        }

        fun apiUrl(serverUrl: String): String = serverUrl.trimEnd('/').let { if (it.endsWith("/api/v1")) it else "$it/api/v1" }

        fun createClient(okHttpClient: OkHttpClient = defaultOkHttp()): HttpClient = HttpClient(OkHttp) {
            expectSuccess = false
            engine { preconfigured = okHttpClient }
        }

        fun defaultOkHttp(): OkHttpClient = OkHttpClient.Builder()
            .connectTimeout(CONNECT_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .readTimeout(READ_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .writeTimeout(READ_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .build()
    }
}

private const val UNAUTHORIZED = 401
