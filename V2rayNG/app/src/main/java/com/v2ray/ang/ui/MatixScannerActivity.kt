package com.v2ray.ang.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.util.Size
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.compose.CameraXViewfinder
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.core.SurfaceRequest
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.core.resolutionselector.ResolutionStrategy
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size as GeoSize
import androidx.compose.ui.geometry.lerp
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.google.zxing.BarcodeFormat
import com.google.zxing.BinaryBitmap
import com.google.zxing.DecodeHintType
import com.google.zxing.MultiFormatReader
import com.google.zxing.PlanarYUVLuminanceSource
import com.google.zxing.common.HybridBinarizer
import com.v2ray.ang.AppConfig
import com.v2ray.ang.R
import com.v2ray.ang.ui.base.HelperBaseComponentActivity
import com.v2ray.ang.util.LogUtil
import kotlinx.coroutines.delay
import java.util.concurrent.Executors
import kotlin.math.min

/**
 * Matix net: the QR scanner. The camera fills the screen, a rounded frame marks where to point it, and the frame's
 * rounded corners turn green with a check mark the moment a code is read; a moment later the screen closes and
 * hands the text back as [EXTRA_RESULT] with RESULT_OK.
 */
class MatixScannerActivity : HelperBaseComponentActivity() {

    @Composable
    override fun ScreenContent() {
        MatixScannerScreen(
            onResult = { text ->
                setResult(RESULT_OK, Intent().putExtra(EXTRA_RESULT, text))
                finish()
            },
            onClose = { finish() }
        )
    }

    companion object {
        const val EXTRA_RESULT = "SCAN_RESULT"
    }
}

private val ScanGreen = Color(0xFF34C759)

@Composable
private fun MatixScannerScreen(onResult: (String) -> Unit, onClose: () -> Unit) {
    val context = LocalContext.current
    var granted by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        )
    }
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { ok ->
        granted = ok
        if (!ok) onClose()
    }
    LaunchedEffect(Unit) {
        if (!granted) permissionLauncher.launch(Manifest.permission.CAMERA)
    }

    var scanned by remember { mutableStateOf<String?>(null) }
    var torchOn by remember { mutableStateOf(false) }
    var hasTorch by remember { mutableStateOf(false) }
    val check = remember { Animatable(0f) }
    val resultNow by rememberUpdatedState(onResult)

    // The check mark draws itself, stays a moment so it is seen, then the result goes back.
    LaunchedEffect(scanned) {
        val text = scanned ?: return@LaunchedEffect
        check.animateTo(1f, tween(480, easing = FastOutSlowInEasing))
        delay(650)
        resultNow(text)
    }

    Box(Modifier.fillMaxSize().background(Color.Black)) {
        if (granted) {
            ScannerCamera(
                torchOn = torchOn,
                onTorchSupport = { hasTorch = it },
                onCode = { if (scanned == null) scanned = it },
                modifier = Modifier.fillMaxSize()
            )
        }
        ScanFrameOverlay(done = scanned != null, checkProgress = check.value)

        Box(
            Modifier
                .statusBarsPadding()
                .padding(16.dp)
                .size(44.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.18f))
                .clickable(onClick = onClose)
                .align(Alignment.TopStart),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_arrow_back_24dp),
                contentDescription = stringResource(R.string.matix_close),
                tint = Color.White
            )
        }
        if (hasTorch && scanned == null) {
            Box(
                Modifier
                    .statusBarsPadding()
                    .padding(16.dp)
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(if (torchOn) Color.White else Color.White.copy(alpha = 0.18f))
                    .clickable { torchOn = !torchOn }
                    .align(Alignment.TopEnd),
                contentAlignment = Alignment.Center
            ) {
                Canvas(Modifier.size(22.dp)) {
                    val w = size.width
                    val bolt = Path().apply {
                        moveTo(w * 0.58f, 0f)
                        lineTo(w * 0.18f, w * 0.56f)
                        lineTo(w * 0.48f, w * 0.56f)
                        lineTo(w * 0.40f, w)
                        lineTo(w * 0.82f, w * 0.40f)
                        lineTo(w * 0.52f, w * 0.40f)
                        close()
                    }
                    drawPath(bolt, if (torchOn) Color.Black else Color.White)
                }
            }
        }
        Text(
            text = stringResource(if (scanned != null) R.string.matix_scan_done else R.string.matix_scan_hint),
            color = if (scanned != null) ScanGreen else Color.White,
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = 56.dp)
        )
    }
}

