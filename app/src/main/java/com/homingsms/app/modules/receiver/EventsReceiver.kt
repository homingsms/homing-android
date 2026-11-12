package com.homingsms.app.modules.receiver

import android.util.Log
import com.homingsms.app.modules.events.EventBus
import com.homingsms.app.modules.events.EventsReceiver
import com.homingsms.app.modules.gateway.GatewaySettings
import com.homingsms.app.modules.receiver.events.MessagesExportRequestedEvent
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import org.koin.core.component.get
import org.koin.core.component.inject

class EventsReceiver : EventsReceiver() {
    private val service by inject<ReceiverService>()
    private val gatewaySettings by inject<GatewaySettings>()

    override suspend fun collect(eventBus: EventBus) {
        coroutineScope {
            launch {
                Log.d("EventsReceiver", "launched MessagesExportRequestedEvent")
                eventBus.collect<MessagesExportRequestedEvent> { event ->
                    Log.d("EventsReceiver", "Event: $event")

                    // This receiver is not stopped by GatewayService.stop(), so it keeps
                    // collecting after unregister. Drop the command unless the device is
                    // still registered with the cloud server.
                    if (gatewaySettings.registrationInfo == null) {
                        Log.d("EventsReceiver", "Export request ignored: device is not registered")
                        return@collect
                    }

                    service.export(
                        context = get(),
                        period = event.since to event.until,
                        messageTypes = event.messageTypes,
                        webhookDelivery = event.webhookDelivery
                    )
                }
            }
        }
    }
}
