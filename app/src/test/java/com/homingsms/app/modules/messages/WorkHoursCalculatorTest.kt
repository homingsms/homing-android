package com.homingsms.app.modules.messages

import com.homingsms.core.model.WorkHours
import java.util.Calendar
import java.util.TimeZone
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class WorkHoursCalculatorTest {
    private fun at(hour: Int, minute: Int = 0): Calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
        clear()
        set(2026, Calendar.NOVEMBER, 12, hour, minute)
    }

    @Test
    fun disabledWorkHoursNeverDelay() {
        assertTrue(WorkHoursCalculator.isInWorkHours(WorkHours(enabled = false), at(3)))
        assertNull(WorkHoursCalculator.nextStart(WorkHours(enabled = false), at(3)))
    }

    @Test
    fun dayWindow() {
        val hours = WorkHours(enabled = true, start = "09:00", end = "19:00")
        assertTrue(WorkHoursCalculator.isInWorkHours(hours, at(9)))
        assertFalse(WorkHoursCalculator.isInWorkHours(hours, at(19)))
        assertEquals(at(9).timeInMillis, WorkHoursCalculator.nextStart(hours, at(7, 30)))
        assertEquals(at(9).apply { add(Calendar.DAY_OF_MONTH, 1) }.timeInMillis, WorkHoursCalculator.nextStart(hours, at(20)))
        assertNull(WorkHoursCalculator.nextStart(hours, at(12)))
    }

    @Test
    fun overnightWindow() {
        val hours = WorkHours(enabled = true, start = "22:00", end = "06:00")
        assertTrue(WorkHoursCalculator.isInWorkHours(hours, at(23)))
        assertTrue(WorkHoursCalculator.isInWorkHours(hours, at(2)))
        assertFalse(WorkHoursCalculator.isInWorkHours(hours, at(12)))
        assertEquals(at(22).timeInMillis, WorkHoursCalculator.nextStart(hours, at(12)))
    }

    @Test
    fun parsesMinutes() {
        assertEquals(510, WorkHoursCalculator.parseMinutes("08:30"))
        assertEquals(0, WorkHoursCalculator.parseMinutes("bad"))
    }
}
