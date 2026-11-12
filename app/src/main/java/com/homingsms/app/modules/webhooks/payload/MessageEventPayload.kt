package com.homingsms.app.modules.webhooks.payload

abstract class MessageEventPayload {
    abstract val messageId: String
    abstract val sender: String?
    abstract val recipient: String?
    abstract val simNumber: Int?

    @Deprecated("Use sender for received events, recipient for sent events")
    abstract val phoneNumber: String
}
