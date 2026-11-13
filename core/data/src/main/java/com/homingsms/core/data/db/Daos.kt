package com.homingsms.core.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.RawQuery
import androidx.room.Transaction
import androidx.room.Update
import androidx.sqlite.db.SupportSQLiteQuery
import com.homingsms.app.data.entities.Message
import com.homingsms.app.data.entities.MessageRecipient
import com.homingsms.app.data.entities.MessageState
import com.homingsms.app.data.entities.MessageWithRecipients
import com.homingsms.app.data.entities.RecipientState
import com.homingsms.app.modules.incoming.db.IncomingMessage
import kotlinx.coroutines.flow.Flow

@Dao
interface ApiMessagesDao {
    @Transaction
    @RawQuery(observedEntities = [Message::class, MessageRecipient::class, MessageState::class])
    suspend fun query(query: SupportSQLiteQuery): List<MessageWithRecipients>

    @Transaction
    @Query("SELECT *, `rowid` FROM message WHERE id = :id")
    suspend fun get(id: String): MessageWithRecipients?

    @Query("SELECT * FROM message_api_meta WHERE messageId IN (:ids)")
    suspend fun meta(ids: List<String>): List<MessageApiMeta>

    @Query("SELECT * FROM message_api_meta WHERE idempotencyKey = :key")
    suspend fun findByIdempotencyKey(key: String): MessageApiMeta?

    @Insert
    suspend fun insertMeta(meta: MessageApiMeta)

    @Query("SELECT * FROM RecipientState WHERE messageId = :id ORDER BY updatedAt ASC")
    suspend fun recipientStates(id: String): List<RecipientState>
}

@Dao
interface ApiInboxDao {
    @RawQuery(observedEntities = [IncomingMessage::class])
    suspend fun query(query: SupportSQLiteQuery): List<IncomingMessage>

    @Query("SELECT * FROM incoming_messages WHERE publicId = :publicId LIMIT 1")
    suspend fun getByPublicId(publicId: String): IncomingMessage?

    @Query("SELECT * FROM incoming_messages WHERE id IN (:ids)")
    suspend fun getByLocalIds(ids: List<String>): List<IncomingMessage>
}

@Dao
interface ApiKeysDao {
    @Query("SELECT * FROM api_keys WHERE revokedAt IS NULL ORDER BY createdAt ASC")
    fun observeActive(): Flow<List<ApiKeyEntity>>

    @Query("SELECT * FROM api_keys WHERE revokedAt IS NULL ORDER BY createdAt ASC")
    suspend fun active(): List<ApiKeyEntity>

    @Query("SELECT * FROM api_keys WHERE hash = :hash LIMIT 1")
    suspend fun findByHash(hash: String): ApiKeyEntity?

    @Insert
    suspend fun insert(entity: ApiKeyEntity)

    @Query("UPDATE api_keys SET revokedAt = :at WHERE id = :id AND revokedAt IS NULL")
    suspend fun revoke(id: String, at: Long): Int

    @Query("UPDATE api_keys SET lastUsedAt = :at WHERE id = :id")
    suspend fun touch(id: String, at: Long)
}

@Dao
interface WebhookEndpointsDao {
    @Query("SELECT * FROM webhook_endpoints ORDER BY createdAt ASC, id ASC")
    fun observeAll(): Flow<List<WebhookEndpointEntity>>

    @Query("SELECT * FROM webhook_endpoints ORDER BY createdAt ASC, id ASC")
    suspend fun all(): List<WebhookEndpointEntity>

    @Query("SELECT * FROM webhook_endpoints WHERE id = :id")
    suspend fun get(id: String): WebhookEndpointEntity?

    @Insert
    suspend fun insert(entity: WebhookEndpointEntity)

    @Update
    suspend fun update(entity: WebhookEndpointEntity)

    @Query("DELETE FROM webhook_endpoints WHERE id = :id")
    suspend fun delete(id: String): Int
}

@Dao
interface WebhookDeliveriesDao {
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(entities: List<WebhookDeliveryEntity>)

    @Update
    suspend fun update(entity: WebhookDeliveryEntity)

    @Query("SELECT * FROM webhook_deliveries WHERE id = :id")
    suspend fun get(id: String): WebhookDeliveryEntity?

    @RawQuery(observedEntities = [WebhookDeliveryEntity::class])
    suspend fun query(query: SupportSQLiteQuery): List<WebhookDeliveryEntity>

    @Query(
        "SELECT * FROM webhook_deliveries WHERE status IN ('pending', 'retrying') AND (nextAttemptAt IS NULL OR nextAttemptAt <= :now) " +
            "ORDER BY nextAttemptAt ASC, createdAt ASC LIMIT :limit"
    )
    suspend fun due(now: Long, limit: Int): List<WebhookDeliveryEntity>

    @Query("SELECT MIN(nextAttemptAt) FROM webhook_deliveries WHERE status IN ('pending', 'retrying')")
    suspend fun nextAttemptAt(): Long?

    @Query("SELECT * FROM webhook_deliveries WHERE endpointId = :endpointId ORDER BY createdAt DESC, id DESC LIMIT :limit")
    fun observeRecent(endpointId: String, limit: Int): Flow<List<WebhookDeliveryEntity>>

    @Query("DELETE FROM webhook_deliveries WHERE createdAt < :until AND status IN ('succeeded', 'failed')")
    suspend fun truncate(until: Long)
}

@Dao
interface CloudOutboxDao {
    @Insert
    suspend fun insertState(entity: CloudStateReportEntity): Long

    @Query("SELECT * FROM cloud_state_outbox ORDER BY id ASC LIMIT :limit")
    suspend fun states(limit: Int): List<CloudStateReportEntity>

