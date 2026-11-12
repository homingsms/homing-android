package com.homingsms.app.modules.localserver.routes

import com.homingsms.app.modules.localserver.auth.AuthScopes
import com.homingsms.app.modules.localserver.auth.requireScope
import com.homingsms.app.modules.settings.SettingsService
import com.homingsms.app.serialization.toJsonElement
import com.homingsms.app.serialization.toPlainMap
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.patch
import kotlinx.serialization.json.JsonObject

class SettingsRoutes(
    private val settingsService: SettingsService
) {
    fun register(routing: Route) {
        routing.apply {
            settingsRoutes()
        }
    }

    private fun Route.settingsRoutes() {
        get {
            if (!requireScope(AuthScopes.SettingsRead)) return@get
            try {
                val settings = settingsService.getAll()
                call.respond(settings.toJsonElement())
            } catch (e: Exception) {
                e.printStackTrace()
                call.respond(
                    HttpStatusCode.InternalServerError,
                    mapOf("message" to "Failed to get settings: ${e.message}")
                )
            }
        }
        patch {
            if (!requireScope(AuthScopes.SettingsWrite)) return@patch
            try {
                val settings = call.receive<JsonObject>().toPlainMap()

                settingsService.update(settings)

                call.respond(
                    HttpStatusCode.OK,
                    settingsService.getAll().toJsonElement()
                )
            } catch (e: Exception) {
                e.printStackTrace()
                call.respond(
                    HttpStatusCode.BadRequest,
                    mapOf("message" to "Invalid request: ${e.message}")
                )
            }
        }
    }
}
