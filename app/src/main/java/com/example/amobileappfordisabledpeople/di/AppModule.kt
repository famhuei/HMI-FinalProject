package com.example.amobileappfordisabledpeople.di

import android.app.Application
import android.content.Context
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCapture.FLASH_MODE_ON
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import com.example.amobileappfordisabledpeople.Data.repository.CustomCameraRepoImpl
import com.example.amobileappfordisabledpeople.domain.repository.CustomCameraRepo
import com.example.amobileappfordisabledpeople.features.face_recognition.FaceNetModel
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import javax.inject.Singleton

//DI: stands for dependency injection, which is responsible for providing the dependencies to other app's components.

//Mark this object as a dagger module where provides  for Dagger Hilt
@Module
//Determine lifecycle scope of this module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    //Mark this method is sole, only one instance of this object will be created
    @Singleton
    fun provideCameraSelector(): CameraSelector {
        return CameraSelector.Builder()
            //Can choose between front or back camera
            .requireLensFacing(CameraSelector.LENS_FACING_BACK)
            .build()
    }

    @Provides
    @Singleton
    fun provideCameraProvider(application: Application): ProcessCameraProvider{
        //for providing camera instance
        return ProcessCameraProvider.getInstance(application).get()
    }

    @Provides
    @Singleton
    fun provideCameraPreview(): Preview {
        //for previewing the camera
        return Preview.Builder().build()
    }

    @Provides
    @Singleton
    fun provideImageCapture(): ImageCapture {
        //for capturing image from camera
        //ignoring the apect ration
        return ImageCapture.Builder()
            .setFlashMode(FLASH_MODE_ON)
            .build()
    }

    @Provides
    @Singleton
    fun provideImageAnalysis(): ImageAnalysis {
        //analyzing image before capturing
        //often using in ML to analyze image
        return ImageAnalysis.Builder()
            .setBackpressureStrategy(STRATEGY_KEEP_ONLY_LATEST)
            .build()
    }

    //    @Provides
//    @Singleton
//    fun provideCameraExecutor(): ExecutorService {
//        //for executing the camera
//        return Executors.newSingleThreadExecutor()
//    }

    @Provides
    @Singleton
    fun provideCustomCameraRepo(
        cameraProvider: ProcessCameraProvider,
        selector: CameraSelector,
        imageCapture: ImageCapture,
        imageAnalysis: ImageAnalysis,
        preview: Preview
    ): CustomCameraRepo {
        return CustomCameraRepoImpl(
            cameraProvider = cameraProvider,
            selector = selector,
            preview = preview,
            imageCapture = imageCapture,
            imageAnalysis = imageAnalysis
        )
    }

    @Provides
    @Singleton
    fun provideFaceNetModel(@ApplicationContext context: Context): FaceNetModel {
        return runBlocking {
            CoroutineScope(Dispatchers.IO).async {
                FaceNetModel(context)
            }.await()
        }
    }
}