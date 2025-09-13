package com.kawaii.meowbah.util

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.GlobalScope // Use with caution, or inject a scope
import kotlinx.coroutines.launch

object MeowTalkEventBus {
    // Event now carries the String phrase that was activated
    private val _events = MutableSharedFlow<String>() 
    val events = _events.asSharedFlow()

    fun emitNewPhraseActivated(phrase: String) {
        GlobalScope.launch { 
            _events.emit(phrase)
        }
    }
}