    @Query("DELETE FROM cloud_state_outbox WHERE id IN (:ids)")
    suspend fun deleteStates(ids: List<Long>)

    @Query("UPDATE cloud_state_outbox SET attempts = attempts + 1 WHERE id IN (:ids)")
    suspend fun incrementStateAttempts(ids: List<Long>)

    @Query("SELECT COUNT(*) FROM cloud_state_outbox")
    fun observeStateCount(): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertInbox(entity: CloudInboxOutboxEntity): Long

    @Query("SELECT * FROM cloud_inbox_outbox ORDER BY createdAt ASC, localId ASC LIMIT :limit")
    suspend fun inbox(limit: Int): List<CloudInboxOutboxEntity>

    @Query("DELETE FROM cloud_inbox_outbox WHERE localId IN (:localIds)")
    suspend fun deleteInbox(localIds: List<String>)

    @Query("UPDATE cloud_inbox_outbox SET attempts = attempts + 1, lastError = :error WHERE localId IN (:localIds)")
    suspend fun markInboxFailed(localIds: List<String>, error: String?)

    @Query("SELECT COUNT(*) FROM cloud_inbox_outbox")
    fun observeInboxCount(): Flow<Int>

    @Query("SELECT * FROM cloud_inbox_outbox WHERE localId = :localId LIMIT 1")
    suspend fun inboxEntry(localId: String): CloudInboxOutboxEntity?

    @Query("DELETE FROM cloud_state_outbox")
    suspend fun clearStates()

    @Query("DELETE FROM cloud_inbox_outbox")
    suspend fun clearInbox()
}

data class CountsRow(val sent: Int, val delivered: Int, val failed: Int)

data class HourRow(val startAt: Long, val sent: Int, val failed: Int, val delivered: Int = 0)

data class HourReceivedRow(val startAt: Long, val received: Int)

data class SimCountRow(val simNumber: Int, val count: Int)

data class SimPartsRow(val simNumber: Int?, val parts: Long)

@Dao
interface StatsDao {
    @Query(
        """
        SELECT
            COALESCE(SUM(CASE WHEN state IN ('Sent', 'Delivered') THEN 1 ELSE 0 END), 0) AS sent,
            COALESCE(SUM(CASE WHEN state = 'Delivered' THEN 1 ELSE 0 END), 0) AS delivered,
            COALESCE(SUM(CASE WHEN state = 'Failed' THEN 1 ELSE 0 END), 0) AS failed
        FROM message
        WHERE COALESCE(processedAt, createdAt) >= :from
        """
    )
    fun observeCounts(from: Long): Flow<CountsRow>

    @Query("SELECT COUNT(*) FROM message WHERE state IN ('Pending', 'Processed')")
    fun observeQueued(): Flow<Int>

    @Query("SELECT COUNT(*) FROM incoming_messages WHERE createdAt >= :from AND type IN ('SMS', 'DATA_SMS', 'MMS_DOWNLOADED')")
    fun observeReceived(from: Long): Flow<Int>

    @Query(
        """
        SELECT (COALESCE(processedAt, createdAt) / 3600000) * 3600000 AS startAt,
            COALESCE(SUM(CASE WHEN state IN ('Sent', 'Delivered') THEN 1 ELSE 0 END), 0) AS sent,
            COALESCE(SUM(CASE WHEN state = 'Failed' THEN 1 ELSE 0 END), 0) AS failed,
            COALESCE(SUM(CASE WHEN state = 'Delivered' THEN 1 ELSE 0 END), 0) AS delivered
        FROM message
        WHERE COALESCE(processedAt, createdAt) >= :from
        GROUP BY startAt
        """
    )
    fun observeHourly(from: Long): Flow<List<HourRow>>

    @Query(
        """
        SELECT (createdAt / 3600000) * 3600000 AS startAt, COUNT(*) AS received
        FROM incoming_messages
        WHERE createdAt >= :from AND type IN ('SMS', 'DATA_SMS', 'MMS_DOWNLOADED')
        GROUP BY startAt
        """
    )
    fun observeHourlyReceived(from: Long): Flow<List<HourReceivedRow>>

    @Query(
        """
        SELECT simNumber AS simNumber, COUNT(*) AS count FROM message
        WHERE processedAt >= :from AND simNumber IS NOT NULL
        GROUP BY simNumber
        """
    )
    fun observeSentPerSim(from: Long): Flow<List<SimCountRow>>

    @Query(
        """
        SELECT simNumber AS simNumber, COUNT(*) AS count FROM message
        WHERE processedAt >= :from AND simNumber IS NOT NULL
        GROUP BY simNumber
        """
    )
    suspend fun sentPerSim(from: Long): List<SimCountRow>

    @Query(
        """
        SELECT m.simNumber AS simNumber, COALESCE(SUM(COALESCE(m.partsCount, 1)), 0) AS parts
        FROM message AS m
        INNER JOIN MessageRecipient AS r ON r.messageId = m.id
        WHERE r.state IN ('Sent', 'Delivered')
            AND m.type IN ('Text', 'Data')
            AND COALESCE(m.processedAt, m.createdAt) >= :from
        GROUP BY m.simNumber
        """
    )
    fun observeSentParts(from: Long): Flow<List<SimPartsRow>>

    @Query("SELECT COUNT(*) FROM message WHERE state IN ('Pending', 'Processed')")
    suspend fun queued(): Int

    @Query("SELECT COUNT(*) FROM message WHERE processedAt >= :from")
    fun processedSince(from: Long): Int

    @Query("SELECT MIN(processedAt) FROM message WHERE state = 'Processed'")
    fun oldestSendingAt(): Long?
}
