package com.homingsms.app.feature.home

import androidx.compose.runtime.Composable
import com.homingsms.app.core.designsystem.preview.HomingPreviewSurface
import com.homingsms.app.core.designsystem.preview.HomingPreviews

@HomingPreviews
@Composable
private fun HomeOnlinePreview() {
    HomingPreviewSurface { HomeScreen(HomeSamples.online, HomeActions()) }
}

@HomingPreviews
@Composable
private fun HomeSmsLimitPreview() {
    HomingPreviewSurface { HomeScreen(HomeSamples.smsLimit, HomeActions()) }
}

@HomingPreviews
@Composable
private fun HomeOfflinePreview() {
    HomingPreviewSurface { HomeScreen(HomeSamples.offline, HomeActions()) }
}
