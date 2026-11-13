package com.homingsms.app.core.designsystem.preview

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.homingsms.app.core.designsystem.theme.HomingTheme
import com.homingsms.app.core.designsystem.theme.LocalHomingClock

const val PREVIEW_NOW_MILLIS = 1_794_830_400_000L

@Preview(name = "Light", widthDp = 411, heightDp = 891, showBackground = true)
@Preview(name = "Dark", widthDp = 411, heightDp = 891, showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Preview(name = "Arabic", widthDp = 411, heightDp = 891, showBackground = true, locale = "ar")
@Preview(name = "Font 200%", widthDp = 411, heightDp = 891, showBackground = true, fontScale = 2f)
annotation class HomingPreviews

@Composable
fun HomingPreviewSurface(content: @Composable () -> Unit) {
    HomingTheme(reducedMotion = true) {
        CompositionLocalProvider(LocalHomingClock provides { PREVIEW_NOW_MILLIS }) {
            Box(Modifier.background(HomingTheme.colors.background)) { content() }
        }
    }
}
