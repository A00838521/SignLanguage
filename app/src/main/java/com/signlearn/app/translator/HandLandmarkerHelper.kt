package com.signlearn.app.translator

import android.content.Context
import android.graphics.*
import android.os.SystemClock
import android.util.Log
import androidx.camera.core.ImageProxy
import com.google.mediapipe.framework.image.BitmapImageBuilder
import com.google.mediapipe.framework.image.MediaImageBuilder
import com.google.mediapipe.framework.image.MPImage
import com.google.mediapipe.tasks.core.BaseOptions
import com.google.mediapipe.tasks.core.Delegate
import com.google.mediapipe.tasks.vision.core.RunningMode
import com.google.mediapipe.tasks.vision.core.ImageProcessingOptions
import com.google.mediapipe.tasks.vision.handlandmarker.HandLandmarker
import com.google.mediapipe.tasks.vision.handlandmarker.HandLandmarkerResult
import java.io.ByteArrayOutputStream

/**
 * Helper para MediaPipe Hand Landmarker (Tasks Vision).
 * Requiere el archivo de modelo en assets: "hand_landmarker.task".
 * Si no está presente, funcionará en modo NO-OP y emitirá null.
 */
class HandLandmarkerHelper(private val context: Context) {
    companion object {
        private const val TAG = "HandLMHelper"
        private const val MODEL_ASSET = "hand_landmarker.task"
    }

    data class Landmark(val x: Float, val y: Float)
    data class Hand(val landmarks: List<Landmark>, val handedness: String?)
    data class Result(
        val hands: List<Hand>,
        val confidence: Float,
        val imageWidth: Int,
        val imageHeight: Int,
        val bitmap: Bitmap,
    )

    private var landmarker: HandLandmarker? = null
    private var isReady: Boolean = false

    init {
        // Verificar que el asset exista
        val hasAsset = runCatching { context.assets.open(MODEL_ASSET).close() }.isSuccess
        if (!hasAsset) {
            Log.w(TAG, "Asset $MODEL_ASSET no encontrado. El Landmarker quedará deshabilitado.")
            isReady = false
        } else {
            try {
                val base = BaseOptions.builder()
                    .setModelAssetPath(MODEL_ASSET)
                    .setDelegate(Delegate.CPU)
                    .build()

                val options = HandLandmarker.HandLandmarkerOptions.builder()
                    .setBaseOptions(base)
                    .setMinHandDetectionConfidence(0.5f)
                    .setMinHandPresenceConfidence(0.5f)
                    .setMinTrackingConfidence(0.5f)
                    .setNumHands(2)
                    .setRunningMode(RunningMode.LIVE_STREAM)
                    .setResultListener(::onResult)
                    .setErrorListener { err -> Log.e(TAG, "Landmarker error: $err") }
                    .build()

                landmarker = HandLandmarker.createFromOptions(context, options)
                isReady = true
            } catch (t: Throwable) {
                Log.e(TAG, "No se pudo inicializar HandLandmarker", t)
                isReady = false
            }
        }
    }

    private var lastCallback: ((Result?) -> Unit)? = null

    private var lastWidth: Int = 0
    private var lastHeight: Int = 0

    private var lastBitmap: Bitmap? = null

    private fun onResult(result: HandLandmarkerResult, input: MPImage) {
        val handsLandmarks = result.landmarks()?.mapIndexed { idx, list ->
            val points = list.map { p -> Landmark(p.x(), p.y()) }
            val label = result.handednesses()?.getOrNull(idx)?.firstOrNull()?.categoryName()
            Hand(points, label)
        } ?: emptyList()
        val conf = result.handednesses()?.firstOrNull()?.firstOrNull()?.score() ?: 0f
        val out = Result(
            hands = handsLandmarks,
            confidence = conf,
            imageWidth = lastWidth,
            imageHeight = lastHeight,
            bitmap = lastBitmap ?: Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888)
        )
        lastCallback?.invoke(out)
    }

    fun analyze(imageProxy: ImageProxy, requireBitmap: Boolean = true, onResult: (Result?) -> Unit) {
        try {
            if (!isReady) {
                onResult(null)
                return
            }
            lastCallback = onResult
            // Forzar ruta Bitmap ARGB_8888 para evitar mismatches de buffer en MediaImage
            val rotation = imageProxy.imageInfo.rotationDegrees
            val ts = SystemClock.uptimeMillis()
            val baseBmp = imageProxy.toBitmap()
            val rotated = if (rotation != 0) baseBmp.rotate(rotation.toFloat()) else baseBmp
            lastWidth = rotated.width
            lastHeight = rotated.height
            lastBitmap = if (requireBitmap) rotated else null
            val mpImage = BitmapImageBuilder(rotated).build()
            landmarker?.detectAsync(mpImage, ts)
        } catch (t: Throwable) {
            Log.e(TAG, "analyze error", t)
            onResult(null)
        } finally {
            imageProxy.close()
        }
    }

    fun close() {
        runCatching { landmarker?.close() }
        landmarker = null
        isReady = false
    }
}

