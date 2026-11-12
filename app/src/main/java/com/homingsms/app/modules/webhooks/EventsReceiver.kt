package com.homingsms.app.modules.webhooks

import android.util.Log
import com.homingsms.app.domain.ProcessingState
import com.homingsms.app.helpers.SubscriptionsHelper
import com.homingsms.app.modules.events.EventBus
import com.homingsms.app.modules.events.EventsReceiver
import com.homingsms.app.modules.messages.events.MessageStateChangedEvent
import com.homingsms.app.modules.ping.events.PingEvent
import com.homingsms.app.modules.webhooks.domain.WebHookEvent
import com.homingsms.app.modules.webhooks.payload.SmsEventPayload
import java.util.Date
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import org.koin.core.component.get

class EventsReceiver : EventsReceiver() {
    override suspend fun collect(eventBus: EventBus) {
        coroutineScope {
            launch {
                eventBus.collect<PingEvent> {
                    Log.d("EventsReceiver", "Event: $it")

                    get<WebHooksService>().emit(
                        get(),
                        WebHookEvent.SystemPing,
                        mapOf("health" to it.health)
                    )
                }
            }

            launch {
                eventBus.collect<MessageStateChangedEvent> { event ->
                    Log.d("EventsReceiver", "Event: $event")

                    val webhookEventType = when (event.state) {
                        ProcessingState.Sent -> WebHookEvent.SmsSent
                        ProcessingState.Delivered -> WebHookEvent.SmsDelivered
                        ProcessingState.Failed -> WebHookEvent.SmsFailed
                        ProcessingState.Cancelled -> WebHookEvent.SmsCancelled
                        else -> return@collect
                    }

                    // Get sender's device number using SubscriptionsHelper
                    val context = get<android.content.Context>()
                    val sender = event.simNumber?.let { simIndex ->
                        SubscriptionsHelper.getPhoneNumber(context, simIndex - 1)
                    }

                    event.phoneNumbers.forEach { destination ->
                        val payload = when (webhookEventType) {
                            WebHookEvent.SmsSent -> SmsEventPayload.SmsSent(
                                messageId = event.id,
                                simNumber = event.simNumber,
                                partsCount = event.partsCount ?: -1,
                                sentAt = Date(),
                                sender = sender,
                                recipient = destination,
                            )

                            WebHookEvent.SmsDelivered -> SmsEventPayload.SmsDelivered(
                                messageId = event.id,
                                simNumber = event.simNumber,
                                deliveredAt = Date(),
                                sender = sender,
                                recipient = destination,
                            )

                            WebHookEvent.SmsFailed -> SmsEventPayload.SmsFailed(
                                messageId = event.id,
                                simNumber = event.simNumber,
                                failedAt = Date(),
                                reason = event.error ?: "Unknown",
                                sender = sender,
                                recipient = destination,
                            )

                            WebHookEvent.SmsCancelled -> SmsEventPayload.SmsCancelled(
                                messageId = event.id,
                                simNumber = event.simNumber,
                                cancelledAt = Date(),
                                sender = sender,
                                recipient = destination,
                            )

                            else -> return@forEach
                        }

                        get<WebHooksService>().emit(
                            get(), webhookEventType, payload
                        )
                    }
                }
            }
        }
    }
}
