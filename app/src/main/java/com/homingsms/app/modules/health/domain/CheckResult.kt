package com.homingsms.app.modules.health.domain

import kotlinx.serialization.Serializable

@Serializable
data class CheckResult(
    val status: Status,
    val observedValue: Long,
    val observedUnit: String,
    val description: String,
)
