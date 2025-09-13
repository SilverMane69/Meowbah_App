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
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FormatPaint
import androidx.compose.material.icons.filled.FormatQuote
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.outlined.FormatPaint
import androidx.compose.material.icons.outlined.FormatQuote
import androidx.compose.material.icons.outlined.Videocam
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.NavDestination
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
import com.kawaii.meowbah.ui.screens.VideosScreen
import com.kawaii.meowbah.ui.screens.videodetail.VideoDetailScreen
import com.kawaii.meowbah.ui.screens.videos.VideosViewModel
import com.kawaii.meowbah.ui.theme.AvailableTheme
import com.kawaii.meowbah.ui.theme.MeowbahTheme
import com.kawaii.meowbah.ui.theme.allThemes
import com.kawaii.meowbah.workers.YoutubeSyncWorker
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
    object Settings : BottomNavItem("settings_tab", "Settings", Icons.Filled.Settings, Icons.Outlined.Settings)
    object Art : BottomNavItem(route = "art_tab", label = "Art", Icons.Filled.FormatPaint, outlinedIcon = Icons.Outlined.FormatPaint)
    object MeowTalk : BottomNavItem(route = "meowtalk_tab", label = "MeowTalk", Icons.Filled.FormatQuote, outlinedIcon = Icons.Outlined.FormatQuote)
}

val bottomNavItems = listOf(
    BottomNavItem.Videos,
    BottomNavItem.Art,
    BottomNavItem.MeowTalk,
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
        const val KEY_VIDEO_NOTIFICATIONS_ENABLED = "videoNotificationsEnabled" // New Key
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
    
    private fun saveVideoNotificationsEnabledPreference(enabled: Boolean) { // New function
        val sharedPrefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        with(sharedPrefs.edit()) {
            putBoolean(KEY_VIDEO_NOTIFICATIONS_ENABLED, enabled)
            apply()
        }
    }

    private fun loadVideoNotificationsEnabledPreference(): Boolean { // New function
        val sharedPrefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return sharedPrefs.getBoolean(KEY_VIDEO_NOTIFICATIONS_ENABLED, true) // Default to true
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
        // Video notification channel is created in NotificationUtils when first needed
        scheduleYoutubeSync()

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
            
            var isVideoNotificationsEnabled by rememberSaveable { mutableStateOf(loadVideoNotificationsEnabledPreference()) } // New state
            val onVideoNotificationsEnabledChange: (Boolean) -> Unit = remember { { enabled -> // New lambda
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
            val windowSizeClass = calculateWindowSizeClass(this@MainActivity)

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
                        isVideoNotificationsEnabled = isVideoNotificationsEnabled, // Pass new state
                        onVideoNotificationsEnabledChange = onVideoNotificationsEnabledChange, // Pass new lambda
                        getPendingVideoId = { getPendingVideoIdFromIntent(intent) },
                        showWelcomeDialog = showWelcomeDialog,
                        onWelcomeDialogDismissed = onWelcomeDialogDismissed,
                        windowSizeClass = windowSizeClass.widthSizeClass
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
    isVideoNotificationsEnabled: Boolean, // New parameter
    onVideoNotificationsEnabledChange: (Boolean) -> Unit, // New parameter
    getPendingVideoId: () -> String?,
    showWelcomeDialog: Boolean,
    onWelcomeDialogDismissed: () -> Unit,
    windowSizeClass: WindowWidthSizeClass
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
                isVideoNotificationsEnabled = isVideoNotificationsEnabled, // Pass down
                onVideoNotificationsEnabledChange = onVideoNotificationsEnabledChange, // Pass down
                getPendingVideoId = getPendingVideoId,
                windowSizeClass = windowSizeClass
            )
        }
    }

    if (showWelcomeDialog) {
        WelcomeDialog(onDismissRequest = onWelcomeDialogDismissed)
    }
}

