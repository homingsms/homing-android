package com.homingsms.app.modules.localserver.domain.messages

import kotlinx.serialization.Serializable

@Serializable
data class DataMessage(
    val data: String,  // Base64-encoded payload
    val port: Int,      // Destination port (0-65535)
)
