package com.kawaii.meowbah.ui.screens

import android.app.Application
// Removed: android.graphics.Bitmap
// Removed: android.graphics.drawable.BitmapDrawable
import android.util.Log
// Removed: androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope // Still needed for other coroutines if any, but might become unused
// Removed: com.google.android.gms.wearable.Asset
// Removed: com.google.android.gms.wearable.PutDataMapRequest
// Removed: com.google.android.gms.wearable.Wearable
import com.kawaii.meowbah.R // Assuming FanArt data class uses R.drawable for imageUrl
// Removed: com.kawaii.meowbah.WearableDataConstants
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
// Removed: kotlinx.coroutines.launch - if sendFanArtToWatch was the only user of viewModelScope.launch
// Removed: kotlinx.coroutines.tasks.await
// Removed: kotlinx.coroutines.withTimeoutOrNull
// Removed: java.io.ByteArrayOutputStream

class FanArtViewModel(private val application: Application) : AndroidViewModel(application) {
    private val _fanArts = MutableStateFlow<List<FanArt>>(emptyList())
    val fanArts: StateFlow<List<FanArt>> = _fanArts.asStateFlow()

    // Removed: private val dataClient by lazy { Wearable.getDataClient(application) }

    companion object {
        private const val TAG = "FanArtViewModel"
    }

    init {
        Log.d(TAG, "FanArtViewModel: Initializing...")
        loadManualFanArts()
    }

    private fun loadManualFanArts() {
        _fanArts.value = listOf(
            FanArt(id = "1", imageUrl = R.drawable.quran, title = "Meow Reads The Quran"),
            FanArt(id = "2", imageUrl = R.drawable.friends, title = "Meow and CommotionSickness"),
            FanArt(id = "3", imageUrl = R.drawable.girlgirl, title = "HEAVY METAL LOVER Animation"),
            FanArt(id = "4", imageUrl = R.drawable.mad, title = "Meowbah Being Sus"),
            FanArt(id = "5", imageUrl = R.drawable.meowcommotion, title = "More Meow and Commotion Shenanigans"),
            FanArt(id = "6", imageUrl = R.drawable.meowism, title = "Holy Meowbah"),
            FanArt(id = "7", imageUrl = R.drawable.moe, title = "Moe Art (Whatever That Means)"),
            FanArt(id = "8", imageUrl = R.drawable.nyan, title = "NYAN NYAN NIHAO NYAN"),
            FanArt(id = "9", imageUrl = R.drawable.oldart, title = "Old Meowbah Art"),
            FanArt(id = "10", imageUrl = R.drawable.plush, title = "Meowbah Made A Plush!!"),
            FanArt(id = "11", imageUrl = R.drawable.soup, title = "Meow Eating Soup... Or At Least Trying To")
        )
        Log.d(TAG, "Loaded manual fan arts. Count: ${_fanArts.value.size}")

        // Removed block that called sendFanArtToWatch
        // _fanArts.value.firstOrNull()?.let {
        //    Log.d(TAG, "FanArtViewModel: Attempting to send initial fan art after loading.")
        //    sendFanArtToWatch(it)
        // }
    }

    // Removed: fun sendFanArtToWatch(fanArt: FanArt) { ... }
}
