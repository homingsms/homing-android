package com.homingsms.core.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ModelBasicsTest {
    @Test
    fun adminGrantsEveryScope() {
        Scope.entries.forEach { assertTrue(setOf(Scope.Admin).grants(it)) }
    }

    @Test
    fun scopeGrantsOnlyItself() {
        val scopes = setOf(Scope.MessagesSend)
        assertTrue(scopes.grants(Scope.MessagesSend))
        assertFalse(scopes.grants(Scope.MessagesRead))
        assertFalse(scopes.grants(Scope.Admin))
    }

    @Test
    fun scopeValuesMatchContract() {
        assertEquals(
            listOf("messages:send", "messages:read", "inbox:read", "webhooks:manage", "devices:manage", "admin"),
            Scope.entries.map { it.value }
        )
        assertEquals(Scope.InboxRead, Scope.fromValue("inbox:read"))
        assertNull(Scope.fromValue("inbox:list"))
    }

    @Test
    fun cursorRoundTrips() {
        val cursor = Cursor(1_731_400_000_123, "msg_abc:def")
        assertEquals(cursor, Cursor.decode(cursor.encode()))
    }

    @Test
    fun invalidCursorsAreRejected() {
        assertNull(Cursor.decode("not base64 !!"))
        assertNull(Cursor.decode(Cursor(1, "x").encode().drop(2)))
        assertNull(Cursor.decode(""))
    }

    @Test
    fun errorCodesRoundTrip() {
        val encoded = ErrorCodes.encode(ErrorCodes.NO_SERVICE, "Send result: RESULT_ERROR_NO_SERVICE (4)")
        assertEquals(MessageError("no_service", "Send result: RESULT_ERROR_NO_SERVICE (4)"), ErrorCodes.decode(encoded))
    }

    @Test
    fun legacyErrorsDecodeAsGenericFailure() {
        assertEquals(MessageError(ErrorCodes.GENERIC_FAILURE, "TTL expired"), ErrorCodes.decode("TTL expired"))
        assertNull(ErrorCodes.decode(null))
        assertNull(ErrorCodes.decode(" "))
    }

    @Test
    fun idsUsePrefixes() {
        assertTrue(Ids.message().matches(Regex("^msg_[0-9A-Za-z]{20}$")))
        assertTrue(Ids.inbox().startsWith("inb_"))
        assertTrue(Ids.webhook().startsWith("whk_"))
        assertTrue(Ids.delivery().startsWith("whd_"))
        assertTrue(Ids.event().startsWith("evt_"))
        assertTrue(Ids.apiKey().startsWith("key_"))
    }

    @Test
    fun hourBucketsCoverTheLastDay() {
        val now = 1_731_400_000_000L + 1_234_567
        val hours = TimeBuckets.last24Hours(now)
        assertEquals(24, hours.size)
        assertEquals(TimeBuckets.hourStart(now), hours.last())
        assertEquals(TimeBuckets.HOUR_MS, hours[1] - hours[0])
        val filled = TimeBuckets.fill(now, mapOf(hours[3] to HourlyBucket(hours[3], 2, 1, 4)))
        assertEquals(2, filled[3].sent)
        assertEquals(0, filled[4].sent)
    }

    @Test
    fun simStatsReportsRemainingQuota() {
        val stats = SimStats(0, "Carrier", null, 3, sentToday = 98, dailyLimit = 100, enabled = true)
        assertEquals(2, stats.remainingToday)
        assertFalse(stats.limitReached)
        assertTrue(stats.copy(sentToday = 100).limitReached)
        assertNull(stats.copy(dailyLimit = null).remainingToday)
    }

    @Test
    fun dailyLimitPrefersRemoteSimSettings() {
        val settings = AppSettings(
            messages = MessageSettings(simDailyLimits = mapOf(0 to 50, 1 to 60)),
            cloud = CloudSettings(
                enabled = true,
                remote = RemoteDeviceSettings(dailyLimit = 500, sims = listOf(RemoteSimSettings(0, enabled = false, dailyLimit = 200)))
            )
        )
        assertEquals(200, settings.dailyLimitFor(0))
        assertEquals(60, settings.dailyLimitFor(1))
        assertEquals(500, settings.dailyLimitFor(2))
        assertFalse(settings.simEnabled(0))
        assertTrue(settings.simEnabled(1))
        assertEquals(50, settings.copy(cloud = CloudSettings()).dailyLimitFor(0))
    }
}
