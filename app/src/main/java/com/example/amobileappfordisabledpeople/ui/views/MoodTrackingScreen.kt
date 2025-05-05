package com.example.amobileappfordisabledpeople.ui.views

import android.annotation.SuppressLint
import android.graphics.PointF
import android.media.MediaPlayer
import android.speech.tts.TextToSpeech
import android.util.Log
import androidx.camera.core.ImageAnalysis
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toComposeRect
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.amobileappfordisabledpeople.DragThreshold
import com.example.amobileappfordisabledpeople.R
import com.example.amobileappfordisabledpeople.SocializingModeBar
import com.example.amobileappfordisabledpeople.features.face_detection.FaceDetectionAnalyzer
import com.example.amobileappfordisabledpeople.presentation.MainViewModel
import com.example.amobileappfordisabledpeople.presentation.SpeechRecognizerViewModel
import com.example.amobileappfordisabledpeople.utils.adjustPoint
import com.example.amobileappfordisabledpeople.utils.adjustSize
import com.example.amobileappfordisabledpeople.utils.drawBounds
import com.google.mlkit.vision.face.Face
import java.util.concurrent.ExecutorService
import kotlin.math.abs
import kotlin.math.absoluteValue

@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@Composable
fun MoodTrackingScreen(
    cameraExecutor: ExecutorService,
    moodTrackingViewModel: MainViewModel = hiltViewModel(),
    navigateToFaceRecognition: () -> Unit = {},
    navigateToExploreMode: () -> Unit = {},
    textToSpeech: TextToSpeech
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var previewView: PreviewView

    val screenWidth = remember { mutableStateOf(context.resources.displayMetrics.widthPixels) }
    val screenHeight = remember { mutableStateOf(context.resources.displayMetrics.heightPixels) }


    val imageWidth = remember { mutableStateOf(0) }
    val imageHeight = remember { mutableStateOf(0) }

    val moodTrackSound = remember { MediaPlayer.create(context, R.raw.mood_tracking) }
    val happySound = remember { MediaPlayer.create(context, R.raw.happy_sound) }
    val upsetSound = remember { MediaPlayer.create(context, R.raw.sad_sound) }

    LaunchedEffect(Unit) {
        moodTrackSound.start()
    }

    DisposableEffect(Unit) {
        onDispose {
            moodTrackSound.stop()
            happySound.stop()
            upsetSound.stop()

            moodTrackSound.release()
            happySound.release()
            upsetSound.release()
        }
    }

    val faces = remember { mutableStateListOf<Face>() }

    val mood = remember { mutableStateOf<MoodState>(MoodState.Normal) }

    LaunchedEffect(faces) {
        mood.value = MoodState.Normal
    }

    val faceDetectionAnalyzer = FaceDetectionAnalyzer { detectedFace, width, height ->
        faces.clear()
        faces.addAll(detectedFace)
        imageWidth.value = width
        imageHeight.value = height
    }

    val imageAnalysis = ImageAnalysis.Builder()
        .setTargetRotation(android.view.Surface.ROTATION_0)
        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
        .build()
        .also {
            it.setAnalyzer(cameraExecutor, faceDetectionAnalyzer)
        }

    moodTrackingViewModel.initRepo(imageAnalysis)
    var lastSpokenEmotion by remember { mutableStateOf<MoodState?>(null) }
    Scaffold(
        modifier = Modifier.pointerInput(Unit) {
            detectDragGestures(
                onDrag = { change, dragAmount ->
                    if (abs(dragAmount.x) > abs(dragAmount.y)) {
                        if (abs(dragAmount.x) > DragThreshold) {
                            navigateToFaceRecognition()


                        }
                    } else {
                        if (abs(dragAmount.y) > DragThreshold) {
                                navigateToExploreMode()
                        }
                    }
                }
            )
        },
        topBar = {
            SocializingModeBar(destinationName = "Mood Tracking")
        }
    ) {
        Box(
            modifier = Modifier.fillMaxSize()
        ) {
            AndroidView(
                factory = {
                    previewView = PreviewView(it)
                    moodTrackingViewModel.showCameraPreview(previewView = previewView, lifecycleOwner = lifecycleOwner)
                    previewView
                },
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onTap = {
                                Log.d("TAP", "Current mood: ${mood.value}")
//                                when (mood.value) {
//                                    is MoodState.Happy -> happySound.start()
//                                    is MoodState.Sad, is MoodState.Upset -> upsetSound.start()
//                                    else -> {}
//                                }


                                    val message = when (mood.value) {
                                        MoodState.Happy -> "The person looks happy."
                                        MoodState.Sad -> "The person looks sad."
                                        MoodState.Upset -> "The person seems upset."
                                        MoodState.Normal -> "The person looks calm."
                                        else -> "Can't detected"
                                    }

                                    message?.let {
                                        textToSpeech.speak(it, TextToSpeech.QUEUE_FLUSH, null, null)
                                    }

                            }
                        )
                    }
            )
//            DrawFaces(faces, imageHeight.value, imageWidth.value, screenWidth.value, screenHeight.value, updateEmotionState = { smile, upset ->
//                if (smile > 0.8) {
//                    mood.value = MoodState.Happy
//                } else {
//                    mood.value = MoodState.Normal
//                }
//            })

//            textToSpeech.speak("Testing speech output", TextToSpeech.QUEUE_FLUSH, null, null)

            DrawFaces(
                faces,
                imageHeight.value,
                imageWidth.value,
                screenWidth.value,
                screenHeight.value ,
                updateEmotionState = { smile, upset ->

                    Log.d("EMOTION_DEBUG", "Smile: ${smile * 100}%, Upset: ${upset * 100}%")

                    val newEmotion = when {
                        upset > 0.8 && upset > smile -> MoodState.Upset
                        upset > 0.4 && smile < 0.8 -> MoodState.Sad
                        smile > 0.8 -> MoodState.Happy
                        else -> MoodState.Normal
                    }

                    // Log emotion mới được xác định
                    Log.d("EMOTION_DEBUG", "New Emotion: $newEmotion")
                    mood.value = newEmotion
            })
        }
    }

}

