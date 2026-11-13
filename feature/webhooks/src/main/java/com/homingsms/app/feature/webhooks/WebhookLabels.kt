package com.homingsms.app.feature.webhooks

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.homingsms.app.core.designsystem.theme.StatusTone

@Composable
internal fun WebhookEvent.title(): String = stringResource(
    when (this) {
        WebhookEvent.MessageSent -> R.string.webhooks_event_sent
        WebhookEvent.MessageDelivered -> R.string.webhooks_event_delivered
        WebhookEvent.MessageFailed -> R.string.webhooks_event_failed
        WebhookEvent.InboxReceived -> R.string.webhooks_event_received
        WebhookEvent.OptOutReceived -> R.string.webhooks_event_opt_out
        WebhookEvent.DeviceOnline -> R.string.webhooks_event_online
        WebhookEvent.DeviceOffline -> R.string.webhooks_event_offline
    }
)

internal fun DeliveryStatus.tone(): StatusTone = when (this) {
    DeliveryStatus.Succeeded -> StatusTone.Success
    DeliveryStatus.Retrying -> StatusTone.Warning
    DeliveryStatus.Pending -> StatusTone.Neutral
    DeliveryStatus.Failed -> StatusTone.Danger
}

@Composable
internal fun DeliveryStatus.label(): String = stringResource(
    when (this) {
        DeliveryStatus.Succeeded -> R.string.webhooks_delivery_succeeded
        DeliveryStatus.Retrying -> R.string.webhooks_delivery_retrying
        DeliveryStatus.Pending -> R.string.webhooks_delivery_pending
        DeliveryStatus.Failed -> R.string.webhooks_delivery_failed
    }
)
