package com.signlearn.app.translator

import android.content.Context
import org.tensorflow.lite.Interpreter
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel

class GestureClassifier(private val context: Context) {
    data class Prediction(val label: String, val confidence: Float)

    private var interpreter: Interpreter? = null
    private var labels: List<String> = emptyList()
    private var inputBuffer: ByteBuffer? = null
    private var inputSize: Int = -1

    fun load(modelAsset: String = "gesture_frame_mlp.tflite", labelsAsset: String = "labels.json") {
        val model = loadModelFile(modelAsset)
        interpreter = Interpreter(model)
        labels = context.assets.open(labelsAsset).use { stream ->
            val text = stream.bufferedReader().readText()
            // labels.json es un array de strings
            Regex("\"(.*?)\"")
                .findAll(text)
                .map { it.groupValues[1] }
                .toList()
        }
    }

    fun isReady(): Boolean = interpreter != null && labels.isNotEmpty()

    // Convierte features (float[]) a input para el MLP y devuelve la mejor predicción
    fun classify(features: FloatArray): Prediction? {
        val tflite = interpreter ?: return null
        if (features.isEmpty()) return null
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
