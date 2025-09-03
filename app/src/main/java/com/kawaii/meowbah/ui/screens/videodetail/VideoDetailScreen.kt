package com.kawaii.meowbah.ui.screens.videodetail

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color // Added import
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.kawaii.meowbah.R
import com.kawaii.meowbah.ui.screens.videos.PlaceholderVideoDetailItem
import java.util.regex.Pattern

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VideoDetailScreen(
    navController: NavController,
    videoId: String,
    viewModel: VideoDetailViewModel = viewModel()
) {
    val videoDetail by viewModel.videoDetail.collectAsState(initial = null)
    val isLoading by viewModel.isLoading.collectAsState(initial = true)
    val error by viewModel.error.collectAsState(initial = null)
    val context = LocalContext.current // Get context for share intent

    LaunchedEffect(videoId) {
        viewModel.fetchVideoDetails(videoId)
    }

    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior(rememberTopAppBarState())

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            MediumTopAppBar(
                title = {
                    val titleText = when {
                        isLoading -> "Loading..."
                        videoDetail != null -> videoDetail?.snippet?.title ?: "Video Detail"
                        error != null -> "Error"
                        else -> "Video Detail"
                    }
                    Text(
                        text = titleText,
                        maxLines = if (scrollBehavior.state.collapsedFraction > 0.5) 1 else 2,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = {
                        videoDetail?.let { detail ->
                            val shareIntent = Intent().apply {
                                action = Intent.ACTION_SEND
                                putExtra(Intent.EXTRA_TEXT, "Check out this video: http://www.youtube.com/watch?v=${detail.id}")
                                type = "text/plain"
                            }
                            context.startActivity(Intent.createChooser(shareIntent, "Share video via"))
                        }
                    }) {
                        Icon(Icons.Filled.Share, contentDescription = "Share video")
                    }
                },
                colors = TopAppBarDefaults.mediumTopAppBarColors(
                    containerColor = Color.Transparent, // Changed
                    titleContentColor = MaterialTheme.colorScheme.onSurface, // Adjusted
                    navigationIconContentColor = MaterialTheme.colorScheme.onSurface, // Adjusted
                    actionIconContentColor = MaterialTheme.colorScheme.onSurface, // Adjusted
                    scrolledContainerColor = MaterialTheme.colorScheme.surfaceVariant
                ),
                scrollBehavior = scrollBehavior
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when {
                isLoading -> {
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
                    val detail = videoDetail!!
                    VideoDetailContent(detail = detail)
                }
                else -> {
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
        ElevatedCard(
            shape = MaterialTheme.shapes.medium,
            elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp),
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
                placeholder = painterResource(id = R.drawable.ic_placeholder),
                error = painterResource(id = R.drawable.ic_placeholder),
                modifier = Modifier.fillMaxSize()
            )
        }

        Text(
            text = detail.snippet?.title ?: "No Title",
            style = MaterialTheme.typography.headlineSmall,
            textAlign = TextAlign.Start,
            modifier = Modifier.fillMaxWidth()
        )

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

        Text(
            text = detail.snippet?.description ?: "No description available.",
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Start,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(8.dp))

        FilledTonalButton(
            onClick = {
                val appIntent = Intent(Intent.ACTION_VIEW, Uri.parse("vnd.youtube:${detail.id}"))
                val webIntent = Intent(Intent.ACTION_VIEW, Uri.parse("http://www.youtube.com/watch?v=${detail.id}"))
                try {
                    context.startActivity(appIntent)
                } catch (ex: ActivityNotFoundException) {
                    context.startActivity(webIntent)
                }
            },
            modifier = Modifier.fillMaxWidth(0.8f)
        ) {
            Text("Open in YouTube")
        }
    }
}

fun formatDuration(isoDuration: String?): String {
    if (isoDuration.isNullOrBlank()) return ""

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
        }.removeSuffix(":")
            .let { if (it.startsWith("0:")) it.substring(2) else it }
            .let { if (it == "00") "0:00" else it}
            .let { if (it.isEmpty()) "0:00" else it }
    }
    return "N/A"
}

