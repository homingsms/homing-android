package com.homingsms.app.modules.incoming

import android.content.Context
import android.util.Base64
import com.homingsms.app.helpers.SubscriptionsHelper
import com.homingsms.app.modules.incoming.db.IncomingMessage
import com.homingsms.app.modules.incoming.db.IncomingMessageType
import com.homingsms.app.modules.incoming.repositories.IncomingMessagesRepository
import com.homingsms.app.modules.incoming.workers.IncomingLogTruncateWorker
import com.homingsms.app.modules.logs.LogsService
import com.homingsms.app.modules.logs.db.LogEntry
import com.homingsms.app.modules.mms.MmsAttachmentStorage
import com.homingsms.app.modules.receiver.data.InboxMessage
import com.homingsms.core.model.Ids

class IncomingMessagesService(
    private val context: Context,
    private val settings: IncomingMessagesSettings,
    private val repository: IncomingMessagesRepository,
    private val attachmentStorage: MmsAttachmentStorage,
    private val logsService: LogsService
) {
    fun save(message: InboxMessage): IncomingMessage {
        val simSlotIndex = message.subscriptionId?.let {
            SubscriptionsHelper.getSimSlotIndex(context, it)
        }
        val simNumber = simSlotIndex?.let { it + 1 }
        val recipient = simSlotIndex?.let {
            SubscriptionsHelper.getPhoneNumber(context, it)
        }

        return IncomingMessage(
            id = buildId(message),
            type = message.storedType(),
            sender = message.address,
            recipient = recipient,
            simNumber = simNumber,
            subscriptionId = message.subscriptionId,
            contentPreview = message.toPreview(),
            createdAt = message.date.time,
            publicId = Ids.inbox(),
            text = message.fullText(),
            data = (message as? InboxMessage.Data)?.data?.let { Base64.encodeToString(it, Base64.NO_WRAP) },
            port = (message as? InboxMessage.Data)?.port,
            subject = message.subject(),
            insertedAt = System.currentTimeMillis()
        ).also {
            try {
                repository.insert(it)
                if (message is InboxMessage.MMS) {
                    persistMmsAttachments(it.id, message)
                }
            } catch (e: Exception) {
                logsService.insert(
                    LogEntry.Priority.ERROR,
                    MODULE_NAME,
                    "Failed to save message",
                    mapOf(
                        "message" to message,
                        "exception" to e.stackTraceToString()
                    )
                )
            }
        }
    }

    private fun persistMmsAttachments(messageId: String, message: InboxMessage.MMS) {
        message.attachments.forEach { att ->
            val decoded = att.data?.let {
                runCatching {
                    Base64.decode(it, Base64.DEFAULT)
                }.onFailure { e ->
                    logsService.insert(
                        LogEntry.Priority.WARN,
                        MODULE_NAME,
                        "Failed to decode MMS attachment data",
                        mapOf(
                            "messageId" to messageId,
                            "partId" to att.partId,
                            "name" to (att.name ?: ""),
                            "error" to (e.message ?: e.toString())
                        )
                    )
                }.getOrNull()
            }
            if (decoded != null) {
                try {
                    attachmentStorage.store(
                        messageId,
                        att.partId,
                        att.name,
                        att.contentType,
                        decoded
                    )
                } catch (e: Exception) {
                    logsService.insert(
                        LogEntry.Priority.WARN,
                        MODULE_NAME,
                        "Failed to persist MMS attachment",
                        mapOf(
                            "messageId" to message.messageId,
                            "partId" to att.partId,
                            "error" to (e.message ?: e.toString())
                        )
                    )
                }
            }
        }
    }

    suspend fun count(type: IncomingMessageType?, from: Long, to: Long): Int = repository.count(type, from, to)

    suspend fun select(type: IncomingMessageType?, from: Long, to: Long, limit: Int, offset: Int): List<IncomingMessage> =
        repository.select(type, from, to, limit, offset)

    fun getById(id: String): IncomingMessage? = repository.selectById(id)

    fun isMessageProcessed(message: InboxMessage): Boolean = getById(buildId(message)) != null

    fun start(context: Context) {
        settings.lifetimeDays ?: return
        IncomingLogTruncateWorker.start(context)
    }

    fun stop(context: Context) {
        IncomingLogTruncateWorker.stop(context)
    }

    suspend fun truncateLog() {
        val lifetime = settings.lifetimeDays ?: return
        repository.truncate(System.currentTimeMillis() - lifetime * 86400000L)
        attachmentStorage.cleanup(lifetime)
    }

    private fun buildId(message: InboxMessage): String {
        val prefix = when (message) {
            is InboxMessage.Data -> "data:"
            is InboxMessage.MMS -> "mms:"
            is InboxMessage.MmsHeaders -> "mms-header:"
            is InboxMessage.Text -> "text:"
        }

        return prefix + message.hashCode().toString()
    }

    private fun InboxMessage.toPreview(): String = when (this) {
        is InboxMessage.Text -> text

        is InboxMessage.Data -> data?.let { bytes ->
            val preview = bytes.take(64).joinToString(separator = "") { "%02x".format(it) }
            if (bytes.size > 64) "$preview..." else preview
        }
            ?: "Empty data"

        is InboxMessage.MmsHeaders -> subject ?: "MMS notification"

        is InboxMessage.MMS -> body ?: subject ?: "MMS content"
    }

    private fun InboxMessage.storedType(): IncomingMessageType = when (this) {
        is InboxMessage.Text -> IncomingMessageType.SMS
        is InboxMessage.Data -> IncomingMessageType.DATA_SMS
        is InboxMessage.MmsHeaders -> IncomingMessageType.MMS
        is InboxMessage.MMS -> IncomingMessageType.MMS_DOWNLOADED
    }

    private fun InboxMessage.fullText(): String? = when (this) {
        is InboxMessage.Text -> text
        is InboxMessage.MMS -> body
        else -> null
    }

    private fun InboxMessage.subject(): String? = when (this) {
        is InboxMessage.MMS -> subject
        is InboxMessage.MmsHeaders -> subject
        else -> null
    }
}
