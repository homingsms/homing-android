package com.homingsms.app.modules.cloud

import com.homingsms.core.data.cloud.SyncOutcome
import java.io.IOException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class CloudFlushTest {
    private fun deferrals(outcome: suspend () -> SyncOutcome): Int = runTestCount { deferred ->
        CloudFlush.now(outcome) { deferred() }
    }

    private fun runTestCount(block: suspend (() -> Unit) -> Unit): Int {
        var count = 0
        runTest { block { count++ } }
        return count
    }

    @Test
    fun successfulFlushIsNotDeferred() {
        assertEquals(0, deferrals { SyncOutcome.Success })
    }

    @Test
    fun unpairedFlushIsNotDeferred() {
        assertEquals(0, deferrals { SyncOutcome.NotPaired })
        assertEquals(0, deferrals { SyncOutcome.Unpaired })
    }

    @Test
    fun retryableFlushFallsBackToTheWorker() {
        assertEquals(1, deferrals { SyncOutcome.Retry("offline") })
    }

    @Test
    fun unexpectedErrorFallsBackToTheWorker() {
        assertEquals(1, deferrals { throw IllegalStateException("bad body") })
        assertEquals(1, deferrals { throw IOException("reset") })
    }

    @Test
    fun cancellationDefersAndPropagates() {
        var count = 0
        assertThrows(CancellationException::class.java) {
            runTest { CloudFlush.now({ throw CancellationException("stopped") }) { count++ } }
        }
        assertEquals(1, count)
    }
}
