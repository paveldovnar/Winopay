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
import androidx.compose.material3.Text
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.solana.mobilewalletadapter.clientlib.ActivityResultSender
import com.winopay.R
import com.winopay.WinoPayApplication
import com.winopay.i18n.L
import com.winopay.data.profile.MerchantProfileStore
import com.winopay.data.profile.RailConnection
import com.winopay.wallet.ReownManager
import com.winopay.wallet.UnifiedWalletConnector
import com.winopay.ui.components.BlurredGradientOverlay
import com.winopay.ui.components.PhosphorIcons
import com.winopay.ui.components.WinoButton
import com.winopay.ui.components.WinoButtonSize
import com.winopay.ui.components.WinoButtonVariant
import com.winopay.ui.components.WinoCheckBox
import com.winopay.ui.theme.WinoRadius
import com.winopay.ui.theme.WinoSpacing
import com.winopay.ui.theme.WinoTheme
import com.winopay.ui.theme.WinoTypography
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private const val TAG = "ConnectScreen"

/**
 * Connection states for the wallet connect flow.
 * Based on Figma designs (nodes 1343:13896, 14199, 14142, 14226, 14237, 14252, 14297, 14306)
 */
private sealed class ConnectState {
    /** Main screen - connect wallet (1343:13896) */
    data object Initial : ConnectState()

    /** Connecting in progress (1343:14199) */
    data object Connecting : ConnectState()

    /** Wallet connected success (1343:14142) - purple fullscreen, auto-dismiss */
    data class Success(val walletName: String?, val connectedRails: Map<String, RailConnection>) : ConnectState()

    /** Connection timed out (1343:14226) */
    data object Timeout : ConnectState()

    /** Something went wrong error (1343:14237) */
    data class Error(val message: String) : ConnectState()

    /** Different wallet detected (1343:14252) - requires confirmation */
    data class WalletChange(
        val newWalletName: String?,
        val oldAddresses: Map<String, String>,
        val newAddresses: Map<String, String>,
        val isClearing: Boolean
    ) : ConnectState()

    /** Session expired (1343:14297) - needs reconnection */
    data object SessionExpired : ConnectState()

    /** Legal documents viewer (1343:14306) */
    data object LegalDocs : ConnectState()
}

/**
 * Result of wallet connection
 */
