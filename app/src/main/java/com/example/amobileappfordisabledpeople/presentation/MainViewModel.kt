package com.example.amobileappfordisabledpeople.presentation

import android.content.Context
import android.net.Uri
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.amobileappfordisabledpeople.domain.repository.CustomCameraRepo
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageCapture
import com.example.amobileappfordisabledpeople.Data.repository.CustomCameraRepoImpl

@HiltViewModel
open class MainViewModel @Inject constructor(
    private var repo: CustomCameraRepo,
    private val cameraProvider: ProcessCameraProvider,
    private val selector: CameraSelector,
    private val preview: Preview,
    private val imageCapture: ImageCapture,
    private val imageAnalysis: ImageAnalysis
): ViewModel() {

    fun initRepo(customImageAnalysis: ImageAnalysis) {
        repo = CustomCameraRepoImpl(
            cameraProvider = cameraProvider,
            selector = selector,
            preview = preview,
            imageCapture = imageCapture,
            imageAnalysis = customImageAnalysis
        )
    }

    var capturedImageUri = mutableStateOf<Uri?>(null)
        private set

    fun showCameraPreview(
        previewView: PreviewView,
        lifecycleOwner: LifecycleOwner,
    ) {
        viewModelScope.launch {
            repo.showCameraPreview(
                previewView = previewView,
                lifecycleOwner = lifecycleOwner
            )
        }
    }

    fun captureAndSave(context: Context, onImageCaptured: () -> Unit = {}) {
        viewModelScope.launch {
            repo.captureAndSaveImage(context) { uri ->
                updateCapturedImageUri(uri)
                onImageCaptured()
            }

        }
    }

    fun updateCapturedImageUri(uri: Uri) {
        capturedImageUri.value = uri
    }
}