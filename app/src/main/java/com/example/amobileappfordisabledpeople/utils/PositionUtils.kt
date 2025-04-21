package com.example.amobileappfordisabledpeople.utils

import android.graphics.PointF
import androidx.compose.ui.geometry.Size

fun adjustPoint(point: PointF, imageWidth: Int, imageHeight: Int, screenWidth: Int, screenHeight: Int): PointF {
    val scaleX = screenWidth.toFloat() / imageWidth
    val scaleY = screenHeight.toFloat() / imageHeight
    val scale = minOf(scaleX, scaleY)

    val offsetX = (screenWidth - imageWidth * scale) / 2
    val offsetY = (screenHeight - imageHeight * scale) / 2

    return PointF(
        point.x * scale + offsetX,
        point.y * scale + offsetY
    )
}

fun adjustSize(size: Size, imageWidth: Int, imageHeight: Int, screenWidth: Int, screenHeight: Int): Size {
    val scaleX = screenWidth.toFloat() / imageWidth
    val scaleY = screenHeight.toFloat() / imageHeight
    val scale = minOf(scaleX, scaleY)

    return Size(
        size.width * scale,
        size.height * scale
    )
}