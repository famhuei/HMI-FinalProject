package com.example.amobileappfordisabledpeople.presentation

import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class SpeechRecognizerViewModel: ViewModel() {

    private val _state = MutableStateFlow(SpeechRecognizerState())
    val state: StateFlow<SpeechRecognizerState> = _state

    fun changeTextValue(text: String?) {
        viewModelScope.launch {
            _state.update { currentState ->
                currentState.copy(text = text)
            }
        }
    }

    fun reset() {
        viewModelScope.launch {
            _state.update { currentState ->
                currentState.copy(text = null)
            }
        }
    }
}