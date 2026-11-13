package com.homingsms.core.data.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.homingsms.app.data.entities.Message

@Entity(
    tableName = "message_api_meta",
    foreignKeys = [
        ForeignKey(entity = Message::class, parentColumns = ["id"], childColumns = ["messageId"], onDelete = ForeignKey.CASCADE)
    ],
    indices = [Index(value = ["idempotencyKey"], unique = true)]
)
data class MessageApiMeta(@PrimaryKey val messageId: String, val idempotencyKey: String?, val requestHash: String?, val metadata: String?)

@Entity(
    tableName = "api_keys",
    indices = [Index(value = ["hash"], unique = true)]
)
data class ApiKeyEntity(
    @PrimaryKey val id: String,
    val name: String,
    val prefix: String,
    val hash: String,
    val scopes: String,
    val createdAt: Long,
    val lastUsedAt: Long?,
    val revokedAt: Long?
)

@Entity(tableName = "webhook_endpoints")
data class WebhookEndpointEntity(
    @PrimaryKey val id: String,
    val url: String,
    val description: String?,
    val events: String,
    val enabled: Boolean,
    val createdAt: Long,
    val updatedAt: Long
)

@Entity(
    tableName = "webhook_deliveries",
    foreignKeys = [
        ForeignKey(
            entity = WebhookEndpointEntity::class,
            parentColumns = ["id"],
            childColumns = ["endpointId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["endpointId", "createdAt"]),
        Index(value = ["status", "nextAttemptAt"])
    ]
)
data class WebhookDeliveryEntity(
    @PrimaryKey val id: String,
    val endpointId: String,
    val eventId: String,
    val eventType: String,
    @ColumnInfo(typeAffinity = ColumnInfo.TEXT)
    val payload: String,
    val status: String,
    val attempts: Int,
    val lastStatusCode: Int?,
    val lastResponseBody: String?,
    val lastError: String?,
    val nextAttemptAt: Long?,
    val deliveredAt: Long?,
    val createdAt: Long,
    val updatedAt: Long
)

@Entity(
    tableName = "cloud_state_outbox",
    indices = [Index(value = ["messageId"])]
)
data class CloudStateReportEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val messageId: String,
    val state: String,
    val at: Long,
    val simSlot: Int?,
    val partsCount: Int?,
    val errorCode: String?,
    val errorMessage: String?,
    val attempts: Int,
    val createdAt: Long
)

@Entity(tableName = "cloud_inbox_outbox")
data class CloudInboxOutboxEntity(@PrimaryKey val localId: String, val createdAt: Long, val attempts: Int, val lastError: String?)
