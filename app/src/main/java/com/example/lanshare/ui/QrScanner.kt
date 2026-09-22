package com.example.lanshare.ui

import android.annotation.SuppressLint
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.FlashlightOff
import androidx.compose.material.icons.rounded.FlashlightOn
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean

@SuppressLint("UnsafeOptInUsageError")
@Composable
internal fun QrScanner(
    modifier: Modifier,
    onValue: (String) -> Unit,
    onError: (String) -> Unit,
    onToggle: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val previewView = remember {
        PreviewView(context).apply { scaleType = PreviewView.ScaleType.FILL_CENTER }
    }
    val executor = remember { Executors.newSingleThreadExecutor() }
    val delivered = remember { AtomicBoolean(false) }
    val scanner = remember {
        BarcodeScanning.getClient(
            BarcodeScannerOptions.Builder().setBarcodeFormats(Barcode.FORMAT_QR_CODE).build()
        )
    }
    var camera by remember { mutableStateOf<Camera?>(null) }
    var torchEnabled by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(camera, torchEnabled) {
        camera?.cameraControl?.enableTorch(torchEnabled)
    }

    DisposableEffect(lifecycleOwner) {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        val bindingGate = QrScannerBindingGate()
        val listener = Runnable {
            runCatching {
                val cameraProvider = cameraProviderFuture.get()
                bindingGate.withActive {
                    val preview = Preview.Builder().build().also { it.surfaceProvider = previewView.surfaceProvider }
                    val analysis = ImageAnalysis.Builder()
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .build()
                    analysis.setAnalyzer(executor) { imageProxy ->
                        val mediaImage = imageProxy.image
                        if (mediaImage == null || delivered.get()) {
                            imageProxy.close()
                            return@setAnalyzer
                        }
                        scanner.process(InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees))
                            .addOnSuccessListener { barcodes ->
                                val raw = barcodes.firstNotNullOfOrNull { it.rawValue }
                                if (raw != null && delivered.compareAndSet(false, true)) onValue(raw)
                            }
                            .addOnFailureListener { onError(it.message ?: "无法识别二维码") }
                            .addOnCompleteListener { imageProxy.close() }
                    }
                    cameraProvider.unbindAll()
                    camera = cameraProvider.bindToLifecycle(
                        lifecycleOwner,
                        CameraSelector.DEFAULT_BACK_CAMERA,
                        preview,
                        analysis
                    )
                }
            }.onFailure {
                if (bindingGate.isActive()) onError(it.message ?: "无法启动相机")
            }
        }
        cameraProviderFuture.addListener(listener, ContextCompat.getMainExecutor(context))
        onDispose {
            bindingGate.dispose()
            camera?.cameraControl?.enableTorch(false)
            if (cameraProviderFuture.isDone) runCatching { cameraProviderFuture.get().unbindAll() }
            scanner.close()
            executor.shutdownNow()
        }
    }

    Surface(modifier = modifier.clip(RoundedCornerShape(26.dp)), color = Color.Black, shape = RoundedCornerShape(26.dp)) {
        Box(Modifier.fillMaxSize()) {
            AndroidView(factory = { previewView }, modifier = Modifier.fillMaxSize())
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = Color.White.copy(alpha = 0.70f),
                shape = RoundedCornerShape(26.dp)
            ) {}
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clickable(onClick = onToggle)
            )
            ScannerFrame(Modifier.fillMaxSize())
            Column(
                modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 14.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                IconButton(onClick = { torchEnabled = !torchEnabled }) {
                    Surface(color = PaleBlue, shape = CircleShape) {
                        Icon(
                            if (torchEnabled) Icons.Rounded.FlashlightOff else Icons.Rounded.FlashlightOn,
                            contentDescription = if (torchEnabled) "关闭手电筒" else "开启手电筒",
                            tint = LanBlue,
                            modifier = Modifier.padding(12.dp).size(24.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
internal fun ScannerPlaceholder(modifier: Modifier, onClick: () -> Unit, error: String) {
    Surface(
        modifier = modifier.clip(RoundedCornerShape(26.dp)).clickable(onClick = onClick),
        color = SoftBlue,
        shape = RoundedCornerShape(26.dp)
    ) {
        Box(Modifier.fillMaxSize()) {
            ScannerFrame(Modifier.fillMaxSize())
            Column(
                modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 14.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                IconButton(onClick = onClick) {
                    Surface(color = PaleBlue, shape = CircleShape) {
                        Icon(
                            Icons.Rounded.FlashlightOn,
                            contentDescription = "打开相机",
                            tint = LanBlue,
                            modifier = Modifier.padding(12.dp).size(24.dp)
                        )
                    }
                }
                if (error.isNotBlank()) Text(error, color = androidx.compose.material3.MaterialTheme.colorScheme.error, style = androidx.compose.material3.MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
private fun ScannerFrame(modifier: Modifier) {
    Canvas(modifier = modifier.padding(horizontal = 52.dp, vertical = 42.dp)) {
        val left = size.width * 0.18f
        val right = size.width * 0.82f
        val top = size.height * 0.12f
        val bottom = size.height * 0.72f
        val corner = 30.dp.toPx()
        val stroke = 4.dp.toPx()
        val color = LanBlue
        drawLine(color, androidx.compose.ui.geometry.Offset(left, top), androidx.compose.ui.geometry.Offset(left + corner, top), stroke, StrokeCap.Round)
        drawLine(color, androidx.compose.ui.geometry.Offset(left, top), androidx.compose.ui.geometry.Offset(left, top + corner), stroke, StrokeCap.Round)
        drawLine(color, androidx.compose.ui.geometry.Offset(right, top), androidx.compose.ui.geometry.Offset(right - corner, top), stroke, StrokeCap.Round)
        drawLine(color, androidx.compose.ui.geometry.Offset(right, top), androidx.compose.ui.geometry.Offset(right, top + corner), stroke, StrokeCap.Round)
        drawLine(color, androidx.compose.ui.geometry.Offset(left, bottom), androidx.compose.ui.geometry.Offset(left + corner, bottom), stroke, StrokeCap.Round)
        drawLine(color, androidx.compose.ui.geometry.Offset(left, bottom), androidx.compose.ui.geometry.Offset(left, bottom - corner), stroke, StrokeCap.Round)
        drawLine(color, androidx.compose.ui.geometry.Offset(right, bottom), androidx.compose.ui.geometry.Offset(right - corner, bottom), stroke, StrokeCap.Round)
        drawLine(color, androidx.compose.ui.geometry.Offset(right, bottom), androidx.compose.ui.geometry.Offset(right, bottom - corner), stroke, StrokeCap.Round)
        drawLine(
            color.copy(alpha = 0.68f),
            androidx.compose.ui.geometry.Offset(left - 16.dp.toPx(), size.height * 0.47f),
            androidx.compose.ui.geometry.Offset(right + 16.dp.toPx(), size.height * 0.47f),
            2.dp.toPx(),
            StrokeCap.Round
        )
    }
}
