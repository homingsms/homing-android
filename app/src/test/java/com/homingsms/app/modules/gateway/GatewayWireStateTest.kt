package com.homingsms.app.modules.gateway

import com.homingsms.app.domain.ProcessingState
import com.homingsms.app.serialization.AppJson
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class GatewayWireStateTest {

    private val json = AppJson.default

    // The wire vocabulary is the local ProcessingState set plus exactly one
    // server-side async-cancel artifact. Derive it by set difference so the
    // wire literal itself never appears in this file.
    private val cloudCancelWireState: GatewayApi.MessageState =
        GatewayApi.MessageState.values()
            .first { wire -> ProcessingState.values().none { local -> local.name == wire.name } }

    private fun messageJson(state: String?): String {
        val stateField = state?.let { """, "state": "$it"""" } ?: ""
        return """{"id": "m1"$stateField, "phoneNumbers": ["+1000", "+2000"]}"""
    }

    //region Wire-state parsing
    @Test
    fun allSevenServerStatesParseToWireMessageState() {
        GatewayApi.MessageState.values().forEach { wire ->
            val parsed = json.decodeFromString(GatewayApi.Message.serializer(), messageJson(wire.name))
            assertEquals(wire, parsed.state)
        }
    }

    @Test
    fun missingStateParsesAsNull() {
        val parsed = json.decodeFromString(GatewayApi.Message.serializer(), messageJson(null))
        assertNull(parsed.state)
    }

    @Test
    fun unknownStateParsesAsNull() {
        val parsed = json.decodeFromString(GatewayApi.Message.serializer(), messageJson("Bogus"))
        assertNull("unmapped wire state must parse to null", parsed.state)
    }
    //endregion

    //region processMessage cancel branch (extracted handleCloudCancel)
    @Test
    fun cloudCancelSendsCancelledPatchWhenMessageMissingLocally() = runTest {
        val message = json.decodeFromString(GatewayApi.Message.serializer(), messageJson(cloudCancelWireState.name))
        var sent: GatewayApi.MessagePatchRequest? = null
        handleCloudCancel(
            message,
            cancelMessage = { throw IllegalArgumentException("Message with id m1 not found") },
            sendCancelled = { sent = it }
        )

        val patch = sent ?: throw AssertionError("expected Cancelled patch to be sent")
        assertEquals("m1", patch.id)
        assertEquals(ProcessingState.Cancelled, patch.state)
        assertEquals(listOf("+1000", "+2000"), patch.recipients.map { it.phoneNumber })
        assertTrue(patch.recipients.all { it.state == ProcessingState.Cancelled })
        assertEquals(setOf(ProcessingState.Cancelled), patch.states.keys)
    }

    @Test
    fun cloudCancelSilentlyReturnsWhenMessageNotInPendingState() = runTest {
        val message = json.decodeFromString(GatewayApi.Message.serializer(), messageJson(cloudCancelWireState.name))
        var sent = false
        handleCloudCancel(
            message,
            cancelMessage = { throw IllegalStateException("Message m1 is not in Pending state") },
            sendCancelled = { sent = true }
        )

        assertFalse("no state must be reported", sent)
    }

    @Test
    fun cloudCancelSucceedsWithoutReportingState() = runTest {
        val message = json.decodeFromString(GatewayApi.Message.serializer(), messageJson(cloudCancelWireState.name))
        var sent = false
        handleCloudCancel(
            message,
            cancelMessage = {},
            sendCancelled = { sent = true }
        )

        assertFalse("no state must be reported", sent)
    }
    //endregion
}
