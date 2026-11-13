package com.homingsms.core.server

import com.homingsms.core.model.api.ApiJson
import com.homingsms.core.model.api.HealthDto
import io.ktor.http.ContentType
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.plugins.BadRequestException
import io.ktor.server.plugins.statuspages.StatusPages
import io.ktor.server.request.httpMethod
import io.ktor.server.request.path
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.route
import io.ktor.server.routing.routing
import io.ktor.util.AttributeKey
import kotlinx.coroutines.CancellationException

private val errorRendered = AttributeKey<Boolean>("homing.errorRendered")

fun Application.installLocalApi(config: LocalApiConfig, version: String): ApiRegistry {
    val registry = ApiRegistry()
    val context = ApiContext(config, registry)
    install(StatusPages) {
        exception<ApiException> { call, cause ->
            call.attributes.put(errorRendered, true)
            call.respondError(config, cause.status, cause.messages, cause.asArray)
        }
        exception<BadRequestException> { call, cause ->
            call.attributes.put(errorRendered, true)
            call.respondError(config, HttpStatusCode.BadRequest, listOf(cause.message ?: "Bad request"))
        }
        exception<CancellationException> { _, cause -> throw cause }
        exception<Throwable> { call, _ ->
            call.attributes.put(errorRendered, true)
            call.respondError(config, HttpStatusCode.InternalServerError, listOf("Internal server error"))
        }
        status(HttpStatusCode.NotFound, HttpStatusCode.MethodNotAllowed) { call, status ->
            if (call.attributes.getOrNull(errorRendered) == true) return@status
            call.attributes.put(errorRendered, true)
            call.respondError(config, status, listOf("Cannot ${call.request.httpMethod.value} ${call.request.path()}"))
        }
    }
    routing {
        route(OpenApiDocument.SERVER_URL) {
            endpoint(
                context,
                RouteSpec(
                    method = HttpMethod.Get,
                    path = "/health",
                    operationId = "getHealth",
                    summary = "Phone and API health",
                    tag = "Health",
                    scope = null,
                    responses = listOf(
                        ResponseSpec(HttpStatusCode.OK, "Healthy.", "Health"),
                        ResponseSpec(HttpStatusCode.ServiceUnavailable, "A dependency is down.", "Health")
                    )
                )
            ) {
                val health = config.health.health()
                val status = if (health.status == HEALTH_OK) HttpStatusCode.OK else HttpStatusCode.ServiceUnavailable
                respond(status, HealthDto.serializer(), health)
            }
            messagesRoutes(context)
            inboxRoutes(context)
            webhooksRoutes(context)
            get(DOCS_PATH) {
                val document = OpenApiDocument.build(registry.routes, version)
                call.respondText(
                    ApiJson.pretty.encodeToString(kotlinx.serialization.json.JsonObject.serializer(), document),
                    ContentType.Application.Json
                )
            }
        }
    }
    return registry
}

const val HEALTH_OK = "ok"
const val HEALTH_DEGRADED = "degraded"
const val DOCS_PATH = "/docs/openapi.json"
