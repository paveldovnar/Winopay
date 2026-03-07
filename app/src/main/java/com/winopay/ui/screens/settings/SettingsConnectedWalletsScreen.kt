package com.winopay.ui.screens.settings

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.winopay.WinoPayApplication
import com.winopay.data.profile.MerchantProfileStore
import com.winopay.data.profile.RailConnection
import com.winopay.payments.TronPaymentRail
import com.winopay.solana.wallet.SessionState
import com.winopay.wallet.AddressNormalizer
import com.winopay.wallet.CaipUtils
import com.winopay.wallet.ReownManager
import com.winopay.wallet.UnifiedWalletConnector
import com.winopay.ui.components.PhosphorIcons
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import com.winopay.ui.components.WinoButton
import com.winopay.ui.components.WinoButtonSize
import com.winopay.ui.components.WinoButtonVariant
import com.winopay.ui.theme.WinoRadius
import com.winopay.ui.theme.WinoSpacing
import com.winopay.ui.theme.WinoTheme
import com.winopay.ui.theme.WinoTypography

private const val TAG = "SettingsConnectedWallets"

/**
 * Connected Wallets settings screen.
 *
 * Shows status of all connected payment rails:
 * - Solana (via MWA)
 * - TRON (manual address entry)
 *
 * ALL CONNECTED RAILS ARE ALWAYS ACTIVE.
 * No "active" selection - both Solana and TRON work simultaneously.
 * POS builds payment methods from connectedRails + enabledMethods.
 */
