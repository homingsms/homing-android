package com.homingsms.app.modules.settings

import com.homingsms.app.modules.gateway.GatewayApi
import com.homingsms.app.modules.gateway.GatewaySettings
import kotlin.reflect.typeOf
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
            GatewaySettings.NotificationChannel.SSE_ONLY,
            serializer.deserialize<GatewaySettings.NotificationChannel>(
                "SSE_ONLY",
                typeOf<GatewaySettings.NotificationChannel>()
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
        val info = GatewayApi.DeviceRegisterResponse(
            id = "id",
            token = "token",
            login = "login",
            password = null
        )

        val encoded = serializer.serialize(info)

        assertEquals("""{"id":"id","token":"token","login":"login"}""", encoded)
        assertEquals(info, serializer.deserialize<GatewayApi.DeviceRegisterResponse>(encoded, typeOf<GatewayApi.DeviceRegisterResponse>()))
    }
}
