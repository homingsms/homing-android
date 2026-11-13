package com.homingsms.core.data.repository

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.homingsms.app.data.AppDatabase
import com.homingsms.app.data.entities.Message
import com.homingsms.app.data.entities.MessageRecipient
import com.homingsms.app.data.entities.MessageWithRecipients
import com.homingsms.app.domain.EntitySource
import com.homingsms.app.domain.MessageContent
import com.homingsms.app.domain.ProcessingState
import com.homingsms.app.modules.incoming.db.IncomingMessage
import com.homingsms.app.modules.incoming.db.IncomingMessageType
import com.homingsms.core.data.FakeSecrets
import com.homingsms.core.data.db.MessageApiMeta
import com.homingsms.core.model.Cursor
import com.homingsms.core.model.ErrorCodes
import com.homingsms.core.model.InboxAttachmentContent
import com.homingsms.core.model.InboxAttachmentInfo
import com.homingsms.core.model.InboxFilter
import com.homingsms.core.model.MessageFilter
import com.homingsms.core.model.MessagePriority
import com.homingsms.core.model.MessageState
import com.homingsms.core.model.MessageType
import com.homingsms.core.model.Scope
import java.util.Date
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class RecordsTest {
    private val context: Context = ApplicationProvider.getApplicationContext()
    private val db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).allowMainThreadQueries().build()
    private val records = MessageRecords(db.apiMessagesDao()) { NOW }

    @After
    fun close() = db.close()

    private data class Seed(
        val state: ProcessingState = ProcessingState.Pending,
        val source: EntitySource = EntitySource.Local,
        val to: String = "+15550001",
        val error: String? = null,
        val scheduleAt: Long? = null,
        val content: MessageContent? = null
    )

    private fun insert(id: String, createdAt: Long, seed: Seed = Seed()) {
        val state = seed.state
        val content = seed.content ?: MessageContent.Text("hello $id")
        val message = Message(
            id = id,
            withDeliveryReport = true,
            simNumber = 2,
            validUntil = null,
            scheduleAt = seed.scheduleAt?.let(::Date),
            isEncrypted = false,
            skipPhoneValidation = true,
            priority = if (id.endsWith("otp")) Message.PRIORITY_EXPEDITED else Message.PRIORITY_DEFAULT,
            source = seed.source,
            content = content,
            createdAt = createdAt
        ).copy(state = state, partsCount = 1)
        db.messagesDao().insert(MessageWithRecipients(message, listOf(MessageRecipient(id, seed.to, state, seed.error))))
    }

    @Test
    fun mapsStoredMessagesToApiRecords() = runBlocking {
        insert("msg_otp", 100, Seed(ProcessingState.Sent))
        records.insertMeta(MessageApiMeta("msg_otp", "key-1", "hash", MessageRecords.encodeMetadata(mapOf("a" to "b"))))
        val record = records.get("msg_otp")!!
        assertEquals(MessageState.Sent, record.state)
        assertEquals(MessagePriority.Otp, record.priority)
        assertEquals(1, record.simSlot)
        assertEquals("key-1", record.idempotencyKey)
        assertEquals(mapOf("a" to "b"), record.metadata)
        assertEquals("hello msg_otp", record.text)
        assertEquals("msg_otp", records.findByIdempotencyKey("key-1")?.messageId)
        assertNull(records.get("msg_otp", EntitySource.Cloud))
    }

    @Test
    fun mapsFailureCodesAndExpiry() = runBlocking {
        insert("msg_failed", 100, Seed(ProcessingState.Failed, error = ErrorCodes.encode(ErrorCodes.NO_SERVICE, "No service")))
        insert("msg_expired", 200, Seed(ProcessingState.Failed, error = ErrorCodes.encode(ErrorCodes.EXPIRED, "Expired")))
        insert("msg_scheduled", 300, Seed(scheduleAt = NOW + 60_000))
        insert("msg_cloud", 400, Seed(source = EntitySource.Cloud))
        assertEquals(MessageState.Failed, records.get("msg_failed")!!.state)
        assertEquals("no_service", records.get("msg_failed")!!.error?.code)
        assertEquals(MessageState.Expired, records.get("msg_expired")!!.state)
        assertEquals(MessageState.Scheduled, records.get("msg_scheduled")!!.state)
        assertEquals(MessageState.Queued, records.get("msg_cloud")!!.state)

        assertEquals(listOf("msg_expired"), records.list(MessageFilter(state = MessageState.Expired), null, 10).items.map { it.id })
        assertEquals(listOf("msg_failed"), records.list(MessageFilter(state = MessageState.Failed), null, 10).items.map { it.id })
        assertEquals(listOf("msg_scheduled"), records.list(MessageFilter(state = MessageState.Scheduled), null, 10).items.map { it.id })
        assertTrue(records.list(MessageFilter(state = MessageState.Pending), null, 10).items.isEmpty())
        assertEquals(4, records.list(MessageFilter(), null, 10, source = null).items.size)
    }

    @Test
    fun pagesNewestFirstWithFilters() = runBlocking {
        (1..5).forEach { insert("msg_$it", it * 100L, Seed(to = if (it % 2 == 0) "+15550002" else "+15550001")) }
        insert(
            "msg_mms",
            50,
            Seed(content = MessageContent.Mms("Subject", null, listOf(MessageContent.Mms.Attachment("image/png", "a.png", "aGVsbG8="))))
        )
        val first = records.list(MessageFilter(), null, 2)
        assertEquals(listOf("msg_5", "msg_4"), first.items.map { it.id })
        val second = records.list(MessageFilter(), Cursor.decode(first.nextCursor!!), 2)
        assertEquals(listOf("msg_3", "msg_2"), second.items.map { it.id })
        val last = records.list(MessageFilter(), Cursor.decode(second.nextCursor!!), 5)
        assertEquals(listOf("msg_1", "msg_mms"), last.items.map { it.id })
        assertNull(last.nextCursor)

        assertEquals(listOf("msg_4", "msg_2"), records.list(MessageFilter(to = "+15550002"), null, 10).items.map { it.id })
        assertEquals(listOf("msg_mms"), records.list(MessageFilter(type = MessageType.Mms), null, 10).items.map { it.id })
        assertEquals(
            listOf("msg_3", "msg_2"),
            records.list(MessageFilter(createdFrom = 200, createdTo = 400), null, 10).items.map {
                it.id
            }
        )
        val mms = records.get("msg_mms")!!
        assertEquals("Subject", mms.subject)
        assertEquals(5L, mms.attachments.single().size)
    }

    @Test
    fun uiFiltersCombineStatesTypesSimsAndSearch() = runBlocking {
        insert("msg_sent", 100, Seed(ProcessingState.Sent, to = "+15550007"))
        insert("msg_failed", 200, Seed(ProcessingState.Failed, error = ErrorCodes.encode(ErrorCodes.NO_SERVICE, "No service")))
        insert("msg_100%_off", 300, Seed(ProcessingState.Delivered, content = MessageContent.Text("Sale 100% off")))
        insert(
            "msg_mms",
            400,
            Seed(content = MessageContent.Mms("Subject", "photo", listOf(MessageContent.Mms.Attachment("image/png", "a.png", "aGVsbG8="))))
        )

        suspend fun ids(filter: MessageFilter) = records.list(filter, null, 10, source = null).items.map { it.id }

        assertEquals(listOf("msg_failed", "msg_sent"), ids(MessageFilter(states = setOf(MessageState.Sent, MessageState.Failed))))
        assertEquals(listOf("msg_mms"), ids(MessageFilter(types = setOf(MessageType.Mms))))
        assertEquals(4, ids(MessageFilter(simSlots = setOf(1))).size)
        assertTrue(ids(MessageFilter(simSlots = setOf(0))).isEmpty())
        assertEquals(listOf("msg_sent"), ids(MessageFilter(search = "550007")))
        assertEquals(listOf("msg_100%_off"), ids(MessageFilter(search = "100%")))
        assertTrue(ids(MessageFilter(search = "a_e")).isEmpty())
        assertEquals(listOf("msg_100%_off"), ids(MessageFilter(states = setOf(MessageState.Delivered), search = "sale")))
    }

    @Test
    fun inboxUiFiltersMatchKindsSimsAndText() = runBlocking {
        val inbox = InboxRecords(
            db.apiInboxDao(),
            object : AttachmentReader {
                override fun list(localId: String) = emptyList<InboxAttachmentInfo>()

                override fun read(localId: String, attachmentId: String): InboxAttachmentContent? = null
            }
        )
        val dao = db.incomingMessagesDao()
        dao.insert(IncomingMessage("text:1", IncomingMessageType.SMS, "+15550001", null, 1, null, "Hi there", 1000, "inb_a", "Hi there"))
        dao.insert(IncomingMessage("text:2", IncomingMessageType.SMS, "+15550002", null, 2, null, "STOP", 2000, "inb_b", "STOP"))
        dao.insert(
            IncomingMessage("mms:1", IncomingMessageType.MMS_DOWNLOADED, "+15550003", null, 2, null, "Pic", 3000, "inb_c", subject = "Trip")
        )

        suspend fun ids(filter: InboxFilter) = inbox.list(filter, null, 10).items.map { it.id }

        assertEquals(listOf("inb_c", "inb_b"), ids(InboxFilter(simSlots = setOf(1))))
        assertEquals(listOf("inb_c", "inb_a"), ids(InboxFilter(types = setOf(MessageType.Mms, MessageType.Sms), search = "i")))
        assertEquals(listOf("inb_b"), ids(InboxFilter(search = "stop")))
        assertEquals(listOf("inb_c"), ids(InboxFilter(search = "trip")))
        assertEquals(listOf("inb_a"), ids(InboxFilter(search = "0001")))
    }

    @Test
    fun inboxRecordsUsePublicIdsAndBuildUploads() = runBlocking {
        val attachments = object : AttachmentReader {
            override fun list(localId: String) = if (localId ==
                "mms:1"
            ) {
                listOf(InboxAttachmentInfo("7", "image/png", "a.png", 3))
            } else {
                emptyList()
            }

            override fun read(localId: String, attachmentId: String) =
                if (localId == "mms:1" && attachmentId == "7") InboxAttachmentContent("image/png", "a.png", byteArrayOf(1, 2, 3)) else null
        }
        val inbox = InboxRecords(db.apiInboxDao(), attachments)
        val dao = db.incomingMessagesDao()
        dao.insert(
            IncomingMessage("text:1", IncomingMessageType.SMS, "+15550001", null, 1, null, "Hi", 1000, "inb_a", "Hi", insertedAt = 1500)
        )
        dao.insert(
            IncomingMessage(
                "data:1", IncomingMessageType.DATA_SMS, "+15550002", null, null, null, "01", 2000, "inb_b", data = "AQ==", port = 9
            )
        )
        dao.insert(
            IncomingMessage("mms:1", IncomingMessageType.MMS_DOWNLOADED, "+15550003", null, 2, null, "Pic", 3000, "inb_c", subject = "S")
        )
        dao.insert(IncomingMessage("mms-header:1", IncomingMessageType.MMS, "+15550003", null, 2, null, "Pic", 3500, "inb_d"))

        val page = inbox.list(InboxFilter(), null, 2)
        assertEquals(listOf("inb_c", "inb_b"), page.items.map { it.id })
        assertEquals(listOf("inb_a"), inbox.list(InboxFilter(), Cursor.decode(page.nextCursor!!), 2).items.map { it.id })
        assertEquals(listOf("inb_b"), inbox.list(InboxFilter(type = MessageType.Data), null, 10).items.map { it.id })
        assertEquals(listOf("inb_a"), inbox.list(InboxFilter(from = "+15550001"), null, 10).items.map { it.id })
        assertEquals(listOf("inb_b"), inbox.list(InboxFilter(receivedFrom = 1500, receivedTo = 2500), null, 10).items.map { it.id })

        val sms = inbox.get("inb_a")!!
        assertEquals(0, sms.simSlot)
        assertEquals(1500, sms.createdAt)
        assertNull(inbox.get("inb_d"))
        assertEquals(9, inbox.get("inb_b")!!.port)
        assertNotNull(inbox.attachment("inb_c", "7"))

        val uploads = inbox.uploads(listOf("text:1", "mms:1", "mms-header:1"))
        assertEquals(setOf("text:1", "mms:1"), uploads.keys)
        assertEquals("AQID", uploads.getValue("mms:1").attachments!!.single().data)
        assertEquals("mms", uploads.getValue("mms:1").type)
    }

    @Test
    fun apiKeysAreHashedAndRevocable() = runBlocking {
        val secrets = FakeSecrets()
        val keys = ApiKeyRepository(db.apiKeysDao(), secrets) { NOW }
        val created = keys.create("Billing", setOf(Scope.MessagesSend, Scope.InboxRead))
        assertTrue(created.secret.startsWith("hk_live_"))
        assertEquals(40, created.secret.length)
        assertEquals(created.secret.take(12), created.info.prefix)
        val stored = db.apiKeysDao().active().single()
        assertTrue(stored.hash != created.secret && !stored.hash.contains(created.secret))
        assertNotNull(secrets.get("apikeys.pepper"))

        val principal = keys.authenticate(created.secret)!!
        assertEquals(setOf(Scope.MessagesSend, Scope.InboxRead), principal.scopes)
        assertEquals(NOW, db.apiKeysDao().active().single().lastUsedAt)
        assertNull(keys.authenticate(created.secret.dropLast(1) + "x"))
        assertNull(keys.authenticate("hk_live_short"))

        assertTrue(keys.revoke(created.info.id))
        assertNull(keys.authenticate(created.secret))
        assertTrue(keys.list().isEmpty())
    }

    private companion object {
        const val NOW = 1_794_478_530_000L
    }
}
