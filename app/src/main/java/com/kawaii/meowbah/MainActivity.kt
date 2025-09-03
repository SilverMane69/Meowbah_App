package com.kawaii.meowbah

import android.app.Activity
import android.content.Context
import android.content.Intent
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
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
import com.google.android.gms.tasks.Task
import com.kawaii.meowbah.ui.screens.FanArtScreen
import com.kawaii.meowbah.ui.screens.LoginScreen
import com.kawaii.meowbah.ui.screens.ProfileScreen
import com.kawaii.meowbah.ui.screens.VideosScreen
import com.kawaii.meowbah.ui.screens.videodetail.VideoDetailScreen
import com.kawaii.meowbah.ui.theme.AvailableTheme
import com.kawaii.meowbah.ui.theme.MeowbahTheme
import com.kawaii.meowbah.ui.theme.allThemes
import com.kawaii.meowbah.workers.YoutubeSyncWorker
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
    private val KEY_PROFILE_IMAGE_URI = "profileImageUri" // Added key
    private val TAG = "MainActivityGoogleSignIn"

    private lateinit var googleSignInClient: GoogleSignInClient
    private lateinit var googleSignInLauncher: ActivityResultLauncher<Intent>
    private lateinit var onLoginSuccessState: () -> Unit

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

    // --- Profile Image URI --- 
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
    // --- End Profile Image URI ---

    private fun startGoogleSignIn() {
        val signInIntent = googleSignInClient.signInIntent
        googleSignInLauncher.launch(signInIntent)
    }

    private fun handleGoogleSignInResult(task: Task<GoogleSignInAccount>) {
        try {
            val account = task.getResult(ApiException::class.java)
            Log.d(TAG, "Google Sign-In successful. Name: ${account?.displayName}, Email: ${account?.email}")
            if (::onLoginSuccessState.isInitialized) {
                onLoginSuccessState()
            }
        } catch (e: ApiException) {
            Log.w(TAG, "Google Sign-In failed code=${e.statusCode} message=${e.message}")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
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
            .requestIdToken("691684333330-k4a1q7tq2cbfm023mal00h1h1ffd6g2q.apps.googleusercontent.com")
            .requestEmail()
            .requestProfile()
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
            val onLogout: () -> Unit = remember { {
                isLoggedIn = false
                // Optionally, clear profile image on logout
                // saveProfileImageUriPreference(null) 
                // currentProfileImageUri = null
            } }

            var selectedTabRoute by rememberSaveable {
                mutableStateOf(BottomNavItem.Videos.route)
            }
            val onSelectedTabRouteChange: (String) -> Unit = remember { { newRoute ->
                selectedTabRoute = newRoute
            } }

            // Profile Image URI State
            var currentProfileImageUri by rememberSaveable { mutableStateOf(loadProfileImageUriPreference()) }
            val onProfileImageUriChange: (String?) -> Unit = remember { { newUri ->
                saveProfileImageUriPreference(newUri)
                currentProfileImageUri = newUri
            } }

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
                        profileImageUri = currentProfileImageUri, // Pass state
                        onProfileImageUriChange = onProfileImageUriChange // Pass setter
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
    profileImageUri: String?, // Added parameter
    onProfileImageUriChange: (String?) -> Unit // Added parameter
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

    NavHost(navController = navController, startDestination = "login") {
        composable("login") {
            if (!isLoggedIn) {
                LoginScreen(
                    onLoginClicked = { _, _ -> onLoginSuccess() },
                    onSignUpClicked = { /* TODO */ },
                    onForgotPasswordClicked = { /* TODO */ },
                    onGuestLoginClicked = { onLoginSuccess() },
                    onGoogleSignInClicked = onGoogleSignInRequested
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
                    profileImageUri = profileImageUri, // Pass through
                    onProfileImageUriChange = onProfileImageUriChange // Pass through
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
    profileImageUri: String?, // Added parameter
    onProfileImageUriChange: (String?) -> Unit // Added parameter
) {
    val innerNavController = rememberNavController()
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

    Scaffold(
        bottomBar = {
            NavigationBar(
                modifier = Modifier
                    .padding(start = 16.dp, end = 16.dp, bottom = 16.dp)
                    .clip(RoundedCornerShape(16.dp))
            ) {
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
    ) { paddingValues ->
        NavHost(
            navController = innerNavController,
            startDestination = selectedTabRoute,
            modifier = Modifier.padding(paddingValues).fillMaxSize()
        ) {
            composable(BottomNavItem.Videos.route) {
                VideosScreen(
                    navController = innerNavController, 
                    profileImageUri = profileImageUri // Pass to VideosScreen
                )
            }
            composable(BottomNavItem.FanArt.route) { FanArtScreen(navController = innerNavController) }
            composable(BottomNavItem.Profile.route) { 
                ProfileScreen(
                    navController = mainNavController, // This seems to be the outer NavController for login/main
                    currentAppTheme = currentAppTheme,
                    onThemeChange = onThemeChange,
                    onLogout = onLogout,
                    profileImageUri = profileImageUri, // Pass to ProfileScreen
                    onProfileImageUriChange = onProfileImageUriChange // Pass to ProfileScreen
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
