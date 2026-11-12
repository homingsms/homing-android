package com.homingsms.app.modules.messages.data

import com.homingsms.app.domain.MessageContent
import java.util.Date

data class Message(
    val id: String,
    val content: MessageContent,
    val phoneNumbers: List<String>,

    val isEncrypted: Boolean,

    val createdAt: Date,
)
