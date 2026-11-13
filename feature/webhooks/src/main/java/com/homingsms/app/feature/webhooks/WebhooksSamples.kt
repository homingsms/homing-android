package com.homingsms.app.feature.webhooks

import com.homingsms.app.core.designsystem.component.AppendState
import com.homingsms.app.core.designsystem.preview.PREVIEW_NOW_MILLIS

internal object WebhooksSamples {
    private const val MINUTE = 60_000L

    private val orders = WebhookEndpointUi(
        id = "whk_orders",
        url = "https://api.example.com/hooks/homing",
        events = setOf(WebhookEvent.MessageDelivered, WebhookEvent.MessageFailed, WebhookEvent.InboxReceived),
        enabled = true,
        lastDelivery = DeliverySummary(DeliveryStatus.Succeeded, 200, PREVIEW_NOW_MILLIS - 2 * MINUTE)
    )

    private val crm = WebhookEndpointUi(
        id = "whk_crm",
        url = "https://crm.example.org/inbound/sms",
        events = setOf(WebhookEvent.InboxReceived, WebhookEvent.OptOutReceived),
        enabled = false,
        lastDelivery = DeliverySummary(DeliveryStatus.Failed, 503, PREVIEW_NOW_MILLIS - 3 * 60 * MINUTE)
    )

    val list = WebhooksUiState(loadState = WebhooksLoadState.Ready, endpoints = listOf(orders, crm))

    val empty = WebhooksUiState(loadState = WebhooksLoadState.Ready)

    val newEditor = WebhookEditorUiState(
        isNew = true,
        url = "http://api.example.com/hooks",
        urlError = WebhookUrlError.NotHttps,
        events = setOf(WebhookEvent.MessageDelivered, WebhookEvent.MessageFailed)
    )

    val editEditor = WebhookEditorUiState(
        isNew = false,
        url = orders.url,
        events = orders.events,
        enabled = true
    )

    val detail = WebhookDetailUiState(
        loadState = WebhooksLoadState.Ready,
        endpoint = orders,
        signingSecret = "whsec_9f8a7b6c5d4e3f2a1b0c",
        testState = TestEventState.Delivered(200),
        deliveriesAppendState = AppendState.EndReached,
        deliveries = listOf(
            DeliveryUi(
                id = "whd_1",
                eventType = "message.delivered",
                status = DeliveryStatus.Succeeded,
                atMillis = PREVIEW_NOW_MILLIS - 2 * MINUTE,
                statusCode = 200,
                durationMillis = 184,
                responseBody = "{\"ok\":true}"
            ),
            DeliveryUi(
                id = "whd_2",
                eventType = "inbox.received",
                status = DeliveryStatus.Retrying,
                atMillis = PREVIEW_NOW_MILLIS - 14 * MINUTE,
                attempt = 3,
                statusCode = 502,
                durationMillis = 10_000,
                responseBody = "Bad gateway",
                nextRetryAtMillis = PREVIEW_NOW_MILLIS + 4 * MINUTE
            ),
            DeliveryUi(
                id = "whd_3",
                eventType = "webhook.test",
                status = DeliveryStatus.Failed,
                atMillis = PREVIEW_NOW_MILLIS - 50 * MINUTE,
                attempt = 8,
                errorMessage = "Connection timed out after 10 seconds"
            )
        )
    )
}
