package com.example.amobileappfordisabledpeople.Data

data class CoordinatesModel(
    //Object detection data
    val result: List<Result>?,
    val error: String?,

    //Caption data
    val response: String?,

    //Segment data
    val labels: List<String>?,
    val polygons: List<List<List<Double>>>?
)