@file:UseSerializers(DateSerializer::class)

package com.homingsms.app.modules.localserver.domain

import com.homingsms.app.domain.WebhookDelivery
import com.homingsms.app.modules.incoming.db.IncomingMessageType
import com.homingsms.app.serialization.DateSerializer
import java.util.Date
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.UseSerializers

@Serializable
data class InboxRefreshRequest(
    @SerialName("since")
    private val _since: Date? = null,
    @SerialName("until")
    private val _until: Date? = null,
    val messageTypes: Set<IncomingMessageType>? = null,
    @Deprecated("Replaced with webhookDelivery")
    val triggerWebhooks: Boolean? = null,
    val webhookDelivery: WebhookDelivery? = null,
) {
    val since: Date
        get() = _since ?: throw IllegalArgumentException("since is required")

    val until: Date
        get() = _until ?: throw IllegalArgumentException("until is required")

    val period: Pair<Date, Date>
        get() = since to until

    fun validate(): InboxRefreshRequest {
        if (since.after(until)) {
            throw IllegalArgumentException("since must be before until")
        }

        val allowed = setOf(IncomingMessageType.SMS, IncomingMessageType.MMS)
        if (messageTypes != null && messageTypes.isEmpty()) {
            throw IllegalArgumentException("messageTypes must not be empty")
        }

        val invalidTypes = (messageTypes ?: emptySet()) - allowed
        if (invalidTypes.isNotEmpty()) {
            throw IllegalArgumentException(
                "messageTypes contains unsupported values: ${invalidTypes.joinToString(",")}"
            )
        }
        return this
    }
}
