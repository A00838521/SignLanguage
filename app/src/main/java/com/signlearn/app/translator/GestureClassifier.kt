package com.signlearn.app.translator

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel

class GestureClassifier(private val context: Context) {
    data class Prediction(val label: String, val confidence: Float)
    companion object { private const val TAG = "GestureClassifier" }

    private var interpreter: Interpreter? = null
    private var labels: List<String> = emptyList()
    private var inputBuffer: ByteBuffer? = null
    private var inputBufferAlt: ByteBuffer? = null
    private var inputSize: Int = -1
    private var isImageModel: Boolean = false
    private var imgW: Int = 0
    private var imgH: Int = 0
    private var imgC: Int = 0

    fun load(modelAsset: String = "gesture_frame_mlp.tflite", labelsAsset: String = "labels.json") {
        val model = loadModelFile(modelAsset)
        interpreter = Interpreter(model)
        // Detectar forma de entrada del modelo
        interpreter?.let { tflite ->
            val inputTensor = tflite.getInputTensor(0)
            val shape = inputTensor.shape() // e.g., [1, 224, 224, 3] o [1, N]
            isImageModel = shape.size == 4
            if (isImageModel) {
                imgH = shape[1]
                imgW = shape[2]
                imgC = if (shape.size > 3) shape[3] else 3
            } else {
                inputSize = if (shape.isNotEmpty()) shape.last() else -1
            }
        }
        labels = context.assets.open(labelsAsset).use { stream ->
            val text = stream.bufferedReader().readText()
            // labels.json es un array de strings
            Regex("\"(.*?)\"")
                .findAll(text)
                .map { it.groupValues[1] }
                .toList()
        }
        // Log de diagnóstico del orden de etiquetas y tipo de modelo
        Log.d(TAG, "loaded labels=${labels.joinToString(",")}")
        Log.d(TAG, "model input=${if (isImageModel) "image ${imgW}x${imgH}x${imgC}" else "vector size=$inputSize"}")
    }

    fun isReady(): Boolean = interpreter != null && labels.isNotEmpty()

    fun expectsImage(): Boolean = isImageModel

    // Convierte features (float[]) a input para el MLP y devuelve la mejor predicción
    fun classify(features: FloatArray): Prediction? {
        val tflite = interpreter ?: return null
        if (features.isEmpty()) return null
        if (isImageModel) return null // este modelo espera imagen, usa classify(bitmap)
        val needed = features.size
        val input = if (inputBuffer == null || inputSize != needed) {
            inputSize = needed
            inputBuffer = ByteBuffer.allocateDirect(needed * 4).order(ByteOrder.nativeOrder())
            inputBuffer!!
        } else {
            inputBuffer!!.apply { clear() }
        }
        features.forEach { input.putFloat(it) }
        input.rewind()
        val output = Array(1) { FloatArray(labels.size) }
        tflite.run(input, output)
        val logits = output[0]
        var bestIdx = 0
        var bestVal = logits[0]
        for (i in 1 until logits.size) {
            if (logits[i] > bestVal) {
                bestVal = logits[i]
                bestIdx = i
            }
        }
        val label = labels.getOrNull(bestIdx) ?: return null
        return Prediction(label, bestVal)
    }

