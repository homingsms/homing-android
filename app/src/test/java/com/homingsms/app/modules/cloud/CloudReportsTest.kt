package com.homingsms.app.modules.cloud

import com.homingsms.app.data.entities.Message
import com.homingsms.app.data.entities.MessageRecipient
import com.homingsms.app.data.entities.MessageWithRecipients
import com.homingsms.app.domain.EntitySource
import com.homingsms.app.domain.MessageContent
import com.homingsms.app.domain.ProcessingState
import com.homingsms.core.data.cloud.DeviceReportState
import com.homingsms.core.model.ErrorCodes
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CloudReportsTest {
    private fun row(state: ProcessingState, error: String? = null) = MessageWithRecipients(
        Message(
            id = "msg_1",
            withDeliveryReport = true,
            simNumber = 2,
            validUntil = null,
            scheduleAt = null,
            isEncrypted = false,
            skipPhoneValidation = true,
            priority = 0,
            source = EntitySource.Cloud,
            content = MessageContent.Text("hi"),
            createdAt = 1
        ).copy(state = state, partsCount = 3),
        listOf(MessageRecipient("msg_1", "+15550001", state, error))
    )

    @Test
    fun reportsTheCurrentLocalState() {
        assertEquals(DeviceReportState.QUEUED, MessagesCloudSink.reportsFor(row(ProcessingState.Pending), 5).single().state)
        assertEquals(DeviceReportState.SENDING, MessagesCloudSink.reportsFor(row(ProcessingState.Processed), 5).single().state)
        val sent = MessagesCloudSink.reportsFor(row(ProcessingState.Sent), 5).single()
        assertEquals(DeviceReportState.SENT, sent.state)
        assertEquals(1, sent.simSlot)
        assertEquals(3, sent.partsCount)
        assertNull(sent.errorCode)
        assertEquals(DeviceReportState.DELIVERED, MessagesCloudSink.reportsFor(row(ProcessingState.Delivered), 5).single().state)
        assertTrue(MessagesCloudSink.reportsFor(row(ProcessingState.Cancelled), 5).isEmpty())
    }

    @Test
    fun failedReportsIncludeTheStoredCode() {
        val failed = MessagesCloudSink.reportsFor(
            row(ProcessingState.Failed, ErrorCodes.encode(ErrorCodes.RADIO_OFF, "Radio off")),
            5
        ).single()
        assertEquals(DeviceReportState.FAILED, failed.state)
        assertEquals(ErrorCodes.RADIO_OFF, failed.errorCode)
        assertEquals("Radio off", failed.errorMessage)
    }
}
