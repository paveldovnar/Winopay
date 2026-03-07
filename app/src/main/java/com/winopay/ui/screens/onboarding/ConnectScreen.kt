package com.winopay.ui.screens.onboarding

import android.util.Log
import androidx.compose.foundation.background
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
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

    val isReownAvailable = remember { ReownManager.isAvailable() }

    // Check onboarding state
    LaunchedEffect(Unit) {
        isOnboardingCompleted = profileStore.isOnboardingCompleted()
        Log.d(TAG, "ONBOARD|INIT|completed=$isOnboardingCompleted")
    }

    // Common result handler (must be defined first for other functions to use)
    fun handleConnectResult(result: UnifiedWalletConnector.ConnectResult, wasOnboarded: Boolean) {
        when (result) {
            is UnifiedWalletConnector.ConnectResult.Success -> {
                Log.i(TAG, "WC|CONNECT|SUCCESS|rails=${result.connectedRails.keys}|wallet=${result.walletName}")
                Log.i(TAG, "CONFIG|CONNECTED_RAILS|solana=${result.connectedRails["solana"]?.accountId?.take(8)}|tron=${result.connectedRails["tron"]?.accountId?.take(8)}|evm=${result.connectedRails["evm"]?.accountId?.take(10)}")

                connectionResult = ConnectionResult(
                    success = true,
                    walletName = result.walletName,
                    connectedRails = result.connectedRails,
                    failedRails = emptyList()
                )
                isOnboardingCompleted = wasOnboarded
            }
            is UnifiedWalletConnector.ConnectResult.PartialSuccess -> {
                Log.w(TAG, "WC|CONNECT|PARTIAL|connected=${result.connectedRails.keys}|failed=${result.failedRails}")
                Log.i(TAG, "CONFIG|CONNECTED_RAILS|solana=${result.connectedRails["solana"]?.accountId?.take(8)}|tron=${result.connectedRails["tron"]?.accountId?.take(8)}|evm=${result.connectedRails["evm"]?.accountId?.take(10)}")

                connectionResult = ConnectionResult(
                    success = true,
                    walletName = result.walletName,
                    connectedRails = result.connectedRails,
                    failedRails = result.failedRails
                )
                isOnboardingCompleted = wasOnboarded
            }
            is UnifiedWalletConnector.ConnectResult.Failure -> {
                Log.e(TAG, "WC|CONNECT|FAIL|error=${result.error}")
                connectionError = result.error
            }
            is UnifiedWalletConnector.ConnectResult.CryptoError -> {
                Log.e(TAG, "WC|CONNECT|CRYPTO_ERROR|error=${result.error}|needsReset=${result.needsHealthReset}")
                // Show error with suggestion to retry
                connectionError = "${result.error} Tap 'Connect Wallet' to retry."
            }
        }
    }

    // Connect EVM only (staged connect - step 1)
    fun connectEvmOnly() {
        scope.launch {
            Log.i(TAG, "WC|CONNECT|START|stage=EVM")
            isConnecting = true
            connectionError = null
            connectionResult = null

            val wasOnboarded = profileStore.isOnboardingCompleted()

            val result = unifiedConnector.connectEvmOnly()

            isConnecting = false

            handleConnectResult(result, wasOnboarded)
        }
    }

    // Add Solana to existing session (staged connect - step 2)
    fun addSolana() {
        scope.launch {
            Log.i(TAG, "WC|CONNECT|START|stage=SOLANA")
            isConnecting = true
            connectionError = null

            val wasOnboarded = profileStore.isOnboardingCompleted()

            val result = unifiedConnector.addNamespace(UnifiedWalletConnector.ConnectStage.SOLANA)

            isConnecting = false

            handleConnectResult(result, wasOnboarded)
        }
    }

    // Add TRON to existing session (staged connect - step 3)
    fun addTron() {
        scope.launch {
            Log.i(TAG, "WC|CONNECT|START|stage=TRON")
            isConnecting = true
            connectionError = null

            val wasOnboarded = profileStore.isOnboardingCompleted()

            val result = unifiedConnector.addNamespace(UnifiedWalletConnector.ConnectStage.TRON)

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
                val canAddSolana = !connectionResult!!.connectedRails.containsKey("solana")
                val canAddTron = !connectionResult!!.connectedRails.containsKey("tron")

                ConnectionResultCard(
                    result = connectionResult!!,
                    onContinue = { proceedAfterConnect() },
                    onAddSolana = { addSolana() },
                    onAddTron = { addTron() },
                    canAddSolana = canAddSolana,
                    canAddTron = canAddTron
                )
            } else if (isConnecting) {
                // Connecting state
                ConnectingState()
            } else {
                // Initial state - show connect button
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

                // Networks info card
                NetworksInfoCard()

                Spacer(modifier = Modifier.height(WinoSpacing.MD))

                // Main connect button (EVM first - staged connect)
                WinoButton(
                    text = "Connect Wallet",
                    onClick = { connectEvmOnly() },
                    modifier = Modifier.fillMaxWidth(),
                    variant = WinoButtonVariant.Primary,
                    size = WinoButtonSize.Large,
                    enabled = isReownAvailable
                )

                Text(
                    text = "Connect BSC first, then add more networks",
                    style = WinoTypography.micro,
                    color = colors.textMuted,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

/**
 * Card showing supported networks before connecting.
 */
@Composable
private fun NetworksInfoCard() {
    val colors = WinoTheme.colors

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(WinoRadius.LG))
            .background(colors.bgSurface)
            .padding(WinoSpacing.MD),
        verticalArrangement = Arrangement.spacedBy(WinoSpacing.SM)
    ) {
        Text(
            text = "Supported Networks",
            style = WinoTypography.h3Medium,
            color = colors.textPrimary
        )

        NetworkRow(name = "Solana", description = "USDC, USDT")
        NetworkRow(name = "TRON", description = "USDT")
        NetworkRow(name = "BSC", description = "USDT, USDC")
    }
}

@Composable
private fun NetworkRow(name: String, description: String) {
    val colors = WinoTheme.colors

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(WinoSpacing.SM)
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(colors.bgSurfaceAlt),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = name.first().toString(),
                style = WinoTypography.smallMedium,
                color = colors.textSecondary
            )
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = name,
                style = WinoTypography.bodyMedium,
                color = colors.textPrimary
            )
            Text(
                text = description,
                style = WinoTypography.micro,
                color = colors.textMuted
            )
        }
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
    onContinue: () -> Unit,
    onAddSolana: () -> Unit = {},
    onAddTron: () -> Unit = {},
    canAddSolana: Boolean = false,
    canAddTron: Boolean = false
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

        // Staged connect buttons for missing networks
        if (canAddSolana || canAddTron) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(WinoSpacing.XS)
            ) {
                Text(
                    text = "Add more networks:",
                    style = WinoTypography.smallMedium,
                    color = colors.textSecondary
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(WinoSpacing.SM)
                ) {
                    if (canAddSolana) {
                        WinoButton(
                            text = "+ Solana",
                            onClick = onAddSolana,
                            modifier = Modifier.weight(1f),
                            variant = WinoButtonVariant.Secondary,
                            size = WinoButtonSize.Small
                        )
                    }
                    if (canAddTron) {
                        WinoButton(
                            text = "+ TRON",
                            onClick = onAddTron,
                            modifier = Modifier.weight(1f),
                            variant = WinoButtonVariant.Secondary,
                            size = WinoButtonSize.Small
                        )
                    }
                }
            }
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
    val connectedRails: Map<String, RailConnection>,
    val failedRails: List<String>
)
