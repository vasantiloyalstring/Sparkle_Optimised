package com.loyalstring.rfid.ui.screens
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL

fun saveImageFromUrlToLocal(
    context: Context,
    imageUrl: String,
    itemCode: String
): File? {
    return try {
        val imageDir = File(context.filesDir, "product_images")
        if (!imageDir.exists()) {
            imageDir.mkdirs()
        }

        val file = File(imageDir, "${itemCode.trim()}.jpg")

        if (file.exists()) {
            Log.d("LocalImageSave", "Already exists: ${file.absolutePath}")
            return file
        }

        val url = URL(imageUrl)
        val connection = url.openConnection() as HttpURLConnection
        connection.connectTimeout = 10000
        connection.readTimeout = 10000
        connection.doInput = true
        connection.connect()

        if (connection.responseCode != HttpURLConnection.HTTP_OK) {
            Log.e("LocalImageSave", "HTTP error: ${connection.responseCode}")
            return null
        }

        val inputStream = connection.inputStream
        val bitmap = BitmapFactory.decodeStream(inputStream)
        inputStream.close()
        connection.disconnect()

        if (bitmap == null) {
            Log.e("LocalImageSave", "Bitmap decode failed")
            return null
        }

        FileOutputStream(file).use { output ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, output)
            output.flush()
        }

        Log.d("LocalImageSave", "Saved: ${file.absolutePath}")
        file
    } catch (e: Exception) {
        Log.e("LocalImageSave", "Save failed: ${e.message}", e)
        null
    }
}