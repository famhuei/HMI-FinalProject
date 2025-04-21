package com.example.amobileappfordisabledpeople.features.object_detection

import android.content.Context
import android.graphics.Bitmap
import android.graphics.ImageFormat
import android.graphics.Rect
import android.graphics.YuvImage
import android.graphics.BitmapFactory
import android.media.Image
import java.io.ByteArrayOutputStream

/**
 * Helper class used to efficiently convert a [Media.Image] object from
 * [ImageFormat.YUV_420_888] format to an RGB [Bitmap] object.
 *
 * The [yuvToRgb] method is optimized to achieve efficient conversion with good performance.
 *
 * NOTE: This code has been tested on a limited number of devices and is not considered
 * production-ready. It is provided for illustration purposes and may require further
 * optimization for specific devices or use cases.
 */

class YuvToRgbConverter(private val context: Context) {

    @Synchronized
    fun yuvToRgb(image: Image, output: Bitmap) {
        val yuvBytes = imageToByteArray(image)

        // Convert YUV to Bitmap
        val yuvImage = YuvImage(yuvBytes, ImageFormat.NV21, image.width, image.height, null)
        val outStream = ByteArrayOutputStream()
        yuvImage.compressToJpeg(Rect(0, 0, image.width, image.height), 100, outStream)
        val jpegBytes = outStream.toByteArray()

        // Decode JPEG bytes to Bitmap
        val bitmap = BitmapFactory.decodeByteArray(jpegBytes, 0, jpegBytes.size)

        // Clear the output Bitmap
        output.eraseColor(0) // Clear the output Bitmap

        // Draw the converted bitmap onto the output bitmap
        val canvas = android.graphics.Canvas(output)
        canvas.drawBitmap(bitmap, 0f, 0f, null)
    }

    private fun imageToByteArray(image: Image): ByteArray {
        assert(image.format == ImageFormat.YUV_420_888)

        val planes = image.planes
        val ySize = planes[0].buffer.remaining()
        val uSize = planes[1].buffer.remaining()
        val vSize = planes[2].buffer.remaining()

        val yuvBytes = ByteArray(ySize + uSize + vSize)
        planes[0].buffer.get(yuvBytes, 0, ySize)
        planes[1].buffer.get(yuvBytes, ySize, uSize)
        planes[2].buffer.get(yuvBytes, ySize + uSize, vSize)

        return yuvBytes
    }
}