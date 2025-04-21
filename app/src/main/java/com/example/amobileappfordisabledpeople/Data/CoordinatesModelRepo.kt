package com.example.amobileappfordisabledpeople.Data

import retrofit2.Response

interface CoordinatesModelRepo {
    suspend fun getCoordinatesModel(requestModel: RequestModel): Response<CoordinatesModel>
}