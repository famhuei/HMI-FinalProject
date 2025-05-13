package com.example.amobileappfordisabledpeople.ui.views

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.graphics.PointF
import android.media.MediaPlayer
import android.speech.tts.TextToSpeech
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
import com.example.amobileappfordisabledpeople.ApplicationViewModel
import com.example.amobileappfordisabledpeople.DragThreshold
import com.example.amobileappfordisabledpeople.R
import com.example.amobileappfordisabledpeople.SocializingModeBar
import com.example.amobileappfordisabledpeople.features.face_recognition.FaceNetModel
import com.example.amobileappfordisabledpeople.features.face_recognition.FaceRecognitionAnalyzer
import com.example.amobileappfordisabledpeople.presentation.MainViewModel
import com.example.amobileappfordisabledpeople.utils.adjustPoint
import com.example.amobileappfordisabledpeople.utils.adjustSize
import com.example.amobileappfordisabledpeople.utils.drawBounds
import com.google.mlkit.vision.face.Face
import java.util.Locale
import java.util.concurrent.ExecutorService
import kotlin.collections.forEach
import kotlin.math.abs
import kotlin.math.absoluteValue

@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@Composable
fun FaceRecognitionScreen(
    cameraExecutor: ExecutorService,
    viewModel: MainViewModel = hiltViewModel(),
//    navigateToMoodTracking: () -> Unit = {},
    navigateToExploreMode: () -> Unit = {} ,
    navigateToDetection: () -> Unit = {},
    navigateToIntro: () -> Unit = {},
    textToSpeech: TextToSpeech,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    lateinit var previewView: PreviewView

    val screenWidth = remember { mutableStateOf(context.resources.displayMetrics.widthPixels) }
    val screenHeight = remember { mutableStateOf(context.resources.displayMetrics.heightPixels) }

    val imageWidth = remember { mutableStateOf(0) }
    val imageHeight = remember { mutableStateOf(0) }

    val faces = remember { mutableStateListOf<Face>() }

    val recognizedPerson = remember { mutableStateOf("None") }
    val distance = remember { mutableStateOf(0f) }

//    var textToSpeech by remember { mutableStateOf<TextToSpeech?>(null) }
    val recognitionSound = remember { MediaPlayer.create(context, R.raw.face_recognition) }
    val moodTrackSound = remember { MediaPlayer.create(context, R.raw.mood_tracking) }
    val happySound = remember { MediaPlayer.create(context, R.raw.happy_sound) }
    val upsetSound = remember { MediaPlayer.create(context, R.raw.sad_sound) }

    LaunchedEffect(Unit) {
//        recognitionSound.start()
//        moodTrackSound.start()
//        textToSpeech = TextToSpeech(context) { status ->
//            if (status == TextToSpeech.SUCCESS) {
//                textToSpeech?.language = Locale.US
//            }
//        }
        textToSpeech.speak("Advanced mode: face and emotion recognition", TextToSpeech.QUEUE_FLUSH, null, null)
    }

    val applicationViewModel: ApplicationViewModel = hiltViewModel()
    val faceNetModel: FaceNetModel = applicationViewModel.faceNetModel

    DisposableEffect(Unit) {
        onDispose {
            recognitionSound.stop()
            recognitionSound.release()
            textToSpeech?.shutdown()
            //emotion
            moodTrackSound.stop()
            happySound.stop()
            upsetSound.stop()

            moodTrackSound.release()
            happySound.release()
            upsetSound.release()
        }
    }

    val faceRecognitionAnalyzer = FaceRecognitionAnalyzer(context, faceNetModel) { detectedFace, width, height, name, actualDistance  ->
        faces.clear()
        faces.addAll(detectedFace)
        imageWidth.value = width
        imageHeight.value = height
        recognizedPerson.value = name
        distance.value = actualDistance
    }


    val mood = remember { mutableStateOf<MoodState>(MoodState.Normal) }
    LaunchedEffect(faces) {
        mood.value = MoodState.Normal
    }

    val imageAnalysis = ImageAnalysis.Builder()
        .setTargetRotation(android.view.Surface.ROTATION_0)
        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
        .build()
        .also {
            it.setAnalyzer(cameraExecutor, faceRecognitionAnalyzer)
        }

    viewModel.initRepo(imageAnalysis)

    Scaffold(
        modifier = Modifier.pointerInput(Unit) {
            detectDragGestures(
                onDrag = { change, dragAmount ->
                    if (abs(dragAmount.x) > abs(dragAmount.y)) {
                        if (abs(dragAmount.x) > DragThreshold) {
//                            navigateToMoodTracking()
//                            navigateToIntro()
                            navigateToExploreMode()
                        } else {
                            navigateToExploreMode()
                        }
                    } else {
                        if (abs(dragAmount.y) > DragThreshold) {
                            navigateToDetection()
                        }
                    }
                }
            )
        },
        topBar = {
            SocializingModeBar(destinationName = "Face Recognition")
        }
    ) {
        Box(
            modifier = Modifier.fillMaxSize()
        ) {
            AndroidView(
                factory = {
                    previewView = PreviewView(it)
                    viewModel.showCameraPreview(previewView = previewView, lifecycleOwner = lifecycleOwner)
                    previewView
                },
                modifier = Modifier.fillMaxSize()
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onTap = {
//
                                val personName = recognizedPerson.value // or dynamically update this value
                                val moodMessage = when (mood.value) {
                                    MoodState.Happy -> "They seem happy and cheerful."
                                    MoodState.Sad -> "They seem a bit sad."
                                    MoodState.Upset -> "They look upset or troubled."
                                    MoodState.Normal -> "They appear calm and relaxed."
                                    else -> "I couldn't detect their mood."
                                }

                                val message = if (personName.isNotEmpty() && personName != "None") {
                                    "I recognized ${personName}. $moodMessage Tap again for more details."
                                } else {
                                    "I couldn't recognize the person. Tap again to try again."
                                }
                                // Speak the feedback for visually impaired users
                                textToSpeech?.speak(message, TextToSpeech.QUEUE_FLUSH, null, null)
                            }
                        )
                    }
            )
            DrawFaces(faces, imageHeight.value, imageWidth.value, screenWidth.value, screenHeight.value, recognizedPerson.value, distance.value)
        }
    }
}

