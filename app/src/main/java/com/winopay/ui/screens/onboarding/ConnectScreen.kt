package com.winopay.ui.screens.onboarding

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.ClickableText
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.solana.mobilewalletadapter.clientlib.ActivityResultSender
import com.winopay.R
import com.winopay.WinoPayApplication
import com.winopay.data.profile.MerchantProfileStore
import com.winopay.data.profile.RailConnection
import com.winopay.wallet.ReownManager
import com.winopay.wallet.UnifiedWalletConnector
import com.winopay.ui.components.PhosphorIcons
import com.winopay.ui.components.WinoButton
import com.winopay.ui.components.WinoButtonSize
import com.winopay.ui.components.WinoButtonVariant
import com.winopay.ui.theme.WinoRadius
import com.winopay.ui.theme.WinoSpacing
import com.winopay.ui.theme.WinoTheme
import com.winopay.ui.theme.WinoTypography
import kotlinx.coroutines.launch

private const val TAG = "ConnectScreen"

@Composable
fun ConnectScreen(
    activityResultSender: ActivityResultSender,
    onConnected: (publicKey: String) -> Unit,
    onTronConnect: () -> Unit = {},
    onBack: () -> Unit
) {
    val colors = WinoTheme.colors
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    val app = WinoPayApplication.instance
    val profileStore = remember { MerchantProfileStore(context) }

    // WalletConnect state
    val unifiedConnector = app.unifiedWalletConnector
    val wcSessionState by unifiedConnector.observeSessionState().collectAsState(initial = UnifiedWalletConnector.SessionState.Disconnected)

    // UI state
    var isConnecting by remember { mutableStateOf(false) }
    var connectionError by remember { mutableStateOf<String?>(null) }
    var connectionResult by remember { mutableStateOf<ConnectionResult?>(null) }
    var isOnboardingCompleted by remember { mutableStateOf<Boolean?>(null) }
    var termsAccepted by remember { mutableStateOf(false) }

    val uriHandler = LocalUriHandler.current

    // Wallet change detection state
    var showWalletChangeDialog by remember { mutableStateOf(false) }
    var pendingConnectionResult by remember { mutableStateOf<ConnectionResult?>(null) }
    var isClearingData by remember { mutableStateOf(false) }

    val isReownAvailable = remember { ReownManager.isAvailable() }

    // Check onboarding state
    LaunchedEffect(Unit) {
        isOnboardingCompleted = profileStore.isOnboardingCompleted()
        Log.d(TAG, "ONBOARD|INIT|completed=$isOnboardingCompleted")
    }

    // Handle late session approval (after timeout)
    // When session becomes Connected even after showing an error, recover and proceed
    LaunchedEffect(wcSessionState) {
        if (wcSessionState is UnifiedWalletConnector.SessionState.Connected && connectionError != null) {
            val connected = wcSessionState as UnifiedWalletConnector.SessionState.Connected
            Log.i(TAG, "WC|LATE_CONNECT|Recovering from timeout|rails=${connected.connectedRails.keys}")

            // Clear error and set success
            connectionError = null
            isConnecting = false
            connectionResult = ConnectionResult(
                success = true,
                walletName = connected.walletName,
                connectedRails = connected.connectedRails
            )
            isOnboardingCompleted = profileStore.isOnboardingCompleted()
        }
    }

    // Common result handler (must be defined first for other functions to use)
    fun handleConnectResult(result: UnifiedWalletConnector.ConnectResult, wasOnboarded: Boolean) {
        when (result) {
            is UnifiedWalletConnector.ConnectResult.Success -> {
                Log.i(TAG, "WC|CONNECT|SUCCESS|rails=${result.connectedRails.keys}|wallet=${result.walletName}")
                Log.i(TAG, "CONFIG|CONNECTED_RAILS|solana=${result.connectedRails["solana"]?.accountId?.take(8)}|tron=${result.connectedRails["tron"]?.accountId?.take(8)}|evm=${result.connectedRails["evm"]?.accountId?.take(10)}")

                // Check if wallet changed - show confirmation if addresses differ
                scope.launch {
                    val (hasExisting, addressesChanged) = unifiedConnector.checkWalletChange(result.connectedRails)
                    Log.i(TAG, "WC|WALLET_CHECK|hasExisting=$hasExisting|changed=$addressesChanged|wasOnboarded=$wasOnboarded")

                    if (hasExisting && addressesChanged && wasOnboarded) {
                        // Different wallet - show confirmation dialog
                        Log.i(TAG, "WC|WALLET_CHANGE|SHOW_DIALOG")
                        pendingConnectionResult = ConnectionResult(
                            success = true,
                            walletName = result.walletName,
                            connectedRails = result.connectedRails
                        )
                        showWalletChangeDialog = true
                    } else {
                        // Same wallet or new setup - proceed normally
                        connectionResult = ConnectionResult(
                            success = true,
                            walletName = result.walletName,
                            connectedRails = result.connectedRails
                        )
                        isOnboardingCompleted = wasOnboarded
                    }
                }
            }
            is UnifiedWalletConnector.ConnectResult.PartialSuccess -> {
                // Partial success: required rails connected, some optional rails (like TRON) may be missing
                Log.i(TAG, "WC|CONNECT|PARTIAL|rails=${result.connectedRails.keys}|failed=${result.failedRails}|wallet=${result.walletName}")
                Log.i(TAG, "CONFIG|CONNECTED_RAILS|solana=${result.connectedRails["solana"]?.accountId?.take(8)}|tron=${result.connectedRails["tron"]?.accountId?.take(8)}|evm=${result.connectedRails["evm"]?.accountId?.take(10)}")

                // Check if wallet changed
                scope.launch {
                    val (hasExisting, addressesChanged) = unifiedConnector.checkWalletChange(result.connectedRails)
                    Log.i(TAG, "WC|WALLET_CHECK|hasExisting=$hasExisting|changed=$addressesChanged|wasOnboarded=$wasOnboarded")

                    if (hasExisting && addressesChanged && wasOnboarded) {
                        // Different wallet - show confirmation dialog
                        Log.i(TAG, "WC|WALLET_CHANGE|SHOW_DIALOG")
                        pendingConnectionResult = ConnectionResult(
                            success = true,
                            walletName = result.walletName,
                            connectedRails = result.connectedRails
                        )
                        showWalletChangeDialog = true
                    } else {
                        // Same wallet or new setup - proceed normally
                        connectionResult = ConnectionResult(
                            success = true,
                            walletName = result.walletName,
                            connectedRails = result.connectedRails
                        )
                        isOnboardingCompleted = wasOnboarded
                    }
                }
            }
            is UnifiedWalletConnector.ConnectResult.Failure -> {
                Log.e(TAG, "WC|CONNECT|FAIL|error=${result.error}")
                connectionError = result.error
            }
            is UnifiedWalletConnector.ConnectResult.CryptoError -> {
                Log.e(TAG, "WC|CONNECT|CRYPTO_ERROR|error=${result.error}|needsReset=${result.needsHealthReset}")
                connectionError = "${result.error} Tap 'Connect Wallet' to retry."
            }
        }
    }

    // Handle wallet change confirmation
    fun confirmWalletChange() {
        scope.launch {
            Log.i(TAG, "WC|WALLET_CHANGE|CONFIRMED|clearing_data")
            isClearingData = true

            try {
                // Clear all previous data via logout
                app.logout()
                Log.i(TAG, "WC|WALLET_CHANGE|DATA_CLEARED")

                // Proceed with new connection
                connectionResult = pendingConnectionResult
                isOnboardingCompleted = false  // Force through onboarding with new wallet
                pendingConnectionResult = null
                showWalletChangeDialog = false
            } catch (e: Exception) {
                Log.e(TAG, "WC|WALLET_CHANGE|ERROR|${e.message}")
                connectionError = "Failed to clear data: ${e.message}"
                showWalletChangeDialog = false
            } finally {
                isClearingData = false
            }
        }
    }

    // Cancel wallet change - disconnect new session
    fun cancelWalletChange() {
        scope.launch {
            Log.i(TAG, "WC|WALLET_CHANGE|CANCELED|disconnecting_new_session")
            showWalletChangeDialog = false

            // Disconnect the new session
            pendingConnectionResult?.connectedRails?.values?.firstOrNull()?.sessionId?.let { sessionId ->
                unifiedConnector.disconnect(sessionId)
            }
            pendingConnectionResult = null

            // Show message
            connectionError = "Connection canceled. Your previous wallet data was preserved."
        }
    }

    // Connect ALL chains at once (Solana + TRON + EVM)
    fun connectAll() {
        scope.launch {
            Log.i(TAG, "WC|CONNECT|START|mode=ALL")
            isConnecting = true
            connectionError = null
            connectionResult = null

            val wasOnboarded = profileStore.isOnboardingCompleted()

            val result = unifiedConnector.connectAll()

            isConnecting = false

            handleConnectResult(result, wasOnboarded)
        }
    }

    // Continue after successful connection
    fun proceedAfterConnect() {
        val solanaAddress = connectionResult?.connectedRails?.get("solana")?.accountId ?: ""

        if (isOnboardingCompleted == true) {
            Log.i(TAG, "ONBOARD|NAV|action=back_to_settings|reason=already_onboarded")
            onBack()
        } else {
            Log.i(TAG, "ONBOARD|NAV|action=continue_onboarding|reason=new_user")
            onConnected(solanaAddress)
        }
    }

    // Wallet change confirmation dialog
    if (showWalletChangeDialog) {
        WalletChangeConfirmationDialog(
            newWalletName = pendingConnectionResult?.walletName,
            isClearing = isClearingData,
            onConfirm = { confirmWalletChange() },
            onCancel = { cancelWalletChange() }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.bgCanvas)
            .statusBarsPadding()
            .navigationBarsPadding()
            .verticalScroll(rememberScrollState())
    ) {
        // Header
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    start = WinoSpacing.LG,
                    end = WinoSpacing.LG,
                    top = WinoSpacing.MD,
                    bottom = WinoSpacing.MD
                ),
            verticalArrangement = Arrangement.spacedBy(WinoSpacing.XXS)
        ) {
            Text(
                text = stringResource(R.string.connect_wallet_title),
                style = WinoTypography.h1Medium,
                color = colors.textPrimary
            )

            Text(
                text = "Connect your multi-chain wallet to accept payments",
                style = WinoTypography.body,
                color = colors.textSecondary
            )
        }

        // Content
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(WinoSpacing.LG),
            verticalArrangement = Arrangement.spacedBy(WinoSpacing.MD)
        ) {
            // Error display
            connectionError?.let { error ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(WinoRadius.MD))
                        .background(colors.stateErrorSoft)
                        .padding(WinoSpacing.SM),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(WinoSpacing.SM)
                ) {
                    PhosphorIcons.Warning(size = 20.dp, color = colors.stateError)
                    Text(
                        text = error,
                        style = WinoTypography.small,
                        color = colors.stateError,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // Show connection result or connect button
            if (connectionResult != null) {
                // Connection successful - show what's connected
                ConnectionResultCard(
                    result = connectionResult!!,
                    onContinue = { proceedAfterConnect() }
                )
            } else if (isConnecting) {
                // Connecting state
                ConnectingState()
            } else {
                // Initial state - show terms checkbox and buttons
                if (!isReownAvailable) {
                    // Reown not configured
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(WinoRadius.MD))
                            .background(colors.stateWarningSoft)
                            .padding(WinoSpacing.SM),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(WinoSpacing.SM)
                    ) {
                        PhosphorIcons.Warning(size = 20.dp, color = colors.stateWarning)
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "WalletConnect not configured",
                                style = WinoTypography.smallMedium,
                                color = colors.textPrimary
                            )
                            Text(
                                text = "Set REOWN_PROJECT_ID in local.properties",
                                style = WinoTypography.micro,
                                color = colors.textSecondary
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.weight(1f))

                // Terms checkbox with link
                TermsCheckbox(
                    checked = termsAccepted,
                    onCheckedChange = { termsAccepted = it },
                    onLinkClick = { uriHandler.openUri("https://winobank.com/terms") }
                )

                Spacer(modifier = Modifier.height(WinoSpacing.LG))

                // Two buttons: How to get started (secondary) + Connect (primary)
                WinoButton(
                    text = "How to get started",
                    onClick = { uriHandler.openUri("https://winobank.com/guide") },
                    modifier = Modifier.fillMaxWidth(),
                    variant = WinoButtonVariant.Secondary,
                    size = WinoButtonSize.Large
                )

                Spacer(modifier = Modifier.height(WinoSpacing.SM))

                WinoButton(
                    text = "Connect",
                    onClick = { connectAll() },
                    modifier = Modifier.fillMaxWidth(),
                    variant = WinoButtonVariant.Primary,
                    size = WinoButtonSize.Large,
                    enabled = isReownAvailable && termsAccepted
                )
            }
        }
    }
}

