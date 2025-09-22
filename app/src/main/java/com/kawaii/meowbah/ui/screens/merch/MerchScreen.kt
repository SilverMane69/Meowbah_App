package com.kawaii.meowbah.ui.screens.merch

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.* // ktlint-disable no-wildcard-imports
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.kawaii.meowbah.R // For placeholder
import com.kawaii.meowbah.data.model.MerchItem

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MerchScreen(
    navController: NavController,
    merchViewModel: MerchViewModel = viewModel()
) {
    val merchItems by merchViewModel.merchItems.collectAsState()

    Scaffold(
        contentWindowInsets = WindowInsets(0.dp) // Set to 0 for edge-to-edge
    ) { paddingValues ->
        if (merchItems.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize().padding(paddingValues).safeDrawingPadding(),
                contentAlignment = Alignment.Center
            ) {
                Text("No merchandise available yet. Stay tuned!")
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2), // You can adjust the number of columns
                modifier = Modifier.fillMaxSize(),
                contentPadding = WindowInsets.safeDrawing
                    .add(WindowInsets(left = 16.dp, top = 16.dp, right = 16.dp, bottom = 16.dp))
                    .asPaddingValues(),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(merchItems) { item ->
                    MerchGridItem(merchItem = item) {
                        // Navigate to MerchDetailScreen, passing the merch item's ID
                        // We'll define "merchDetail/{merchId}" route later
                        navController.navigate("merchDetail/${item.id}")
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MerchGridItem(
    merchItem: MerchItem,
    onItemClick: () -> Unit
) {
    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onItemClick),
        shape = MaterialTheme.shapes.medium,
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(merchItem.imageResId) // Using local drawable
                    .crossfade(true)
                    .placeholder(R.drawable.ic_placeholder) // Generic placeholder
                    .error(R.drawable.ic_launcher_background) // Error placeholder
                    .build(),
                contentDescription = merchItem.name,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f), // Makes the image square
                contentScale = ContentScale.Crop
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = merchItem.name,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = merchItem.price,
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center
            )
        }
    }
}