private data class ConnectionResult(
    val success: Boolean,
    val walletName: String?,
    val connectedRails: Map<String, RailConnection>
)

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
    var currentState by remember { mutableStateOf<ConnectState>(ConnectState.Initial) }
    var connectionResult by remember { mutableStateOf<ConnectionResult?>(null) }
    var isOnboardingCompleted by remember { mutableStateOf<Boolean?>(null) }
    var termsAccepted by remember { mutableStateOf(false) }
    var pendingConnectionResult by remember { mutableStateOf<ConnectionResult?>(null) }
    var timeoutElapsed by remember { mutableStateOf(false) }

    val uriHandler = LocalUriHandler.current
    // Don't cache - re-evaluate on each recomposition so button updates after SDK state changes
    val isReownAvailable = ReownManager.isAvailable()

    // Check onboarding state
    LaunchedEffect(Unit) {
        isOnboardingCompleted = profileStore.isOnboardingCompleted()
        Log.d(TAG, "ONBOARD|INIT|completed=$isOnboardingCompleted")
    }

    // Handle late session approval (after timeout)
    LaunchedEffect(wcSessionState) {
        if (wcSessionState is UnifiedWalletConnector.SessionState.Connected && currentState is ConnectState.Timeout) {
            val connected = wcSessionState as UnifiedWalletConnector.SessionState.Connected
            Log.i(TAG, "WC|LATE_CONNECT|Recovering from timeout|rails=${connected.connectedRails.keys}")

            // Clear timeout and set success
            currentState = ConnectState.Success(
                walletName = connected.walletName,
                connectedRails = connected.connectedRails
            )
            connectionResult = ConnectionResult(
                success = true,
                walletName = connected.walletName,
                connectedRails = connected.connectedRails
            )
        }
    }

    // Timeout timer for connecting state
    LaunchedEffect(currentState) {
        if (currentState is ConnectState.Connecting) {
            timeoutElapsed = false
            delay(90_000) // 90 seconds as specified in Figma
            if (currentState is ConnectState.Connecting) {
                Log.i(TAG, "WC|TIMEOUT|Exceeded 90 seconds")
                timeoutElapsed = true
                currentState = ConnectState.Timeout
            }
        }
    }

    // Proceed after successful connection
    fun proceedAfterConnect(
        successState: ConnectState.Success,
        wasOnboarded: Boolean?,
        onConnectedCallback: (String) -> Unit,
        onBackCallback: () -> Unit
    ) {
        val solanaAddress = successState.connectedRails["solana"]?.accountId ?: ""

        if (wasOnboarded == true) {
            Log.i(TAG, "ONBOARD|NAV|action=back_to_settings|reason=already_onboarded")
            onBackCallback()
        } else {
            Log.i(TAG, "ONBOARD|NAV|action=continue_onboarding|reason=new_user")
            onConnectedCallback(solanaAddress)
        }
    }

    // Auto-dismiss success screen after 2 seconds
    LaunchedEffect(currentState) {
        if (currentState is ConnectState.Success) {
            delay(2000)
            proceedAfterConnect(currentState as ConnectState.Success, isOnboardingCompleted, onConnected, onBack)
        }
    }

    // Common result handler
    fun handleConnectResult(result: UnifiedWalletConnector.ConnectResult, wasOnboarded: Boolean) {
        when (result) {
            is UnifiedWalletConnector.ConnectResult.Success -> {
                Log.i(TAG, "WC|CONNECT|SUCCESS|rails=${result.connectedRails.keys}|wallet=${result.walletName}")
                Log.i(TAG, "CONFIG|CONNECTED_RAILS|solana=${result.connectedRails["solana"]?.accountId?.take(8)}|tron=${result.connectedRails["tron"]?.accountId?.take(8)}|evm=${result.connectedRails["evm"]?.accountId?.take(10)}")

                // Check if wallet changed
                scope.launch {
                    val (hasExisting, addressesChanged) = unifiedConnector.checkWalletChange(result.connectedRails)
                    Log.i(TAG, "WC|WALLET_CHECK|hasExisting=$hasExisting|changed=$addressesChanged|wasOnboarded=$wasOnboarded")

                    if (hasExisting && addressesChanged && wasOnboarded) {
                        // Different wallet - show confirmation
                        val oldAddresses = unifiedConnector.getExistingWalletInfo()
                        val newAddresses = result.connectedRails.mapValues { it.value.accountId }

                        pendingConnectionResult = ConnectionResult(
                            success = true,
                            walletName = result.walletName,
                            connectedRails = result.connectedRails
                        )
                        currentState = ConnectState.WalletChange(
                            newWalletName = result.walletName,
                            oldAddresses = oldAddresses,
                            newAddresses = newAddresses,
                            isClearing = false
                        )
                    } else {
                        // Same wallet or new setup - show success
                        currentState = ConnectState.Success(
                            walletName = result.walletName,
                            connectedRails = result.connectedRails
                        )
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
                Log.i(TAG, "WC|CONNECT|PARTIAL|rails=${result.connectedRails.keys}|failed=${result.failedRails}|wallet=${result.walletName}")

                // Treat partial success same as success
                scope.launch {
                    val (hasExisting, addressesChanged) = unifiedConnector.checkWalletChange(result.connectedRails)

                    if (hasExisting && addressesChanged && wasOnboarded) {
                        val oldAddresses = unifiedConnector.getExistingWalletInfo()
                        val newAddresses = result.connectedRails.mapValues { it.value.accountId }

                        pendingConnectionResult = ConnectionResult(
                            success = true,
                            walletName = result.walletName,
                            connectedRails = result.connectedRails
                        )
                        currentState = ConnectState.WalletChange(
                            newWalletName = result.walletName,
                            oldAddresses = oldAddresses,
                            newAddresses = newAddresses,
                            isClearing = false
                        )
                    } else {
                        currentState = ConnectState.Success(
                            walletName = result.walletName,
                            connectedRails = result.connectedRails
                        )
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
                currentState = ConnectState.Error(result.error)
            }
            is UnifiedWalletConnector.ConnectResult.CryptoError -> {
                Log.e(TAG, "WC|CONNECT|CRYPTO_ERROR|error=${result.error}|needsReset=${result.needsHealthReset}")
                currentState = ConnectState.Error("${result.error} Please try again.")
            }
        }
    }

    // Connect wallet
    fun connectWallet() {
        scope.launch {
            Log.i(TAG, "WC|CONNECT|START|mode=ALL")
            currentState = ConnectState.Connecting
            connectionResult = null

            val wasOnboarded = profileStore.isOnboardingCompleted()
            val result = unifiedConnector.connectAll()

            // Don't change state here if timeout already triggered
            if (!timeoutElapsed) {
                handleConnectResult(result, wasOnboarded)
            } else {
                Log.i(TAG, "WC|CONNECT|RESULT_IGNORED|reason=timeout_already_shown")
            }
        }
    }

    // Retry connection
    fun retryConnection() {
        currentState = ConnectState.Initial
        connectWallet()
    }

    // Cancel connection
    fun cancelConnection() {
        currentState = ConnectState.Initial
    }

    // Confirm wallet change
    fun confirmWalletChange() {
        scope.launch {
            Log.i(TAG, "WC|WALLET_CHANGE|CONFIRMED|clearing_data")
            currentState = (currentState as ConnectState.WalletChange).copy(isClearing = true)

            try {
                app.logout()
                Log.i(TAG, "WC|WALLET_CHANGE|DATA_CLEARED")

                connectionResult = pendingConnectionResult
                isOnboardingCompleted = false

                currentState = ConnectState.Success(
                    walletName = pendingConnectionResult?.walletName,
                    connectedRails = pendingConnectionResult?.connectedRails ?: emptyMap()
                )

                pendingConnectionResult = null
            } catch (e: Exception) {
                Log.e(TAG, "WC|WALLET_CHANGE|ERROR|${e.message}")
                currentState = ConnectState.Error("Failed to clear data: ${e.message}")
            }
        }
    }

    // Cancel wallet change
    fun cancelWalletChange() {
        scope.launch {
            Log.i(TAG, "WC|WALLET_CHANGE|CANCELED|disconnecting_new_session")

            pendingConnectionResult?.connectedRails?.values?.firstOrNull()?.sessionId?.let { sessionId ->
                unifiedConnector.disconnect(sessionId)
            }
            pendingConnectionResult = null

            currentState = ConnectState.Initial
        }
    }

    // Render appropriate screen based on state
    when (val state = currentState) {
        is ConnectState.Initial -> {
            InitialConnectScreen(
                termsAccepted = termsAccepted,
                onTermsChange = { termsAccepted = it },
                onTermsLinkClick = { currentState = ConnectState.LegalDocs },
                onConnectClick = { connectWallet() },
                isReownAvailable = isReownAvailable,
                onBack = onBack
            )
        }
        is ConnectState.Connecting -> {
            ConnectingScreen(
                onCancel = { cancelConnection() }
            )
        }
        is ConnectState.Success -> {
            SuccessScreen()
        }
        is ConnectState.Timeout -> {
            TimeoutScreen(
                onTryAgain = { retryConnection() },
                onCancel = { cancelConnection() }
            )
        }
        is ConnectState.Error -> {
            ErrorScreen(
                message = state.message,
                onTryAgain = { retryConnection() },
                onCancel = { cancelConnection() },
                onGetHelp = { uriHandler.openUri("https://t.me/Winobank_support") }
            )
        }
        is ConnectState.WalletChange -> {
            WalletChangeScreen(
                newWalletName = state.newWalletName,
                oldAddresses = state.oldAddresses,
                newAddresses = state.newAddresses,
                isClearing = state.isClearing,
                onConfirm = { confirmWalletChange() },
                onCancel = { cancelWalletChange() }
            )
        }
        is ConnectState.SessionExpired -> {
            SessionExpiredScreen(
                onReconnect = { retryConnection() }
            )
        }
        is ConnectState.LegalDocs -> {
            LegalDocsScreen(
                onClose = { currentState = ConnectState.Initial }
            )
        }
    }
}

/**
 * Initial connect screen (Figma 1343:13896)
 * Figma structure (from reference code):
 * - Main: flex flex-col justify-between (Column with SpaceBetween)
 * - Header: pt-[60px] (safe-area/up)
 * - Middle: flex-[1_0_0] (weight(1f))
 * - Bottom: pb-[16px] (safe-area/bottom)
 *   - Helper: gap-[16px] px-[32px] py-[16px]
 *   - Buttons: gap-[12px] p-[24px]
 */
@Composable
private fun InitialConnectScreen(
    termsAccepted: Boolean,
    onTermsChange: (Boolean) -> Unit,
    onTermsLinkClick: () -> Unit,
    onConnectClick: () -> Unit,
    isReownAvailable: Boolean,
    onBack: () -> Unit
) {
    val colors = WinoTheme.colors

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.bgCanvas)
    ) {
        // Main content
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // Header - pt-[60px]
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = WinoSpacing.XL) // Safe area = 32dp per Figma
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(
                            horizontal = 24.dp,  // spacing/lg
                            vertical = 16.dp      // spacing/md
                        ),
                    verticalArrangement = Arrangement.spacedBy(4.dp)  // spacing/xxs
                ) {
                    Text(
                        text = L("onboarding.connect_wallet.title"),
                        style = WinoTypography.h1Medium,
                        color = colors.textPrimary
                    )

                    Text(
                        text = L("onboarding.connect_wallet.subtitle"),
                        style = WinoTypography.body,
                        color = colors.textSecondary
                    )
                }
            }

            // Middle spacer
            Spacer(modifier = Modifier.weight(1f))

            // Spacer for bottom overlay
            Spacer(modifier = Modifier.height(200.dp))
        }

        // Bottom with gradient overlay
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
        ) {
            BlurredGradientOverlay(canvasColor = colors.bgCanvas) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp)  // spacing/safe-area/bottom
                ) {
                    // Helper section: gap-[16px] px-[32px] py-[16px]
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(
                                horizontal = 32.dp,  // spacing/xl
                                vertical = 16.dp      // spacing/md
                            ),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),  // spacing/md
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        WinoCheckBox(
                            checked = termsAccepted,
                            onCheckedChange = onTermsChange
                        )

                        val annotatedText = buildAnnotatedString {
                            append(L("onboarding.connect_wallet.terms_agree"))
                            pushStringAnnotation(tag = "TERMS", annotation = "terms")
                            withStyle(
                                style = SpanStyle(
                                    color = colors.brandPrimary,
                                    textDecoration = TextDecoration.Underline
                                )
                            ) {
                                append(L("onboarding.connect_wallet.terms_link"))
                            }
                            pop()
                        }

                        ClickableText(
                            text = annotatedText,
                            style = WinoTypography.body.copy(color = colors.textPrimary),
                            onClick = { offset ->
                                annotatedText.getStringAnnotations(tag = "TERMS", start = offset, end = offset)
                                    .firstOrNull()?.let {
                                        onTermsLinkClick()
                                    }
                            },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    // Button section: gap-[12px] p-[24px]
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),  // spacing/lg
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        WinoButton(
                            text = L("common.connect"),
                            onClick = onConnectClick,
                            modifier = Modifier.fillMaxWidth(),
                            enabled = termsAccepted && isReownAvailable,
                            size = WinoButtonSize.Large
                        )
                    }
                }
            }
        }
    }
}

