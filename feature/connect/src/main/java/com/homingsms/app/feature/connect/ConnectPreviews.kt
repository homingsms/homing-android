package com.homingsms.app.feature.connect

import androidx.compose.runtime.Composable
import com.homingsms.app.core.designsystem.preview.HomingPreviewSurface
import com.homingsms.app.core.designsystem.preview.HomingPreviews

@HomingPreviews
@Composable
private fun ConnectConnectedPreview() {
    HomingPreviewSurface { ConnectScreen(ConnectSamples.connected, ConnectActions()) }
}

@HomingPreviews
@Composable
private fun ConnectNotPairedPreview() {
    HomingPreviewSurface { ConnectScreen(ConnectSamples.notPaired, ConnectActions()) }
}

@HomingPreviews
@Composable
private fun QrScannerPermissionPreview() {
    HomingPreviewSurface {
        QrScannerContent(permission = CameraPermissionState.Denied, onRequestPermission = {}, onOpenSettings = {}, onClose = {})
    }
}
