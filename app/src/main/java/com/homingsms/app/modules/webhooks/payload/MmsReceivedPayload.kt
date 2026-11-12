@file:UseSerializers(DateSerializer::class)

package com.homingsms.app.modules.webhooks.payload

import com.homingsms.app.serialization.DateSerializer
import java.util.Date
import kotlinx.serialization.Serializable
import kotlinx.serialization.UseSerializers

@Serializable
class MmsReceivedPayload(
    override val messageId: String,
    override val sender: String,
    override val recipient: String?,
    override val simNumber: Int?,
    val transactionId: String,
    val subject: String?,
    val size: Long,
    val contentClass: String?,
    val receivedAt: Date
) : MessageEventPayload() {
    @Deprecated("Use sender for received events, recipient for sent events")
    override val phoneNumber: String = sender
}