/**
 * Connecting screen (Figma 1343:14199)
 * Figma structure (from reference code):
 * - Main: flex flex-col gap-[40px] justify-center (Column centered)
 * - Icon: 96x96 rounded-[36px] bg-surface
 * - Text: gap-[8px] px-[24px]
 * - Bottom: absolute positioned, backdrop-blur, gradient
 *   - Helper: px-[32px] py-[24px/16px]
 *   - Button: p-[24px]
 */
@Composable
private fun ConnectingScreen(
    onCancel: () -> Unit
) {
    val colors = WinoTheme.colors

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.bgCanvas)
    ) {
        // Main content - centered
        Column(
            modifier = Modifier
                .fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(40.dp, Alignment.CenterVertically)
        ) {
            // Icon
            Box(
                modifier = Modifier
                    .size(96.dp)
                    .clip(RoundedCornerShape(36.dp))  // radius/xl
                    .background(colors.bgSurface),
                contentAlignment = Alignment.Center
            ) {
                PhosphorIcons.ArrowsClockwise(
                    size = 48.dp,
                    color = colors.textPrimary,
                    strokeWidth = 2f
                )
            }

            // Text section
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),  // spacing/lg
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)  // spacing/xs
            ) {
                Text(
                    text = L("onboarding.connecting.title"),
                    style = WinoTypography.h1Medium,
                    color = colors.textPrimary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )

                Text(
                    text = L("onboarding.connecting.subtitle"),
                    style = WinoTypography.body,
                    color = colors.textSecondary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        // Bottom section with gradient overlay
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
        ) {
            BlurredGradientOverlay(canvasColor = colors.bgCanvas) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp)
                ) {
                    // Helper: pb-[16px] pt-[24px] px-[32px]
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 32.dp)
                            .padding(top = 24.dp, bottom = 16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = L("onboarding.connecting.helper"),
                            style = WinoTypography.small,
                            color = colors.textSecondary,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    // Button: p-[24px]
                    WinoButton(
                        text = L("common.cancel"),
                        onClick = onCancel,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        variant = WinoButtonVariant.Secondary,
                        size = WinoButtonSize.Large
                    )
                }
            }
        }
    }
}

