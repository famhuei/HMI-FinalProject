package com.example.amobileappfordisabledpeople.Data.repository

import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import com.example.amobileappfordisabledpeople.domain.repository.CustomCameraRepo
import java.io.File
import java.text.SimpleDateFormat
import java.util.Locale
import javax.inject.Inject

class CustomCameraRepoImpl @Inject constructor(
    private val cameraProvider: ProcessCameraProvider,
    private val selector: CameraSelector,
    private val preview: Preview,
    private val imageAnalysis: ImageAnalysis,
    private val imageCapture: ImageCapture
):CustomCameraRepo {


    override suspend fun captureAndSaveImage(context: Context, onImageCaptured: (Uri) -> Unit) {

        //for file name
        val name = SimpleDateFormat(
            "yyyy-MM-dd-HH-mm-ss-SSS",
            Locale.ENGLISH
        ).format(System.currentTimeMillis())

        //Store captured image in cache
        val file = File(context.cacheDir, name)

        val outputOptions = ImageCapture.OutputFileOptions
            .Builder(file)
            .build()

        imageCapture.takePicture(
            outputOptions,
            ContextCompat.getMainExecutor(context),
            object : ImageCapture.OnImageSavedCallback{
                override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                    Toast.makeText(
                        context,
                        "Saved image ${file.absolutePath}",
                        Toast.LENGTH_LONG
                    ).show()
                    onImageCaptured(Uri.fromFile(file))
                }

                override fun onError(exception: ImageCaptureException) {
                    Toast.makeText(
                        context,
                        "some error occurred ${exception.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        )

    }

    override suspend fun showCameraPreview(
        previewView: PreviewView,
        lifecycleOwner: LifecycleOwner
    ) {

        preview.setSurfaceProvider(previewView.surfaceProvider)
        try {
            cameraProvider.unbindAll()
            cameraProvider.bindToLifecycle(
                lifecycleOwner,
                selector,
                preview,
                imageAnalysis,
                imageCapture
            )
        }catch (e:Exception){
            e.printStackTrace()
        }
    }
}