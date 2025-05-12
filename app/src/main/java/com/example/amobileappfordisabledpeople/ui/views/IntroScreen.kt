package com.example.amobileappfordisabledpeople.ui.views

import android.speech.tts.TextToSpeech
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.amobileappfordisabledpeople.R
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember

import androidx.compose.runtime.setValue

import com.airbnb.lottie.compose.*

@Composable
fun IntroScreen(
    navigateToDetection: () -> Unit = {},
    textToSpeech: TextToSpeech,
    navigateToFaceRecognition: () -> Unit = {}
) {
    val context = LocalContext.current
    val composition by rememberLottieComposition(LottieCompositionSpec.Asset("intro_animation.json"))
    var isPlaying by remember { mutableStateOf(true) }
    val progress by animateLottieCompositionAsState(
        composition = composition,
        iterations = LottieConstants.IterateForever,
        isPlaying = isPlaying,
//        restartOnPlay = true // Cho phép chạy lại từ đầu
    )
    // Speak welcome message when screen loads
    LaunchedEffect(Unit) {
        val welcomeMessage = "Welcome to Accessibility Vision. " +
                "Touch the bottom left side for basic object detection. " +
                "Touch the bottom right side for face and emotion recognition."
        textToSpeech.speak(welcomeMessage, TextToSpeech.QUEUE_FLUSH, null, null)
    }
//    DisposableEffect(Unit) {
//        val message = "Welcome to Accessibility Vision. Touch the bottom left side for basic object detection. Touch the bottom right side for face and emotion recognition."
//        textToSpeech.speak(message, TextToSpeech.QUEUE_FLUSH, null, null)
//        onDispose { }
//    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF9BC0FD)),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // Header
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp)
                .background(Color(0xFF1A73E8)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Vision Mate",
                color = Color.White,
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .pointerInput(Unit) {
                    detectTapGestures(
//                        onTap = {
//                            textToSpeech.speak(
//                                "Double tap to hear the intro message again.",
//                                TextToSpeech.QUEUE_FLUSH,
//                                null,
//                                null
//                            )
//                        },
                        onTap = {
                            // TTS đọc lại lời chào
                            val welcomeMessage = "Welcome to Accessibility Vision. " +
                                    "Touch the bottom left side for basic object detection. " +
                                    "Touch the bottom right side for face and emotion recognition."
                            textToSpeech.speak(welcomeMessage, TextToSpeech.QUEUE_FLUSH, null, null)

                            // Restart animation
//                            isPlaying = false
                            isPlaying = true
                        }
                    )
                },
            contentAlignment = Alignment.Center
        ) {
            LottieAnimation(
                composition,
                progress,
                modifier = Modifier.size(250.dp)
            )
        }

        // Bottom touch areas
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(300.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            // Left - Basic Mode
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .background(Color(0xFF2196F3))
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onTap = {
                                // 1 tap -> chỉ đọc tên chức năng
                                textToSpeech.speak("Basic mode: object detection. Double taps to open", TextToSpeech.QUEUE_FLUSH, null, null)
                            },
                            onDoubleTap = {
                                // 2 tap -> chuyển sang màn hình tương ứng
                                textToSpeech.speak("Opening basic mode", TextToSpeech.QUEUE_FLUSH, null, null)
                                navigateToDetection()
                            }
                        )
                    },
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Image(
                        painter = painterResource(id = R.drawable.`object`),
                        contentDescription = "Basic Mode",
                        modifier = Modifier.size(80.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "BASIC MODE",
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                }
            }

            // Right - Advanced Mode
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .background(Color(0xFF673AB7))
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onTap = {
                                textToSpeech.speak("Advanced mode: face and emotion recognition. Double taps to open", TextToSpeech.QUEUE_FLUSH, null, null)
                            },
                            onDoubleTap = {
                                textToSpeech.speak("Opening advanced mode", TextToSpeech.QUEUE_FLUSH, null, null)
                                navigateToFaceRecognition()
                            }
                        )
                    },
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Image(
                        painter = painterResource(id = R.drawable.face),
                        contentDescription = "Advanced Mode",
                        modifier = Modifier.size(80.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "ADVANCED MODE",
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}
