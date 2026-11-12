package com.homingsms.app.modules.webhooks.domain

import com.homingsms.app.domain.EntitySource
import kotlinx.serialization.Serializable

@Serializable
data class WebHookDTO(
    val id: String?,
    val deviceId: String?,
    val url: String,
    val event: WebHookEvent,
    val source: EntitySource,
)
