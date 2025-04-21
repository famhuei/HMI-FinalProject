package com.example.amobileappfordisabledpeople.Data

import android.graphics.RectF

data class DetectionObject (
    val score: Float,
    val label: String,
    val boundingBox: RectF,
    val horizontalPosition: String,
    val verticalPosition: String
)