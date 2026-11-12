package com.homingsms.app

import android.util.Log
import com.homingsms.app.modules.logs.LogsService
import com.homingsms.app.modules.logs.db.LogEntry
import java.lang.Thread.UncaughtExceptionHandler
import org.koin.core.component.KoinComponent

class GlobalExceptionHandler(
    private val defaultHandler: UncaughtExceptionHandler,
    private val logger: LogsService
) : UncaughtExceptionHandler, KoinComponent {

    override fun uncaughtException(thread: Thread, throwable: Throwable) {
        try {
            logger.insert(
                LogEntry.Priority.ERROR,
                "GlobalExceptionHandler",
                "Unhandled exception in ${thread.name}",
                mapOf(
                    "message" to throwable.message,
                    "stackTrace" to throwable.stackTrace.joinToString("\n"),
                    "threadName" to thread.name,
                )
            )
        } catch (e: Exception) {
            Log.e("GlobalExceptionHandler", "Failed to log uncaught exception", e)
        } finally {
            defaultHandler.uncaughtException(thread, throwable)
        }
    }
}
