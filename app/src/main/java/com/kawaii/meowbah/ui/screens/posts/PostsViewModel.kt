package com.kawaii.meowbah.ui.screens.posts

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.kawaii.meowbah.data.db.AppDatabase
import com.kawaii.meowbah.data.model.RssPost
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

class PostsViewModel(application: Application) : AndroidViewModel(application) {

    private val rssPostDao = AppDatabase.getInstance(application).rssPostDao()

    val allPosts: StateFlow<List<RssPost>> = rssPostDao.getAllPosts()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000), // Keep the flow active for 5s after the last collector stops
            initialValue = emptyList()
        )
}
