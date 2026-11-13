package com.homingsms.app.feature.messages

import com.homingsms.app.core.designsystem.component.AppendState
import com.homingsms.app.core.designsystem.preview.PREVIEW_NOW_MILLIS

internal object MessagesSamples {
    private const val MINUTE = 60_000L

    private val items = listOf(
        MessageListItem(
            "msg_V1StGXR8Z5jdHi6B",
            "+1 202 555 0143",
            MessageType.Sms,
            MessageState.Delivered,
            "Your verification code is 481 223",
            PREVIEW_NOW_MILLIS - 2 * MINUTE,
            0,
            MessagePriority.Otp
        ),
        MessageListItem(
            "msg_2",
            "+44 7700 900982",
            MessageType.Sms,
            MessageState.Failed,
            "Your order #4821 has shipped and arrives Thursday.",
            PREVIEW_NOW_MILLIS - 9 * MINUTE,
            1,
            errorCode = "no_service"
        ),
        MessageListItem(
            "msg_3",
            "+234 803 555 0199",
            MessageType.Mms,
            MessageState.Sent,
            null,
            PREVIEW_NOW_MILLIS - 25 * MINUTE,
            0,
            attachmentCount = 2
        ),
        MessageListItem(
            "msg_4",
            "+49 1512 3456789",
            MessageType.Sms,
            MessageState.Queued,
            "Reminder: your appointment is tomorrow at 10:00.",
            PREVIEW_NOW_MILLIS - 31 * MINUTE,
            1
        ),
        MessageListItem(
            "msg_5",
            "+7 912 555 0177",
            MessageType.Data,
            MessageState.Sending,
            null,
            PREVIEW_NOW_MILLIS - 40 * MINUTE,
            0,
            dataPort = 8901
        ),
        MessageListItem(
            "msg_6",
            "+86 138 0013 8000",
            MessageType.Sms,
            MessageState.Expired,
            "Flash sale ends tonight.",
            PREVIEW_NOW_MILLIS - 95 * MINUTE,
            0
        ),
        MessageListItem(
            "msg_7",
            "+1 415 555 0100",
            MessageType.Sms,
            MessageState.Cancelled,
            "Your table is ready.",
            PREVIEW_NOW_MILLIS - 180 * MINUTE,
            1
        )
    )

    val sims = listOf(SimOption(0, "Vodafone UK"), SimOption(1, "EE"))

    val list = MessagesUiState(items = items, availableSims = sims, loadState = LoadState.Ready, appendState = AppendState.Loading)

    val filteredEmpty = MessagesUiState(
        query = "+1 999",
        filters = MessageFilters(states = setOf(MessageState.Failed)),
        availableSims = sims,
        loadState = LoadState.Ready
    )

    val empty = MessagesUiState(loadState = LoadState.Ready)

    val failedDetail = MessageDetailUiState(
        loadState = LoadState.Ready,
        canRetry = true,
        message = MessageDetail(
            id = "msg_V1StGXR8Z5jdHi6BmyT",
            recipient = "+44 7700 900982",
            type = MessageType.Sms,
            state = MessageState.Failed,
            createdAtMillis = PREVIEW_NOW_MILLIS - 10 * MINUTE,
            text = "Your order #4821 has shipped and arrives Thursday.",
            simSlot = 1,
            attempts = 1,
            idempotencyKey = "order-4821-shipped",
            source = MessageSource.Cloud,
            error = MessageError("no_service", "The carrier reported no service on SIM 2."),
            events = listOf(
                MessageEvent(MessageState.Pending, PREVIEW_NOW_MILLIS - 10 * MINUTE),
                MessageEvent(MessageState.Leased, PREVIEW_NOW_MILLIS - 10 * MINUTE + 3_000),
                MessageEvent(MessageState.Queued, PREVIEW_NOW_MILLIS - 10 * MINUTE + 4_000),
                MessageEvent(MessageState.Sending, PREVIEW_NOW_MILLIS - 10 * MINUTE + 5_000),
                MessageEvent(MessageState.Failed, PREVIEW_NOW_MILLIS - 9 * MINUTE, "no_service")
            )
        )
    )

    val sentDetail = MessageDetailUiState(
        loadState = LoadState.Ready,
        message = MessageDetail(
            id = "msg_3",
            recipient = "+234 803 555 0199",
            type = MessageType.Mms,
            state = MessageState.Sent,
            createdAtMillis = PREVIEW_NOW_MILLIS - 26 * MINUTE,
            subject = "Receipt",
            text = "Here is your receipt for March.",
            attachments = listOf(MessageAttachmentUi("att_1", "receipt.jpg", "image/jpeg", 182_400)),
            simSlot = 0,
            attempts = 1,
            events = listOf(
                MessageEvent(MessageState.Queued, PREVIEW_NOW_MILLIS - 26 * MINUTE),
                MessageEvent(MessageState.Sent, PREVIEW_NOW_MILLIS - 25 * MINUTE)
            )
        )
    )

    val cancellable = MessageDetailUiState(
        loadState = LoadState.Ready,
        canCancel = true,
        message = MessageDetail(
            id = "msg_8",
            recipient = "+1 202 555 0177",
            type = MessageType.Sms,
            state = MessageState.Pending,
            createdAtMillis = PREVIEW_NOW_MILLIS - MINUTE,
            text = "See you at 6.",
            events = listOf(MessageEvent(MessageState.Pending, PREVIEW_NOW_MILLIS - MINUTE))
        )
    )
}
