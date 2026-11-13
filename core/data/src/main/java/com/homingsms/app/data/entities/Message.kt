package com.homingsms.app.data.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.homingsms.app.domain.EntitySource
import com.homingsms.app.domain.MessageContent
import com.homingsms.app.domain.ProcessingState
import com.homingsms.app.serialization.AppJson
import java.util.Date

enum class MessageType {
    Text,
    Data,
    Mms,
}

@Entity(
    indices = [
        androidx.room.Index(value = ["createdAt"]),
        androidx.room.Index(value = ["state", "processedAt"]),
        androidx.room.Index(value = ["state", "createdAt"]),
        androidx.room.Index(value = ["state", "scheduleAt"]),
    ]
)
data class Message constructor(
    @PrimaryKey val id: String,

    @ColumnInfo(defaultValue = "1")
    val withDeliveryReport: Boolean,
    val simNumber: Int?,
    val validUntil: Date?,
    val scheduleAt: Date?,
    @ColumnInfo(defaultValue = "0")
    val isEncrypted: Boolean,
    @ColumnInfo(defaultValue = "0")
    val skipPhoneValidation: Boolean,
    @ColumnInfo(defaultValue = "0")
    val priority: Byte,

    @ColumnInfo(defaultValue = "Local")
    val source: EntitySource,

    @ColumnInfo(defaultValue = "Text")
    val type: MessageType = MessageType.Text,

    val content: String,

    val state: ProcessingState = ProcessingState.Pending,
    val partsCount: Int? = null,
    @ColumnInfo(defaultValue = "0")
    val createdAt: Long = System.currentTimeMillis(),
    val processedAt: Long? = null,
) {
    constructor(
        id: String,
        withDeliveryReport: Boolean,
        simNumber: Int?,
        validUntil: Date?,
        scheduleAt: Date?,
        isEncrypted: Boolean,
        skipPhoneValidation: Boolean,
        priority: Byte,
        source: EntitySource,

        content: MessageContent,

        createdAt: Long,
    ) : this(
        id,
        withDeliveryReport,
        simNumber,
        validUntil,
        scheduleAt,
        isEncrypted,
        skipPhoneValidation,
        priority,
        source,

        content = encodeContent(content),
        type = when (content) {
            is MessageContent.Text -> MessageType.Text
            is MessageContent.Data -> MessageType.Data
            is MessageContent.Mms -> MessageType.Mms
        },
        createdAt = createdAt,
    )

    val textContent: MessageContent.Text?
        get() = when (type) {
            MessageType.Text -> AppJson.default.decodeFromString(MessageContent.Text.serializer(), content)
            else -> null
        }

    val dataContent: MessageContent.Data?
        get() = when (type) {
            MessageType.Data -> AppJson.default.decodeFromString(MessageContent.Data.serializer(), content)
            else -> null
        }

    val mmsContent: MessageContent.Mms?
        get() = when (type) {
            MessageType.Mms -> AppJson.default.decodeFromString(MessageContent.Mms.serializer(), content)
            else -> null
        }

    companion object {
        const val PRIORITY_MIN: Byte = Byte.MIN_VALUE
        const val PRIORITY_DEFAULT: Byte = 0
        const val PRIORITY_EXPEDITED: Byte = 100

        fun encodeContent(content: MessageContent): String = when (content) {
            is MessageContent.Text -> AppJson.default.encodeToString(MessageContent.Text.serializer(), content)
            is MessageContent.Data -> AppJson.default.encodeToString(MessageContent.Data.serializer(), content)
            is MessageContent.Mms -> AppJson.default.encodeToString(MessageContent.Mms.serializer(), content)
        }

        fun decodeContent(type: MessageType, content: String): MessageContent = when (type) {
            MessageType.Text -> AppJson.default.decodeFromString(MessageContent.Text.serializer(), content)
            MessageType.Data -> AppJson.default.decodeFromString(MessageContent.Data.serializer(), content)
            MessageType.Mms -> AppJson.default.decodeFromString(MessageContent.Mms.serializer(), content)
        }
    }
}
