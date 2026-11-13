package com.homingsms.core.model

enum class GatewayStatus {
    Online,
    Degraded,
    Offline
}

sealed interface StatusReason {
    data object Stopped : StatusReason

    data object NoModeEnabled : StatusReason

    data class PermissionsMissing(val permissions: List<String>) : StatusReason

    data object NoSim : StatusReason

    data object AirplaneMode : StatusReason

    data object NoNetwork : StatusReason

    data class NoSignal(val simSlot: Int) : StatusReason

    data object BatterySaver : StatusReason

    data class LowBattery(val level: Int) : StatusReason

    data class OutgoingSmsLimit(val sentInWindow: Int, val maxCount: Int, val windowMs: Long, val detectedAt: Long?) : StatusReason {
        val fixCommand: String
            get() = "adb shell settings put global $SETTING_MAX_COUNT $SUGGESTED_MAX_COUNT"

        companion object {
            const val SETTING_MAX_COUNT = "sms_outgoing_check_max_count"
            const val SETTING_INTERVAL = "sms_outgoing_check_interval_ms"
            const val SUGGESTED_MAX_COUNT = 10_000
        }
    }

    data class SimDailyLimitReached(val simSlot: Int, val limit: Int) : StatusReason

    data class CloudUnreachable(val since: Long?, val lastError: String?) : StatusReason

    data object CloudPaused : StatusReason

    data object CloudNotPaired : StatusReason

    data object CloudDeviceRemoved : StatusReason

    data class LocalServerDown(val lastError: String?) : StatusReason
}

data class StatusIssue(val reason: StatusReason, val blocking: Boolean)

data class GatewayState(val status: GatewayStatus, val issues: List<StatusIssue>) {
    val reasons: List<StatusReason>
        get() = issues.map { it.reason }

    companion object {
        val Stopped = GatewayState(GatewayStatus.Offline, listOf(StatusIssue(StatusReason.Stopped, blocking = true)))
    }
}

data class SmsLimitState(val maxCount: Int, val windowMs: Long, val sentInWindow: Int, val limitHitAt: Long?) {
    val nearLimit: Boolean
        get() = maxCount > 0 && sentInWindow >= maxCount

    companion object {
        const val DEFAULT_MAX_COUNT = 30
        const val DEFAULT_WINDOW_MS = 1_800_000L

        val Clear = SmsLimitState(DEFAULT_MAX_COUNT, DEFAULT_WINDOW_MS, 0, null)
    }
}

data class GatewayInputs(
    val running: Boolean,
    val localEnabled: Boolean,
    val localRunning: Boolean,
    val localError: String?,
    val cloudEnabled: Boolean,
    val cloudPairing: CloudPairingState,
    val cloudConnection: CloudConnectionState,
    val health: DeviceHealth,
    val missingPermissions: List<String>,
    val smsLimit: SmsLimitState,
    val sims: List<SimStats>,
    val now: Long
)

object GatewayStatusDeriver {
    const val LOW_BATTERY_LEVEL = 15
    const val CLOUD_FAILURE_THRESHOLD = 2
    const val SMS_LIMIT_RECENT_MS = 3_600_000L
    const val SEND_SMS_PERMISSION = "android.permission.SEND_SMS"

    fun derive(inputs: GatewayInputs): GatewayState {
        if (!inputs.running) return GatewayState.Stopped
        if (!inputs.localEnabled && !inputs.cloudEnabled) {
            return state(listOf(StatusIssue(StatusReason.NoModeEnabled, blocking = true)))
        }
        val issues = buildList {
            addAll(permissionIssues(inputs))
            addAll(radioIssues(inputs))
            addAll(batteryIssues(inputs))
            smsLimitIssue(inputs)?.let(::add)
            addAll(modeIssues(inputs))
        }
        return state(issues)
    }

    private fun state(issues: List<StatusIssue>): GatewayState {
        val status = when {
            issues.any { it.blocking } -> GatewayStatus.Offline
            issues.isNotEmpty() -> GatewayStatus.Degraded
            else -> GatewayStatus.Online
        }
        return GatewayState(status, issues.sortedByDescending { it.blocking })
    }

