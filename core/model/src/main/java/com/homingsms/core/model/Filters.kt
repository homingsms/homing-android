package com.homingsms.core.model

data class MessageFilter(
    val state: MessageState? = null,
    val type: MessageType? = null,
    val to: String? = null,
    val createdFrom: Long? = null,
    val createdTo: Long? = null
)

data class InboxFilter(
    val type: MessageType? = null,
    val from: String? = null,
    val receivedFrom: Long? = null,
    val receivedTo: Long? = null
)
