package com.kawaii.meowbah.ui.dialogs

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
// import androidx.compose.ui.Modifier // Removed unused import
import androidx.compose.ui.tooling.preview.Preview
import com.kawaii.meowbah.ui.theme.MeowbahTheme

@Composable
fun WelcomeDialog(onDismissRequest: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = {
            Text(text = "Konnichiwa~!!")
        },
        text = {
            Text(text = "Welcome to the all-new all-kawaii Meowbah App!! This app was created for the Meowists of the world. Please note that the app can currently only show 15 of Meow\'s most recent videos, but a button at the bottom will take you to the rest")
        },
        confirmButton = {
            TextButton(
                onClick = onDismissRequest
            ) {
                Text("Got it!")
            }
        }
        // Material 3 AlertDialog has rounded corners by default.
        // If specific corner radius is needed, it can be done via shape parameter of AlertDialog.
        // shape = RoundedCornerShape(16.dp) // Example, but default is usually fine.
    )
}

@Preview(showBackground = true)
@Composable
fun WelcomeDialogPreview() {
    MeowbahTheme {
        WelcomeDialog(onDismissRequest = {})
    }
}
