package com.homingsms.app.core.designsystem.util

import android.content.ClipData
import android.os.Build
import android.os.PersistableBundle
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.platform.LocalClipboard
import kotlinx.coroutines.launch

private const val EXTRA_IS_SENSITIVE = "android.content.extra.IS_SENSITIVE"

fun interface CopyToClipboard {
    fun copy(label: String, text: String, sensitive: Boolean)
}

@Composable
fun rememberCopyToClipboard(onShowConfirmation: () -> Unit = {}): CopyToClipboard {
    val clipboard = LocalClipboard.current
    val scope = rememberCoroutineScope()
    val confirm by rememberUpdatedState(onShowConfirmation)
    return remember(clipboard, scope) {
        CopyToClipboard { label, text, sensitive ->
            val clip = ClipData.newPlainText(label, text)
            if (sensitive) {
                clip.description.extras = PersistableBundle().apply { putBoolean(EXTRA_IS_SENSITIVE, true) }
            }
            scope.launch {
                clipboard.setClipEntry(ClipEntry(clip))
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) confirm()
            }
        }
    }
}
