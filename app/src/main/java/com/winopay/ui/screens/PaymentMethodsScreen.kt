package com.winopay.ui.screens

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.winopay.data.profile.MerchantProfileStore
import com.winopay.data.profile.RailConnection
import com.winopay.payments.PaymentRailFactory
import com.winopay.payments.SupportedToken
import com.winopay.ui.components.PhosphorIcons
import com.winopay.ui.components.WinoButton
import com.winopay.ui.components.WinoButtonSize
import com.winopay.ui.components.WinoButtonVariant
import com.winopay.ui.components.WinoPrimaryButton
import com.winopay.ui.components.WinoToggle
import com.winopay.ui.theme.WinoRadius
import com.winopay.ui.theme.WinoSpacing
import com.winopay.ui.theme.WinoTheme
import com.winopay.ui.theme.WinoTypography
import kotlinx.coroutines.launch

private const val TAG = "PaymentMethodsScreen"

/**
 * Unified Payment Methods screen.
 *
 * SINGLE SOURCE OF TRUTH:
 * - Reads connectedRails from MerchantProfileStore
 * - Reads/writes enabledMethods to MerchantProfileStore
 * - Shows ALL supported rails with their tokens
 * - Unconnected rails show "Connect" button (no "Soon" labels)
 *
 * Used by both onboarding flow and settings.
 *
 * @param isOnboarding Whether this is shown during onboarding (affects button text)
 * @param onContinue Called when user finishes (Continue/Done)
 * @param onConnectSolana Called to navigate to Solana connect screen
 * @param onConnectTron Called to navigate to TRON connect screen
 */
