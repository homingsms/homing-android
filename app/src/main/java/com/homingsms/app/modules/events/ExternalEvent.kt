package com.homingsms.app.modules.events

data class ExternalEvent(
    val type: ExternalEventType,
    val data: String?,
)
