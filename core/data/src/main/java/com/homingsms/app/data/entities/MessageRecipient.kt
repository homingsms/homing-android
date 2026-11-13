package com.homingsms.app.data.entities

import androidx.room.Entity
import androidx.room.ForeignKey

@Entity(
    primaryKeys = ["messageId", "phoneNumber"],
    foreignKeys = [
        ForeignKey(entity = Message::class, parentColumns = ["id"], childColumns = ["messageId"], onDelete = ForeignKey.CASCADE)
    ]
)
data class MessageRecipient(
    val messageId: String,
    val phoneNumber: String,
    val state: com.homingsms.app.domain.ProcessingState = com.homingsms.app.domain.ProcessingState.Pending,
    val error: String? = null
)
