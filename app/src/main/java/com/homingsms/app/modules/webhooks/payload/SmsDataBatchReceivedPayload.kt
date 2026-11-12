package com.homingsms.app.modules.webhooks.payload

import kotlinx.serialization.Serializable

@Serializable
class SmsDataBatchReceivedPayload(override val messages: List<SmsEventPayload.SmsDataReceived>) :
    BatchEventPayload<SmsEventPayload.SmsDataReceived>()