    private fun permissionIssues(inputs: GatewayInputs): List<StatusIssue> {
        if (inputs.missingPermissions.isEmpty()) return emptyList()
        val blocking = SEND_SMS_PERMISSION in inputs.missingPermissions
        return listOf(StatusIssue(StatusReason.PermissionsMissing(inputs.missingPermissions), blocking))
    }

    private fun radioIssues(inputs: GatewayInputs): List<StatusIssue> {
        val health = inputs.health
        if (health.airplaneMode) return listOf(StatusIssue(StatusReason.AirplaneMode, blocking = true))
        if (health.sims.isEmpty()) return listOf(StatusIssue(StatusReason.NoSim, blocking = true))
        val withoutSignal = health.sims.filter { !it.inService || it.signalLevel == 0 }
        val allDown = withoutSignal.size == health.sims.size
        val signalIssues = withoutSignal.map { StatusIssue(StatusReason.NoSignal(it.slot), blocking = allDown) }
        val limited = inputs.sims.filter { it.enabled && it.limitReached }
        val allLimited = inputs.sims.isNotEmpty() && limited.size == inputs.sims.count { it.enabled }
        val limitIssues = limited.map {
            StatusIssue(StatusReason.SimDailyLimitReached(it.slot, it.dailyLimit ?: 0), blocking = allLimited)
        }
        return signalIssues + limitIssues
    }

    private fun batteryIssues(inputs: GatewayInputs): List<StatusIssue> {
        val battery = inputs.health.battery
        return buildList {
            if (battery.powerSaver) add(StatusIssue(StatusReason.BatterySaver, blocking = false))
            if (!battery.charging && battery.level in 0 until LOW_BATTERY_LEVEL && inputs.health.collectedAt > 0) {
                add(StatusIssue(StatusReason.LowBattery(battery.level), blocking = false))
            }
        }
    }

    private fun smsLimitIssue(inputs: GatewayInputs): StatusIssue? {
        val limit = inputs.smsLimit
        val recentlyHit = limit.limitHitAt?.let { inputs.now - it < SMS_LIMIT_RECENT_MS } ?: false
        if (!recentlyHit && !limit.nearLimit) return null
        return StatusIssue(
            StatusReason.OutgoingSmsLimit(limit.sentInWindow, limit.maxCount, limit.windowMs, limit.limitHitAt),
            blocking = false
        )
    }

    private fun modeIssues(inputs: GatewayInputs): List<StatusIssue> {
        val local = localIssue(inputs)
        val cloud = cloudIssue(inputs)
        val localHealthy = inputs.localEnabled && local == null
        val cloudHealthy = inputs.cloudEnabled && cloud == null
        val anyHealthy = localHealthy || cloudHealthy
        val networkIssue = if (inputs.health.network == NetworkType.None) {
            StatusIssue(StatusReason.NoNetwork, blocking = true)
        } else {
            null
        }
        return listOfNotNull(
            networkIssue,
            local?.let { StatusIssue(it, blocking = !anyHealthy) },
            cloud?.let { StatusIssue(it, blocking = !anyHealthy && it !is StatusReason.CloudPaused) }
        )
    }

    private fun localIssue(inputs: GatewayInputs): StatusReason? = when {
        !inputs.localEnabled -> null
        !inputs.localRunning -> StatusReason.LocalServerDown(inputs.localError)
        else -> null
    }

    private fun cloudIssue(inputs: GatewayInputs): StatusReason? {
        if (!inputs.cloudEnabled) return null
        val connection = inputs.cloudConnection
        return when (inputs.cloudPairing) {
            is CloudPairingState.Removed -> StatusReason.CloudDeviceRemoved

            !is CloudPairingState.Paired -> StatusReason.CloudNotPaired

            else -> when {
                connection.consecutiveFailures >= CLOUD_FAILURE_THRESHOLD ->
                    StatusReason.CloudUnreachable(connection.lastSuccessAt, connection.lastError)

                connection.remotePaused -> StatusReason.CloudPaused

                else -> null
            }
        }
    }
}
