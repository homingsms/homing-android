package com.homingsms.app.core.designsystem.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import com.homingsms.app.core.designsystem.theme.HomingTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomingBottomSheet(
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    title: String? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val colors = HomingTheme.colors
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        modifier = modifier,
        sheetState = sheetState,
        shape = MaterialTheme.shapes.extraLarge,
        containerColor = colors.surface,
        contentColor = colors.foreground,
        scrimColor = colors.overlay,
        dragHandle = { BottomSheetDefaults.DragHandle(color = colors.borderStrong) }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = HomingTheme.spacing.gutter)
                .padding(bottom = HomingTheme.spacing.xxl),
            verticalArrangement = Arrangement.spacedBy(HomingTheme.spacing.md)
        ) {
            if (title != null) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.semantics { heading() }
                )
            }
            content()
        }
    }
}

@Composable
fun HomingEditorSheet(
    title: String,
    confirmLabel: String,
    onConfirm: () -> Unit,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    confirmEnabled: Boolean = true,
    content: @Composable ColumnScope.() -> Unit
) {
    HomingBottomSheet(onDismissRequest = onDismissRequest, modifier = modifier, title = title) {
        content()
        HomingButton(
            text = confirmLabel,
            onClick = onConfirm,
            enabled = confirmEnabled,
            modifier = Modifier.fillMaxWidth()
        )
    }
}
