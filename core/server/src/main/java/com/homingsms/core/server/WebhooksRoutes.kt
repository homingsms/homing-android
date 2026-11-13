package com.homingsms.core.server

import com.homingsms.core.model.Scope
import com.homingsms.core.model.WebhookDeliveryStatus
import com.homingsms.core.model.WebhookEventType
import com.homingsms.core.model.api.WebhookDeliveryDto
import com.homingsms.core.model.api.WebhookDeliveryPage
import com.homingsms.core.model.api.WebhookEndpointDto
import com.homingsms.core.model.api.WebhookEndpointList
import com.homingsms.core.model.api.WebhookEndpointWithSecretDto
import com.homingsms.core.model.api.WebhookSecretDto
import com.homingsms.core.model.api.toDto
import com.homingsms.core.model.api.toDtoWithSecret
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import java.net.URI
import java.net.URISyntaxException
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull

@Serializable
data class CreateWebhookRequest(
    val url: String? = null,
    val events: List<String>? = null,
    val description: String? = null,
    val enabled: Boolean? = null
)

@Serializable
data class UpdateWebhookRequest(
    val url: String? = null,
    val events: List<String>? = null,
    val description: String? = null,
    val enabled: Boolean? = null
)

internal fun Route.webhooksRoutes(context: ApiContext) {
    listWebhooksRoute(context)
    createWebhookRoute(context)
    getWebhookRoute(context)
    updateWebhookRoute(context)
    deleteWebhookRoute(context)
    rotateSecretRoute(context)
    testWebhookRoute(context)
    deliveriesRoute(context)
    redeliverRoute(context)
}

private fun Route.listWebhooksRoute(context: ApiContext) = endpoint(
    context,
    spec(HttpMethod.Get, "/webhooks", "listWebhooks", "List webhook endpoints", ok("Endpoints, oldest first.", "WebhookEndpointList"))
) {
    respond(HttpStatusCode.OK, WebhookEndpointList.serializer(), WebhookEndpointList(config.webhooks.list().map { it.toDto() }))
}

private fun Route.createWebhookRoute(context: ApiContext) = endpoint(
    context,
    spec(
        HttpMethod.Post,
        "/webhooks",
        "createWebhook",
        "Create a webhook endpoint",
        ResponseSpec(HttpStatusCode.Created, "Endpoint created.", "WebhookEndpointWithSecret"),
        requestSchema = "CreateWebhookRequest"
    )
) {
    val request = body(CreateWebhookRequest.serializer())
    val errors = mutableListOf<String>()
    val url = validateUrl(request.url, errors)
    val events = validateEvents(request.events, errors)
    if ((request.description?.length ?: 0) > MAX_DESCRIPTION) errors += DESCRIPTION_TOO_LONG
    if (errors.isNotEmpty() || url == null || events == null) throw ApiException(HttpStatusCode.BadRequest, errors, asArray = true)
    val created = config.webhooks.create(NewWebhook(url, events, request.description, request.enabled ?: true))
    respond(HttpStatusCode.Created, WebhookEndpointWithSecretDto.serializer(), created.endpoint.toDtoWithSecret(created.secret))
}

private fun Route.getWebhookRoute(context: ApiContext) = endpoint(
    context,
    spec(HttpMethod.Get, WEBHOOK, "getWebhook", "Get a webhook endpoint", ok("Endpoint.", "WebhookEndpoint"))
) {
    val endpoint = config.webhooks.get(pathParam("webhookId")) ?: throw webhookNotFound()
    respond(HttpStatusCode.OK, WebhookEndpointDto.serializer(), endpoint.toDto())
}

private fun Route.updateWebhookRoute(context: ApiContext) = endpoint(
    context,
    spec(
        HttpMethod.Patch,
        WEBHOOK,
        "updateWebhook",
        "Update a webhook endpoint",
        ok("Updated endpoint.", "WebhookEndpoint"),
        requestSchema = "UpdateWebhookRequest"
    )
) {
    val patch = parsePatch(body(JsonObject.serializer()))
    val updated = config.webhooks.update(pathParam("webhookId"), patch) ?: throw webhookNotFound()
    respond(HttpStatusCode.OK, WebhookEndpointDto.serializer(), updated.toDto())
}

private fun Route.deleteWebhookRoute(context: ApiContext) = endpoint(
    context,
    spec(
        HttpMethod.Delete,
        WEBHOOK,
        "deleteWebhook",
        "Delete a webhook endpoint",
        ResponseSpec(HttpStatusCode.NoContent, "Deleted with its delivery log.")
    )
) {
    if (!config.webhooks.delete(pathParam("webhookId"))) throw webhookNotFound()
    call.respond(HttpStatusCode.NoContent)
}

private fun Route.rotateSecretRoute(context: ApiContext) = endpoint(
    context,
    spec(HttpMethod.Post, "$WEBHOOK/rotate-secret", "rotateWebhookSecret", "Replace the signing secret", ok("New secret.", "WebhookSecret"))
) {
    val secret = config.webhooks.rotateSecret(pathParam("webhookId")) ?: throw webhookNotFound()
    respond(HttpStatusCode.OK, WebhookSecretDto.serializer(), WebhookSecretDto(secret))
}

private fun Route.testWebhookRoute(context: ApiContext) = endpoint(
    context,
    spec(
        HttpMethod.Post,
        "$WEBHOOK/test",
        "sendWebhookTest",
        "Send a webhook.test event to this endpoint",
        ResponseSpec(HttpStatusCode.Accepted, "Test delivery queued.", "WebhookDelivery")
    )
) {
    val delivery = config.webhooks.test(pathParam("webhookId")) ?: throw webhookNotFound()
    respond(HttpStatusCode.Accepted, WebhookDeliveryDto.serializer(), delivery.toDto())
}