/**
 * Success screen (Figma 1343:14142)
 * Figma structure (from reference code):
 * - Background: brand/primary (#7c5cff)
 * - Icon: 96x96 rounded-[36px] bg-white
 * - Text: gap-[8px] px-[24px], color white/white-75%
 */
@Composable
private fun SuccessScreen() {
    val colors = WinoTheme.colors

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.brandPrimary),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(40.dp, Alignment.CenterVertically)
    ) {
        // Icon - Bold weight (strokeWidth = 3f)
        Box(
            modifier = Modifier
                .size(96.dp)
                .clip(RoundedCornerShape(36.dp))  // radius/xl
                .background(Color.White),
            contentAlignment = Alignment.Center
        ) {
            PhosphorIcons.Check(
                size = 48.dp,
                color = colors.brandPrimary,
                strokeWidth = 3f  // Bold weight
            )
        }

        // Text section - just title, no subtitles
        Text(
            text = L("onboarding.connected.title"),
            style = WinoTypography.h1Medium,
            color = Color.White,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
        )
    }
}

/**
 * Timeout screen (Figma 1343:14226)
 * Figma structure (from reference code):
 * - Background: state/error (#f2304e)
 * - Icon: 96x96 rounded-[36px] bg-error
 * - Text: gap-[8px] px-[24px]
 * - Bottom: absolute, backdrop-blur, gradient
 *   - Helper: px-[32px] py-[24px/16px]
 *   - Buttons: gap-[12px] p-[24px]
 */
