package com.kawaii.meowbah.ui.screens

import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.MusicOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.kawaii.meowbah.ui.theme.AvailableTheme
import com.kawaii.meowbah.ui.theme.allThemes

@Composable
fun SettingsScreen(
    navController: NavController,
    currentAppTheme: AvailableTheme,
    onThemeChange: (AvailableTheme) -> Unit,
    isLoginMusicEnabled: Boolean,
    onLoginMusicEnabledChange: (Boolean) -> Unit
) {
    var showThemeDialog by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val tiktokUrl = "https://tiktok.com/@meowbahx"
    val tiktokPackageName = "com.zhiliaoapp.musically"

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Column(
            modifier = Modifier
                .weight(1f) // This column takes up available space, pushing other elements to bottom
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Spacer(modifier = Modifier.height(16.dp))

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

            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(2.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Kawaii Madoka Music", style = MaterialTheme.typography.titleMedium) // Changed Label
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
        }

        // Social Link Buttons
        FilledTonalButton(
            onClick = {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://discord.gg/meowcord"))
                context.startActivity(intent) // Discord typically handles this well with a generic intent
            },
            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
        ) {
            Text("Meow's Discord")
        }

        FilledTonalButton(
            onClick = {
                val tiktokSpecificIntent = Intent(Intent.ACTION_VIEW, Uri.parse(tiktokUrl)).apply {
                    setPackage(tiktokPackageName)
                }
                if (context.packageManager.resolveActivity(tiktokSpecificIntent, PackageManager.MATCH_DEFAULT_ONLY) != null) {
                    context.startActivity(tiktokSpecificIntent)
                } else {
                    // Fallback to generic intent if TikTok app is not installed or can't handle it
                    val genericIntent = Intent(Intent.ACTION_VIEW, Uri.parse(tiktokUrl))
                    context.startActivity(genericIntent)
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Meow's TikTok")
        }

        // Copyright text at the bottom
        Text(
            text = "© Meowbah 2022-2025 All Rights Reserved",
            style = MaterialTheme.typography.bodySmall,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp)
        )
        Text(
            text = "This app was made with ❤️ by SilverMane69",
            style = MaterialTheme.typography.bodySmall,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 4.dp) // Added padding for spacing
        )
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
                            onCheckedChange = { onThemeSelected(theme) },
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