@Composable
fun DrawFaces(faces: List<Face>, imageWidth: Int, imageHeight: Int, screenWidth: Int, screenHeight: Int, updateEmotionState: (Float, Float) -> Unit) {
    Canvas(modifier = Modifier.fillMaxSize()) {
        faces.forEach { face ->
            val boundingBox = face.boundingBox.toComposeRect()
            val topLeft = adjustPoint(PointF(boundingBox.left, boundingBox.top), imageWidth, imageHeight, screenWidth, screenHeight)
            val size = adjustSize(
                boundingBox.size,
                imageWidth, imageHeight, screenWidth, screenHeight
            )
            drawBounds(topLeft, size, Color.Yellow, 5f)

            // Calculate the level of certainty
            val certainty = "Certainty: ${calculateCertainty(face.headEulerAngleX, face.headEulerAngleY, face.headEulerAngleZ)}%"


            val smileLevel = "Smile: ${"%.2f".format((face.smilingProbability ?: 0f) * 100)}%"
            val upsetLevel = "Upset: ${calculateUpsetLevel(face)}%"

            updateEmotionState(face.smilingProbability ?: 0f, calculateUpsetLevel(face).toFloat()/100)

            drawContext.canvas.nativeCanvas.drawText(
                "$smileLevel" + "$upsetLevel",
                topLeft.x,
                topLeft.y - 10, // Position the text above the bounding box
                android.graphics.Paint().apply {
                    color = android.graphics.Color.YELLOW
                    textSize = 40f
                }
            )
        }
    }
}

fun calculateCertainty(angleX: Float, angleY: Float, angleZ: Float): Int {
    // Example calculation: combine the angles to form a certainty score
    val maxAngle = 30.0f // Define a maximum angle for normalization
    val normalizedX = (maxAngle - Math.abs(angleX)) / maxAngle
    val normalizedY = (maxAngle - Math.abs(angleY)) / maxAngle
    val normalizedZ = (maxAngle - Math.abs(angleZ)) / maxAngle
    return ((normalizedX + normalizedY + normalizedZ) / 3 * 100).toInt()
}

