package com.homingsms.core.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PairingLinkTest {
    @Test
    fun parsesTheConsoleQrLink() {
        val link = PairingLink.parse("homing://pair?server=https%3A%2F%2Fapi.homingsms.com&code=K7PM-4XQ2")
        assertEquals(PairingLink("https://api.homingsms.com", "K7PM-4XQ2"), link)
    }

    @Test
    fun acceptsParametersInAnyOrderAndTrailingSlash() {
        val link = PairingLink.parse("homing://pair?code=abcd1234&server=https%3A%2F%2Fcloud.example.com%2F")
        assertEquals(PairingLink("https://cloud.example.com", "abcd1234"), link)
    }

    @Test
    fun rejectsCleartextUnlessAllowed() {
        val raw = "homing://pair?server=http%3A%2F%2F10.0.2.2%3A7010&code=K7PM-4XQ2"
        assertNull(PairingLink.parse(raw))
        assertEquals("http://10.0.2.2:7010", PairingLink.parse(raw, allowCleartext = true)?.serverUrl)
    }

    @Test
    fun rejectsInvalidLinks() {
        assertNull(PairingLink.parse("https://pair?server=https%3A%2F%2Fa.com&code=K7PM-4XQ2"))
        assertNull(PairingLink.parse("homing://other?server=https%3A%2F%2Fa.com&code=K7PM-4XQ2"))
        assertNull(PairingLink.parse("homing://pair?server=https%3A%2F%2Fa.com"))
        assertNull(PairingLink.parse("homing://pair?server=https%3A%2F%2Fa.com&code=x"))
        assertNull(PairingLink.parse("homing://pair?server=ftp%3A%2F%2Fa.com&code=K7PM-4XQ2"))
        assertNull(PairingLink.parse("homing://pair?server=https%3A%2F%2Fa.com&code=K7PM%204XQ2"))
        assertNull(PairingLink.parse("not a uri at all %%%"))
    }

    @Test
    fun normalizesServerUrls() {
        assertEquals("https://a.com", PairingLink.normalizeServerUrl(" https://a.com/ "))
        assertNull(PairingLink.normalizeServerUrl("https://a.com?x=1"))
        assertNull(PairingLink.normalizeServerUrl("a.com"))
    }
}
