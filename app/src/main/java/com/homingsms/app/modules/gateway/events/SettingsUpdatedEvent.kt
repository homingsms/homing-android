package com.homingsms.app.modules.gateway.events

import com.homingsms.app.modules.events.AppEvent

class SettingsUpdatedEvent : AppEvent(NAME) {

    companion object {
        const val NAME = "SettingsUpdatedEvent"
    }
}
