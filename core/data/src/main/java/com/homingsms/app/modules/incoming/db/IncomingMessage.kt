package com.homingsms.app.modules.incoming.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

enum class IncomingMessageType {
    SMS,
    DATA_SMS,
    MMS,
    MMS_DOWNLOADED
}

@Entity(
    tableName = "incoming_messages",
    indices = [
        Index(value = ["createdAt"]),
        Index(value = ["type"]),
        Index(value = ["publicId"], unique = true)
    ]
)
data class IncomingMessage(
    @PrimaryKey val id: String,
    val type: IncomingMessageType,
    val sender: String,
    val recipient: String?,
    val simNumber: Int?,
    val subscriptionId: Int?,
    val contentPreview: String,
    val createdAt: Long = System.currentTimeMillis(),
    @ColumnInfo(defaultValue = "")
    val publicId: String = "",
    val text: String? = null,
    val data: String? = null,
    val port: Int? = null,
    val subject: String? = null,
    @ColumnInfo(defaultValue = "0")
    val insertedAt: Long = createdAt
)
