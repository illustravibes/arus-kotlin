package com.illustravibes.arus.ui

import android.content.ContentResolver
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import androidx.exifinterface.media.ExifInterface
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import java.io.File
import java.io.FileOutputStream
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.util.UUID
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.suspendCancellableCoroutine

internal data class ReceiptCrop(
    val left: Float = 0.025f,
    val top: Float = 0.025f,
    val right: Float = 0.025f,
    val bottom: Float = 0.025f,
)

internal object ReceiptImageProcessor {
    private const val MAX_IMAGE_DIMENSION = 2048
    private const val MAX_INPUT_FILE_BYTES = 50L * 1024L * 1024L

    fun copyFromUri(contentResolver: ContentResolver, uri: Uri, destination: File) {
        val input = contentResolver.openInputStream(uri) ?: throw IllegalArgumentException("Foto tidak dapat dibuka.")
        try {
            BufferedInputStream(input).use { source ->
                BufferedOutputStream(destination.outputStream()).use { output ->
                    val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                    var bytesCopied = 0L
                    while (true) {
                        val count = source.read(buffer)
                        if (count < 0) break
                        bytesCopied += count
                        require(bytesCopied <= MAX_INPUT_FILE_BYTES) { "Ukuran foto melebihi 50 MB." }
                        output.write(buffer, 0, count)
                    }
                }
            }
        } catch (failure: Exception) {
            destination.delete()
            throw failure
        }
    }

    fun decode(file: File): Bitmap {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(file.absolutePath, bounds)
        require(bounds.outWidth > 0 && bounds.outHeight > 0) { "Berkas foto tidak valid." }
        var sampleSize = 1
        while (bounds.outWidth / sampleSize > MAX_IMAGE_DIMENSION || bounds.outHeight / sampleSize > MAX_IMAGE_DIMENSION) {
            sampleSize *= 2
        }
        val bitmap = BitmapFactory.decodeFile(file.absolutePath, BitmapFactory.Options().apply {
            inSampleSize = sampleSize
            inPreferredConfig = Bitmap.Config.ARGB_8888
        }) ?: throw IllegalArgumentException("Foto tidak dapat dibaca.")
        val rotation = runCatching { ExifInterface(file).rotationDegrees }.getOrDefault(0)
        return rotate(bitmap, rotation)
    }

    fun rotate(bitmap: Bitmap, degrees: Int): Bitmap {
        val normalized = ((degrees % 360) + 360) % 360
        if (normalized == 0) return bitmap
        val matrix = Matrix().apply { postRotate(normalized.toFloat()) }
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }

    fun crop(bitmap: Bitmap, crop: ReceiptCrop): Bitmap {
        val left = (bitmap.width * crop.left.coerceIn(0f, 0.8f)).toInt()
        val top = (bitmap.height * crop.top.coerceIn(0f, 0.8f)).toInt()
        val right = (bitmap.width * (1f - crop.right.coerceIn(0f, 0.8f))).toInt()
        val bottom = (bitmap.height * (1f - crop.bottom.coerceIn(0f, 0.8f))).toInt()
        require(right - left >= bitmap.width / 5 && bottom - top >= bitmap.height / 5) {
            "Area potong terlalu kecil."
        }
        return Bitmap.createBitmap(bitmap, left, top, right - left, bottom - top)
    }

    fun saveJpeg(bitmap: Bitmap, directory: File): File {
        val targetDirectory = File(directory, "receipts").apply { check(isDirectory || mkdirs()) }
        val output = File(targetDirectory, "${UUID.randomUUID()}.jpg")
        FileOutputStream(output).use { stream ->
            check(bitmap.compress(Bitmap.CompressFormat.JPEG, 90, stream)) { "Foto struk tidak dapat disimpan." }
            stream.fd.sync()
        }
        return output
    }

    suspend fun recognize(bitmap: Bitmap): String = suspendCancellableCoroutine { continuation ->
        val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
        val task = recognizer.process(InputImage.fromBitmap(bitmap, 0))
        task.addOnSuccessListener { result ->
            if (continuation.isActive) continuation.resume(result.text)
            recognizer.close()
        }.addOnFailureListener { error ->
            if (continuation.isActive) continuation.resumeWithException(error)
            recognizer.close()
        }
        continuation.invokeOnCancellation { recognizer.close() }
    }
}
