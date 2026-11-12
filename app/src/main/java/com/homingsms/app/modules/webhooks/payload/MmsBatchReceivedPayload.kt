package com.homingsms.app.modules.webhooks.payload

import kotlinx.serialization.Serializable

@Serializable
class MmsBatchReceivedPayload(override val messages: List<MmsReceivedPayload>) : BatchEventPayload<MmsReceivedPayload>()