private fun Route.deliveriesRoute(context: ApiContext) = endpoint(
    context,
    spec(
        HttpMethod.Get,
        "$WEBHOOK/deliveries",
        "listWebhookDeliveries",
        "List the delivery log of an endpoint",
        ok("Deliveries, newest first.", "WebhookDeliveryPage"),
        params = listOf(ParamSpec("status", "query", Schemas.ref("WebhookDeliveryStatus")), Params.cursor, Params.limit)
    )
) {
    val status = enum("status", WebhookDeliveryStatus.entries.joinToString { it.value }, WebhookDeliveryStatus::fromValue)
    val cursor = cursor()
    val limit = limit()
    val page = config.webhooks.deliveries(pathParam("webhookId"), status, cursor, limit) ?: throw webhookNotFound()
    respond(HttpStatusCode.OK, WebhookDeliveryPage.serializer(), WebhookDeliveryPage(page.items.map { it.toDto() }, page.nextCursor))
}

private fun Route.redeliverRoute(context: ApiContext) = endpoint(
    context,
    spec(
        HttpMethod.Post,
        "$WEBHOOK/deliveries/{deliveryId}/redeliver",
        "redeliverWebhook",
        "Send a delivery again",
        ResponseSpec(HttpStatusCode.Accepted, "Redelivery queued.", "WebhookDelivery"),
        params = listOf(Params.deliveryId)
    )
) {
    val delivery = config.webhooks.redeliver(pathParam("webhookId"), pathParam("deliveryId"))
        ?: throw ApiException(HttpStatusCode.NotFound, "Delivery not found")
    respond(HttpStatusCode.Accepted, WebhookDeliveryDto.serializer(), delivery.toDto())
}

private fun ok(description: String, schema: String) = ResponseSpec(HttpStatusCode.OK, description, schema)

private fun spec(
    method: HttpMethod,
    path: String,
    operationId: String,
    summary: String,
    success: ResponseSpec,
    requestSchema: String? = null,
    params: List<ParamSpec> = emptyList()
): RouteSpec {
    val hasId = "{webhookId}" in path
    val validates = requestSchema != null || params.isNotEmpty()
    return RouteSpec(
        method = method,
        path = path,
        operationId = operationId,
        summary = summary,
        tag = "Webhooks",
        scope = Scope.WebhooksManage,
        requestSchema = requestSchema,
        params = (if (hasId) listOf(Params.webhookId) else emptyList()) + params,
        responses = listOfNotNull(
            success,
            Responses.badRequest.takeIf { validates },
            Responses.notFound.takeIf { hasId }
        ) + Responses.secured
    )
}

private fun webhookNotFound() = ApiException(HttpStatusCode.NotFound, "Webhook not found")

private fun ApiCall.validateUrl(value: String?, errors: MutableList<String>): String? {
    if (value == null || value.length > MAX_URL) {
        errors += "url must be a URL address"
        return null
    }
    val uri = try {
        URI(value)
    } catch (_: URISyntaxException) {
        errors += "url must be a URL address"
        return null
    }
    val host = uri.host
    val scheme = uri.scheme?.lowercase()
    val loopback = host == "127.0.0.1" || host == "localhost" || host == "[::1]"
    val allowed = when (scheme) {
        "https" -> !host.isNullOrBlank()
        "http" -> !host.isNullOrBlank() && (loopback || config.allowCleartextWebhooks)
        else -> false
    }
    if (!allowed) {
        errors += "url must use https:// (plain http is accepted only for 127.0.0.1)"
        return null
    }
    return value
}

private fun validateEvents(values: List<String>?, errors: MutableList<String>): Set<WebhookEventType>? {
    if (values.isNullOrEmpty()) {
        errors += "events must contain at least 1 elements"
        return null
    }
    val parsed = values.map { WebhookEventType.fromValue(it)?.takeIf { type -> type.subscribable } }
    if (parsed.any { it == null }) {
        val allowed = WebhookEventType.entries.filter { it.subscribable }.joinToString { it.value }
        errors += "each value in events must be one of the following values: $allowed"
        return null
    }
    if (values.toSet().size != values.size) {
        errors += "All events's elements must be unique"
        return null
    }
    return parsed.filterNotNull().toSet()
}

private fun ApiCall.parsePatch(json: JsonObject): WebhookPatch {
    val errors = mutableListOf<String>()
    val url = json["url"]?.let { element ->
        val text = (element as? JsonPrimitive)?.takeIf { it.isString }?.content
        validateUrl(text, errors)
    }
    val events = json["events"]?.let { element ->
        val list = (element as? JsonArray)?.map { (it as? JsonPrimitive)?.contentOrNull.orEmpty() }
        validateEvents(list, errors)
    }
    val descriptionSet = json.containsKey("description")
    val description = when (val element = json["description"]) {
        null, JsonNull -> null

        is JsonPrimitive -> element.contentOrNull

        else -> {
            errors += "description must be a string"
            null
        }
    }
    if ((description?.length ?: 0) > MAX_DESCRIPTION) errors += DESCRIPTION_TOO_LONG
    val enabled = json["enabled"]?.let { element ->
        (element as? JsonPrimitive)?.booleanOrNull ?: run {
            errors += "enabled must be a boolean value"
            null
        }
    }
    if (errors.isNotEmpty()) throw ApiException(HttpStatusCode.BadRequest, errors, asArray = true)
    return WebhookPatch(url, events, descriptionSet, description, enabled)
}

private const val MAX_URL = 2048
private const val MAX_DESCRIPTION = 200
private const val WEBHOOK = "/webhooks/{webhookId}"
private const val DESCRIPTION_TOO_LONG = "description must be shorter than or equal to $MAX_DESCRIPTION characters"
