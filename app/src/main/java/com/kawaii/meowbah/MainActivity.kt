package com.kawaii.meowbah

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.media.MediaPlayer // Added for MainScreen music
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Videocam
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect // Added for MainScreen music
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext // Added for MainScreen music
import androidx.compose.ui.platform.LocalLifecycleOwner // Added for MainScreen music
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle // Added for MainScreen music
import androidx.lifecycle.LifecycleEventObserver // Added for MainScreen music
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewmodel.compose.viewModel // For creating ViewModel in MainScreen
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
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.Scope
import com.google.android.gms.tasks.Task
import com.kawaii.meowbah.data.AuthRepository
import com.kawaii.meowbah.data.TokenStorageService
import com.kawaii.meowbah.data.remote.AuthApiService
import com.kawaii.meowbah.ui.screens.FanArtScreen
import com.kawaii.meowbah.ui.screens.LoginScreen
import com.kawaii.meowbah.ui.screens.ProfileScreen
import com.kawaii.meowbah.ui.screens.VideosScreen
import com.kawaii.meowbah.ui.screens.videodetail.VideoDetailScreen
import com.kawaii.meowbah.ui.screens.videos.VideosViewModel // Added for ViewModel creation
import com.kawaii.meowbah.ui.theme.AvailableTheme
import com.kawaii.meowbah.ui.theme.MeowbahTheme
import com.kawaii.meowbah.ui.theme.allThemes
import com.kawaii.meowbah.widgets.VideoWidgetViewsFactory // For intent action constants
import com.kawaii.meowbah.workers.YoutubeSyncWorker
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

sealed class BottomNavItem(
    val route: String,
    val label: String,
    val icon: ImageVector,
    val outlinedIcon: ImageVector
) {
    object Videos : BottomNavItem("videos_tab", "Videos", Icons.Filled.Videocam, Icons.Outlined.Videocam)
    object FanArt : BottomNavItem("fan_art_tab", "Fan Art", Icons.Filled.Favorite, Icons.Outlined.FavoriteBorder)
    object Profile : BottomNavItem("profile_tab", "Profile", Icons.Filled.AccountCircle, Icons.Outlined.AccountCircle)
}

val bottomNavItems = listOf(
    BottomNavItem.Videos,
    BottomNavItem.FanArt,
    BottomNavItem.Profile
)

class MainActivity : ComponentActivity() {

    private val PREFS_NAME = "MeowbahAppPreferences"
    private val KEY_SELECTED_THEME = "selectedTheme"
    private val KEY_LOGIN_MUSIC_ENABLED = "loginMusicEnabled"
    private val KEY_PENDING_VIDEO_ID = "pendingVideoId" // For widget click
    private val TAG = "MainActivityOAuth"

