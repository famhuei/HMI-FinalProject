package com.example.amobileappfordisabledpeople.features.face_recognition

import android.content.Context
import android.util.Log
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.support.image.TensorImage
import java.io.FileInputStream
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel

class FaceNetModel(context: Context) {
    private val model: Interpreter

    init {
        Log.d("FaceNetModel", "Loading model")
        val modelFile = loadModelFile(context, "mobile_face_net.tflite")
        model = Interpreter(modelFile)
        Log.d("FaceNetModel", "Model loaded")
    }

    private fun loadModelFile(context: Context, modelName: String): MappedByteBuffer {
        val fileDescriptor = context.assets.openFd(modelName)
        val inputStream = FileInputStream(fileDescriptor.fileDescriptor)
        val fileChannel = inputStream.channel
        val startOffset = fileDescriptor.startOffset
        val declaredLength = fileDescriptor.declaredLength
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength)
    }

    fun getEmbedding(image: TensorImage): FloatArray {
        val embedding = Array(1) { FloatArray(192) }
        model.run(image.buffer, embedding)
        return embedding[0]
    }
}