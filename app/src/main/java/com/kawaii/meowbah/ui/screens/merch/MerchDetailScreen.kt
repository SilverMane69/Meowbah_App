package com.kawaii.meowbah.ui.screens.merch

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.* // ktlint-disable no-wildcard-imports
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Share // Added for Share icon
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.kawaii.meowbah.R // For placeholder
import com.kawaii.meowbah.data.model.MerchItem

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MerchDetailScreen(
    navController: NavController,
    merchId: String?,
    merchViewModel: MerchViewModel = viewModel()
) {
    val context = LocalContext.current
    var merchItem by remember { mutableStateOf<MerchItem?>(null) }

    LaunchedEffect(merchId) {
        if (merchId != null) {
            merchItem = merchViewModel.getMerchItemById(merchId)
        }
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0.dp)
    ) { paddingValues ->
        if (merchItem == null) {
            Box(
                modifier = Modifier.fillMaxSize().padding(paddingValues).safeDrawingPadding(),
                contentAlignment = Alignment.Center
            ) {
                // Back button for consistency even when item is not found, or error
                FilledTonalIconButton(
                    onClick = { navController.popBackStack() },
                    modifier = Modifier.align(Alignment.TopStart).padding(16.dp) // Positioned top-left
                ) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }
                if (merchId == null) {
                    Text("Error: Merch ID not provided.", modifier = Modifier.padding(16.dp))
                } else {
                    Text("Loading merch details or item not found...", modifier = Modifier.padding(16.dp))
                }
            }
        } else {
            merchItem?.let { item ->
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues) // Apply scaffold padding first
                        .verticalScroll(rememberScrollState())
                        .safeDrawingPadding()
                    ,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Spacer(modifier = Modifier.height(64.dp)) // Added Spacer
                    // Row for Back and Share buttons
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp), // Padding for the row itself
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        FilledTonalIconButton(onClick = { navController.popBackStack() }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                        // Item name Text composable removed from here
                        FilledTonalIconButton(onClick = {
                            val shareText = if (item.storeUrl != null) {
                                "Check out this merch: ${item.name}. Available here: ${item.storeUrl}"
                            } else {
                                "Check out this merch: ${item.name}"
                            }
                            val shareIntent = Intent().apply {
                                action = Intent.ACTION_SEND
                                putExtra(Intent.EXTRA_TEXT, shareText)
                                type = "text/plain"
                            }
                            context.startActivity(Intent.createChooser(shareIntent, "Share Merch Via"))
                        }) {
                            Icon(Icons.Filled.Share, contentDescription = "Share Merch")
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp)) // Spacer after the buttons row

                    // Content padding now applied to a new inner Column for better control
                    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                        AsyncImage(
                            model = ImageRequest.Builder(LocalContext.current)
                                .data(item.imageResId)
                                .crossfade(true)
                                .placeholder(R.drawable.ic_placeholder) // Generic placeholder
                                .error(R.drawable.ic_launcher_background) // Error placeholder
                                .build(),
                            contentDescription = item.name,
                            modifier = Modifier
                                .fillMaxWidth()
                                .aspectRatio(1f) // Adjust aspect ratio as needed, 1f for square
                                .padding(bottom = 16.dp),
                            contentScale = ContentScale.Fit
                        )
                        Text(
                            text = item.name, // Name is still displayed here, below the image
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.align(Alignment.CenterHorizontally)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Price: ${item.price}",
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.align(Alignment.CenterHorizontally)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = item.description,
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                        item.storeUrl?.let {
                            FilledTonalButton(
                                onClick = {
                                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(it))
                                    context.startActivity(intent)
                                },
                                shape = MaterialTheme.shapes.medium,
                                modifier = Modifier.align(Alignment.CenterHorizontally)
                                               .fillMaxWidth(0.8f)
                            ) {
                                Text("View in Store")
                            }
                        }
                        Spacer(modifier = Modifier.height(16.dp)) // Add some padding at the bottom
                    }
                }
            }
        }
    }
}
