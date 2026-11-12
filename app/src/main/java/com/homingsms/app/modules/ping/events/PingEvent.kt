package com.homingsms.app.modules.ping.events

import com.homingsms.app.domain.HealthResponse
import com.homingsms.app.modules.events.AppEvent

class PingEvent(
    val health: HealthResponse,
) : AppEvent(TYPE) {
    companion object {
        const val TYPE = "PingEvent"
    }
}
