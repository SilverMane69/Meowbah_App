package com.kawaii.meowbah.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape // Ensured import for FAB
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add // Added for FAB
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.material3.ripple
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.kawaii.meowbah.R
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

// Mock data - replace with your actual data source and ViewModel
data class FanArt(val id: String, val imageUrl: String, val artistName: String)

// Mock ViewModel - replace with your actual ViewModel
class FanArtViewModel : androidx.lifecycle.ViewModel() {
    private val _fanArts = MutableStateFlow<List<FanArt>>(emptyList())
    val fanArts: StateFlow<List<FanArt>> = _fanArts.asStateFlow()

    init {
        loadFanArts()
    }

    private fun loadFanArts() {
        // Simulate loading data
        _fanArts.value = listOf(
            FanArt("1", "https://pbs.twimg.com/media/GEUyAJzaMAAc6r3.jpg", "Artist 1"),
            FanArt("2", "https://pbs.twimg.com/media/GEUyAJpagAAg1x0.jpg", "Artist 2"),
            FanArt("3", "https://pbs.twimg.com/media/GEUyAJvaMAEWl2i.jpg", "Artist 3"),
            FanArt("4", "https://pbs.twimg.com/media/GEUyAJxaYAA3R2v.jpg", "Artist 4"),
            FanArt("5", "https://pbs.twimg.com/media/F5z4XkKXgAAK5uM.jpg","Artist 5"),
            FanArt("6", "https://pbs.twimg.com/media/GCgE2I4WkAAg1i6.jpg","Artist 6"),
            FanArt("7", "https://pbs.twimg.com/media/GA70za2XIAA01Bw.jpg","Artist 7"),
            FanArt("8", "https://pbs.twimg.com/media/GCc2N2EX0AAXz8y.jpg","Artist 8")
            // Add more fan art
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FanArtScreen(navController: NavController) {
    val viewModel: FanArtViewModel = viewModel()
    val fanArts by viewModel.fanArts.collectAsState()
    var selectedFanArt by remember { mutableStateOf<FanArt?>(null) }

    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(rememberTopAppBarState())

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeTopAppBar(
                title = { Text("Fan Art Gallery") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.largeTopAppBarColors(
                    containerColor = Color.Transparent, 
                    titleContentColor = MaterialTheme.colorScheme.onSurface, 
                    navigationIconContentColor = MaterialTheme.colorScheme.onSurface, 
                    actionIconContentColor = MaterialTheme.colorScheme.onSurface, 
                    scrolledContainerColor = MaterialTheme.colorScheme.surfaceVariant
                ),
                scrollBehavior = scrollBehavior
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { /* TODO: Handle Add Fan Art action */ },
                icon = { Icon(Icons.Filled.Add, contentDescription = "Add new fan art") },
                text = { Text("Add New Fan Art") },
                shape = RoundedCornerShape(16.dp) // Pill shape
            )
        }
    ) { paddingValues ->
        if (fanArts.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(paddingValues), contentAlignment = Alignment.Center) {
                Text("No fan art available yet!")
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                contentPadding = PaddingValues(
                    start = 16.dp, 
                    top = paddingValues.calculateTopPadding() + 16.dp, 
                    end = 16.dp, 
                    bottom = paddingValues.calculateBottomPadding() + 16.dp 
                ),
                modifier = Modifier.fillMaxSize(), 
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(fanArts) { fanArt ->
                    FanArtListItem(fanArt = fanArt, onFanArtClick = { selectedFanArt = it })
                }
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
                indication = ripple(),
                onClick = { onFanArtClick(fanArt) }
            ),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp),
        shape = MaterialTheme.shapes.medium
    ) {
        Column {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(fanArt.imageUrl)
                    .crossfade(true)
                    .build(),
                contentDescription = "Fan art by ${fanArt.artistName}",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f),
                error = painterResource(id = R.drawable.ic_launcher_background)
            )
            Text(
                text = fanArt.artistName,
                style = MaterialTheme.typography.labelSmall, 
                modifier = Modifier.padding(8.dp).align(Alignment.CenterHorizontally)
            )
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
                    indication = null, 
                    onClick = onDismiss
                ),
            contentAlignment = Alignment.Center
        ) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(fanArt.imageUrl)
                    .crossfade(true)
                    .build(),
                contentDescription = "Full screen fan art by ${fanArt.artistName}",
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp), 
                error = painterResource(id = R.drawable.ic_launcher_background)
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
