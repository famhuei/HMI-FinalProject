package com.example.amobileappfordisabledpeople.features.face_recognition

import android.content.Context
import com.example.amobileappfordisabledpeople.R
import com.example.amobileappfordisabledpeople.features.FaceDetectorProvider.faceDetector
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions
import org.tensorflow.lite.support.image.TensorImage


object EmbeddingStore {
    private var storedImageEmbeddings: List<FloatArray> = mutableListOf()

    fun initialize(context: Context, faceNetModel: FaceNetModel) {
        if (storedImageEmbeddings.isEmpty()) {
            storedImageEmbeddings = embeddingStoredImages(context, faceNetModel)
        }
    }

    fun getEmbeddings(): List<FloatArray> {
        return storedImageEmbeddings
    }

    private fun embeddingStoredImages(context: Context, faceNetModel: FaceNetModel): List<FloatArray> {
        val famousImages = listOf(
            R.drawable.billie_eilish,
            R.drawable.david_beckham,
            R.drawable.donal_trump,
            R.drawable.mtp,
            R.drawable.rihanna,
            R.drawable.thu_vu,
//            R.drawable.hung,
            R.drawable.ha
        )

        val embeddings = mutableListOf<FloatArray>()

        famousImages.forEach { drawableResId ->
            val bitmap = loadDrawableAsBitmap(context, drawableResId)
            val inputImage = InputImage.fromBitmap(bitmap, 0)

            faceDetector.process(inputImage)
                .addOnSuccessListener { faces ->
                    faces.forEach { face ->
                        val faceBitmap = cropFace(bitmap, face.boundingBox, 0f)
                        if (faceBitmap != null) {
                            val tensorImage = TensorImage.fromBitmap(faceBitmap)
                            val preprocessedImage = preprocessImage(tensorImage)
                            val embedding = faceNetModel.getEmbedding(preprocessedImage)
                            embeddings.add(embedding)
                        }
                    }
                }
                .addOnFailureListener { e ->
                    e.printStackTrace()
                }
        }
        return embeddings
    }
}