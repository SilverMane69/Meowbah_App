package com.kawaii.meowbah.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MusicNote // Optional: for music toggle
import androidx.compose.material.icons.filled.MusicOff // Optional: for music toggle
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController // Changed import back
import coil.compose.AsyncImage
import com.kawaii.meowbah.ui.theme.AvailableTheme
import com.kawaii.meowbah.ui.theme.allThemes

private const val PREFS_NAME_PROFILE = "MeowbahProfilePrefs"
private const val KEY_USER_NAME = "userName"
private const val KEY_USER_BIO = "userBio"

@Composable
fun ProfileScreen(
    navController: NavController, // Changed type back to NavController
    currentAppTheme: AvailableTheme,
    onThemeChange: (AvailableTheme) -> Unit,
    onLogout: () -> Unit,
    profileImageUri: String?,
    onProfileImageUriChange: (String?) -> Unit,
    isLoginMusicEnabled: Boolean, // New parameter
    onLoginMusicEnabledChange: (Boolean) -> Unit // New parameter
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

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
        onResult = { uri: Uri? ->
            uri?.let {
                try {
                    val takeFlags = Intent.FLAG_GRANT_READ_URI_PERMISSION
                    context.contentResolver.takePersistableUriPermission(uri, takeFlags)
                } catch (e: SecurityException) {
                    // Log.e("ProfileScreen", "Failed to take persistable URI permission for $uri", e)
                }
                onProfileImageUriChange(it.toString())
            }
        }
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Card(
            shape = CircleShape,
            modifier = Modifier
                .size(120.dp)
                .clickable { imagePickerLauncher.launch("image/*") },
            elevation = CardDefaults.cardElevation(4.dp)
        ) {
            val imageModifier = Modifier.fillMaxSize().clip(CircleShape)
            if (profileImageUri != null) {
                AsyncImage(
                    model = profileImageUri,
                    placeholder = rememberVectorPainter(image = Icons.Filled.AccountCircle),
                    error = rememberVectorPainter(image = Icons.Filled.AccountCircle),
                    contentDescription = "Profile picture",
                    modifier = imageModifier,
                    contentScale = ContentScale.Crop
                )
            } else {
                Box(modifier = imageModifier, contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Filled.AccountCircle,
                        contentDescription = "Profile picture placeholder",
                        modifier = Modifier.size(80.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)){
            Button(onClick = { imagePickerLauncher.launch("image/*") }) {
                Icon(Icons.Filled.Edit, contentDescription = "Change picture")
                Spacer(modifier = Modifier.width(4.dp))
                Text("Change")
            }
            if (profileImageUri != null) {
                Button(onClick = { onProfileImageUriChange(null) }) {
                    Icon(Icons.Filled.Delete, contentDescription = "Remove picture")
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Remove")
                }
            }
        }

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
