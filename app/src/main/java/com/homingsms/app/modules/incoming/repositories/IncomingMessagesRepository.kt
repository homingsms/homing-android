package com.homingsms.app.modules.incoming.repositories

import com.homingsms.app.modules.incoming.db.IncomingMessage
import com.homingsms.app.modules.incoming.db.IncomingMessageType
import com.homingsms.app.modules.incoming.db.IncomingMessagesDao

class IncomingMessagesRepository(private val dao: IncomingMessagesDao) {
    suspend fun count(type: IncomingMessageType?, from: Long, to: Long): Int = dao.count(type, from, to)

    suspend fun select(type: IncomingMessageType?, from: Long, to: Long, limit: Int, offset: Int): List<IncomingMessage> =
        dao.select(type, from, to, limit, offset)

    fun selectById(id: String): IncomingMessage? = dao.selectById(id)

    fun insert(message: IncomingMessage) = dao.insert(message)

    fun delete(from: Long, to: Long, types: Set<IncomingMessageType>) = dao.delete(from, to, types)
    suspend fun truncate(until: Long) = dao.truncate(until)
}
