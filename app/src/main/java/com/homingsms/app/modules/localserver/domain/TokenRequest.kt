package com.homingsms.app.modules.localserver.domain

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable


@Serializable

data class TokenRequest(
    @SerialName("ttl")
    val ttl: Long?,
    @SerialName("scopes")
    val scopes: List<String>,
)
