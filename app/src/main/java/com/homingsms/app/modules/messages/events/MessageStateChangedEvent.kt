package com.homingsms.app.modules.messages.events

import com.homingsms.app.domain.EntitySource
import com.homingsms.app.domain.ProcessingState
import com.homingsms.app.modules.events.AppEvent

class MessageStateChangedEvent(
    val id: String,
    val source: EntitySource,
    val phoneNumbers: Set<String>,
    val state: ProcessingState,
    val simNumber: Int?,
    val partsCount: Int?,
    val error: String?
): AppEvent(NAME) {

    companion object {
        const val NAME = "MessageStateChangedEvent"
    }
}
