package com.kawaii.meowbah.ui.screens

import android.content.Context
// import android.content.Intent // No longer needed
// import android.net.Uri // No longer needed
// import androidx.activity.compose.rememberLauncherForActivityResult // No longer needed
// import androidx.activity.result.contract.ActivityResultContracts // No longer needed
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
// import androidx.compose.foundation.layout.Box // No longer needed
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
// import androidx.compose.foundation.layout.size // No longer needed for profile image
// import androidx.compose.foundation.layout.width // No longer needed for profile image buttons
import androidx.compose.foundation.rememberScrollState
// import androidx.compose.foundation.shape.CircleShape // No longer needed
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
// import androidx.compose.material.icons.filled.AccountCircle // No longer needed for placeholder
import androidx.compose.material.icons.filled.ArrowForward
// import androidx.compose.material.icons.filled.Delete // No longer needed
// import androidx.compose.material.icons.filled.Edit // No longer needed
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.MusicOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
// import androidx.compose.ui.draw.clip // No longer needed
// import androidx.compose.ui.graphics.vector.rememberVectorPainter // No longer needed
// import androidx.compose.ui.layout.ContentScale // No longer needed
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
// import coil.compose.AsyncImage // No longer needed
import com.kawaii.meowbah.ui.theme.AvailableTheme
import com.kawaii.meowbah.ui.theme.allThemes

private const val PREFS_NAME_PROFILE = "MeowbahProfilePrefs"
private const val KEY_USER_NAME = "userName"
private const val KEY_USER_BIO = "userBio"

@Composable
fun ProfileScreen(
    navController: NavController,
    currentAppTheme: AvailableTheme,
    onThemeChange: (AvailableTheme) -> Unit,
    onLogout: () -> Unit,
    isLoginMusicEnabled: Boolean,
    onLoginMusicEnabledChange: (Boolean) -> Unit
) {
    val context = LocalContext.current
    var showThemeDialog by remember { mutableStateOf(false) }

    var userName by rememberSaveable { mutableStateOf("") }
    var userBio by rememberSaveable { mutableStateOf("") }

    LaunchedEffect(Unit) {
        val sharedPrefs = context.getSharedPreferences(PREFS_NAME_PROFILE, Context.MODE_PRIVATE)
        userName = sharedPrefs.getString(KEY_USER_NAME, "User Name") ?: "User Name"
        userBio = sharedPrefs.getString(KEY_USER_BIO, "") ?: ""
    }

    // imagePickerLauncher removed

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Profile Image Card and Buttons removed
        Spacer(modifier = Modifier.height(32.dp)) // Added spacer for visual balance

        OutlinedTextField(
            value = userName,
            onValueChange = {
                userName = it
                val sharedPrefs = context.getSharedPreferences(PREFS_NAME_PROFILE, Context.MODE_PRIVATE)
                with(sharedPrefs.edit()) {
                    putString(KEY_USER_NAME, it)
                    apply()
                }
            },
            label = { Text("User Name") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            textStyle = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold)
        )

        OutlinedTextField(
            value = userBio,
            onValueChange = {
                userBio = it
                val sharedPrefs = context.getSharedPreferences(PREFS_NAME_PROFILE, Context.MODE_PRIVATE)
                with(sharedPrefs.edit()) {
                    putString(KEY_USER_BIO, it)
                    apply()
                }
            },
            label = { Text("Bio") },
            modifier = Modifier.fillMaxWidth().height(100.dp),
            maxLines = 3
        )

        HorizontalDivider()

        Card(
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.cardElevation(2.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showThemeDialog = true }
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("App Theme", style = MaterialTheme.typography.titleMedium)
                    Text(currentAppTheme.displayName, style = MaterialTheme.typography.bodyMedium)
                }
                Icon(Icons.Filled.ArrowForward, contentDescription = "Change theme")
            }
        }
        if (showThemeDialog) {
            ThemePickerDialog(currentTheme = currentAppTheme, onThemeSelected = {
                onThemeChange(it)
                showThemeDialog = false
            }, onDismiss = { showThemeDialog = false })
        }

        // Login Music Toggle Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.cardElevation(2.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    // .clickable { onLoginMusicEnabledChange(!isLoginMusicEnabled) } // Optional: make whole row clickable
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Login Music", style = MaterialTheme.typography.titleMedium)
                Switch(
                    checked = isLoginMusicEnabled,
                    onCheckedChange = onLoginMusicEnabledChange,
                    thumbContent = {
                        if (isLoginMusicEnabled) {
                            Icon(Icons.Filled.MusicNote, contentDescription = "Music Enabled")
                        } else {
                            Icon(Icons.Filled.MusicOff, contentDescription = "Music Disabled")
                        }
                    }
                )
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        Button(
            onClick = {
                onLogout()
                // Navigation to login is handled by AppNavigation's LaunchedEffect when isLoggedIn becomes false
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Logout")
        }
    }
}

@Composable
fun ThemePickerDialog(
    currentTheme: AvailableTheme,
    onThemeSelected: (AvailableTheme) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Select Theme") },
        text = {
            Column {
                allThemes.forEach { theme ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clickable { onThemeSelected(theme) }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Switch(
                            checked = theme == currentTheme,
                            onCheckedChange = { onThemeSelected(theme) }, // This directly selects the theme
                            modifier = Modifier.padding(end = 16.dp)
                        )
                        Text(theme.displayName)
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
