package com.homingsms.core.model

import java.util.Currency
import java.util.Locale

data class SmsPartsUsage(val month: Map<Int?, Long> = emptyMap(), val allTime: Map<Int?, Long> = emptyMap()) {
    val monthTotal: Long
        get() = month.values.sum()

    val allTimeTotal: Long
        get() = allTime.values.sum()

    companion object {
        val Empty = SmsPartsUsage()
    }
}

data class SimSpendEstimate(val slot: Int, val rateMicros: Long?, val monthParts: Long, val allTimeParts: Long) {
    val monthMicros: Long?
        get() = rateMicros?.let { SpendMath.cost(monthParts, it) }

    val allTimeMicros: Long?
        get() = rateMicros?.let { SpendMath.cost(allTimeParts, it) }
}

data class SpendEstimate(
    val currencyCode: String,
    val hasRate: Boolean,
    val monthParts: Long,
    val allTimeParts: Long,
    val monthMicros: Long,
    val allTimeMicros: Long,
    val unpricedMonthParts: Long,
    val sims: List<SimSpendEstimate>
)

object SpendMath {
    const val MICROS_PER_UNIT = 1_000_000L
    const val RATE_DECIMALS = 6
    const val MAX_RATE_UNITS = 100_000L
    const val MAX_RATE_MICROS = MAX_RATE_UNITS * MICROS_PER_UNIT
    const val FALLBACK_CURRENCY = "USD"

    fun cost(parts: Long, rateMicros: Long): Long = try {
        Math.multiplyExact(parts.coerceAtLeast(0), rateMicros.coerceAtLeast(0))
    } catch (_: ArithmeticException) {
        Long.MAX_VALUE
    }

    fun estimate(usage: SmsPartsUsage, settings: SpendSettings, slots: List<Int>, currencyCode: String): SpendEstimate {
        val rates = settings.simRatesMicros
        val unknownSlotRate = (if (slots.isEmpty()) rates.values.toList() else slots.map { rates[it] }).distinct().singleOrNull()
        val rateFor: (Int?) -> Long? = { slot -> if (slot == null) unknownSlotRate else rates[slot] }
        val month = price(usage.month, rateFor)
        val allTime = price(usage.allTime, rateFor)
        return SpendEstimate(
            currencyCode = currencyCode,
            hasRate = settings.hasRate,
            monthParts = usage.monthTotal,
            allTimeParts = usage.allTimeTotal,
            monthMicros = month.micros,
            allTimeMicros = allTime.micros,
            unpricedMonthParts = month.unpricedParts,
            sims = slots.distinct().sorted().map { slot ->
                SimSpendEstimate(slot, rates[slot], usage.month[slot] ?: 0L, usage.allTime[slot] ?: 0L)
            }
        )
    }

    fun currencyFor(locale: Locale): String = try {
        Currency.getInstance(locale)?.currencyCode ?: FALLBACK_CURRENCY
    } catch (_: IllegalArgumentException) {
        FALLBACK_CURRENCY
    }

    fun isCurrencyCode(code: String): Boolean = code.length == CODE_LENGTH &&
        try {
            Currency.getInstance(code).currencyCode == code
        } catch (_: IllegalArgumentException) {
            false
        }

    private fun price(parts: Map<Int?, Long>, rateFor: (Int?) -> Long?): Priced {
        var micros = 0L
        var unpriced = 0L
        parts.forEach { (slot, count) ->
            val rate = rateFor(slot)
            if (rate == null) {
                unpriced += count
            } else {
                micros = saturatingAdd(micros, cost(count, rate))
            }
        }
        return Priced(micros, unpriced)
    }

    private fun saturatingAdd(a: Long, b: Long): Long = try {
        Math.addExact(a, b)
    } catch (_: ArithmeticException) {
        Long.MAX_VALUE
    }

    private data class Priced(val micros: Long, val unpricedParts: Long)

    private const val CODE_LENGTH = 3
}
