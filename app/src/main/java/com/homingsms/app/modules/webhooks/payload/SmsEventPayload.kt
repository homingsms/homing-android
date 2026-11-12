@file:UseSerializers(DateSerializer::class)

package com.homingsms.app.modules.webhooks.payload

import com.homingsms.app.serialization.DateSerializer
import java.util.Date
import kotlinx.serialization.Serializable
import kotlinx.serialization.UseSerializers

sealed class SmsEventPayload : MessageEventPayload() {
    @Serializable
    class SmsSent(
        override val messageId: String,
        override val sender: String?,
        override val recipient: String,
        override val simNumber: Int?,
        val partsCount: Int,
        val sentAt: Date
    ) : SmsEventPayload() {
        @Deprecated("Use sender for received events, recipient for sent events")
        override val phoneNumber: String = recipient
    }

    @Serializable
    class SmsDelivered(
        override val messageId: String,
        override val sender: String?,
        override val recipient: String,
        override val simNumber: Int?,
        val deliveredAt: Date
    ) : SmsEventPayload() {
        @Deprecated("Use sender for received events, recipient for sent events")
        override val phoneNumber: String = recipient
    }

    @Serializable
    class SmsFailed(
        override val messageId: String,
        override val sender: String?,
        override val recipient: String,
        override val simNumber: Int?,
        val failedAt: Date,
        val reason: String
    ) : SmsEventPayload() {
        @Deprecated("Use sender for received events, recipient for sent events")
        override val phoneNumber: String = recipient
    }

    @Serializable
    class SmsReceived(
        override val messageId: String,
        override val sender: String,
        override val recipient: String?,
        override val simNumber: Int?,
        val message: String,
        val receivedAt: Date
    ) : SmsEventPayload() {
        @Deprecated("Use sender for received events, recipient for sent events")
        override val phoneNumber: String = sender
    }

    @Serializable
    class SmsDataReceived(
        override val messageId: String,
        override val sender: String,
        override val recipient: String?,
        override val simNumber: Int?,
        val data: String,
        val receivedAt: Date
    ) : SmsEventPayload() {
        @Deprecated("Use sender for received events, recipient for sent events")
        override val phoneNumber: String = sender
    }

    @Serializable
    class SmsCancelled(
        override val messageId: String,
        override val sender: String?,
        override val recipient: String,
        override val simNumber: Int?,
        val cancelledAt: Date
    ) : SmsEventPayload() {
        @Deprecated("Use sender for received events, recipient for sent events")
        override val phoneNumber: String = recipient
    }
}
