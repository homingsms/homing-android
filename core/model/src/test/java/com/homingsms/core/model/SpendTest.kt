package com.homingsms.core.model

import java.util.Locale
import java.util.TimeZone
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SpendTest {
    private val utc = TimeZone.getTimeZone("UTC")

    @Test
    fun costMultipliesPartsByMicrosAndSaturates() {
        assertEquals(22_500L, SpendMath.cost(3, 7_500))
        assertEquals(0L, SpendMath.cost(0, 7_500))
        assertEquals(0L, SpendMath.cost(12, 0))
        assertEquals(Long.MAX_VALUE, SpendMath.cost(Long.MAX_VALUE / 2, SpendMath.MAX_RATE_MICROS))
    }

    @Test
    fun estimatePricesEachSimAtItsOwnRate() {
        val usage = SmsPartsUsage(month = mapOf(0 to 100L, 1 to 40L), allTime = mapOf(0 to 1_000L, 1 to 400L))
        val settings = SpendSettings("USD", mapOf(0 to 7_500L, 1 to 20_000L))

        val estimate = SpendMath.estimate(usage, settings, listOf(1, 0), "USD")

        assertTrue(estimate.hasRate)
        assertEquals(140L, estimate.monthParts)
        assertEquals(1_400L, estimate.allTimeParts)
        assertEquals(100 * 7_500L + 40 * 20_000L, estimate.monthMicros)
        assertEquals(1_000 * 7_500L + 400 * 20_000L, estimate.allTimeMicros)
        assertEquals(0L, estimate.unpricedMonthParts)
        assertEquals(listOf(0, 1), estimate.sims.map { it.slot })
        assertEquals(750_000L, estimate.sims[0].monthMicros)
        assertEquals(8_000_000L, estimate.sims[1].allTimeMicros)
    }

    @Test
    fun simWithoutRateIsUnpricedNotFree() {
        val usage = SmsPartsUsage(month = mapOf(0 to 10L, 1 to 5L), allTime = mapOf(0 to 10L, 1 to 5L))
        val estimate = SpendMath.estimate(usage, SpendSettings(simRatesMicros = mapOf(0 to 1_000_000L)), listOf(0, 1), "EUR")

        assertEquals(10_000_000L, estimate.monthMicros)
        assertEquals(5L, estimate.unpricedMonthParts)
        assertNull(estimate.sims[1].monthMicros)
        assertEquals(5L, estimate.sims[1].monthParts)
    }

    @Test
    fun unknownSimUsesTheOnlyRateThatCouldApply() {
        val usage = SmsPartsUsage(month = mapOf(null to 4L), allTime = mapOf(null to 4L, 0 to 6L))
        val single = SpendMath.estimate(usage, SpendSettings(simRatesMicros = mapOf(0 to 50_000L)), listOf(0), "GBP")
        assertEquals(200_000L, single.monthMicros)
        assertEquals(500_000L, single.allTimeMicros)

        val sameRates = SpendMath.estimate(usage, SpendSettings(simRatesMicros = mapOf(0 to 50_000L, 1 to 50_000L)), listOf(0, 1), "GBP")
        assertEquals(200_000L, sameRates.monthMicros)

        val differentRates = SpendMath.estimate(
            usage,
            SpendSettings(simRatesMicros = mapOf(0 to 50_000L, 1 to 70_000L)),
            listOf(0, 1),
            "GBP"
        )
        assertEquals(0L, differentRates.monthMicros)
        assertEquals(4L, differentRates.unpricedMonthParts)

        val noSimInfo = SpendMath.estimate(usage, SpendSettings(simRatesMicros = mapOf(1 to 10L)), emptyList(), "GBP")
        assertEquals(40L, noSimInfo.monthMicros)
    }

    @Test
    fun zeroRateIsARealRate() {
        val usage = SmsPartsUsage(month = mapOf(0 to 9L), allTime = mapOf(0 to 9L))
        val estimate = SpendMath.estimate(usage, SpendSettings(simRatesMicros = mapOf(0 to 0L)), listOf(0), "USD")
        assertTrue(estimate.hasRate)
        assertEquals(0L, estimate.monthMicros)
        assertEquals(0L, estimate.unpricedMonthParts)
    }

    @Test
    fun noRateMeansNoEstimate() {
        val usage = SmsPartsUsage(month = mapOf(0 to 3L), allTime = mapOf(0 to 8L))
        val estimate = SpendMath.estimate(usage, SpendSettings(), listOf(0), "USD")
        assertFalse(estimate.hasRate)
        assertEquals(3L, estimate.monthParts)
        assertEquals(8L, estimate.allTimeParts)
        assertEquals(3L, estimate.unpricedMonthParts)
    }

    @Test
    fun currencyDefaultsToDeviceLocaleAndRejectsBadCodes() {
        assertEquals("NGN", SpendSettings().effectiveCurrency(Locale("en", "NG")))
        assertEquals("EUR", SpendSettings().effectiveCurrency(Locale.GERMANY))
        assertEquals("USD", SpendSettings().effectiveCurrency(Locale("en")))
        assertEquals("GBP", SpendSettings("GBP").effectiveCurrency(Locale.GERMANY))
        assertEquals("EUR", SpendSettings("XYZ").effectiveCurrency(Locale.GERMANY))
        assertTrue(SpendMath.isCurrencyCode("JPY"))
        assertFalse(SpendMath.isCurrencyCode("usd"))
        assertFalse(SpendMath.isCurrencyCode("DOLLAR"))
    }

    @Test
    fun withRateSetsClearsAndRejectsOutOfRangeValues() {
        val settings = SpendSettings().withRate(0, 7_500L).withRate(1, 0L)
        assertEquals(mapOf(0 to 7_500L, 1 to 0L), settings.simRatesMicros)
        assertEquals(mapOf(1 to 0L), settings.withRate(0, null).simRatesMicros)
        assertEquals(mapOf(1 to 0L), settings.withRate(0, -1L).simRatesMicros)
        assertEquals(mapOf(1 to 0L), settings.withRate(0, SpendMath.MAX_RATE_MICROS + 1).simRatesMicros)
    }

    @Test
    fun monthStartFollowsTheLocalCalendar() {
        val march15 = 1_773_576_000_000L
        assertEquals(1_772_323_200_000L, TimeBuckets.localMonthStart(march15, utc))
        assertEquals(1_775_001_600_000L, TimeBuckets.nextLocalMonthStart(march15, utc))

        val lagos = TimeZone.getTimeZone("Africa/Lagos")
        val lastMinuteOfFebruaryUtc = 1_772_323_140_000L
        assertEquals(1_772_319_600_000L, TimeBuckets.localMonthStart(lastMinuteOfFebruaryUtc, lagos))
        assertEquals(1_769_900_400_000L, TimeBuckets.localMonthStart(lastMinuteOfFebruaryUtc - 3_600_000L, lagos))
    }

    @Test
    fun monthBoundaryRollsOverTheYear() {
        val newYork = TimeZone.getTimeZone("America/New_York")
        val dec31Evening = 1_798_768_800_000L
        val janFirstLocal = 1_798_779_600_000L
        assertEquals(janFirstLocal, TimeBuckets.nextLocalMonthStart(dec31Evening, newYork))
        assertEquals(janFirstLocal, TimeBuckets.localMonthStart(janFirstLocal, newYork))
        assertEquals(janFirstLocal, TimeBuckets.localMonthStart(janFirstLocal + 1, newYork))
    }
}
