package com.kawaii.meowbah

import android.content.Context
import android.content.Intent
import android.media.MediaPlayer
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
// import androidx.compose.foundation.shape.RoundedCornerShape // No longer needed for Nav Bar
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Settings
// import androidx.compose.material.icons.filled.Favorite // Not used
import androidx.compose.material.icons.filled.Videocam
// import androidx.compose.material.icons.outlined.FavoriteBorder // Not used
import androidx.compose.material.icons.outlined.Videocam
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
// import androidx.compose.ui.draw.clip // No longer needed for Nav Bar
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.kawaii.meowbah.data.AuthRepository
import com.kawaii.meowbah.data.TokenStorageService
import com.kawaii.meowbah.data.remote.AuthApiService
import com.kawaii.meowbah.ui.screens.SettingsScreen
import com.kawaii.meowbah.ui.screens.VideosScreen
import com.kawaii.meowbah.ui.screens.videodetail.VideoDetailScreen
import com.kawaii.meowbah.ui.screens.videos.VideosViewModel
import com.kawaii.meowbah.ui.theme.AvailableTheme
import com.kawaii.meowbah.ui.theme.MeowbahTheme
import com.kawaii.meowbah.ui.theme.allThemes
// import com.kawaii.meowbah.widgets.VideoWidgetViewsFactory // Widget import removed
import com.kawaii.meowbah.workers.RssSyncWorker
import com.kawaii.meowbah.workers.YoutubeSyncWorker
import java.util.concurrent.TimeUnit

sealed class BottomNavItem(
    val route: String,
    val label: String,
    val icon: ImageVector,
    val outlinedIcon: ImageVector
) {
    object Videos : BottomNavItem("videos_tab", "Videos", Icons.Filled.Videocam, Icons.Outlined.Videocam)
    object Settings : BottomNavItem("settings_tab", "Settings", Icons.Filled.Settings, Icons.Outlined.Settings)
}

val bottomNavItems = listOf(
    BottomNavItem.Videos,
    BottomNavItem.Settings
)

class MainActivity : ComponentActivity() {

    private val PREFS_NAME = "MeowbahAppPreferences"
    private val KEY_SELECTED_THEME = "selectedTheme"
    private val KEY_LOGIN_MUSIC_ENABLED = "loginMusicEnabled"
    // private val KEY_PENDING_VIDEO_ID = "pendingVideoId" // Widget related key removed
    private val TAG = "MainActivity"
    
    private lateinit var onLoginSuccessState: () -> Unit 

    private lateinit var authApiService: AuthApiService
    private lateinit var authRepository: AuthRepository 
    private lateinit var tokenStorageService: TokenStorageService

    private fun saveThemePreference(theme: AvailableTheme) {
        val sharedPrefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        with(sharedPrefs.edit()) {
            putString(KEY_SELECTED_THEME, theme.displayName)
            apply()
        }
    }

    fun loadThemePreference(): AvailableTheme {
        val sharedPrefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val themeName = sharedPrefs.getString(KEY_SELECTED_THEME, null)
        return allThemes.firstOrNull { it.displayName == themeName } ?: AvailableTheme.Pink
    }

    private fun saveLoginMusicPreference(enabled: Boolean) {
        val sharedPrefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        with(sharedPrefs.edit()) {
            putBoolean(KEY_LOGIN_MUSIC_ENABLED, enabled)
            apply()
        }
    }

    private fun loadLoginMusicPreference(): Boolean {
        val sharedPrefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return sharedPrefs.getBoolean(KEY_LOGIN_MUSIC_ENABLED, true)
    }

    // Widget related intent handling removed
    // private fun handleWidgetIntent(intent: Intent?) { ... }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        setIntent(intent)
        // handleWidgetIntent(intent) // Widget related call removed
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // handleWidgetIntent(intent) // Widget related call removed

        tokenStorageService = TokenStorageService(applicationContext)
        authApiService = AuthApiService.create()
        authRepository = AuthRepository(authApiService) 

        scheduleYoutubeSync()