@Composable
private fun TimeoutScreen(
    onTryAgain: () -> Unit,
    onCancel: () -> Unit
) {
    val colors = WinoTheme.colors

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.bgCanvas)
    ) {
        // Main content - centered
        Column(
            modifier = Modifier
                .fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(40.dp, Alignment.CenterVertically)
        ) {
            // Icon
            Box(
                modifier = Modifier
                    .size(96.dp)
                    .clip(RoundedCornerShape(36.dp))  // radius/xl
                    .background(colors.stateError),
                contentAlignment = Alignment.Center
            ) {
                PhosphorIcons.HourglassMedium(
                    size = 48.dp,
                    color = Color.White,
                    strokeWidth = 2f
                )
            }

            // Text section
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),  // spacing/lg
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)  // spacing/xs
            ) {
                Text(
                    text = L("onboarding.timeout.title"),
                    style = WinoTypography.h1Medium,
                    color = colors.textPrimary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )

                Text(
                    text = L("onboarding.timeout.subtitle"),
                    style = WinoTypography.body,
                    color = colors.textSecondary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        // Bottom section with gradient overlay
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
        ) {
            BlurredGradientOverlay(canvasColor = colors.bgCanvas) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp)
                ) {
                    // Helper: pb-[16px] pt-[24px] px-[32px]
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 32.dp)
                            .padding(top = 24.dp, bottom = 16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = L("onboarding.timeout.tip"),
                            style = WinoTypography.small,
                            color = colors.textSecondary,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    // Buttons: p-[24px] gap-[12dp]
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        WinoButton(
                            text = L("common.cancel"),
                            onClick = onCancel,
                            modifier = Modifier.weight(1f),
                            variant = WinoButtonVariant.Secondary,
                            size = WinoButtonSize.Large
                        )

                        WinoButton(
                            text = L("common.retry"),
                            onClick = onTryAgain,
                            modifier = Modifier.weight(1f),
                            size = WinoButtonSize.Large
                        )
                    }
                }
            }
        }
    }
}

/**
 * Error screen (Figma 1343:14237)
 * Red rounded square with SmileyXEyes icon, error message box, "Get help" link, Cancel/Try again buttons
 */
