package com.example.amobileappfordisabledpeople.ui.navigation

import com.example.amobileappfordisabledpeople.R

interface NavigationDestination {
    val route: String
    val titleRes: Int
}

object DetectionDestination : NavigationDestination {
    override val route = "detection"
    override val titleRes = R.string.detection
}

object DangerWarningDestination : NavigationDestination {
    override val route = "danger_warning"
    override val titleRes = R.string.danger_warning
}

object ExploreDestination: NavigationDestination {
    override val route = "explore"
    override val titleRes = R.string.explore
}

object MoodTrackingDestination: NavigationDestination {
    override val route = "tracking"
    override val titleRes = R.string.mood_tracking
}

object FaceRecognition: NavigationDestination {
    override val route = "face_recognition"
    override val titleRes = R.string.face_recognition
}