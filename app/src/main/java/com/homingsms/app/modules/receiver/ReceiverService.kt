package com.homingsms.app.modules.receiver

import android.content.Context
import com.homingsms.app.modules.incoming.IncomingMessagesService
import com.homingsms.app.modules.logs.LogsService
import com.homingsms.app.modules.logs.db.LogEntry
import com.homingsms.app.modules.receiver.data.InboxMessage
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class ReceiverService : KoinComponent {
    private val logsService: LogsService by inject()
    private val incomingMessagesService: IncomingMessagesService by inject()
    private val receiverSettings: ReceiverSettings by inject()
    private val inboxDispatcher: InboxDispatcher by inject()

    private val mmsContentObserver by lazy { MmsContentObserver() }
    private val smsContentObserver by lazy { SmsContentObserver() }

    fun start(context: Context) {
        MessagesReceiver.start(context)
        MmsReceiver.start(context)
        mmsContentObserver.start()
        if (receiverSettings.contentProviderEnabled) {
            smsContentObserver.start()
        }
    }

    fun stop(context: Context) {
        smsContentObserver.stop()
        mmsContentObserver.stop()
        MmsReceiver.stop(context)
        MessagesReceiver.stop(context)
    }

    fun process(message: InboxMessage, triggerWebhooks: Boolean) {
        if (incomingMessagesService.isMessageProcessed(message)) {
            logsService.insert(
                LogEntry.Priority.DEBUG,
                MODULE_NAME,
                "ReceiverService::process - duplicate message, skipping",
                mapOf("messageType" to message.javaClass.simpleName, "date" to message.date)
            )
            return
        }

        val incoming = incomingMessagesService.save(message)
        inboxDispatcher.dispatch(incoming, triggerWebhooks)

        logsService.insert(
            LogEntry.Priority.DEBUG,
            MODULE_NAME,
            "ReceiverService::process - message processed",
            mapOf("messageType" to message.javaClass.simpleName, "id" to incoming.id)
        )
    }
}
