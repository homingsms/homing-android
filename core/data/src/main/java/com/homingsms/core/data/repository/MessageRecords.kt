package com.homingsms.core.data.repository

import androidx.sqlite.db.SimpleSQLiteQuery
import com.homingsms.app.data.entities.Message
import com.homingsms.app.data.entities.MessageType as StoredType
import com.homingsms.app.data.entities.MessageWithRecipients
import com.homingsms.app.domain.EntitySource
import com.homingsms.app.domain.MessageContent
import com.homingsms.app.domain.ProcessingState
import com.homingsms.core.data.db.ApiMessagesDao
import com.homingsms.core.data.db.MessageApiMeta
import com.homingsms.core.model.Cursor
import com.homingsms.core.model.ErrorCodes
import com.homingsms.core.model.MessageAttachmentInfo
import com.homingsms.core.model.MessageError
import com.homingsms.core.model.MessageEventRecord
import com.homingsms.core.model.MessageFilter
import com.homingsms.core.model.MessagePriority
import com.homingsms.core.model.MessageRecord
import com.homingsms.core.model.MessageState
import com.homingsms.core.model.MessageType
import com.homingsms.core.model.Page
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json

object MessageStateMapper {
    fun map(stored: ProcessingState, source: EntitySource, scheduleAt: Long?, error: MessageError?, now: Long): MessageState =
        when (stored) {
            ProcessingState.Pending -> when {
                scheduleAt != null && scheduleAt > now -> MessageState.Scheduled
                source == EntitySource.Local -> MessageState.Pending
                else -> MessageState.Queued
            }

            ProcessingState.Processed -> MessageState.Sending

            ProcessingState.Sent -> MessageState.Sent

            ProcessingState.Delivered -> MessageState.Delivered

            ProcessingState.Cancelled -> MessageState.Cancelled

            ProcessingState.Failed -> if (error?.code == ErrorCodes.EXPIRED) MessageState.Expired else MessageState.Failed
        }

    fun type(stored: StoredType): MessageType = when (stored) {
        StoredType.Text -> MessageType.Sms
        StoredType.Data -> MessageType.Data
        StoredType.Mms -> MessageType.Mms
    }

    fun storedType(type: MessageType): StoredType = when (type) {
        MessageType.Sms -> StoredType.Text
        MessageType.Data -> StoredType.Data
        MessageType.Mms -> StoredType.Mms
    }
}

class MessageRecords(private val dao: ApiMessagesDao, private val clock: () -> Long = System::currentTimeMillis) {
    suspend fun get(id: String, source: EntitySource? = null): MessageRecord? {
        val row = dao.get(id)?.takeIf { source == null || it.message.source == source } ?: return null
        val meta = dao.meta(listOf(id)).firstOrNull()
        return toRecord(row, meta, dao.recipientStates(id).map { it.state to it.updatedAt })
    }

    suspend fun findByIdempotencyKey(key: String): MessageApiMeta? = dao.findByIdempotencyKey(key)

    suspend fun insertMeta(meta: MessageApiMeta) = dao.insertMeta(meta)

    suspend fun list(filter: MessageFilter, cursor: Cursor?, limit: Int, source: EntitySource? = EntitySource.Local): Page<MessageRecord> {
        val conditions = mutableListOf("1")
        val args = mutableListOf<Any>()
        source?.let {
            conditions += "source = ?"
            args += it.name
        }
        filter.state?.let { conditions += stateCondition(it, args) }
        filter.type?.let {
            conditions += "type = ?"
            args += MessageStateMapper.storedType(it).name
        }
        filter.to?.let {
            conditions += "EXISTS (SELECT 1 FROM messagerecipient r WHERE r.messageId = message.id AND r.phoneNumber = ?)"
            args += it
        }
        filter.createdFrom?.let {
            conditions += "createdAt >= ?"
            args += it
        }
        filter.createdTo?.let {
            conditions += "createdAt < ?"
            args += it
        }
        conditions += uiConditions(filter, args)
        cursor?.let {
            conditions += "(createdAt < ? OR (createdAt = ? AND id < ?))"
            args += it.timestamp
            args += it.timestamp
            args += it.id
        }
        args += limit + 1
        val sql = "SELECT *, `rowid` FROM message WHERE ${conditions.joinToString(" AND ")} ORDER BY createdAt DESC, id DESC LIMIT ?"
        val rows = dao.query(SimpleSQLiteQuery(sql, args.toTypedArray()))
        val items = rows.take(limit)
        val meta = if (items.isEmpty()) emptyMap() else dao.meta(items.map { it.message.id }).associateBy { it.messageId }
        val records = items.map { toRecord(it, meta[it.message.id], emptyList()) }
        val next = if (rows.size > limit) items.last().message.let { Cursor(it.createdAt, it.id).encode() } else null
        return Page(records, next)
    }

    private fun uiConditions(filter: MessageFilter, args: MutableList<Any>): List<String> = buildList {
        if (filter.states.isNotEmpty()) {
            add(filter.states.sortedBy { it.ordinal }.joinToString(" OR ", "(", ")") { stateCondition(it, args) })
        }
        if (filter.types.isNotEmpty()) {
            add(filter.types.sortedBy { it.ordinal }.joinToString(", ", "type IN (", ")") { "'${MessageStateMapper.storedType(it).name}'" })
        }
        if (filter.simSlots.isNotEmpty()) {
            add(filter.simSlots.sorted().joinToString(", ", "simNumber IN (", ")") { (it + 1).toString() })
        }
        filter.search?.trim()?.takeIf { it.isNotEmpty() }?.let { query ->
            val pattern = "%" + escapeLike(query) + "%"
            add(
                "(EXISTS (SELECT 1 FROM messagerecipient r WHERE r.messageId = message.id AND r.phoneNumber LIKE ? ESCAPE '\\') " +
                    "OR (isEncrypted = 0 AND content LIKE ? ESCAPE '\\'))"
            )
            args += pattern
            args += pattern
        }
    }