    fun classify(bitmap: Bitmap): Prediction? {
        val tflite = interpreter ?: return null
        if (!isImageModel) return null
        if (imgW <= 0 || imgH <= 0) return null
        val resized = if (bitmap.width != imgW || bitmap.height != imgH) {
            Bitmap.createScaledBitmap(bitmap, imgW, imgH, true)
        } else bitmap
        val needed = imgW * imgH * (if (imgC > 0) imgC else 3)
        val input = if (inputBuffer == null || inputSize != needed) {
            inputSize = needed
            inputBuffer = ByteBuffer.allocateDirect(needed * 4).order(ByteOrder.nativeOrder())
            inputBuffer!!
        } else {
            inputBuffer!!.apply { clear() }
        }
        val inputAlt = if (inputBufferAlt == null || inputSize != needed) {
            inputBufferAlt = ByteBuffer.allocateDirect(needed * 4).order(ByteOrder.nativeOrder())
            inputBufferAlt!!
        } else {
            inputBufferAlt!!.apply { clear() }
        }
        // Rellenar como float32 [0,1]
        val pixels = IntArray(imgW * imgH)
        resized.getPixels(pixels, 0, imgW, 0, 0, imgW, imgH)
        for (i in pixels.indices) {
            val p = pixels[i]
            val r = ((p shr 16) and 0xFF) / 255f
            val g = ((p shr 8) and 0xFF) / 255f
            val b = (p and 0xFF) / 255f
            input.putFloat(r)
            input.putFloat(g)
            input.putFloat(b)
            // Alternativa: normalización [-1,1] por si el modelo se entrenó con preprocess_input
            inputAlt.putFloat(r * 2f - 1f)
            inputAlt.putFloat(g * 2f - 1f)
            inputAlt.putFloat(b * 2f - 1f)
        }
        input.rewind()
        inputAlt.rewind()

        // Infiere con [0,1]
        val out1 = Array(1) { FloatArray(labels.size) }
        tflite.run(input, out1)
        var bestIdx1 = 0
        var bestVal1 = out1[0][0]
        for (i in 1 until out1[0].size) if (out1[0][i] > bestVal1) { bestVal1 = out1[0][i]; bestIdx1 = i }
        val label1 = labels.getOrNull(bestIdx1) ?: "?"
        // Top-3 para [0,1]
        val idxs1 = out1[0].indices.sortedByDescending { out1[0][it] }.take(3)
        val top3_01 = idxs1.joinToString(",") { i -> "${labels.getOrNull(i) ?: "?"}=${String.format("%.2f", out1[0][i])}" }

        // Si la confianza es baja, prueba alternativa [-1,1]
        var bestIdx = bestIdx1
        var bestVal = bestVal1
        if (bestVal1 < 0.20f) {
            val out2 = Array(1) { FloatArray(labels.size) }
            tflite.run(inputAlt, out2)
            var bestIdx2 = 0
            var bestVal2 = out2[0][0]
            for (i in 1 until out2[0].size) if (out2[0][i] > bestVal2) { bestVal2 = out2[0][i]; bestIdx2 = i }
            if (bestVal2 > bestVal1) { bestVal = bestVal2; bestIdx = bestIdx2 }
            val label2 = labels.getOrNull(bestIdx2) ?: "?"
            // Top-3 para [-1,1]
            val idxs2 = out2[0].indices.sortedByDescending { out2[0][it] }.take(3)
            val top3_m11 = idxs2.joinToString(",") { i -> "${labels.getOrNull(i) ?: "?"}=${String.format("%.2f", out2[0][i])}" }
            Log.d(TAG, "infer [0,1]: $label1=${String.format("%.2f", bestVal1)} top3=[$top3_01] | [-1,1]: $label2=${String.format("%.2f", bestVal2)} top3=[$top3_m11] -> chosen=${labels.getOrNull(bestIdx) ?: "?"}=${String.format("%.2f", bestVal)}")
        } else {
            Log.d(TAG, "infer [0,1]: $label1=${String.format("%.2f", bestVal1)} top3=[$top3_01] (chosen)")
        }

        val label = labels.getOrNull(bestIdx) ?: return null
        return Prediction(label, bestVal)
    }

    fun close() {
        interpreter?.close()
        interpreter = null
    }

    private fun loadModelFile(assetName: String): MappedByteBuffer {
        val afd = context.assets.openFd(assetName)
        val input = java.io.FileInputStream(afd.fileDescriptor)
        val channel = input.channel
        return channel.map(FileChannel.MapMode.READ_ONLY, afd.startOffset, afd.length)
    }
}
