package com.homingsms.app.modules.receiver

import com.homingsms.app.modules.cloud.CloudController
import com.homingsms.app.modules.incoming.db.IncomingMessage
import com.homingsms.app.modules.webhooks.WebhooksService
import com.homingsms.core.data.repository.InboxRecords
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

class InboxDispatcher(private val webhooks: WebhooksService, private val cloud: CloudController, private val scope: CoroutineScope) {
    fun dispatch(incoming: IncomingMessage, triggerWebhooks: Boolean) {
        if (incoming.type !in InboxRecords.listed) return
        scope.launch {
            if (triggerWebhooks) webhooks.emitInbox(incoming.id)
            cloud.queueInbox(incoming.id)
        }
    }
}
