package com.homingsms.app.modules.localserver.domain

import com.homingsms.app.serialization.AppJson
import java.util.Date
import java.util.TimeZone
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

/**
 * Pins the wire contract of the local server's device listing, which is what
 * `GET /devices` and `GET /device` must stay compatible with: the field names,
 * their values, and the omission of the key fields when the device has no E2E
 * key.
 *
 * JSON key order is not asserted, deliberately: objects are unordered per
 * RFC 8259. Comparisons are therefore structural.
 *
 * The serializer is the production configuration ([AppJson.default]), which
 * omits null values so the nullable key fields are left out when unset.
 */
class DeviceTest {

    private lateinit var json: Json
    private lateinit var defaultTimeZone: TimeZone

    @Before
    fun pinTimeZone() {
        defaultTimeZone = TimeZone.getDefault()
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"))
        json = AppJson.default
    }

    @After
    fun restoreTimeZone() {
        TimeZone.setDefault(defaultTimeZone)
    }

    private fun assertJsonEquals(expected: String, actual: String) {
        assertEquals(
            Json.parseToJsonElement(expected),
            Json.parseToJsonElement(actual)
        )
    }

    private fun device(
        publicKey: String? = null,
        keyVersion: Int? = null,
        simCards: List<SimCard> = emptyList(),
    ) = Device(
        id = "PyDmBQZZXYmyxMwED8Fzy",
        name = "Google/sargo",
        createdAt = Date(0),
        updatedAt = Date(10_000),
        lastSeen = Date(20_000),
        simCards = simCards,
        publicKey = publicKey,
        keyVersion = keyVersion,
    )

    @Test
    fun serializesKeyFields() {
        val encoded = json.encodeToString(
            Device.serializer(),
            device(
                publicKey = "MIIBIjANBgkqh",
                keyVersion = 1,
                simCards = listOf(
                    SimCard(0, 1, "+79990001234", "MTS", "897010112233445")
                ),
            )
        )

        assertJsonEquals(
            """{
                "publicKey": "MIIBIjANBgkqh",
                "keyVersion": 1,
                "id": "PyDmBQZZXYmyxMwED8Fzy",
                "name": "Google/sargo",
                "createdAt": "1970-01-01T00:00:00.000Z",
                "updatedAt": "1970-01-01T00:00:10.000Z",
                "lastSeen": "1970-01-01T00:00:20.000Z",
                "simCards": [{
                    "slotIndex": 0,
                    "simNumber": 1,
                    "phoneNumber": "+79990001234",
                    "carrierName": "MTS",
                    "iccid": "897010112233445"
                }]
            }""",
            encoded
        )
    }

    @Test
    fun omitsKeyFieldsWhenDeviceHasNoKey() {
        val encoded = json.encodeToString(Device.serializer(), device())

        assertJsonEquals(
            """{
                "id": "PyDmBQZZXYmyxMwED8Fzy",
                "name": "Google/sargo",
                "createdAt": "1970-01-01T00:00:00.000Z",
                "updatedAt": "1970-01-01T00:00:10.000Z",
                "lastSeen": "1970-01-01T00:00:20.000Z",
                "simCards": []
            }""",
            encoded
        )
    }

    @Test
    fun neverEmitsDeletedAt() {
        val encoded = json.encodeToString(
            Device.serializer(),
            device(publicKey = "MIIBIjANBgkqh", keyVersion = 2),
        )

        assertEquals(
            setOf(
                "publicKey", "keyVersion", "id", "name",
                "createdAt", "updatedAt", "lastSeen", "simCards"
            ),
            Json.parseToJsonElement(encoded).jsonObject.keys
        )
    }
}