/**
 * Terms checkbox with clickable link.
 */
@Composable
private fun TermsCheckbox(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    onLinkClick: () -> Unit
) {
    val colors = WinoTheme.colors

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(WinoSpacing.SM)
    ) {
        // Custom checkbox
        Box(
            modifier = Modifier
                .size(24.dp)
                .clip(RoundedCornerShape(WinoRadius.XS))
                .background(if (checked) colors.brandPrimary else colors.bgSurface)
                .border(
                    width = 2.dp,
                    color = if (checked) colors.brandPrimary else colors.borderDefault,
                    shape = RoundedCornerShape(WinoRadius.XS)
                ),
            contentAlignment = Alignment.Center
        ) {
            if (checked) {
                PhosphorIcons.Check(size = 16.dp, color = colors.textOnBrand)
            }
        }

        // Text with clickable link
        val annotatedString = buildAnnotatedString {
            append("I agree to the ")
            pushStringAnnotation(tag = "TERMS", annotation = "https://winobank.com/terms")
            withStyle(
                style = SpanStyle(
                    color = colors.brandPrimary,
                    textDecoration = TextDecoration.Underline
                )
            ) {
                append("Terms and Conditions")
            }
            pop()
        }

        ClickableText(
            text = annotatedString,
            style = WinoTypography.small.copy(color = colors.textSecondary),
            onClick = { offset ->
                annotatedString.getStringAnnotations(tag = "TERMS", start = offset, end = offset)
                    .firstOrNull()?.let {
                        onLinkClick()
                    }
            },
            modifier = Modifier.weight(1f)
        )
    }
}