        setContent {
            var currentAppTheme by remember { mutableStateOf(loadThemePreference()) }
            val onThemeChange: (AvailableTheme) -> Unit = remember { {
                newTheme ->
                    saveThemePreference(newTheme)
                    currentAppTheme = newTheme
            } }

            var isLoggedIn by rememberSaveable { mutableStateOf(false) } 
            onLoginSuccessState = remember { { isLoggedIn = true } } 
            
            var selectedTabRoute by rememberSaveable { mutableStateOf(BottomNavItem.Videos.route) }
            val onSelectedTabRouteChange: (String) -> Unit = remember { { newRoute ->
                selectedTabRoute = newRoute
            } }

            var isLoginMusicEnabled by rememberSaveable { mutableStateOf(loadLoginMusicPreference()) }
            val onLoginMusicEnabledChange: (Boolean) -> Unit = remember { { enabled ->
                saveLoginMusicPreference(enabled)
                isLoginMusicEnabled = enabled
            } }

            LaunchedEffect(Unit) {
                if (tokenStorageService.isAccessTokenValid()) {
                    Log.d(TAG, "Valid access token found. Setting isLoggedIn to true.")
                    isLoggedIn = true
                } else {
                    Log.d(TAG, "No valid access token. Setting isLoggedIn to false.")
                    isLoggedIn = false
                }
            }

            MeowbahTheme(currentSelectedTheme = currentAppTheme) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                ) {
                    AppNavigation(
                        currentAppTheme = currentAppTheme,
                        onThemeChange = onThemeChange,
                        isLoggedIn = isLoggedIn, 
                        selectedTabRoute = selectedTabRoute,
                        onSelectedTabRouteChange = onSelectedTabRouteChange,
                        isLoginMusicEnabled = isLoginMusicEnabled,
                        onLoginMusicEnabledChange = onLoginMusicEnabledChange,
                        getPendingVideoId = { 
                            // Original logic for widget pending video ID removed.
                            // Return null as there's no widget setting this value anymore.
                            null
                        }
                    )
                }
            }
        }
    }

    private fun scheduleYoutubeSync() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val periodicSyncRequest = PeriodicWorkRequestBuilder<YoutubeSyncWorker>(
            12, TimeUnit.HOURS
        )
            .setConstraints(constraints)
            .build()

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            YoutubeSyncWorker.WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            periodicSyncRequest
        )
        Log.i(TAG, "YoutubeSyncWorker scheduled.")
    }

    fun scheduleRssSyncWorker() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val periodicRssSyncRequest = PeriodicWorkRequestBuilder<RssSyncWorker>(
            6, TimeUnit.HOURS 
        )
            .setConstraints(constraints)
            .build()

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            RssSyncWorker.WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP, 
            periodicRssSyncRequest
        )
        Log.i(TAG, "RssSyncWorker scheduled to run every 6 hours.")
    }

    fun cancelRssSyncWorker() {
        WorkManager.getInstance(this).cancelUniqueWork(RssSyncWorker.WORK_NAME)
        Log.i(TAG, "RssSyncWorker cancelled.")
    }
}

@Composable
fun AppNavigation(
    currentAppTheme: AvailableTheme,
    onThemeChange: (AvailableTheme) -> Unit,
    isLoggedIn: Boolean,
    selectedTabRoute: String,
    onSelectedTabRouteChange: (String) -> Unit,
    isLoginMusicEnabled: Boolean,
    onLoginMusicEnabledChange: (Boolean) -> Unit,
    getPendingVideoId: () -> String?
) {
    val navController = rememberNavController()

    LaunchedEffect(isLoggedIn, navController) {
        val currentRoute = navController.currentDestination?.route
        if (currentRoute != "main_screen") {
            navController.navigate("main_screen") {
                popUpTo(navController.graph.id) { inclusive = true } 
                launchSingleTop = true
            }
        }
    }
    
    LaunchedEffect(Unit, isLoggedIn) {
        if (isLoggedIn) { 
            val pendingVideoId = getPendingVideoId()
            if (pendingVideoId != null) {
                // This log might still appear if getPendingVideoId is called, 
                // but it will always be null now unless re-purposed.
                Log.d("AppNavigation", "Pending video ID found: $pendingVideoId. Navigation handled by MainScreen.")
            }
        }
    }

    NavHost(navController = navController, startDestination = "main_screen" ) {
        composable("main_screen") {
            MainScreen(
                currentAppTheme = currentAppTheme,
                onThemeChange = onThemeChange,
                mainNavController = navController, 
                selectedTabRoute = selectedTabRoute,
                onSelectedTabRouteChange = onSelectedTabRouteChange,
                isLoginMusicEnabled = isLoginMusicEnabled,
                onLoginMusicEnabledChange = onLoginMusicEnabledChange,
                getPendingVideoId = getPendingVideoId
            )
        }
    }
}

