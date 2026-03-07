package com.winopay.update

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.viewmodel.compose.viewModel
import com.winopay.BuildConfig

/**
 * Integration example for app update checker.
 *
 * Add this composable to your main screen to enable automatic update checks.
 *
 * Usage in MainActivity or App composable:
 *
 *   @Composable
 *   fun AppContent() {
 *       // ... your app content
 *       
 *       // Add update checker
 *       AppUpdateChecker()
 *   }
 *
 * The update checker will:
 * 1. Automatically check for updates on app start (if credentials configured)
 * 2. Show update dialog if newer version available
 * 3. Allow user to download and install update
 *
 * Configuration:
 * - Set TG_BOT_TOKEN in local.properties or .env
 * - Set TG_CHAT_ID in local.properties or .env
 * - Or configure in build.gradle.kts defaultConfig
 */
@Composable
fun AppUpdateChecker(
    updateViewModel: UpdateViewModel = viewModel(),
    autoCheck: Boolean = true
) {
    val updateInfo by updateViewModel.updateInfo.collectAsState()

    // Auto-check on startup
    LaunchedEffect(Unit) {
        if (autoCheck &&
            BuildConfig.TG_BOT_TOKEN.isNotBlank() &&
            BuildConfig.TG_CHAT_ID.isNotBlank()) {
            
            updateViewModel.checkForUpdates(
                botToken = BuildConfig.TG_BOT_TOKEN,
                chatUsername = BuildConfig.TG_CHAT_ID
            )
        }
    }

    // Show update dialog if available
    updateInfo?.let { info ->
        UpdateDialog(
            updateInfo = info,
            onDismiss = { updateViewModel.dismissUpdate() }
        )
    }
}

/**
 * Manual update check button.
 *
 * Usage:
 *   Button(onClick = { viewModel.checkForUpdates() }) {
 *       Text("Check for updates")
 *   }
 */
fun UpdateViewModel.checkForUpdates() {
    if (BuildConfig.TG_BOT_TOKEN.isNotBlank() && BuildConfig.TG_CHAT_ID.isNotBlank()) {
        checkForUpdates(
            botToken = BuildConfig.TG_BOT_TOKEN,
            chatUsername = BuildConfig.TG_CHAT_ID
        )
    }
}