/** The dimmed screen with a clear rounded frame, its four rounded corners, a sweeping line and the check mark. */
@Composable
private fun ScanFrameOverlay(done: Boolean, checkProgress: Float) {
    val sweep = rememberInfiniteTransition(label = "scan-sweep")
    val line by sweep.animateFloat(
        initialValue = 0.08f,
        targetValue = 0.92f,
        animationSpec = infiniteRepeatable(tween(1800, easing = LinearEasing), RepeatMode.Reverse),
        label = "line"
    )
    Canvas(
        Modifier
            .fillMaxSize()
            .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
    ) {
        val side = min(size.width * 0.72f, 320.dp.toPx())
        val left = (size.width - side) / 2f
        val top = (size.height - side) / 2f - 40.dp.toPx()
        val right = left + side
        val bottom = top + side
        val radius = 36.dp.toPx()
        val arm = 40.dp.toPx()

        // The dimmed surround with the frame cut out of it.
        drawRect(Color.Black.copy(alpha = 0.55f))
        drawRoundRect(
            color = Color.Black,
            topLeft = Offset(left, top),
            size = GeoSize(side, side),
            cornerRadius = CornerRadius(radius, radius),
            blendMode = BlendMode.Clear
        )

        // The four rounded corners of the frame.
        val corners = Path().apply {
            moveTo(left, top + radius + arm)
            lineTo(left, top + radius)
            arcTo(Rect(left, top, left + 2 * radius, top + 2 * radius), 180f, 90f, false)
            lineTo(left + radius + arm, top)

            moveTo(right - radius - arm, top)
            lineTo(right - radius, top)
            arcTo(Rect(right - 2 * radius, top, right, top + 2 * radius), 270f, 90f, false)
            lineTo(right, top + radius + arm)

            moveTo(right, bottom - radius - arm)
            lineTo(right, bottom - radius)
            arcTo(Rect(right - 2 * radius, bottom - 2 * radius, right, bottom), 0f, 90f, false)
            lineTo(right - radius - arm, bottom)

            moveTo(left + radius + arm, bottom)
            lineTo(left + radius, bottom)
            arcTo(Rect(left, bottom - 2 * radius, left + 2 * radius, bottom), 90f, 90f, false)
            lineTo(left, bottom - radius - arm)
        }
        drawPath(
            corners,
            color = if (done) ScanGreen else Color.White,
            style = Stroke(width = 6.dp.toPx(), cap = StrokeCap.Round)
        )

        if (!done) {
            val y = top + side * line
            drawLine(
                color = Color.White.copy(alpha = 0.75f),
                start = Offset(left + radius, y),
                end = Offset(right - radius, y),
                strokeWidth = 3.dp.toPx(),
                cap = StrokeCap.Round
            )
        } else {
            // A green disc grows in the middle and a white check mark draws itself inside it.
            val c = Offset(left + side / 2f, top + side / 2f)
            val p = checkProgress.coerceIn(0f, 1f)
            val r = 46.dp.toPx() * p
            drawCircle(ScanGreen, radius = r, center = c)
            val a = Offset(c.x - r * 0.38f, c.y + r * 0.02f)
            val b = Offset(c.x - r * 0.10f, c.y + r * 0.30f)
            val d = Offset(c.x + r * 0.42f, c.y - r * 0.28f)
            val t = ((p - 0.35f) / 0.65f).coerceIn(0f, 1f)
            val first = (t / 0.4f).coerceIn(0f, 1f)
            val second = ((t - 0.4f) / 0.6f).coerceIn(0f, 1f)
            if (first > 0f) {
                drawLine(Color.White, a, lerp(a, b, first), strokeWidth = r * 0.16f, cap = StrokeCap.Round)
            }
            if (second > 0f) {
                drawLine(Color.White, b, lerp(b, d, second), strokeWidth = r * 0.16f, cap = StrokeCap.Round)
            }
        }
    }
}

