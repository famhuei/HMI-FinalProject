package com.example.amobileappfordisabledpeople.features.face_detection

import androidx.annotation.OptIn
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.example.amobileappfordisabledpeople.features.FaceDetectorProvider.faceDetector
import com.google.android.gms.tasks.Task
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.Face
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions

class FaceDetectionAnalyzer(
    private val onFaceDetected: (faces: MutableList<Face>, width: Int, height: Int) -> Unit
): ImageAnalysis.Analyzer {

    @OptIn(ExperimentalGetImage::class)
    override fun analyze(image: ImageProxy) {
        image.image?.let {
            val imageValue = InputImage.fromMediaImage(it, image.imageInfo.rotationDegrees)
            faceDetector.process(imageValue)
                .addOnCompleteListener { task: Task<List<Face>> ->
                    if (task.isSuccessful) {
                        val faces = task.result
                        val imageWidth = image.width
                        val imageHeight = image.height

                        // Find the most prominent face (largest bounding box)
                        val mostProminentFace = faces.maxByOrNull { it.boundingBox.width() * it.boundingBox.height() }

                        onFaceDetected(mostProminentFace?.let { mutableListOf(it) } ?: mutableListOf(), imageWidth, imageHeight)
                    }
                    image.image?.close()
                    image.close()
                }
        }

    }

}