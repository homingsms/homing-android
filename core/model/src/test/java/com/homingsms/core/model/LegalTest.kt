package com.homingsms.core.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LegalTest {
    @Test
    fun defaultsHaveNotAcceptedAnything() {
        val legal = AppSettings().legal
        assertNull(legal.acceptedTermsVersion)
        assertNull(legal.acceptedAt)
        assertFalse(legal.acceptedCurrentTerms)
    }

    @Test
    fun acceptingStoresTheCurrentVersionAndTime() {
        val accepted = LegalSettings().accept(1_700_000_000_000L)
        assertEquals(LegalTerms.CURRENT_VERSION, accepted.acceptedTermsVersion)
        assertEquals(1_700_000_000_000L, accepted.acceptedAt)
        assertTrue(accepted.acceptedCurrentTerms)
    }

    @Test
    fun anOlderVersionIsNotCurrent() {
        assertFalse(LegalSettings("2025-01-01", 1L).acceptedCurrentTerms)
        assertEquals("2026-11-14", LegalTerms.CURRENT_VERSION)
    }

    @Test
    fun documentUrlsJoinTheBaseUrl() {
        assertEquals("https://homingsms.com/legal/terms", LegalDocument.Terms.url("https://homingsms.com"))
        assertEquals("https://homingsms.com/legal/privacy", LegalDocument.Privacy.url("https://homingsms.com/"))
        assertEquals("https://example.test/legal/acceptable-use", LegalDocument.AcceptableUse.url("https://example.test"))
    }
}
