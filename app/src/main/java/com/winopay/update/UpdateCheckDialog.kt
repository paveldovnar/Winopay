package com.winopay.update

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog

/**
 * Dialog showing update check status (checking, no updates, error).
 */
@Composable
fun UpdateCheckDialog(
    isChecking: Boolean,
    error: String?,
    showNoUpdate: Boolean = false,
    onDismiss: () -> Unit
) {
    if (!isChecking && error == null && !showNoUpdate) {
        // Nothing to show
        return
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                when {
                    isChecking -> {
                        CircularProgressIndicator()
                        Text(
                            text = "Checking for updates...",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                    error != null -> {
                        Text(
                            text = "Error",
                            style = MaterialTheme.typography.headlineSmall,
                            color = MaterialTheme.colorScheme.error
                        )
                        Text(
                            text = error,
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Button(onClick = onDismiss) {
                            Text("OK")
                        }
                    }
                    else -> {
                        Text(
                            text = "You're up to date!",
                            style = MaterialTheme.typography.headlineSmall
                        )
                        Text(
                            text = "You have the latest version",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Button(onClick = onDismiss) {
                            Text("OK")
                        }
                    }
                }
            }
        }
    }
}
