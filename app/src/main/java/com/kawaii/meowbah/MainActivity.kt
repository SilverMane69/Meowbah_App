package com.kawaii.meowbah

import android.app.Activity // Added for Google Sign-In
import android.content.Context
import android.content.Intent // Added for Google Sign-In
import android.os.Bundle
import android.util.Log // Added for Google Sign-In logging
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResultLauncher // Added for Google Sign-In
import androidx.activity.result.contract.ActivityResultContracts // Added for Google Sign-In
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
import com.google.android.gms.auth.api.signin.GoogleSignIn // Added for Google Sign-In
import com.google.android.gms.auth.api.signin.GoogleSignInAccount // Added for Google Sign-In
import com.google.android.gms.auth.api.signin.GoogleSignInClient // Added for Google Sign-In
import com.google.android.gms.auth.api.signin.GoogleSignInOptions // Added for Google Sign-In
import com.google.android.gms.common.api.ApiException // Added for Google Sign-In
import com.google.android.gms.tasks.Task // Added for Google Sign-In
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
    private val TAG = "MainActivityGoogleSignIn" // For logging

    private lateinit var googleSignInClient: GoogleSignInClient
    private lateinit var googleSignInLauncher: ActivityResultLauncher<Intent>
    private lateinit var onLoginSuccessState: () -> Unit // To hold the onLoginSuccess lambda

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

    private fun startGoogleSignIn() {
        val signInIntent = googleSignInClient.signInIntent
        googleSignInLauncher.launch(signInIntent)
    }

    private fun handleGoogleSignInResult(task: Task<GoogleSignInAccount>) {
        try {
            val account = task.getResult(ApiException::class.java)
            // Signed in successfully
            Log.d(TAG, "Google Sign-In successful. Name: ${account?.displayName}, Email: ${account?.email}")
            // You can now get the ID token if you configured .requestIdToken(YOUR_WEB_CLIENT_ID)
            // val idToken = account?.idToken
            // TODO: Send ID Token to your backend server for verification if needed
            if (::onLoginSuccessState.isInitialized) {
                onLoginSuccessState() // Call the onLoginSuccess lambda from setContent
            }
        } catch (e: ApiException) {
            Log.w(TAG, "Google Sign-In failed code=${e.statusCode} message=${e.message}")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        scheduleYoutubeSync()

        // Configure Google Sign-In launcher
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

        // Configure Google Sign-In options
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken("691684333330-k4a1q7tq2cbfm023mal00h1h1ffd6g2q.apps.googleusercontent.com") // Replaced placeholder
            .requestEmail()
            .requestProfile() // requestProfile is optional if you only need email and ID token
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
            // Assign the lambda that changes isLoggedIn to the member variable
            onLoginSuccessState = remember { { isLoggedIn = true } }
            val onLogout: () -> Unit = remember { { isLoggedIn = false } }

            var selectedTabRoute by rememberSaveable {
                mutableStateOf(BottomNavItem.Videos.route)
            }
            val onSelectedTabRouteChange: (String) -> Unit = remember { { newRoute ->
                selectedTabRoute = newRoute
            } }

            MeowbahTheme(currentSelectedTheme = currentAppTheme) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                ) {
                    AppNavigation(
                        currentAppTheme = currentAppTheme,
                        onThemeChange = onThemeChange,
                        isLoggedIn = isLoggedIn,
                        onLoginSuccess = onLoginSuccessState, // Pass the stored lambda
                        onLogout = onLogout,
                        selectedTabRoute = selectedTabRoute,
                        onSelectedTabRouteChange = onSelectedTabRouteChange,
                        onGoogleSignInRequested = ::startGoogleSignIn // Pass the sign-in function reference
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
    onGoogleSignInRequested: () -> Unit // Added callback for Google Sign-In
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
                    onGoogleSignInClicked = onGoogleSignInRequested // Pass the callback to LoginScreen
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
                    onSelectedTabRouteChange = onSelectedTabRouteChange
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
    onSelectedTabRouteChange: (String) -> Unit
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
                VideosScreen(navController = innerNavController)
            }
            composable(BottomNavItem.FanArt.route) { FanArtScreen(navController = innerNavController) }
            composable(BottomNavItem.Profile.route) { 
                ProfileScreen(
                    navController = mainNavController,
                    currentAppTheme = currentAppTheme,
                    onThemeChange = onThemeChange,
                    onLogout = onLogout
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
