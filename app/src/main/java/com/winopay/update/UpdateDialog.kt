package com.winopay.update

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.winopay.ui.theme.WinoSpacing
import com.winopay.ui.theme.WinoTheme
import com.winopay.ui.theme.WinoTypography
import kotlinx.coroutines.launch

/**
 * Update checker dialog with download and install flow.
 *
 * FLOW:
 * 1. Show "Checking..." while fetching manifest
 * 2. If update available → show "Update available" with version
 * 3. On "Download" → show progress bar
 * 4. On complete → verify SHA-256 → launch installer
 * 5. If no update / error → show message
 */
@Composable
fun UpdateDialog(
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val colors = WinoTheme.colors

    var state by remember { mutableStateOf<UpdateState>(UpdateState.Checking) }
    var downloadProgress by remember { mutableIntStateOf(0) }

    // Auto-check on mount
    LaunchedEffect(Unit) {
        scope.launch {
            when (val result = UpdateChecker.checkForUpdate(context)) {
                is UpdateChecker.UpdateCheckResult.UpdateAvailable -> {
                    state = UpdateState.Available(result.manifest)
                }
                is UpdateChecker.UpdateCheckResult.NoUpdateAvailable -> {
                    state = UpdateState.NoUpdate
                }
                is UpdateChecker.UpdateCheckResult.Error -> {
                    state = UpdateState.Error(result.message)
                }
            }
        }
    }

    when (val currentState = state) {
        is UpdateState.Checking -> {
            AlertDialog(
                onDismissRequest = { /* Block dismiss while checking */ },
                containerColor = colors.bgSurface,
                titleContentColor = colors.textPrimary,
                textContentColor = colors.textSecondary,
                title = {
                    Text("Checking for updates", style = WinoTypography.h3Medium)
                },
                text = {
                    Text("Please wait...", style = WinoTypography.body)
                },
                confirmButton = {}
            )
        }

        is UpdateState.Available -> {
            AlertDialog(
                onDismissRequest = onDismiss,
                containerColor = colors.bgSurface,
                titleContentColor = colors.textPrimary,
                textContentColor = colors.textSecondary,
                title = {
                    Text("Update available", style = WinoTypography.h3Medium)
                },
                text = {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(WinoSpacing.XS)
                    ) {
                        Text(
                            "Version ${currentState.manifest.versionName} is available.",
                            style = WinoTypography.body
                        )
                        Text(
                            "Current: ${com.winopay.BuildConfig.VERSION_NAME}",
                            style = WinoTypography.small,
                            color = colors.textMuted
                        )
                    }
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            scope.launch {
                                state = UpdateState.Downloading
                                when (val result = UpdateChecker.downloadAndVerifyApk(
                                    context,
                                    currentState.manifest
                                ) { progress ->
                                    downloadProgress = progress
                                }) {
                                    is UpdateChecker.DownloadResult.Success -> {
                                        // Launch installer
                                        UpdateChecker.launchInstaller(context, result.apkFile)
                                        onDismiss()
                                    }
                                    is UpdateChecker.DownloadResult.Error -> {
                                        state = UpdateState.Error(result.message)
                                    }
                                }
                            }
                        }
                    ) {
                        Text("Download", color = colors.brandPrimary, style = WinoTypography.bodyMedium)
                    }
                },
                dismissButton = {
                    TextButton(onClick = onDismiss) {
                        Text("Later", color = colors.textTertiary, style = WinoTypography.bodyMedium)
                    }
                }
            )
        }

        is UpdateState.Downloading -> {
            AlertDialog(
                onDismissRequest = { /* Block dismiss while downloading */ },
                containerColor = colors.bgSurface,
                titleContentColor = colors.textPrimary,
                textContentColor = colors.textSecondary,
                title = {
                    Text("Downloading update", style = WinoTypography.h3Medium)
                },
                text = {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(WinoSpacing.SM)
                    ) {
                        Text(
                            "Downloading and verifying... $downloadProgress%",
                            style = WinoTypography.body
                        )
                        LinearProgressIndicator(
                            progress = { downloadProgress / 100f },
                            modifier = Modifier.fillMaxWidth(),
                            color = colors.brandPrimary,
                        )
                    }
                },
                confirmButton = {}
            )
        }

        is UpdateState.NoUpdate -> {
            AlertDialog(
                onDismissRequest = onDismiss,
                containerColor = colors.bgSurface,
                titleContentColor = colors.textPrimary,
                textContentColor = colors.textSecondary,
                title = {
                    Text("No update available", style = WinoTypography.h3Medium)
                },
                text = {
                    Text(
                        "You're running the latest version (${com.winopay.BuildConfig.VERSION_NAME}).",
                        style = WinoTypography.body
                    )
                },
                confirmButton = {
                    TextButton(onClick = onDismiss) {
                        Text("OK", color = colors.brandPrimary, style = WinoTypography.bodyMedium)
                    }
                }
            )
        }

        is UpdateState.Error -> {
            AlertDialog(
                onDismissRequest = onDismiss,
                containerColor = colors.bgSurface,
                titleContentColor = colors.textPrimary,
                textContentColor = colors.textSecondary,
                title = {
                    Text("Update check failed", style = WinoTypography.h3Medium)
                },
                text = {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(WinoSpacing.XS)
                    ) {
                        Text(currentState.message, style = WinoTypography.body)
                        Text(
                            "Make sure UPDATE_ENDPOINT is configured in build.gradle.",
                            style = WinoTypography.small,
                            color = colors.textMuted
                        )
                    }
                },
                confirmButton = {
                    TextButton(onClick = onDismiss) {
                        Text("OK", color = colors.brandPrimary, style = WinoTypography.bodyMedium)
                    }
                }
            )
        }
    }
}

private sealed class UpdateState {
    data object Checking : UpdateState()
    data class Available(val manifest: UpdateChecker.UpdateManifest) : UpdateState()
    data object Downloading : UpdateState()
    data object NoUpdate : UpdateState()
    data class Error(val message: String) : UpdateState()
}
