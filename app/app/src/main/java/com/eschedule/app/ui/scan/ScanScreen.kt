package com.eschedule.app.ui.scan

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
import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import android.widget.Toast
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import com.eschedule.app.data.ScanStage
import kotlinx.coroutines.delay
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.eschedule.app.theme.AppTheme
import com.eschedule.app.theme.Spacing
import com.eschedule.app.R
import com.eschedule.app.ui.components.CardRadius
import com.eschedule.app.ui.components.FigmaTextMuted
import com.eschedule.app.ui.components.ScreenMargin
import com.eschedule.app.ui.components.PageTitle
import com.eschedule.app.ui.components.appShadow

// ── Design tokens ──────────────────────────────────────────────────────────────
private val TorchActiveColor = Color(0xFFFFE066)

@Composable
fun ScanScreen(
    captureRequested: Boolean = false,
    onCaptureHandled: () -> Unit = {},
    /** Called with the captured photo as JPEG bytes — hand it to `ScheduleScanRepository.scan`. */
    /** The photo (JPEG) and the clockwise turn that makes it upright. */
    onImageCaptured: (ByteArray, Int) -> Unit = { _, _ -> },
    /** The captured photo, shown in place of the live view while it is being read. */
    frozenFrame: ImageBitmap? = null,
    /** Non-null while a scan is running: dims the preview and says what is happening. */
    scanStage: ScanStage? = null,
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
                            val rotation = image.imageInfo.rotationDegrees
                            image.close()
                            onImageCaptured(bytes, rotation)
                        }
                        override fun onError(exc: ImageCaptureException) {
                            Toast.makeText(context, "Couldn't take the photo. Please try again.", Toast.LENGTH_SHORT).show()
                        }
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

                    // While the photo is being read: the frame we captured replaces the live view
                    // (the camera stays bound underneath so it is ready the moment we're done).
                    if (frozenFrame != null) {
                        Image(
                            bitmap = frozenFrame,
                            contentDescription = "The photo you just took",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize(),
                        )
                    }
                    if (scanStage != null) ScanBusyOverlay(scanStage)
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

/**
 * Drawn over the preview while a scan runs: a dark fade up from the bottom edge, then a spinner,
 * the current step in words and a row of dots showing how far along it is.
 */
@Composable
private fun ScanBusyOverlay(stage: ScanStage) {
    // Recognizing is the long wait; if it drags on, say so instead of looking stuck.
    var slow by remember(stage) { mutableStateOf(false) }
    LaunchedEffect(stage) {
        if (stage == ScanStage.Recognizing) {
            delay(SlowAfterMs)
            slow = true
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0x33000000))
            .background(
                Brush.verticalGradient(
                    0.35f to Color.Transparent,
                    1f to Color(0xCC000000),
                ),
            ),
        contentAlignment = Alignment.BottomCenter,
    ) {
        Column(
            modifier = Modifier.padding(start = Spacing.md, end = Spacing.md, bottom = Spacing.lg),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(28.dp),
                color = Color.White,
                trackColor = Color(0x40FFFFFF),
                strokeWidth = 3.dp,
            )
            Spacer(Modifier.height(Spacing.sm))
            Crossfade(
                targetState = stageLabel(stage, slow),
                animationSpec = tween(200),
                label = "scanStageLabel",
            ) { label ->
                Text(
                    text = label,
                    fontSize = 13.sp,
                    lineHeight = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White,
                    textAlign = TextAlign.Center,
                )
            }
            Text(
                text = if (slow) "This can take a little longer when the service is busy." else " ",
                fontSize = 10.sp,
                lineHeight = 14.sp,
                color = Color(0xB3FFFFFF),
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(Spacing.sm))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                ScanStage.entries.forEach { step ->
                    val reached = step.ordinal <= stage.ordinal
                    val dot by animateColorAsState(
                        targetValue = if (reached) Color.White else Color(0x59FFFFFF),
                        animationSpec = tween(200),
                        label = "scanStageDot",
                    )
                    Box(Modifier.size(width = if (step == stage) 18.dp else 6.dp, height = 6.dp).background(dot, CircleShape))
                }
            }
        }
    }
}

private const val SlowAfterMs = 7_000L

private fun stageLabel(stage: ScanStage, slow: Boolean): String = when (stage) {
    ScanStage.Preparing -> "Preparing your photo…"
    ScanStage.Uploading -> "Uploading your photo…"
    ScanStage.Recognizing -> if (slow) "Waiting for the response…" else "Recognizing your schedule…"
    ScanStage.Reading -> "Reading the results…"
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