@Composable
private fun ErrorScreen(
    message: String,
    onTryAgain: () -> Unit,
    onCancel: () -> Unit,
    onGetHelp: () -> Unit
) {
    val colors = WinoTheme.colors

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.bgCanvas)
    ) {
        // Main content - centered with gap-[40dp]
        Column(
            modifier = Modifier
                .fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(40.dp, Alignment.CenterVertically)
        ) {
            // Icon: 96x96 rounded-[36dp]
            Box(
                modifier = Modifier
                    .size(96.dp)
                    .clip(RoundedCornerShape(36.dp))
                    .background(colors.stateError),
                contentAlignment = Alignment.Center
            ) {
                PhosphorIcons.SmileyXEyes(
                    size = 48.dp,
                    color = Color.White,
                    strokeWidth = 2f
                )
            }

            // Text section: gap-[8dp] px-[24dp]
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = L("onboarding.error.title"),
                    style = WinoTypography.h1Medium,
                    color = colors.textPrimary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )

                Text(
                    text = L("onboarding.error.subtitle"),
                    style = WinoTypography.body,
                    color = colors.textSecondary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // Error message box: px-[24dp]
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(36.dp))
                        .background(colors.bgSurface)
                        .padding(24.dp)
                ) {
                    Text(
                        text = message,
                        style = WinoTypography.small,
                        color = colors.textSecondary
                    )
                }
            }
        }

        // Bottom section with gradient overlay
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
        ) {
            BlurredGradientOverlay(canvasColor = colors.bgCanvas) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp)
                ) {
                    // Helper: pb-[16px] pt-[24px] px-[32px] gap-[4dp]
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 32.dp)
                            .padding(top = 24.dp, bottom = 16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = L("onboarding.error.helper"),
                            style = WinoTypography.small,
                            color = colors.textSecondary,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )

                        Text(
                            text = L("onboarding.error.get_help"),
                            style = WinoTypography.small,
                            color = colors.brandPrimary,
                            textAlign = TextAlign.Center,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onGetHelp() }
                        )
                    }

                    // Buttons: gap-[12dp] p-[24dp]
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        WinoButton(
                            text = L("common.cancel"),
                            onClick = onCancel,
                            modifier = Modifier.weight(1f),
                            variant = WinoButtonVariant.Secondary,
                            size = WinoButtonSize.Large
                        )

                        WinoButton(
                            text = L("common.retry"),
                            onClick = onTryAgain,
                            modifier = Modifier.weight(1f),
                            size = WinoButtonSize.Large
                        )
                    }
                }
            }
        }
    }
}

/**
 * Wallet change screen (Figma 1343:14252)
 * Warning icon, shows old→new addresses, lists what will be deleted, Cancel/Delete buttons
 */