/**
 * Connecting in progress state.
 */
@Composable
private fun ConnectingState() {
    val colors = WinoTheme.colors

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(WinoSpacing.XL),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(WinoSpacing.MD)
    ) {
        CircularProgressIndicator(
            color = colors.brandPrimary,
            modifier = Modifier.size(48.dp)
        )
        Text(
            text = "Connecting to wallet...",
            style = WinoTypography.bodyMedium,
            color = colors.textPrimary
        )
        Text(
            text = "Approve the connection in your wallet app",
            style = WinoTypography.small,
            color = colors.textMuted,
            textAlign = TextAlign.Center
        )
    }
}

/**
 * Connection result card showing which networks connected.
 * Shows "Add Solana" / "Add TRON" buttons for staged connect.
 */
@Composable
private fun ConnectionResultCard(
    result: ConnectionResult,
    onContinue: () -> Unit
) {
    val colors = WinoTheme.colors

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(WinoRadius.LG))
            .background(colors.stateSuccessSoft)
            .padding(WinoSpacing.MD),
        verticalArrangement = Arrangement.spacedBy(WinoSpacing.MD)
    ) {
        // Success header
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(WinoSpacing.SM)
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(colors.stateSuccess),
                contentAlignment = Alignment.Center
            ) {
                PhosphorIcons.CheckCircle(
                    size = 24.dp,
                    color = colors.bgCanvas
                )
            }

            Column {
                Text(
                    text = "Wallet Connected",
                    style = WinoTypography.h3Medium,
                    color = colors.stateSuccess
                )
                result.walletName?.let {
                    Text(
                        text = it,
                        style = WinoTypography.small,
                        color = colors.textSecondary
                    )
                }
            }
        }

        // Networks status
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(WinoRadius.MD))
                .background(colors.bgCanvas)
                .padding(WinoSpacing.SM),
            verticalArrangement = Arrangement.spacedBy(WinoSpacing.XS)
        ) {
            NetworkStatusRow(
                name = "Solana",
                isConnected = result.connectedRails.containsKey("solana"),
                address = result.connectedRails["solana"]?.accountId
            )
            NetworkStatusRow(
                name = "TRON",
                isConnected = result.connectedRails.containsKey("tron"),
                address = result.connectedRails["tron"]?.accountId
            )
            NetworkStatusRow(
                name = "BSC",
                isConnected = result.connectedRails.containsKey("evm"),
                address = result.connectedRails["evm"]?.accountId
            )
        }

        // Continue button
        WinoButton(
            text = "Continue",
            onClick = onContinue,
            modifier = Modifier.fillMaxWidth(),
            variant = WinoButtonVariant.Primary,
            size = WinoButtonSize.Large
        )
    }
}

