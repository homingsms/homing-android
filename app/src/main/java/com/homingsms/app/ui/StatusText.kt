package com.homingsms.app.ui

import com.homingsms.core.model.StatusReason

object StatusText {
    private val fixed: Map<StatusReason, String> = mapOf(
        StatusReason.Stopped to "The service is stopped",
        StatusReason.NoModeEnabled to "Turn on the local server or Homing Cloud",
        StatusReason.NoSim to "No SIM card",
        StatusReason.AirplaneMode to "Airplane mode is on",
        StatusReason.NoNetwork to "No network connection",
        StatusReason.BatterySaver to "Battery saver is on",
        StatusReason.CloudPaused to "Paused from the Homing Cloud console",
        StatusReason.CloudNotPaired to "Not paired with Homing Cloud",
        StatusReason.CloudDeviceRemoved to "This phone was removed from Homing Cloud"
    )

    fun describe(reason: StatusReason): String = fixed[reason] ?: detailed(reason)

    private fun detailed(reason: StatusReason): String = when (reason) {
        is StatusReason.PermissionsMissing -> "Missing permissions: " + reason.permissions.joinToString { it.substringAfterLast('.') }

        is StatusReason.NoSignal -> "No signal on SIM ${reason.simSlot + 1}"

        is StatusReason.LowBattery -> "Battery low (${reason.level}%)"

        is StatusReason.OutgoingSmsLimit ->
            "Android outgoing SMS limit reached (${reason.sentInWindow}/${reason.maxCount}). Fix: ${reason.fixCommand}"

        is StatusReason.SimDailyLimitReached -> "SIM ${reason.simSlot + 1} reached its daily limit of ${reason.limit}"

        is StatusReason.CloudUnreachable -> "Homing Cloud unreachable" + suffix(reason.lastError)

        is StatusReason.LocalServerDown -> "Local server is not running" + suffix(reason.lastError)

        else -> reason.toString()
    }

    private fun suffix(error: String?): String = if (error.isNullOrEmpty()) "" else ": $error"
}
