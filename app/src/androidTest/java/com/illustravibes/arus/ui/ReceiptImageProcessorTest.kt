package com.illustravibes.arus.ui

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.net.Uri
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ReceiptImageProcessorTest {
    @Test
    fun galleryUriCopiesImageIntoPrivateFile() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val source = java.io.File(context.cacheDir, "gallery-source.png")
        val destination = java.io.File(context.cacheDir, "gallery-copy.png")
        val bitmap = Bitmap.createBitmap(80, 120, Bitmap.Config.ARGB_8888)
        bitmap.eraseColor(Color.WHITE)
        source.outputStream().use { check(bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)) }
        bitmap.recycle()

        try {
            ReceiptImageProcessor.copyFromUri(context.contentResolver, Uri.fromFile(source), destination)
            assertTrue(destination.exists())
            assertTrue(destination.length() > 0)
            val copied = ReceiptImageProcessor.decode(destination)
            assertEquals(80, copied.width)
            copied.recycle()
        } finally {
            source.delete()
            destination.delete()
        }
    }

    @Test
    fun cropAndRotateKeepReceiptPixelsInsideBounds() {
        val bitmap = Bitmap.createBitmap(400, 600, Bitmap.Config.ARGB_8888)
        bitmap.eraseColor(Color.WHITE)

        val cropped = ReceiptImageProcessor.crop(bitmap, ReceiptCrop(left = 0.1f, top = 0.1f, right = 0.1f, bottom = 0.1f))
        val rotated = ReceiptImageProcessor.rotate(cropped, 90)

        assertEquals(480, rotated.width)
        assertEquals(320, rotated.height)
        assertEquals(Color.WHITE, rotated.getPixel(rotated.width / 2, rotated.height / 2))
        rotated.recycle()
        bitmap.recycle()
    }

    @Test
    fun bundledOcrRecognizesTextWithoutNetwork() = runBlocking {
        val bitmap = Bitmap.createBitmap(1200, 300, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(Color.WHITE)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.BLACK
            textSize = 112f
            typeface = android.graphics.Typeface.create("sans-serif", android.graphics.Typeface.BOLD)
        }
        canvas.drawText("TOTAL 12345", 40f, 190f, paint)

        val text = try {
            ReceiptImageProcessor.recognize(bitmap)
        } finally {
            bitmap.recycle()
        }

        assertTrue("OCR should return receipt text: $text", text.contains("12345"))
    }
}