@Composable
fun PaymentMethodsScreen(
    isOnboarding: Boolean = false,
    onContinue: () -> Unit,
    onConnectSolana: (() -> Unit)? = null,
    onConnectTron: (() -> Unit)? = null
) {
    val colors = WinoTheme.colors
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val uriHandler = LocalUriHandler.current

    // SINGLE SOURCE OF TRUTH: MerchantProfileStore
    val profileStore = remember { MerchantProfileStore(context) }

    // Observe connected rails
    val connectedRails by profileStore.observeConnectedRails().collectAsState(initial = emptyMap())

    // Observe enabled methods
    val enabledMethods by profileStore.observeEnabledMethods().collectAsState(initial = emptySet())

    // Token data for each rail
    var solanaTokens by remember { mutableStateOf<List<SupportedToken>>(emptyList()) }
    var tronTokens by remember { mutableStateOf<List<SupportedToken>>(emptyList()) }
    var evmTokens by remember { mutableStateOf<List<SupportedToken>>(emptyList()) }

    // Load tokens for connected rails
    LaunchedEffect(connectedRails) {
        Log.i(TAG, "CONFIG|CONNECTED_RAILS|rails=${connectedRails.keys}|count=${connectedRails.size}")
        Log.d(TAG, "METHODS|LOAD|connectedRails=${connectedRails.keys}")

        // Load Solana tokens
        val solanaConnection = connectedRails["solana"]
        if (solanaConnection != null) {
            try {
                val rail = PaymentRailFactory.getRailByRailId("solana", solanaConnection.networkId)
                solanaTokens = rail.getSupportedTokens()
                Log.d(TAG, "METHODS|LOAD|solana|tokens=${solanaTokens.map { it.symbol }}")
            } catch (e: Exception) {
                Log.e(TAG, "METHODS|LOAD|solana|error=${e.message}")
                solanaTokens = emptyList()
            }
        } else {
            // Show default Solana tokens even if not connected (for display)
            try {
                val rail = PaymentRailFactory.getDefaultRail()
                solanaTokens = rail.getSupportedTokens()
            } catch (e: Exception) {
                solanaTokens = emptyList()
            }
        }

        // Load TRON tokens
        val tronConnection = connectedRails["tron"]
        if (tronConnection != null) {
            try {
                val rail = PaymentRailFactory.getRailByRailId("tron", tronConnection.networkId)
                tronTokens = rail.getSupportedTokens()
                Log.d(TAG, "METHODS|LOAD|tron|tokens=${tronTokens.map { it.symbol }}")
            } catch (e: Exception) {
                Log.e(TAG, "METHODS|LOAD|tron|error=${e.message}")
                tronTokens = emptyList()
            }
        } else {
            // Show default TRON tokens even if not connected
            try {
                val rail = PaymentRailFactory.getRailByRailId("tron", com.winopay.BuildConfig.TRON_NETWORK)
                tronTokens = rail.getSupportedTokens()
            } catch (e: Exception) {
                tronTokens = emptyList()
            }
        }

        // Load EVM/BSC tokens
        val evmConnection = connectedRails["evm"]
        if (evmConnection != null) {
            try {
                val rail = PaymentRailFactory.getRailByRailId("evm", evmConnection.networkId)
                evmTokens = rail.getSupportedTokens()
                Log.d(TAG, "METHODS|LOAD|evm|tokens=${evmTokens.map { it.symbol }}")
            } catch (e: Exception) {
                Log.e(TAG, "METHODS|LOAD|evm|error=${e.message}")
                evmTokens = emptyList()
            }
        } else {
            // Show default EVM tokens even if not connected
            try {
                val rail = PaymentRailFactory.getRailByRailId("evm", com.winopay.wallet.CaipUtils.getPrimaryEvmNetworkId())
                evmTokens = rail.getSupportedTokens()
            } catch (e: Exception) {
                evmTokens = emptyList()
            }
        }

        // Log effective methods
        val effectiveMethods = mutableListOf<String>()
        for ((railId, _) in connectedRails) {
            val tokens = when (railId) {
                "solana" -> solanaTokens
                "tron" -> tronTokens
                "evm" -> evmTokens
                else -> emptyList()
            }
            tokens.filter { it.isEnabled }.forEach { token ->
                val methodId = "$railId:${token.tokenId}"
                if (enabledMethods.isEmpty() || enabledMethods.contains(methodId)) {
                    effectiveMethods.add(methodId)
                }
            }
        }
        Log.i(TAG, "METHODS|EFFECTIVE|methods=$effectiveMethods")
    }

    // Helper to check if a method is enabled
    // CRITICAL: Method can only be "enabled" if rail is connected
    fun isMethodEnabled(railId: String, tokenId: String): Boolean {
        // Rail must be connected for method to be enabled
        if (!connectedRails.containsKey(railId)) {
            return false
        }
        val methodId = "$railId:$tokenId"
        // If enabledMethods is empty, all methods are enabled by default
        return enabledMethods.isEmpty() || enabledMethods.contains(methodId)
    }

    // Helper to toggle method
    fun toggleMethod(railId: String, tokenId: String, enabled: Boolean) {
        scope.launch {
            val methodId = "$railId:$tokenId"
            val current = enabledMethods.toMutableSet()

            // If current set is empty, initialize with all connected rail tokens
            if (current.isEmpty()) {
                // Get all tokens for all connected rails
                for ((rId, conn) in connectedRails) {
                    try {
                        val rail = PaymentRailFactory.getRailByRailId(rId, conn.networkId)
                        rail.getSupportedTokens().filter { it.isEnabled }.forEach { token ->
                            current.add("$rId:${token.tokenId}")
                        }
                    } catch (e: Exception) {
                        // Skip
                    }
                }
            }

            if (enabled) {
                current.add(methodId)
            } else {
                current.remove(methodId)
            }

            profileStore.setEnabledMethods(current)
            Log.i(TAG, "METHODS|TOGGLE|$methodId|enabled=$enabled")
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.bgCanvas)
            .statusBarsPadding()
    ) {
        // Header
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = WinoSpacing.LG,
                    vertical = WinoSpacing.MD
                ),
            verticalArrangement = Arrangement.spacedBy(WinoSpacing.XXS)
        ) {
            Text(
                text = if (isOnboarding) "Set Payment methods" else "Payment Methods",
                style = WinoTypography.h1Medium,
                color = colors.textPrimary
            )
            Text(
                text = "Enable payment methods for your customers",
                style = WinoTypography.body,
                color = colors.textSecondary
            )
        }

        // Content - scrollable
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = WinoSpacing.LG)
        ) {
            // ━━━━━ SOLANA SECTION ━━━━━
            val solanaConnected = connectedRails.containsKey("solana")

            RailSection(
                railName = "Solana",
                railId = "solana",
                isConnected = solanaConnected,
                tokens = solanaTokens,
                enabledMethods = enabledMethods,
                onToggle = { tokenId, enabled -> toggleMethod("solana", tokenId, enabled) },
                onConnect = onConnectSolana
            )

            Spacer(modifier = Modifier.height(WinoSpacing.LG))

            // ━━━━━ TRON SECTION ━━━━━
            val tronConnected = connectedRails.containsKey("tron")

            RailSection(
                railName = "TRON",
                railId = "tron",
                isConnected = tronConnected,
                tokens = tronTokens,
                enabledMethods = enabledMethods,
                onToggle = { tokenId, enabled -> toggleMethod("tron", tokenId, enabled) },
                onConnect = onConnectTron
            )

            Spacer(modifier = Modifier.height(WinoSpacing.LG))

            // ━━━━━ EVM/BSC SECTION ━━━━━
            val evmConnected = connectedRails.containsKey("evm")
            val evmConnection = connectedRails["evm"]
            val evmNetworkName = evmConnection?.networkId?.let {
                com.winopay.wallet.CaipUtils.getEvmNetworkDisplayName(it)
            } ?: "BNB Smart Chain"

            RailSection(
                railName = evmNetworkName,
                railId = "evm",
                isConnected = evmConnected,
                tokens = evmTokens,
                enabledMethods = enabledMethods,
                onToggle = { tokenId, enabled -> toggleMethod("evm", tokenId, enabled) },
                onConnect = null  // EVM connects via WalletConnect on Connect screen
            )

            Spacer(modifier = Modifier.height(WinoSpacing.LG))

            // ━━━━━ TON SECTION (Coming Soon) ━━━━━
            ComingSoonRailSection(
                railName = "TON",
                railId = "ton"
            )

            Spacer(modifier = Modifier.height(WinoSpacing.LG))
        }

        // Bottom
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(WinoSpacing.LG),
            verticalArrangement = Arrangement.spacedBy(WinoSpacing.SM)
        ) {
            // Helper text with link
            val helperText = buildAnnotatedString {
                append("Learn more about payment methods at ")
                pushStringAnnotation(tag = "URL", annotation = "https://winobank.com/payments")
                withStyle(
                    style = SpanStyle(
                        color = colors.brandPrimary,
                        textDecoration = TextDecoration.Underline
                    )
                ) {
                    append("winobank.com")
                }
                pop()
            }

            androidx.compose.foundation.text.ClickableText(
                text = helperText,
                style = WinoTypography.small.copy(color = colors.textSecondary),
                onClick = { offset ->
                    helperText.getStringAnnotations(tag = "URL", start = offset, end = offset)
                        .firstOrNull()?.let {
                            uriHandler.openUri(it.item)
                        }
                }
            )

            if (isOnboarding) {
                // Two buttons for onboarding
                WinoButton(
                    text = "Continue",
                    onClick = onContinue,
                    modifier = Modifier.fillMaxWidth(),
                    variant = WinoButtonVariant.Secondary,
                    size = WinoButtonSize.Large
                )

                WinoButton(
                    text = "Set all and skip",
                    onClick = onContinue,
                    modifier = Modifier.fillMaxWidth(),
                    variant = WinoButtonVariant.Primary,
                    size = WinoButtonSize.Large
                )
            } else {
                // Single button for settings
                WinoPrimaryButton(
                    text = "Done",
                    onClick = onContinue
                )
            }
        }
    }
}

