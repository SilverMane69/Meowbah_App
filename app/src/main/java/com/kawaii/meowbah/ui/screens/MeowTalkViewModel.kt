package com.kawaii.meowbah.ui.screens

import android.app.Application
import android.content.Context
import android.content.Intent // Added import
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.kawaii.meowbah.MainActivity // For accessing companion object constants
import com.kawaii.meowbah.data.MeowTalkPhrases
import com.kawaii.meowbah.util.MeowTalkEventBus
import com.kawaii.meowbah.widget.MeowTalkWidgetProvider // Added import
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import kotlin.random.Random

class MeowTalkViewModel(application: Application) : AndroidViewModel(application) {

    private val _phrases = MeowTalkPhrases.list
    private val app = application

    private val _currentPhrase = MutableStateFlow("") // Displayed on screen
    val currentPhrase: StateFlow<String> = _currentPhrase.asStateFlow()

    companion object {
        private const val TAG = "MeowTalkViewModel"
    }

    init {
        Log.d(TAG, "MeowTalkViewModel: Initializing...")
        loadOrActivateInitialPhrase()

        viewModelScope.launch {
            MeowTalkEventBus.events
                .catch { e -> Log.e(TAG, "Error collecting MeowTalk events", e) }
                .collect { activatedPhraseByNotification -> 
                    Log.d(TAG, "Event received: Phrase \"$activatedPhraseByNotification\" was activated by notification.")
                    _currentPhrase.value = activatedPhraseByNotification
                }
        }
    }

    private fun loadOrActivateInitialPhrase() {
        val sharedPrefs = app.getSharedPreferences(MainActivity.PREFS_NAME, Context.MODE_PRIVATE)
        val existingPhrase = sharedPrefs.getString(MainActivity.KEY_MEOWTALK_CURRENT_PHRASE, null)
        if (existingPhrase != null) {
            _currentPhrase.value = existingPhrase
            Log.d(TAG, "Loaded initial phrase from Prefs: $existingPhrase")
        } else {
            Log.d(TAG, "No initial phrase in Prefs. Activating and saving a new one.")
            activateNewRandomPhrase(isInitial = true)
        }
    }

    fun userRequestedNewPhrase() {
        Log.d(TAG, "User requested new phrase.")
        activateNewRandomPhrase()
    }

    private fun activateNewRandomPhrase(isInitial: Boolean = false) {
        val newPhrase = if (_phrases.isNotEmpty()) {
            _phrases[Random.nextInt(_phrases.size)]
        } else {
            "No MeowTalk phrases available!"
        }
        _currentPhrase.value = newPhrase
        saveCurrentPhraseToPrefs(newPhrase)

        if(isInitial) {
            Log.d(TAG, "Initial phrase activated and saved: $newPhrase")
        } else {
            Log.d(TAG, "New phrase activated by user and saved: $newPhrase")
        }
    }

    private fun saveCurrentPhraseToPrefs(phrase: String) {
        val sharedPrefs = app.getSharedPreferences(MainActivity.PREFS_NAME, Context.MODE_PRIVATE)
        with(sharedPrefs.edit()) {
            putString(MainActivity.KEY_MEOWTALK_CURRENT_PHRASE, phrase)
            apply()
        }
        Log.d(TAG, "Saved current phrase to SharedPreferences: $phrase")

        // Broadcast that the phrase has been updated
        val intent = Intent(app, MeowTalkWidgetProvider::class.java).apply {
            action = MeowTalkWidgetProvider.ACTION_MEOWTALK_PHRASE_UPDATED
        }
        app.sendBroadcast(intent)
        Log.d(TAG, "Sent broadcast: ${MeowTalkWidgetProvider.ACTION_MEOWTALK_PHRASE_UPDATED}")
    }
}