@Composable
private fun NetworkStatusRow(
    name: String,
    isConnected: Boolean,
    address: String?
) {
    val colors = WinoTheme.colors

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(WinoSpacing.SM)
    ) {
        // Status icon
        if (isConnected) {
            PhosphorIcons.CheckCircle(size = 20.dp, color = colors.stateSuccess)
        } else {
            PhosphorIcons.XCircle(size = 20.dp, color = colors.textMuted)
        }

        // Network name
        Text(
            text = name,
            style = WinoTypography.bodyMedium,
            color = if (isConnected) colors.textPrimary else colors.textMuted,
            modifier = Modifier.weight(1f)
        )

        // Address (truncated)
        if (isConnected && address != null) {
            Text(
                text = formatAddress(address),
                style = WinoTypography.micro,
                color = colors.textSecondary
            )
        } else if (!isConnected) {
            Text(
                text = "Not supported",
                style = WinoTypography.micro,
                color = colors.textMuted
            )
        }
    }
}

private fun formatAddress(address: String): String {
    return if (address.length > 12) {
        "${address.take(6)}...${address.takeLast(4)}"
    } else {
        address
    }
}

/**
 * Data class for connection result.
 */
private data class ConnectionResult(
    val success: Boolean,
    val walletName: String?,
    val connectedRails: Map<String, RailConnection>
)

