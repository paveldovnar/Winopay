package com.winopay.update

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import kotlinx.coroutines.launch

/**
 * Dialog for app update notification and download.
 */
@Composable
fun UpdateDialog(
    updateInfo: UpdateChecker.UpdateInfo,
    onDismiss: () -> Unit
) {
    var downloadProgress by remember { mutableStateOf<ApkDownloader.DownloadProgress?>(null) }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

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
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Title
                Text(
                    text = "Update Available",
                    style = MaterialTheme.typography.headlineSmall
                )

                // Version info
                Text(
                    text = "New version ${updateInfo.versionName} is available",
                    style = MaterialTheme.typography.bodyMedium
                )

                // Size info
                val sizeInMB = updateInfo.fileSize / (1024f * 1024f)
                Text(
                    text = "Size: %.1f MB".format(sizeInMB),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // Release notes
                if (updateInfo.releaseNotes.isNotBlank()) {
                    Text(
                        text = updateInfo.releaseNotes,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 3
                    )
                }

                // Download progress
                when (val progress = downloadProgress) {
                    is ApkDownloader.DownloadProgress.Started -> {
                        CircularProgressIndicator(
                            modifier = Modifier.align(Alignment.CenterHorizontally)
                        )
                        Text("Starting download...")
                    }
                    is ApkDownloader.DownloadProgress.Progress -> {
                        val percent = (progress.downloaded.toFloat() / progress.total * 100).toInt()
                        LinearProgressIndicator(
                            progress = { progress.downloaded.toFloat() / progress.total },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Text("Downloading... $percent%")
                    }
                    is ApkDownloader.DownloadProgress.Success -> {
                        Text("Download complete! Opening installer...")
                    }
                    is ApkDownloader.DownloadProgress.Error -> {
                        Text(
                            text = "Error: ${progress.message}",
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                    null -> {
                        // Not downloading yet
                    }
                }

                // Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (downloadProgress !is ApkDownloader.DownloadProgress.Started &&
                        downloadProgress !is ApkDownloader.DownloadProgress.Progress) {
                        
                        OutlinedButton(
                            onClick = onDismiss,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Later")
                        }

                        Button(
                            onClick = {
                                scope.launch {
                                    ApkDownloader.downloadApk(
                                        context = context,
                                        url = updateInfo.downloadUrl,
                                        fileName = "winopay-update.apk"
                                    ).collect { progress ->
                                        downloadProgress = progress
                                        
                                        // Auto-install when download completes
                                        if (progress is ApkDownloader.DownloadProgress.Success) {
                                            if (ApkInstaller.canInstallApks(context)) {
                                                ApkInstaller.installApk(context, progress.file)
                                                onDismiss()
                                            } else {
                                                ApkInstaller.openInstallSettings(context)
                                            }
                                        }
                                    }
                                }
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Update")
                        }
                    }
                }
            }
        }
    }
}
