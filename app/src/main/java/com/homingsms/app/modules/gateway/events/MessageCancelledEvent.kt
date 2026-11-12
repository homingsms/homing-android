package com.homingsms.app.modules.gateway.events

import com.homingsms.app.modules.events.AppEvent
import com.homingsms.app.serialization.AppJson
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

class MessageCancelledEvent(val messageId: String) : AppEvent(NAME) {
    @Serializable
    data class Payload(
        @SerialName("messageId")
        val messageId: String
    )

    companion object {
        fun withPayload(payload: String): MessageCancelledEvent {
            val obj = AppJson.default.decodeFromString(Payload.serializer(), payload)

            return MessageCancelledEvent(
                obj.messageId,
            )
        }

        const val NAME = "MessageCancelledEvent"
    }
}
