package com.kawaii.meowbah.ui.screens

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.layout.width
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
import androidx.compose.material3.Divider
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
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
import androidx.compose.ui.window.Dialog
import androidx.core.content.ContextCompat
import androidx.navigation.NavController
import com.kawaii.meowbah.ui.theme.AvailableTheme
import com.kawaii.meowbah.ui.theme.allThemes
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

data class MeowTalkIntervalOption(val label: String, val minutes: Int)

val meowTalkIntervalOptions = listOf(
    MeowTalkIntervalOption("15 Minutes", 15),
    MeowTalkIntervalOption("30 Minutes", 30),
    MeowTalkIntervalOption("1 Hour", 60),
    MeowTalkIntervalOption("2 Hours", 120),
    MeowTalkIntervalOption("4 Hours", 240),
    MeowTalkIntervalOption("8 Hours", 480)
)

private fun formatTime(hour: Int, minute: Int): String {
    val calendar = Calendar.getInstance()
    calendar.set(Calendar.HOUR_OF_DAY, hour)
    calendar.set(Calendar.MINUTE, minute)
    val sdf = SimpleDateFormat("hh:mm a", Locale.getDefault())
    return sdf.format(calendar.time)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    navController: NavController,
    currentAppTheme: AvailableTheme,
    onThemeChange: (AvailableTheme) -> Unit,
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
    onMeowTalkSpecificTimeChange: (hour: Int, minute: Int) -> Unit
) {
    var showThemeDialog by remember { mutableStateOf(false) }
    var showTimePickerDialog by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val tiktokUrl = "https://tiktok.com/@meowbahx"
    val tiktokPackageName = "com.zhiliaoapp.musically"

    var showPermissionRationale by remember { mutableStateOf(false) }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { isGranted ->
            if (isGranted) {
                onMeowTalkEnabledChange(true)
                showPermissionRationale = false // Hide rationale if permission is now granted
            } else {
                onMeowTalkEnabledChange(false)
                // Only show rationale if permission is explicitly denied by user FROM THE DIALOG
                // and they try to enable it again. The visibility is handled by check below.
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    showPermissionRationale = ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
                }
            }
        }
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
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
                    Text("Kawaii Madoka Music", style = MaterialTheme.typography.titleMedium)
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

            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Enable MeowTalk Notifications", style = MaterialTheme.typography.titleMedium)
                        Switch(
                            checked = isMeowTalkEnabled,
                            onCheckedChange = {
                                val newCheckedState = it
                                if (newCheckedState) {
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                        when (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)) {
                                            PackageManager.PERMISSION_GRANTED -> {
                                                onMeowTalkEnabledChange(true)
                                                showPermissionRationale = false
                                            }
                                            else -> {
                                                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                            }
                                        }
                                    } else {
                                        onMeowTalkEnabledChange(true)
                                        showPermissionRationale = false
                                    }
                                } else {
                                    onMeowTalkEnabledChange(false)
                                    // No need to show rationale when user explicitly disables
                                    showPermissionRationale = false
                                }
                            }
                        )
                    }

                    // Determine if rationale should be shown based on current state (not just after launcher result)
                    val shouldShowRationaleNow = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        !isMeowTalkEnabled && ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
                    } else {
                        false // Not relevant for older versions
                    }

                    AnimatedVisibility(visible = shouldShowRationaleNow) {
                        Column(modifier = Modifier.padding(top = 8.dp)) {
                            Text(
                                "MeowTalk notifications are currently disabled by system settings. Please grant permission to receive them.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Button(onClick = {
                                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                                intent.data = Uri.fromParts("package", context.packageName, null)
                                context.startActivity(intent)
                            }) {
                                Text("Open App Settings")
                            }
                        }
                    }

                    AnimatedVisibility(visible = isMeowTalkEnabled) {
                        Column {
                            Spacer(modifier = Modifier.height(8.dp))
                            Divider()
                            Spacer(modifier = Modifier.height(16.dp))
                            Text("Scheduling Options", style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(bottom = 8.dp))
                            Row(Modifier.fillMaxWidth()) {
                                Row(
                                    Modifier
                                        .weight(1f)
                                        .clickable { onMeowTalkSchedulingTypeChange("interval") }
                                        .padding(vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    RadioButton(
                                        selected = (meowTalkSchedulingType == "interval"),
                                        onClick = { onMeowTalkSchedulingTypeChange("interval") }
                                    )
                                    Spacer(Modifier.width(6.dp))
                                    Text("By Interval", style = MaterialTheme.typography.bodyMedium)
                                }
                                Row(
                                    Modifier
                                        .weight(1f)
                                        .clickable { onMeowTalkSchedulingTypeChange("specific_time") }
                                        .padding(vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    RadioButton(
                                        selected = (meowTalkSchedulingType == "specific_time"),
                                        onClick = { onMeowTalkSchedulingTypeChange("specific_time") }
                                    )
                                    Spacer(Modifier.width(6.dp))
                                    Text("Specific Time", style = MaterialTheme.typography.bodyMedium)
                                }
                            }
                            Spacer(modifier = Modifier.height(16.dp))
                            AnimatedVisibility(visible = meowTalkSchedulingType == "interval") {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Notification Interval", style = MaterialTheme.typography.titleMedium)
                                    var expanded by remember { mutableStateOf(false) }
                                    val currentIntervalLabel = meowTalkIntervalOptions.find { it.minutes == meowTalkIntervalMinutes }?.label ?: "Select Interval"
                                    Box {
                                        Text(
                                            text = currentIntervalLabel,
                                            modifier = Modifier
                                                .clickable { expanded = true }
                                                .padding(vertical = 4.dp, horizontal = 8.dp),
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                                            meowTalkIntervalOptions.forEach { option ->
                                                DropdownMenuItem(
                                                    text = { Text(option.label) },
                                                    onClick = {
                                                        onMeowTalkIntervalChange(option.minutes)
                                                        expanded = false
                                                    }
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                            AnimatedVisibility(visible = meowTalkSchedulingType == "specific_time") {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Scheduled Time", style = MaterialTheme.typography.titleMedium)
                                    TextButton(onClick = { showTimePickerDialog = true }) {
                                        Text(formatTime(meowTalkSpecificHour, meowTalkSpecificMinute))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        FilledTonalButton(
            onClick = {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://discord.gg/meowcord"))
                context.startActivity(intent)
            },
            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
        ) { Text("Meow's Discord") }

        FilledTonalButton(
            onClick = {
                val tiktokSpecificIntent = Intent(Intent.ACTION_VIEW, Uri.parse(tiktokUrl)).apply { setPackage(tiktokPackageName) }
                if (context.packageManager.resolveActivity(tiktokSpecificIntent, PackageManager.MATCH_DEFAULT_ONLY) != null) {
                    context.startActivity(tiktokSpecificIntent)
                } else {
                    val genericIntent = Intent(Intent.ACTION_VIEW, Uri.parse(tiktokUrl))
                    context.startActivity(genericIntent)
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) { Text("Meow's TikTok") }

        Text(
            text = "© Meowbah 2022-2025 All Rights Reserved",
            style = MaterialTheme.typography.bodySmall,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth().padding(top = 16.dp)
        )
        Text(
            text = "This app was made with ❤️ by SilverMane69",
            style = MaterialTheme.typography.bodySmall,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth().padding(top = 4.dp)
        )
    }

    if (showTimePickerDialog) {
        val timePickerState = rememberTimePickerState(
            initialHour = meowTalkSpecificHour,
            initialMinute = meowTalkSpecificMinute,
            is24Hour = false
        )
        TimePickerDialog(
            title = "Select Scheduled Time",
            onCancel = { showTimePickerDialog = false },
            onConfirm = {
                onMeowTalkSpecificTimeChange(timePickerState.hour, timePickerState.minute)
                showTimePickerDialog = false
            },
            content = {
                TimePicker(state = timePickerState, modifier = Modifier.padding(16.dp))
            }
        )
    }
}

@Composable
fun TimePickerDialog(
    title: String = "Select Time",
    onCancel: () -> Unit,
    onConfirm: () -> Unit,
    content: @Composable () -> Unit
) {
    Dialog(onDismissRequest = onCancel) {
        Card(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            shape = MaterialTheme.shapes.large,
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(title, style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(bottom = 20.dp))
                content()
                Spacer(modifier = Modifier.height(24.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onCancel) { Text("Cancel") }
                    Spacer(modifier = Modifier.width(8.dp))
                    TextButton(onClick = onConfirm) { Text("OK") }
                }
            }
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
                            onCheckedChange = { onThemeSelected(theme) },
                            modifier = Modifier.padding(end = 16.dp)
                        )
                        Text(theme.displayName)
                    }
                }
            }
        },
        confirmButton = { Button(onClick = onDismiss) { Text("Cancel") } }
    )
}
