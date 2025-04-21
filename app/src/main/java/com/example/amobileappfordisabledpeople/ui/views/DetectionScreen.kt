package com.example.amobileappfordisabledpeople.ui.views

import android.graphics.Paint
import android.media.MediaPlayer
import android.speech.tts.TextToSpeech
import android.util.Log
import android.util.Size
import androidx.camera.core.ImageAnalysis
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.LifecycleOwner
import com.example.amobileappfordisabledpeople.AppBar
import com.example.amobileappfordisabledpeople.DragThreshold
import com.example.amobileappfordisabledpeople.R
import com.example.amobileappfordisabledpeople.features.object_detection.ObjectDetector
import com.example.amobileappfordisabledpeople.features.object_detection.YuvToRgbConverter
import com.example.amobileappfordisabledpeople.presentation.MainViewModel
import com.example.amobileappfordisabledpeople.ui.navigation.DetectionDestination
import com.google.accompanist.permissions.*
import kotlinx.coroutines.delay
import org.tensorflow.lite.Interpreter
import java.util.concurrent.ExecutorService
import kotlin.math.abs


@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun DetectionScreen(
    cameraExecutor: ExecutorService,
    yuvToRgbConverter: YuvToRgbConverter,
    interpreter: Interpreter,
    labels: List<String>,
    textToSpeech: TextToSpeech,
    navigateToDangerWarning: () -> Unit = {},
    navigateToExplore: () -> Unit = {},
    navigateToSocializingMode: () -> Unit = {},
) {

    val context = LocalContext.current
    val detectionSound = remember { MediaPlayer.create(context, R.raw.object_detection) }

    LaunchedEffect(Unit) {
        detectionSound.start()
        delay(detectionSound.duration.toLong())
    }

    DisposableEffect(Unit) {
        onDispose {
            detectionSound.stop()
            detectionSound.release()
            textToSpeech.stop()
        }
    }

    val cameraPermissionState = rememberPermissionState(android.Manifest.permission.CAMERA)

    Scaffold(
        modifier = Modifier.pointerInput(Unit) {
            detectDragGestures(
                onDrag = { change, dragAmount ->
                    if (abs(dragAmount.x) > abs(dragAmount.y)) {
                        if (abs(dragAmount.x) > DragThreshold) {
                            if (dragAmount.x > 0) {
                                navigateToExplore()
                            } else {
                                navigateToDangerWarning()
                            }
                        }
                    } else {
                        if (abs(dragAmount.y) > DragThreshold) {
                            navigateToSocializingMode()
                        }
                    }
                }
            )
        },
        topBar = {
            AppBar(destinationName = stringResource(DetectionDestination.titleRes))
        }
    ) { innerPadding ->
        if (cameraPermissionState.status.isGranted) {
            OpenCamera(
                cameraExecutor = cameraExecutor,
                yuvToRgbConverter = yuvToRgbConverter,
                interpreter = interpreter,
                labels = labels,
                textToSpeech = textToSpeech,
                contentPadding = innerPadding
            )
        } else {
            CameraPermission(cameraPermissionState)
        }
    }
}


//----------------------------- CAMERA --------------------------------------

@Composable
fun OpenCamera(
    cameraExecutor: ExecutorService,
    yuvToRgbConverter: YuvToRgbConverter,
    interpreter: Interpreter,
    labels: List<String>,
    textToSpeech: TextToSpeech, // Nhận TextToSpeech,
    contentPadding: PaddingValues = PaddingValues(0.dp)
) {
    val lifecycleOwner = LocalLifecycleOwner.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(contentPadding),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        CameraPreview(
            lifecycleOwner = lifecycleOwner,
            cameraExecutor = cameraExecutor,
            yuvToRgbConverter = yuvToRgbConverter,
            interpreter = interpreter,
            labels = labels,
            textToSpeech = textToSpeech // Truyền TextToSpeech vào CameraPreview
        )
    }
}

