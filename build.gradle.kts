// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
}

// Define the versions as constants
val composeVersion: String by project
val cameraxVersion: String by project

// Top-level build file where you can add configuration options common to all sub-projects/modules.
buildscript {
    repositories {
        google()
        mavenCentral()
    }

    dependencies {
        classpath("com.google.dagger:hilt-android-gradle-plugin:2.48")
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:1.9.21")
    }
}

// Define the versions
extra["compose_version"] = "1.1.1"
extra["camerax_version"] = "1.2.0-alpha03"

// Task to clean the build directory
tasks.register<Delete>("clean") {
    delete(layout.buildDirectory)
}

