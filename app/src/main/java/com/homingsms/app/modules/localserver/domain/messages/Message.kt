@file:UseSerializers(DateSerializer::class)

package com.homingsms.app.modules.localserver.domain.messages

import com.homingsms.app.domain.ProcessingState
import com.homingsms.app.serialization.DateSerializer
import java.util.Date
import kotlinx.serialization.Serializable
import kotlinx.serialization.UseSerializers

@Serializable
open class Message(
    val id: String,
    val deviceId: String,
    val state: ProcessingState,
    val isHashed: Boolean,
    val isEncrypted: Boolean,
    val scheduleAt: Date?,
    val createdAt: Date,
    val textMessage: TextMessage?,
    val dataMessage: DataMessage?,
    val mmsMessage: MmsMessage?,
    val hashedMessage: HashedMessage?,
    val recipients: List<Recipient>,
    val states: Map<ProcessingState, Date>,
) {
    @Serializable
    data class Recipient(
        val phoneNumber: String,
        val state: ProcessingState,
        val error: String?,
    )
}
