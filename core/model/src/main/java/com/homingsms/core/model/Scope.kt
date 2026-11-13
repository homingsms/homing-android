package com.homingsms.core.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class Scope(val value: String) {
    @SerialName("messages:send")
    MessagesSend("messages:send"),

    @SerialName("messages:read")
    MessagesRead("messages:read"),

    @SerialName("inbox:read")
    InboxRead("inbox:read"),

    @SerialName("webhooks:manage")
    WebhooksManage("webhooks:manage"),

    @SerialName("devices:manage")
    DevicesManage("devices:manage"),

    @SerialName("admin")
    Admin("admin")
    ;

    companion object {
        fun fromValue(value: String): Scope? = entries.firstOrNull { it.value == value }
    }
}

fun Set<Scope>.grants(required: Scope): Boolean = Scope.Admin in this || required in this
