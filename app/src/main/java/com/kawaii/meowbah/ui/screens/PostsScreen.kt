package com.kawaii.meowbah.ui.screens

import android.util.Log // Added for logging
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.kawaii.meowbah.data.model.RssPost
import com.kawaii.meowbah.ui.screens.posts.PostsViewModel
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

private const val TAG = "PostsScreen"

@Composable
fun PostsScreen(
    navController: NavController,
    postsViewModel: PostsViewModel = viewModel()
) {
    val posts by postsViewModel.allPosts.collectAsState()

    if (posts.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text("No posts yet. Check back later!")
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(posts) { post ->
                PostItem(post = post)
            }
        }
    }
}

@Composable
fun PostItem(post: RssPost) {
    // Log the image URL received by the PostItem
    Log.d(TAG, "PostItem for '${post.title}' - Received imageUrl: ${post.imageUrl}")

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column {
            if (post.imageUrl != null) {
                Log.d(TAG, "Attempting to load image for '${post.title}': ${post.imageUrl}") // Log attempt to load
                AsyncImage(
                    model = post.imageUrl,
                    contentDescription = "Post image: ${post.title}",
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    contentScale = ContentScale.Crop,
                    onError = { error ->
                        Log.e(TAG, "Error loading image for '${post.title}': ${post.imageUrl}", error.result.throwable)
                    },
                    onSuccess = {
                        Log.d(TAG, "Successfully loaded image for '${post.title}': ${post.imageUrl}")
                    }
                )
                Spacer(modifier = Modifier.height(8.dp))
            } else {
                 Log.d(TAG, "No image URL for '${post.title}', skipping image display.")
            }
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = post.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))

                val formattedDate = Instant.ofEpochSecond(post.pubDateEpochSeconds)
                    .atZone(ZoneId.systemDefault())
                    .format(DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM))
                Text(
                    text = "Published: $formattedDate",
                    style = MaterialTheme.typography.bodySmall
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = android.text.Html.fromHtml(post.description, android.text.Html.FROM_HTML_MODE_COMPACT).toString(),
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )
                // TODO: Add a way to view the full post (e.g., navigate to a detail screen or open link)
            }
        }
    }
}
