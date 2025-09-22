package com.kawaii.meowbah

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Notification // Required for Notification.VISIBILITY_PUBLIC
import android.content.Context
import android.content.Intent
import android.media.MediaPlayer
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
// import androidx.compose.foundation.layout.PaddingValues // No longer needed for the reverted version
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets // Ensure this import is present and used
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding // Ensure this is available
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Article
import androidx.compose.material.icons.filled.FormatPaint
import androidx.compose.material.icons.filled.FormatQuote
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.outlined.Article
import androidx.compose.material.icons.outlined.FormatPaint
import androidx.compose.material.icons.outlined.FormatQuote
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Storefront
import androidx.compose.material.icons.outlined.Videocam
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalIconButton // Added this import
import androidx.compose.material3.Icon
// import androidx.compose.material3.IconButton // No longer explicitly used, FilledTonalIconButton is used instead
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDrawerState
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
// import androidx.compose.ui.platform.LocalLayoutDirection // Likely no longer needed
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
import com.kawaii.meowbah.ui.dialogs.WelcomeDialog
import com.kawaii.meowbah.ui.notifications.MeowTalkAlarmReceiver
import com.kawaii.meowbah.ui.notifications.MeowTalkScheduler
import com.kawaii.meowbah.ui.screens.FanArtScreen
import com.kawaii.meowbah.ui.screens.SettingsScreen
import com.kawaii.meowbah.ui.screens.MeowTalkScreen
import com.kawaii.meowbah.ui.screens.PostsScreen
import com.kawaii.meowbah.ui.screens.VideosScreen
import com.kawaii.meowbah.ui.screens.merch.MerchDetailScreen
import com.kawaii.meowbah.ui.screens.merch.MerchScreen
import com.kawaii.meowbah.ui.screens.videodetail.VideoDetailScreen
import com.kawaii.meowbah.ui.screens.videos.VideosViewModel
import com.kawaii.meowbah.ui.theme.AvailableTheme
import com.kawaii.meowbah.ui.theme.MeowbahTheme
import com.kawaii.meowbah.ui.theme.allThemes
import com.kawaii.meowbah.workers.PostsSyncWorker
import com.kawaii.meowbah.workers.YoutubeSyncWorker
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.concurrent.TimeUnit

const val MEOWTALK_NOTIFICATION_CHANNEL_ID = "meowtalk_channel"
const val MEOWTALK_NOTIFICATION_CHANNEL_NAME = "MeowTalk Notifications"

sealed class BottomNavItem(
    val route: String,
    val label: String,
    val icon: ImageVector,
    val outlinedIcon: ImageVector
) {
    object Videos : BottomNavItem("videos_tab", "Videos", Icons.Filled.Videocam, Icons.Outlined.Videocam)
    object Art : BottomNavItem(route = "art_tab", label = "Art", Icons.Filled.FormatPaint, outlinedIcon = Icons.Outlined.FormatPaint)
    object Merch : BottomNavItem("merch_tab", "Merch", Icons.Filled.Storefront, Icons.Outlined.Storefront)
    object MeowTalk : BottomNavItem(route = "meowtalk_tab", label = "MeowTalk", Icons.Filled.FormatQuote, outlinedIcon = Icons.Outlined.FormatQuote)
    object Posts : BottomNavItem("posts_tab", "Posts", Icons.Filled.Article, Icons.Outlined.Article)
    object Settings : BottomNavItem("settings_tab", "Settings", Icons.Filled.Settings, Icons.Outlined.Settings)
}

val bottomNavItems = listOf(
    BottomNavItem.Videos,
    BottomNavItem.Art,
    BottomNavItem.Merch,
    BottomNavItem.MeowTalk,
    BottomNavItem.Posts,
    BottomNavItem.Settings
)

class MainActivity : ComponentActivity() {

