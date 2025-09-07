package com.kawaii.meowbah.ui.screens

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.speech.RecognizerIntent
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DockedSearchBar
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.kawaii.meowbah.R
import com.kawaii.meowbah.data.CachedVideoInfo
import com.kawaii.meowbah.ui.screens.videos.VideosViewModel
// import java.io.File // No longer needed
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VideosScreen(
    navController: NavController,
    viewModel: VideosViewModel
) {
    android.util.Log.d("VideosScreen", "ViewModel instance: $viewModel")

    val videosState: List<CachedVideoInfo> by viewModel.videos.collectAsState()
    val isLoading: Boolean by viewModel.isLoading.collectAsState()
    val errorMessage: String? by viewModel.error.collectAsState()

    var searchQuery by rememberSaveable { mutableStateOf("") }
    var searchActive by rememberSaveable { mutableStateOf(false) }
    val context = LocalContext.current
    val youtubeChannelUrl = "https://www.youtube.com/@Meowbahx"

    val speechRecognizerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val data: Intent? = result.data
            val results: ArrayList<String>? = data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            if (!results.isNullOrEmpty()) {
                searchQuery = results[0]
                searchActive = true
            }
        }
    }

    LaunchedEffect(Unit) {
        if (!searchActive && videosState.isEmpty() && !isLoading) {
            viewModel.fetchVideos()
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            DockedSearchBar(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                query = searchQuery,
                onQueryChange = { searchQuery = it },
                onSearch = { /* searchActive = false */ },
                active = searchActive,
                onActiveChange = { searchActive = it },
                placeholder = { Text("Search Kawaii Videos") },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = "Search") },
                trailingIcon = {
                    IconButton(onClick = {
                        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                            putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak now...")
                        }
                        try {
                            speechRecognizerLauncher.launch(intent)
                        } catch (e: ActivityNotFoundException) {
                            Toast.makeText(context, "Speech recognition is not available on this device.", Toast.LENGTH_LONG).show()
                        }
                    }) {
                        Icon(Icons.Filled.Mic, contentDescription = "Voice search")
                    }
                }
            ) {
                val filteredVideos = videosState.filter {
                    it.title.contains(searchQuery, ignoreCase = true)
                }
                if (filteredVideos.isEmpty() && searchQuery.isNotEmpty()) {
                    Box(modifier = Modifier.fillMaxSize().padding(16.dp), contentAlignment = Alignment.Center) {
                        Text("No results found for \"$searchQuery\"")
                    }
                } else {
                    LazyColumn(
                        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 8.dp)
                    ) {
                        items(filteredVideos) { video ->
                            ListItem(
                                headlineContent = { Text(video.title, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                                leadingContent = {
                                    AsyncImage(
                                        model = video.thumbnailUrl ?: R.drawable.ic_placeholder, // Changed to thumbnailUrl
                                        contentDescription = "Thumbnail for ${video.title}",
                                        modifier = Modifier.size(56.dp).clip(MaterialTheme.shapes.small),
                                        contentScale = ContentScale.Crop,
                                        error = painterResource(id = R.drawable.ic_placeholder),
                                        placeholder = painterResource(id = R.drawable.ic_placeholder)
                                    )
                                },
                                modifier = Modifier.clickable {
                                    searchActive = false
                                    navController.navigate("video_detail/${video.id}")
                                }
                            )
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            if (isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else if (errorMessage != null) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        text = "Error: $errorMessage",
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = innerPadding.calculateBottomPadding() + 8.dp)
                ) {
                    items(videosState) { video ->
                        VideoListItem(video = video, onVideoClick = { clickedVideo ->
                            navController.navigate("video_detail/${clickedVideo.id}")
                        })
                    }
                    if (videosState.isNotEmpty()) { 
                        item {
                            FilledTonalButton(
                                onClick = {
                                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(youtubeChannelUrl))
                                    context.startActivity(intent)
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 16.dp) 
                            ) {
                                Text("Show More on YouTube")
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun VideoListItem(video: CachedVideoInfo, onVideoClick: (CachedVideoInfo) -> Unit) {
    val publishedDateFormatted: String = video.publishedAt?.let { pubAtNonNull ->
        try {
            OffsetDateTime.parse(pubAtNonNull)
                .format(DateTimeFormatter.ofLocalizedDate(FormatStyle.LONG).withLocale(Locale.getDefault()))
        } catch (e: Exception) {
            pubAtNonNull
        }
    } ?: "Date unavailable"

    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(),
                onClick = { onVideoClick(video) }
            ),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 4.dp)
    ) {
        Column {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(video.thumbnailUrl ?: R.drawable.ic_placeholder) // Changed to thumbnailUrl
                    .crossfade(true)
                    .error(R.drawable.ic_placeholder)
                    .placeholder(R.drawable.ic_placeholder)
                    .build(),
                contentDescription = "Video thumbnail for ${video.title}",
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 9f)
                    .clip(MaterialTheme.shapes.medium),
                contentScale = ContentScale.Crop
            )
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = video.title,
                    style = MaterialTheme.typography.titleLarge,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = publishedDateFormatted,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 4.dp) 
                )
            }
        }
    }
}
