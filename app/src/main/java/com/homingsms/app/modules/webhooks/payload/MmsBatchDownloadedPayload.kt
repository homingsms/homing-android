package com.homingsms.app.modules.webhooks.payload

import kotlinx.serialization.Serializable

@Serializable
class MmsBatchDownloadedPayload(override val messages: List<MmsDownloadedPayload>) : BatchEventPayload<MmsDownloadedPayload>()
