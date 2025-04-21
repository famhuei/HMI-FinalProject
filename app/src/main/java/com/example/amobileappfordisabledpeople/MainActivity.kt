package com.example.amobileappfordisabledpeople

import android.content.res.AssetFileDescriptor
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.amobileappfordisabledpeople.features.face_recognition.FaceNetModel
import com.example.amobileappfordisabledpeople.features.object_detection.YuvToRgbConverter
import com.example.amobileappfordisabledpeople.ui.theme.ObjectDetectionTheme
import com.example.amobileappfordisabledpeople.ui.views.FaceRecognitionScreen
import com.google.accompanist.systemuicontroller.rememberSystemUiController
import dagger.hilt.android.AndroidEntryPoint
import org.tensorflow.lite.Interpreter
import java.io.BufferedReader
import java.io.FileInputStream
import java.io.InputStream
import java.io.InputStreamReader
import java.nio.ByteBuffer
import java.nio.channels.FileChannel
import java.util.Locale
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

const val DragThreshold = 30f

@AndroidEntryPoint
class MainActivity : ComponentActivity(), TextToSpeech.OnInitListener {

    private lateinit var textToSpeech: TextToSpeech
    //------------------------  onCreate ----------------------------------
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize TextToSpeech
        textToSpeech = TextToSpeech(this, this)

        setContent {
            val systemUiController = rememberSystemUiController()

            if(isSystemInDarkTheme()){
                systemUiController.setNavigationBarColor(Color.Black, darkIcons = false)
                systemUiController.setStatusBarColor(Color.Black, darkIcons = false)
            }

            // Initialize ExecutorService for camera preview in another thread
            cameraExecutor = Executors.newSingleThreadExecutor()

            ObjectDetectionTheme {
                Surface(
                    modifier = Modifier.fillMaxSize()
                ) {
                    val applicationViewModel: ApplicationViewModel = hiltViewModel()

                    LaunchedEffect(Unit) {
                        applicationViewModel.faceNetModel
                    }
                    App(cameraExecutor = cameraExecutor,
                        yuvToRgbConverter = yuvToRgbConverter,
                        interpreter = interpreter,
                        labels = labels,
                        textToSpeech = textToSpeech
                    )
//                    FaceRecognitionScreen(cameraExecutor)
                }
            }
        }
    }
    //------------------------Fin  onCreate --------------------------------

    //Implements TextToSpeech.OnInitListener
    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            // Thiết lập ngôn ngữ (US English)
            val result = textToSpeech.setLanguage(Locale.US)
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                Toast.makeText(this, "Ngôn ngữ không hỗ trợ", Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(this, "Khởi tạo TTS thất bại", Toast.LENGTH_SHORT).show()
        }
    }

    //Destroy TextToSpeech & ExecutorService instance
    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
        if (::textToSpeech.isInitialized) {
            textToSpeech.stop()
            textToSpeech.shutdown()
        }
    }

    //----------------------------------------------------------------------//
    companion object {
        private const val MODEL_MOBILENETV1 = "mobilenetv1.tflite"
        private const val MODEL_EFFICIENTDETV0 = "efficientdet-lite0.tflite"
        private const val MODEL_EFFICIENTDETV1 = "efficientdet-lite1.tflite"
        private const val MODEL_EFFICIENTDETV2 = "efficientdet-lite2.tflite"

        //private const val MODEL_FILE_NAME = "ssd_mobilenet_v1_1_metadata_1.tflite"

        // Tên tệp nhãn
        //private const val LABEL_FILE_NAME = "coco_labels.txt"  // Tệp nhãn chung cho cả MobileNet và EfficientDet

        private const val MODEL_FILE_NAME = MODEL_MOBILENETV1
        private const val LABEL_FILE_NAME = "coco_dataset_labels_v1.txt"
    }


    private lateinit var cameraExecutor: ExecutorService

    private val faceNetModel: FaceNetModel by lazy {
        FaceNetModel(this)
    }

    //Call load model function
    private val interpreter: Interpreter by lazy {
        Interpreter(loadModel())
    }

    //Call load labels function
    private val labels: List<String> by lazy {
        loadLabels()
    }

    //Call converter function
    private val yuvToRgbConverter: YuvToRgbConverter by lazy {
        YuvToRgbConverter(this)
    }

    //Load object detection model
    private fun loadModel(fileName: String = MODEL_FILE_NAME): ByteBuffer {
        lateinit var modelBuffer: ByteBuffer
        var file: AssetFileDescriptor? = null
        try {
            file = assets.openFd(fileName)
            val inputStream = FileInputStream(file.fileDescriptor)
            val fileChannel = inputStream.channel
            modelBuffer = fileChannel.map(FileChannel.MapMode.READ_ONLY, file.startOffset, file.declaredLength)
        } catch (e: Exception) {
            Toast.makeText(this, "Error loading model", Toast.LENGTH_SHORT).show()
            finish()
        } finally {
            file?.close()
        }
        return modelBuffer
    }

    // Load labels for object detection model
    private fun loadLabels(fileName: String = LABEL_FILE_NAME): List<String> {
        var labels = listOf<String>()
        var inputStream: InputStream? = null
        try {
            inputStream = assets.open(fileName)
            val reader = BufferedReader(InputStreamReader(inputStream))
            labels = reader.readLines()
        } catch (e: Exception) {
            Toast.makeText(this, "Error loading model", Toast.LENGTH_SHORT).show()
            finish()
        } finally {
            inputStream?.close()
        }
        return labels
    }

    // speak text via TextToSpeech
    fun speakText(text: String) {
        textToSpeech.speak(text, TextToSpeech.QUEUE_FLUSH, null, null)
    }
}






