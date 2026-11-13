package com.homingsms.app.modules.incoming.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface IncomingMessagesDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(message: IncomingMessage)

    @Query(
        """
        SELECT COUNT(*)
        FROM incoming_messages
        WHERE (:type IS NULL OR type = :type)
          AND createdAt BETWEEN :from AND :to
        """
    )
    suspend fun count(type: IncomingMessageType?, from: Long, to: Long): Int

    @Query(
        """
        SELECT *
        FROM incoming_messages
        WHERE (:type IS NULL OR type = :type)
          AND createdAt BETWEEN :from AND :to
        ORDER BY createdAt DESC, id DESC
        LIMIT :limit OFFSET :offset
        """
    )
    suspend fun select(type: IncomingMessageType?, from: Long, to: Long, limit: Int, offset: Int): List<IncomingMessage>

    @Query("SELECT * FROM incoming_messages WHERE id = :id LIMIT 1")
    fun selectById(id: String): IncomingMessage?

    @Query(
        """
            DELETE FROM incoming_messages
            WHERE type IN (:types) AND createdAt BETWEEN :from and :to 
        """
    )
    fun delete(from: Long, to: Long, types: Set<IncomingMessageType>)

    @Query("DELETE FROM incoming_messages WHERE createdAt < :until")
    suspend fun truncate(until: Long)
}
