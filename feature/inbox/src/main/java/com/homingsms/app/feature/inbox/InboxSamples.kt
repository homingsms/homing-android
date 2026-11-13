package com.homingsms.app.feature.inbox

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Shader
import com.homingsms.app.core.designsystem.component.AppendState
import com.homingsms.app.core.designsystem.preview.PREVIEW_NOW_MILLIS
import java.io.ByteArrayOutputStream

internal object InboxSamples {
    private const val MINUTE = 60_000L
    private const val THUMB_SIZE = 96
    private const val PNG_QUALITY = 100

    fun thumbnail(start: Int, end: Int): AttachmentThumbnail {
        val bitmap = Bitmap.createBitmap(THUMB_SIZE, THUMB_SIZE, Bitmap.Config.ARGB_8888)
        val paint = Paint().apply {
            shader = LinearGradient(0f, 0f, THUMB_SIZE.toFloat(), THUMB_SIZE.toFloat(), start, end, Shader.TileMode.CLAMP)
        }
        Canvas(bitmap).drawRect(0f, 0f, THUMB_SIZE.toFloat(), THUMB_SIZE.toFloat(), paint)
        val out = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.PNG, PNG_QUALITY, out)
        return AttachmentThumbnail(out.toByteArray())
    }

    private val photo by lazy { thumbnail(0xFF0A6870.toInt(), 0xFFE3A642.toInt()) }

    private val sims = listOf(InboxSim(0, "Vodafone UK"), InboxSim(1, "EE"))

    val list by lazy {
        InboxUiState(
            availableSims = sims,
            loadState = InboxLoadState.Ready,
            appendState = AppendState.EndReached,
            items = listOf(
                InboxListItem(
                    "inb_1",
                    "+44 7700 900111",
                    InboxKind.Sms,
                    "Yes, please book me in for Thursday at 3.",
                    PREVIEW_NOW_MILLIS - 3 * MINUTE,
                    0
                ),
                InboxListItem(
                    "inb_2",
                    "+44 7700 900222",
                    InboxKind.Sms,
                    "STOP",
                    PREVIEW_NOW_MILLIS - 12 * MINUTE,
                    1,
                    optOutKeyword = "STOP"
                ),
                InboxListItem(
                    "inb_3",
                    "+1 202 555 0109",
                    InboxKind.Mms,
                    "Photo of the damaged parcel",
                    PREVIEW_NOW_MILLIS - 47 * MINUTE,
                    0,
                    attachmentCount = 1,
                    thumbnail = photo
                ),
                InboxListItem("inb_4", "+234 803 555 0101", InboxKind.Data, null, PREVIEW_NOW_MILLIS - 70 * MINUTE, 1, dataPort = 8901),
                InboxListItem(
                    "inb_5",
                    "BANK",
                    InboxKind.Sms,
                    "Your one-time code is 552 019. It expires in 5 minutes.",
                    PREVIEW_NOW_MILLIS - 130 * MINUTE,
                    0
                )
            )
        )
    }

    val empty = InboxUiState(loadState = InboxLoadState.Ready)

    val mmsDetail by lazy {
        InboxDetailUiState(
            loadState = InboxLoadState.Ready,
            message = InboxDetail(
                id = "inb_3",
                sender = "+1 202 555 0109",
                kind = InboxKind.Mms,
                receivedAtMillis = PREVIEW_NOW_MILLIS - 47 * MINUTE,
                simSlot = 0,
                subject = "Parcel",
                text = "Photo of the damaged parcel. The box was open when it arrived.",
                forwardStatus = ForwardStatus.Forwarded,
                attachments = listOf(
                    InboxAttachment("att_1", "IMG_2041.jpg", "image/jpeg", 241_664, photo),
                    InboxAttachment("att_2", "contact.vcf", "text/x-vcard", 812)
                )
            )
        )
    }
}