/**
 * Section for a single rail with its tokens.
 */
@Composable
private fun RailSection(
    railName: String,
    railId: String,
    isConnected: Boolean,
    tokens: List<SupportedToken>,
    enabledMethods: Set<String>,
    onToggle: (tokenId: String, enabled: Boolean) -> Unit,
    onConnect: (() -> Unit)?
) {
    val colors = WinoTheme.colors

    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        // Section header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = WinoSpacing.SM),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(WinoSpacing.SM)
        ) {
            // Rail icon
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(
                        when (railId) {
                            "solana" -> colors.bgAccentSoft
                            "tron" -> colors.stateErrorSoft
                            "evm" -> colors.stateWarningSoft
                            else -> colors.bgSurfaceAlt
                        }
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = when (railId) {
                        "solana" -> "S"
                        "tron" -> "T"
                        "evm" -> "B"  // BSC
                        else -> railId.first().uppercase()
                    },
                    style = WinoTypography.smallMedium,
                    color = when (railId) {
                        "solana" -> colors.brandPrimary
                        "tron" -> colors.stateError
                        "evm" -> colors.stateWarning
                        else -> colors.textSecondary
                    }
                )
            }

            Text(
                text = railName,
                style = WinoTypography.h3Medium,
                color = colors.textPrimary,
                modifier = Modifier.weight(1f)
            )

            // Status badge
            if (isConnected) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(WinoRadius.SM))
                        .background(colors.stateSuccessSoft)
                        .padding(horizontal = WinoSpacing.XS, vertical = 2.dp)
                ) {
                    Text(
                        text = "Connected",
                        style = WinoTypography.micro,
                        color = colors.stateSuccess
                    )
                }
            }
        }

        // Tokens
        if (isConnected) {
            // Show toggleable tokens
            tokens.filter { it.isEnabled }.forEach { token ->
                val methodId = "$railId:${token.tokenId}"
                val isEnabled = enabledMethods.isEmpty() || enabledMethods.contains(methodId)

                TokenRow(
                    token = token,
                    railId = railId,
                    isEnabled = isEnabled,
                    canToggle = true,
                    onToggle = { enabled -> onToggle(token.tokenId, enabled) }
                )
            }

            // Show disabled tokens (if any)
            tokens.filter { !it.isEnabled }.forEach { token ->
                TokenRow(
                    token = token,
                    railId = railId,
                    isEnabled = false,
                    canToggle = false,
                    disabledReason = token.disabledReason,
                    onToggle = {}
                )
            }
        } else {
            // Rail not connected - show tokens as disabled with Connect button
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(WinoRadius.LG))
                    .background(colors.bgSurfaceAlt)
                    .padding(WinoSpacing.MD)
            ) {
                // Show preview of available tokens
                tokens.filter { it.isEnabled }.take(2).forEach { token ->
                    TokenRow(
                        token = token,
                        railId = railId,
                        isEnabled = false,
                        canToggle = false,
                        disabledReason = null,
                        onToggle = {}
                    )
                }

                // Connect button
                if (onConnect != null) {
                    Spacer(modifier = Modifier.height(WinoSpacing.SM))
                    WinoButton(
                        text = "Connect $railName wallet",
                        onClick = onConnect,
                        modifier = Modifier.fillMaxWidth(),
                        variant = WinoButtonVariant.Secondary,
                        size = WinoButtonSize.Large
                    )
                }
            }
        }
    }
}