/** The camera preview, with every frame looked at for a QR code; [onCode] is called on the main thread. */
@Composable
private fun ScannerCamera(
    torchOn: Boolean,
    onTorchSupport: (Boolean) -> Unit,
    onCode: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var surfaceRequest by remember { mutableStateOf<SurfaceRequest?>(null) }
    var camera by remember { mutableStateOf<Camera?>(null) }
    val codeNow by rememberUpdatedState(onCode)
    val supportNow by rememberUpdatedState(onTorchSupport)

    DisposableEffect(lifecycleOwner) {
        val analysisExecutor = Executors.newSingleThreadExecutor()
        val mainExecutor = ContextCompat.getMainExecutor(context)
        val providerFuture = ProcessCameraProvider.getInstance(context)
        var provider: ProcessCameraProvider? = null
        providerFuture.addListener({
            try {
                val p = providerFuture.get()
                provider = p
                val preview = Preview.Builder().build().also { use ->
                    use.setSurfaceProvider { request -> surfaceRequest = request }
                }
                val analysis = ImageAnalysis.Builder()
                    .setResolutionSelector(
                        ResolutionSelector.Builder()
                            .setResolutionStrategy(
                                ResolutionStrategy(Size(1280, 720), ResolutionStrategy.FALLBACK_RULE_CLOSEST_HIGHER_THEN_LOWER)
                            )
                            .build()
                    )
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build()
                analysis.setAnalyzer(analysisExecutor, QrAnalyzer { text -> mainExecutor.execute { codeNow(text) } })
                p.unbindAll()
                val bound = p.bindToLifecycle(lifecycleOwner, CameraSelector.DEFAULT_BACK_CAMERA, preview, analysis)
                camera = bound
                supportNow(bound.cameraInfo.hasFlashUnit())
            } catch (e: Exception) {
                LogUtil.e(AppConfig.TAG, "The scanner camera could not be started", e)
            }
        }, mainExecutor)
        onDispose {
            provider?.unbindAll()
            analysisExecutor.shutdown()
        }
    }
    LaunchedEffect(camera, torchOn) {
        camera?.cameraControl?.enableTorch(torchOn)
    }
    surfaceRequest?.let { request ->
        CameraXViewfinder(surfaceRequest = request, modifier = modifier)
    }
}

/** Reads QR codes from the brightness plane of each camera frame. */
private class QrAnalyzer(private val onFound: (String) -> Unit) : ImageAnalysis.Analyzer {
    private val reader = MultiFormatReader().apply {
        setHints(
            mapOf(
                DecodeHintType.POSSIBLE_FORMATS to listOf(BarcodeFormat.QR_CODE),
                DecodeHintType.TRY_HARDER to true
            )
        )
    }

    override fun analyze(image: ImageProxy) {
        try {
            val plane = image.planes[0]
            val buffer = plane.buffer
            val data = ByteArray(buffer.remaining())
            buffer.get(data)
            val source = PlanarYUVLuminanceSource(
                data, plane.rowStride, image.height, 0, 0, image.width, image.height, false
            )
            val text = try {
                reader.decodeWithState(BinaryBitmap(HybridBinarizer(source))).text
            } catch (e: com.google.zxing.ReaderException) {
                null
            } finally {
                reader.reset()
            }
            if (!text.isNullOrBlank()) onFound(text)
        } catch (e: Exception) {
            LogUtil.e(AppConfig.TAG, "A scanner frame could not be read", e)
        } finally {
            image.close()
        }
    }
}
