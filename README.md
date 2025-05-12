
# VisionMate

**VisionMate** is an Android application designed to assist visually impaired users in interacting with their surroundings using image recognition and facial emotion analysis technologies.

## 🧠 Main Features

The app includes 3 functional screens:

---

### 1. 🟦 Intro Screen

- **Description**: Introduction screen providing a welcome message and basic instructions for visually impaired users.
- **Features**:
  - Plays a welcome message using Text-to-Speech:  
    `"Welcome to Accessibility Vision. Touch the bottom left side for basic object detection. Touch the bottom right side for face and emotion recognition."`
  - Animated logo to enhance user experience and provide visual feedback.
  - **Navigation**:
    - Single tap to read the button label.
    - Double tap to navigate to:
      - **Basic Detection Mode** (left side)
      - **Face + Emotion Recognition Mode** (right side)
        
![image](https://github.com/user-attachments/assets/7497c564-0c4e-4b85-9e45-2ac509fd49e6)

---

### 2. 🟩 Basic Detection Mode (Object Detection)

- **Description**: Detects surrounding objects using the camera.
- **Features**:
  - Uses an object detection model (e.g., YOLO) for real-time analysis.
  - Outputs:
    - **Label** of detected object
    - **Estimated distance** from the camera
  - Provides audio feedback using Text-to-Speech.
  - Swipe horizontally to return to Intro or move to another screen.

---

### 3. 🟥 Face + Emotion Recognition Mode

- **Description**: Detects faces and analyzes emotions.
- **Features**:
  - Outputs:
    - **Name** of the recognized person (if previously trained)
    - **Current emotion**: `Happy`, `Sad`, `Upset`, or `Normal`
  - Speaks out feedback such as:  
    `"I recognized John. They seem happy and cheerful."`
  - Plays emotion-specific audio cues.
  - Supports horizontal swipe to navigate between modes.

---

## 🛠️ Technologies Used

- **Jetpack Compose** for modern UI
- **CameraX** for camera preview and image processing
- **TensorFlow Lite / ML Kit** for object and emotion recognition
- **TextToSpeech API** for voice feedback
- **MediaPlayer** for guidance and emotion-specific sounds

---

## 🔄 Navigation Patterns

- **Single tap**: Read out UI element or button
- **Double tap**: Trigger action (e.g., navigate, repeat intro)
- **Swipe Up/Down**: Switch between Detection / Face & Emotion modes
- **Swipe Left/Right**: Return to Intro screen

---

## 📌 Notes

- Ensure permissions for **Camera**, **Audio**, and **Internet** are granted.
- Designed specifically for visually impaired users:  
  Voice-first experience, minimal text, non-reliant on visuals.

---

## 📱 Demo

*(Add screenshots or demo video here if available)*

---

## ✅ How to Clone and Run VisionMate

### 1. Clone project from GitHub

```bash
git clone https://github.com/nvnhat04/HMI-FinalProject.git
cd visionmate
```

> 📌 Replace with your actual GitHub repository link.

### 2. Open project in Android Studio

1. Launch Android Studio.
2. Select `Open an existing project`.
3. Navigate to the `visionmate` folder.
4. Let Gradle sync and download dependencies.

### 3. Configure the project

Ensure the following:

* **Android Gradle Plugin**: 8.0 or higher  
* **Jetpack Compose Compiler**: Compatible version  
* **Minimum SDK**: 24+

Required permissions:

* `CAMERA`  
* `RECORD_AUDIO` (for TTS)  
* `INTERNET` (if using online models)

### 4. Run the project

* Connect an Android device or start an emulator.
* Click the green `Run ▶` button in Android Studio.

Feel free to contribute or adapt the code for your use case!
