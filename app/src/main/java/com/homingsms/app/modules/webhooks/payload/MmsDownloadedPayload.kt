@file:UseSerializers(DateSerializer::class)

package com.homingsms.app.modules.webhooks.payload

import com.homingsms.app.serialization.DateSerializer
import java.util.Date
import kotlinx.serialization.Serializable
import kotlinx.serialization.UseSerializers

@Serializable
class MmsDownloadedPayload(
    override val messageId: String,
    override val sender: String,
    override val recipient: String?,
    override val simNumber: Int?,
    val body: String?,
    val subject: String?,
    val attachments: List<Attachment>,
    val receivedAt: Date
) : MessageEventPayload() {
    @Deprecated("Use sender for received events, recipient for sent events")
    override val phoneNumber: String = sender

    @Serializable
    class Attachment(val partId: Long, val contentType: String, val name: String?, val size: Long?, val data: String?)
}
