package com.loyalstring.rfid.ui.utils

import android.content.Context
import android.graphics.Bitmap
import org.tensorflow.lite.Interpreter
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel

class FaceRecognizerHelper(context: Context) {

    private var interpreter: Interpreter? = null

    init {
        try {
            val model = loadModelFile(context, "mobile_face_net.tflite")
            interpreter = Interpreter(model)
        } catch (e: Exception) {
            e.printStackTrace()
            interpreter = null
        }
    }

    fun isModelLoaded(): Boolean = interpreter != null

    fun getEmbedding(bitmap: Bitmap): FloatArray {
        val localInterpreter = interpreter
            ?: throw IllegalStateException("mobile_face_net.tflite not found in assets")

        val resized = Bitmap.createScaledBitmap(bitmap, 112, 112, true)
        val input = convertBitmapToBuffer(resized)

        // Change 192 if your model output size is different
        val output = Array(1) { FloatArray(192) }
        localInterpreter.run(input, output)

        return output[0]
    }

    private fun convertBitmapToBuffer(bitmap: Bitmap): ByteBuffer {
        val buffer = ByteBuffer.allocateDirect(1 * 112 * 112 * 3 * 4)
        buffer.order(ByteOrder.nativeOrder())

        val pixels = IntArray(112 * 112)
        bitmap.getPixels(pixels, 0, 112, 0, 0, 112, 112)

        var pixelIndex = 0
        for (i in 0 until 112) {
            for (j in 0 until 112) {
                val pixel = pixels[pixelIndex++]

                val r = ((pixel shr 16) and 0xFF) / 255.0f
                val g = ((pixel shr 8) and 0xFF) / 255.0f
                val b = (pixel and 0xFF) / 255.0f

                buffer.putFloat((r - 0.5f) / 0.5f)
                buffer.putFloat((g - 0.5f) / 0.5f)
                buffer.putFloat((b - 0.5f) / 0.5f)
            }
        }

        buffer.rewind()
        return buffer
    }

    private fun loadModelFile(context: Context, modelName: String): MappedByteBuffer {
        val fileDescriptor = context.assets.openFd(modelName)
        android.util.Log.d("TFLITE", "Assets = $fileDescriptor")
        android.util.Log.d("TFLITE", "Trying to load = $modelName")

        val inputStream = FileInputStream(fileDescriptor.fileDescriptor)
        val fileChannel = inputStream.channel
        val startOffset = fileDescriptor.startOffset
        val declaredLength = fileDescriptor.declaredLength

        return fileChannel.map(
            FileChannel.MapMode.READ_ONLY,
            startOffset,
            declaredLength
        )
    }
}