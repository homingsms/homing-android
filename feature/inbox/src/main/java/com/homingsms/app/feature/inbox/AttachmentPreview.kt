package com.homingsms.app.feature.inbox

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import com.homingsms.app.core.designsystem.theme.HomingTheme

@Composable
internal fun rememberThumbnail(thumbnail: AttachmentThumbnail?): ImageBitmap? = remember(thumbnail) {
    thumbnail?.let { runCatching { BitmapFactory.decodeByteArray(it.bytes, 0, it.bytes.size)?.asImageBitmap() }.getOrNull() }
}

@Composable
internal fun AttachmentThumb(
    thumbnail: AttachmentThumbnail?,
    isImage: Boolean,
    size: Dp,
    modifier: Modifier = Modifier,
    contentDescription: String? = null
) {
    val bitmap = rememberThumbnail(thumbnail)
    val shape = MaterialTheme.shapes.small
    if (bitmap != null) {
        Image(
            bitmap = bitmap,
            contentDescription = contentDescription,
            contentScale = ContentScale.Crop,
            modifier = modifier
                .size(size)
                .clip(shape)
        )
    } else {
        Box(
            modifier = modifier
                .size(size)
                .background(HomingTheme.colors.muted, shape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (isImage) Icons.Outlined.Image else Icons.Outlined.Description,
                contentDescription = contentDescription,
                tint = HomingTheme.colors.mutedForeground
            )
        }
    }
}