@Composable
fun SettingsConnectedWalletsScreen(
    onBack: () -> Unit,
    onConnectSolana: () -> Unit,
    onConnectTron: () -> Unit
) {
    val colors = WinoTheme.colors
    val app = WinoPayApplication.instance
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val profileStore = remember { MerchantProfileStore(context) }

    // Solana session state (live from MWA)
    val sessionState by app.sessionManager.sessionState.collectAsState()
    val solanaSessionActive = sessionState is SessionState.Active
    val solanaSessionAddress = (sessionState as? SessionState.Active)?.publicKey

    // SINGLE SOURCE OF TRUTH: All connected rails from MerchantProfileStore
    val connectedRails by profileStore.observeConnectedRails().collectAsState(initial = emptyMap())

    // Get Solana connection from MerchantProfileStore (persisted)
    val solanaRail = connectedRails["solana"]
    val solanaConnected = solanaRail != null || solanaSessionActive
    val solanaAddress = solanaRail?.accountId ?: solanaSessionAddress
    val solanaNetworkId = solanaRail?.networkId ?: if (solanaSessionActive) com.winopay.BuildConfig.SOLANA_CLUSTER else null

    // Get TRON connection from MerchantProfileStore (persisted)
    val tronRail = connectedRails[TronPaymentRail.RAIL_ID]
    val tronAddress = tronRail?.accountId
    val tronNetworkId = tronRail?.networkId

    // Get EVM connection from MerchantProfileStore (persisted)
    val evmRail = connectedRails["evm"]
    val evmAddress = evmRail?.accountId
    val evmNetworkId = evmRail?.networkId

    // WalletConnect state
    val unifiedConnector = app.unifiedWalletConnector
    val wcSessionState by unifiedConnector.observeSessionState().collectAsState(initial = UnifiedWalletConnector.SessionState.Disconnected)
    var isWcConnecting by remember { mutableStateOf(false) }
    var wcError by remember { mutableStateOf<String?>(null) }
    val isReownAvailable = remember { ReownManager.isAvailable() }

    // Manual BSC address entry state
    var showManualBscEntry by remember { mutableStateOf(false) }
    var manualBscAddress by remember { mutableStateOf("") }
    var manualBscError by remember { mutableStateOf<String?>(null) }

    // Common result handler (must be defined first for other functions to use)
    fun handleConnectResult(result: UnifiedWalletConnector.ConnectResult) {
        when (result) {
            is UnifiedWalletConnector.ConnectResult.Success -> {
                Log.i(TAG, "WC|CONNECT|SUCCESS|rails=${result.connectedRails.keys}")
                Log.i(TAG, "CONFIG|CONNECTED_RAILS|solana=${result.connectedRails["solana"]?.accountId?.take(8)}|tron=${result.connectedRails["tron"]?.accountId?.take(8)}|evm=${result.connectedRails["evm"]?.accountId?.take(10)}")
            }
            is UnifiedWalletConnector.ConnectResult.PartialSuccess -> {
                Log.w(TAG, "WC|CONNECT|PARTIAL|connected=${result.connectedRails.keys}|failed=${result.failedRails}")
                Log.i(TAG, "CONFIG|CONNECTED_RAILS|solana=${result.connectedRails["solana"]?.accountId?.take(8)}|tron=${result.connectedRails["tron"]?.accountId?.take(8)}|evm=${result.connectedRails["evm"]?.accountId?.take(10)}")
            }
            is UnifiedWalletConnector.ConnectResult.Failure -> {
                Log.e(TAG, "WC|CONNECT|FAIL|error=${result.error}")
                wcError = result.error
            }
            is UnifiedWalletConnector.ConnectResult.CryptoError -> {
                Log.e(TAG, "WC|CONNECT|CRYPTO_ERROR|error=${result.error}|needsReset=${result.needsHealthReset}")
                // Show error with suggestion to retry
                wcError = "${result.error} Tap 'Connect Wallet' to retry."
            }
        }
    }

    // Connect EVM only (staged connect - step 1)
    fun connectEvmOnly() {
        scope.launch {
            Log.d(TAG, "WC|CONNECT|START|stage=EVM")
            isWcConnecting = true
            wcError = null

            val result = unifiedConnector.connectEvmOnly()

            isWcConnecting = false
            handleConnectResult(result)
        }
    }

    // Add Solana to existing session (staged connect - step 2)
    fun addSolana() {
        scope.launch {
            Log.d(TAG, "WC|CONNECT|START|stage=SOLANA")
            isWcConnecting = true
            wcError = null

            val result = unifiedConnector.addNamespace(UnifiedWalletConnector.ConnectStage.SOLANA)

            isWcConnecting = false
            handleConnectResult(result)
        }
    }

    // Add TRON to existing session (staged connect - step 3)
    fun addTron() {
        scope.launch {
            Log.d(TAG, "WC|CONNECT|START|stage=TRON")
            isWcConnecting = true
            wcError = null

            val result = unifiedConnector.addNamespace(UnifiedWalletConnector.ConnectStage.TRON)

            isWcConnecting = false
            handleConnectResult(result)
        }
    }

    // Save manual BSC address
    fun saveManualBscAddress() {
        val trimmed = manualBscAddress.trim()
        if (!AddressNormalizer.isValid("evm", trimmed)) {
            manualBscError = "Invalid BSC address. Must be 0x followed by 40 hex characters."
            Log.w(TAG, "MANUAL|BSC|INVALID|address=${trimmed.take(10)}...")
            return
        }

        val normalized = AddressNormalizer.normalize("evm", trimmed)
        val networkId = CaipUtils.getPrimaryEvmNetworkId()

        scope.launch {
            val connection = RailConnection(
                railId = "evm",
                networkId = networkId,
                accountId = normalized,
                connectedAt = System.currentTimeMillis(),
                sessionId = null  // Manual entry, no WC session
            )
            profileStore.connectRail(connection)
            Log.i(TAG, "MANUAL|BSC|SAVED|address=${normalized.take(10)}...|networkId=$networkId")
            Log.i(TAG, "CONFIG|CONNECTED_RAILS|evm=$normalized")

            showManualBscEntry = false
            manualBscAddress = ""
            manualBscError = null
        }
    }

    LaunchedEffect(Unit) {
        Log.i(TAG, "CONFIG|CONNECTED_RAILS|rails=${connectedRails.keys}|solana=$solanaConnected|tron=${tronAddress != null}|evm=${evmAddress != null}")
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.bgCanvas)
            .statusBarsPadding()
    ) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    start = WinoSpacing.LG,
                    end = WinoSpacing.LG,
                    top = WinoSpacing.XS,
                    bottom = WinoSpacing.XS
                ),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(WinoSpacing.SM)
        ) {
            Text(
                text = "Connected Wallets",
                style = WinoTypography.h1Medium,
                color = colors.textPrimary,
                modifier = Modifier.weight(1f)
            )

            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(WinoRadius.XL))
                    .background(colors.bgSurface)
                    .clickable { onBack() },
                contentAlignment = Alignment.Center
            ) {
                PhosphorIcons.X(
                    size = 20.dp,
                    color = colors.textPrimary
                )
            }
        }

        // Content
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding()
                .padding(WinoSpacing.LG),
            verticalArrangement = Arrangement.spacedBy(WinoSpacing.MD)
        ) {
            // Info text
            Text(
                text = "Connect wallets to receive payments on different networks. All connected wallets are active simultaneously.",
                style = WinoTypography.body,
                color = colors.textSecondary
            )

            // WalletConnect error display
            wcError?.let { error ->
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

            // Connect button via WalletConnect (staged connect)
            if (isReownAvailable) {
                // Check which networks are already connected
                val hasEvm = evmAddress != null
                val hasSolana = solanaConnected
                val hasTron = tronAddress != null

                if (!hasEvm) {
                    // No wallet connected - show initial connect button
                    WinoButton(
                        text = if (isWcConnecting) "Connecting..." else "Connect Wallet",
                        onClick = { if (!isWcConnecting) connectEvmOnly() },
                        modifier = Modifier.fillMaxWidth(),
                        variant = WinoButtonVariant.Primary,
                        size = WinoButtonSize.Large,
                        enabled = !isWcConnecting
                    )

                    Text(
                        text = "Connect BSC wallet first, then add more networks",
                        style = WinoTypography.micro,
                        color = colors.textMuted,
                        modifier = Modifier.padding(bottom = WinoSpacing.SM)
                    )
                } else {
                    // EVM connected - show staged connect buttons for missing networks
                    if (!hasSolana || !hasTron) {
                        Text(
                            text = "Add more networks:",
                            style = WinoTypography.smallMedium,
                            color = colors.textSecondary,
                            modifier = Modifier.padding(bottom = WinoSpacing.XS)
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(WinoSpacing.SM)
                        ) {
                            if (!hasSolana) {
                                WinoButton(
                                    text = if (isWcConnecting) "..." else "+ Solana",
                                    onClick = { if (!isWcConnecting) addSolana() },
                                    modifier = Modifier.weight(1f),
                                    variant = WinoButtonVariant.Secondary,
                                    size = WinoButtonSize.Large,
                                    enabled = !isWcConnecting
                                )
                            }
                            if (!hasTron) {
                                WinoButton(
                                    text = if (isWcConnecting) "..." else "+ TRON",
                                    onClick = { if (!isWcConnecting) addTron() },
                                    modifier = Modifier.weight(1f),
                                    variant = WinoButtonVariant.Secondary,
                                    size = WinoButtonSize.Large,
                                    enabled = !isWcConnecting
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(WinoSpacing.SM))
                    }
                }
            }

            // Solana Rail Card
            WalletRailCard(
                railName = "Solana",
                isConnected = solanaConnected,
                address = solanaAddress,
                networkId = solanaNetworkId,
                sessionId = solanaRail?.sessionId,
                onConnect = {
                    Log.d(TAG, "NAV|CLICK|ConnectSolana")
                    onConnectSolana()
                }
            )

            // TRON Rail Card
            WalletRailCard(
                railName = "TRON",
                isConnected = tronAddress != null,
                address = tronAddress,
                networkId = tronNetworkId,
                sessionId = tronRail?.sessionId,
                onConnect = {
                    Log.d(TAG, "NAV|CLICK|ConnectTron")
                    onConnectTron()
                }
            )

            // EVM Rail Card (BSC/Ethereum)
            WalletRailCard(
                railName = evmNetworkId?.let { CaipUtils.getEvmNetworkDisplayName(it) } ?: "BNB Smart Chain",
                isConnected = evmAddress != null,
                address = evmAddress,
                networkId = evmNetworkId,
                sessionId = evmRail?.sessionId,
                onConnect = {
                    Log.d(TAG, "NAV|CLICK|ConnectEvm")
                    // EVM only via WalletConnect (staged connect - step 1)
                    if (isReownAvailable && !isWcConnecting) {
                        connectEvmOnly()
                    }
                },
                showManualEntry = evmAddress == null,
                onManualEntry = {
                    Log.d(TAG, "NAV|CLICK|ManualBscEntry")
                    showManualBscEntry = true
                }
            )

            // Manual BSC Address Entry UI
            if (showManualBscEntry) {
                ManualBscEntryCard(
                    address = manualBscAddress,
                    onAddressChange = {
                        manualBscAddress = it
                        manualBscError = null
                    },
                    error = manualBscError,
                    onSave = { saveManualBscAddress() },
                    onCancel = {
                        showManualBscEntry = false
                        manualBscAddress = ""
                        manualBscError = null
                    }
                )
            }
        }
    }
}

/**
 * Card showing wallet connection status.
 *
 * NO "active" concept - all connected wallets are always active.
 * Shows: connection status, address, network, session info, and Connect button if not connected.
 */
@Composable
private fun WalletRailCard(
    railName: String,
    isConnected: Boolean,
    address: String?,
    networkId: String?,
    sessionId: String? = null,
    onConnect: () -> Unit,
    showManualEntry: Boolean = false,
    onManualEntry: (() -> Unit)? = null
) {
    val colors = WinoTheme.colors

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(WinoRadius.XL))
            .background(colors.bgSurface)
            .padding(WinoSpacing.MD),
        verticalArrangement = Arrangement.spacedBy(WinoSpacing.SM)
    ) {
        // Header row
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(WinoSpacing.SM)
        ) {
            // Icon
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(WinoRadius.LG))
                    .background(if (isConnected) colors.stateSuccessSoft else colors.bgSurfaceAlt),
                contentAlignment = Alignment.Center
            ) {
                PhosphorIcons.Wallet(
                    size = 24.dp,
                    color = if (isConnected) colors.stateSuccess else colors.textMuted
                )
            }

            // Name and status
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = railName,
                    style = WinoTypography.h3Medium,
                    color = colors.textPrimary
                )
                Text(
                    text = if (isConnected) "Connected" else "Not connected",
                    style = WinoTypography.small,
                    color = if (isConnected) colors.stateSuccess else colors.textMuted
                )
            }
        }

        // Address display (if connected)
        if (isConnected && address != null) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(WinoRadius.MD))
                    .background(colors.bgSurfaceAlt)
                    .padding(WinoSpacing.SM),
                verticalArrangement = Arrangement.spacedBy(WinoSpacing.XXS)
            ) {
                Text(
                    text = "Address",
                    style = WinoTypography.micro,
                    color = colors.textMuted
                )
                Text(
                    text = formatAddress(address),
                    style = WinoTypography.small,
                    color = colors.textPrimary
                )
                networkId?.let {
                    Text(
                        text = "Network: $it",
                        style = WinoTypography.micro,
                        color = colors.textSecondary
                    )
                }
                if (sessionId != null) {
                    Text(
                        text = "via WalletConnect",
                        style = WinoTypography.micro,
                        color = colors.brandPrimary
                    )
                }
            }
        }

        // Connect button (only shown if not connected)
        if (!isConnected) {
            WinoButton(
                text = "Connect $railName",
                onClick = onConnect,
                modifier = Modifier.fillMaxWidth(),
                variant = WinoButtonVariant.Primary,
                size = WinoButtonSize.Large
            )

            // Manual entry option (for BSC/EVM)
            if (showManualEntry && onManualEntry != null) {
                Spacer(modifier = Modifier.height(WinoSpacing.XS))
                WinoButton(
                    text = "Add address manually",
                    onClick = onManualEntry,
                    modifier = Modifier.fillMaxWidth(),
                    variant = WinoButtonVariant.Secondary,
                    size = WinoButtonSize.Small
                )
            }
        }
    }
}