@Composable
private fun WalletChangeScreen(
    newWalletName: String?,
    oldAddresses: Map<String, String>,
    newAddresses: Map<String, String>,
    isClearing: Boolean,
    onConfirm: () -> Unit,
    onCancel: () -> Unit
) {
    val colors = WinoTheme.colors

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.bgCanvas)
    ) {
        // Main scrollable content
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(bottom = 200.dp) // Space for bottom section
        ) {
            // Content - flex-col gap-[40px] pt-[60px]
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = WinoSpacing.XL), // Safe area = 32dp per Figma
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(40.dp)
            ) {
                // Warning icon: 96x96 rounded-[36dp] bg-destructivesoft
                Box(
                    modifier = Modifier
                        .size(96.dp)
                        .clip(RoundedCornerShape(36.dp))
                        .background(Color(0xFFFFE8EC)), // bg-destructivesoft
                    contentAlignment = Alignment.Center
                ) {
                    PhosphorIcons.Warning(
                        size = 48.dp,
                        color = colors.stateError,
                        strokeWidth = 2f
                    )
                }

                // Text section: gap-[8dp] px-[24dp]
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = L("onboarding.different_wallet.title"),
                        style = WinoTypography.h1Medium,
                        color = colors.textPrimary,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Text(
                        text = L("onboarding.different_wallet.subtitle"),
                        style = WinoTypography.body,
                        color = colors.textSecondary,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                // Layout section: px-[24dp]
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(0.dp)
                ) {
                    // Network addresses list
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(36.dp))
                            .background(colors.bgSurface)
                            .padding(horizontal = 16.dp)
                    ) {
                        // Tron address
                        oldAddresses["tron"]?.let { oldAddr ->
                            newAddresses["tron"]?.let { newAddr ->
                                WalletChangeNetworkItem(
                                    networkName = L("onboarding.different_wallet.tron_address"),
                                    oldAddress = oldAddr,
                                    newAddress = newAddr,
                                    iconColor = Color(0xFFFE0A0E) // Tron red
                                )
                            }
                        }

                        // Solana address
                        oldAddresses["solana"]?.let { oldAddr ->
                            newAddresses["solana"]?.let { newAddr ->
                                WalletChangeNetworkItem(
                                    networkName = L("onboarding.different_wallet.solana_address"),
                                    oldAddress = oldAddr,
                                    newAddress = newAddr,
                                    iconColor = colors.brandPrimary
                                )
                            }
                        }
                    }

                    // "This will DELETE" text: pb-[16dp] pt-[24dp]
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(
                                bottom = 16.dp,
                                top = 24.dp
                            )
                    ) {
                        Text(
                            text = L("onboarding.different_wallet.warning_delete"),
                            style = WinoTypography.h3Medium,
                            color = colors.textPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    // Delete items list
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(36.dp))
                            .background(colors.bgSurface)
                            .padding(horizontal = 16.dp)
                    ) {
                        WalletChangeDeleteItem(
                            icon = { size, color, strokeWidth ->
                                PhosphorIcons.ClockCounterClockwise(
                                    size = size,
                                    color = color,
                                    strokeWidth = strokeWidth
                                )
                            },
                            title = L("onboarding.different_wallet.warning_history"),
                            subtitle = L("onboarding.different_wallet.warning_deleted")
                        )

                        WalletChangeDeleteItem(
                            icon = { size, color, strokeWidth ->
                                PhosphorIcons.FileText(
                                    size = size,
                                    color = color,
                                    strokeWidth = strokeWidth
                                )
                            },
                            title = L("onboarding.different_wallet.warning_invoices"),
                            subtitle = L("onboarding.different_wallet.warning_deleted")
                        )

                        WalletChangeDeleteItem(
                            icon = { size, color, strokeWidth ->
                                PhosphorIcons.Gear(
                                    size = size,
                                    color = color,
                                    strokeWidth = strokeWidth
                                )
                            },
                            title = L("onboarding.different_wallet.warning_settings"),
                            subtitle = L("onboarding.different_wallet.warning_deleted")
                        )
                    }
                }
            }
        }

        // Bottom section with gradient overlay
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
        ) {
            BlurredGradientOverlay(canvasColor = colors.bgCanvas) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp)
                ) {
                    // Helper text: px-[32dp] py-[16dp to 24dp]
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(
                                horizontal = 32.dp
                            )
                            .padding(
                                bottom = 16.dp,
                                top = 24.dp
                            )
                    ) {
                        Text(
                            text = L("onboarding.different_wallet.warning_irreversible"),
                            style = WinoTypography.small,
                            color = colors.textSecondary,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    // Buttons: gap-[12dp] p-[24dp]
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        WinoButton(
                            text = L("common.cancel"),
                            onClick = onCancel,
                            modifier = Modifier.weight(1f),
                            variant = WinoButtonVariant.Secondary,
                            size = WinoButtonSize.Large,
                            enabled = !isClearing
                        )

                        WinoButton(
                            text = if (isClearing) L("common.deleting") else L("common.delete"),
                            onClick = onConfirm,
                            modifier = Modifier.weight(1f),
                            variant = WinoButtonVariant.Destructive,
                            size = WinoButtonSize.Large,
                            enabled = !isClearing
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun WalletChangeNetworkItem(
    networkName: String,
    oldAddress: String,
    newAddress: String,
    iconColor: Color
) {
    val colors = WinoTheme.colors
    val maskOld = "${oldAddress.take(8)}...${oldAddress.takeLast(8)}"
    val maskNew = "${newAddress.take(8)}...${newAddress.takeLast(8)}"

    Row(
        modifier = Modifier
            .fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Network icon: 36x36 circle
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(iconColor)
        )

        // Text section: py-[12dp]
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(vertical = 12.dp)
        ) {
            Text(
                text = networkName,
                style = WinoTypography.bodyMedium,
                color = colors.textPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Text(
                text = "$maskOld → $maskNew",
                style = WinoTypography.body,
                color = colors.textTertiary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun WalletChangeDeleteItem(
    icon: @Composable (size: Dp, color: Color, strokeWidth: Float) -> Unit,
    title: String,
    subtitle: String
) {
    val colors = WinoTheme.colors

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Icon: 36x36 rounded-[36dp] bg-elevated
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(36.dp))
                .background(colors.bgElevated),
            contentAlignment = Alignment.Center
        ) {
            icon(20.dp, colors.textSecondary, 2f)
        }

        // Text section: py-[12dp]
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(vertical = 12.dp)
        ) {
            Text(
                text = title,
                style = WinoTypography.bodyMedium,
                color = colors.textPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Text(
                text = subtitle,
                style = WinoTypography.body,
                color = colors.textTertiary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

/**
 * Session expired screen (Figma 1343:14297)
 * Gray rounded square with HourglassMedium icon, simple message, "Reconnect Wallet" button
 */
@Composable
private fun SessionExpiredScreen(
    onReconnect: () -> Unit
) {
    val colors = WinoTheme.colors

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.bgCanvas)
    ) {
        // Main content - centered with gap-[40dp]
        Column(
            modifier = Modifier
                .fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(40.dp, Alignment.CenterVertically)
        ) {
            // Icon: 96x96 rounded-[36dp]
            Box(
                modifier = Modifier
                    .size(96.dp)
                    .clip(RoundedCornerShape(36.dp))
                    .background(colors.bgSurface),
                contentAlignment = Alignment.Center
            ) {
                PhosphorIcons.HourglassMedium(
                    size = 48.dp,
                    color = colors.textSecondary,
                    strokeWidth = 2f
                )
            }

            // Text section: gap-[8dp] px-[24dp]
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = L("onboarding.session_expired.title"),
                    style = WinoTypography.h1Medium,
                    color = colors.textPrimary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )

                Text(
                    text = L("onboarding.session_expired.subtitle"),
                    style = WinoTypography.body,
                    color = colors.textSecondary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        // Bottom section with gradient overlay
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
        ) {
            BlurredGradientOverlay(canvasColor = colors.bgCanvas) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp)
                ) {
                    // Button: p-[24dp]
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp)
                    ) {
                        WinoButton(
                            text = L("onboarding.session_expired.reconnect"),
                            onClick = onReconnect,
                            modifier = Modifier.fillMaxWidth(),
                            size = WinoButtonSize.Large
                        )
                    }
                }
            }
        }
    }
}

