package com.homingsms.app.modules.health.domain

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable


@Serializable

enum class Status {
    @SerialName("pass")
    PASS,

    @SerialName("warn")
    WARN,

    @SerialName("fail")
    FAIL,
}
