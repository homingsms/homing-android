package com.homingsms.app.modules.messages.data

import com.homingsms.app.domain.EntitySource

open class SendRequest(
    val source: EntitySource,
    val message: Message,
    val params: SendParams,
)
