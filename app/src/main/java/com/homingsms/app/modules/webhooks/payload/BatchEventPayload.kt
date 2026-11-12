package com.homingsms.app.modules.webhooks.payload

abstract class BatchEventPayload<T : MessageEventPayload> {
    abstract val messages: List<T>
}
