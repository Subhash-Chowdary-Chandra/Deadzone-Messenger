package com.deadzone.messenger.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.deadzone.messenger.ui.theme.AccentNeon
import com.deadzone.messenger.ui.theme.BackgroundDark

@Composable
fun OnboardingScreen(onGrantPermissionsClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundDark),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(32.dp)
        ) {
            Text(
                text = "Welcome to Deadzone",
                style = MaterialTheme.typography.headlineMedium,
                color = AccentNeon
            )
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = "To find nearby devices and share voice notes and photos without internet, Deadzone Messenger needs access to your Location, Bluetooth, Microphone, and Media Storage.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onBackground,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "We do NOT track your location remotely. This is strictly required by Android for offline peer-to-peer connections.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(32.dp))
            Button(
                onClick = onGrantPermissionsClick,
                colors = ButtonDefaults.buttonColors(containerColor = AccentNeon)
            ) {
                Text("Grant Permissions", color = BackgroundDark)
            }
        }
    }
}
