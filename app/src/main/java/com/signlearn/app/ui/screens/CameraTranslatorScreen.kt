package com.signlearn.app.ui.screens

import android.Manifest
import android.annotation.SuppressLint
import android.util.Size
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.signlearn.app.ui.theme.*
import java.util.concurrent.Executors
import com.signlearn.app.translator.HandLandmarkerHelper
import com.signlearn.app.translator.GestureClassifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.signlearn.app.translator.TranslatorViewModel
import com.signlearn.app.translator.HandLandmarkerHelper.Result as HLResult

@OptIn(ExperimentalMaterial3Api::class, ExperimentalPermissionsApi::class)
@Composable
fun CameraTranslatorScreen(onNavigateBack: () -> Unit) {
    val cameraPermissionState = rememberPermissionState(Manifest.permission.CAMERA)
    val vm: TranslatorViewModel = viewModel()
    val uiState by vm.state.collectAsState()

    var isDetecting by remember { mutableStateOf(false) }
    var detectedSign by remember { mutableStateOf<DetectedSign?>(null) }
    var confidence by remember { mutableStateOf(0f) }
    val lifecycleOwner = LocalLifecycleOwner.current
    val context = LocalContext.current
    val cameraProviderFuture = remember { ProcessCameraProvider.getInstance(context) }
    val cameraExecutor = remember { Executors.newSingleThreadExecutor() }
    var previewView by remember { mutableStateOf<PreviewView?>(null) }
    var analyzer by remember { mutableStateOf<FrameAnalyzer?>(null) }
    val handHelper = remember { HandLandmarkerHelper(context) }
    val classifier = remember { GestureClassifier(context) }
    var lensFacing by remember { mutableStateOf(CameraSelector.LENS_FACING_FRONT) }
    var lastResult by remember { mutableStateOf<HLResult?>(null) }
    var alphabetOnly by remember { mutableStateOf(false) }
    var debugText by remember { mutableStateOf("") }
    var rawLabel by remember { mutableStateOf<String?>(null) }
    var rawConfidence by remember { mutableStateOf(0f) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Traductor en vivo") },
                navigationIcon = { IconButton(onClick = onNavigateBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Volver") } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Primary, titleContentColor = OnPrimary, navigationIconContentColor = OnPrimary)
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            when {
                !cameraPermissionState.status.isGranted -> {
                    CameraPermissionRequest(onRequestPermission = { cameraPermissionState.launchPermissionRequest() })
                }
                else -> {
                    // Vista de cámara (siempre encendida)
                    AndroidView(
                        factory = {
                            PreviewView(it).apply {
                                layoutParams = ViewGroup.LayoutParams(MATCH_PARENT, MATCH_PARENT)
                                scaleType = PreviewView.ScaleType.FILL_CENTER
                                implementationMode = PreviewView.ImplementationMode.COMPATIBLE
                                previewView = this
                            }
                        },
                        modifier = Modifier.fillMaxSize()
                    )

                    // Overlay de landmarks
                    val displayWord = rawLabel ?: uiState.word
                    LandmarksOverlay(
                        result = lastResult,
                        lensFacing = lensFacing,
                        label = displayWord,
                        modifier = Modifier.fillMaxSize()
                    )

                    // HUD de depuración (predicción por frame / modo / estabilidad)
                    Box(modifier = Modifier.fillMaxSize()) {
                        if (debugText.isNotBlank()) {
                            Surface(
                                tonalElevation = 2.dp,
                                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f),
                                shape = SignLearnShapes.CategoryButton,
                                modifier = Modifier.align(Alignment.TopStart).padding(8.dp)
                            ) {
                                Text(
                                    text = debugText,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }

                    // Controles y panel de resultado
                    CameraTranslatorControls(
                        isDetecting = isDetecting,
                        alphabetOnly = alphabetOnly,
                        detectedSign = displayWord?.let { DetectedSign(it, uiState.translation ?: it, "gesture") },
                        confidence = rawLabel?.let { rawConfidence } ?: uiState.confidence,
                        onToggleDetection = {
                            isDetecting = !isDetecting
                            vm.setDetecting(isDetecting)
                            if (!isDetecting) vm.clear()
                        },
                        onToggleAlphabetOnly = {
                            alphabetOnly = !alphabetOnly
                            vm.setAlphabetOnly(alphabetOnly)
                        },
                        onFlipCamera = {
                            lensFacing = if (lensFacing == CameraSelector.LENS_FACING_FRONT) CameraSelector.LENS_FACING_BACK else CameraSelector.LENS_FACING_FRONT
                        },
                    )

                    // Enlazar/Re-enlazar cámara cuando haya preview o cambie la lente
                    LaunchedEffect(previewView, lensFacing, alphabetOnly) {
                        val provider = cameraProviderFuture.get()
                        val preview = androidx.camera.core.Preview.Builder()
                            .setTargetResolution(Size(640, 480))
                            .build().also { it.setSurfaceProvider(previewView?.surfaceProvider) }

                        val selector = CameraSelector.Builder()
                            .requireLensFacing(lensFacing)
                            .build()

                        val imageAnalyzer = ImageAnalysis.Builder()
                            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                            .setTargetResolution(Size(640, 480))
                            .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888)
                            .build()

                        if (!classifier.isReady()) {
                            runCatching { classifier.load() }
                        }
                        analyzer = FrameAnalyzer(
                            handHelper,
                            classifier,
                            onResult = { r ->
                                lastResult = r
                            },
                            onStable = { label, conf ->
                                vm.publish(label?.takeIf { it.isNotBlank() }, conf)
                            },
                            alphabetOnly = alphabetOnly,
                            mirrorForModel = (lensFacing == CameraSelector.LENS_FACING_FRONT),
                            onDebug = { txt -> debugText = txt },
                            onRaw = { lbl, conf ->
                                rawLabel = lbl
                                rawConfidence = conf
                            },
                            cacheDir = context.cacheDir
                        )
                        imageAnalyzer.setAnalyzer(cameraExecutor) { imageProxy ->
                            analyzer?.analyze(imageProxy, isDetecting)
                        }

                        provider.unbindAll()
                        provider.bindToLifecycle(lifecycleOwner, selector, preview, imageAnalyzer)
                    }

                    DisposableEffect(Unit) {
                        onDispose {
                            runCatching { cameraProviderFuture.get().unbindAll() }
                            runCatching { handHelper.close() }
                            runCatching { classifier.close() }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CameraPermissionRequest(onRequestPermission: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(Icons.Default.CameraAlt, contentDescription = null, modifier = Modifier.size(80.dp), tint = Primary)
        Spacer(Modifier.height(24.dp))
        Text(text = "Permiso de cámara requerido", style = MaterialTheme.typography.headlineSmall, textAlign = TextAlign.Center)
        Spacer(Modifier.height(16.dp))
        Text(text = "SignLearn necesita acceso a la cámara para poder detectar y traducir señas en tiempo real.", style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(32.dp))
        Button(onClick = onRequestPermission, colors = ButtonDefaults.buttonColors(containerColor = Primary), shape = SignLearnShapes.CategoryButton) {
            Icon(Icons.Default.Check, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("Permitir acceso")
        }
    }
}

@Composable
private fun CameraTranslatorControls(
    isDetecting: Boolean,
    alphabetOnly: Boolean,
    detectedSign: DetectedSign?,
    confidence: Float,
    onToggleDetection: () -> Unit,
    onToggleAlphabetOnly: () -> Unit,
    onFlipCamera: () -> Unit,
) {
    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            if (detectedSign != null) {
                Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Primary), elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)) {
                    Column(modifier = Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(text = detectedSign.word, style = MaterialTheme.typography.displaySmall, color = OnPrimary)
                        Spacer(Modifier.height(8.dp))
                        Text(text = detectedSign.translation, style = MaterialTheme.typography.titleMedium, color = OnPrimary.copy(alpha = 0.9f))
                        Spacer(Modifier.height(12.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                            LinearProgressIndicator(progress = { confidence }, modifier = Modifier.weight(1f).height(6.dp), color = OnPrimary, trackColor = OnPrimary.copy(alpha = 0.3f))
                            Text(text = "${(confidence * 100).toInt()}%", style = MaterialTheme.typography.labelLarge, color = OnPrimary)
                        }
                    }
                }
            }
            Spacer(Modifier.weight(1f))
            Card(modifier = Modifier.fillMaxWidth(), elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                            Surface(modifier = Modifier.size(12.dp), shape = androidx.compose.foundation.shape.CircleShape, color = if (isDetecting) Tertiary else MaterialTheme.colorScheme.onSurfaceVariant) {}
                            Text(text = if (isDetecting) "Detectando..." else "En pausa", style = MaterialTheme.typography.bodyMedium)
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = onFlipCamera, colors = IconButtonDefaults.iconButtonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                                Icon(Icons.Default.Cameraswitch, contentDescription = "Cambiar cámara")
                            }
                            IconButton(onClick = { onToggleDetection() }, colors = IconButtonDefaults.iconButtonColors(containerColor = if (isDetecting) Error else Primary)) {
                            Icon(imageVector = if (isDetecting) Icons.Default.Pause else Icons.Default.PlayArrow, contentDescription = if (isDetecting) "Pausar" else "Iniciar", tint = OnPrimary)
                            }
                            IconButton(onClick = onToggleAlphabetOnly, colors = IconButtonDefaults.iconButtonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                                Icon(Icons.Default.FontDownload, contentDescription = "Solo letras", tint = if (alphabetOnly) Primary else MaterialTheme.colorScheme.onSurface)
                            }
                        }
                    }
                    Text(text = "Coloca tu mano frente a la cámara y realiza una seña. El sistema la detectará y traducirá automáticamente.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}

data class DetectedSign(val word: String, val translation: String, val category: String)

/**
 * FrameAnalyzer encapsula la lógica de análisis de frames.
 * Aquí conectaremos el Hand Landmarker de MediaPipe y (opcional) un clasificador de gestos.
 */
private class FrameAnalyzer(
    private val handHelper: HandLandmarkerHelper,
    private val classifier: GestureClassifier?,
    private val onResult: (HLResult?) -> Unit,
    private val onStable: (label: String?, confidence: Float) -> Unit,
    private val alphabetOnly: Boolean = false,
    private val mirrorForModel: Boolean = false,
    private val onDebug: (String) -> Unit = {},
    private val onRaw: (label: String?, confidence: Float) -> Unit = { _, _ -> },
    private val cacheDir: java.io.File? = null
) {
    // Buffer temporal para smoothing de etiquetas (más pequeño para menor costo)
    private val windowSize = 5
    private val labelBuffer: ArrayDeque<String> = ArrayDeque()
    private val confBuffer: ArrayDeque<Float> = ArrayDeque()
    private var lastLandmarks: List<com.signlearn.app.translator.HandLandmarkerHelper.Landmark>? = null
    private var stillFrames: Int = 0
    // Throttling para limitar la frecuencia de análisis y reducir GC
        private var lastAnalyzeMs: Long = 0L
        private val minIntervalMs: Long = 40L // alivio ligero de carga sin bajar resolución

    private fun pushPrediction(label: String, confidence: Float) {
        if (labelBuffer.size >= windowSize) labelBuffer.removeFirst()
        if (confBuffer.size >= windowSize) confBuffer.removeFirst()
        labelBuffer.addLast(label)
        confBuffer.addLast(confidence)
    }

    private fun stablePrediction(minSupport: Int = 3, minAvgConf: Float = 0.30f): Pair<String, Float>? {
        if (labelBuffer.isEmpty()) return null
        val counts = labelBuffer.groupingBy { it }.eachCount()
        val best = counts.maxByOrNull { it.value } ?: return null
        val support = best.value
        val label = best.key
        val avgConf = if (confBuffer.isNotEmpty()) confBuffer.average().toFloat() else 0f
        // Anti-bias: suprimir 'D' si la confianza media es baja
        if (label.equals("D", ignoreCase = true) && avgConf < 0.35f) return null
        return if (support >= minSupport && avgConf >= minAvgConf) label to avgConf else null
    }

    private fun isStill(current: List<com.signlearn.app.translator.HandLandmarkerHelper.Landmark>): Boolean {
        val prev = lastLandmarks ?: return false
        val n = minOf(prev.size, current.size)
        if (n == 0) return false
        var sum = 0.0
        for (i in 0 until n) {
            val dx = (current[i].x - prev[i].x).toDouble()
            val dy = (current[i].y - prev[i].y).toDouble()
            sum += kotlin.math.hypot(dx, dy)
        }
        val avg = (sum / n).toFloat()
        return avg < 0.003f // umbral de movimiento bajo en espacio normalizado
    }
    @SuppressLint("UnsafeOptInUsageError")
    fun analyze(imageProxy: ImageProxy, isDetecting: Boolean) {
        try {
            if (!isDetecting) {
                imageProxy.close()
                return
            }
            // Saltar frames si no ha pasado el intervalo mínimo
            val now = System.currentTimeMillis()
            if (now - lastAnalyzeMs < minIntervalMs) {
                imageProxy.close()
                return
            }
            lastAnalyzeMs = now
            val needBitmap = try {
                val tfl = classifier
                tfl != null && tfl.isReady() && tfl.expectsImage()
            } catch (_: Throwable) { false }
            handHelper.analyze(imageProxy, requireBitmap = needBitmap) { r ->
                // Emitir resultados crudos para overlay
                onResult(r)
                // Aplicar clasificación + smoothing si hay manos
                val tfl = classifier
                if (r != null && r.hands.isNotEmpty() && tfl != null && tfl.isReady()) {
                    val lm = r.hands.first().landmarks
                    val expectsImg = tfl.expectsImage()
                    val pred = if (expectsImg) {
                        var crop = cropHandBitmap(r.bitmap, lm, 0.8f)
                        if (mirrorForModel) crop = crop.flipHorizontally()
                        tfl.classify(crop)
                    } else {
                        val feats = landmarksToFeatures(lm)
                        if (feats.isNotEmpty()) tfl.classify(feats) else null
                    }
                    // Enviar Top-1 crudo siempre a la UI
                    onRaw(pred?.label, pred?.confidence ?: 0f)
                    val allow = if (alphabetOnly) {
                        val still = isStill(lm)
                        if (still) stillFrames++ else stillFrames = 0
                        stillFrames >= 3 // requerir 3 frames consecutivos con poco movimiento
                    } else true
                    if (pred != null && allow) {
                        // Gating extra: ignorar predicciones con confianza cruda muy baja
                        if (pred.confidence >= 0.10f) {
                            pushPrediction(pred.label, pred.confidence)
                        } else {
                            pushPrediction("", 0f)
                        }
                    } else {
                        pushPrediction("", 0f)
                    }
                    lastLandmarks = lm
                    val avg = if (confBuffer.isNotEmpty()) confBuffer.average().toFloat() else 0f
                    val stable = stablePrediction()
                    onStable(stable?.first, stable?.second ?: 0f)
                    // Debug HUD (ligero)
                    onDebug("pred=" + (pred?.label ?: "-") + " " + String.format("%.2f", pred?.confidence ?: 0f) + " | stable=" + (stable?.first ?: "-") + " " + String.format("%.2f", stable?.second ?: 0f))
                } else {
                    // cuando no hay manos visibles, limpiar suavemente
                    pushPrediction("", 0f)
                    val stable = stablePrediction()
                    onStable(stable?.first, stable?.second ?: 0f)
                    lastLandmarks = null
                    stillFrames = 0
                    onRaw(null, 0f)
                    onDebug("")
                }
            }
        } catch (t: Throwable) {
            onResult(null)
            onDebug("error: ${t.message}")
        }
    }
}

private fun landmarksToFeatures(landmarks: List<com.signlearn.app.translator.HandLandmarkerHelper.Landmark>): FloatArray {
    val w = landmarks.getOrNull(0) ?: return FloatArray(0)
    val ref = landmarks.getOrNull(8) ?: w
    val scale = kotlin.math.hypot((ref.x - w.x).toDouble(), (ref.y - w.y).toDouble()).coerceAtLeast(1e-6).toFloat()
    val feats = ArrayList<Float>(landmarks.size * 2)
    landmarks.forEach { p ->
        feats.add(((p.x - w.x) / scale))
        feats.add(((p.y - w.y) / scale))
    }
    return feats.toFloatArray()
}

private fun cropHandBitmap(
    src: android.graphics.Bitmap,
    landmarks: List<com.signlearn.app.translator.HandLandmarkerHelper.Landmark>,
    pad: Float = 0.4f
): android.graphics.Bitmap {
    if (landmarks.isEmpty()) return src
    var minX = 1f
    var minY = 1f
    var maxX = 0f
    var maxY = 0f
    landmarks.forEach { p ->
        if (p.x < minX) minX = p.x
        if (p.y < minY) minY = p.y
        if (p.x > maxX) maxX = p.x
        if (p.y > maxY) maxY = p.y
    }
    val w = src.width
    val h = src.height
    var cx = ((minX + maxX) / 2f) * w
    var cy = ((minY + maxY) / 2f) * h
    var bw = (maxX - minX) * w
    var bh = (maxY - minY) * h
    var size = kotlin.math.max(bw, bh)
    size *= (1f + pad)
    // cuadrado con padding, limitar a bordes
    var left = (cx - size / 2f).toInt()
    var top = (cy - size / 2f).toInt()
    var right = (cx + size / 2f).toInt()
    var bottom = (cy + size / 2f).toInt()
    if (left < 0) { right -= left; left = 0 }
    if (top < 0) { bottom -= top; top = 0 }
    if (right > w) { left -= (right - w); right = w }
    if (bottom > h) { top -= (bottom - h); bottom = h }
    left = left.coerceIn(0, w - 1)
    top = top.coerceIn(0, h - 1)
    right = right.coerceIn(left + 1, w)
    bottom = bottom.coerceIn(top + 1, h)
    val cw = right - left
    val ch = bottom - top
    return try {
        android.graphics.Bitmap.createBitmap(src, left, top, cw, ch)
    } catch (t: Throwable) {
        src
    }
}

private fun android.graphics.Bitmap.flipHorizontally(): android.graphics.Bitmap {
    val m = android.graphics.Matrix().apply { preScale(-1f, 1f) }
    return android.graphics.Bitmap.createBitmap(this, 0, 0, width, height, m, true)
}

@Composable
private fun LandmarksOverlay(result: HLResult?, lensFacing: Int, label: String?, modifier: Modifier = Modifier) {
    if (result == null || result.hands.isEmpty()) return
    // Throttling ligero del overlay para reducir coste de dibujo
    var frameCounter by remember { mutableStateOf(0) }
    frameCounter++
    if (frameCounter % 2 != 0) return
    Canvas(modifier = modifier) {
        val viewW = size.width
        val viewH = size.height
        val srcW = result.imageWidth.toFloat().coerceAtLeast(1f)
        val srcH = result.imageHeight.toFloat().coerceAtLeast(1f)
        val scale = maxOf(viewW / srcW, viewH / srcH)
        val rW = srcW * scale
        val rH = srcH * scale
        val dx = (viewW - rW) / 2f
        val dy = (viewH - rH) / 2f
        val mirrorX = lensFacing == CameraSelector.LENS_FACING_FRONT

        val stroke = Stroke(width = 3f, cap = StrokeCap.Round)
        val pointColor = Color(0xFF00E5FF)
        val lineColor = Color(0xFF00B8D4)

        val edges = listOf(
            0 to 1, 1 to 2, 2 to 3, 3 to 4, // pulgar
            0 to 5, 5 to 6, 6 to 7, 7 to 8, // índice
            0 to 9, 9 to 10, 10 to 11, 11 to 12, // medio
            0 to 13, 13 to 14, 14 to 15, 15 to 16, // anular
            0 to 17, 17 to 18, 18 to 19, 19 to 20 // meñique
        )

        fun mapX(nx: Float): Float {
            val x = dx + nx * rW
            return if (mirrorX) viewW - x else x
        }
        fun mapY(ny: Float): Float = dy + ny * rH

        result.hands.forEach { hand ->
            // líneas
            edges.forEach { (a, b) ->
                if (a < hand.landmarks.size && b < hand.landmarks.size) {
                    val pa = hand.landmarks[a]
                    val pb = hand.landmarks[b]
                    drawLine(
                        color = lineColor,
                        start = androidx.compose.ui.geometry.Offset(mapX(pa.x), mapY(pa.y)),
                        end = androidx.compose.ui.geometry.Offset(mapX(pb.x), mapY(pb.y)),
                        strokeWidth = stroke.width,
                        cap = stroke.cap
                    )
                }
            }
            // puntos
            hand.landmarks.forEach { p ->
                drawCircle(
                    color = pointColor,
                    radius = 6f,
                    center = androidx.compose.ui.geometry.Offset(mapX(p.x), mapY(p.y))
                )
            }

            // etiquetas cerca de la muñeca (landmark 0): Left/Right + label estable si existe
            val anchor = hand.landmarks.getOrNull(0)
            val handed = when (hand.handedness?.lowercase()) {
                "left" -> "Left"
                "right" -> "Right"
                else -> hand.handedness ?: "Hand"
            }
            if (anchor != null) {
                val ax = mapX(anchor.x)
                var ay = mapY(anchor.y) - 24.dp.toPx()

                val textSize = 14.dp.toPx()
                drawIntoCanvas { canvas ->
                    val paint = android.graphics.Paint().apply {
                        color = android.graphics.Color.WHITE
                        this.textSize = textSize
                        isAntiAlias = true
                    }
                    val bgPaint = android.graphics.Paint().apply {
                        color = android.graphics.Color.parseColor("#66000000")
                        isAntiAlias = true
                    }
                    val padding = 8.dp.toPx()
                    fun drawTag(text: String) {
                        val textWidth = paint.measureText(text)
                        val textHeight = paint.fontMetrics.let { it.bottom - it.top }
                        val left = ax - textWidth / 2 - padding
                        val top = ay - textHeight - padding
                        val right = ax + textWidth / 2 + padding
                        val bottom = ay + padding / 2
                        val rect = android.graphics.RectF(left, top, right, bottom)
                        canvas.nativeCanvas.drawRoundRect(rect, 12.dp.toPx(), 12.dp.toPx(), bgPaint)
                        canvas.nativeCanvas.drawText(text, ax - textWidth / 2, ay, paint)
                        ay -= (textHeight + 12.dp.toPx())
                    }
                    drawTag(handed)
                    if (!label.isNullOrBlank()) drawTag(label)
                }
            }
        }
    }
}