    companion object {
        const val PREFS_NAME = "MeowbahAppPreferences"
        private const val KEY_SELECTED_THEME = "selectedTheme"
        private const val KEY_LOGIN_MUSIC_ENABLED = "loginMusicEnabled"
        private const val KEY_WELCOME_DIALOG_SHOWN = "welcomeDialogShown"
        const val KEY_MEOWTALK_ENABLED = "meowTalkEnabled"
        const val KEY_MEOWTALK_INTERVAL_MINUTES = "meowTalkIntervalMinutes"
        const val KEY_MEOWTALK_SCHEDULING_TYPE = "meowTalkSchedulingType"
        const val KEY_MEOWTALK_SPECIFIC_HOUR = "meowTalkSpecificHour"
        const val KEY_MEOWTALK_SPECIFIC_MINUTE = "meowTalkSpecificMinute"
        const val KEY_MEOWTALK_CURRENT_PHRASE = "meowTalkCurrentPhrase"
        const val KEY_VIDEO_NOTIFICATIONS_ENABLED = "videoNotificationsEnabled"
        private const val TAG = "MainActivity"
    }

    private var initialScreenRouteFromIntent by mutableStateOf<String?>(null)

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

    private fun saveMeowTalkEnabledPreference(enabled: Boolean) {
        val sharedPrefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        with(sharedPrefs.edit()) {
            putBoolean(KEY_MEOWTALK_ENABLED, enabled)
            apply()
        }
    }

    private fun loadMeowTalkEnabledPreference(): Boolean {
        val sharedPrefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return sharedPrefs.getBoolean(KEY_MEOWTALK_ENABLED, false)
    }

    private fun saveMeowTalkIntervalPreference(intervalMinutes: Int) {
        val sharedPrefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        with(sharedPrefs.edit()) {
            putInt(KEY_MEOWTALK_INTERVAL_MINUTES, intervalMinutes)
            apply()
        }
    }

    private fun loadMeowTalkIntervalPreference(): Int {
        val sharedPrefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return sharedPrefs.getInt(KEY_MEOWTALK_INTERVAL_MINUTES, 30)
    }

    private fun saveMeowTalkSchedulingTypePreference(type: String) {
        val sharedPrefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        with(sharedPrefs.edit()) {
            putString(KEY_MEOWTALK_SCHEDULING_TYPE, type)
            apply()
        }
    }

    private fun loadMeowTalkSchedulingTypePreference(): String {
        val sharedPrefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return sharedPrefs.getString(KEY_MEOWTALK_SCHEDULING_TYPE, "interval") ?: "interval"
    }

    private fun saveMeowTalkSpecificTimePreference(hour: Int, minute: Int) {
        val sharedPrefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        with(sharedPrefs.edit()) {
            putInt(KEY_MEOWTALK_SPECIFIC_HOUR, hour)
            putInt(KEY_MEOWTALK_SPECIFIC_MINUTE, minute)
            apply()
        }
    }

    private fun loadMeowTalkSpecificHourPreference(): Int {
        val sharedPrefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return sharedPrefs.getInt(KEY_MEOWTALK_SPECIFIC_HOUR, 12)
    }

    private fun loadMeowTalkSpecificMinutePreference(): Int {
        val sharedPrefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return sharedPrefs.getInt(KEY_MEOWTALK_SPECIFIC_MINUTE, 0)
    }
    
    private fun saveVideoNotificationsEnabledPreference(enabled: Boolean) {
        val sharedPrefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        with(sharedPrefs.edit()) {
            putBoolean(KEY_VIDEO_NOTIFICATIONS_ENABLED, enabled)
            apply()
        }
    }

    private fun loadVideoNotificationsEnabledPreference(): Boolean {
        val sharedPrefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return sharedPrefs.getBoolean(KEY_VIDEO_NOTIFICATIONS_ENABLED, true)
    }