    private lateinit var googleSignInClient: GoogleSignInClient
    private lateinit var googleSignInLauncher: ActivityResultLauncher<Intent>
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
        return sharedPrefs.getBoolean(KEY_LOGIN_MUSIC_ENABLED, true) // Default to true
    }

    private fun startGoogleSignIn() {
        val signInIntent = googleSignInClient.signInIntent
        googleSignInLauncher.launch(signInIntent)
    }

    private fun handleGoogleSignInResult(task: Task<GoogleSignInAccount>) {
        try {
            val account = task.getResult(ApiException::class.java)
            Log.d(TAG, "Google Sign-In successful. Name: ${account?.displayName}, Email: ${account?.email}")
            val serverAuthCode = account?.serverAuthCode
            if (serverAuthCode != null && serverAuthCode.isNotBlank()) {
                Log.d(TAG, "Server Auth Code received (first 10 chars): ${serverAuthCode.take(10)}...")
                lifecycleScope.launch {
                    val tokenResult = authRepository.exchangeCodeForTokens(serverAuthCode)
                    tokenResult.fold(
                        onSuccess = {
                            tokenResponse ->
                            tokenStorageService.saveTokens(tokenResponse)
                            Log.i(TAG, "OAuth tokens successfully exchanged and saved.")
                            if (::onLoginSuccessState.isInitialized) {
                                onLoginSuccessState() // Update UI / navigation
                            }
                        },
                        onFailure = {
                            exception ->
                            Log.e(TAG, "Failed to exchange auth code for tokens or save tokens.", exception)
                        }
                    )
                }
            } else {
                Log.e(TAG, "Server auth code is null or blank after Google Sign-In.")
            }
        } catch (e: ApiException) {
            Log.w(TAG, "Google Sign-In failed with ApiException: code=${e.statusCode} message=${e.message}")
        } catch (e: Exception) {
            Log.e(TAG, "An unexpected error occurred during Google Sign-In handling.", e)
        }
    }

    private fun handleWidgetIntent(intent: Intent?) {
        if (intent?.action == VideoWidgetViewsFactory.ACTION_VIEW_VIDEO) {
            val videoId = intent.getStringExtra(VideoWidgetViewsFactory.EXTRA_VIDEO_ID)
            if (videoId != null) {
                Log.d(TAG, "Widget requested to view video ID: $videoId. Storing pending ID.")
                // Store this ID, AppNavigation will pick it up.
                val sharedPrefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                with(sharedPrefs.edit()) {
                    putString(KEY_PENDING_VIDEO_ID, videoId)
                    apply()
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent?) { // Handle intent if activity is already running
        super.onNewIntent(intent)
        setIntent(intent) // Important to update the activity's intent
        handleWidgetIntent(intent)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        handleWidgetIntent(intent) // Handle intent if activity is launched fresh

        tokenStorageService = TokenStorageService(applicationContext)
        authApiService = AuthApiService.create()
        authRepository = AuthRepository(authApiService)

        scheduleYoutubeSync()

        googleSignInLauncher = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
                handleGoogleSignInResult(task)
            } else {
                Log.w(TAG, "Google Sign-In cancelled by user or failed. Result Code: ${result.resultCode}")
            }
        }

        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestServerAuthCode("691684333330-k4a1q7tq2cbfm023mal00h1h1ffd6g2q.apps.googleusercontent.com")
            .requestEmail()
            .requestProfile()
            .requestScopes(Scope("https.www.googleapis.com/auth/youtube.readonly"))
            .build()

        googleSignInClient = GoogleSignIn.getClient(this, gso)

        setContent {
            var currentAppTheme by remember { mutableStateOf(loadThemePreference()) }
            val onThemeChange: (AvailableTheme) -> Unit = remember { {
                newTheme ->
                    saveThemePreference(newTheme)
                    currentAppTheme = newTheme
            } }

            var isLoggedIn by rememberSaveable { mutableStateOf(false) }
            onLoginSuccessState = remember { { isLoggedIn = true } }
            val onLogout: () -> Unit = remember {
                {
                    tokenStorageService.clearTokens()
                    isLoggedIn = false
                }
            }

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
                    Log.d(TAG, "Valid access token found. Setting user as logged in.")
                    isLoggedIn = true
                } else {
                    Log.d(TAG, "No valid access token. User is logged out.")
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
                        onLoginSuccess = onLoginSuccessState,
                        onLogout = onLogout,
                        selectedTabRoute = selectedTabRoute,
                        onSelectedTabRouteChange = onSelectedTabRouteChange,
                        onGoogleSignInRequested = ::startGoogleSignIn,
                        isLoginMusicEnabled = isLoginMusicEnabled,
                        onLoginMusicEnabledChange = onLoginMusicEnabledChange,
                        getPendingVideoId = { // Pass a lambda to get and clear the ID
                            val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                            val videoId = prefs.getString(KEY_PENDING_VIDEO_ID, null)
                            if (videoId != null) {
                                prefs.edit().remove(KEY_PENDING_VIDEO_ID).apply()
                            }
                            videoId
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
    }
}

@Composable
fun AppNavigation(
    currentAppTheme: AvailableTheme,
    onThemeChange: (AvailableTheme) -> Unit,
    isLoggedIn: Boolean,
    onLoginSuccess: () -> Unit,
    onLogout: () -> Unit,
    selectedTabRoute: String,
    onSelectedTabRouteChange: (String) -> Unit,
    onGoogleSignInRequested: () -> Unit,
    isLoginMusicEnabled: Boolean,
    onLoginMusicEnabledChange: (Boolean) -> Unit,
    getPendingVideoId: () -> String? // Lambda to retrieve pending video ID
) {
    val navController = rememberNavController()

    LaunchedEffect(isLoggedIn, navController) {
        val currentRoute = navController.currentDestination?.route
        if (isLoggedIn) {
            if (currentRoute == "login" || currentRoute == null) {
                navController.navigate("main_screen") {
                    popUpTo("login") { inclusive = true }
                    launchSingleTop = true
                }
            }
        } else {
            if (currentRoute != "login") {
                 navController.navigate("login") {
                    popUpTo(navController.graph.id) { inclusive = true }
                    launchSingleTop = true
                }
            }
        }
    }
    
    // Handle pending video ID for widget navigation after login/main_screen is ready
    LaunchedEffect(Unit, isLoggedIn) { // Re-check if isLoggedIn changes or on initial composition
        if (isLoggedIn) { // Only attempt navigation if logged in and main_screen is presumably the target
            val pendingVideoId = getPendingVideoId() // Get and clear the ID
            if (pendingVideoId != null) {
                Log.d("AppNavigation", "Pending video ID found: $pendingVideoId. Navigating from AppNav.")
                // We need to navigate on the innerNavController inside MainScreen.
                // This requires a different approach. For now, this AppNavigation NavController
                // cannot directly control the inner one. This will be handled in MainScreen's LaunchedEffect.
            }
        }
    }

    NavHost(navController = navController, startDestination = if (isLoggedIn) "main_screen" else "login" ) {
        composable("login") {
            if (!isLoggedIn) {
                LoginScreen(
                    onLoginClicked = { _, _ -> /* TODO: Implement standard login if needed */ },
                    onSignUpClicked = { /* TODO */ },
                    onForgotPasswordClicked = { /* TODO */ },
                    onGuestLoginClicked = { onLoginSuccess() },
                    onGoogleSignInClicked = onGoogleSignInRequested,
                    isLoginMusicEnabled = isLoginMusicEnabled,
                    onLoginMusicEnabledChange = onLoginMusicEnabledChange
                )
            }
        }
        composable("main_screen") {
            if (isLoggedIn) {
                MainScreen(
                    currentAppTheme = currentAppTheme,
                    onThemeChange = onThemeChange,
                    onLogout = onLogout,
                    mainNavController = navController, 
                    selectedTabRoute = selectedTabRoute,
                    onSelectedTabRouteChange = onSelectedTabRouteChange,
                    isLoginMusicEnabled = isLoginMusicEnabled,
                    onLoginMusicEnabledChange = onLoginMusicEnabledChange,
                    getPendingVideoId = getPendingVideoId // Pass it down
                )
            }
        }
    }
}

@Composable
fun MainScreen(
    currentAppTheme: AvailableTheme,
    onThemeChange: (AvailableTheme) -> Unit,
    onLogout: () -> Unit,
    mainNavController: NavController, 
    selectedTabRoute: String,        
    onSelectedTabRouteChange: (String) -> Unit,
    isLoginMusicEnabled: Boolean,
    onLoginMusicEnabledChange: (Boolean) -> Unit,
    getPendingVideoId: () -> String? // Receive lambda
) {
    val innerNavController = rememberNavController()
    val videosViewModel: VideosViewModel = viewModel() // Create ViewModel instance here

    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var mediaPlayer by remember { mutableStateOf<MediaPlayer?>(null) }

    // Handle widget navigation once innerNavController is available
    LaunchedEffect(Unit, innerNavController) {
        val videoId = getPendingVideoId() // Get and clear the ID
        if (videoId != null) {
            Log.d("MainScreen", "Pending video ID $videoId found. Navigating on innerNavController.")
            // Ensure Videos tab is selected before navigating to detail for better UX
            if (innerNavController.currentDestination?.route?.startsWith("video_detail/") == false &&
                selectedTabRoute != BottomNavItem.Videos.route) {
                 innerNavController.navigate(BottomNavItem.Videos.route) {
                    popUpTo(innerNavController.graph.findStartDestination().id) { saveState = true }
                    launchSingleTop = true
                    restoreState = true
                }
                 onSelectedTabRouteChange(BottomNavItem.Videos.route) // Update the selected tab state
            }
            innerNavController.navigate("video_detail/$videoId")
        }
    }

    LaunchedEffect(isLoginMusicEnabled, context) {
        // ... (music logic remains the same)
        if (isLoginMusicEnabled) {
            var playerInstance = mediaPlayer
            if (playerInstance == null) {
                Log.d("MainScreenMusic", "MediaPlayer is null, creating.")
                playerInstance = MediaPlayer.create(context, R.raw.madoka_music)
                playerInstance?.isLooping = true
                mediaPlayer = playerInstance // Assign back to the state
            }

            playerInstance?.let { p ->
                if (!p.isPlaying) {
                    try {
                        Log.d("MainScreenMusic", "Attempting to start MediaPlayer.")
                        p.start()
                    } catch (e: IllegalStateException) {
                        Log.e("MainScreenMusic", "Error starting MediaPlayer: ${e.message}. Releasing and re-creating.")
                        try {
                            p.release() // Release the faulty player
                        } catch (re: Exception) {
                            Log.e("MainScreenMusic", "Error releasing faulty player: ${re.message}")
                        }
                        mediaPlayer = MediaPlayer.create(context, R.raw.madoka_music)?.apply {
                            isLooping = true
                            try {
                                start()
                                Log.d("MainScreenMusic", "Successfully started after re-creation.")
                            } catch (e2: IllegalStateException) {
                                Log.e("MainScreenMusic", "Error starting MediaPlayer on second attempt: ${e2.message}")
                            }
                        }
                    }
                }
            }
        } else { // isLoginMusicEnabled is false
            mediaPlayer?.let { player ->
                if (player.isPlaying) {
                    Log.d("MainScreenMusic", "Pausing MediaPlayer because isLoginMusicEnabled is false.")
                    try {
                        player.pause()
                    } catch (e: IllegalStateException) {
                        Log.e("MainScreenMusic", "Error pausing MediaPlayer: ${e.message}")
                    }
                }
            }
        }
    }

    DisposableEffect(lifecycleOwner) {
        // ... (music logic remains the same)
        val lifecycleObserver = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_PAUSE -> {
                    if (isLoginMusicEnabled && mediaPlayer?.isPlaying == true) {
                        Log.d("MainScreenMusic", "Pausing MediaPlayer on ON_PAUSE")
                        try {
                            mediaPlayer?.pause()
                        } catch (e: IllegalStateException) {
                            Log.e("MainScreenMusic", "Error pausing MediaPlayer in ON_PAUSE: ${e.message}")
                        }
                    }
                }
                Lifecycle.Event.ON_RESUME -> {
                    if (isLoginMusicEnabled && mediaPlayer != null && mediaPlayer?.isPlaying == false) {
                        try {
                            Log.d("MainScreenMusic", "Starting MediaPlayer on ON_RESUME")
                            mediaPlayer?.start()
                        } catch (e: IllegalStateException) {
                            Log.e("MainScreenMusic", "Error starting MediaPlayer on ON_RESUME: ${e.message}")
                        }
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
                try {
                    if (it.isPlaying) {
                        it.stop()
                    }
                    it.release()
                } catch (e: Exception) {
                    Log.e("MainScreenMusic", "Error stopping/releasing MediaPlayer in onDispose: ${e.message}")
                }
            }
            mediaPlayer = null
        }
    }

    Scaffold(
        bottomBar = {
            NavigationBar(
                modifier = Modifier
                    .padding(start = 16.dp, end = 16.dp, bottom = 16.dp)
                    .clip(RoundedCornerShape(16.dp))
            ) {
                val navBackStackEntry by innerNavController.currentBackStackEntryAsState()
                val currentInnerDestination = navBackStackEntry?.destination
                bottomNavItems.forEach { screen ->
                    NavigationBarItem(
                        selected = currentInnerDestination?.hierarchy?.any { it.route == screen.route } == true,
                        onClick = {
                            if (currentInnerDestination?.route != screen.route) {
                                innerNavController.navigate(screen.route) {
                                    popUpTo(innerNavController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                                onSelectedTabRouteChange(screen.route) // Update selected tab state when bottom nav item clicked
                            }
                        },
                        icon = {
                            val iconVector = if (currentInnerDestination?.hierarchy?.any { it.route == screen.route } == true) {
                                screen.icon
                            } else {
                                screen.outlinedIcon
                            }
                            Icon(imageVector = iconVector, contentDescription = screen.label)
                        },
                        label = { Text(screen.label) }
                    )
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
                VideosScreen(navController = innerNavController, viewModel = videosViewModel) // Pass instance
            }
            composable(BottomNavItem.FanArt.route) {
                FanArtScreen(navController = innerNavController)
            }
            composable(BottomNavItem.Profile.route) {
                ProfileScreen(
                    navController = innerNavController,
                    currentAppTheme = currentAppTheme,
                    onThemeChange = onThemeChange,
                    onLogout = onLogout,
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
                        videosViewModel = videosViewModel // Pass instance
                    )
                } else {
                    Log.e("AppNavigation", "Error: videoId was null for VideoDetailScreen.")
                    Text("Error loading video details. Video ID missing.") 
                }
            }
        }
    }
}
