package com.homingsms.core.telephony

import com.homingsms.core.model.ErrorCodes
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SmsResultsTest {
    @Test
    fun mapsResultCodesToContractErrorCodes() {
        assertEquals(ErrorCodes.GENERIC_FAILURE, SmsResults.errorCode(SmsResults.RESULT_ERROR_GENERIC_FAILURE))
        assertEquals(ErrorCodes.RADIO_OFF, SmsResults.errorCode(SmsResults.RESULT_ERROR_RADIO_OFF))
        assertEquals(ErrorCodes.NULL_PDU, SmsResults.errorCode(SmsResults.RESULT_ERROR_NULL_PDU))
        assertEquals(ErrorCodes.NO_SERVICE, SmsResults.errorCode(SmsResults.RESULT_ERROR_NO_SERVICE))
        assertEquals(ErrorCodes.SMS_LIMIT_REACHED, SmsResults.errorCode(SmsResults.RESULT_ERROR_LIMIT_EXCEEDED))
        assertEquals(ErrorCodes.SHORT_CODE_NOT_ALLOWED, SmsResults.errorCode(SmsResults.RESULT_ERROR_SHORT_CODE_NEVER_ALLOWED))
        assertEquals(ErrorCodes.GENERIC_FAILURE, SmsResults.errorCode(999))
    }

    @Test
    fun detectsTheOutgoingLimit() {
        assertTrue(SmsResults.isLimitHit(SmsResults.RESULT_ERROR_LIMIT_EXCEEDED))
        assertTrue(SmsResults.isLimitHit(SmsResults.RESULT_UNEXPECTED_EVENT_STOP_SENDING))
        assertFalse(SmsResults.isLimitHit(SmsResults.RESULT_ERROR_NO_SERVICE))
    }

    @Test
    fun errorStringsCarryTheCode() {
        val decoded = ErrorCodes.decode(SmsResults.error(SmsResults.RESULT_ERROR_NO_SERVICE))
        assertEquals(ErrorCodes.NO_SERVICE, decoded?.code)
        assertEquals("Send result: RESULT_ERROR_NO_SERVICE (4)", decoded?.message)
    }

    @Test
    fun limitDetectorFlagsAStuckPromptAtTheLimit() {
        val now = 10_000_000L
        val stuck = SmsLimitDetector.evaluate(30, 1_800_000, 30, null, now - 60_000, now)
        assertEquals(now, stuck.limitHitAt)
        assertTrue(stuck.nearLimit)

        val fresh = SmsLimitDetector.evaluate(30, 1_800_000, 30, null, now - 1_000, now)
        assertNull(fresh.limitHitAt)
        assertTrue(fresh.nearLimit)

        val below = SmsLimitDetector.evaluate(30, 1_800_000, 5, null, now - 600_000, now)
        assertNull(below.limitHitAt)
        assertFalse(below.nearLimit)

        val remembered = SmsLimitDetector.evaluate(30, 1_800_000, 5, now - 1, null, now)
        assertNotNull(remembered.limitHitAt)
    }
}
