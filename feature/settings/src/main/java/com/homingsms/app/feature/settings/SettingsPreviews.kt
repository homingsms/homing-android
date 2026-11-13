package com.homingsms.app.feature.settings

import androidx.compose.runtime.Composable
import com.homingsms.app.core.designsystem.preview.HomingPreviewSurface
import com.homingsms.app.core.designsystem.preview.HomingPreviews

@HomingPreviews
@Composable
private fun SettingsPreview() {
    HomingPreviewSurface { SettingsScreen(SettingsSamples.typical, SettingsActions()) }
}
