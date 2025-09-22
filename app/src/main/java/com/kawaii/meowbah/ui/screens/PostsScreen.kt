package com.kawaii.meowbah.ui.screens

import android.content.Intent
import android.net.Uri
import android.util.Log // Added for logging
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
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
    val context = LocalContext.current
    val twitterUrl = "https://x.com/meowbahv"

    Scaffold(
        contentWindowInsets = WindowInsets(0.dp) // Set to 0 for edge-to-edge
    ) { paddingValues ->
        Column(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            if (posts.isEmpty()) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .safeDrawingPadding(),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No posts yet. Check back later!")
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentPadding = WindowInsets.safeDrawing
                        .add(WindowInsets(left = 16.dp, top = 72.dp, right = 16.dp, bottom = 8.dp))
                        .asPaddingValues(), // Reduced bottom padding
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(posts) { post ->
                        PostItem(post = post)
                    }
                }
            }

            Button(
                onClick = {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(twitterUrl))
                    try {
                        context.startActivity(intent)
                    } catch (e: Exception) {
                        Log.e(TAG, "Error opening Twitter URL: $twitterUrl", e)
                        // Optionally show a toast or snackbar to the user about the error
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .navigationBarsPadding() // Symmetrical horizontal padding
            ) {
                Text("View on X (Twitter)")
            }
        }
    }
}

@Composable
fun PostItem(post: RssPost) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
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
