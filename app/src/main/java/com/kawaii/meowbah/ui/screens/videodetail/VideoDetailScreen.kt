package com.kawaii.meowbah.ui.screens.videodetail

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.kawaii.meowbah.R
import com.kawaii.meowbah.data.CachedVideoInfo // Changed from VideoItem
import com.kawaii.meowbah.ui.screens.videos.VideosViewModel 
// import com.kawaii.meowbah.ui.screens.videos.formatViewCount // formatViewCount might be removed if stats are gone
// import java.io.File // For loading image from path - Removed
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

private const val TAG = "VideoDetailScreen"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VideoDetailScreen(
    navController: NavController,
    videoId: String, 
    videosViewModel: VideosViewModel
) {
    Log.d(TAG, "Composing for videoId: '$videoId'. ViewModel instance: $videosViewModel")

    val videosList by videosViewModel.videos.collectAsState() // This is List<CachedVideoInfo>
    val isLoadingFromVM by videosViewModel.isLoading.collectAsState() 
    val errorFromVM by videosViewModel.error.collectAsState()

    Log.d(TAG, "videosList size: ${videosList.size}. First item ID (if any): ${videosList.firstOrNull()?.id}. IsLoading: $isLoadingFromVM, Error: $errorFromVM")

    val videoItem: CachedVideoInfo? = remember(videosList, videoId) {
        val idToFind = videoId.trim().removeSurrounding("\"") 
        val foundItem = videosList.find { it.id == idToFind }
        Log.d(TAG, "Finding videoId (sanitized): '$idToFind'. Found item: ${foundItem != null}. Item Title: ${foundItem?.title?.take(30)}..., Item PubAt: ${foundItem?.publishedAt}")
        foundItem
    }

    Scaffold(
        modifier = Modifier.fillMaxSize()
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when {
                isLoadingFromVM && videoItem == null -> {
                    Log.d(TAG, "Showing loading indicator (isLoadingFromVM && videoItem == null)")
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }
                errorFromVM != null && videoItem == null -> {
                    Log.d(TAG, "Showing error: $errorFromVM (errorFromVM != null && videoItem == null)")
                    Text(
                        text = "Error: $errorFromVM",
                        color = MaterialTheme.colorScheme.error,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(16.dp)
                    )
                }
                videoItem != null -> {
                    Log.d(TAG, "Showing VideoDetailContent for videoId: ${videoItem.id}")
                    VideoDetailContent(videoItem = videoItem, navController = navController)
                }
                else -> { 
                    Log.d(TAG, "Showing fallback: Video details not found for ID '$videoId'. isLoading: $isLoadingFromVM, error: $errorFromVM, videoListSize: ${videosList.size}")
                    Text(
                        "Video details not found. It might have been removed or the ID is incorrect.",
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(16.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun VideoDetailContent(
    videoItem: CachedVideoInfo, // Changed from VideoItem
    navController: NavController
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(horizontal = 16.dp, vertical = 16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            FilledTonalIconButton(onClick = { navController.popBackStack() }) {
                Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
            }
            FilledTonalIconButton(onClick = {
                val shareIntent = Intent().apply {
                    action = Intent.ACTION_SEND
                    putExtra(Intent.EXTRA_TEXT, "Check out this video: http://www.youtube.com/watch?v=${videoItem.id}")
                    type = "text/plain"
                }
                context.startActivity(Intent.createChooser(shareIntent, "Share video via"))
            }) {
                Icon(imageVector = Icons.Filled.Share, contentDescription = "Share video")
            }
        }
        Spacer(modifier = Modifier.height(8.dp))

        ElevatedCard(
            shape = MaterialTheme.shapes.medium,
            elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp),
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 9f)
        ) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(videoItem.thumbnailUrl ?: R.drawable.ic_placeholder) // Corrected: Use thumbnailUrl
                    .crossfade(true)
                    .error(R.drawable.ic_placeholder)
                    .placeholder(R.drawable.ic_placeholder)
                    .build(),
                contentDescription = "Video thumbnail for ${videoItem.title}",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }

        Text(
            text = videoItem.title,
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
        )
        
        Text(
            text = formatPublishedAtDate(videoItem.publishedAt),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.fillMaxWidth()
        )

        // Statistics (view count, like count) have been removed as they are not in CachedVideoInfo
        // If these are needed, a separate mechanism to fetch full video details would be required.

        videoItem.description?.let {
             Text(
                text = it,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
            )
        }
       
        Spacer(modifier = Modifier.height(16.dp))

        FilledTonalButton(
            onClick = {
                val appIntent = Intent(Intent.ACTION_VIEW, Uri.parse("vnd.youtube:${videoItem.id}"))
                val webIntent = Intent(Intent.ACTION_VIEW, Uri.parse("http://www.youtube.com/watch?v=${videoItem.id}"))
                try {
                    context.startActivity(appIntent)
                } catch (ex: ActivityNotFoundException) {
                    context.startActivity(webIntent)
                }
            },
            shape = MaterialTheme.shapes.medium,
            modifier = Modifier.align(Alignment.CenterHorizontally)
                           .fillMaxWidth(0.8f)
        ) {
            Text("Open in YouTube")
        }
        Spacer(modifier = Modifier.height(16.dp))
    }
}

fun formatPublishedAtDate(isoDate: String?): String {
    if (isoDate.isNullOrBlank()) return "Unknown date"
    return try {
        val odt = OffsetDateTime.parse(isoDate)
        val formatter = DateTimeFormatter.ofPattern("MMM d, yyyy", Locale.getDefault())
        odt.format(formatter)
    } catch (e: Exception) {
        Log.w(TAG, "Error formatting date: $isoDate", e) 
        "Date unavailable"
    }
}
