package com.homingsms.app.modules.webhooks.domain

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

@Serializable
data class WebHookEventDTO(
    val id: String,
    val webhookId: String,
    val event: WebHookEvent,
    val deviceId: String,
    val payload: JsonElement,
)