/**
 * Manual BSC address entry card.
 */
@Composable
private fun ManualBscEntryCard(
    address: String,
    onAddressChange: (String) -> Unit,
    error: String?,
    onSave: () -> Unit,
    onCancel: () -> Unit
) {
    val colors = WinoTheme.colors
    val keyboardController = LocalSoftwareKeyboardController.current

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(WinoRadius.XL))
            .background(colors.bgSurface)
            .padding(WinoSpacing.MD),
        verticalArrangement = Arrangement.spacedBy(WinoSpacing.SM)
    ) {
        Text(
            text = "Add BSC Address Manually",
            style = WinoTypography.h3Medium,
            color = colors.textPrimary
        )

        Text(
            text = "Enter your BSC/BNB Smart Chain wallet address",
            style = WinoTypography.small,
            color = colors.textSecondary
        )

        OutlinedTextField(
            value = address,
            onValueChange = onAddressChange,
            modifier = Modifier.fillMaxWidth(),
            placeholder = {
                Text(
                    text = "0x...",
                    style = WinoTypography.body,
                    color = colors.textMuted
                )
            },
            textStyle = WinoTypography.body,
            singleLine = true,
            isError = error != null,
            supportingText = error?.let {
                { Text(text = it, color = colors.stateError) }
            },
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Ascii,
                imeAction = ImeAction.Done
            ),
            keyboardActions = KeyboardActions(
                onDone = {
                    keyboardController?.hide()
                    onSave()
                }
            ),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = colors.textPrimary,
                unfocusedTextColor = colors.textPrimary,
                focusedBorderColor = colors.brandPrimary,
                unfocusedBorderColor = colors.borderDefault,
                errorBorderColor = colors.stateError,
                focusedContainerColor = colors.bgSurfaceAlt,
                unfocusedContainerColor = colors.bgSurfaceAlt
            ),
            shape = RoundedCornerShape(WinoRadius.MD)
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(WinoSpacing.SM)
        ) {
            WinoButton(
                text = "Cancel",
                onClick = onCancel,
                modifier = Modifier.weight(1f),
                variant = WinoButtonVariant.Secondary,
                size = WinoButtonSize.Small
            )
            WinoButton(
                text = "Save",
                onClick = onSave,
                modifier = Modifier.weight(1f),
                variant = WinoButtonVariant.Primary,
                size = WinoButtonSize.Small,
                enabled = address.isNotBlank()
            )
        }
    }
}

private fun formatAddress(address: String): String {
    return if (address.length > 16) {
        "${address.take(8)}...${address.takeLast(8)}"
    } else {
        address
    }
}
