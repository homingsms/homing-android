package com.homingsms.core.server

import com.homingsms.core.model.ApiKeyPrincipal
import com.homingsms.core.model.Cursor
import com.homingsms.core.model.Scope
import com.homingsms.core.model.api.ApiError
import com.homingsms.core.model.api.ApiJson
import com.homingsms.core.model.api.Iso8601
import com.homingsms.core.model.grants
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCall
import io.ktor.server.request.path
import io.ktor.server.request.receiveChannel
import io.ktor.server.response.respondText
import io.ktor.server.routing.Route
import io.ktor.server.routing.route
import io.ktor.utils.io.readRemaining
import kotlinx.io.readByteArray
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerializationException
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive

class ApiException(val status: HttpStatusCode, val messages: List<String>, val asArray: Boolean = false, cause: Throwable? = null) :
    RuntimeException(messages.joinToString("; "), cause) {
    constructor(status: HttpStatusCode, message: String, cause: Throwable? = null) : this(status, listOf(message), cause = cause)
}

data class ParamSpec(
    val name: String,
    val location: String,
    val schema: JsonElement,
    val required: Boolean = false,
    val description: String? = null
)

data class ResponseSpec(
    val status: HttpStatusCode,
    val description: String,
    val schema: String? = null,
    val contentType: String = "application/json",
    val headers: List<String> = emptyList()
)

data class RouteSpec(
    val method: HttpMethod,
    val path: String,
    val operationId: String,
    val summary: String,
    val tag: String,
    val scope: Scope?,
    val requestSchema: String? = null,
    val params: List<ParamSpec> = emptyList(),
    val responses: List<ResponseSpec>
)

class ApiRegistry {
    private val specs = mutableListOf<RouteSpec>()

    val routes: List<RouteSpec>
        get() = specs.toList()

    internal fun add(spec: RouteSpec) {
        specs += spec
    }
}

internal class ApiContext(val config: LocalApiConfig, val registry: ApiRegistry)

class ApiCall internal constructor(val call: ApplicationCall, internal val context: ApiContext, val principal: ApiKeyPrincipal?) {
    val config: LocalApiConfig
        get() = context.config

    fun pathParam(name: String): String = call.parameters[name]
        ?: throw ApiException(HttpStatusCode.BadRequest, "$name is required")

    fun query(name: String): String? = call.request.queryParameters[name]?.takeIf { it.isNotEmpty() }

    fun limit(): Int {
        val raw = query("limit") ?: return DEFAULT_LIMIT
        val value = raw.toIntOrNull()
        if (value == null || value !in 1..MAX_LIMIT) {
            throw ApiException(HttpStatusCode.BadRequest, "limit must not be less than 1 and not greater than $MAX_LIMIT")
        }
        return value
    }

    fun cursor(): Cursor? {
        val raw = query("cursor") ?: return null
        if (raw.length > MAX_CURSOR) throw ApiException(HttpStatusCode.BadRequest, "cursor is invalid")
        return Cursor.decode(raw) ?: throw ApiException(HttpStatusCode.BadRequest, "cursor is invalid")
    }

    fun time(name: String): Long? {
        val raw = query(name) ?: return null
        return Iso8601.parse(raw) ?: throw ApiException(HttpStatusCode.BadRequest, "$name must be a valid ISO 8601 date string")
    }

    fun <T : Any> enum(name: String, allowed: String, parse: (String) -> T?): T? {
        val raw = query(name) ?: return null
        return parse(raw) ?: throw ApiException(HttpStatusCode.BadRequest, "$name must be one of the following values: $allowed")
    }

    suspend fun <T> body(serializer: KSerializer<T>): T {
        val text = bodyText()
        return try {
            ApiJson.default.decodeFromString(serializer, text)
        } catch (e: SerializationException) {
            throw invalidBody(e)
        } catch (e: IllegalArgumentException) {
            throw invalidBody(e)
        }
    }

    private fun invalidBody(cause: Exception) =
        ApiException(HttpStatusCode.BadRequest, cause.message?.lineSequence()?.firstOrNull() ?: "Invalid JSON body", cause)

    suspend fun bodyText(): String {
        val declared = call.request.headers[HttpHeaders.ContentLength]?.toLongOrNull()
        val bytes = if (declared != null && declared > MAX_BODY_BYTES) {
            null
        } else {
            call.receiveChannel().readRemaining(MAX_BODY_BYTES + 1).readByteArray()
        }
        val failure = when {
            bytes == null || bytes.size > MAX_BODY_BYTES -> ApiException(HttpStatusCode.PayloadTooLarge, "Request body is too large")
            bytes.isEmpty() -> ApiException(HttpStatusCode.BadRequest, "Request body is required")
            else -> null
        }
        if (failure != null) throw failure
        return requireNotNull(bytes).toString(Charsets.UTF_8)
    }

    suspend fun <T> respond(status: HttpStatusCode, serializer: KSerializer<T>, value: T) {
        val json = if (config.prettyJson) ApiJson.pretty else ApiJson.default
        call.respondText(json.encodeToString(serializer, value), ContentType.Application.Json, status)
    }

    companion object {
        const val DEFAULT_LIMIT = 50
        const val MAX_LIMIT = 100
        const val MAX_CURSOR = 200
        const val MAX_BODY_BYTES = 2L * 1024 * 1024
    }
}

internal fun Route.endpoint(context: ApiContext, spec: RouteSpec, handler: suspend ApiCall.() -> Unit) {
    context.registry.add(spec)
    route(spec.path, spec.method) {
        handle {
            val principal = spec.scope?.let { authorize(context, call, it) }
            ApiCall(call, context, principal).handler()
        }
    }
}

private suspend fun authorize(context: ApiContext, call: ApplicationCall, scope: Scope): ApiKeyPrincipal {
    val header = call.request.headers[HttpHeaders.Authorization]
    val key = header?.takeIf { it.startsWith(BEARER, ignoreCase = true) }?.substring(BEARER.length)?.trim()
    val principal = key?.let { context.config.authenticator.authenticate(it) }
    val failure = when {
        key == null -> ApiException(HttpStatusCode.Unauthorized, "Missing API key")
        principal == null -> ApiException(HttpStatusCode.Unauthorized, "Invalid API key")
        !principal.scopes.grants(scope) -> ApiException(HttpStatusCode.Forbidden, "This API key lacks the ${scope.value} scope")
        else -> null
    }
    if (failure != null) throw failure
    return requireNotNull(principal)
}

private const val BEARER = "Bearer "

internal suspend fun ApplicationCall.respondError(
    config: LocalApiConfig,
    status: HttpStatusCode,
    messages: List<String>,
    asArray: Boolean = false
) {
    val message: JsonElement = if (messages.size == 1 && !asArray) {
        JsonPrimitive(messages.first())
    } else {
        ApiJson.default.encodeToJsonElement(ListSerializer(String.serializer()), messages)
    }
    val error = ApiError(
        statusCode = status.value,
        message = message,
        error = status.description,
        timestamp = Iso8601.format(config.clock()),
        path = request.path()
    )
    respondText(ApiJson.default.encodeToString(ApiError.serializer(), error), ContentType.Application.Json, status)
}
