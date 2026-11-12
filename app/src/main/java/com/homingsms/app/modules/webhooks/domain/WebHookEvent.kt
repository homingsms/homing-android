package com.homingsms.app.modules.webhooks.domain

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable


@Serializable

enum class WebHookEvent(val value: String) {
    @SerialName("sms:received")
    SmsReceived("sms:received"),

    @SerialName("sms:batch:received")
    SmsBatchReceived("sms:batch:received"),

    @SerialName("sms:sent")
    SmsSent("sms:sent"),

    @SerialName("sms:delivered")
    SmsDelivered("sms:delivered"),

    @SerialName("sms:failed")
    SmsFailed("sms:failed"),

    @SerialName("system:ping")
    SystemPing("system:ping"),

    @SerialName("sms:data-received")
    SmsDataReceived("sms:data-received"),

    @SerialName("sms:batch:data-received")
    SmsBatchDataReceived("sms:batch:data-received"),

    @SerialName("mms:received")
    MmsReceived("mms:received"),

    @SerialName("mms:batch:received")
    MmsBatchReceived("mms:batch:received"),

    @SerialName("mms:downloaded")
    MmsDownloaded("mms:downloaded"),

    @SerialName("mms:batch:downloaded")
    MmsBatchDownloaded("mms:batch:downloaded"),

    @SerialName("app:started")
    AppStarted("app:started"),

    @SerialName("sms:cancelled")
    SmsCancelled("sms:cancelled");

    fun batchEvent(): WebHookEvent = when (this) {
        SmsReceived -> SmsBatchReceived
        SmsDataReceived -> SmsBatchDataReceived
        MmsReceived -> MmsBatchReceived
        MmsDownloaded -> MmsBatchDownloaded
        else -> throw NotImplementedError(this.name + " doesn't support batching")
    }
}
