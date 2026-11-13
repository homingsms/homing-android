package com.homingsms.app.modules.settings

import kotlin.reflect.typeOf
import kotlinx.serialization.Serializable
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ValueSerializerTest {
    private val serializer = ValueSerializer()

    @Test
    fun decodesNumbersStoredAsStrings() {
        assertEquals(30, serializer.deserialize<Int>("30", typeOf<Int>()))
        assertEquals(86_400L, serializer.deserialize<Long>("86400", typeOf<Long>()))
    }

    @Test
    fun decodesUnquotedEnumNames() {
        assertEquals(
            SampleChannel.SSE_ONLY,
            serializer.deserialize<SampleChannel>(
                "SSE_ONLY",
                typeOf<SampleChannel>()
            )
        )
    }

    @Test
    fun blankValuesAreNull() {
        assertNull(serializer.deserialize<String>("", typeOf<String>()))
        assertNull(serializer.deserialize<Int>("  ", typeOf<Int>()))
    }

    @Test
    fun roundTripsObjects() {
        val info = SampleRegistration(
            id = "id",
            token = "token",
            login = "login",
            password = null
        )

        val encoded = serializer.serialize(info)

        assertEquals("""{"id":"id","token":"token","login":"login"}""", encoded)
        assertEquals(info, serializer.deserialize<SampleRegistration>(encoded, typeOf<SampleRegistration>()))
    }
}

@Serializable
data class SampleRegistration(val id: String, val token: String, val login: String, val password: String? = null)
