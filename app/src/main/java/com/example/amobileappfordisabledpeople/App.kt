package com.example.amobileappfordisabledpeople

import android.speech.tts.TextToSpeech
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Tab
import androidx.compose.material.TabRow
import androidx.compose.material.TabRowDefaults
import androidx.compose.material.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material.Text
import androidx.compose.material.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import com.example.amobileappfordisabledpeople.features.face_recognition.FaceNetModel
import com.example.amobileappfordisabledpeople.features.object_detection.YuvToRgbConverter
import com.example.amobileappfordisabledpeople.ui.navigation.ApplicationNavHost
import org.tensorflow.lite.Interpreter
import java.util.concurrent.ExecutorService

@Composable
fun AppBar(
    destinationName: String,
    modifier: Modifier = Modifier,
) {
    var selectedTabIndex = 0
    if (destinationName == stringResource(R.string.detection)) {
        selectedTabIndex = 0
    } else if (destinationName == stringResource(R.string.explore)) {
        selectedTabIndex = 1
    }

    val tabs = listOf("OBJECT DETECTION", "EXPLORE")

    TabRow(
        selectedTabIndex = selectedTabIndex,
        backgroundColor = Color.White,
        contentColor = Color.Black,
        indicator = { tabPositions ->
            TabRowDefaults.Indicator(
                modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTabIndex]),
                color = Color.Black,
                height = 4.dp
            )
        },
        modifier = Modifier.fillMaxWidth()
    ) {
        tabs.forEachIndexed { index, title ->
            Tab(
                selected = selectedTabIndex == index,
                onClick = { /**/ },
                text = {
                    Text(
                        text = title,
                        color = if (selectedTabIndex == index) Color.Black else Color.Gray,
                        style = MaterialTheme.typography.subtitle2
                    )
                }
            )
        }
    }
}

@Composable
fun SocializingModeBar(
    destinationName: String,
    modifier: Modifier = Modifier,
) {
    var selectedTabIndex = 0
    if (destinationName == stringResource(R.string.mood_tracking)) {
        selectedTabIndex = 0
    } else if (destinationName == stringResource(R.string.face_recognition)) {
        selectedTabIndex = 1
    }

    val tabs = listOf("MOOD TRACKING", "Face Recognition")

    TabRow(
        selectedTabIndex = selectedTabIndex,
        backgroundColor = Color.White,
        contentColor = Color.Black,
        indicator = { tabPositions ->
            TabRowDefaults.Indicator(
                modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTabIndex]),
                color = Color.Black,
                height = 4.dp
            )
        },
        modifier = Modifier.fillMaxWidth()
    ) {
        tabs.forEachIndexed { index, title ->
            Tab(
                selected = selectedTabIndex == index,
                onClick = { /**/ },
                text = {
                    Text(
                        text = title,
                        color = if (selectedTabIndex == index) Color.Black else Color.Gray,
                        style = MaterialTheme.typography.subtitle2
                    )
                }
            )
        }
    }
}


@Composable
fun App(navHostController: NavHostController = rememberNavController(), cameraExecutor: ExecutorService, yuvToRgbConverter: YuvToRgbConverter, interpreter: Interpreter, labels: List<String>, textToSpeech: TextToSpeech) {
    ApplicationNavHost(
        navController = navHostController,
        cameraExecutor = cameraExecutor,
        yuvToRgbConverter = yuvToRgbConverter,
        interpreter = interpreter,
        labels = labels, textToSpeech =
            textToSpeech)
}