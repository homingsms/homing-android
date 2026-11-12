@file:UseSerializers(DateSerializer::class)

package com.homingsms.app.modules.localserver.domain

import com.homingsms.app.serialization.DateSerializer
import java.util.Date
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.UseSerializers

@Serializable
data class TokenResponse(
    @SerialName("id")
    val id: String,
    @SerialName("token_type")
    val tokenType: String,
    @SerialName("access_token")
    val accessToken: String,
    @SerialName("expires_at")
    val expiresAt: Date,
    @SerialName("refresh_token")
    val refreshToken: String? = null,
)