@Composable
fun DrawFaces(faces: List<Face>, imageWidth: Int, imageHeight: Int, screenWidth: Int, screenHeight: Int, name: String, distance: Float) {
    Canvas(modifier = Modifier.fillMaxSize()) {
        faces.forEach { face ->
            val boundingBox = face.boundingBox.toComposeRect()
            val topLeft = adjustPoint(PointF(boundingBox.left, boundingBox.top), imageWidth, imageHeight, screenWidth, screenHeight)
            val size = adjustSize(
                boundingBox.size,
                imageWidth, imageHeight, screenWidth, screenHeight
            )
            drawBounds(topLeft, size, Color.Yellow, 5f)

            val recognition = "$name: ${"%.2f".format(distance)}"
            val certainty = "Certainty: ${calculateCertainty(face.headEulerAngleX, face.headEulerAngleY, face.headEulerAngleZ)}%"
            val smileLevel = "Smile: ${"%.2f".format((face.smilingProbability ?: 0f) * 100)}%"
            val upsetLevel = "Upset: ${calculateUpsetLevel(face)}%"
            drawContext.canvas.nativeCanvas.drawText(
                "$recognition"  + "$smileLevel" + "$upsetLevel",
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
//fun calculateCertainty(angleX: Float, angleY: Float, angleZ: Float): Int {
//    // Example calculation: combine the angles to form a certainty score
//    val maxAngle = 30.0f // Define a maximum angle for normalization
//    val normalizedX = (maxAngle - Math.abs(angleX)) / maxAngle
//    val normalizedY = (maxAngle - Math.abs(angleY)) / maxAngle
//    val normalizedZ = (maxAngle - Math.abs(angleZ)) / maxAngle
//    return ((normalizedX + normalizedY + normalizedZ) / 3 * 100).toInt()
//}
//
//
//fun calculateUpsetLevel(face: Face): Int {
//    // Define configurable weights and thresholds
//    val weights = UpsetWeights(
//        smile = 0.5f,
//        eyeOpen = 0.3f,
//        headPose = 0.2f
//    )
//    val thresholds = UpsetThresholds(
//        maxHeadAngle = 30f,
//        maxUpsetScore = 100f
//    )
//
//    // Calculate component scores with null safety and boundary checks
//    val smileScore = 1 - (face.smilingProbability?.coerceIn(0f, 1f) ?: 0f)
//
//    val leftEyeScore = 1 - (face.leftEyeOpenProbability?.coerceIn(0f, 1f) ?: 1f)  // Assume eyes are open if null
//    val rightEyeScore = 1 - (face.rightEyeOpenProbability?.coerceIn(0f, 1f) ?: 1f)
//    val eyeScore = (leftEyeScore + rightEyeScore) / 2
//
//    val headPoseScore = calculateHeadPoseScore(
//        face.headEulerAngleX ?: 0f,
//        face.headEulerAngleY ?: 0f,
//        face.headEulerAngleZ ?: 0f,
//        thresholds.maxHeadAngle
//    )
//
//    // Calculate weighted upset score
//    val rawScore = (smileScore * weights.smile) +
//            (eyeScore * weights.eyeOpen) +
//            (headPoseScore * weights.headPose)
//
//    // Normalize and return (0-100 scale)
//    return (rawScore.coerceIn(0f, 1f) * thresholds.maxUpsetScore).toInt()
//}
//
//private fun calculateHeadPoseScore(x: Float, y: Float, z: Float, maxAngle: Float): Float {
//    if (maxAngle <= 0) return 0f
//
//    val normalizedX = (x.absoluteValue / maxAngle).coerceIn(0f, 1f)
//    val normalizedY = (y.absoluteValue / maxAngle).coerceIn(0f, 1f)
//    val normalizedZ = (z.absoluteValue / maxAngle).coerceIn(0f, 1f)
//
//    return (normalizedX + normalizedY + normalizedZ) / 3f
//}
//
//// Configuration classes
//data class UpsetWeights(
//    val smile: Float,
//    val eyeOpen: Float,
//    val headPose: Float
//) {
//    init {
//        require(smile + eyeOpen + headPose == 1f) { "Weights must sum to 1" }
//    }
//}
//
//data class UpsetThresholds(
//    val maxHeadAngle: Float,
//    val maxUpsetScore: Float
//)
//sealed class MoodState {
//    object Normal : MoodState() {
//        override fun toString() = "calm"
//    }
//    object Happy : MoodState() {
//        override fun toString() = "happy"
//    }
//    object Sad : MoodState() {
//        override fun toString() = "sad"
//    }
//    object Upset : MoodState() {
//        override fun toString() = "upset"
//    }
//}