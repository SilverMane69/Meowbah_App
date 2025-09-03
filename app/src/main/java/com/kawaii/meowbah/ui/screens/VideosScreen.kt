package com.kawaii.meowbah.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.PlayCircleOutline
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DockedSearchBar
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.kawaii.meowbah.ui.screens.videos.VideoItem
import com.kawaii.meowbah.ui.screens.videos.VideosViewModel
import com.kawaii.meowbah.ui.screens.videos.formatViewCount

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VideosScreen(
    navController: NavController,
    viewModel: VideosViewModel = viewModel(),
    profileImageUri: String?
) {
    val videosState: List<VideoItem> by viewModel.videos.collectAsState()
    val isLoading: Boolean by viewModel.isLoading.collectAsState()
    val errorMessage: String? by viewModel.error.collectAsState()

    var searchQuery by rememberSaveable { mutableStateOf("") }
    var searchActive by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        if (!searchActive && videosState.isEmpty()) {
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
                placeholder = { Text("Search your library") },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = "Search") },
                trailingIcon = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = { /* TODO: Implement voice search */ }) {
                            Icon(Icons.Filled.Mic, contentDescription = "Voice search")
                        }
                        Spacer(modifier = Modifier.width(4.dp))
                        val imageModifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                        if (profileImageUri != null) {
                            AsyncImage(
                                model = profileImageUri,
                                placeholder = rememberVectorPainter(image = Icons.Filled.AccountCircle),
                                error = rememberVectorPainter(image = Icons.Filled.AccountCircle),
                                contentDescription = "User profile",
                                modifier = imageModifier,
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Filled.AccountCircle,
                                contentDescription = "User profile",
                                modifier = imageModifier,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant // Optional: match placeholder tint
                            )
                        }
                    }
                }
            ) {
                val filteredVideos = videosState.filter {
                    it.snippet.title.contains(searchQuery, ignoreCase = true) ||
                    it.snippet.channelTitle.contains(searchQuery, ignoreCase = true)
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
                                headlineContent = { Text(video.snippet.title, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                                supportingContent = { Text(video.snippet.channelTitle, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                                leadingContent = {
                                    AsyncImage(
                                        model = video.snippet.thumbnails.default.url,
                                        contentDescription = "Thumbnail for ${video.snippet.title}",
                                        modifier = Modifier.size(56.dp).clip(MaterialTheme.shapes.small),
                                        contentScale = ContentScale.Crop
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
                        VideoListItem(video = video, onVideoClick = {
                            navController.navigate("video_detail/${video.id}")
                        })
                    }
                }
            }
        }
    }
}

@Composable
fun VideoListItem(video: VideoItem, onVideoClick: (VideoItem) -> Unit) {
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
                    .data(video.snippet.thumbnails.high.url ?: video.snippet.thumbnails.medium.url)
                    .crossfade(true)
                    .build(),
                contentDescription = "Video thumbnail for ${video.snippet.title}",
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
                    text = video.snippet.title,
                    style = MaterialTheme.typography.headlineSmall,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = video.snippet.channelTitle,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Filled.PlayCircleOutline,
                        contentDescription = "Views",
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "${formatViewCount(video.statistics.viewCount.toLongOrNull() ?: 0)} views",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
    }
}
