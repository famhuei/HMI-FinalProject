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
            "bicycle" to 1.2f,
            "car" to 1.5f,
            "motorcycle" to 1.1f,
            "airplane" to 5.0f,  // Chiều cao trung bình của máy bay nhỏ
            "bus" to 3.0f,
            "train" to 4.0f,
            "truck" to 3.0f,
            "boat" to 2.0f,  // Tùy thuộc vào loại thuyền
            "traffic light" to 0.3f,
            "fire hydrant" to 0.8f,
            "street sign" to 0.5f,
            "stop sign" to 0.6f,
            "parking meter" to 1.0f,
            "bench" to 0.9f,
            "bird" to 0.2f,
            "cat" to 0.25f,
            "dog" to 0.5f,  // Tùy thuộc vào giống chó
            "horse" to 1.8f,
            "sheep" to 1.0f,
            "cow" to 1.5f,
            "elephant" to 3.0f,
            "bear" to 1.5f,
            "zebra" to 1.5f,
            "giraffe" to 5.0f,
            "hat" to 0.2f,
            "backpack" to 0.5f,
            "umbrella" to 1.0f,
            "shoe" to 0.15f,
            "eye glasses" to 0.1f,
            "handbag" to 0.3f,
            "tie" to 0.5f,
            "suitcase" to 0.7f,
            "frisbee" to 0.03f,
            "skis" to 1.8f,
            "snowboard" to 1.5f,
            "sports ball" to 0.25f,
            "kite" to 0.5f,
            "baseball bat" to 0.9f,
            "baseball glove" to 0.3f,
            "skateboard" to 0.1f,
            "surfboard" to 1.8f,
            "tennis racket" to 0.7f,
            "bottle" to 0.25f,
            "plate" to 0.03f,
            "wine glass" to 0.15f,
            "cup" to 0.1f,
            "fork" to 0.2f,
            "knife" to 0.2f,
            "spoon" to 0.2f,
            "bowl" to 0.1f,
            "banana" to 0.2f,
            "apple" to 0.08f,
            "sandwich" to 0.05f,
            "orange" to 0.08f,
            "broccoli" to 0.2f,
            "carrot" to 0.2f,
            "hot dog" to 0.05f,
            "pizza" to 0.03f,
            "donut" to 0.04f,
            "cake" to 0.15f,
            "chair" to 1.0f,
            "couch" to 0.8f,
            "potted plant" to 0.5f,
            "bed" to 0.5f,  // Chiều cao giường
            "mirror" to 1.0f,  // Tùy thuộc vào loại gương
            "dining table" to 0.75f,
            "window" to 1.5f,  // Tùy thuộc vào cửa sổ
            "desk" to 0.75f,
            "toilet" to 0.4f,
            "door" to 2.0f,
            "tv" to 0.6f,  // Tùy thuộc vào kích thước TV
            "laptop" to 0.03f,
            "mouse" to 0.03f,
            "remote" to 0.15f,
            "keyboard" to 0.03f,
            "cell phone" to 0.15f,
            "microwave" to 0.3f,
            "oven" to 0.6f,
            "toaster" to 0.2f,
            "sink" to 0.2f,
            "refrigerator" to 1.8f,
            "blender" to 0.3f,
            "book" to 0.03f,
            "clock" to 0.3f,
            "vase" to 0.3f,
            "scissors" to 0.15f,
            "teddy bear" to 0.3f,
            "hair drier" to 0.2f,
            "toothbrush" to 0.2f,
            "hair brush" to 0.2f
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