// --- Utilidades de conversión/rotación ---

private fun ImageProxy.toBitmap(): Bitmap {
    // Fast path: RGBA_8888 from ImageAnalysis (one plane)
    if (planes.size == 1 && planes[0].pixelStride == 4) {
        val p0 = planes[0]
        val buf = p0.buffer
        val rowStride = p0.rowStride
        val pixelStride = p0.pixelStride // should be 4 (RGBA)
        val bytes = ByteArray(buf.remaining())
        buf.get(bytes)
        buf.rewind()
        val bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        // Optimización: si el buffer es contiguo (rowStride == width*4), convertir en bloque
        if (rowStride == width * 4) {
            val out = IntArray(width * height)
            var si = 0
            var di = 0
            val total = width * height
            while (di < total) {
                val r = bytes[si + 0].toInt() and 0xFF
                val g = bytes[si + 1].toInt() and 0xFF
                val b = bytes[si + 2].toInt() and 0xFF
                val a = bytes[si + 3].toInt() and 0xFF
                out[di] = (a shl 24) or (r shl 16) or (g shl 8) or b
                si += pixelStride
                di++
            }
            bmp.setPixels(out, 0, width, 0, 0, width, height)
        } else {
            // Copia por filas si hay padding de stride
            val tmp = IntArray(width)
            var srcOffset: Int
            for (j in 0 until height) {
                val rowStart = j * rowStride
                srcOffset = rowStart
                var i = 0
                var col = 0
                while (col < width) {
                    val r = bytes[srcOffset + 0].toInt() and 0xFF
                    val g = bytes[srcOffset + 1].toInt() and 0xFF
                    val b = bytes[srcOffset + 2].toInt() and 0xFF
                    val a = bytes[srcOffset + 3].toInt() and 0xFF
                    tmp[i++] = (a shl 24) or (r shl 16) or (g shl 8) or b
                    srcOffset += pixelStride
                    col++
                }
                bmp.setPixels(tmp, 0, width, 0, j, width, 1)
            }
        }
        return bmp
    }

    // Fallback: YUV_420_888 manual conversion
    val yPlane = planes[0]
    val uPlane = planes[1]
    val vPlane = planes[2]

    val yBuffer = yPlane.buffer
    val uBuffer = uPlane.buffer
    val vBuffer = vPlane.buffer

    // Copiar a arrays para acceso aleatorio rápido sin mover posiciones del buffer
    val ySize = yBuffer.remaining()
    val uSize = uBuffer.remaining()
    val vSize = vBuffer.remaining()
    val yArr = ByteArray(ySize)
    val uArr = ByteArray(uSize)
    val vArr = ByteArray(vSize)
    yBuffer.get(yArr)
    uBuffer.get(uArr)
    vBuffer.get(vArr)
    yBuffer.rewind(); uBuffer.rewind(); vBuffer.rewind()

    val out = IntArray(width * height)

    val yRowStride = yPlane.rowStride
    val yPixelStride = yPlane.pixelStride
    val uRowStride = uPlane.rowStride
    val uPixelStride = uPlane.pixelStride
    val vRowStride = vPlane.rowStride
    val vPixelStride = vPlane.pixelStride

    var outIndex = 0
    var yIndex: Int
    var uIndex: Int
    var vIndex: Int

    fun clamp(v: Int) = if (v < 0) 0 else if (v > 255) 255 else v

    for (j in 0 until height) {
        val yRow = j * yRowStride
        val uRow = (j / 2) * uRowStride
        val vRow = (j / 2) * vRowStride
        for (i in 0 until width) {
            yIndex = yRow + i * yPixelStride
            uIndex = uRow + (i / 2) * uPixelStride
            vIndex = vRow + (i / 2) * vPixelStride

            val y = (yArr[yIndex].toInt() and 0xFF)
            val u = (uArr[uIndex].toInt() and 0xFF)
            val v = (vArr[vIndex].toInt() and 0xFF)

            val c = y - 16
            val d = u - 128
            val e = v - 128
            var r = (298 * c + 409 * e + 128) shr 8
            var g = (298 * c - 100 * d - 208 * e + 128) shr 8
            var b = (298 * c + 516 * d + 128) shr 8
            r = clamp(r); g = clamp(g); b = clamp(b)
            out[outIndex++] = (0xFF shl 24) or (r shl 16) or (g shl 8) or b
        }
    }
    val bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    bmp.setPixels(out, 0, width, 0, 0, width, height)
    return bmp
}

private fun Bitmap.rotate(degrees: Float): Bitmap {
    val m = Matrix()
    m.postRotate(degrees)
    return Bitmap.createBitmap(this, 0, 0, width, height, m, true)
}
