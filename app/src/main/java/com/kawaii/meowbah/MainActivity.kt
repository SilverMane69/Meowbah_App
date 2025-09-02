package com.kawaii.meowbah

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.kawaii.meowbah.ui.screens.FanArtScreen
import com.kawaii.meowbah.ui.screens.LoginScreen
import com.kawaii.meowbah.ui.screens.ProfileScreen
import com.kawaii.meowbah.ui.screens.VideosScreen
import com.kawaii.meowbah.ui.screens.videodetail.VideoDetailScreen
import com.kawaii.meowbah.ui.theme.MeowbahTheme

sealed class BottomNavItem(val route: String, val label: String, val icon: ImageVector) {
    object Videos : BottomNavItem("videos_tab", "Videos", Icons.Filled.Videocam)
    object FanArt : BottomNavItem("fan_art_tab", "Fan Art", Icons.Filled.Favorite)
    object Profile : BottomNavItem("profile_tab", "Profile", Icons.Filled.AccountCircle)
}

val bottomNavItems = listOf(
    BottomNavItem.Videos,
    BottomNavItem.FanArt,
    BottomNavItem.Profile
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MeowbahTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                ) {
                    AppNavigation()
                }
            }
        }
    }
}

@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    var isLoggedIn by remember { mutableStateOf(false) } // Simple state for login status

    NavHost(navController = navController, startDestination = "login") {
        composable("login") {
            LoginScreen(
                onLoginClicked = { _, _ ->
                    isLoggedIn = true
                    navController.navigate("main_screen") {
                        popUpTo("login") { inclusive = true }
                        launchSingleTop = true
                    }
                },
                onSignUpClicked = { /* TODO */ },
                onForgotPasswordClicked = { /* TODO */ },
                onGuestLoginClicked = { // Added guest login handler
                    isLoggedIn = true
                    navController.navigate("main_screen") {
                        popUpTo("login") { inclusive = true }
                        launchSingleTop = true
                    }
                }
            )
        }
        composable("main_screen") {
            if (isLoggedIn) {
                MainScreen()
            } else {
                // If not logged in and somehow tried to access main_screen, redirect to login
                navController.navigate("login") {
                    popUpTo("main_screen") { inclusive = true }
                }
            }
        }
    }
}

@Composable
fun MainScreen() {
    val innerNavController = rememberNavController()
    Scaffold(
        bottomBar = {
            NavigationBar {
                val navBackStackEntry by innerNavController.currentBackStackEntryAsState()
                val currentDestination = navBackStackEntry?.destination
                bottomNavItems.forEach { screen ->
                    NavigationBarItem(
                        icon = { Icon(screen.icon, contentDescription = screen.label) },
                        label = { Text(screen.label) },
                        selected = currentDestination?.hierarchy?.any { it.route == screen.route } == true,
                        onClick = {
                            innerNavController.navigate(screen.route) {
                                popUpTo(innerNavController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = innerNavController,
            startDestination = BottomNavItem.Videos.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(BottomNavItem.Videos.route) {
                VideosScreen(navController = innerNavController)
            }
            composable(BottomNavItem.FanArt.route) { FanArtScreen(navController = innerNavController) } // CORRECTED
            composable(BottomNavItem.Profile.route) { ProfileScreen(navController = innerNavController) } // CORRECTED

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
