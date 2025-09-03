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
        // Read API key from local.properties and add to BuildConfig
        val youtubeApiKeyFromProperties = properties.getProperty("YOUTUBE_API_KEY") ?: ""
        // Escape backslashes and double quotes in the key for safe embedding in Java string literal
        val escapedYoutubeApiKey = youtubeApiKeyFromProperties
            .replace("\\", "\\\\") // Replace \ with \\
            .replace("\"", "\\\"") // Replace " with \"
        buildConfigField("String", "YOUTUBE_API_KEY", "\"$escapedYoutubeApiKey\"")

        // Read Channel ID from local.properties and add to BuildConfig
        val youtubeChannelIdFromProperties = properties.getProperty("YOUTUBE_CHANNEL_ID") ?: ""
        val escapedYoutubeChannelId = youtubeChannelIdFromProperties
            .replace("\\", "\\\\")
            .replace("\"", "\\\"")
        buildConfigField("String", "YOUTUBE_CHANNEL_ID", "\"$escapedYoutubeChannelId\"")
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
        buildConfig = true // Ensure buildConfig is enabled
    }
    composeOptions {
        kotlinCompilerExtensionVersion = libs.versions.androidxComposeCompiler.get()
    }
    kotlinOptions { 
        jvmTarget = "11"
    }
}

dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom)) 
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation("androidx.appcompat:appcompat:1.7.1") // Added AppCompat
    
    // Material 3 and Adaptive Dependencies
    implementation("androidx.compose.material3:material3") 
    implementation("androidx.compose.material3:material3-window-size-class")
    implementation("androidx.compose.material3:material3-adaptive-navigation-suite:1.5.0-alpha03")

    implementation(libs.androidx.navigation.compose) // For Jetpack Compose Navigation


    implementation(libs.androidx.material.icons.core)
    implementation(libs.androidx.material.icons.extended)
    implementation(libs.retrofit) // This should be com.squareup.retrofit2:retrofit
    implementation(libs.converter.gson) // This should be com.squareup.retrofit2:converter-gson
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0") // Added OkHttp Logging Interceptor
    implementation(libs.coil.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.work.runtime.ktx) // Added WorkManager
    implementation("com.google.android.gms:play-services-auth:21.4.0") // Added Google Play Services Auth
    implementation("androidx.security:security-crypto:1.1.0") // Added for EncryptedSharedPreferences

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
}

