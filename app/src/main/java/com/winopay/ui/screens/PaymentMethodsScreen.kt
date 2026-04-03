package com.winopay.ui.screens

import android.util.Log
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.ui.res.painterResource
import com.winopay.R
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.winopay.data.profile.MerchantProfileStore
import com.winopay.i18n.L
import com.winopay.payments.PaymentRailFactory
import com.winopay.payments.SupportedToken
import com.winopay.ui.components.BlurredGradientOverlay
import com.winopay.ui.components.WinoButton
import com.winopay.ui.components.WinoButtonSize
import com.winopay.ui.components.WinoButtonVariant
import com.winopay.ui.components.WinoPrimaryButton
import com.winopay.ui.components.WinoRadioSwitch
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

    // Build list of active payment methods (connected rails with tokens)
    val activePaymentMethods = remember(connectedRails, solanaTokens, tronTokens, evmTokens) {
        val methods = mutableListOf<PaymentMethodItem>()

        // TRON tokens first (as shown in Figma)
        if (connectedRails.containsKey("tron")) {
            tronTokens.filter { it.isEnabled }.forEach { token ->
                methods.add(PaymentMethodItem(
                    railId = "tron",
                    token = token,
                    networkName = "on Tron (TRC-20)",
                    tokenBgColor = Color(0xFF1BA27A), // USDT green
                    networkBgColor = Color(0xFFFE0A0E)  // Tron red
                ))
            }
        }

        // Solana tokens
        if (connectedRails.containsKey("solana")) {
            solanaTokens.filter { it.isEnabled }.forEach { token ->
                methods.add(PaymentMethodItem(
                    railId = "solana",
                    token = token,
                    networkName = "on Solana",
                    tokenBgColor = if (token.symbol == "USDC") Color(0xFF0088FF) else Color(0xFF1BA27A),
                    networkBgColor = Color(0xFF7C5CFF)  // Solana purple
                ))
            }
        }

        methods
    }

    // Figma: 1.6 Payment methods (1343:13921)
    val scrollState = rememberScrollState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.bgCanvas)
    ) {
        // Scrollable content
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(scrollState)
                .padding(bottom = 200.dp) // Space for bottom overlay
        ) {
            // Header: pt=60dp (safe area), px=24dp, py=16dp, gap=4dp (Figma: 1343:13922)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = WinoSpacing.XL) // Safe area top = 32dp per Figma
                    .padding(horizontal = WinoSpacing.LG, vertical = WinoSpacing.MD),
                verticalArrangement = Arrangement.spacedBy(WinoSpacing.XXS)
            ) {
                // Title: H1 Medium (Figma: 1343:13924)
                Text(
                    text = if (isOnboarding) L("onboarding.payment_methods.title_setup") else L("onboarding.payment_methods.title_settings"),
                    style = WinoTypography.h1Medium,
                    color = colors.textPrimary
                )
                // Subtitle: Body Regular (Figma: 1343:13925)
                Text(
                    text = if (isOnboarding) {
                        L("onboarding.payment_methods.subtitle_setup")
                    } else {
                        L("onboarding.payment_methods.subtitle_settings")
                    },
                    style = WinoTypography.body,
                    color = colors.textSecondary
                )
            }

            // Content (Figma: 1343:13926)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = WinoSpacing.MD)
            ) {
            // Main payment methods container: bg=surface, rounded=36dp (Figma: 1343:13927)
            // NO internal padding on container - each row handles its own padding
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(36.dp))
                    .background(colors.bgSurface)
            ) {
                val isSolanaConnected = connectedRails.containsKey("solana")
                val isTronConnected = connectedRails.containsKey("tron")

                // Solana USDC - always show (Figma: 1343-14760)
                solanaTokens.filter { it.symbol == "USDC" }.firstOrNull()?.let { token ->
                    val methodId = "solana:${token.tokenId}"
                    val isEnabled = isSolanaConnected && (enabledMethods.isEmpty() || enabledMethods.contains(methodId))
                    PaymentMethodRow(
                        item = PaymentMethodItem(
                            railId = "solana",
                            token = token,
                            networkName = L("onboarding.payment_methods.solana_label"),
                            tokenBgColor = Color(0xFF0088FF),
                            networkBgColor = Color(0xFF7C5CFF)
                        ),
                        isEnabled = isEnabled,
                        onToggle = if (isSolanaConnected) {
                            { enabled -> toggleMethod("solana", token.tokenId, enabled) }
                        } else null
                    )
                }

                // Solana USDT - always show
                solanaTokens.filter { it.symbol == "USDT" }.firstOrNull()?.let { token ->
                    val methodId = "solana:${token.tokenId}"
                    val isEnabled = isSolanaConnected && (enabledMethods.isEmpty() || enabledMethods.contains(methodId))
                    PaymentMethodRow(
                        item = PaymentMethodItem(
                            railId = "solana",
                            token = token,
                            networkName = L("onboarding.payment_methods.solana_label"),
                            tokenBgColor = Color(0xFF1BA27A),
                            networkBgColor = Color(0xFF7C5CFF)
                        ),
                        isEnabled = isEnabled,
                        onToggle = if (isSolanaConnected) {
                            { enabled -> toggleMethod("solana", token.tokenId, enabled) }
                        } else null
                    )
                }

                // TRON USDT - show Connect button if not connected (Figma: 1383-3391)
                tronTokens.filter { it.symbol == "USDT" }.firstOrNull()?.let { token ->
                    if (isTronConnected) {
                        // TRON connected - show toggle
                        val methodId = "tron:${token.tokenId}"
                        val isEnabled = enabledMethods.isEmpty() || enabledMethods.contains(methodId)
                        PaymentMethodRow(
                            item = PaymentMethodItem(
                                railId = "tron",
                                token = token,
                                networkName = L("onboarding.payment_methods.tron_label"),
                                tokenBgColor = Color(0xFF1BA27A),
                                networkBgColor = Color(0xFFFE0A0E)
                            ),
                            isEnabled = isEnabled,
                            onToggle = { enabled -> toggleMethod("tron", token.tokenId, enabled) }
                        )
                    } else {
                        // TRON not connected - show Connect button
                        ConnectRailRow(
                            tokenSymbol = "USDT",
                            networkName = L("onboarding.payment_methods.tron_label"),
                            tokenBgColor = Color(0xFF1BA27A),
                            networkBgColor = Color(0xFFFE0A0E),
                            onConnect = onConnectTron
                        )
                    }
                }
            }

            }
        }

        // Bottom section with gradient overlay - ABSOLUTELY POSITIONED (Figma: 1343:13969)
        // Figma: absolute bottom-0, backdrop-blur-[10px], bg-gradient-to-b from-transparent to-canvas
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
        ) {
            BlurredGradientOverlay(
                canvasColor = colors.bgCanvas
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = WinoSpacing.MD) // Safe area bottom
                ) {
                    // Helper section: pt=24dp, pb=16dp, px=32dp, gap=8dp (Figma helper component)
                    if (isOnboarding) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(
                                    top = WinoSpacing.LG,
                                    bottom = WinoSpacing.MD,
                                    start = WinoSpacing.XL,
                                    end = WinoSpacing.XL
                                ),
                            verticalArrangement = Arrangement.spacedBy(WinoSpacing.XXS)
                        ) {
                            // Helper text: Small Regular
                            Text(
                                text = L("onboarding.payment_methods.disclaimer"),
                                style = WinoTypography.small,
                                color = colors.textSecondary
                            )
                            // "Learn more" link: Small Medium, brand color
                            Text(
                                text = L("common.learn_more"),
                                style = WinoTypography.smallMedium,
                                color = colors.brandPrimary,
                                modifier = Modifier.clickable {
                                    uriHandler.openUri("https://winobank.com/docs/legal/termsofuse")
                                }
                            )
                        }
                    }

                    // Button layout: p=24dp, gap=12dp (Figma: 1343:13971)
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(WinoSpacing.LG),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        if (isOnboarding) {
                            // Button 1: "Continue" - Secondary (Figma: 1343:13972)
                            WinoButton(
                                text = L("common.continue"),
                                onClick = onContinue,
                                modifier = Modifier.fillMaxWidth(),
                                variant = WinoButtonVariant.Secondary,
                                size = WinoButtonSize.Large
                            )

                            // Button 2: "Set all and skip" - Primary (Figma: 1343:13973)
                            WinoButton(
                                text = L("onboarding.payment_methods.skip"),
                                onClick = onContinue,
                                modifier = Modifier.fillMaxWidth(),
                                variant = WinoButtonVariant.Primary,
                                size = WinoButtonSize.Large
                            )
                        } else {
                            // Single button for settings
                            WinoPrimaryButton(
                                text = L("common.done"),
                                onClick = onContinue
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Data class for payment method items.
 */
private data class PaymentMethodItem(
    val railId: String,
    val token: SupportedToken,
    val networkName: String,
    val tokenBgColor: Color,
    val networkBgColor: Color
)

/**
 * Payment method row matching Figma design (1343:13928).
 *
 * Layout: px=16dp, py=16dp, gap=16dp (same as SettingsCurrencyScreen rows)
 * Icon: 36dp with token color + network badge
 * Text: H3 Medium (18sp) name, Small Regular (14sp) network
 * Toggle: WinoRadioSwitch (52dp)
 */
@Composable
private fun PaymentMethodRow(
    item: PaymentMethodItem,
    isEnabled: Boolean,
    onToggle: ((Boolean) -> Unit)?
) {
    val colors = WinoTheme.colors

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = WinoSpacing.MD, vertical = WinoSpacing.MD),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(WinoSpacing.MD)
    ) {
        // Token icon with network badge (Figma: 1182:10241)
        Box(modifier = Modifier.size(36.dp)) {
            // Token icon - full PNG with colored background from cryptologos.cc
            Image(
                painter = painterResource(
                    id = if (item.token.symbol == "USDC") {
                        R.drawable.usdc_icon
                    } else {
                        R.drawable.usdt_icon
                    }
                ),
                contentDescription = item.token.symbol,
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
            )
            // Network badge (bottom-right) with PNG logo
            // Border OUTSIDE: outer box with border, inner box with color
            Box(
                modifier = Modifier
                    .size(21.dp) // 17dp + 2*2dp border
                    .align(Alignment.BottomEnd)
                    .offset(x = 5.dp, y = 5.dp)
                    .clip(CircleShape)
                    .background(colors.bgSurface), // Border color (canvas/surface)
                contentAlignment = Alignment.Center
            ) {
                // Inner colored circle
                Box(
                    modifier = Modifier
                        .size(17.dp)
                        .clip(CircleShape)
                        .background(item.networkBgColor),
                    contentAlignment = Alignment.Center
                ) {
                    // Network PNG logo (white on transparent)
                    Image(
                        painter = painterResource(
                            id = when (item.railId) {
                                "tron" -> R.drawable.network_tron
                                "solana" -> R.drawable.network_solana
                                else -> R.drawable.network_solana
                            }
                        ),
                        contentDescription = item.railId,
                        modifier = Modifier.size(10.dp)
                    )
                }
            }
        }

        // Text section (Figma: 1343:13930)
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(0.dp)
        ) {
            // Token name: H3 Medium (Figma: 1343:13931)
            Text(
                text = item.token.symbol,
                style = WinoTypography.h3Medium,
                color = colors.textPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            // Network: Small Regular (Figma: 1343:13932)
            Text(
                text = item.networkName,
                style = WinoTypography.small,
                color = colors.textTertiary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        // Toggle switch
        WinoRadioSwitch(
            checked = isEnabled,
            onCheckedChange = onToggle,
            enabled = onToggle != null,
            showLockIcon = onToggle == null
        )
    }
}

/**
 * Coming soon payment row (Figma: 1343:13951).
 *
 * Layout: py=12dp, gap=16dp (slightly smaller than active rows)
 * Shows "Soon" text instead of toggle
 */
@Composable
private fun ComingSoonPaymentRow(
    tokenSymbol: String,
    networkName: String,
    tokenBgColor: Color,
    networkBgColor: Color
) {
    val colors = WinoTheme.colors

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = WinoSpacing.MD, vertical = WinoSpacing.SM),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(WinoSpacing.MD)
    ) {
        // Token icon with network badge (Figma: 1182:10241)
        Box(modifier = Modifier.size(36.dp)) {
            // Token icon - full PNG with colored background from cryptologos.cc
            Image(
                painter = painterResource(
                    id = if (tokenSymbol == "USDC") {
                        R.drawable.usdc_icon
                    } else {
                        R.drawable.usdt_icon
                    }
                ),
                contentDescription = tokenSymbol,
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
            )
            // Network badge with PNG logo
            // Border OUTSIDE: outer box with border, inner box with color
            Box(
                modifier = Modifier
                    .size(21.dp) // 17dp + 2*2dp border
                    .align(Alignment.BottomEnd)
                    .offset(x = 5.dp, y = 5.dp)
                    .clip(CircleShape)
                    .background(colors.bgCanvas), // Border color
                contentAlignment = Alignment.Center
            ) {
                // Inner colored circle
                Box(
                    modifier = Modifier
                        .size(17.dp)
                        .clip(CircleShape)
                        .background(networkBgColor),
                    contentAlignment = Alignment.Center
                ) {
                    // Network PNG logo (white on transparent)
                    Image(
                        painter = painterResource(
                            id = when {
                                networkName.contains("Binance") -> R.drawable.network_bsc
                                networkName.contains("TON") -> R.drawable.network_ton
                                else -> R.drawable.network_solana
                            }
                        ),
                        contentDescription = networkName,
                        modifier = Modifier.size(10.dp)
                    )
                }
            }
        }

        // Text section
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(0.dp)
        ) {
            Text(
                text = tokenSymbol,
                style = WinoTypography.h3Medium,
                color = colors.textPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = networkName,
                style = WinoTypography.small,
                color = colors.textTertiary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        // "Soon" text (Figma: 1343:13956)
        Text(
            text = L("common.soon"),
            style = WinoTypography.bodyMedium,
            color = colors.textTertiary
        )
    }
}

/**
 * Connect rail row (Figma: 1383-3391).
 *
 * Shows payment method with "Connect" button for unconnected rails.
 * Layout matches PaymentMethodRow but with Connect button instead of toggle.
 */
@Composable
private fun ConnectRailRow(
    tokenSymbol: String,
    networkName: String,
    tokenBgColor: Color,
    networkBgColor: Color,
    onConnect: (() -> Unit)?
) {
    val colors = WinoTheme.colors

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = WinoSpacing.MD, vertical = WinoSpacing.MD),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(WinoSpacing.MD)
    ) {
        // Token icon with network badge
        Box(modifier = Modifier.size(36.dp)) {
            Image(
                painter = painterResource(
                    id = if (tokenSymbol == "USDC") {
                        R.drawable.usdc_icon
                    } else {
                        R.drawable.usdt_icon
                    }
                ),
                contentDescription = tokenSymbol,
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
            )
            // Network badge
            Box(
                modifier = Modifier
                    .size(21.dp)
                    .align(Alignment.BottomEnd)
                    .offset(x = 5.dp, y = 5.dp)
                    .clip(CircleShape)
                    .background(colors.bgSurface),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(17.dp)
                        .clip(CircleShape)
                        .background(networkBgColor),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = painterResource(
                            id = when {
                                networkName.contains("Tron") -> R.drawable.network_tron
                                networkName.contains("Solana") -> R.drawable.network_solana
                                else -> R.drawable.network_solana
                            }
                        ),
                        contentDescription = networkName,
                        modifier = Modifier.size(10.dp)
                    )
                }
            }
        }

        // Text section
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(0.dp)
        ) {
            Text(
                text = tokenSymbol,
                style = WinoTypography.h3Medium,
                color = colors.textPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = networkName,
                style = WinoTypography.small,
                color = colors.textTertiary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        // Connect button (Figma: 1383-3391) - Small, Primary
        WinoButton(
            text = L("common.connect"),
            onClick = { onConnect?.invoke() },
            variant = WinoButtonVariant.Primary,
            size = WinoButtonSize.Small
        )
    }
}

