package com.homingsms.app.modules.messages

import androidx.lifecycle.LiveData
import androidx.lifecycle.distinctUntilChanged
import com.homingsms.app.data.dao.MessagesDao
import com.homingsms.app.data.entities.Message
import com.homingsms.app.data.entities.MessageRecipient
import com.homingsms.app.data.entities.MessageType
import com.homingsms.app.data.entities.MessageWithRecipients
import com.homingsms.app.data.entities.MessagesTotals
import com.homingsms.app.domain.MessageContent
import com.homingsms.app.domain.ProcessingState
import com.homingsms.app.modules.messages.data.SendParams
import com.homingsms.app.modules.messages.data.SendRequest
import com.homingsms.app.modules.messages.data.StoredSendRequest
import com.homingsms.core.model.ProcessingOrder
import java.util.Date

class MessagesRepository(private val dao: MessagesDao, private val machine: MessageStateService) {
    fun selectLast(limit: Int) = dao.selectLast(limit).distinctUntilChanged()

    fun selectLast(limit: Int, state: ProcessingState? = null) = dao.selectLastFiltered(limit, state?.name).distinctUntilChanged()

    val messagesTotals: LiveData<MessagesTotals> = dao.getMessagesStats().distinctUntilChanged()

    fun get(id: String): StoredSendRequest = dao.get(id)?.toRequest()
        ?: throw IllegalArgumentException("Message with id $id not found")

    fun enqueue(request: SendRequest): MessageWithRecipients {
        val message = MessageWithRecipients(
            Message(
                id = request.message.id,
                content = request.message.content,
                withDeliveryReport = request.params.withDeliveryReport,
                simNumber = request.params.simNumber,
                validUntil = request.params.validUntil,
                scheduleAt = request.params.scheduleAt,
                isEncrypted = request.message.isEncrypted,
                skipPhoneValidation = request.params.skipPhoneValidation,
                priority = request.params.priority ?: Message.PRIORITY_DEFAULT,
                source = request.source,

                createdAt = request.message.createdAt.time
            ),
            request.message.phoneNumbers.map {
                MessageRecipient(
                    request.message.id,
                    it,
                    ProcessingState.Pending
                )
            }
        )

        dao.insert(message)

        return message
    }

    suspend fun getPending(order: ProcessingOrder): StoredSendRequest? {
        while (true) {
            val now = Date()
            val message = when (order) {
                ProcessingOrder.LIFO -> dao.getPendingLifo(now)
                ProcessingOrder.FIFO -> dao.getPendingFifo(now)
            } ?: return null

            if (message.state != ProcessingState.Pending) {
                // stored state is not in sync with recipients state -
                // reconciliation goes through the machine (table-gated,
                // per-message lock, silent like the legacy read-side sync)
                machine.syncMessageFromRecipients(message.message.id)
                continue
            }

            return message.toRequest()
        }
    }

    private fun MessageWithRecipients.toRequest(): StoredSendRequest {
        val message = this

        return StoredSendRequest(
            id = message.rowId,
            state = message.state,
            recipients = this.recipients,
            message.message.source,
            com.homingsms.app.modules.messages.data.Message(
                id = message.message.id,
                content = Message.decodeContent(message.message.type, message.message.content),
                phoneNumbers = message.recipients.filter { it.state == ProcessingState.Pending }
                    .map { it.phoneNumber },
                isEncrypted = message.message.isEncrypted,
                createdAt = Date(message.message.createdAt)
            ),
            SendParams(
                withDeliveryReport = message.message.withDeliveryReport,
                skipPhoneValidation = message.message.skipPhoneValidation,
                simNumber = message.message.simNumber,
                validUntil = message.message.validUntil,
                scheduleAt = message.message.scheduleAt,
                priority = message.message.priority
            )
        )
    }
}
