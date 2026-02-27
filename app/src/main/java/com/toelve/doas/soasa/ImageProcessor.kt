package com.toelve.doas.soasa

import android.graphics.*
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*
import androidx.exifinterface.media.ExifInterface

object ImageProcessor {

    fun processAndSave(
        file: File,
        isFrontCamera: Boolean,
        alamat: String,
        lat: Double?,
        lon: Double?,
        quality: Int = 50
    ) {

        val original = BitmapFactory.decodeFile(file.absolutePath) ?: return

        val rotated = fixOrientation(original, file, isFrontCamera)

        // resize ke 720
        val resized = resizeBitmap(rotated, 720)

        val watermarked = addWatermark(resized, alamat, lat, lon)

        FileOutputStream(file).use {
            watermarked.compress(Bitmap.CompressFormat.WEBP, quality, it)
        }

        original.recycle()
        rotated.recycle()
        resized.recycle()
        watermarked.recycle()
    }


    /* ================= ROTASI ================= */

    private fun fixOrientation(
        bitmap: Bitmap,
        file: File,
        isFrontCamera: Boolean
    ): Bitmap {

        val exif = ExifInterface(file.absolutePath)

        val rotation = when (
            exif.getAttributeInt(
                ExifInterface.TAG_ORIENTATION,
                ExifInterface.ORIENTATION_NORMAL
            )
        ) {
            ExifInterface.ORIENTATION_ROTATE_90 -> 90
            ExifInterface.ORIENTATION_ROTATE_180 -> 180
            ExifInterface.ORIENTATION_ROTATE_270 -> 270
            else -> 0
        }

        val matrix = Matrix()

        if (rotation != 0) {
            matrix.postRotate(rotation.toFloat())
        }

        if (isFrontCamera) {
            matrix.postScale(-1f, 1f)
        }

        return Bitmap.createBitmap(
            bitmap,
            0,
            0,
            bitmap.width,
            bitmap.height,
            matrix,
            true
        )
    }

    /* ================= RESIZE ================= */

    private fun resizeBitmap(
        bitmap: Bitmap,
        maxSize: Int = 1280
    ): Bitmap {

        val width = bitmap.width
        val height = bitmap.height

        if (width <= maxSize && height <= maxSize) {
            return bitmap
        }

        val ratio = width.toFloat() / height.toFloat()

        val newWidth: Int
        val newHeight: Int

        if (ratio > 1) {
            newWidth = maxSize
            newHeight = (maxSize / ratio).toInt()
        } else {
            newHeight = maxSize
            newWidth = (maxSize * ratio).toInt()
        }

        return Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true)
    }

    /* ================= WATERMARK ================= */

    private fun addWatermark(
        bitmap: Bitmap,
        alamat: String,
        lat: Double?,
        lon: Double?
    ): Bitmap {

        val result = bitmap.copy(Bitmap.Config.ARGB_8888, true)
        val canvas = Canvas(result)

        val textSize = bitmap.width * 0.025f

        val paintText = Paint().apply {
            color = Color.WHITE
            this.textSize = textSize
            isAntiAlias = true
            setShadowLayer(3f, 0f, 0f, Color.BLACK)
        }

        val paintBg = Paint().apply {
            color = Color.argb(70, 0, 0, 0)
        }

        val waktu = SimpleDateFormat(
            "yyyy-MM-dd HH:mm",
            Locale.getDefault()
        ).format(Date())

        val latlonText =
            if (lat != null && lon != null)
                "Lat %.5f  Lon %.5f".format(lat, lon)
            else
                "Lat/Lon tidak tersedia"

        // WRAP alamat
        val maxWidth = bitmap.width * 0.90f
        val alamatLines = wrapText(alamat, paintText, maxWidth)

        val lines = mutableListOf<String>()
        lines.add(waktu)
        lines.addAll(alamatLines)
        lines.add(latlonText)

        val padding = textSize * 0.6f
        val lineHeight = textSize * 1.3f
        val boxHeight = (lines.size * lineHeight) + padding * 2

        canvas.drawRect(
            padding,
            bitmap.height - boxHeight - padding,
            bitmap.width - padding,
            bitmap.height - padding,
            paintBg
        )

        var y = bitmap.height - boxHeight + lineHeight

        for (line in lines) {
            canvas.drawText(line, padding * 2, y, paintText)
            y += lineHeight
        }

        return result
    }

    /* ================= TEXT WRAP ================= */

    private fun wrapText(
        text: String,
        paint: Paint,
        maxWidth: Float
    ): List<String> {

        val words = text.split(" ")
        val lines = mutableListOf<String>()
        var currentLine = ""

        for (word in words) {

            val testLine =
                if (currentLine.isEmpty()) word
                else "$currentLine $word"

            if (paint.measureText(testLine) <= maxWidth) {
                currentLine = testLine
            } else {
                lines.add(currentLine)
                currentLine = word
            }
        }

        if (currentLine.isNotEmpty()) {
            lines.add(currentLine)
        }

        return lines
    }
}
