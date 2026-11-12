package com.homingsms.app.serialization

import java.util.Date
import java.util.GregorianCalendar
import java.util.TimeZone
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

class DateSerializerTest {
    private lateinit var defaultTimeZone: TimeZone

    @Before
    fun pinTimeZone() {
        defaultTimeZone = TimeZone.getDefault()
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"))
    }

    @After
    fun restoreTimeZone() {
        TimeZone.setDefault(defaultTimeZone)
    }

    @Test
    fun formatsWithMillisAndOffset() {
        assertEquals("1970-01-01T00:00:10.000Z", DateSerializer.format(Date(10_000)))
    }

    @Test
    fun formatsInDefaultTimeZone() {
        TimeZone.setDefault(TimeZone.getTimeZone("GMT+03:00"))
        assertEquals("1970-01-01T03:00:00.000+03:00", DateSerializer.format(Date(0)))
    }

    @Test
    fun parsesCanonicalFormat() {
        assertEquals(Date(10_000), DateSerializer.parse("1970-01-01T00:00:10.000Z"))
        assertEquals(Date(0), DateSerializer.parse("1970-01-01T03:00:00.000+03:00"))
    }

    @Test
    fun parsesIsoWithoutMillis() {
        assertEquals(Date(1_704_164_645_000), DateSerializer.parse("2024-01-02T03:04:05Z"))
        assertEquals(Date(1_704_161_045_000), DateSerializer.parse("2024-01-02T03:04:05+01:00"))
        assertEquals(Date(1_704_161_045_000), DateSerializer.parse("2024-01-02T03:04:05+0100"))
    }

    @Test
    fun parsesIsoWithoutSeconds() {
        assertEquals(Date(1_704_164_640_000), DateSerializer.parse("2024-01-02T03:04Z"))
    }

    @Test
    fun parsesDateOnlyAsLocalMidnight() {
        assertEquals(GregorianCalendar(2024, 0, 2).time, DateSerializer.parse("2024-01-02"))
    }

    @Test
    fun rejectsInvalidValues() {
        assertNull(DateSerializer.parse("not a date"))
        assertNull(DateSerializer.parse("2024-01-02T03:04:05"))
        assertNull(DateSerializer.parse("2024-13-40T03:04:05Z"))
    }

    @Test
    fun roundTripsThroughJson() {
        val json = AppJson.default.encodeToString(DateSerializer, Date(1_704_164_645_123))
        assertEquals("\"2024-01-02T03:04:05.123Z\"", json)
        assertEquals(Date(1_704_164_645_123), AppJson.default.decodeFromString(DateSerializer, json))
    }
}
