package com.homingsms.app.modules.cloud

import com.homingsms.core.data.cloud.SyncOutcome
import kotlinx.coroutines.CancellationException

internal object CloudFlush {
    suspend fun now(flush: suspend () -> SyncOutcome, defer: () -> Unit) {
        val outcome = try {
            flush()
        } catch (e: CancellationException) {
            defer()
            throw e
        } catch (_: Exception) {
            null
        }
        if (outcome == null || outcome is SyncOutcome.Retry) defer()
    }
}
