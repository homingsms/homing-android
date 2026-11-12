package com.homingsms.app.domain

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable


@Serializable

enum class WebhookDelivery {
    @SerialName("Disabled")
    Disabled,

    @SerialName("Individual")
    Individual,

    @SerialName("Batch")
    Batch,
}
