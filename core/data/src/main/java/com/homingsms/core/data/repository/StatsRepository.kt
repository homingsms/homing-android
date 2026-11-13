package com.homingsms.core.data.repository

import com.homingsms.core.data.db.SimPartsRow
import com.homingsms.core.data.db.StatsDao
import com.homingsms.core.model.HourlyBucket
import com.homingsms.core.model.SmsPartsUsage
import com.homingsms.core.model.TimeBuckets
import com.homingsms.core.model.TodayCounts
import java.util.Calendar
import java.util.TimeZone
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map

@OptIn(ExperimentalCoroutinesApi::class)
class StatsRepository(
    private val dao: StatsDao,
    private val clock: () -> Long = System::currentTimeMillis,
    private val zone: () -> TimeZone = TimeZone::getDefault
) {
    private val hourTicks: Flow<Long> = flow {
        while (true) {
            val now = clock()
            emit(now)
            delay(TimeBuckets.HOUR_MS - Math.floorMod(now, TimeBuckets.HOUR_MS) + 1)
        }
    }

    private val monthStarts: Flow<Long> = flow {
        while (true) {
            val now = clock()
            val start = TimeBuckets.localMonthStart(now, zone())
            emit(start)
            val untilNext = TimeBuckets.nextLocalMonthStart(start, zone()) - now
            delay(untilNext.coerceIn(1L, TimeBuckets.HOUR_MS))
        }
    }.distinctUntilChanged()

    val smsParts: Flow<SmsPartsUsage> = monthStarts.flatMapLatest { from ->
        combine(dao.observeSentParts(from), dao.observeSentParts(ALL_TIME)) { month, allTime ->
            SmsPartsUsage(month.bySlot(), allTime.bySlot())
        }
    }

    val today: Flow<TodayCounts> = hourTicks.map { localDayStart(it) }.distinctUntilChanged().flatMapLatest { from ->
        combine(dao.observeCounts(from), dao.observeQueued(), dao.observeReceived(from)) { counts, queued, received ->
            TodayCounts(counts.sent, counts.delivered, counts.failed, received, queued)
        }
    }

    val last24Hours: Flow<List<HourlyBucket>> = hourTicks.flatMapLatest { now ->
        val from = TimeBuckets.last24Hours(now).first()
        combine(dao.observeHourly(from), dao.observeHourlyReceived(from)) { sent, received ->
            val receivedByHour = received.associate { it.startAt to it.received }
            val buckets = (sent.map { it.startAt } + received.map { it.startAt }).distinct().associateWith { start ->
                val row = sent.firstOrNull { it.startAt == start }
                HourlyBucket(start, row?.sent ?: 0, row?.failed ?: 0, receivedByHour[start] ?: 0, row?.delivered ?: 0)
            }
            TimeBuckets.fill(now, buckets)
        }
    }

    val sentTodayPerSlot: Flow<Map<Int, Int>> = hourTicks.map { TimeBuckets.utcDayStart(it) }.distinctUntilChanged()
        .flatMapLatest { from -> dao.observeSentPerSim(from).map { rows -> rows.associate { it.simNumber - 1 to it.count } } }

    suspend fun sentToday(): Map<Int, Int> = dao.sentPerSim(TimeBuckets.utcDayStart(clock())).associate { it.simNumber - 1 to it.count }

    suspend fun queued(): Int = dao.queued()

    private fun List<SimPartsRow>.bySlot(): Map<Int?, Long> = groupBy { row -> row.simNumber?.let { it - 1 } }
        .mapValues { (_, rows) -> rows.sumOf { it.parts } }

    companion object {
        private const val ALL_TIME = 0L

        fun localDayStart(timestamp: Long): Long = Calendar.getInstance().apply {
            timeInMillis = timestamp
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }
}