@Composable
fun CameraPreview(
    lifecycleOwner: LifecycleOwner,
    cameraExecutor: ExecutorService,
    yuvToRgbConverter: YuvToRgbConverter,
    interpreter: Interpreter,
    labels: List<String>,
    viewModel: DetectionViewModel = hiltViewModel(),
    mainViewModel: MainViewModel = hiltViewModel(),
    textToSpeech: TextToSpeech
) {
    val drawCanvas by remember { viewModel.isLoading }
    val detectionListObject by remember { viewModel.detectionList }

    val paint = Paint()
    val pathColorList = listOf(Color.Red, Color.Green, Color.Cyan, Color.Blue)
    val pathColorListInt = listOf(
        android.graphics.Color.RED,
        android.graphics.Color.GREEN,
        android.graphics.Color.CYAN,
        android.graphics.Color.BLUE
    )

    BoxWithConstraints(
        modifier = Modifier.fillMaxSize()
    ){
        val boxConstraint = this
        val sizeWith = with(LocalDensity.current) { boxConstraint.maxWidth.toPx() }
        val sizeHeight = with(LocalDensity.current) { boxConstraint.maxHeight.toPx() }

        var previousDetectedObjects: List<String> = emptyList()

        // Image analysis -> Detect objects in real-time
        val imageAnalyzer = ImageAnalysis.Builder()
            .setTargetRotation(android.view.Surface.ROTATION_0)
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .build()
            .also {
                it.setAnalyzer(
                    cameraExecutor,
                    ObjectDetector(
                        yuvToRgbConverter = yuvToRgbConverter,
                        interpreter = interpreter,
                        labels = labels,
                        resultViewSize = Size(sizeWith.toInt(), sizeHeight.toInt()
                        )
                    ) { detectedObjectList ->
                        // So sánh danh sách đối tượng hiện tại với danh sách trước đó
                        var check: Boolean = detectedObjectList.map { it.label } == previousDetectedObjects
                        Log.d ("Check", "Check: ${check}")
                        if (detectedObjectList.isNotEmpty() && !check) {

                            // Cập nhật danh sách đối tượng đã phát hiện
                            Log.d("ObjectDetection", "Previous Detected Objects: ${previousDetectedObjects}")

                            previousDetectedObjects = detectedObjectList.map { it.label }
                            Log.d("ObjectDetection", "Detected Objects: ${previousDetectedObjects}")

                            // Đọc nhãn của đối tượng đầu tiên (hoặc tất cả các đối tượng nếu muốn)
                            detectedObjectList.firstOrNull()?.let { detectedObject ->
                                textToSpeech.speak(
                                    detectedObject.label,
                                    TextToSpeech.QUEUE_FLUSH,
                                    null,
                                    null
                                )
                            }
                        }
                        viewModel.setList(detectedObjectList)
                    }
                )
            }

        mainViewModel.initRepo(imageAnalyzer)

        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { ctx ->
                val previewView = PreviewView(ctx)
                mainViewModel.showCameraPreview(previewView, lifecycleOwner)
                previewView
            }
        )

        if (drawCanvas){
            Canvas(
                modifier = Modifier.fillMaxSize(),
                onDraw = {

                    detectionListObject.mapIndexed { i, detectionObject ->
                        Log.d("Object", detectionObject.label + " --- " + detectionObject.score + " --- " + detectionObject.horizontalPosition + " --- " + detectionObject.verticalPosition)

                        Log.e("PositionCalculation", "Horizontal: $detectionObject.horizontalPosition, Vertical: $detectionObject.verticalPosition")

                        paint.apply {
                            color = pathColorListInt[i]
                            style = Paint.Style.FILL
                            isAntiAlias = true
                            textSize = 50f
                        }

                        drawRect(
                            color = pathColorList[i],
                            topLeft =  Offset(
                                x = detectionObject.boundingBox.left,
                                y = detectionObject.boundingBox.top
                            ),
                            size = androidx.compose.ui.geometry.Size(
                                width = detectionObject.boundingBox.width(),
                                height = detectionObject.boundingBox.height()
                            ),
                            style = Stroke(width = 3.dp.toPx())
                        )

                        drawIntoCanvas {
                            it.nativeCanvas.drawText(
                                "${detectionObject.label} ${"%,.2f".format(detectionObject.score * 100)}% " +
                                        "(${detectionObject.horizontalPosition}, ${detectionObject.verticalPosition})",
                                detectionObject.boundingBox.left,            // x-coordinate (top left)
                                detectionObject.boundingBox.top - 5f,        // y-coordinate (top left)
                                paint
                            )
                        }
                    }
                }
            )
        }
    }
}


//----------------------------- PERMISSION --------------------------------------
@OptIn(ExperimentalPermissionsApi::class)
@Composable
private fun Permission(
    cameraPermissionState: PermissionState
) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        if (!cameraPermissionState.status.isGranted) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally) {

                val textToShow = if (cameraPermissionState.status.shouldShowRationale) {
                    "The camera is important for this app.\n Please grant the permission."
                } else {
                    "Camera not available"
                }
                Text(textToShow, textAlign = TextAlign.Center, color = MaterialTheme.colors.onSurface)

                Spacer(modifier = Modifier.height(8.dp))

                Button(
                    shape = CircleShape,
                    onClick = { cameraPermissionState.launchPermissionRequest() }) {
                    Text("Request permission")
                    Icon(
                        painterResource(id = R.drawable.ic_baseline_camera_24),
                        contentDescription = "Icon camera", modifier = Modifier.padding(start = 8.dp))
                }
            }
        }
    }
}
