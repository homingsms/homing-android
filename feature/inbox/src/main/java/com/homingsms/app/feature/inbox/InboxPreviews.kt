package com.homingsms.app.feature.inbox

import androidx.compose.runtime.Composable
import com.homingsms.app.core.designsystem.preview.HomingPreviewSurface
import com.homingsms.app.core.designsystem.preview.HomingPreviews

@HomingPreviews
@Composable
private fun InboxListPreview() {
    HomingPreviewSurface { InboxScreen(InboxSamples.list, InboxActions()) }
}

@HomingPreviews
@Composable
private fun InboxEmptyPreview() {
    HomingPreviewSurface { InboxScreen(InboxSamples.empty, InboxActions()) }
}

@HomingPreviews
@Composable
private fun InboxDetailPreview() {
    HomingPreviewSurface { InboxDetailScreen(InboxSamples.mmsDetail, InboxDetailActions()) }
}