/**
 * Section for a coming soon rail (TON, etc).
 */
@Composable
private fun ComingSoonRailSection(
    railName: String,
    railId: String
) {
    val colors = WinoTheme.colors

    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        // Section header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = WinoSpacing.SM),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(WinoSpacing.SM)
        ) {
            // Rail icon
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(colors.bgSurfaceAlt),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = railId.first().uppercase(),
                    style = WinoTypography.smallMedium,
                    color = colors.textMuted
                )
            }

            Text(
                text = railName,
                style = WinoTypography.h3Medium,
                color = colors.textTertiary,
                modifier = Modifier.weight(1f)
            )

            // Coming Soon badge
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(WinoRadius.SM))
                    .background(colors.bgSurfaceAlt)
                    .padding(horizontal = WinoSpacing.XS, vertical = 2.dp)
            ) {
                Text(
                    text = "Coming Soon",
                    style = WinoTypography.micro,
                    color = colors.textMuted
                )
            }
        }
    }
}

/**
 * Single token row with toggle or status.
 */
@Composable
private fun TokenRow(
    token: SupportedToken,
    railId: String,
    isEnabled: Boolean,
    canToggle: Boolean,
    disabledReason: String? = null,
    onToggle: (Boolean) -> Unit
) {
    val colors = WinoTheme.colors

    // Icon styling based on token
    val (iconBgColor, iconColor, iconText) = when (token.symbol) {
        "USDC" -> Triple(
            colors.stateSuccessSoft,
            colors.stateSuccess,
            "$"
        )
        "USDT" -> Triple(
            colors.stateInfoSoft,
            colors.stateInfo,
            "₮"
        )
        else -> Triple(
            colors.bgSurfaceAlt,
            colors.textSecondary,
            token.symbol.firstOrNull()?.toString() ?: "?"
        )
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = WinoSpacing.SM),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(WinoSpacing.MD)
    ) {
        // Token icon
        Box(
            modifier = Modifier
                .size(42.dp)
                .clip(CircleShape)
                .background(if (canToggle) iconBgColor else colors.bgSurfaceAlt),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = iconText,
                style = WinoTypography.h3Medium,
                color = if (canToggle) iconColor else colors.textMuted
            )
        }

        // Token info
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(WinoSpacing.XXS)
        ) {
            Text(
                text = token.symbol,
                style = WinoTypography.h3Medium,
                color = if (canToggle) colors.textPrimary else colors.textTertiary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = token.displayName,
                style = WinoTypography.small,
                color = if (canToggle) colors.textSecondary else colors.textTertiary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        // Trailing: toggle or status
        if (canToggle) {
            WinoToggle(
                checked = isEnabled,
                onCheckedChange = onToggle
            )
        } else if (disabledReason != null) {
            Text(
                text = disabledReason,
                style = WinoTypography.small,
                color = colors.textTertiary
            )
        }
    }
}
