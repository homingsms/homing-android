package com.homingsms.core.data.repository

import androidx.sqlite.db.SimpleSQLiteQuery
import com.homingsms.app.modules.incoming.db.IncomingMessage
import com.homingsms.app.modules.incoming.db.IncomingMessageType
import com.homingsms.core.data.cloud.InboxAttachmentUploadDto
import com.homingsms.core.data.cloud.InboxUploadDto
import com.homingsms.core.data.db.ApiInboxDao
import com.homingsms.core.model.Cursor
import com.homingsms.core.model.InboxAttachmentContent
import com.homingsms.core.model.InboxAttachmentInfo
import com.homingsms.core.model.InboxFilter
import com.homingsms.core.model.InboxRecord
import com.homingsms.core.model.MessageType
import com.homingsms.core.model.Page
import com.homingsms.core.model.api.Iso8601
import kotlin.io.encoding.Base64

interface AttachmentReader {
    fun list(localId: String): List<InboxAttachmentInfo>

    fun read(localId: String, attachmentId: String): InboxAttachmentContent?
}

class InboxRecords(private val dao: ApiInboxDao, private val attachments: AttachmentReader) {
    suspend fun get(publicId: String): InboxRecord? = dao.getByPublicId(publicId)?.takeIf { it.type in listed }?.toRecord()

    suspend fun getByLocalId(localId: String): InboxRecord? =
        dao.getByLocalIds(listOf(localId)).firstOrNull()?.takeIf { it.type in listed }?.toRecord()

    suspend fun attachment(publicId: String, attachmentId: String): InboxAttachmentContent? {
        val message = dao.getByPublicId(publicId) ?: return null
        return attachments.read(message.id, attachmentId)
    }

    suspend fun list(filter: InboxFilter, cursor: Cursor?, limit: Int): Page<InboxRecord> {
        val conditions = mutableListOf<String>()
        val args = mutableListOf<Any>()
        val requested = (listOfNotNull(filter.type) + filter.types).distinct()
        val types = if (requested.isEmpty()) listed else requested.flatMap(::storedTypes).distinct()
        conditions += "type IN (${types.joinToString { "'${it.name}'" }})"
        if (filter.simSlots.isNotEmpty()) {
            conditions += filter.simSlots.sorted().joinToString(", ", "simNumber IN (", ")") { (it + 1).toString() }
        }
        filter.search?.trim()?.takeIf { it.isNotEmpty() }?.let { query ->
            val pattern = "%" + MessageRecords.escapeLike(query) + "%"
            conditions += listOf("sender", "text", "contentPreview", "subject").joinToString(" OR ", "(", ")") { "$it LIKE ? ESCAPE '\\'" }
            repeat(SEARCH_COLUMNS) { args += pattern }
        }
        filter.from?.let {
            conditions += "sender = ?"
            args += it
        }
        filter.receivedFrom?.let {
            conditions += "createdAt >= ?"
            args += it
        }
        filter.receivedTo?.let {
            conditions += "createdAt < ?"
            args += it
        }
        cursor?.let {
            conditions += "(createdAt < ? OR (createdAt = ? AND publicId < ?))"
            args += it.timestamp
            args += it.timestamp
            args += it.id
        }
        args += limit + 1
        val sql = "SELECT * FROM incoming_messages WHERE ${conditions.joinToString(" AND ")} ORDER BY createdAt DESC, publicId DESC LIMIT ?"
        val rows = dao.query(SimpleSQLiteQuery(sql, args.toTypedArray()))
        val items = rows.take(limit)
        val next = if (rows.size > limit) items.last().let { Cursor(it.createdAt, it.publicId).encode() } else null
        return Page(items.map { it.toRecord() }, next)
    }

    suspend fun uploads(localIds: List<String>): Map<String, InboxUploadDto> = dao.getByLocalIds(localIds)
        .filter { it.type in listed }
        .associate { it.id to it.toUpload() }

    private fun IncomingMessage.toRecord(): InboxRecord = InboxRecord(
        id = publicId,
        localId = id,
        type = typeOf(type),
        from = sender,
        to = recipient,
        simSlot = simNumber?.minus(1),
        text = text ?: contentPreview.takeIf { type == IncomingMessageType.SMS },
        data = data,
        port = port,
        subject = subject,
        attachments = if (type == IncomingMessageType.MMS_DOWNLOADED) attachments.list(id) else emptyList(),
        receivedAt = createdAt,
        createdAt = insertedAt.takeIf { it > 0 } ?: createdAt
    )

    private fun IncomingMessage.toUpload(): InboxUploadDto {
        val record = toRecord()
        val files = record.attachments.mapNotNull { info ->
            attachments.read(id, info.id)?.let { InboxAttachmentUploadDto(it.contentType, it.name, Base64.Default.encode(it.bytes)) }
        }
        return InboxUploadDto(
            localId = id,
            type = record.type.value,
            from = sender.take(MAX_ADDRESS),
            to = recipient?.take(MAX_ADDRESS),
            simSlot = record.simSlot,
            receivedAt = Iso8601.format(createdAt),
            text = record.text?.take(MAX_TEXT) ?: if (record.type == MessageType.Sms) "" else null,
            data = record.data,
            port = record.port,
            subject = record.subject?.take(MAX_SUBJECT),
            attachments = files.takeIf { it.isNotEmpty() }
        )
    }

    companion object {
        private const val MAX_ADDRESS = 32
        private const val MAX_TEXT = 10_000
        private const val MAX_SUBJECT = 200
        private const val SEARCH_COLUMNS = 4
        val listed = listOf(IncomingMessageType.SMS, IncomingMessageType.DATA_SMS, IncomingMessageType.MMS_DOWNLOADED)

        fun typeOf(type: IncomingMessageType): MessageType = when (type) {
            IncomingMessageType.SMS -> MessageType.Sms
            IncomingMessageType.DATA_SMS -> MessageType.Data
            IncomingMessageType.MMS, IncomingMessageType.MMS_DOWNLOADED -> MessageType.Mms
        }

        private fun storedTypes(type: MessageType): List<IncomingMessageType> = when (type) {
            MessageType.Sms -> listOf(IncomingMessageType.SMS)
            MessageType.Data -> listOf(IncomingMessageType.DATA_SMS)
            MessageType.Mms -> listOf(IncomingMessageType.MMS_DOWNLOADED)
        }
    }
}
