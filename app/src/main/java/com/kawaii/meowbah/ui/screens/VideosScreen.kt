package com.kawaii.meowbah.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.PlayCircleOutline
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CardDefaults 
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem 
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MediumTopAppBar
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SearchBar
// import androidx.compose.material3.SearchBarDefaults // Keep commented or use if specific default tweaks are needed
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState // Added import
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.isTraversalGroup
import androidx.compose.ui.semantics.semantics
// import androidx.compose.ui.semantics.traversalIndex // Not strictly needed for this adjustment
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
    viewModel: VideosViewModel = viewModel()
) {
    val videosState: List<VideoItem> by viewModel.videos.collectAsState()
    val isLoading: Boolean by viewModel.isLoading.collectAsState()
    val errorMessage: String? by viewModel.error.collectAsState()

    var searchQuery by rememberSaveable { mutableStateOf("") }
    var searchActive by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        if (!searchActive) { 
            viewModel.fetchVideos()
        }
    }

    // Corrected scrollBehavior for the MediumTopAppBar when search is not active
    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior(rememberTopAppBarState())

    Box(Modifier.fillMaxSize()) { 
        Scaffold(
            modifier = Modifier
                .fillMaxSize()
                // Apply nestedScroll only when search is NOT active, as SearchBar handles its own scroll/elevation.
                .then(if (!searchActive) Modifier.nestedScroll(scrollBehavior.nestedScrollConnection) else Modifier),
            topBar = {
                if (!searchActive) {
                    MediumTopAppBar(
                        title = { Text("Videos") },
                        scrollBehavior = scrollBehavior,
                        colors = TopAppBarDefaults.mediumTopAppBarColors(
                            containerColor = Color.Transparent,
                            titleContentColor = MaterialTheme.colorScheme.onSurface,
                            actionIconContentColor = MaterialTheme.colorScheme.onSurface,
                            scrolledContainerColor = MaterialTheme.colorScheme.surfaceVariant 
                        ),
                        actions = {
                            IconButton(onClick = { searchActive = true }) {
                                Icon(Icons.Filled.Search, contentDescription = "Search videos")
                            }
                        }
                    )
                } // Else: No TopAppBar when SearchBar is active
            }
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    // Use innerPadding from Scaffold only when the main content is shown
                    .padding(if (!searchActive) innerPadding else PaddingValues(0.dp)) 
            ) {
                if (isLoading && !searchActive) { // Show loader only if not searching
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally).padding(16.dp))
                } else if (errorMessage != null && !searchActive) { // Show error only if not searching
                    Text(
                        text = "Error: $errorMessage",
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.align(Alignment.CenterHorizontally).padding(16.dp)
                    )
                } else if (!searchActive) {
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

        // SearchBar is placed in the Box to overlay Scaffold when active
        if (searchActive) {
            SearchBar(
                modifier = Modifier
                    .fillMaxWidth() // Ensures SearchBar takes full width when active
                    .align(Alignment.TopCenter)
                    .semantics { isTraversalGroup = true },
                query = searchQuery,
                onQueryChange = { searchQuery = it },
                onSearch = { searchActive = false }, 
                active = searchActive,
                onActiveChange = { 
                    searchActive = it 
                    if (!it) {
                        // Optional: Clear search query when search becomes inactive
                        // searchQuery = ""
                    }
                },
                placeholder = { Text("Search videos") },
                leadingIcon = {
                    IconButton(onClick = { 
                        searchActive = false 
                        // searchQuery = "" // Also clear query on back press
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Filled.Clear, contentDescription = "Clear search")
                        }
                    }
                }
                // Rely on default SearchBar colors for system-like appearance
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
                    LazyColumn(contentPadding = PaddingValues(16.dp)) {
                        items(filteredVideos) { video ->
                            // Using a more standard ListItem for search results
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
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(video.snippet.thumbnails.medium.url)
                    .crossfade(true)
                    .build(),
                contentDescription = "Video thumbnail",
                modifier = Modifier
                    .size(120.dp, 90.dp)
                    .clip(MaterialTheme.shapes.medium),
                contentScale = ContentScale.Crop
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = video.snippet.title,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = video.snippet.channelTitle,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Filled.PlayCircleOutline,
                        contentDescription = "Views",
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "${formatViewCount(video.statistics.viewCount.toLongOrNull() ?: 0)} views",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
    }
}
