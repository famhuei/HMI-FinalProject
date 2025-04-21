package com.example.amobileappfordisabledpeople.ui.views

import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.amobileappfordisabledpeople.Data.CoordinatesModelRepo
import com.example.amobileappfordisabledpeople.Data.RequestModel
import com.example.amobileappfordisabledpeople.Data.Result
import com.example.amobileappfordisabledpeople.ui.views.ObjectDetectionUiData
import com.example.amobileappfordisabledpeople.ui.views.UiState
import kotlinx.coroutines.launch
import java.util.Locale

class CoordinatesModelViewModel(
    private val coordinatesModelRepo: CoordinatesModelRepo
) : ViewModel() {

    private val _uiState = MutableLiveData<UiState>()
    val uiState: LiveData<UiState> get() = _uiState

    var imageLeftDistance by mutableFloatStateOf(0.0f)

    init {
        resetState()
    }

    fun resetState() {
        _uiState.value = UiState.Idle
    }

    fun getCoordinatesModel(requestModel: RequestModel) {
        _uiState.value = UiState.Loading
        Log.d("ExploreScreen", "Coordinate - Loading")

        viewModelScope.launch {
            try {
                val coordinatesModel = coordinatesModelRepo
                    .getCoordinatesModel(requestModel)
                    .body()

                _uiState.value = when {

                    coordinatesModel?.response != null -> {
                        UiState.CaptionResponse(coordinatesModel.response)
                    }

                    coordinatesModel?.error != null -> {
                        UiState.Error(
                            coordinatesModel.error
                        )
                    }

                    else -> {
                        UiState.Error("No result found.")
                    }
                }

                Log.d("ExploreScreen", "Coordinate - ${_uiState.value.toString()}")

            } catch (e: Exception) {
                e.printStackTrace()
                _uiState.value = if (e.message != null) {
                    UiState.Error(e.message!!)
                } else {
                    UiState.Error("An unknown error occurred.")
                }
            }
        }
    }

    private fun getObjectDetectionUiData(results: List<Result>): List<ObjectDetectionUiData> {
        val map = HashMap<String, Color>()
        return results.map { result ->
            val (y1, x1, y2, x2) = result.coordinates
            map.putIfAbsent(result.label.filterLabel(), getRandomColor())

            ObjectDetectionUiData(
                topLeft = Offset(x1.toFloat() + imageLeftDistance, y1.toFloat()),
                color = map.getOrDefault(result.label.filterLabel(), Color.Transparent),
                size = Size(
                    width = (x2 - x1).toFloat(),
                    height = (y2 - y1).toFloat()
                ),
                text = result.label.filterLabel(),
                textTopLeft = Offset(x1.toFloat() + imageLeftDistance, y1.toFloat() - 40)
            )
        }
    }
}

class CoordinatesModelViewModelFactory(private val coordinatesModelRepo: CoordinatesModelRepo) :
    ViewModelProvider.NewInstanceFactory() {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return CoordinatesModelViewModel(coordinatesModelRepo) as T
    }
}

private fun String.filterLabel(): String {
    return this.replace("'", "").trim().replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.US) else it.toString() }
}

private fun getRandomColor(): Color {
    val r = (1..254).random()
    val g = (1..254).random()
    val b = (1..254).random()
    return Color(
        red = r,
        green = g,
        blue = b
    )
}