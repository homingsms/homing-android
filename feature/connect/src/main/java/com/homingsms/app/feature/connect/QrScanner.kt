package com.homingsms.app.feature.connect

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.google.zxing.BarcodeFormat
import com.google.zxing.BinaryBitmap
import com.google.zxing.DecodeHintType
import com.google.zxing.MultiFormatReader
import com.google.zxing.PlanarYUVLuminanceSource
import com.google.zxing.ReaderException
import com.google.zxing.common.HybridBinarizer
import com.homingsms.app.core.designsystem.component.EmptyState
import com.homingsms.app.core.designsystem.component.HomingButton
import com.homingsms.app.core.designsystem.component.HomingButtonStyle
import com.homingsms.app.core.designsystem.theme.HomingTheme
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean

enum class CameraPermissionState { Unknown, Granted, Denied }

@Composable
fun QrScannerDialog(onScanned: (String) -> Unit, onDismiss: () -> Unit) {
    val context = LocalContext.current
    var permission by rememberSaveable { mutableStateOf(context.cameraPermission()) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        permission = if (granted) CameraPermissionState.Granted else CameraPermissionState.Denied
    }
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)
    ) {
        QrScannerContent(
            permission = permission,
            onRequestPermission = { launcher.launch(Manifest.permission.CAMERA) },
            onOpenSettings = { context.openAppSettings() },
            onClose = onDismiss,
            camera = { CameraQrPreview(onScanned = onScanned, modifier = Modifier.fillMaxSize()) }
        )
    }
}

@Composable
fun QrScannerContent(
    permission: CameraPermissionState,
    onRequestPermission: () -> Unit,
    onOpenSettings: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
    camera: @Composable () -> Unit = {}
) {
    val granted = permission == CameraPermissionState.Granted
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(if (granted) Color.Black else HomingTheme.colors.background)
    ) {
        if (granted) {
            camera()
            Viewfinder()
        } else {
            CameraPermissionPrompt(permission, onRequestPermission, onOpenSettings, onClose)
        }
        IconButton(
            onClick = onClose,
            modifier = Modifier
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .padding(HomingTheme.spacing.sm)
                .align(Alignment.TopStart)
        ) {
            Icon(
                imageVector = Icons.Outlined.Close,
                contentDescription = stringResource(com.homingsms.app.core.designsystem.R.string.homing_action_close),
                tint = if (granted) Color.White else HomingTheme.colors.foreground
            )
        }
    }
}

@Composable
private fun Viewfinder() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(HomingTheme.spacing.xxl),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            Modifier
                .size(240.dp)
                .border(3.dp, Color.White, MaterialTheme.shapes.large)
        )
        Text(
            text = stringResource(R.string.connect_scan_hint),
            style = MaterialTheme.typography.bodyLarge,
            color = Color.White,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = HomingTheme.spacing.xxl)
        )
    }
}

@Composable
private fun CameraPermissionPrompt(
    permission: CameraPermissionState,
    onRequestPermission: () -> Unit,
    onOpenSettings: () -> Unit,
    onClose: () -> Unit
) {
    val denied = permission == CameraPermissionState.Denied
    Column(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(HomingTheme.spacing.gutter),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        EmptyState(
            icon = Icons.Outlined.PhotoCamera,
            title = stringResource(if (denied) R.string.connect_camera_denied_title else R.string.connect_camera_title),
            body = stringResource(if (denied) R.string.connect_camera_denied_body else R.string.connect_camera_body)
        )
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(HomingTheme.spacing.sm)
        ) {
            HomingButton(
                text = stringResource(R.string.connect_camera_allow),
                onClick = onRequestPermission,
                modifier = Modifier.fillMaxWidth()
            )
            if (denied) {
                HomingButton(
                    text = stringResource(R.string.connect_camera_settings),
                    onClick = onOpenSettings,
                    style = HomingButtonStyle.Outline,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            HomingButton(
                text = stringResource(R.string.connect_type_code_instead),
                onClick = onClose,
                style = HomingButtonStyle.Text,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun CameraQrPreview(onScanned: (String) -> Unit, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val latestOnScanned by rememberUpdatedState(onScanned)
    val executor = remember { Executors.newSingleThreadExecutor() }
    val providerFuture = remember(context) { ProcessCameraProvider.getInstance(context) }
    DisposableEffect(lifecycleOwner) {
        onDispose {
            if (providerFuture.isDone) providerFuture.get().unbindAll()
            executor.shutdown()
        }
    }
    AndroidView(
        modifier = modifier,
        factory = { viewContext ->
            PreviewView(viewContext).also { view ->
                providerFuture.addListener({
                    val provider = providerFuture.get()
                    val preview = Preview.Builder().build().also { it.setSurfaceProvider(view.surfaceProvider) }
                    val analysis = ImageAnalysis.Builder()
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .build()
                        .also { it.setAnalyzer(executor, QrAnalyzer { text -> view.post { latestOnScanned(text) } }) }
                    provider.unbindAll()
                    provider.bindToLifecycle(lifecycleOwner, CameraSelector.DEFAULT_BACK_CAMERA, preview, analysis)
                }, ContextCompat.getMainExecutor(viewContext))
            }
        }
    )
}

internal class QrAnalyzer(private val onResult: (String) -> Unit) : ImageAnalysis.Analyzer {
    private val reader = MultiFormatReader().apply {
        setHints(mapOf(DecodeHintType.POSSIBLE_FORMATS to listOf(BarcodeFormat.QR_CODE)))
    }
    private val found = AtomicBoolean(false)

    override fun analyze(image: ImageProxy) {
        image.use {
            if (found.get()) return
            val text = decode(it) ?: return
            if (found.compareAndSet(false, true)) onResult(text)
        }
    }

    private fun decode(image: ImageProxy): String? {
        val plane = image.planes.firstOrNull() ?: return null
        val buffer = plane.buffer
        val bytes = ByteArray(buffer.remaining())
        buffer.get(bytes)
        val source = PlanarYUVLuminanceSource(bytes, plane.rowStride, image.height, 0, 0, image.width, image.height, false)
        return try {
            reader.decodeWithState(BinaryBitmap(HybridBinarizer(source))).text
        } catch (_: ReaderException) {
            null
        } finally {
            reader.reset()
        }
    }
}

private fun Context.cameraPermission(): CameraPermissionState =
    if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
        CameraPermissionState.Granted
    } else {
        CameraPermissionState.Unknown
    }

private fun Context.openAppSettings() {
    startActivity(
        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", packageName, null))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    )
}