@Composable
fun MainScreen(
    currentAppTheme: AvailableTheme,
    onThemeChange: (AvailableTheme) -> Unit,
    mainNavController: NavController, 
    selectedTabRoute: String,        
    onSelectedTabRouteChange: (String) -> Unit,
    isLoginMusicEnabled: Boolean,
    onLoginMusicEnabledChange: (Boolean) -> Unit,
    getPendingVideoId: () -> String?
) {
    val innerNavController = rememberNavController()
    val videosViewModel: VideosViewModel = viewModel()

    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var mediaPlayer by remember { mutableStateOf<MediaPlayer?>(null) }

    val navBackStackEntry by innerNavController.currentBackStackEntryAsState()
    val currentInnerDestination = navBackStackEntry?.destination

    val showBottomBar = remember(currentInnerDestination) {
        currentInnerDestination?.route?.startsWith("video_detail/") == false
    }

    LaunchedEffect(Unit, innerNavController) {
        val videoId = getPendingVideoId()
        if (videoId != null) {
            Log.d("MainScreen", "Pending video ID $videoId found. Navigating on innerNavController.")
            if (innerNavController.currentDestination?.route?.startsWith("video_detail/") == false &&
                selectedTabRoute != BottomNavItem.Videos.route) {
                 innerNavController.navigate(BottomNavItem.Videos.route) {
                    popUpTo(innerNavController.graph.findStartDestination().id) { saveState = true }
                    launchSingleTop = true
                    restoreState = true
                }
                 onSelectedTabRouteChange(BottomNavItem.Videos.route)
            }
            innerNavController.navigate("video_detail/$videoId")
        }
    }

    LaunchedEffect(isLoginMusicEnabled, context) {
        if (isLoginMusicEnabled) {
            var playerInstance = mediaPlayer
            if (playerInstance == null) {
                Log.d("MainScreenMusic", "MediaPlayer is null, creating.")
                playerInstance = MediaPlayer.create(context, R.raw.madoka_music)
                playerInstance?.isLooping = true
                mediaPlayer = playerInstance
            }
            playerInstance?.let { p ->
                if (!p.isPlaying) {
                    try {
                        Log.d("MainScreenMusic", "Attempting to start MediaPlayer.")
                        p.start()
                    } catch (e: IllegalStateException) {
                        Log.e("MainScreenMusic", "Error starting MediaPlayer: ${e.message}. Releasing and re-creating.")
                        try { p.release() } catch (re: Exception) { Log.e("MainScreenMusic", "Error releasing faulty player: ${re.message}") }
                        mediaPlayer = MediaPlayer.create(context, R.raw.madoka_music)?.apply {
                            isLooping = true
                            try { start(); Log.d("MainScreenMusic", "Successfully started after re-creation.") }
                            catch (e2: IllegalStateException) { Log.e("MainScreenMusic", "Error starting MediaPlayer on second attempt: ${e2.message}") }
                        }
                    }
                }
            }
        } else {
            mediaPlayer?.let {
                if (it.isPlaying) {
                    Log.d("MainScreenMusic", "Pausing MediaPlayer because isLoginMusicEnabled is false.")
                    try { it.pause() } catch (e: IllegalStateException) { Log.e("MainScreenMusic", "Error pausing MediaPlayer: ${e.message}") }
                }
            }
        }
    }

    DisposableEffect(lifecycleOwner) {
        val lifecycleObserver = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_PAUSE -> {
                    if (isLoginMusicEnabled && mediaPlayer?.isPlaying == true) {
                        Log.d("MainScreenMusic", "Pausing MediaPlayer on ON_PAUSE")
                        try { mediaPlayer?.pause() } catch (e: IllegalStateException) { Log.e("MainScreenMusic", "Error pausing MediaPlayer in ON_PAUSE: ${e.message}") }
                    }
                }
                Lifecycle.Event.ON_RESUME -> {
                    if (isLoginMusicEnabled && mediaPlayer != null && mediaPlayer?.isPlaying == false) {
                        try {
                            Log.d("MainScreenMusic", "Starting MediaPlayer on ON_RESUME")
                            mediaPlayer?.start() 
                        } catch (e: IllegalStateException) { Log.e("MainScreenMusic", "Error starting MediaPlayer on ON_RESUME: ${e.message}") }
                    }
                }
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(lifecycleObserver)
        onDispose {
            Log.d("MainScreenMusic", "Disposing MediaPlayer in MainScreen")
            lifecycleOwner.lifecycle.removeObserver(lifecycleObserver)
            mediaPlayer?.let {
                try { if (it.isPlaying) { it.stop() }; it.release() }
                catch (e: Exception) { Log.e("MainScreenMusic", "Error stopping/releasing MediaPlayer in onDispose: ${e.message}") }
            }
            mediaPlayer = null
        }
    }

    Scaffold(
        bottomBar = {
            AnimatedVisibility(
                visible = showBottomBar,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                NavigationBar(
                    // Modifiers for padding and clip removed to make it non-floating
                ) {
                    // val navBackStackEntry by innerNavController.currentBackStackEntryAsState() // Already defined above
                    // val currentInnerDestination = navBackStackEntry?.destination // Already defined above
                    bottomNavItems.forEach { screen ->
                        NavigationBarItem(
                            selected = currentInnerDestination?.hierarchy?.any { it.route == screen.route } == true,
                            onClick = {
                                if (currentInnerDestination?.route != screen.route) {
                                    innerNavController.navigate(screen.route) {
                                        popUpTo(innerNavController.graph.findStartDestination().id) { saveState = true }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                    onSelectedTabRouteChange(screen.route)
                                }
                            },
                            icon = {
                                val iconVector = if (currentInnerDestination?.hierarchy?.any { it.route == screen.route } == true) screen.icon else screen.outlinedIcon
                                Icon(imageVector = iconVector, contentDescription = screen.label)
                            },
                            label = { Text(screen.label) }
                        )
                    }
                }
            }
        }
    ) { paddingValues ->
        NavHost(
            navController = innerNavController,
            startDestination = selectedTabRoute, 
            modifier = Modifier.padding(paddingValues).fillMaxSize()
        ) {
            composable(BottomNavItem.Videos.route) {
                VideosScreen(navController = innerNavController, viewModel = videosViewModel)
            }
            composable(BottomNavItem.Settings.route) {
                SettingsScreen(
                    navController = innerNavController,
                    currentAppTheme = currentAppTheme,
                    onThemeChange = onThemeChange,
                    isLoginMusicEnabled = isLoginMusicEnabled,
                    onLoginMusicEnabledChange = onLoginMusicEnabledChange
                )
            }
            composable(
                route = "video_detail/{videoId}",
                arguments = listOf(navArgument("videoId") { type = NavType.StringType })
            ) { backStackEntry ->
                val videoId = backStackEntry.arguments?.getString("videoId")
                if (videoId != null) {
                    VideoDetailScreen(
                        navController = innerNavController,
                        videoId = videoId,
                        videosViewModel = videosViewModel
                    )
                } else {
                    Log.e("AppNavigation", "Error: videoId was null for VideoDetailScreen.")
                    Text("Error loading video details. Video ID missing.") 
                }
            }
        }
    }
}
