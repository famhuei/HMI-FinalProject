package com.example.amobileappfordisabledpeople.Data

import android.net.Uri

data class RequestModel(
    val text: String,
    val width: String,
    val height: String,
    val uri: Uri,
)