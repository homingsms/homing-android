package com.homingsms.core.telephony

import com.homingsms.core.model.SmsLimitState

object SmsLimitDetector {
    const val PROMPT_SUSPECT_AFTER_MS = 30_000L

    fun evaluate(maxCount: Int, windowMs: Long, sentInWindow: Int, lastHitAt: Long?, oldestSendingSince: Long?, now: Long): SmsLimitState {
        val atLimit = maxCount > 0 && sentInWindow >= maxCount
        val stuck = oldestSendingSince != null && now - oldestSendingSince >= PROMPT_SUSPECT_AFTER_MS
        val hitAt = when {
            atLimit && stuck -> now
            else -> lastHitAt
        }
        return SmsLimitState(maxCount = maxCount, windowMs = windowMs, sentInWindow = sentInWindow, limitHitAt = hitAt)
    }
}