    private fun stateCondition(state: MessageState, args: MutableList<Any>): String {
        val nowIso = isoFormat().format(Date(clock()))
        return when (state) {
            MessageState.Scheduled -> {
                args += nowIso
                "state = 'Pending' AND scheduleAt IS NOT NULL AND scheduleAt > ?"
            }

            MessageState.Pending, MessageState.Queued -> {
                args += nowIso
                "state = 'Pending' AND (scheduleAt IS NULL OR scheduleAt <= ?)"
            }

            MessageState.Leased -> "0"

            MessageState.Sending -> "state = 'Processed'"

            MessageState.Sent -> "state = 'Sent'"

            MessageState.Delivered -> "state = 'Delivered'"

            MessageState.Cancelled -> "state = 'Cancelled'"

            MessageState.Failed -> "state = 'Failed' AND NOT $EXPIRED_EXISTS"

            MessageState.Expired -> "state = 'Failed' AND $EXPIRED_EXISTS"
        }
    }

    fun toRecord(row: MessageWithRecipients, meta: MessageApiMeta?, recipientStates: List<Pair<ProcessingState, Long>>): MessageRecord {
        val message = row.message
        val recipient = row.recipients.firstOrNull()
        val error = row.recipients.firstNotNullOfOrNull { ErrorCodes.decode(it.error) }
        val now = clock()
        val scheduleAt = message.scheduleAt?.time
        val timeline = (row.states.map { it.state to it.updatedAt } + recipientStates)
            .groupBy({ it.first }, { it.second })
            .mapValues { (_, times) -> times.min() }
        val content = decodeContent(message)
        return MessageRecord(
            id = message.id,
            type = MessageStateMapper.type(message.type),
            to = recipient?.phoneNumber.orEmpty(),
            text = (content as? MessageContent.Text)?.text ?: (content as? MessageContent.Mms)?.text,
            data = (content as? MessageContent.Data)?.data,
            port = (content as? MessageContent.Data)?.port?.toInt(),
            subject = (content as? MessageContent.Mms)?.subject,
            attachments = (content as? MessageContent.Mms)?.attachments.orEmpty().mapIndexed { index, attachment ->
                MessageAttachmentInfo("att_$index", attachment.contentType, attachment.name, base64Size(attachment.data))
            },
            state = MessageStateMapper.map(message.state, message.source, scheduleAt, error, now),
            priority = if (message.priority >= Message.PRIORITY_EXPEDITED) MessagePriority.Otp else MessagePriority.Normal,
            simSlot = message.simNumber?.minus(1),
            idempotencyKey = meta?.idempotencyKey,
            scheduleAt = scheduleAt,
            validUntil = message.validUntil?.time,
            deliveryReport = message.withDeliveryReport,
            metadata = meta?.metadata?.let(::decodeMetadata).orEmpty(),
            attempts = if (message.state == ProcessingState.Pending || message.state == ProcessingState.Cancelled) 0 else 1,
            partsCount = message.partsCount,
            error = error,
            createdAt = message.createdAt,
            updatedAt = (timeline.values + message.createdAt).max(),
            sentAt = timeline[ProcessingState.Sent],
            deliveredAt = timeline[ProcessingState.Delivered],
            failedAt = timeline[ProcessingState.Failed],
            events = events(row, scheduleAt, error)
        )
    }

    private fun events(row: MessageWithRecipients, scheduleAt: Long?, error: MessageError?): List<MessageEventRecord> =
        row.states.sortedBy { it.updatedAt }.map {
            val scheduledAt = scheduleAt?.takeIf { schedule -> it.updatedAt < schedule }
            MessageEventRecord(
                state = MessageStateMapper.map(it.state, row.message.source, scheduledAt, error, it.updatedAt),
                at = it.updatedAt,
                simSlot = row.message.simNumber?.minus(1),
                error = if (it.state == ProcessingState.Failed) error else null
            )
        }

    private fun decodeContent(message: Message): MessageContent? {
        if (message.isEncrypted) return null
        return runCatching { Message.decodeContent(message.type, message.content) }.getOrNull()
    }

    companion object {
        private const val EXPIRED_EXISTS =
            "EXISTS (SELECT 1 FROM messagerecipient r WHERE r.messageId = message.id AND r.error LIKE '[expired]%')"
        private val metadataSerializer = MapSerializer(String.serializer(), String.serializer())
        private val json = Json { ignoreUnknownKeys = true }

        fun encodeMetadata(metadata: Map<String, String>): String? = metadata.takeIf { it.isNotEmpty() }?.let {
            json.encodeToString(metadataSerializer, it)
        }

        fun escapeLike(value: String): String = value.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_")

        fun decodeMetadata(value: String): Map<String, String> = runCatching { json.decodeFromString(metadataSerializer, value) }
            .getOrDefault(emptyMap())

        @Suppress("MagicNumber")
        fun base64Size(value: String): Long {
            val trimmed = value.trimEnd('=')
            return trimmed.length * 3L / 4L
        }

        private fun isoFormat() = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("GMT")
        }
    }
}
