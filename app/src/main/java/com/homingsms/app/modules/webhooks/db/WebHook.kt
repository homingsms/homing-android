package com.homingsms.app.modules.webhooks.db

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.homingsms.app.domain.EntitySource
import com.homingsms.app.modules.webhooks.domain.WebHookEvent

@Entity
data class WebHook(
    @PrimaryKey
    val id: String,
    val url: String,
    val event: WebHookEvent,
    val source: EntitySource,
)
