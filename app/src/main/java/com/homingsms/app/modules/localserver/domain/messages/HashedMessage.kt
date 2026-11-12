package com.homingsms.app.modules.localserver.domain.messages

import kotlinx.serialization.Serializable

@Serializable
data class HashedMessage(
    val hash: String,
)
