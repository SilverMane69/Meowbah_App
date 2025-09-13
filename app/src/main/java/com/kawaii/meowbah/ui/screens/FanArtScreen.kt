package com.kawaii.meowbah.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
// import androidx.compose.material.ripple.rememberRipple // Import removed
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
// ViewModel import will be used from the separate FanArtViewModel.kt file
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.kawaii.meowbah.R
// kotlinx.coroutines.flow imports are used by the external FanArtViewModel

// Data class for Fan Art, imageUrl is a Drawable Int Res ID
data class FanArt(val id: String, val imageUrl: Int, val title: String? = null)

// Removed the embedded FanArtViewModel class definition from here
// The app will use FanArtViewModel from FanArtViewModel.kt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FanArtScreen(navController: NavController) {
    // This will now refer to the FanArtViewModel in FanArtViewModel.kt
    val viewModel: FanArtViewModel = viewModel()
    val fanArts by viewModel.fanArts.collectAsState()
    var selectedFanArt by remember { mutableStateOf<FanArt?>(null) }
    val context = LocalContext.current

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        // topBar = { TopAppBar(title = { Text("Fan Art Gallery") }) } // Optional: Add a title
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues) // Apply padding from Scaffold to the Column
        ) {
            if (fanArts.isEmpty()) {
                Box(
                    modifier = Modifier
                        .weight(1f) // Takes up available space
                        .fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No fan art available yet!\nCheck back soon!",
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    contentPadding = PaddingValues(16.dp), // Padding around the grid items
                    modifier = Modifier.weight(1f), // Grid takes available space
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(fanArts) { fanArt ->
                        FanArtListItem(fanArt = fanArt, onFanArtClick = { selectedFanArt = it })
                    }
                }
            }
            Spacer(modifier = Modifier.height(8.dp)) // Space above the button
            FilledTonalButton(
                onClick = {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.patreon.com/meowbah"))
                    context.startActivity(intent)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Text("Support Meowbah on Patreon")
            }
        }
    }

    selectedFanArt?.let { fanArt ->
        FullScreenFanArtDialog(fanArt = fanArt, onDismiss = { selectedFanArt = null })
    }
}

@Composable
fun FanArtListItem(fanArt: FanArt, onFanArtClick: (FanArt) -> Unit) {
    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(), // Reverted to ripple()
                onClick = { onFanArtClick(fanArt) }
            ),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp),
        shape = MaterialTheme.shapes.medium
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(fanArt.imageUrl)
                    .crossfade(true)
                    .placeholder(R.drawable.ic_placeholder)
                    .error(R.drawable.ic_launcher_background)
                    .build(),
                contentDescription = fanArt.title ?: "Fan art",
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
            )
            fanArt.title?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.labelSmall,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                        .fillMaxWidth(),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
fun FullScreenFanArtDialog(fanArt: FanArt, onDismiss: () -> Unit) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.85f))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null, // No indication for the background click
                    onClick = onDismiss
                ),
            contentAlignment = Alignment.Center
        ) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(fanArt.imageUrl)
                    .crossfade(true)
                    .placeholder(R.drawable.ic_placeholder)
                    .error(R.drawable.ic_launcher_background)
                    .build(),
                contentDescription = fanArt.title ?: "Full screen fan art",
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp) // Padding for the image itself within the dialog
            )
            IconButton(
                onClick = onDismiss,
                modifier = Modifier.align(Alignment.TopEnd).padding(16.dp)
            ) {
                Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
            }
        }
    }
}
