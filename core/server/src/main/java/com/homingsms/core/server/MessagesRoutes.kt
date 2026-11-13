package com.homingsms.core.server

import com.homingsms.core.model.MessageFilter
import com.homingsms.core.model.MessageState
import com.homingsms.core.model.MessageType
import com.homingsms.core.model.Scope
import com.homingsms.core.model.api.ApiJson
import com.homingsms.core.model.api.MessageDetailDto
import com.homingsms.core.model.api.MessagePage
import com.homingsms.core.model.api.SendMessageRequest
import com.homingsms.core.model.api.SendMessageValidator
import com.homingsms.core.model.api.ValidatedSend
import com.homingsms.core.model.api.Validation
import com.homingsms.core.model.api.toDetailDto
import com.homingsms.core.model.api.toDto
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.server.routing.Route
import java.security.MessageDigest

internal object Params {
    val cursor = ParamSpec(
        "cursor",
        "query",
        Schemas.string(maxLength = ApiCall.MAX_CURSOR),
        description = "Opaque cursor from nextCursor."
    )
    val limit = ParamSpec("limit", "query", Schemas.integer(1, ApiCall.MAX_LIMIT, ApiCall.DEFAULT_LIMIT))
    val messageId = ParamSpec("messageId", "path", Schemas.string(), required = true)
    val inboxMessageId = ParamSpec("inboxMessageId", "path", Schemas.string(), required = true)
    val attachmentId = ParamSpec("attachmentId", "path", Schemas.string(), required = true)
    val webhookId = ParamSpec("webhookId", "path", Schemas.string(), required = true)
    val deliveryId = ParamSpec("deliveryId", "path", Schemas.string(), required = true)
    val deviceId = ParamSpec("deviceId", "query", Schemas.string())
}

internal object Responses {
    val badRequest = ResponseSpec(HttpStatusCode.BadRequest, "The request is malformed or failed validation.", "Error")
    val unauthorized = ResponseSpec(HttpStatusCode.Unauthorized, "Missing or invalid API key.", "Error")
    val forbidden = ResponseSpec(HttpStatusCode.Forbidden, "The API key lacks the required scope.", "Error")
    val notFound = ResponseSpec(HttpStatusCode.NotFound, "The resource does not exist.", "Error")
    val conflict = ResponseSpec(HttpStatusCode.Conflict, "The request conflicts with the current state.", "Error")
    val tooLarge = ResponseSpec(HttpStatusCode.PayloadTooLarge, "The request body is too large.", "Error")
    val secured = listOf(unauthorized, forbidden)
}

internal fun Route.messagesRoutes(context: ApiContext) {
    sendMessageRoute(context)
    listMessagesRoute(context)
    getMessageRoute(context)
    cancelMessageRoute(context)
}

private fun Route.sendMessageRoute(context: ApiContext) = endpoint(
    context,
    RouteSpec(
        method = HttpMethod.Post,
        path = "/messages",
        operationId = "sendMessage",
        summary = "Send an SMS, data SMS or MMS",
        tag = TAG,
        scope = Scope.MessagesSend,
        requestSchema = "SendMessageRequest",
        responses = listOf(
            ResponseSpec(
                HttpStatusCode.OK,
                "Idempotent replay of an earlier request with the same key.",
                DETAIL,
                headers = listOf(IDEMPOTENT_REPLAYED)
            ),
            ResponseSpec(HttpStatusCode.Accepted, "Message accepted and queued.", DETAIL),
            Responses.badRequest,
            Responses.notFound,
            Responses.conflict,
            Responses.tooLarge
        ) + Responses.secured
    )
) { sendMessage() }

private fun Route.listMessagesRoute(context: ApiContext) = endpoint(
    context,
    RouteSpec(
        method = HttpMethod.Get,
        path = "/messages",
        operationId = "listMessages",
        summary = "List messages",
        tag = TAG,
        scope = Scope.MessagesRead,
        params = listOf(
            ParamSpec("state", "query", Schemas.ref("MessageState")),
            ParamSpec("type", "query", Schemas.ref("MessageType")),
            Params.deviceId,
            ParamSpec("to", "query", Schemas.string(), description = "Exact recipient phone number."),
            ParamSpec("createdFrom", "query", Schemas.dateTime(), description = "Inclusive lower bound on createdAt."),
            ParamSpec("createdTo", "query", Schemas.dateTime(), description = "Exclusive upper bound on createdAt."),
            Params.cursor,
            Params.limit
        ),
        responses = listOf(ResponseSpec(HttpStatusCode.OK, "Messages, newest first.", "MessagePage"), Responses.badRequest) +
            Responses.secured
    )
) { listMessages() }

