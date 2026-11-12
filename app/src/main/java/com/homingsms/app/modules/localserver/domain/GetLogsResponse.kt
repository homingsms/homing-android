@file:UseSerializers(DateSerializer::class)

package com.homingsms.app.modules.localserver.domain

import com.homingsms.app.modules.logs.db.LogEntry
import com.homingsms.app.serialization.DateSerializer
import java.util.Date
import kotlinx.serialization.Serializable
import kotlinx.serialization.UseSerializers
import kotlinx.serialization.json.JsonElement

@Serializable
data class GetLogsResponse(
    val priority: LogEntry.Priority,
    val module: String,
    val message: String,
    val id: Long = 0,
    val context: JsonElement? = null,
    val createdAt: Date,
) {
    companion object {
        fun from(log: LogEntry) = GetLogsResponse(
            priority = log.priority,
            module = log.module,
            message = log.message,
            id = log.id,
            context = log.context,
            createdAt = Date(log.createdAt)
        )
    }
}
