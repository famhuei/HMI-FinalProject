package com.example.amobileappfordisabledpeople.features.face_recognition

import android.content.Context
import android.graphics.Bitmap
import androidx.core.graphics.drawable.toBitmap
import com.google.mlkit.vision.common.InputImage
import org.tensorflow.lite.support.common.ops.NormalizeOp
import org.tensorflow.lite.support.image.ImageProcessor
import org.tensorflow.lite.support.image.TensorImage
import org.tensorflow.lite.support.image.ops.ResizeOp

fun preprocessImage(tensorImage: TensorImage): TensorImage {

    val imageProcessor = ImageProcessor.Builder()
        .add(ResizeOp(112, 112, ResizeOp.ResizeMethod.BILINEAR))
        .add(NormalizeOp(0f, 255f))
        .build()

    return imageProcessor.process(tensorImage)
}

fun loadDrawableAsBitmap(context: Context, drawableId: Int): Bitmap {
    val drawable = context.resources.getDrawable(drawableId, null)
    return drawable.toBitmap()
}