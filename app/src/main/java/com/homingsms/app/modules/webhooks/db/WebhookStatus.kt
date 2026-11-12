package com.homingsms.app.modules.webhooks.db

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable


/**
 * Enumeration representing webhook queue processing status.
 */
@Serializable
enum class WebhookStatus(val value: String, val displayName: String) {
    @SerialName("pending")
    PENDING("pending", "Pending"),

    @SerialName("processing")
    PROCESSING("processing", "Processing"),

    @SerialName("completed")
    COMPLETED("completed", "Completed"),

    @SerialName("failed")
    FAILED("failed", "Failed"),

    @SerialName("permanently_failed")
    PERMANENTLY_FAILED("permanently_failed", "Permanently Failed");

    companion object {
        fun fromValue(value: String): WebhookStatus? {
            return values().find { it.value == value }
        }

        fun getDefault(): WebhookStatus = PENDING

        fun isProcessing(status: WebhookStatus): Boolean {
            return status == PENDING || status == PROCESSING
        }

        fun isTerminal(status: WebhookStatus): Boolean {
            return status == COMPLETED || status == PERMANENTLY_FAILED
        }
    }
}
