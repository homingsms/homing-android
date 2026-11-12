package com.homingsms.app.modules.events

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable


@Serializable

enum class ExternalEventType {
    @SerialName("MessageEnqueued")
    MessageEnqueued,

    @SerialName("WebhooksUpdated")
    WebhooksUpdated,

    @SerialName("MessagesExportRequested")
    MessagesExportRequested,

    @SerialName("SettingsUpdated")
    SettingsUpdated,

    @SerialName("MessageCancelled")
    MessageCancelled,
}
