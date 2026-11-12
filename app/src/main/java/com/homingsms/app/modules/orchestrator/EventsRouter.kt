package com.homingsms.app.modules.orchestrator

import com.homingsms.app.modules.events.EventBus
import com.homingsms.app.modules.events.ExternalEvent
import com.homingsms.app.modules.events.ExternalEventType
import com.homingsms.app.modules.gateway.events.MessageCancelledEvent
import com.homingsms.app.modules.gateway.events.MessageEnqueuedEvent
import com.homingsms.app.modules.gateway.events.SettingsUpdatedEvent
import com.homingsms.app.modules.gateway.events.WebhooksUpdatedEvent
import com.homingsms.app.modules.receiver.events.MessagesExportRequestedEvent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class EventsRouter(
    private val eventBus: EventBus,
) {
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    fun route(event: ExternalEvent) {
        scope.launch {
            when (event.type) {
                ExternalEventType.MessageEnqueued ->
                    eventBus.emit(
                        MessageEnqueuedEvent()
                    )

                ExternalEventType.WebhooksUpdated ->
                    eventBus.emit(
                        WebhooksUpdatedEvent()
                    )

                ExternalEventType.MessagesExportRequested ->
                    eventBus.emit(
                        MessagesExportRequestedEvent.withPayload(
                            requireNotNull(event.data)
                        )
                    )

                ExternalEventType.SettingsUpdated ->
                    eventBus.emit(
                        SettingsUpdatedEvent()
                    )

                ExternalEventType.MessageCancelled -> {
                    eventBus.emit(
                        MessageCancelledEvent.withPayload(
                            requireNotNull(event.data)
                        )
                    )
                }
            }
        }
    }
}
