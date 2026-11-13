package com.homingsms.core.model

data class TodayCounts(val sent: Int, val delivered: Int, val failed: Int, val received: Int, val queued: Int) {
    companion object {
        val Empty = TodayCounts(0, 0, 0, 0, 0)
    }
}

data class HourlyBucket(val startAt: Long, val sent: Int, val failed: Int, val received: Int, val delivered: Int = 0)

data class SimStats(
    val slot: Int,
    val carrier: String?,
    val phoneNumber: String?,
    val signalLevel: Int?,
    val sentToday: Int,
    val dailyLimit: Int?,
    val enabled: Boolean
) {
    val remainingToday: Int?
        get() = dailyLimit?.let { (it - sentToday).coerceAtLeast(0) }

    val limitReached: Boolean
        get() = dailyLimit != null && sentToday >= dailyLimit
}

object TimeBuckets {
    const val HOUR_MS = 3_600_000L
    const val DAY_MS = 86_400_000L
    const val HOURS_PER_DAY = 24

    fun hourStart(timestamp: Long): Long = timestamp - Math.floorMod(timestamp, HOUR_MS)

    fun utcDayStart(timestamp: Long): Long = timestamp - Math.floorMod(timestamp, DAY_MS)

    fun last24Hours(now: Long): List<Long> {
        val current = hourStart(now)
        return (HOURS_PER_DAY - 1 downTo 0).map { current - it * HOUR_MS }
    }

    fun fill(now: Long, buckets: Map<Long, HourlyBucket>): List<HourlyBucket> = last24Hours(now).map { start ->
        buckets[start] ?: HourlyBucket(start, 0, 0, 0)
    }
}
