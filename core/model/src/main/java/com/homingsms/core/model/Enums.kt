package com.homingsms.core.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class MessageState(val value: String) {
    @SerialName("scheduled")
    Scheduled("scheduled"),

    @SerialName("pending")
    Pending("pending"),

    @SerialName("leased")
    Leased("leased"),

    @SerialName("queued")
    Queued("queued"),

    @SerialName("sending")
    Sending("sending"),

    @SerialName("sent")
    Sent("sent"),

    @SerialName("delivered")
    Delivered("delivered"),

    @SerialName("failed")
    Failed("failed"),

    @SerialName("cancelled")
    Cancelled("cancelled"),

    @SerialName("expired")
    Expired("expired")
    ;

    companion object {
        fun fromValue(value: String): MessageState? = entries.firstOrNull { it.value == value }
    }
}

@Serializable
enum class MessageType(val value: String) {
    @SerialName("sms")
    Sms("sms"),

    @SerialName("data")
    Data("data"),

    @SerialName("mms")
    Mms("mms")
    ;

    companion object {
        fun fromValue(value: String): MessageType? = entries.firstOrNull { it.value == value }
    }
}

@Serializable
enum class MessagePriority(val value: String) {
    @SerialName("normal")
    Normal("normal"),

    @SerialName("otp")
    Otp("otp")
    ;

    companion object {
        fun fromValue(value: String): MessagePriority? = entries.firstOrNull { it.value == value }
    }
}

@Serializable
enum class WebhookEventType(val value: String, val subscribable: Boolean) {
    @SerialName("message.sent")
    MessageSent("message.sent", true),

    @SerialName("message.delivered")
    MessageDelivered("message.delivered", true),

    @SerialName("message.failed")
    MessageFailed("message.failed", true),

    @SerialName("inbox.received")
    InboxReceived("inbox.received", true),

    @SerialName("device.online")
    DeviceOnline("device.online", true),

    @SerialName("device.offline")
    DeviceOffline("device.offline", true),

    @SerialName("opt_out.received")
    OptOutReceived("opt_out.received", true),

    @SerialName("webhook.test")
    WebhookTest("webhook.test", false)
    ;

    companion object {
        fun fromValue(value: String): WebhookEventType? = entries.firstOrNull { it.value == value }
    }
}

@Serializable
enum class WebhookDeliveryStatus(val value: String) {
    @SerialName("pending")
    Pending("pending"),

    @SerialName("retrying")
    Retrying("retrying"),

    @SerialName("succeeded")
    Succeeded("succeeded"),

    @SerialName("failed")
    Failed("failed")
    ;

    companion object {
        fun fromValue(value: String): WebhookDeliveryStatus? = entries.firstOrNull { it.value == value }
    }
}

@Serializable
enum class NetworkType(val value: String) {
    @SerialName("wifi")
    Wifi("wifi"),

    @SerialName("cellular")
    Cellular("cellular"),

    @SerialName("ethernet")
    Ethernet("ethernet"),

    @SerialName("none")
    None("none"),

    @SerialName("unknown")
    Unknown("unknown")
}
