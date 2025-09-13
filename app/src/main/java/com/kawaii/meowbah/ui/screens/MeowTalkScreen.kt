package com.kawaii.meowbah.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
// import androidx.compose.material3.TopAppBar // Removed TopAppBar import
import androidx.compose.material3.ExperimentalMaterial3Api // Kept for Scaffold if needed
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController

@OptIn(ExperimentalMaterial3Api::class) // Kept for Scaffold if it uses experimental APIs
@Composable
fun MeowTalkScreen(
    navController: NavController, // Kept for future navigation if needed from this screen
    meowTalkViewModel: MeowTalkViewModel = viewModel()
) {
    val currentPhrase by meowTalkViewModel.currentPhrase.collectAsState()

    Scaffold(
        // topBar = { TopAppBar(title = { Text("MeowTalk") }) } // TopAppBar removed
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues) // Apply padding from Scaffold
                .padding(16.dp), // Add overall padding for content
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = currentPhrase,
                style = MaterialTheme.typography.headlineMedium,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(bottom = 24.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Button(onClick = { meowTalkViewModel.userRequestedNewPhrase() }) {
                Text("New MeowTalk")
            }
        }
    }
}