/**
 * Confirmation dialog when connecting a different wallet.
 * Warns user that all previous transaction data will be deleted.
 */
@Composable
private fun WalletChangeConfirmationDialog(
    newWalletName: String?,
    isClearing: Boolean,
    onConfirm: () -> Unit,
    onCancel: () -> Unit
) {
    val colors = WinoTheme.colors

    AlertDialog(
        onDismissRequest = { /* BLOCKING - cannot dismiss during clearing */ },
        containerColor = colors.bgSurface,
        titleContentColor = colors.textPrimary,
        textContentColor = colors.textSecondary,
        icon = {
            PhosphorIcons.Warning(size = 32.dp, color = colors.stateWarning)
        },
        title = {
            Text(
                text = "Different wallet detected",
                style = WinoTypography.h3Medium
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(WinoSpacing.SM)) {
                Text(
                    text = "You are connecting a different wallet${newWalletName?.let { " ($it)" } ?: ""}.",
                    style = WinoTypography.body
                )
                Text(
                    text = "This will delete all previous transaction history and payment data.",
                    style = WinoTypography.bodyMedium,
                    color = colors.stateError
                )
                Text(
                    text = "Are you sure you want to continue?",
                    style = WinoTypography.body
                )
            }
        },
        confirmButton = {
            if (isClearing) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(WinoSpacing.XS),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        color = colors.brandPrimary,
                        strokeWidth = 2.dp
                    )
                    Text(
                        text = "Clearing...",
                        style = WinoTypography.bodyMedium,
                        color = colors.textSecondary
                    )
                }
            } else {
                TextButton(onClick = onConfirm) {
                    Text(
                        text = "Delete & Continue",
                        color = colors.stateError
                    )
                }
            }
        },
        dismissButton = {
            if (!isClearing) {
                TextButton(onClick = onCancel) {
                    Text(
                        text = "Cancel",
                        color = colors.textSecondary
                    )
                }
            }
        }
    )
}
