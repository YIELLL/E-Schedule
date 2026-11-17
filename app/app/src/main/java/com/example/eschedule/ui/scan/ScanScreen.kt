package com.example.eschedule.ui.scan

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.example.eschedule.theme.AppTheme
import com.example.eschedule.theme.Spacing
import com.example.eschedule.R
import com.example.eschedule.ui.components.CardRadius
import com.example.eschedule.ui.components.FigmaTextMuted
import com.example.eschedule.ui.components.ScreenMargin
import com.example.eschedule.ui.components.PageTitle
import com.example.eschedule.ui.components.appShadow

// ── Design tokens ──────────────────────────────────────────────────────────────
private val TorchActiveColor = Color(0xFFFFE066)

@Composable
fun ScanScreen(
    captureRequested: Boolean = false,
    onCaptureHandled: () -> Unit = {},
    /** Called with the captured photo as JPEG bytes — hand it to `ScheduleScanRepository.scan`. */
    onImageCaptured: (ByteArray) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val context        = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    // ── Permission ────────────────────────────────────────────────────────────
    var hasPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED,
        )
    }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted -> hasPermission = granted }

    LaunchedEffect(Unit) {
        if (!hasPermission) permissionLauncher.launch(Manifest.permission.CAMERA)
    }

    // ── Camera state ─────────────────────────────────────────────────────────
    var cameraSelectors by remember {
        mutableStateOf(listOf(CameraSelector.DEFAULT_BACK_CAMERA))
    }
    var selectorIndex by remember { mutableIntStateOf(0) }
    var torchEnabled  by remember { mutableStateOf(false) }
    var activeCamera: Camera?              by remember { mutableStateOf(null) }
    var imageCaptureUc: ImageCapture?      by remember { mutableStateOf(null) }
    var cameraProvider: ProcessCameraProvider? by remember { mutableStateOf(null) }

    DisposableEffect(Unit) {
        onDispose { cameraProvider?.unbindAll() }
    }

    val currentSelector = cameraSelectors[selectorIndex]

    LaunchedEffect(currentSelector) { torchEnabled = false }

    // ── Shutter trigger ───────────────────────────────────────────────────────
    LaunchedEffect(captureRequested) {
        if (captureRequested && hasPermission) {
            val ic = imageCaptureUc
            if (ic != null) {
                ic.takePicture(
                    ContextCompat.getMainExecutor(context),
                    object : ImageCapture.OnImageCapturedCallback() {
                        override fun onCaptureSuccess(image: androidx.camera.core.ImageProxy) {
                            // ImageCapture's default format is JPEG: plane 0 holds the whole file.
                            val buffer = image.planes[0].buffer
                            val bytes = ByteArray(buffer.remaining()).also { buffer.get(it) }
                            image.close()
                            onImageCaptured(bytes)
                        }
                        override fun onError(exc: ImageCaptureException) { /* log */ }
                    },
                )
            }
            onCaptureHandled()
        }
    }

    // ── Root ──────────────────────────────────────────────────────────────────
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(AppTheme.colors.background),
    ) {
        // ── Scrollable content ────────────────────────────────────────────────
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(bottom = 140.dp),
        ) {
            // Title at (30, 100); the preview card starts at y 140 → 10dp gap
            PageTitle(title = "Scan Class Schedule")

            // ── Camera card: 342 × 442, radius 10; 302 × 402 preview inset 20dp ──
            Box(
                modifier = Modifier
                    .padding(horizontal = ScreenMargin)
                    .fillMaxWidth()
                    .aspectRatio(0.78f)
                    .appShadow(cornerRadius = CardRadius)
                    .background(AppTheme.colors.surface, RoundedCornerShape(CardRadius))
                    .padding(Spacing.md),
            ) {
                // Preview with a 5dp black-25% frame drawn over its edge
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(5.dp))
                        .border(5.dp, AppTheme.colors.placeholder, RoundedCornerShape(5.dp)),
                ) {
                    when {
                        !hasPermission -> {
                            Text(
                                text = "Camera permission required.\nTap to grant.",
                                fontSize = 12.sp,
                                lineHeight = 18.sp,
                                color = FigmaTextMuted,
                                textAlign = TextAlign.Center,
                                modifier = Modifier
                                    .align(Alignment.Center)
                                    .padding(horizontal = 32.dp)
                                    .clickable(
                                        interactionSource = remember { MutableInteractionSource() },
                                        indication = null,
                                    ) { permissionLauncher.launch(Manifest.permission.CAMERA) },
                            )
                        }
                        else -> {
                            AndroidView(
                                factory = { ctx ->
                                    PreviewView(ctx).apply {
                                        implementationMode = PreviewView.ImplementationMode.COMPATIBLE
                                        scaleType = PreviewView.ScaleType.FILL_CENTER
                                    }
                                },
                                update = { previewView ->
                                    val future = ProcessCameraProvider.getInstance(context)
                                    future.addListener({
                                        val provider = future.get()
                                        cameraProvider = provider
                                        provider.unbindAll()

                                        if (cameraSelectors.size == 1) {
                                            val infos = provider.getAvailableCameraInfos()
                                            val selectors = mutableListOf<CameraSelector>()
                                            if (infos.any { info ->
                                                    runCatching {
                                                        CameraSelector.DEFAULT_BACK_CAMERA
                                                            .filter(infos).contains(info)
                                                    }.getOrDefault(false)
                                                }
                                            ) selectors.add(CameraSelector.DEFAULT_BACK_CAMERA)
                                            if (infos.any { info ->
                                                    runCatching {
                                                        CameraSelector.DEFAULT_FRONT_CAMERA
                                                            .filter(infos).contains(info)
                                                    }.getOrDefault(false)
                                                }
                                            ) selectors.add(CameraSelector.DEFAULT_FRONT_CAMERA)
                                            if (selectors.isNotEmpty()) cameraSelectors = selectors
                                        }

                                        val capture = ImageCapture.Builder().build()
                                        val preview = Preview.Builder().build().also {
                                            it.surfaceProvider = previewView.surfaceProvider
                                        }

                                        runCatching {
                                            activeCamera = provider.bindToLifecycle(
                                                lifecycleOwner,
                                                currentSelector,
                                                preview,
                                                capture,
                                            )
                                            imageCaptureUc = capture
                                        }
                                    }, ContextCompat.getMainExecutor(context))
                                },
                                modifier = Modifier.fillMaxSize(),
                            )
                        }
                    }
                }
            }

            // ── Disclaimer: Poppins Regular 10 / 12, black 25%, centred, 10dp below the card
            Spacer(Modifier.height(Spacing.md))
            Text(
                text = "Unclear text, lighting, or unique table formats can cause errors—" +
                    "ensure all days, times, and room assignments match your official " +
                    "schedule before tapping save.",
                fontSize = 11.sp,
                fontWeight = FontWeight.Normal,
                color = AppTheme.colors.placeholder,
                lineHeight = 16.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .padding(horizontal = ScreenMargin)
                    .fillMaxWidth(),
            )
        }

        // ── Floating overlay buttons: 50 × 50 at x 336, camera flip y 661, flash y 721 ──
        if (hasPermission) {
            Column(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = Spacing.lg, bottom = 104.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                if (cameraSelectors.size > 1) {
                    CameraOverlayButton(
                        glyph = R.drawable.ic_scan_flip_glyph,
                        description = "Switch camera",
                        tint = Color.White,
                        onClick = { selectorIndex = (selectorIndex + 1) % cameraSelectors.size },
                    )
                }

                val torchTint by animateColorAsState(
                    targetValue = if (torchEnabled) TorchActiveColor else Color.White,
                    animationSpec = tween(150),
                    label = "torchTint",
                )
                CameraOverlayButton(
                    glyph = R.drawable.ic_scan_flash_glyph,
                    description = "Toggle flashlight",
                    tint = torchTint,
                    onClick = {
                        torchEnabled = !torchEnabled
                        activeCamera?.cameraControl?.enableTorch(torchEnabled)
                    },
                )
            }
        }
    }
}

/** 56dp round button: black @ 20% disc + white glyph (Figma "Flash Button" / "Change Camera"). */
@Composable
private fun CameraOverlayButton(
    glyph: Int,
    description: String,
    tint: Color,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .size(56.dp)
            .background(AppTheme.colors.controlFill, CircleShape)
            .clip(CircleShape)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            painter = painterResource(glyph),
            contentDescription = description,
            tint = tint,
            modifier = Modifier.size(56.dp),
        )
    }
}