@Composable
private fun AppNavigationRail(
    innerNavController: NavController,
    currentInnerDestination: NavDestination?,
    selectedTabRoute: String,
    onSelectedTabRouteChange: (String) -> Unit,
    items: List<BottomNavItem>,
    modifier: Modifier = Modifier
) {
    NavigationRail(modifier = modifier.fillMaxHeight()) {
        items.forEach { screen ->
            val isSelected = currentInnerDestination?.hierarchy?.any { it.route == screen.route } == true || selectedTabRoute == screen.route
            NavigationRailItem(
                selected = isSelected,
                onClick = {
                    if (currentInnerDestination?.route != screen.route) {
                        onSelectedTabRouteChange(screen.route)
                        innerNavController.navigate(screen.route) {
                            popUpTo(innerNavController.graph.findStartDestination().id) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                },
                icon = { Icon(imageVector = if (isSelected) screen.icon else screen.outlinedIcon, contentDescription = screen.label) },
                label = { Text(screen.label) },
                alwaysShowLabel = false
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
    isMeowTalkEnabled: Boolean,
    onMeowTalkEnabledChange: (Boolean) -> Unit,
    meowTalkIntervalMinutes: Int,
    onMeowTalkIntervalChange: (Int) -> Unit,
    meowTalkSchedulingType: String,
    onMeowTalkSchedulingTypeChange: (String) -> Unit,
    meowTalkSpecificHour: Int,
    meowTalkSpecificMinute: Int,
    onMeowTalkSpecificTimeChange: (Int, Int) -> Unit,
    isVideoNotificationsEnabled: Boolean, // New parameter
    onVideoNotificationsEnabledChange: (Boolean) -> Unit, // New parameter
    getPendingVideoId: () -> String?,
    windowSizeClass: WindowWidthSizeClass
) {
    val TAG = "MainScreen"
    val innerNavController = rememberNavController()
    val videosViewModel: VideosViewModel = viewModel()
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var mediaPlayer by remember { mutableStateOf<MediaPlayer?>(null) }

    val navBackStackEntry by innerNavController.currentBackStackEntryAsState()
    val currentInnerDestination = navBackStackEntry?.destination

    val showNavigationUi = remember(currentInnerDestination) {
        currentInnerDestination?.route?.startsWith("video_detail/") == false
    }

    val useNavRail = windowSizeClass > WindowWidthSizeClass.Compact

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

    Scaffold(
        bottomBar = {
            if (!useNavRail && showNavigationUi) {
                AnimatedVisibility(visible = true, enter = fadeIn(), exit = fadeOut()) {
                    NavigationBar {
                        bottomNavItems.forEach { screen ->
                            val isSelected = currentInnerDestination?.hierarchy?.any { it.route == screen.route } == true || selectedTabRoute == screen.route
                            NavigationBarItem(
                                selected = isSelected,
                                onClick = {
                                    if (currentInnerDestination?.route != screen.route) {
                                        onSelectedTabRouteChange(screen.route)
                                        innerNavController.navigate(screen.route) {
                                            popUpTo(innerNavController.graph.findStartDestination().id) { saveState = true }
                                            launchSingleTop = true
                                            restoreState = true
                                        }
                                    }
                                },
                                icon = { Icon(imageVector = if (isSelected) screen.icon else screen.outlinedIcon, contentDescription = screen.label) },
                                label = { Text(screen.label) }
                            )
                        }
                    }
                }
            }
        }
    ) { scaffoldPaddingValues ->
        Row(modifier = Modifier.fillMaxSize().padding(scaffoldPaddingValues)) {
            if (useNavRail && showNavigationUi) {
                AnimatedVisibility(visible = true, enter = fadeIn(), exit = fadeOut()) {
                    AppNavigationRail(
                        innerNavController = innerNavController,
                        currentInnerDestination = currentInnerDestination,
                        selectedTabRoute = selectedTabRoute,
                        onSelectedTabRouteChange = onSelectedTabRouteChange,
                        items = bottomNavItems
                    )
                }
            }
            NavHost(
                navController = innerNavController,
                startDestination = selectedTabRoute,
                modifier = Modifier.weight(1f).fillMaxHeight()
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
                        isVideoNotificationsEnabled = isVideoNotificationsEnabled, // Pass to SettingsScreen
                        onVideoNotificationsEnabledChange = onVideoNotificationsEnabledChange // Pass to SettingsScreen
                    )
                }
                composable(BottomNavItem.Art.route) {
                    FanArtScreen(navController = innerNavController)
                }
                composable(BottomNavItem.MeowTalk.route) {
                    MeowTalkScreen(navController = innerNavController)
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
            }
        }
    }
}
