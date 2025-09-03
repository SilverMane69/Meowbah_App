package com.kawaii.meowbah.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.gms.common.SignInButton
import com.kawaii.meowbah.R
import com.kawaii.meowbah.ui.theme.MeowbahTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(
    modifier: Modifier = Modifier,
    onLoginClicked: (String, String) -> Unit = { _, _ -> },
    onSignUpClicked: () -> Unit = {},
    onForgotPasswordClicked: () -> Unit = {},
    onGuestLoginClicked: () -> Unit = {},
    onGoogleSignInClicked: () -> Unit = {},
    isLoginMusicEnabled: Boolean = true,
    onLoginMusicEnabledChange: (Boolean) -> Unit = {}
) {
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Login") },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            )
        },
        modifier = modifier.fillMaxSize()
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            ElevatedCard(
                elevation = CardDefaults.elevatedCardElevation(defaultElevation = 6.dp),
                modifier = Modifier.fillMaxWidth(0.95f)
            ) {
                Column(
                    modifier = Modifier
                        .padding(all = 24.dp)
                        .fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.meowlogo),
                        contentDescription = "App Logo",
                        modifier = Modifier
                            .fillMaxWidth(1f) 
                            .height(135.dp),
                        contentScale = ContentScale.FillBounds 
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Welcome Back!", style = MaterialTheme.typography.headlineMedium)
                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = username,
                        onValueChange = { username = it },
                        label = { Text("Username") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = { Text("Password") },
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Button(
                        onClick = { onLoginClicked(username, password) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Login")
                    }
                    TextButton(onClick = onForgotPasswordClicked) {
                        Text("Forgot Password?")
                    }
                }
            }
            Spacer(modifier = Modifier.height(24.dp))

            AndroidView(
                factory = { contextView -> // Renamed context to contextView to avoid clash
                    SignInButton(contextView).apply {
                        setSize(SignInButton.SIZE_WIDE)
                        setColorScheme(SignInButton.COLOR_AUTO)
                        setOnClickListener {
                            onGoogleSignInClicked()
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(0.95f)
            )

            Spacer(modifier = Modifier.height(16.dp))
            OutlinedButton(onClick = onSignUpClicked, modifier = Modifier.fillMaxWidth(0.95f)) {
                Text("Don't have an account? Sign Up")
            }
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedButton(onClick = onGuestLoginClicked, modifier = Modifier.fillMaxWidth(0.95f)) {
                Text("Login as Guest")
            }
            Spacer(modifier = Modifier.height(16.dp)) // Spacer before the toggle

            // Login Music Toggle
            Row(
                modifier = Modifier
                    .fillMaxWidth(0.95f)
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Enable Login Music", style = MaterialTheme.typography.bodyLarge)
                Switch(
                    checked = isLoginMusicEnabled,
                    onCheckedChange = onLoginMusicEnabledChange
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun LoginScreenPreview() {
    MeowbahTheme {
        LoginScreen(
            isLoginMusicEnabled = true,
            onLoginMusicEnabledChange = {}
        )
    }
}