    private fun createMeowTalkNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel(MEOWTALK_NOTIFICATION_CHANNEL_ID, MEOWTALK_NOTIFICATION_CHANNEL_NAME, importance).apply {
                description = "Notifications for random MeowTalk phrases"
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            }
            val notificationManager:
                NotificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
            Log.d(TAG, "MeowTalk Notification Channel created with public lockscreen visibility.")
        }
    }

    private fun handleIntentForNavigation(intent: Intent?) {
        intent?.getStringExtra(MeowTalkAlarmReceiver.EXTRA_NAVIGATE_TO_TAB)?.let { targetRoute ->
            if (targetRoute == MeowTalkAlarmReceiver.MEOWTALK_TAB_ROUTE) {
                Log.d(TAG, "Intent requests navigation to MeowTalk tab: $targetRoute")
                initialScreenRouteFromIntent = targetRoute
            }
        }
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        setIntent(intent) 
        handleIntentForNavigation(intent) 
    }

    @OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        handleIntentForNavigation(intent) 
        createMeowTalkNotificationChannel()
        scheduleYoutubeSync()
        schedulePostsSync()

        val sharedPrefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val welcomeDialogAlreadyShown = sharedPrefs.getBoolean(KEY_WELCOME_DIALOG_SHOWN, false)
        val meowTalkScheduler = MeowTalkScheduler(this)

        setContent {
            var currentAppTheme by remember { mutableStateOf(loadThemePreference()) }
            val onThemeChange: (AvailableTheme) -> Unit = remember { {
                newTheme ->
                    saveThemePreference(newTheme)
                    currentAppTheme = newTheme
            } }

            var selectedTabRoute by rememberSaveable { mutableStateOf(BottomNavItem.Videos.route) }
            val onSelectedTabRouteChange: (String) -> Unit = remember { { newRoute ->
                selectedTabRoute = newRoute
            } }

            LaunchedEffect(initialScreenRouteFromIntent) {
                initialScreenRouteFromIntent?.let {
                    route ->
                    Log.d(TAG, "LaunchedEffect: Navigating to initial tab from intent: $route")
                    onSelectedTabRouteChange(route)
                    initialScreenRouteFromIntent = null 
                }
            }

            var isLoginMusicEnabled by rememberSaveable { mutableStateOf(loadLoginMusicPreference()) }
            val onLoginMusicEnabledChange: (Boolean) -> Unit = remember { { enabled ->
                saveLoginMusicPreference(enabled)
                isLoginMusicEnabled = enabled
            } }

            var isMeowTalkEnabled by rememberSaveable { mutableStateOf(loadMeowTalkEnabledPreference()) }
            val onMeowTalkEnabledChange: (Boolean) -> Unit = remember { { enabled ->
                saveMeowTalkEnabledPreference(enabled)
                isMeowTalkEnabled = enabled
                if (enabled) {
                    meowTalkScheduler.schedule(loadMeowTalkIntervalPreference(), loadMeowTalkSchedulingTypePreference(), loadMeowTalkSpecificHourPreference(), loadMeowTalkSpecificMinutePreference())
                } else {
                    meowTalkScheduler.cancel()
                }
            } }

            var meowTalkIntervalMinutes by rememberSaveable { mutableStateOf(loadMeowTalkIntervalPreference()) }
            val onMeowTalkIntervalChange: (Int) -> Unit = remember { { minutes ->
                saveMeowTalkIntervalPreference(minutes)
                meowTalkIntervalMinutes = minutes
                if (isMeowTalkEnabled) meowTalkScheduler.schedule(minutes, loadMeowTalkSchedulingTypePreference(), loadMeowTalkSpecificHourPreference(), loadMeowTalkSpecificMinutePreference())
            } }

            var meowTalkSchedulingType by rememberSaveable { mutableStateOf(loadMeowTalkSchedulingTypePreference()) }
            val onMeowTalkSchedulingTypeChange: (String) -> Unit = remember { { type ->
                saveMeowTalkSchedulingTypePreference(type)
                meowTalkSchedulingType = type
                if (isMeowTalkEnabled) meowTalkScheduler.schedule(loadMeowTalkIntervalPreference(), type, loadMeowTalkSpecificHourPreference(), loadMeowTalkSpecificMinutePreference())
            } }

            var meowTalkSpecificHour by rememberSaveable { mutableStateOf(loadMeowTalkSpecificHourPreference()) }
            var meowTalkSpecificMinute by rememberSaveable { mutableStateOf(loadMeowTalkSpecificMinutePreference()) }
            val onMeowTalkSpecificTimeChange: (Int, Int) -> Unit = remember { { hour, minute ->
                saveMeowTalkSpecificTimePreference(hour, minute)
                meowTalkSpecificHour = hour
                meowTalkSpecificMinute = minute
                if (isMeowTalkEnabled) meowTalkScheduler.schedule(loadMeowTalkIntervalPreference(), loadMeowTalkSchedulingTypePreference(), hour, minute)
            } }
            
            var isVideoNotificationsEnabled by rememberSaveable { mutableStateOf(loadVideoNotificationsEnabledPreference()) }
            val onVideoNotificationsEnabledChange: (Boolean) -> Unit = remember { { enabled ->
                saveVideoNotificationsEnabledPreference(enabled)
                isVideoNotificationsEnabled = enabled
            } }

            var showWelcomeDialog by rememberSaveable { mutableStateOf(!welcomeDialogAlreadyShown) }
            val onWelcomeDialogDismissed: () -> Unit = {
                showWelcomeDialog = false
                with(sharedPrefs.edit()) {
                    putBoolean(KEY_WELCOME_DIALOG_SHOWN, true)
                    apply()
                }
            }

            MeowbahTheme(currentSelectedTheme = currentAppTheme) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    AppNavigation(
                        currentAppTheme = currentAppTheme,
                        onThemeChange = onThemeChange,
                        selectedTabRoute = selectedTabRoute,
                        onSelectedTabRouteChange = onSelectedTabRouteChange,
                        isLoginMusicEnabled = isLoginMusicEnabled,
                        onLoginMusicEnabledChange = onLoginMusicEnabledChange,
                        isMeowTalkEnabled = isMeowTalkEnabled,
                        onMeowTalkEnabledChange = onMeowTalkEnabledChange,
                        meowTalkIntervalMinutes = meowTalkIntervalMinutes,
                        onMeowTalkIntervalChange = onMeowTalkIntervalChange,
                        meowTalkSchedulingType = meowTalkSchedulingType,
                        onMeowTalkSchedulingTypeChange = onMeowTalkSchedulingTypeChange,
                        meowTalkSpecificHour = meowTalkSpecificHour,
                        meowTalkSpecificMinute = meowTalkSpecificMinute,
                        onMeowTalkSpecificTimeChange = onMeowTalkSpecificTimeChange,
                        isVideoNotificationsEnabled = isVideoNotificationsEnabled,
                        onVideoNotificationsEnabledChange = onVideoNotificationsEnabledChange,
                        getPendingVideoId = { getPendingVideoIdFromIntent(intent) },
                        showWelcomeDialog = showWelcomeDialog,
                        onWelcomeDialogDismissed = onWelcomeDialogDismissed
                    )
                }
            }
        }
    }

    private fun getPendingVideoIdFromIntent(intent: Intent?): String? {
        return if (intent?.action == Intent.ACTION_VIEW && intent.data?.host == "www.youtube.com") {
            intent.data?.getQueryParameter("v")
        } else {
            intent?.getStringExtra("com.kawaii.meowbah.EXTRA_VIDEO_ID")
        }
    }

    private fun scheduleYoutubeSync() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val periodicSyncRequest = PeriodicWorkRequestBuilder<YoutubeSyncWorker>(
            30, TimeUnit.MINUTES
        ).setConstraints(constraints).build()

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            YoutubeSyncWorker.WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            periodicSyncRequest
        )
        Log.i(TAG, "YoutubeSyncWorker (RSS) scheduled to run every 30 minutes.")
    }

    private fun schedulePostsSync() { 
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val periodicSyncRequest = PeriodicWorkRequestBuilder<PostsSyncWorker>(
            15, TimeUnit.MINUTES
        ).setConstraints(constraints).build()

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            PostsSyncWorker.WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            periodicSyncRequest
        )
        Log.i(TAG, "PostsSyncWorker (RSS) scheduled to run every 15 minutes.")
    }
}

