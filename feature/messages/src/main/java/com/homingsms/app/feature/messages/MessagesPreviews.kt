package com.homingsms.app.feature.messages

import androidx.compose.runtime.Composable
import com.homingsms.app.core.designsystem.preview.HomingPreviewSurface
import com.homingsms.app.core.designsystem.preview.HomingPreviews

@HomingPreviews
@Composable
private fun MessagesListPreview() {
    HomingPreviewSurface { MessagesScreen(MessagesSamples.list, MessagesActions()) }
}

@HomingPreviews
@Composable
private fun MessagesEmptyPreview() {
    HomingPreviewSurface { MessagesScreen(MessagesSamples.filteredEmpty, MessagesActions()) }
}

@HomingPreviews
@Composable
private fun MessageDetailFailedPreview() {
    HomingPreviewSurface { MessageDetailScreen(MessagesSamples.failedDetail, MessageDetailActions()) }
}

@HomingPreviews
@Composable
private fun MessageDetailSentPreview() {
    HomingPreviewSurface { MessageDetailScreen(MessagesSamples.sentDetail, MessageDetailActions()) }
}