private fun Route.getMessageRoute(context: ApiContext) = endpoint(
    context,
    RouteSpec(
        method = HttpMethod.Get,
        path = "/messages/{messageId}",
        operationId = "getMessage",
        summary = "Get a message with its state timeline",
        tag = TAG,
        scope = Scope.MessagesRead,
        params = listOf(Params.messageId),
        responses = listOf(ResponseSpec(HttpStatusCode.OK, "Message.", DETAIL), Responses.notFound) + Responses.secured
    )
) {
    val record = config.messages.get(pathParam("messageId")) ?: throw ApiException(HttpStatusCode.NotFound, "Message not found")
    respond(HttpStatusCode.OK, MessageDetailDto.serializer(), record.toDetailDto(primaryDeviceId()))
}

private fun Route.cancelMessageRoute(context: ApiContext) = endpoint(
    context,
    RouteSpec(
        method = HttpMethod.Post,
        path = "/messages/{messageId}/cancel",
        operationId = "cancelMessage",
        summary = "Cancel a message that has not reached the phone's send queue",
        tag = TAG,
        scope = Scope.MessagesSend,
        params = listOf(Params.messageId),
        responses = listOf(ResponseSpec(HttpStatusCode.OK, "Cancelled message.", DETAIL), Responses.notFound, Responses.conflict) +
            Responses.secured
    )
) {
    val failure = when (val result = config.messages.cancel(pathParam("messageId"))) {
        is CancelResult.Cancelled -> {
            respond(HttpStatusCode.OK, MessageDetailDto.serializer(), result.record.toDetailDto(primaryDeviceId()))
            null
        }

        CancelResult.NotFound -> ApiException(HttpStatusCode.NotFound, "Message not found")

        is CancelResult.Conflict -> ApiException(HttpStatusCode.Conflict, result.message)
    }
    if (failure != null) throw failure
}

internal fun ApiCall.primaryDeviceId(): String? = config.deviceIds().firstOrNull()

internal fun ApiCall.isOwnDevice(deviceId: String): Boolean = deviceId in config.deviceIds()

private suspend fun ApiCall.sendMessage() {
    val request = body(SendMessageRequest.serializer())
    val validated = validate(request)
    val canonical = ApiJson.default.encodeToString(SendMessageRequest.serializer(), request)
    val hash = MessageDigest.getInstance("SHA-256").digest(canonical.toByteArray(Charsets.UTF_8))
        .joinToString("") { "%02x".format(it) }
    val failure = when (val result = config.messages.send(validated, hash)) {
        is SendResult.Created -> {
            respond(HttpStatusCode.Accepted, MessageDetailDto.serializer(), result.record.toDetailDto(primaryDeviceId()))
            null
        }

        is SendResult.Replayed -> {
            call.response.headers.append(IDEMPOTENT_REPLAYED, "true")
            respond(HttpStatusCode.OK, MessageDetailDto.serializer(), result.record.toDetailDto(primaryDeviceId()))
            null
        }

        is SendResult.Conflict -> ApiException(HttpStatusCode.Conflict, result.message)

        is SendResult.Rejected -> ApiException(HttpStatusCode.BadRequest, result.message)
    }
    if (failure != null) throw failure
}

private fun ApiCall.validate(request: SendMessageRequest): ValidatedSend {
    val validated = when (val result = SendMessageValidator.validate(request)) {
        is Validation.Invalid -> throw ApiException(HttpStatusCode.BadRequest, result.errors, asArray = true)
        is Validation.Valid -> result.value
    }
    validated.deviceId?.let { if (!isOwnDevice(it)) throw ApiException(HttpStatusCode.NotFound, "Device not found") }
    return validated
}

private suspend fun ApiCall.listMessages() {
    val filter = MessageFilter(
        state = enum("state", MessageState.entries.joinToString { it.value }, MessageState::fromValue),
        type = enum("type", MessageType.entries.joinToString { it.value }, MessageType::fromValue),
        to = query("to"),
        createdFrom = time("createdFrom"),
        createdTo = time("createdTo")
    )
    val cursor = cursor()
    val limit = limit()
    val deviceId = query("deviceId")
    if (deviceId != null && !isOwnDevice(deviceId)) {
        respond(HttpStatusCode.OK, MessagePage.serializer(), MessagePage(emptyList(), null))
        return
    }
    val page = config.messages.list(filter, cursor, limit)
    val ownId = primaryDeviceId()
    respond(HttpStatusCode.OK, MessagePage.serializer(), MessagePage(page.items.map { it.toDto(ownId) }, page.nextCursor))
}

internal const val IDEMPOTENT_REPLAYED = "Idempotent-Replayed"
private const val TAG = "Messages"
private const val DETAIL = "MessageDetail"
