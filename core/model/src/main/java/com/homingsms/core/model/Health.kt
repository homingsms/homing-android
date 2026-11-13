package com.homingsms.core.model

data class BatteryInfo(val level: Int, val charging: Boolean, val powerSaver: Boolean)

data class SimHealth(
    val slot: Int,
    val carrier: String?,
    val phoneNumber: String?,
    val signalLevel: Int?,
    val sentToday: Int,
    val inService: Boolean = true
)

data class DeviceHealth(
    val battery: BatteryInfo,
    val network: NetworkType,
    val sims: List<SimHealth>,
    val appVersion: String,
    val airplaneMode: Boolean,
    val localQueueSize: Int,
    val collectedAt: Long
) {
    companion object {
        val Unknown = DeviceHealth(
            battery = BatteryInfo(level = 0, charging = false, powerSaver = false),
            network = NetworkType.Unknown,
            sims = emptyList(),
            appVersion = "",
            airplaneMode = false,
            localQueueSize = 0,
            collectedAt = 0
        )
    }
}
