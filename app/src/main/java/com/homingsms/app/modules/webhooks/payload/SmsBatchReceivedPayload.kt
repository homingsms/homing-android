package com.homingsms.app.modules.webhooks.payload

import kotlinx.serialization.Serializable

@Serializable
class SmsBatchReceivedPayload(override val messages: List<SmsEventPayload.SmsReceived>) : BatchEventPayload<SmsEventPayload.SmsReceived>()
