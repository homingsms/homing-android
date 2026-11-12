package com.homingsms.app.modules.gateway.inbox

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable


@Serializable

data class EncryptedAttachment(
    @SerialName("partId") val partId: Long,
    @SerialName("contentType") val contentType: String,
    @SerialName("name") val name: String,
    @SerialName("size") val size: Long?,
    @SerialName("data") val data: String,
)