/**
 * Legal documents screen (Figma 1343:14306)
 * Document list (Privacy, Risk, Terms) with "Read" buttons, Close button
 */
@Composable
private fun LegalDocsScreen(
    onClose: () -> Unit
) {
    val colors = WinoTheme.colors
    val uriHandler = LocalUriHandler.current

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.bgCanvas)
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // Header - pt-[60px] safe-area/up
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = WinoSpacing.XL) // Safe area = 32dp per Figma
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(
                            horizontal = 24.dp,
                            vertical = 16.dp
                        ),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = L("legal.title"),
                        style = WinoTypography.h1Medium,
                        color = colors.textPrimary
                    )

                    Text(
                        text = "",
                        style = WinoTypography.body,
                        color = colors.textSecondary
                    )
                }
            }

            // Document list - px-[24dp] py-[16dp]
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        horizontal = 24.dp,
                        vertical = 16.dp
                    )
            ) {
                // List container with rounded bg
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(36.dp))
                        .background(colors.bgSurface)
                        .padding(horizontal = 16.dp)
                ) {
                    LegalDocItem(
                        title = L("legal.privacy"),
                        onRead = { uriHandler.openUri("https://winobank.com/docs/legal/privacypolicy") }
                    )

                    LegalDocItem(
                        title = L("legal.risk"),
                        onRead = { uriHandler.openUri("https://winobank.com/docs/legal/riskdisclosure/pay") }
                    )

                    LegalDocItem(
                        title = L("legal.terms"),
                        onRead = { uriHandler.openUri("https://winobank.com/docs/legal/termsofuse") }
                    )
                }
            }
        }

        // Bottom section with gradient overlay
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
        ) {
            BlurredGradientOverlay(canvasColor = colors.bgCanvas) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp)
                ) {
                    // Button: p-[24dp]
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp)
                    ) {
                        WinoButton(
                            text = L("common.close"),
                            onClick = onClose,
                            modifier = Modifier.fillMaxWidth(),
                            variant = WinoButtonVariant.Secondary,
                            size = WinoButtonSize.Large
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun LegalDocItem(
    title: String,
    onRead: () -> Unit
) {
    val colors = WinoTheme.colors

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(68.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // File icon: 36x36 rounded-[36dp] bg-elevated
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(36.dp))
                .background(colors.bgElevated),
            contentAlignment = Alignment.Center
        ) {
            PhosphorIcons.File(
                size = 20.dp,
                color = colors.textSecondary
            )
        }

        // Title - flex-[1_0_0]
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(vertical = 24.dp),
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = title,
                style = WinoTypography.bodyMedium,
                color = colors.textPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        // Read button - small variant
        WinoButton(
            text = L("common.read"),
            onClick = onRead,
            variant = WinoButtonVariant.Secondary,
            size = WinoButtonSize.Small
        )
    }
}
