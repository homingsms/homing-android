package com.homingsms.app.feature.about

import androidx.compose.runtime.Composable
import com.homingsms.app.core.designsystem.preview.HomingPreviewSurface
import com.homingsms.app.core.designsystem.preview.HomingPreviews

internal val SampleAbout = AboutUiState(versionName = "1.0.0", versionCode = 1, buildType = "debug")

@HomingPreviews
@Composable
private fun AboutPreview() {
    HomingPreviewSurface { AboutScreen(SampleAbout, AboutActions(onBack = {})) }
}
