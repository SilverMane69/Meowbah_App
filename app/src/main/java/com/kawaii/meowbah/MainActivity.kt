package com.kawaii.meowbah

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.MediaPlayer
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
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
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavController
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
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
import com.kawaii.meowbah.data.AuthRepository
import com.kawaii.meowbah.data.TokenStorageService // Added import
import com.kawaii.meowbah.data.remote.AuthApiService
import com.kawaii.meowbah.ui.activities.NotificationUtils
import com.kawaii.meowbah.ui.screens.FanArtScreen
import com.kawaii.meowbah.ui.screens.LoginScreen
import com.kawaii.meowbah.ui.screens.ProfileScreen
import com.kawaii.meowbah.ui.screens.VideosScreen
import com.kawaii.meowbah.ui.screens.videodetail.VideoDetailScreen
import com.kawaii.meowbah.ui.theme.AvailableTheme
import com.kawaii.meowbah.ui.theme.MeowbahTheme
import com.kawaii.meowbah.ui.theme.allThemes
import com.kawaii.meowbah.widgets.VideoWidgetViewsFactory
import com.kawaii.meowbah.workers.YoutubeSyncWorker
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.delay

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
    private val KEY_PROFILE_IMAGE_URI = "profileImageUri"
    private val KEY_IS_LOGGED_IN = "isLoggedIn"
    private val KEY_LOGIN_TYPE = "loginType"
    private val KEY_LOGIN_MUSIC_ENABLED = "loginMusicEnabled"
    private val TAG = "MainActivity"

    private lateinit var googleSignInClient: GoogleSignInClient
    private lateinit var googleSignInLauncher: ActivityResultLauncher<Intent>

    private var loginSoundPlayer: MediaPlayer? = null
    private var wasLoginSoundPlaying: Boolean = false
    private var isLoginMusicGloballyEnabled: Boolean = true

    private val _videoIdToOpenFromWidget = MutableStateFlow<String?>(null)

    private lateinit var authApiService: AuthApiService
    private lateinit var authRepository: AuthRepository
    private lateinit var tokenStorageService: TokenStorageService

    companion object {
        const val ACTION_VIEW_VIDEO = "com.kawaii.meowbah.ACTION_VIEW_VIDEO"
        const val EXTRA_VIDEO_ID = "com.kawaii.meowbah.EXTRA_VIDEO_ID"
    }

    private var onGoogleSignInSuccessInternal: (account: GoogleSignInAccount) -> Unit = { acc ->
        Log.d(TAG, "Default onGoogleSignInSuccessInternal for ${acc.displayName}. State setters not hooked up.")
        saveLoginState(true, "google")
    }
    private var onGoogleSignInFailureInternal: (exception: Exception?) -> Unit = { e ->
        Log.w(TAG, "Default onGoogleSignInFailureInternal. State setters not hooked up.", e)
    }

    private fun saveLoginState(loggedIn: Boolean, loginType: String? = null) {
        val sharedPrefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        with(sharedPrefs.edit()) {
            putBoolean(KEY_IS_LOGGED_IN, loggedIn)
            if (loggedIn && loginType != null) {
                putString(KEY_LOGIN_TYPE, loginType)
            } else {
                remove(KEY_LOGIN_TYPE)
            }
            apply()
        }
    }

    private fun loadLoginState(): Pair<Boolean, String?> {
        val sharedPrefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val isLoggedIn = sharedPrefs.getBoolean(KEY_IS_LOGGED_IN, false)
        val loginType = if (isLoggedIn) sharedPrefs.getString(KEY_LOGIN_TYPE, null) else null
        return Pair(isLoggedIn, loginType)
    }

    private fun saveLoginMusicPreference(enabled: Boolean) {
        val sharedPrefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        with(sharedPrefs.edit()) {
            putBoolean(KEY_LOGIN_MUSIC_ENABLED, enabled)
            apply()
        }
        isLoginMusicGloballyEnabled = enabled
    }

    private fun loadLoginMusicPreference(): Boolean {
        val sharedPrefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        isLoginMusicGloballyEnabled = sharedPrefs.getBoolean(KEY_LOGIN_MUSIC_ENABLED, true)
        return isLoginMusicGloballyEnabled
    }

    fun onLoginMusicEnabledChanged(enabled: Boolean, isLoggedInState: Boolean) {
        saveLoginMusicPreference(enabled)
        if (!enabled) {
            loginSoundPlayer?.let {
                if (it.isPlaying) {
                    Log.d(TAG, "Login music disabled by user, pausing MediaPlayer.")
                    it.pause()
                }
            }
        } else {
            if (isLoggedInState && wasLoginSoundPlaying && loginSoundPlayer != null && loginSoundPlayer?.isPlaying == false) {
                Log.d(TAG, "Login music enabled by user, resuming MediaPlayer.")
                try {
                    loginSoundPlayer?.start()
                } catch (e: IllegalStateException) {
                    Log.e(TAG, "Error resuming login sound on enable toggle", e)
                    loginSoundPlayer?.release()
                    loginSoundPlayer = null
                    wasLoginSoundPlaying = false
                }
            } else if (isLoggedInState && loginSoundPlayer == null && isLoginMusicGloballyEnabled) {
                 Log.d(TAG, "Login music enabled, player was null, calling playLoginSound()");
                 playLoginSound()
            }
        }
    }

    fun playLoginSound() {
        if (!isLoginMusicGloballyEnabled) {
            Log.d(TAG, "Login music is globally disabled, not playing sound.")
            return
        }
        try {
            if (loginSoundPlayer == null) {
                Log.d(TAG, "Creating new MediaPlayer for login sound.")
                loginSoundPlayer = MediaPlayer.create(this, R.raw.madoka_music)
                loginSoundPlayer?.isLooping = true
                loginSoundPlayer?.setOnErrorListener { mp, what, extra ->
                    Log.e(TAG, "MediaPlayer Error: what: $what, extra: $extra")
                    mp?.release()
                    if (loginSoundPlayer == mp) {
                        loginSoundPlayer = null
                        wasLoginSoundPlaying = false
                    }
                    true
                }
                loginSoundPlayer?.setOnCompletionListener { mp ->
                    Log.d(TAG, "Login sound MediaPlayer (looping) completed - indicates isLooping was set to false or an issue occurred.")
                    mp.release()
                    if (loginSoundPlayer == mp) {
                        loginSoundPlayer = null
                        wasLoginSoundPlaying = false
                    }
                }
            }
            loginSoundPlayer?.let {
                if (!it.isPlaying) {
                    Log.d(TAG, "Starting/Resuming login sound MediaPlayer (looping).")
                    it.start()
                    wasLoginSoundPlaying = true
                } else {
                     Log.d(TAG, "playLoginSound called, but player is already playing.")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception in playLoginSound R.raw.madoka_music", e)
            loginSoundPlayer?.release()
            loginSoundPlayer = null
            wasLoginSoundPlaying = false
        }
    }

    override fun onStart() {
        super.onStart()
        if (!isLoginMusicGloballyEnabled) {
            Log.d(TAG, "Login music is globally disabled, not resuming in onStart.")
            return
        }
        if (wasLoginSoundPlaying && loginSoundPlayer != null && loginSoundPlayer?.isPlaying == false) {
            Log.d(TAG, "App started, resuming login sound MediaPlayer.")
            try {
                loginSoundPlayer?.start()
            } catch (e: IllegalStateException) {
                Log.e(TAG, "Error resuming login sound MediaPlayer in onStart", e)
                loginSoundPlayer?.release()
                loginSoundPlayer = null
                wasLoginSoundPlaying = false
            }
        }
    }

    override fun onStop() {
        super.onStop()
        loginSoundPlayer?.let {
            if (it.isPlaying) {
                Log.d(TAG, "App stopped, pausing login sound MediaPlayer.")
                it.pause()
            } else {
                 Log.d(TAG, "App stopped, login sound player exists but is not playing.")
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "Releasing login sound MediaPlayer in onDestroy.")
        loginSoundPlayer?.release()
        loginSoundPlayer = null
        wasLoginSoundPlaying = false
    }

    private fun saveThemePreference(theme: AvailableTheme) {
        val sharedPrefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        with(sharedPrefs.edit()) {
            putString(KEY_SELECTED_THEME, theme.displayName)
            apply()
        }
    }

    private fun loadThemePreference(): AvailableTheme {
        val sharedPrefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val themeName = sharedPrefs.getString(KEY_SELECTED_THEME, null)
        return allThemes.firstOrNull { it.displayName == themeName } ?: AvailableTheme.Pink
    }

    private fun saveProfileImageUriPreference(uri: String?) {
        val sharedPrefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        with(sharedPrefs.edit()) {
            if (uri == null) {
                remove(KEY_PROFILE_IMAGE_URI)
            } else {
                putString(KEY_PROFILE_IMAGE_URI, uri)
            }
            apply()
        }
    }

    private fun loadProfileImageUriPreference(): String? {
        val sharedPrefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return sharedPrefs.getString(KEY_PROFILE_IMAGE_URI, null)
    }

    private fun startGoogleSignIn() {
        val signInIntent = googleSignInClient.signInIntent
        googleSignInLauncher.launch(signInIntent)
    }

    private fun handleIntent(intent: Intent?) {
        intent?.let {
            if (it.action == VideoWidgetViewsFactory.ACTION_VIEW_VIDEO || it.action == ACTION_VIEW_VIDEO) {
                val videoId = it.getStringExtra(VideoWidgetViewsFactory.EXTRA_VIDEO_ID)
                    ?: it.getStringExtra(EXTRA_VIDEO_ID)
                if (videoId != null) {
                    Log.d(TAG, "Intent to open video from widget received: $videoId")
                    _videoIdToOpenFromWidget.value = videoId
                } else {
                    Log.w(TAG, "Widget intent received with null videoId for action ${it.action}")
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        Log.d(TAG, "onNewIntent called with action: ${intent?.action}")
        setIntent(intent)
        handleIntent(intent)
    }

    @OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        scheduleYoutubeSync()
        NotificationUtils.createNotificationChannel(this)

        handleIntent(intent) // Handle initial intent

        tokenStorageService = TokenStorageService(applicationContext)
        authApiService = AuthApiService.create()
        authRepository = AuthRepository(authApiService)

        val (initialIsLoggedIn, initialLoginType) = loadLoginState()
        val initialLoginMusicEnabled = loadLoginMusicPreference()

        googleSignInLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
                try {
                    val account = task.getResult(ApiException::class.java)
                    account?.let { onGoogleSignInSuccessInternal(it) }
                } catch (e: ApiException) {
                    Log.w(TAG, "Google Sign-In failed to get account: code=${e.statusCode}, message=${e.message}")
                    onGoogleSignInFailureInternal(e)
                }
            } else {
                Log.w(TAG, "Google Sign-In cancelled or failed via launcher. Result Code: ${result.resultCode}")
                onGoogleSignInFailureInternal(null)
            }
        }

        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestServerAuthCode("1017808544125-taqamrcgk5n6rbm0llubi8t1dge6hi4c.apps.googleusercontent.com")
            .requestEmail()
            .requestProfile()
            .requestScopes(Scope("https.www.googleapis.com/auth/youtube.readonly"))
            .build()

        googleSignInClient = GoogleSignIn.getClient(this, gso)

        setContent {
            RequestNotificationPermission()

            val windowSizeClass = calculateWindowSizeClass(this)
            val widthSizeClass = windowSizeClass.widthSizeClass

            var currentAppTheme by remember { mutableStateOf(loadThemePreference()) }
            val onThemeChange: (AvailableTheme) -> Unit = remember { {
                newTheme ->
                    saveThemePreference(newTheme)
                    currentAppTheme = newTheme
            } }

            var isLoggedIn by rememberSaveable { mutableStateOf(initialIsLoggedIn) }
            var currentLoginType by rememberSaveable { mutableStateOf<String?>(initialLoginType) }
            var isLoginMusicEnabledState by rememberSaveable { mutableStateOf(initialLoginMusicEnabled) }

            val videoIdToOpen by _videoIdToOpenFromWidget.collectAsState()
            val onWidgetVideoOpened = remember { { _videoIdToOpenFromWidget.value = null } }

            onGoogleSignInSuccessInternal = { account ->
                Log.d(TAG, "Google Sign-In successful (Composable context). Name: ${account.displayName}")
                Log.d(TAG, "Server Auth Code (first 10 chars): ${account.serverAuthCode?.take(10)}...")
                isLoggedIn = true
                currentLoginType = "google"
                saveLoginState(true, "google")

                account.serverAuthCode?.let { authCode ->
                    if (authCode.isNotBlank()) {
                        lifecycleScope.launch {
                            Log.d(TAG, "Exchanging server auth code for tokens...")
                            val tokenResult = authRepository.exchangeCodeForTokens(authCode)
                            tokenResult.fold(
                                onSuccess = { tokenResponse ->
                                    Log.i(TAG, "Tokens received successfully!")
                                    Log.i(TAG, "Access Token (first 10): ${tokenResponse.accessToken.take(10)}...")
                                    tokenResponse.refreshToken?.let {
                                        Log.i(TAG, "Refresh Token (first 10): ${it.take(10)}...")
                                    } ?: Log.i(TAG, "Refresh Token: null")
                                    tokenStorageService.saveTokens(tokenResponse)
                                },
                                onFailure = { error ->
                                    Log.e(TAG, "Failed to exchange server auth code for tokens", error)
                                    // TODO: Handle token exchange failure (e.g., inform user, retry options?)
                                }
                            )
                        }
                    } else {
                        Log.w(TAG, "Server auth code is blank, cannot exchange for tokens.")
                    }
                } ?: Log.w(TAG, "Server auth code is null, cannot exchange for tokens.")
            }

            onGoogleSignInFailureInternal = { exception ->
                Log.w(TAG, "Google Sign-In failed (Composable context).", exception)
            }

            val onActualLoginMusicEnabledChange: (Boolean) -> Unit = remember { {
                enabled ->
                    onLoginMusicEnabledChanged(enabled, isLoggedIn)
                    isLoginMusicEnabledState = enabled
            } }

            val onGuestLogin: () -> Unit = remember { {
                isLoggedIn = true
                currentLoginType = "guest"
                saveLoginState(true, "guest")
            } }

            val onLogout: () -> Unit = remember { {
                if (currentLoginType == "google") {
                    googleSignInClient.signOut().addOnCompleteListener(this@MainActivity) { task ->
                        Log.d(TAG, "Google Sign Out task completed. Successful: ${task.isSuccessful}")
                    }
                }
                isLoggedIn = false
                currentLoginType = null
                saveLoginState(false)
                tokenStorageService.clearTokens()
                loginSoundPlayer?.apply {
                    if (isPlaying) stop()
                    release()
                }
                loginSoundPlayer = null
                wasLoginSoundPlaying = false
            } }

            var selectedTabRoute by rememberSaveable {
                mutableStateOf(BottomNavItem.Videos.route)
            }
            val onSelectedTabRouteChange: (String) -> Unit = remember { { newRoute ->
                selectedTabRoute = newRoute
            } }

            var currentProfileImageUri by rememberSaveable { mutableStateOf(loadProfileImageUriPreference()) }
            val onProfileImageUriChange: (String?) -> Unit = remember { { newUri ->
                saveProfileImageUriPreference(newUri)
                currentProfileImageUri = newUri
            } }
            
            LaunchedEffect(Unit) {
                if (isLoggedIn && currentLoginType == "google") {
                    val lastAccount = GoogleSignIn.getLastSignedInAccount(this@MainActivity)
                    val isTokenValid = tokenStorageService.isAccessTokenValid()

                    if (lastAccount == null || !isTokenValid) {
                        Log.w(TAG, "User was marked as Google logged in, but lastAccount is ${if(lastAccount == null) "null" else "present"} and token is ${if(isTokenValid) "valid" else "invalid/expired"}. Forcing logout.")
                        
                        googleSignInClient.signOut().addOnCompleteListener(this@MainActivity) { task ->
                            Log.d(TAG, "Forced Google Sign Out during launch check. Successful: ${task.isSuccessful}")
                        }
                        isLoggedIn = false 
                        currentLoginType = null
                        saveLoginState(false) 
                        tokenStorageService.clearTokens()

                        loginSoundPlayer?.apply {
                            if (isPlaying) stop()
                            release()
                        }
                        loginSoundPlayer = null
                        wasLoginSoundPlaying = false
                    } else {
                        Log.d(TAG, "User previously signed in with Google, and token is still valid.")
                        if (!GoogleSignIn.hasPermissions(lastAccount, Scope("https.www.googleapis.com/auth/youtube.readonly"))) {
                            Log.d(TAG, "User previously signed in but YouTube scope not granted/revoked. May need re-consent.")
                        }
                    }
                }
            }

            MeowbahTheme(currentSelectedTheme = currentAppTheme) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                ) {
                    AppNavigation(
                        widthSizeClass = widthSizeClass,
                        currentAppTheme = currentAppTheme,
                        onThemeChange = onThemeChange,
                        isLoggedIn = isLoggedIn,
                        onGuestLogin = onGuestLogin,
                        onLogout = onLogout,
                        selectedTabRoute = selectedTabRoute,
                        onSelectedTabRouteChange = onSelectedTabRouteChange,
                        onGoogleSignInRequested = ::startGoogleSignIn,
                        profileImageUri = currentProfileImageUri,
                        onProfileImageUriChange = onProfileImageUriChange,
                        onPlayLoginSound = ::playLoginSound,
                        isLoginMusicEnabled = isLoginMusicEnabledState,
                        onLoginMusicEnabledChange = onActualLoginMusicEnabledChange,
                        initialVideoIdToOpen = videoIdToOpen,
                        onWidgetVideoOpened = onWidgetVideoOpened
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
fun RequestNotificationPermission() {
    val context = LocalContext.current
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { isGranted: Boolean ->
            if (isGranted) {
                Log.d("NotificationPermission", "Permission Granted")
            } else {
                Log.d("NotificationPermission", "Permission Denied")
            }
        }
    )

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            when (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)) {
                PackageManager.PERMISSION_GRANTED -> {
                    Log.d("NotificationPermission", "Permission already granted")
                }
                else -> {
                    Log.d("NotificationPermission", "Requesting permission")
                    launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            }
        }
    }
}


@Composable
fun AppNavigation(
    widthSizeClass: WindowWidthSizeClass,
    currentAppTheme: AvailableTheme,
    onThemeChange: (AvailableTheme) -> Unit,
    isLoggedIn: Boolean,
    onGuestLogin: () -> Unit, 
    onLogout: () -> Unit,
    selectedTabRoute: String,
    onSelectedTabRouteChange: (String) -> Unit,
    onGoogleSignInRequested: () -> Unit,
    profileImageUri: String?,
    onProfileImageUriChange: (String?) -> Unit,
    onPlayLoginSound: () -> Unit,
    isLoginMusicEnabled: Boolean,
    onLoginMusicEnabledChange: (Boolean) -> Unit,
    initialVideoIdToOpen: String? = null,
    onWidgetVideoOpened: () -> Unit = {}
) {
    Log.d("AppNavigation", "Composing with isLoggedIn: $isLoggedIn, initialVideoIdToOpen: $initialVideoIdToOpen")
    val navController: NavHostController = rememberNavController() // Explicitly typed
    var previousLoggedIn by rememberSaveable { mutableStateOf(isLoggedIn) }

    LaunchedEffect(Unit) {
        if (isLoggedIn) {
            Log.d("AppNavigation", "App launch: User is already logged in. Calling onPlayLoginSound()")
            onPlayLoginSound()
        }
    }

    LaunchedEffect(isLoggedIn) { 
        Log.d("AppNavigation", "Login state transition check. Current isLoggedIn: $isLoggedIn, Previous: $previousLoggedIn")
        if (isLoggedIn && !previousLoggedIn) { 
            Log.d("AppNavigation", "User logged IN during session. Calling onPlayLoginSound()")
            delay(100) 
            onPlayLoginSound()
        } else if (!isLoggedIn && previousLoggedIn) { 
            Log.d("AppNavigation", "User logged OUT during session. Navigating to login.")
            navController.navigate("login") {
                popUpTo(navController.graph.id) { inclusive = true }
                launchSingleTop = true
            }
        }
        previousLoggedIn = isLoggedIn 
    }

    val startDestination = if (isLoggedIn) "main_screen" else "login"
    Log.d("AppNavigation", "NavHost startDestination: $startDestination")

    NavHost(navController = navController, startDestination = startDestination) {
        composable("login") {
            LoginScreen(
                onLoginClicked = { _, _ -> /* TODO */ }, 
                onSignUpClicked = { /* TODO */ },
                onForgotPasswordClicked = { /* TODO */ },
                onGuestLoginClicked = onGuestLogin,
                onGoogleSignInClicked = onGoogleSignInRequested,
                isLoginMusicEnabled = isLoginMusicEnabled,
                onLoginMusicEnabledChange = onLoginMusicEnabledChange
            )
        }
        composable("main_screen") {
            MainScreen(
                widthSizeClass = widthSizeClass,
                currentAppTheme = currentAppTheme,
                onThemeChange = onThemeChange,
                onLogout = onLogout,
                mainNavController = navController, 
                selectedTabRoute = selectedTabRoute,
                onSelectedTabRouteChange = onSelectedTabRouteChange,
                profileImageUri = profileImageUri,
                onProfileImageUriChange = onProfileImageUriChange,
                isLoginMusicEnabled = isLoginMusicEnabled,
                onLoginMusicEnabledChange = onLoginMusicEnabledChange,
                initialVideoIdToOpen = initialVideoIdToOpen, // Pass down
                onWidgetVideoOpened = onWidgetVideoOpened      // Pass down
            )
        }
    }
}

@Composable
fun MainScreen(
    widthSizeClass: WindowWidthSizeClass,
    currentAppTheme: AvailableTheme,
    onThemeChange: (AvailableTheme) -> Unit,
    onLogout: () -> Unit,
    mainNavController: NavController, // This remains NavController as it's passed down from AppNavigation
    selectedTabRoute: String,
    onSelectedTabRouteChange: (String) -> Unit,
    profileImageUri: String?,
    onProfileImageUriChange: (String?) -> Unit,
    isLoginMusicEnabled: Boolean,
    onLoginMusicEnabledChange: (Boolean) -> Unit,
    initialVideoIdToOpen: String? = null,
    onWidgetVideoOpened: () -> Unit = {}
) {
    val innerNavController: NavHostController = rememberNavController() // Explicitly typed
    val navBackStackEntry by innerNavController.currentBackStackEntryAsState()
    val currentInnerDestination = navBackStackEntry?.destination

    LaunchedEffect(innerNavController, selectedTabRoute) {
        if (currentInnerDestination?.route != selectedTabRoute) {
            innerNavController.navigate(selectedTabRoute) {
                popUpTo(innerNavController.graph.findStartDestination().id) {
                    saveState = true
                }
                launchSingleTop = true
                restoreState = true
            }
        }
    }

    LaunchedEffect(initialVideoIdToOpen) {
        if (initialVideoIdToOpen != null) {
            Log.d("MainScreen", "Widget requested to open video: $initialVideoIdToOpen. Navigating innerNavController.")
            if (selectedTabRoute != BottomNavItem.Videos.route) {
                onSelectedTabRouteChange(BottomNavItem.Videos.route)
            }
            innerNavController.navigate("video_detail/$initialVideoIdToOpen") {
                launchSingleTop = true 
            }
            onWidgetVideoOpened() // Reset the trigger so it doesn't re-fire on recomposition
        }
    }

    Scaffold(
        bottomBar = {
            if (widthSizeClass == WindowWidthSizeClass.Compact) {
                NavigationBar {
                    bottomNavItems.forEach { screen ->
                        NavigationBarItem(
                            selected = currentInnerDestination?.hierarchy?.any { it.route == screen.route } == true,
                            onClick = {
                                if (currentInnerDestination?.route != screen.route) {
                                    onSelectedTabRouteChange(screen.route)
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
        }
    ) { paddingValues -> 
        Row(
            Modifier
                .fillMaxSize()
                .padding(paddingValues) 
        ) {
            if (widthSizeClass != WindowWidthSizeClass.Compact) {
                NavigationRail { 
                    bottomNavItems.forEach { screen ->
                        NavigationRailItem(
                            selected = currentInnerDestination?.hierarchy?.any { it.route == screen.route } == true,
                            onClick = {
                                if (currentInnerDestination?.route != screen.route) {
                                    onSelectedTabRouteChange(screen.route)
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
                            label = { Text(screen.label) },
                            alwaysShowLabel = false
                        )
                    }
                }
            }
            NavHost(
                navController = innerNavController,
                startDestination = selectedTabRoute, 
                modifier = Modifier
                    .weight(1f)
                    .fillMaxSize()
            ) {
                composable(BottomNavItem.Videos.route) {
                    VideosScreen(
                        navController = innerNavController, 
                        profileImageUri = profileImageUri
                    )
                }
                composable(BottomNavItem.FanArt.route) { FanArtScreen(navController = innerNavController) }
                composable(BottomNavItem.Profile.route) { 
                    ProfileScreen(
                        navController = mainNavController, 
                        currentAppTheme = currentAppTheme,
                        onThemeChange = onThemeChange,
                        onLogout = onLogout,
                        profileImageUri = profileImageUri,
                        onProfileImageUriChange = onProfileImageUriChange,
                        isLoginMusicEnabled = isLoginMusicEnabled,
                        onLoginMusicEnabledChange = onLoginMusicEnabledChange
                    ) 
                }
                composable(
                    route = "video_detail/{videoId}",
                    arguments = listOf(navArgument("videoId") { type = NavType.StringType })
                ) { backStackEntry ->
                    VideoDetailScreen(
                        navController = innerNavController,
                        videoId = backStackEntry.arguments?.getString("videoId") ?: ""
                    )
                }
            }
        }
    }
}