@Composable
fun AppNavigation(
    currentAppTheme: AvailableTheme,
    onThemeChange: (AvailableTheme) -> Unit,
    selectedTabRoute: String,
    onSelectedTabRouteChange: (String) -> Unit,
    isLoginMusicEnabled: Boolean,
    onLoginMusicEnabledChange: (Boolean) -> Unit,
    isMeowTalkEnabled: Boolean,
    onMeowTalkEnabledChange: (Boolean) -> Unit,
    meowTalkIntervalMinutes: Int,
    onMeowTalkIntervalChange: (Int) -> Unit,
    meowTalkSchedulingType: String,
    onMeowTalkSchedulingTypeChange: (String) -> Unit,
    meowTalkSpecificHour: Int,
    meowTalkSpecificMinute: Int,
    onMeowTalkSpecificTimeChange: (Int, Int) -> Unit,
    isVideoNotificationsEnabled: Boolean, 
    onVideoNotificationsEnabledChange: (Boolean) -> Unit, 
    getPendingVideoId: () -> String?,
    showWelcomeDialog: Boolean,
    onWelcomeDialogDismissed: () -> Unit
) {
    val TAG = "AppNavigation"
    val navController = rememberNavController()

    LaunchedEffect(navController) {
        val currentRoute = navController.currentDestination?.route
        if (currentRoute != "main_screen") {
            navController.navigate("main_screen") {
                popUpTo(navController.graph.id) { inclusive = true }
                launchSingleTop = true
            }
        }
    }

    LaunchedEffect(Unit) {
        val pendingVideoId = getPendingVideoId()
        if (pendingVideoId != null) {
            Log.d(TAG, "Pending video ID found: $pendingVideoId. This will be handled by MainScreen.")
        }
    }

    NavHost(navController = navController, startDestination = "main_screen") {
        composable("main_screen") {
            MainScreen(
                currentAppTheme = currentAppTheme,
                onThemeChange = onThemeChange,
                mainNavController = navController,
                selectedTabRoute = selectedTabRoute,
                onSelectedTabRouteChange = onSelectedTabRouteChange,
                isLoginMusicEnabled = isLoginMusicEnabled,
                onLoginMusicEnabledChange = onLoginMusicEnabledChange,
                isMeowTalkEnabled = isMeowTalkEnabled,
                onMeowTalkEnabledChange = onMeowTalkEnabledChange,
                meowTalkIntervalMinutes = meowTalkIntervalMinutes,
                onMeowTalkIntervalChange = onMeowTalkIntervalChange,
                meowTalkSchedulingType = meowTalkSchedulingType,
                onMeowTalkSchedulingTypeChange = onMeowTalkSchedulingTypeChange,
                meowTalkSpecificHour = meowTalkSpecificHour,
                meowTalkSpecificMinute = meowTalkSpecificMinute,
                onMeowTalkSpecificTimeChange = onMeowTalkSpecificTimeChange,
                isVideoNotificationsEnabled = isVideoNotificationsEnabled, 
                onVideoNotificationsEnabledChange = onVideoNotificationsEnabledChange, 
                getPendingVideoId = getPendingVideoId
            )
        }
    }

    if (showWelcomeDialog) {
        WelcomeDialog(onDismissRequest = onWelcomeDialogDismissed)
    }
}


