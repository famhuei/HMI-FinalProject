package com.example.amobileappfordisabledpeople.features.object_detection

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.graphics.RectF
import android.media.Image
import android.util.Log
import android.util.Size
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.example.amobileappfordisabledpeople.Data.DetectionObject
import com.example.amobileappfordisabledpeople.ObjectDetectorCallback
import org.tensorflow.lite.DataType
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.support.common.ops.NormalizeOp
import org.tensorflow.lite.support.image.ImageProcessor
import org.tensorflow.lite.support.image.TensorImage
import org.tensorflow.lite.support.image.ops.ResizeOp
import org.tensorflow.lite.support.image.ops.Rot90Op

class ObjectDetector(
    private val yuvToRgbConverter: YuvToRgbConverter,
    private val interpreter: Interpreter,
    private val labels: List<String>,
    private val resultViewSize: Size,
    private val listener: ObjectDetectorCallback
) : ImageAnalysis.Analyzer {

    companion object {
        private const val IMG_SIZE_X = 300
        private const val IMG_SIZE_Y = 300
        private const val MAX_DETECTION_NUM = 10
        private const val NORMALIZE_MEAN = 0f
        private const val NORMALIZE_STD = 1f
        private const val SCORE_THRESHOLD = 0.6f

        // Approximate focal length in pixels (calibrate for best results)
        private const val FOCAL_LENGTH_PIXELS = 800f

        // Known real-world object heights in meters
        val KNOWN_HEIGHTS = mapOf(
            "person" to 1.7f,
            "bottle" to 0.25f,
            "chair" to 1.0f
            // Add more if needed
        )
    }

    private var imageRotationDegrees: Int = 0

    private val tfImageProcessor by lazy {
        ImageProcessor.Builder()
            .add(ResizeOp(IMG_SIZE_X, IMG_SIZE_Y, ResizeOp.ResizeMethod.BILINEAR))
            .add(Rot90Op(-imageRotationDegrees / 90))
            .add(NormalizeOp(NORMALIZE_MEAN, NORMALIZE_STD))
            .build()
    }

    private val tfImageBuffer = TensorImage(DataType.UINT8)

    private val outputBoundingBoxes = arrayOf(Array(MAX_DETECTION_NUM) { FloatArray(4) })
    private val outputLabels = arrayOf(FloatArray(MAX_DETECTION_NUM))
    private val outputScores = arrayOf(FloatArray(MAX_DETECTION_NUM))
    private val outputDetectionNum = FloatArray(1)

    private val outputMap = mapOf(
        0 to outputBoundingBoxes,
        1 to outputLabels,
        2 to outputScores,
        3 to outputDetectionNum
    )

    @SuppressLint("UnsafeExperimentalUsageError", "UnsafeOptInUsageError")
    override fun analyze(image: ImageProxy) {
        if (image.image == null) return
        imageRotationDegrees = image.imageInfo.rotationDegrees
        val detectedObjectList = detect(image.image!!)
        listener(detectedObjectList)
        image.close()
    }

    private fun detect(targetImage: Image): List<DetectionObject> {
        val targetBitmap = Bitmap.createBitmap(targetImage.width, targetImage.height, Bitmap.Config.ARGB_8888)
        yuvToRgbConverter.yuvToRgb(targetImage, targetBitmap)

        tfImageBuffer.load(targetBitmap)
        val tensorImage = tfImageProcessor.process(tfImageBuffer)

        interpreter.runForMultipleInputsOutputs(arrayOf(tensorImage.buffer), outputMap)

        val detectedObjectList = arrayListOf<DetectionObject>()
        val centerX = resultViewSize.width / 2
        val centerY = resultViewSize.height / 2
        val horizontalBoundary = resultViewSize.width * 0.33f
        val verticalBoundary = resultViewSize.height * 0.33f

        loop@ for (i in 0 until outputDetectionNum[0].toInt()) {
            val score = outputScores[0][i]
            val label = labels[outputLabels[0][i].toInt()]
            val boundingBox = RectF(
                outputBoundingBoxes[0][i][1] * resultViewSize.width,
                outputBoundingBoxes[0][i][0] * resultViewSize.height,
                outputBoundingBoxes[0][i][3] * resultViewSize.width,
                outputBoundingBoxes[0][i][2] * resultViewSize.height
            )

            val objectCenterX = boundingBox.centerX()
            val objectCenterY = boundingBox.centerY()

            val horizontalPosition = when {
                objectCenterX < horizontalBoundary -> "Left"
                objectCenterX > resultViewSize.width - horizontalBoundary -> "Right"
                else -> "Center"
            }

            val verticalPosition = when {
                objectCenterY < verticalBoundary -> "Top"
                objectCenterY > resultViewSize.height - verticalBoundary -> "Bottom"
                else -> "Center"
            }

            // --- Distance estimation ---
            val objectPixelHeight = boundingBox.height()
            val realHeight = KNOWN_HEIGHTS[label.lowercase()]
            val distanceMeters = if (realHeight != null && objectPixelHeight > 0) {
                (realHeight * FOCAL_LENGTH_PIXELS) / objectPixelHeight
            } else null

            Log.d("ObjectDistance", "Label: $label, Distance: ${distanceMeters?.let { "%.2f".format(it) } ?: "N/A"} m")

            if (score >= SCORE_THRESHOLD) {
                detectedObjectList.add(
                    DetectionObject(
                        score = score,
                        label = label,
                        boundingBox = boundingBox,
                        horizontalPosition = horizontalPosition,
                        verticalPosition = verticalPosition,
                        distanceMeters = distanceMeters
                    )
                )
            } else {
                break@loop
            }
        }
        return detectedObjectList.take(4)
    }
}
