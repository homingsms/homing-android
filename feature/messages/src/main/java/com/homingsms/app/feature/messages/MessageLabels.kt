package com.homingsms.app.feature.messages

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.homingsms.app.core.designsystem.theme.StatusTone

fun MessageState.tone(): StatusTone = when (this) {
    MessageState.Scheduled, MessageState.Pending, MessageState.Cancelled -> StatusTone.Neutral
    MessageState.Leased, MessageState.Queued, MessageState.Sending -> StatusTone.Info
    MessageState.Sent -> StatusTone.Brand
    MessageState.Delivered -> StatusTone.Success
    MessageState.Failed -> StatusTone.Danger
    MessageState.Expired -> StatusTone.Warning
}

@Composable
fun MessageState.label(): String = stringResource(
    when (this) {
        MessageState.Scheduled -> R.string.messages_state_scheduled
        MessageState.Pending -> R.string.messages_state_pending
        MessageState.Leased -> R.string.messages_state_leased
        MessageState.Queued -> R.string.messages_state_queued
        MessageState.Sending -> R.string.messages_state_sending
        MessageState.Sent -> R.string.messages_state_sent
        MessageState.Delivered -> R.string.messages_state_delivered
        MessageState.Failed -> R.string.messages_state_failed
        MessageState.Cancelled -> R.string.messages_state_cancelled
        MessageState.Expired -> R.string.messages_state_expired
    }
)

@Composable
fun MessageType.label(): String = stringResource(
    when (this) {
        MessageType.Sms -> R.string.messages_type_sms
        MessageType.Data -> R.string.messages_type_data
        MessageType.Mms -> R.string.messages_type_mms
    }
)
