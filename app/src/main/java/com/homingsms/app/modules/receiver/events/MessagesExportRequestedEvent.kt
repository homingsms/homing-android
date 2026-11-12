@file:UseSerializers(DateSerializer::class)

package com.homingsms.app.modules.receiver.events

import com.homingsms.app.domain.WebhookDelivery
import com.homingsms.app.modules.events.AppEvent
import com.homingsms.app.modules.incoming.db.IncomingMessageType
import com.homingsms.app.serialization.AppJson
import com.homingsms.app.serialization.DateSerializer
import java.util.Date
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.UseSerializers

class MessagesExportRequestedEvent(
    val since: Date,
    val until: Date,
    val messageTypes: Set<IncomingMessageType>,
    val webhookDelivery: WebhookDelivery,
) : AppEvent(NAME) {
    @Serializable
    data class Payload(
        @SerialName("since")
        val since: Date,
        @SerialName("until")
        val until: Date,
        @SerialName("messageTypes")
        val messageTypes: String? = null,
        @SerialName("triggerWebhooks")
        @Deprecated("Replaced with webhookDelivery")
        val triggerWebhooks: Boolean? = null,
        @SerialName("webhookDelivery")
        val webhookDelivery: WebhookDelivery? = null,
    )

    companion object {
        const val NAME = "MessagesExportRequestedEvent"

        fun withPayload(payload: String): MessagesExportRequestedEvent {
            val obj = AppJson.default.decodeFromString(Payload.serializer(), payload)
            val messageTypes = try {
                obj.messageTypes
                    ?.split(',')
                    ?.map { IncomingMessageType.valueOf(it) }
                    ?.toSet()
            } catch (_: Exception) {
                null
            }

            return MessagesExportRequestedEvent(
                since = obj.since,
                until = obj.until,
                messageTypes = messageTypes ?: setOf(IncomingMessageType.SMS),
                webhookDelivery = obj.webhookDelivery
                    ?: (if (obj.triggerWebhooks == false) WebhookDelivery.Disabled else null)
                    ?: WebhookDelivery.Individual,
            )
        }
    }
}
