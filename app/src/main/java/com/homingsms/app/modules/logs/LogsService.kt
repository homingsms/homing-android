package com.homingsms.app.modules.logs

import android.content.Context
import com.homingsms.app.modules.logs.db.LogEntriesDao
import com.homingsms.app.modules.logs.db.LogEntry
import com.homingsms.app.modules.logs.workers.TruncateWorker
import com.homingsms.app.serialization.toJsonElement

class LogsService(
    private val dao: LogEntriesDao,
    private val settings: LogsSettings,
) {
    fun start(context: Context) {
        TruncateWorker.start(context)
    }

    fun stop(context: Context) {
        TruncateWorker.stop(context)
    }

    suspend fun select(
        from: Long? = null,
        to: Long? = null
    ): List<LogEntry> {
        val from = from ?: 0
        val to = to ?: System.currentTimeMillis()

        return dao.selectByPeriod(from, to)
    }

    fun insert(
        priority: LogEntry.Priority,
        module: String,
        message: String,
        context: Any? = null
    ) {
        try {
            dao.insert(
                LogEntry(
                    priority,
                    module,
                    message,
                    context = context?.toJsonElement()
                )
            )
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun truncate() {
        val lifetimeDays = settings.lifetimeDays ?: return
        val until = System.currentTimeMillis() - lifetimeDays * 86400000L

        dao.truncate(until)
    }
}
