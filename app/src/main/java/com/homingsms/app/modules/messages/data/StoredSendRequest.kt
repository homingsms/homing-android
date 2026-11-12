package com.homingsms.app.modules.messages.data

import com.homingsms.app.data.entities.MessageRecipient
import com.homingsms.app.domain.EntitySource
import com.homingsms.app.domain.ProcessingState

class StoredSendRequest(
    val id: Long,
    val state: ProcessingState,
    val recipients: List<MessageRecipient>,
    source: EntitySource,
    message: Message,
    params: SendParams
) :
    SendRequest(source, message, params)
