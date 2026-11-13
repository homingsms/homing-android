package com.homingsms.app.feature.webhooks

import androidx.compose.runtime.Composable
import com.homingsms.app.core.designsystem.preview.HomingPreviewSurface
import com.homingsms.app.core.designsystem.preview.HomingPreviews

@HomingPreviews
@Composable
private fun WebhooksListPreview() {
    HomingPreviewSurface { WebhooksScreen(WebhooksSamples.list, WebhooksActions()) }
}

@HomingPreviews
@Composable
private fun WebhooksEmptyPreview() {
    HomingPreviewSurface { WebhooksScreen(WebhooksSamples.empty, WebhooksActions()) }
}

@HomingPreviews
@Composable
private fun WebhookEditorPreview() {
    HomingPreviewSurface { WebhookEditorScreen(WebhooksSamples.newEditor, WebhookEditorActions()) }
}

@HomingPreviews
@Composable
private fun WebhookDetailPreview() {
    HomingPreviewSurface { WebhookDetailScreen(WebhooksSamples.detail, WebhookDetailActions()) }
}
