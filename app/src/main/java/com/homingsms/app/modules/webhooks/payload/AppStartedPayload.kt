package com.homingsms.app.modules.webhooks.payload

import com.homingsms.app.modules.localserver.domain.SimCard
import kotlinx.serialization.Serializable

@Serializable
data class AppStartedPayload(val simCards: List<SimCard>)
