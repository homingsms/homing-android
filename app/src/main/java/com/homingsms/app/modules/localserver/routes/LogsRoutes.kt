package com.homingsms.app.modules.localserver.routes

import com.homingsms.app.helpers.DateTimeParser
import com.homingsms.app.modules.localserver.auth.AuthScopes
import com.homingsms.app.modules.localserver.auth.requireScope
import com.homingsms.app.modules.localserver.domain.GetLogsResponse
import com.homingsms.app.modules.logs.LogsService
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get

class LogsRoutes(
    private val logsService: LogsService,
) {

    fun register(routing: Route) {
        routing.apply {
            logsRoutes()
        }
    }

    private fun Route.logsRoutes() {
        get {
            if (!requireScope(AuthScopes.LogsRead)) return@get
            try {
                val from = call.request.queryParameters["from"]?.let {
                    DateTimeParser.parseIsoDateTime(it)?.time
                }
                val to = call.request.queryParameters["to"]?.let {
                    DateTimeParser.parseIsoDateTime(it)?.time
                }

                call.respond(logsService.select(from, to).map { GetLogsResponse.from(it) })
            } catch (e: Exception) {
                call.respond(HttpStatusCode.BadRequest, mapOf("message" to e.message))
                return@get
            }
        }
    }
}
