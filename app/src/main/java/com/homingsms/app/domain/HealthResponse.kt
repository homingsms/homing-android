package com.homingsms.app.domain

import com.homingsms.app.BuildConfig
import com.homingsms.app.modules.health.domain.CheckResult
import com.homingsms.app.modules.health.domain.HealthResult
import com.homingsms.app.modules.health.domain.Status
import kotlinx.serialization.Serializable

@Serializable
class HealthResponse(
    val status: Status,
    val checks: Map<String, CheckResult>,
    val version: String = BuildConfig.VERSION_NAME,
    val releaseId: Int = BuildConfig.VERSION_CODE,
) {
    constructor(healthResult: HealthResult) : this(
        status = healthResult.status,
        checks = healthResult.checks,
    )
}
