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
import com.homingsms.core.data.db.CountsRow
import com.homingsms.core.data.db.HourReceivedRow
import com.homingsms.core.data.db.HourRow
import com.homingsms.core.data.db.SimCountRow
import com.homingsms.core.data.db.SimPartsRow
import com.homingsms.core.data.db.StatsDao
import com.homingsms.core.model.SmsPartsUsage
import java.util.TimeZone
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class SmsPartsTest {
    private val context: Context = ApplicationProvider.getApplicationContext()
    private val db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).allowMainThreadQueries().build()

    @After
    fun close() = db.close()

    private fun insert(
        id: String,
        sim: Int?,
        parts: Int?,
        recipients: List<ProcessingState>,
        processedAt: Long? = THIS_MONTH,
        content: MessageContent = MessageContent.Text("hello $id")
    ) {
        val messageState = recipients.firstOrNull { it != ProcessingState.Failed } ?: ProcessingState.Failed
        val message = Message(
            id = id,
            withDeliveryReport = true,
            simNumber = sim,
            validUntil = null,
            scheduleAt = null,
            isEncrypted = false,
            skipPhoneValidation = true,
            priority = Message.PRIORITY_DEFAULT,
            source = EntitySource.Local,
            content = content,
            createdAt = processedAt ?: THIS_MONTH
        ).copy(state = messageState, partsCount = parts, processedAt = processedAt)
        val rows = recipients.mapIndexed { index, state -> MessageRecipient(id, "+1555000$index", state) }
        db.messagesDao().insert(MessageWithRecipients(message, rows))
    }

    @Test
    fun sumsPartsPerSimForRecipientsThatWereSentOrDelivered() = runBlocking {
        insert("multi", sim = 1, parts = 3, recipients = listOf(ProcessingState.Sent, ProcessingState.Delivered))
        insert("no-parts", sim = 2, parts = null, recipients = listOf(ProcessingState.Delivered))
        insert("partial", sim = 2, parts = 2, recipients = listOf(ProcessingState.Sent, ProcessingState.Failed))
        insert("failed", sim = 1, parts = 2, recipients = listOf(ProcessingState.Failed))
        insert("queued", sim = 1, parts = 2, recipients = listOf(ProcessingState.Pending))
        insert("sending", sim = 1, parts = 2, recipients = listOf(ProcessingState.Processed))
        insert("cancelled", sim = 1, parts = 2, recipients = listOf(ProcessingState.Cancelled))
        insert(
            "mms",
            sim = 1,
            parts = 4,
            recipients = listOf(ProcessingState.Sent),
            content = MessageContent.Mms(subject = null, text = "hi", attachments = emptyList())
        )
        insert(
            "data",
            sim = null,
            parts = 1,
            recipients = listOf(ProcessingState.Sent),
            content = MessageContent.Data("aGk=", 8901u)
        )
        insert("old", sim = 1, parts = 5, recipients = listOf(ProcessingState.Delivered), processedAt = LAST_MONTH)

        val month = db.statsDao().observeSentParts(MONTH_START).first().associate { it.simNumber to it.parts }
        val allTime = db.statsDao().observeSentParts(0L).first().associate { it.simNumber to it.parts }

        assertEquals(mapOf(1 to 6L, 2 to 3L, null to 1L), month)
        assertEquals(mapOf(1 to 11L, 2 to 3L, null to 1L), allTime)
    }

    @Test
    fun emptyDatabaseHasNoRows() = runBlocking {
        assertEquals(emptyList<SimPartsRow>(), db.statsDao().observeSentParts(0L).first())
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun requeriesWhenTheLocalMonthRollsOver() = runTest {
        val lagos = TimeZone.getTimeZone("Africa/Lagos")
        val start = MARCH_FIRST_LAGOS - HALF_HOUR
        val dao = PartsDao()
        val repository = StatsRepository(dao, clock = { start + testScheduler.currentTime }, zone = { lagos })
        val seen = mutableListOf<SmsPartsUsage>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { repository.smsParts.toList(seen) }
        runCurrent()

        assertEquals(listOf(FEBRUARY_FIRST_LAGOS, 0L), dao.requested)
        assertEquals(mapOf(0 to 3L, null to 2L), seen.last().month)
        assertEquals(mapOf(0 to 10L, 1 to 4L), seen.last().allTime)

        advanceTimeBy(HALF_HOUR + 1_000L)
        runCurrent()
        assertEquals(listOf(FEBRUARY_FIRST_LAGOS, 0L, MARCH_FIRST_LAGOS, 0L), dao.requested)

        advanceTimeBy(3 * HALF_HOUR * 2)
        runCurrent()
        assertEquals(4, dao.requested.size)
    }

    private class PartsDao : StatsDao {
        val requested = mutableListOf<Long>()

        override fun observeSentParts(from: Long): Flow<List<SimPartsRow>> {
            requested += from
            return flowOf(
                if (from == 0L) {
                    listOf(SimPartsRow(1, 10), SimPartsRow(2, 4))
                } else {
                    listOf(SimPartsRow(1, 3), SimPartsRow(null, 2))
                }
            )
        }

        override fun observeCounts(from: Long): Flow<CountsRow> = error("unused")
        override fun observeQueued(): Flow<Int> = error("unused")
        override fun observeReceived(from: Long): Flow<Int> = error("unused")
        override fun observeHourly(from: Long): Flow<List<HourRow>> = error("unused")
        override fun observeHourlyReceived(from: Long): Flow<List<HourReceivedRow>> = error("unused")
        override fun observeSentPerSim(from: Long): Flow<List<SimCountRow>> = error("unused")
        override suspend fun sentPerSim(from: Long): List<SimCountRow> = error("unused")
        override suspend fun queued(): Int = error("unused")
        override fun processedSince(from: Long): Int = error("unused")
        override fun oldestSendingAt(): Long? = error("unused")
    }

    private companion object {
        const val HALF_HOUR = 1_800_000L
        const val FEBRUARY_FIRST_LAGOS = 1_769_900_400_000L
        const val MARCH_FIRST_LAGOS = 1_772_319_600_000L
        const val MONTH_START = MARCH_FIRST_LAGOS
        const val THIS_MONTH = MARCH_FIRST_LAGOS + 86_400_000L
        const val LAST_MONTH = MARCH_FIRST_LAGOS - 86_400_000L
    }
}
