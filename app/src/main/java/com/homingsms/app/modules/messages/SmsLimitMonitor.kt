package com.homingsms.app.modules.messages

import android.content.Context
import com.homingsms.core.data.db.StatsDao
import com.homingsms.core.model.SmsLimitState
import com.homingsms.core.telephony.SmsLimitDetector
import com.homingsms.core.telephony.SmsOutgoingLimits
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class SmsLimitMonitor(
    private val context: Context,
    private val stats: StatsDao,
    private val clock: () -> Long = System::currentTimeMillis
) {
    private val state = MutableStateFlow(SmsLimitState.Clear)
    private var lastHitAt: Long? = null

    val limitState: StateFlow<SmsLimitState> = state.asStateFlow()

    fun recordLimitHit() {
        lastHitAt = clock()
        refresh()
    }

    fun refresh(): SmsLimitState {
        val now = clock()
        val maxCount = SmsOutgoingLimits.maxCount(context, SmsLimitState.DEFAULT_MAX_COUNT)
        val windowMs = SmsOutgoingLimits.intervalMs(context, SmsLimitState.DEFAULT_WINDOW_MS)
        val evaluated = SmsLimitDetector.evaluate(
            maxCount = maxCount,
            windowMs = windowMs,
            sentInWindow = stats.processedSince(now - windowMs),
            lastHitAt = lastHitAt,
            oldestSendingSince = stats.oldestSendingAt(),
            now = now
        )
        lastHitAt = evaluated.limitHitAt
        state.value = evaluated
        return evaluated
    }
}
