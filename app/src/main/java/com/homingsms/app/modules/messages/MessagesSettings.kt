package com.homingsms.app.modules.messages

import com.homingsms.core.data.settings.SettingsRepository
import com.homingsms.core.model.LimitPeriod
import com.homingsms.core.model.MessageSettings
import com.homingsms.core.model.ProcessingOrder
import com.homingsms.core.model.SimSelectionMode
import com.homingsms.core.model.WorkHours
import java.util.Calendar

class MessagesSettings(private val repository: SettingsRepository) {
    private val current: MessageSettings
        get() = repository.current.messages

    val sendIntervalRange: IntRange?
        get() {
            val min = current.sendIntervalMinSeconds
            val max = current.sendIntervalMaxSeconds ?: return null
            return if (min > max) null else min..max
        }

    val limitEnabled: Boolean
        get() = limitValue > 0 && limitPeriod != LimitPeriod.Disabled

    val limitPeriod: LimitPeriod
        get() = current.limitPeriod

    val limitValue: Int
        get() = current.limitValue

    val simSelectionMode: SimSelectionMode
        get() = current.simSelection

    val logLifetimeDays: Int?
        get() = current.logLifetimeDays?.takeIf { it > 0 }

    val processingOrder: ProcessingOrder
        get() = current.processingOrder

    val deliveryReports: Boolean
        get() = current.deliveryReports

    val failoverToOtherSim: Boolean
        get() = current.failoverToOtherSim

    fun nextWorkHoursStart(now: Calendar = Calendar.getInstance()): Long? = WorkHoursCalculator.nextStart(current.workHours, now)
}

object WorkHoursCalculator {
    private const val MINUTES_PER_HOUR = 60

    fun isInWorkHours(workHours: WorkHours, now: Calendar): Boolean {
        if (!workHours.enabled) return true
        val nowMinutes = now.get(Calendar.HOUR_OF_DAY) * MINUTES_PER_HOUR + now.get(Calendar.MINUTE)
        val start = parseMinutes(workHours.start)
        val end = parseMinutes(workHours.end)
        return when {
            start == end -> true
            start < end -> nowMinutes in start until end
            else -> nowMinutes >= start || nowMinutes < end
        }
    }

    fun nextStart(workHours: WorkHours, now: Calendar): Long? {
        if (!workHours.enabled || isInWorkHours(workHours, now)) return null
        val nowMinutes = now.get(Calendar.HOUR_OF_DAY) * MINUTES_PER_HOUR + now.get(Calendar.MINUTE)
        val start = parseMinutes(workHours.start)
        val end = parseMinutes(workHours.end)
        val todayStart = (now.clone() as Calendar).apply {
            set(Calendar.HOUR_OF_DAY, start / MINUTES_PER_HOUR)
            set(Calendar.MINUTE, start % MINUTES_PER_HOUR)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        if (start <= end && nowMinutes >= start) todayStart.add(Calendar.DAY_OF_MONTH, 1)
        return todayStart.timeInMillis
    }

    fun parseMinutes(value: String): Int {
        val parts = value.split(":")
        val hours = parts.getOrNull(0)?.toIntOrNull() ?: return 0
        return hours * MINUTES_PER_HOUR + (parts.getOrNull(1)?.toIntOrNull() ?: 0)
    }
}
