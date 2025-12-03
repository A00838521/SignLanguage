package com.signlearn.app.translator

import android.content.Context
import android.graphics.*
import android.os.SystemClock
import android.util.Log
import androidx.camera.core.ImageProxy
import com.google.mediapipe.framework.image.BitmapImageBuilder
import com.google.mediapipe.framework.image.MPImage
import com.google.mediapipe.tasks.core.BaseOptions
import com.google.mediapipe.tasks.core.Delegate
import com.google.mediapipe.tasks.vision.core.RunningMode
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
            imageHeight = lastHeight
        )
        lastCallback?.invoke(out)
    }

    fun analyze(imageProxy: ImageProxy, onResult: (Result?) -> Unit) {
        try {
            if (!isReady) {
                onResult(null)
                return
            }
            lastCallback = onResult

            val rotation = imageProxy.imageInfo.rotationDegrees
            val bitmap = imageProxy.toBitmap()
            val rotated = if (rotation != 0) bitmap.rotate(rotation.toFloat()) else bitmap
            lastWidth = rotated.width
            lastHeight = rotated.height
            val mpImage = BitmapImageBuilder(rotated).build()
            landmarker?.detectAsync(mpImage, SystemClock.uptimeMillis())
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
    val nv21 = yuv420888toNv21()
    val yuvImage = YuvImage(nv21, ImageFormat.NV21, width, height, null)
    val out = ByteArrayOutputStream()
    yuvImage.compressToJpeg(Rect(0, 0, width, height), 90, out)
    val imageBytes = out.toByteArray()
    return BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
}

private fun ImageProxy.yuv420888toNv21(): ByteArray {
    val yBuffer = planes[0].buffer
    val uBuffer = planes[1].buffer
    val vBuffer = planes[2].buffer

    val ySize = yBuffer.remaining()
    val uSize = uBuffer.remaining()
    val vSize = vBuffer.remaining()

    val nv21 = ByteArray(ySize + uSize + vSize)

    yBuffer.get(nv21, 0, ySize)
    val chromaRowStride = planes[1].rowStride
    val chromaRowPadding = chromaRowStride - width / 2

    var offset = ySize
    if (chromaRowPadding == 0) {
        vBuffer.get(nv21, offset, vSize)
        offset += vSize
        uBuffer.get(nv21, offset, uSize)
    } else {
        for (row in 0 until height / 2) {
            vBuffer.get(nv21, offset, width / 2)
            offset += width / 2
            vBuffer.position(vBuffer.position() + chromaRowPadding)

            uBuffer.get(nv21, offset, width / 2)
            offset += width / 2
            uBuffer.position(uBuffer.position() + chromaRowPadding)
        }
    }
    return nv21
}

private fun Bitmap.rotate(degrees: Float): Bitmap {
    val m = Matrix()
    m.postRotate(degrees)
    return Bitmap.createBitmap(this, 0, 0, width, height, m, true)
}
