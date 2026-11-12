package com.homingsms.app.modules.gateway

import android.util.Log
import com.homingsms.app.domain.EntitySource
import com.homingsms.app.modules.device.events.DeviceKeyRotatedEvent
import com.homingsms.app.modules.events.EventBus
import com.homingsms.app.modules.events.EventsReceiver
import com.homingsms.app.modules.gateway.events.DeviceRegisteredEvent
import com.homingsms.app.modules.gateway.events.MessageCancelledEvent
import com.homingsms.app.modules.gateway.events.MessageEnqueuedEvent
import com.homingsms.app.modules.gateway.events.SettingsUpdatedEvent
import com.homingsms.app.modules.gateway.events.WebhooksUpdatedEvent
import com.homingsms.app.modules.gateway.services.SSEForegroundService
import com.homingsms.app.modules.gateway.workers.PullMessagesWorker
import com.homingsms.app.modules.gateway.workers.RegistrationWorker
import com.homingsms.app.modules.gateway.workers.SendStateWorker
import com.homingsms.app.modules.gateway.workers.SettingsUpdateWorker
import com.homingsms.app.modules.gateway.workers.WebhooksUpdateWorker
import com.homingsms.app.modules.messages.MessagesService
import com.homingsms.app.modules.messages.events.MessageStateChangedEvent
import com.homingsms.app.modules.ping.events.PingEvent
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import org.koin.core.component.get

class EventsReceiver : EventsReceiver() {

    private val settings = get<GatewaySettings>()

    override suspend fun collect(eventBus: EventBus) {
        coroutineScope {
            launch {
                Log.d("EventsReceiver", "launched MessageEnqueuedEvent")
                eventBus.collect<MessageEnqueuedEvent> { event ->
                    try {
                        Log.d("EventsReceiver", "Event: $event")

                        if (!settings.enabled) return@collect

                        PullMessagesWorker.start(get())
                    } catch (e: CancellationException) {
                        throw e
                    } catch (e: Exception) {
                        // per-collector isolation: an exception here must not cancel
                        // the coroutineScope and kill sibling collectors
                        Log.e("EventsReceiver", "MessageEnqueuedEvent collector failed", e)
                    }
                }
            }
            launch {
                Log.d("EventsReceiver", "launched MessageStateChangedEvent")
                val allowedSources = setOf(EntitySource.Cloud, EntitySource.Gateway)
                eventBus.collect<MessageStateChangedEvent> { event ->
                    try {
                        Log.d("EventsReceiver", "Event: $event")

                        if (!settings.enabled) return@collect

                        if (event.source !in allowedSources) return@collect

                        SendStateWorker.start(get(), event.id)
                    } catch (e: CancellationException) {
                        throw e
                    } catch (e: Exception) {
                        // per-collector isolation: an exception here must not cancel
                        // the coroutineScope and kill sibling collectors
                        Log.e("EventsReceiver", "MessageStateChangedEvent collector failed", e)
                    }
                }
            }

            launch {
                Log.d("EventsReceiver", "launched PingEvent")
                eventBus.collect<PingEvent> {
                    try {
                        Log.d("EventsReceiver", "Event: $it")

                        if (!settings.enabled) return@collect

                        PullMessagesWorker.start(get())
                    } catch (e: CancellationException) {
                        throw e
                    } catch (e: Exception) {
                        // per-collector isolation: an exception here must not cancel
                        // the coroutineScope and kill sibling collectors
                        Log.e("EventsReceiver", "PingEvent collector failed", e)
                    }
                }
            }

            launch {
                Log.d("EventsReceiver", "launched WebhooksUpdatedEvent")
                eventBus.collect<WebhooksUpdatedEvent> {
                    try {
                        Log.d("EventsReceiver", "Event: $it")

                        if (!settings.enabled) return@collect

                        WebhooksUpdateWorker.start(get())
                    } catch (e: CancellationException) {
                        throw e
                    } catch (e: Exception) {
                        // per-collector isolation: an exception here must not cancel
                        // the coroutineScope and kill sibling collectors
                        Log.e("EventsReceiver", "WebhooksUpdatedEvent collector failed", e)
                    }
                }
            }

            launch {
                Log.d("EventsReceiver", "launched SettingsUpdatedEvent")
                eventBus.collect<SettingsUpdatedEvent> {
                    try {
                        Log.d("EventsReceiver", "Event: $it")

                        if (!settings.enabled) return@collect

                        SettingsUpdateWorker.start(get())
                    } catch (e: CancellationException) {
                        throw e
                    } catch (e: Exception) {
                        // per-collector isolation: an exception here must not cancel
                        // the coroutineScope and kill sibling collectors
                        Log.e("EventsReceiver", "SettingsUpdatedEvent collector failed", e)
                    }
                }
            }

            launch {
                Log.d("EventsReceiver", "launched DeviceRegisteredEvent")
                eventBus.collect<DeviceRegisteredEvent> {
                    try {
                        Log.d("EventsReceiver", "Event: $it")

                        if (!settings.enabled) return@collect

                        if (settings.notificationChannel == GatewaySettings.NotificationChannel.SSE_ONLY) {
                            // The user pinned the SSE transport: a Firebase token refresh
                            // must not switch the device over to FCM and tear the stream
                            // down. The push token is only refreshed in the background,
                            // it is not a transport switch.
                            SSEForegroundService.start(get())
                            return@collect
                        }

                        if (settings.fcmToken != null) {
                            // FCM is the active transport; tear down the SSE fallback so
                            // the same cloud command is not delivered twice.
                            SSEForegroundService.stop(get())
                        } else {
                            SSEForegroundService.start(get())
                        }
                    } catch (e: CancellationException) {
                        throw e
                    } catch (e: Exception) {
                        // per-collector isolation: an exception here must not cancel
                        // the coroutineScope and kill sibling collectors
                        Log.e("EventsReceiver", "DeviceRegisteredEvent collector failed", e)
                    }
                }
            }

            launch {
                Log.d("EventsReceiver", "launched MessageCancelledEvent")
                eventBus.collect<MessageCancelledEvent> { event ->
                    try {
                        Log.d("EventsReceiver", "Event: $event")

                        if (!settings.enabled) return@collect

                        try {
                            get<MessagesService>().cancelMessage(event.messageId)
                        } catch (_: IllegalArgumentException) {
                            // message not found locally — nothing to cancel
                        } catch (_: IllegalStateException) {
                            // message not in Pending state — already sent/cancelled
                        }
                    } catch (e: CancellationException) {
                        throw e
                    } catch (e: Exception) {
                        // per-collector isolation: a non-IAE/ISE exception must not
                        // cancel the coroutineScope and kill sibling collectors
                        Log.e("EventsReceiver", "MessageCancelledEvent collector failed", e)
                    }
                }
            }

            launch {
                Log.d("EventsReceiver", "launched DeviceKeyRotatedEvent")
                eventBus.collect<DeviceKeyRotatedEvent> {
                    Log.d("EventsReceiver", "Event: $it")

                    if (!settings.enabled) return@collect

                    RegistrationWorker.start(get(), settings.fcmToken)
                }
            }
        }

    }
}