@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3WindowSizeClassApi::class)
@Composable
fun MainScreen(
    currentAppTheme: AvailableTheme,
    onThemeChange: (AvailableTheme) -> Unit,
    mainNavController: NavController, 
    selectedTabRoute: String,
    onSelectedTabRouteChange: (String) -> Unit,
    isLoginMusicEnabled: Boolean,
    onLoginMusicEnabledChange: (Boolean) -> Unit,
    isMeowTalkEnabled: Boolean,
    onMeowTalkEnabledChange: (Boolean) -> Unit,
    meowTalkIntervalMinutes: Int,
    onMeowTalkIntervalChange: (Int) -> Unit,
    meowTalkSchedulingType: String,
    onMeowTalkSchedulingTypeChange: (String) -> Unit,
    meowTalkSpecificHour: Int,
    meowTalkSpecificMinute: Int,
    onMeowTalkSpecificTimeChange: (Int, Int) -> Unit,
    isVideoNotificationsEnabled: Boolean, 
    onVideoNotificationsEnabledChange: (Boolean) -> Unit, 
    getPendingVideoId: () -> String?
) {
    val TAG = "MainScreen"
    val innerNavController = rememberNavController()
    val videosViewModel: VideosViewModel = viewModel()
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var mediaPlayer by remember { mutableStateOf<MediaPlayer?>(null) }

    val navBackStackEntry by innerNavController.currentBackStackEntryAsState()
    val currentInnerDestination = navBackStackEntry?.destination

    val showDrawerButton = remember(currentInnerDestination) { 
        currentInnerDestination?.route?.startsWith("video_detail/") == false &&
        currentInnerDestination?.route?.startsWith("merchDetail/") == false
    }

    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    LaunchedEffect(selectedTabRoute) {
        if (innerNavController.currentDestination?.route != selectedTabRoute) {
            innerNavController.navigate(selectedTabRoute) {
                popUpTo(innerNavController.graph.findStartDestination().id) { saveState = true }
                launchSingleTop = true
                restoreState = true
            }
        }
    }

    LaunchedEffect(Unit) {
        val videoId = getPendingVideoId()
        if (videoId != null) {
            Log.d(TAG, "Pending video ID $videoId found in MainScreen. Navigating on innerNavController.")
            if (selectedTabRoute != BottomNavItem.Videos.route) {
                onSelectedTabRouteChange(BottomNavItem.Videos.route) 
            }
            innerNavController.navigate("video_detail/$videoId") {
                popUpTo(innerNavController.graph.findStartDestination().id) {
                    saveState = true
                }
                launchSingleTop = true
            }
        }
    }

    LaunchedEffect(isLoginMusicEnabled, context) {
        if (isLoginMusicEnabled) {
            var playerInstance = mediaPlayer
            if (playerInstance == null) {
                Log.d(TAG, "MediaPlayer is null, creating.")
                playerInstance = MediaPlayer.create(context, R.raw.madoka_music)
                playerInstance?.isLooping = true
                mediaPlayer = playerInstance
            }
            playerInstance?.let { p ->
                if (!p.isPlaying) {
                    try {
                        Log.d(TAG, "Attempting to start MediaPlayer.")
                        p.start()
                    } catch (e: IllegalStateException) {
                        Log.e(TAG, "Error starting MediaPlayer: ${e.message}. Releasing and re-creating.")
                        try { p.release() } catch (re: Exception) { Log.e(TAG, "Error releasing faulty player: ${re.message}") }
                        mediaPlayer = MediaPlayer.create(context, R.raw.madoka_music)?.apply {
                            isLooping = true
                            try { start(); Log.d(TAG, "Successfully started after re-creation.") }
                            catch (e2: IllegalStateException) { Log.e(TAG, "Error starting MediaPlayer on second attempt: ${e2.message}") }
                        }
                    }
                }
            }
        } else {
            mediaPlayer?.let {
                if (it.isPlaying) {
                    Log.d(TAG, "Pausing MediaPlayer because isLoginMusicEnabled is false.")
                    try { it.pause() } catch (e: IllegalStateException) { Log.e(TAG, "Error pausing MediaPlayer: ${e.message}") }
                }
            }
        }
    }

    DisposableEffect(lifecycleOwner) {
        val lifecycleObserver = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_PAUSE -> {
                    if (isLoginMusicEnabled && mediaPlayer?.isPlaying == true) {
                        Log.d(TAG, "Pausing MediaPlayer on ON_PAUSE")
                        try { mediaPlayer?.pause() } catch (e: IllegalStateException) { Log.e(TAG, "Error pausing MediaPlayer in ON_PAUSE: ${e.message}") }
                    }
                }
                Lifecycle.Event.ON_RESUME -> {
                    if (isLoginMusicEnabled && mediaPlayer != null && mediaPlayer?.isPlaying == false) {
                        try {
                            Log.d(TAG, "Starting MediaPlayer on ON_RESUME")
                            mediaPlayer?.start()
                        } catch (e: IllegalStateException) { Log.e(TAG, "Error starting MediaPlayer on ON_RESUME: ${e.message}") }
                    }
                }
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(lifecycleObserver)
        onDispose {
            Log.d(TAG, "Disposing MediaPlayer in MainScreen")
            lifecycleOwner.lifecycle.removeObserver(lifecycleObserver)
            mediaPlayer?.let {
                try { if (it.isPlaying) { it.stop() }; it.release() }
                catch (e: Exception) { Log.e(TAG, "Error stopping/releasing MediaPlayer in onDispose: ${e.message}") }
            }
            mediaPlayer = null
        }
    }
    
    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet {
                Spacer(Modifier.height(12.dp))
                bottomNavItems.forEach { screen ->
                    val isSelected = currentInnerDestination?.hierarchy?.any { it.route == screen.route } == true || selectedTabRoute == screen.route
                    NavigationDrawerItem(
                        icon = { Icon(if (isSelected) screen.icon else screen.outlinedIcon, contentDescription = screen.label) },
                        label = { Text(screen.label) },
                        selected = isSelected,
                        onClick = {
                            scope.launch { drawerState.close() }
                            if (currentInnerDestination?.route != screen.route) {
                                onSelectedTabRouteChange(screen.route)
                            }
                        },
                        modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                    )
                }
            }
        }
    ) {
        Scaffold(contentWindowInsets = WindowInsets(0.dp)) { scaffoldPaddingValues -> // Modified this line
            // Reverted: Removed LocalLayoutDirection and conditional actualPadding logic
            Box(modifier = Modifier.fillMaxSize().padding(scaffoldPaddingValues)) { // Reverted to original padding
                NavHost(
                    navController = innerNavController,
                    startDestination = selectedTabRoute,
                    modifier = Modifier.fillMaxSize()
                ) {
                    composable(BottomNavItem.Videos.route) {
                        VideosScreen(navController = innerNavController, viewModel = videosViewModel)
                    }
                    composable(BottomNavItem.Art.route) {
                        FanArtScreen(navController = innerNavController)
                    }
                    composable(BottomNavItem.Merch.route) { 
                        MerchScreen(navController = innerNavController)
                    }
                    composable(BottomNavItem.MeowTalk.route) {
                        MeowTalkScreen(navController = innerNavController)
                    }
                    composable(BottomNavItem.Posts.route) { 
                        PostsScreen(navController = innerNavController)
                    }
                    composable(BottomNavItem.Settings.route) {
                        SettingsScreen(
                            navController = innerNavController,
                            currentAppTheme = currentAppTheme,
                            onThemeChange = onThemeChange,
                            isLoginMusicEnabled = isLoginMusicEnabled,
                            onLoginMusicEnabledChange = onLoginMusicEnabledChange,
                            isMeowTalkEnabled = isMeowTalkEnabled,
                            onMeowTalkEnabledChange = onMeowTalkEnabledChange,
                            meowTalkIntervalMinutes = meowTalkIntervalMinutes,
                            onMeowTalkIntervalChange = onMeowTalkIntervalChange,
                            meowTalkSchedulingType = meowTalkSchedulingType,
                            onMeowTalkSchedulingTypeChange = onMeowTalkSchedulingTypeChange,
                            meowTalkSpecificHour = meowTalkSpecificHour,
                            meowTalkSpecificMinute = meowTalkSpecificMinute,
                            onMeowTalkSpecificTimeChange = onMeowTalkSpecificTimeChange,
                            isVideoNotificationsEnabled = isVideoNotificationsEnabled, 
                            onVideoNotificationsEnabledChange = onVideoNotificationsEnabledChange 
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
                            Log.e(TAG, "Error: videoId was null for VideoDetailScreen.")
                            Text("Error loading video details. Video ID missing.")
                        }
                    }
                    composable(
                        route = "merchDetail/{merchId}", 
                        arguments = listOf(navArgument("merchId") { type = NavType.StringType })
                    ) { backStackEntry ->
                        val merchId = backStackEntry.arguments?.getString("merchId")
                        MerchDetailScreen(navController = innerNavController, merchId = merchId)
                    }
                }
                if (showDrawerButton) { 
                    FilledTonalIconButton(
                        onClick = { scope.launch { drawerState.open() } },
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .statusBarsPadding()
                            .padding(horizontal = 8.dp) // Adjusted padding
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Menu,
                            contentDescription = "Open navigation drawer"
                        )
                    }
                }
            }
        }
    }
}
