import java.util.Properties // Added for local.properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    id("kotlin-parcelize") // Added this line
}

// Load local.properties
val properties = Properties()
val localPropertiesFile = project.rootProject.file("local.properties")
if (localPropertiesFile.exists()) {
    properties.load(localPropertiesFile.inputStream())
}

android {
    namespace = "com.kawaii.meowbah"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.kawaii.meowbah"
        minSdk = 31
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        // Removed YOUTUBE_API_KEY and YOUTUBE_CHANNEL_ID BuildConfig fields
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        compose = true
        buildConfig = true // Keep true if other BuildConfig fields might be used, or set to false if none are left.
    }
    composeOptions {
        kotlinCompilerExtensionVersion = libs.versions.androidxComposeCompiler.get()
    }
    kotlinOptions { 
        jvmTarget = "11"
    }
}

dependencies {
    // wearApp(project(":wear")) // REMOVED THIS LINE

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom)) 
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation("androidx.appcompat:appcompat:1.7.1") // Added AppCompat
    implementation("com.google.android.material:material:1.13.0") // Added Material Components for Android
    // implementation("com.google.android.gms:play-services-wearable:18.1.0") // REMOVED THIS LINE
    // implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.7.3") // REMOVED THIS LINE
    
    // Material 3 and Adaptive Dependencies
    implementation("androidx.compose.material3:material3") 
    implementation("androidx.compose.material3:material3-window-size-class")
    implementation("androidx.compose.material3:material3-adaptive-navigation-suite:1.5.0-alpha03")

    implementation(libs.androidx.navigation.compose) // For Jetpack Compose Navigation


    implementation(libs.androidx.material.icons.core)
    implementation(libs.androidx.material.icons.extended)
    implementation(libs.coil.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.work.runtime.ktx) // Added WorkManager
    implementation("androidx.security:security-crypto:1.1.0") // Added for EncryptedSharedPreferences
    implementation("androidx.compose.material3:material3-window-size-class:1.2.1")
    implementation(libs.androidx.compose.material)
    implementation(libs.androidx.compose.foundation)
    implementation(libs.androidx.wear.tooling.preview)
    implementation(libs.androidx.core.splashscreen)

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
}
