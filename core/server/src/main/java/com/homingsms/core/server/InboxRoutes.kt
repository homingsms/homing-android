package com.homingsms.core.server

import com.homingsms.core.model.InboxFilter
import com.homingsms.core.model.MessageType
import com.homingsms.core.model.Scope
import com.homingsms.core.model.api.InboxMessageDto
import com.homingsms.core.model.api.InboxPage
import com.homingsms.core.model.api.toDto
import io.ktor.http.ContentDisposition
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.server.response.header
import io.ktor.server.response.respondBytes
import io.ktor.server.routing.Route

internal fun Route.inboxRoutes(context: ApiContext) {
    listInboxRoute(context)
    getInboxRoute(context)
    attachmentRoute(context)
}

private fun Route.listInboxRoute(context: ApiContext) = endpoint(
    context,
    RouteSpec(
        method = HttpMethod.Get,
        path = "/inbox",
        operationId = "listInbox",
        summary = "List received messages",
        tag = TAG,
        scope = Scope.InboxRead,
        params = listOf(
            ParamSpec("type", "query", Schemas.ref("MessageType")),
            Params.deviceId,
            ParamSpec("from", "query", Schemas.string(maxLength = MAX_SENDER), description = "Exact sender."),
            ParamSpec("receivedFrom", "query", Schemas.dateTime()),
            ParamSpec("receivedTo", "query", Schemas.dateTime()),
            Params.cursor,
            Params.limit
        ),
        responses = listOf(ResponseSpec(HttpStatusCode.OK, "Received messages, newest first.", "InboxPage"), Responses.badRequest) +
            Responses.secured
    )
) {
    val filter = InboxFilter(
        type = enum("type", MessageType.entries.joinToString { it.value }, MessageType::fromValue),
        from = query("from"),
        receivedFrom = time("receivedFrom"),
        receivedTo = time("receivedTo")
    )
    val cursor = cursor()
    val limit = limit()
    val deviceId = query("deviceId")
    val page = if (deviceId != null && !isOwnDevice(deviceId)) null else config.inbox.list(filter, cursor, limit)
    val ownId = primaryDeviceId()
    val body = InboxPage(page?.items.orEmpty().map { it.toDto(ownId) }, page?.nextCursor)
    respond(HttpStatusCode.OK, InboxPage.serializer(), body)
}

private fun Route.getInboxRoute(context: ApiContext) = endpoint(
    context,
    RouteSpec(
        method = HttpMethod.Get,
        path = "/inbox/{inboxMessageId}",
        operationId = "getInboxMessage",
        summary = "Get a received message",
        tag = TAG,
        scope = Scope.InboxRead,
        params = listOf(Params.inboxMessageId),
        responses = listOf(ResponseSpec(HttpStatusCode.OK, "Received message.", "InboxMessage"), Responses.notFound) + Responses.secured
    )
) {
    val record = config.inbox.get(pathParam("inboxMessageId")) ?: throw ApiException(HttpStatusCode.NotFound, "Inbox message not found")
    respond(HttpStatusCode.OK, InboxMessageDto.serializer(), record.toDto(primaryDeviceId()))
}

private fun Route.attachmentRoute(context: ApiContext) = endpoint(
    context,
    RouteSpec(
        method = HttpMethod.Get,
        path = "/inbox/{inboxMessageId}/attachments/{attachmentId}",
        operationId = "getInboxAttachment",
        summary = "Download an MMS attachment",
        tag = TAG,
        scope = Scope.InboxRead,
        params = listOf(Params.inboxMessageId, Params.attachmentId),
        responses = listOf(
            ResponseSpec(HttpStatusCode.OK, "Raw attachment bytes with their original content type.", "Binary", "application/octet-stream"),
            Responses.notFound
        ) + Responses.secured
    )
) {
    val content = config.inbox.attachment(pathParam("inboxMessageId"), pathParam("attachmentId"))
        ?: throw ApiException(HttpStatusCode.NotFound, "Attachment not found")
    val contentType = runCatching { ContentType.parse(content.contentType) }.getOrDefault(ContentType.Application.OctetStream)
    content.name?.let {
        call.response.header(
            HttpHeaders.ContentDisposition,
            ContentDisposition.Attachment.withParameter(ContentDisposition.Parameters.FileName, it).toString()
        )
    }
    call.respondBytes(content.bytes, contentType, HttpStatusCode.OK)
}

private const val MAX_SENDER = 32
private const val TAG = "Inbox"
