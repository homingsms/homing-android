package com.homingsms.app.modules.device.events

import com.homingsms.app.modules.events.AppEvent

class DeviceKeyRotatedEvent : AppEvent(NAME) {
    companion object {
        const val NAME = "DeviceKeyRotatedEvent"
    }
}
