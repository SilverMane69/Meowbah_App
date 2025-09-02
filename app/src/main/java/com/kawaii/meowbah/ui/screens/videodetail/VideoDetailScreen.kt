package com.kawaii.meowbah.ui.screens.videodetail

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.kawaii.meowbah.R // Make sure you have R.drawable.ic_placeholder
import com.kawaii.meowbah.ui.screens.videos.PlaceholderVideoDetailItem
import java.util.regex.Pattern

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VideoDetailScreen(
    navController: NavController,
    videoId: String,
    viewModel: VideoDetailViewModel = viewModel()
) {
    // Provide initial values for collectAsState
    val videoDetail by viewModel.videoDetail.collectAsState(initial = null)
    val isLoading by viewModel.isLoading.collectAsState(initial = true) // Assuming it starts loading
    val error by viewModel.error.collectAsState(initial = null)
    val context = LocalContext.current

    LaunchedEffect(videoId) {
        viewModel.fetchVideoDetails(videoId)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    val titleText = when {
                        isLoading -> "Loading..."
                        videoDetail != null -> videoDetail?.snippet?.title ?: "Video Detail"
                        error != null -> "Error" // Or some other appropriate title for error state
                        else -> "Video Detail" // Default title if not loading, no error, and no data yet
                    }
                    Text(
                        text = titleText,
                        maxLines = 1,
                        style = MaterialTheme.typography.titleLarge
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
        // The duplicated/misplaced block that was previously here has been removed.
        // The Scaffold's topBar lambda now correctly contains a single, complete TopAppBar.
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when {
                isLoading -> { // isLoading will now be true initially
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }
                error != null -> {
                    Text(
                        text = "Error: $error",
                        color = MaterialTheme.colorScheme.error,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(16.dp)
                    )
                }
                videoDetail != null -> {
                    val detail = videoDetail!! // Safe to use !! here because of the null check
                    VideoDetailContent(detail = detail)
                }
                else -> { // This case might be hit briefly if videoDetail is null initially
                    Text(
                        "No video details available.",
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
fun VideoDetailContent(detail: PlaceholderVideoDetailItem) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Thumbnail
        Card(
            shape = MaterialTheme.shapes.medium,
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 9f)
        ) {
            AsyncImage(
                model = detail.snippet?.thumbnails?.high?.url
                    ?: detail.snippet?.thumbnails?.medium?.url
                    ?: detail.snippet?.thumbnails?.default?.url,
                contentDescription = "Video thumbnail for ${detail.snippet?.title}",
                contentScale = ContentScale.Crop,
                placeholder = painterResource(id = R.drawable.ic_placeholder), // Ensure this drawable exists
                error = painterResource(id = R.drawable.ic_placeholder),      // Ensure this drawable exists
                modifier = Modifier.fillMaxSize()
            )
        }

        // Title
        Text(
            text = detail.snippet?.title ?: "No Title",
            style = MaterialTheme.typography.headlineSmall,
            textAlign = TextAlign.Start,
            modifier = Modifier.fillMaxWidth()
        )

        // Channel and Duration (in a Row)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = detail.snippet?.channelTitle ?: "Unknown Channel",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            detail.contentDetails?.duration?.let { isoDuration ->
                Text(
                    text = formatDuration(isoDuration),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }


        // Description
        Text(
            text = detail.snippet?.description ?: "No description available.",
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Start,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(8.dp)) // Extra space before button

        // Open in YouTube Button
        Button(
            onClick = {
                val appIntent = Intent(Intent.ACTION_VIEW, Uri.parse("vnd.youtube:${detail.id}"))
                val webIntent = Intent(Intent.ACTION_VIEW, Uri.parse("http://www.youtube.com/watch?v=${detail.id}"))
                try {
                    context.startActivity(appIntent)
                } catch (ex: ActivityNotFoundException) {
                    context.startActivity(webIntent)
                }
            },
            modifier = Modifier.fillMaxWidth(0.8f) // Button takes 80% of width
        ) {
            Text("Open in YouTube")
        }
    }
}

fun formatDuration(isoDuration: String?): String {
    if (isoDuration.isNullOrBlank()) return ""

    // Regular expression to parse ISO 8601 duration
    // Example: PT1M30S (1 minute, 30 seconds), PT2H (2 hours), PT45S (45 seconds)
    // This regex handles hours (H), minutes (M), and seconds (S)
    val pattern = Pattern.compile("PT(?:(\\d+)H)?(?:(\\d+)M)?(?:(\\d+)S)?")
    val matcher = pattern.matcher(isoDuration)

    if (matcher.matches()) {
        val hours = matcher.group(1)?.toLong() ?: 0L
        val minutes = matcher.group(2)?.toLong() ?: 0L
        val seconds = matcher.group(3)?.toLong() ?: 0L

        val totalSeconds = hours * 3600 + minutes * 60 + seconds

        val displayHours = totalSeconds / 3600
        val displayMinutes = (totalSeconds % 3600) / 60
        val displaySeconds = totalSeconds % 60

        return buildString {
            if (displayHours > 0) {
                append(String.format("%d:", displayHours))
                append(String.format("%02d:", displayMinutes))
            } else {
                append(String.format("%d:", displayMinutes))
            }
            append(String.format("%02d", displaySeconds))
        }.removeSuffix(":") // Clean up if only hours or minutes exist and seconds are zero
            .let { if (it.startsWith("0:")) it.substring(2) else it } // Remove leading "0:" if no hours
            .let { if (it == "00") "0:00" else it} // Handle PT0S case
            .let { if (it.isEmpty()) "0:00" else it }
    }
    return "N/A" // Return N/A or empty if parsing fails
}