//fun calculateUpsetLevel(face: Face): Int {
//    // Define weights for each attribute
//    val smileWeight = 0.5f
//    val eyeOpenWeight = 0.3f
//    val headPoseWeight = 0.2f
//
//    // Calculate the smile score (higher probability means less upset)
//    val smileScore = 1 - (face.smilingProbability ?: 0f)
//
//    // Calculate the eye openness score (lower probability means more upset)
//    val leftEyeOpenScore = 1 - (face.leftEyeOpenProbability ?: 0f)
//    val rightEyeOpenScore = 1 - (face.rightEyeOpenProbability ?: 0f)
//    val eyeOpenScore = (leftEyeOpenScore + rightEyeOpenScore) / 2
//
//    // Calculate the head pose score (larger angles mean more upset)
//    val maxAngle = 30.0f
//    val normalizedX = Math.abs(face.headEulerAngleX) / maxAngle
//    val normalizedY = Math.abs(face.headEulerAngleY) / maxAngle
//    val normalizedZ = Math.abs(face.headEulerAngleZ) / maxAngle
//    val headPoseScore = (normalizedX + normalizedY + normalizedZ) / 3
//
//    // Combine the scores using the defined weights
//    val upsetLevel = (smileScore * smileWeight + eyeOpenScore * eyeOpenWeight + headPoseScore * headPoseWeight) * 100
//
//    return upsetLevel.toInt()
//}
fun calculateUpsetLevel(face: Face): Int {
    // Define configurable weights and thresholds
    val weights = UpsetWeights(
        smile = 0.5f,
        eyeOpen = 0.3f,
        headPose = 0.2f
    )
    val thresholds = UpsetThresholds(
        maxHeadAngle = 30f,
        maxUpsetScore = 100f
    )

    // Calculate component scores with null safety and boundary checks
    val smileScore = 1 - (face.smilingProbability?.coerceIn(0f, 1f) ?: 0f)

    val leftEyeScore = 1 - (face.leftEyeOpenProbability?.coerceIn(0f, 1f) ?: 1f)  // Assume eyes are open if null
    val rightEyeScore = 1 - (face.rightEyeOpenProbability?.coerceIn(0f, 1f) ?: 1f)
    val eyeScore = (leftEyeScore + rightEyeScore) / 2

    val headPoseScore = calculateHeadPoseScore(
        face.headEulerAngleX ?: 0f,
        face.headEulerAngleY ?: 0f,
        face.headEulerAngleZ ?: 0f,
        thresholds.maxHeadAngle
    )

    // Calculate weighted upset score
    val rawScore = (smileScore * weights.smile) +
            (eyeScore * weights.eyeOpen) +
            (headPoseScore * weights.headPose)

    // Normalize and return (0-100 scale)
    return (rawScore.coerceIn(0f, 1f) * thresholds.maxUpsetScore).toInt()
}

private fun calculateHeadPoseScore(x: Float, y: Float, z: Float, maxAngle: Float): Float {
    if (maxAngle <= 0) return 0f

    val normalizedX = (x.absoluteValue / maxAngle).coerceIn(0f, 1f)
    val normalizedY = (y.absoluteValue / maxAngle).coerceIn(0f, 1f)
    val normalizedZ = (z.absoluteValue / maxAngle).coerceIn(0f, 1f)

    return (normalizedX + normalizedY + normalizedZ) / 3f
}

// Configuration classes
data class UpsetWeights(
    val smile: Float,
    val eyeOpen: Float,
    val headPose: Float
) {
    init {
        require(smile + eyeOpen + headPose == 1f) { "Weights must sum to 1" }
    }
}

data class UpsetThresholds(
    val maxHeadAngle: Float,
    val maxUpsetScore: Float
)
sealed class MoodState {
    object Normal : MoodState() {
        override fun toString() = "calm"
    }
    object Happy : MoodState() {
        override fun toString() = "happy"
    }
    object Sad : MoodState() {
        override fun toString() = "sad"
    }
    object Upset : MoodState() {
        override fun toString() = "upset"
    }
}


