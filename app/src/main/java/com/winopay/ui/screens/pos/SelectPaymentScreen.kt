package com.winopay.ui.screens.pos

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.winopay.WinoPayApplication
import com.winopay.data.CurrencyConverter
import com.winopay.data.pos.PaymentMethod
import com.winopay.data.profile.MerchantProfileStore
import com.winopay.data.profile.RailConnection
import com.winopay.payments.PaymentRailFactory
import com.winopay.payments.SupportedToken
import com.winopay.ui.components.WinoButton
import com.winopay.ui.components.WinoButtonSize
import com.winopay.ui.components.WinoButtonVariant
import com.winopay.ui.theme.WinoRadius
import com.winopay.ui.theme.WinoSpacing
import com.winopay.ui.theme.WinoTheme
import com.winopay.ui.theme.WinoTypography
import java.text.NumberFormat
import java.util.Locale

private const val TAG = "SelectPaymentScreen"

/**
 * Data class for available payment method in POS.
 */
data class AvailablePosMethod(
    val railId: String,
    val tokenId: String,
    val symbol: String,
    val displayName: String,
    val networkDisplayName: String,
    val isPrimary: Boolean = false  // First method is primary (highlighted button)
)

/**
 * Stateful SelectPayment screen that integrates with PosManager.
 *
 * MULTICHAIN SUPPORT:
 * - Reads connectedRails from MerchantProfileStore
 * - Reads enabledMethods from MerchantProfileStore
 * - Shows ALL enabled methods from ALL connected rails
 * - If enabledMethods is empty, defaults to all supported tokens
 */
@Composable
fun SelectPaymentScreenStateful(
    amount: Double,
    invoiceId: String,
    onSelectMethod: (PaymentMethod) -> Unit,
    onCancel: () -> Unit
) {
    val app = WinoPayApplication.instance
    val context = LocalContext.current
    val currency by app.dataStoreManager.currency.collectAsState(initial = "USD")
    val businessIdentity by app.dataStoreManager.businessIdentity.collectAsState(initial = null)

    // SINGLE SOURCE OF TRUTH: MerchantProfileStore
    val profileStore = remember { MerchantProfileStore(context) }

    // Observe connected rails and enabled methods
    val connectedRails by profileStore.observeConnectedRails().collectAsState(initial = emptyMap())
    val enabledMethods by profileStore.observeEnabledMethods().collectAsState(initial = emptySet())

    // Build available payment methods
    var availableMethods by remember { mutableStateOf<List<AvailablePosMethod>>(emptyList()) }

    LaunchedEffect(connectedRails, enabledMethods) {
        availableMethods = buildAvailableMethods(connectedRails, enabledMethods)
        Log.i(TAG, "POS|METHODS|methods=${availableMethods.map { "${it.railId}:${it.symbol}" }}")
    }

    SelectPaymentScreen(
        amount = amount.toString(),
        currency = currency,
        businessName = businessIdentity?.name ?: "Business",
        availableMethods = availableMethods,
        onSelectMethod = { method ->
            val paymentMethod = PaymentMethod(
                railId = method.railId,
                tokenId = method.tokenId,
                symbol = method.symbol,
                displayName = method.displayName,
                networkDisplayName = method.networkDisplayName
            )
            onSelectMethod(paymentMethod)
        },
        onCancel = onCancel
    )
}

/**
 * Build list of available payment methods from connected rails and enabled methods.
 */
private suspend fun buildAvailableMethods(
    connectedRails: Map<String, RailConnection>,
    enabledMethods: Set<String>
): List<AvailablePosMethod> {
    val methods = mutableListOf<AvailablePosMethod>()

    Log.d(TAG, "METHODS|BUILD|connectedRails=${connectedRails.keys}|enabledMethods=$enabledMethods")

    for ((railId, connection) in connectedRails) {
        try {
            val rail = PaymentRailFactory.getRailByRailId(railId, connection.networkId)
            val tokens = rail.getSupportedTokens().filter { it.isEnabled }

            for (token in tokens) {
                val methodId = "$railId:${token.tokenId}"

                // Check if method is enabled (empty set = all enabled)
                val isEnabled = enabledMethods.isEmpty() || enabledMethods.contains(methodId)
                if (!isEnabled) {
                    Log.d(TAG, "METHODS|SKIP|$methodId|disabled in settings")
                    continue
                }

                val networkDisplayName = when (railId) {
                    "solana" -> "on Solana"
                    "tron" -> "on TRON"
                    else -> "on $railId"
                }

                methods.add(
                    AvailablePosMethod(
                        railId = railId,
                        tokenId = token.tokenId,
                        symbol = token.symbol,
                        displayName = "${token.symbol} $networkDisplayName",
                        networkDisplayName = networkDisplayName,
                        isPrimary = methods.isEmpty()  // First method is primary
                    )
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "METHODS|ERROR|railId=$railId|error=${e.message}")
        }
    }

    return methods
}

/**
 * SelectPayment screen UI component.
 */
@Composable
fun SelectPaymentScreen(
    amount: String,
    currency: String,
    businessName: String,
    availableMethods: List<AvailablePosMethod>,
    onSelectMethod: (AvailablePosMethod) -> Unit,
    onCancel: () -> Unit
) {
    val colors = WinoTheme.colors

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
                text = "Select payment method",
                style = WinoTypography.h1Medium,
                color = colors.textPrimary
            )
            Text(
                text = "Choose how your customer will pay",
                style = WinoTypography.body,
                color = colors.textSecondary
            )
        }

        // Content
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = WinoSpacing.LG),
            verticalArrangement = Arrangement.spacedBy(WinoSpacing.LG)
        ) {
            // Amount card
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(WinoRadius.XL))
                    .background(colors.bgSurface)
                    .padding(
                        horizontal = WinoSpacing.MD,
                        vertical = WinoSpacing.XL
                    ),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(WinoSpacing.XXS)
            ) {
                val displayAmount = formatPaymentAmount(amount)
                Text(
                    text = "$displayAmount $currency",
                    style = WinoTypography.display,
                    color = colors.textPrimary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )

                val usdValue = amount.toDoubleOrNull()?.let {
                    CurrencyConverter.convertToUsd(it, currency)
                } ?: 0.0
                Text(
                    text = "\$${String.format("%.0f", usdValue)} \u2022 $businessName",
                    style = WinoTypography.body,
                    color = colors.textSecondary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // Payment methods
            if (availableMethods.isEmpty()) {
                // No methods available
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(WinoRadius.LG))
                        .background(colors.stateWarningSoft)
                        .padding(WinoSpacing.MD),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(WinoSpacing.SM)
                ) {
                    Text(
                        text = "No payment methods available",
                        style = WinoTypography.h3Medium,
                        color = colors.stateWarning
                    )
                    Text(
                        text = "Connect a wallet and enable payment methods in Settings.",
                        style = WinoTypography.small,
                        color = colors.textSecondary,
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                // Group methods by rail for display
                val methodsByRail = availableMethods.groupBy { it.railId }

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(WinoSpacing.SM)
                ) {
                    // Show primary method first (highlighted)
                    val primaryMethod = availableMethods.firstOrNull()
                    if (primaryMethod != null) {
                        PaymentMethodButton(
                            method = primaryMethod,
                            isPrimary = true,
                            onClick = { onSelectMethod(primaryMethod) }
                        )
                    }

                    // Show "Other Payment methods" divider if there are more methods
                    if (availableMethods.size > 1) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = WinoSpacing.XS),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(WinoSpacing.XS)
                        ) {
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(1.dp)
                                    .background(colors.borderSubtle)
                            )
                            Text(
                                text = "Other Payment methods",
                                style = WinoTypography.micro,
                                color = colors.textTertiary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(1.dp)
                                    .background(colors.borderSubtle)
                            )
                        }

                        // Other methods (secondary buttons)
                        availableMethods.drop(1).forEach { method ->
                            PaymentMethodButton(
                                method = method,
                                isPrimary = false,
                                onClick = { onSelectMethod(method) }
                            )
                        }
                    }
                }
            }
        }

        // Bottom
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Customer will scan QR code to pay",
                style = WinoTypography.small,
                color = colors.textSecondary,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        horizontal = WinoSpacing.XL,
                        vertical = WinoSpacing.XS
                    )
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        horizontal = WinoSpacing.LG,
                        vertical = WinoSpacing.SM
                    )
            ) {
                WinoButton(
                    text = "Revoke invoice",
                    onClick = onCancel,
                    modifier = Modifier.fillMaxWidth(),
                    variant = WinoButtonVariant.Destructive,
                    size = WinoButtonSize.Large
                )
            }
        }
    }
}

/**
 * Payment method button component.
 */
@Composable
private fun PaymentMethodButton(
    method: AvailablePosMethod,
    isPrimary: Boolean,
    onClick: () -> Unit
) {
    val colors = WinoTheme.colors

    // Icon styling based on token
    val (iconBgColor, iconColor, iconText) = when (method.symbol) {
        "USDC" -> Triple(colors.stateSuccessSoft, colors.stateSuccess, "$")
        "USDT" -> Triple(colors.stateInfoSoft, colors.stateInfo, "₮")
        else -> Triple(colors.bgSurfaceAlt, colors.textSecondary, method.symbol.first().toString())
    }

    // Rail badge styling
    val (railBadgeBg, railBadgeColor, railBadgeText) = when (method.railId) {
        "solana" -> Triple(colors.bgAccentSoft, colors.brandPrimary, "S")
        "tron" -> Triple(colors.stateErrorSoft, colors.stateError, "T")
        else -> Triple(colors.bgSurfaceAlt, colors.textSecondary, method.railId.first().uppercase())
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(WinoRadius.XL))
            .background(if (isPrimary) colors.brandPrimary else colors.bgSurface)
            .clickable(onClick = onClick)
            .padding(WinoSpacing.MD),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(WinoSpacing.MD)
    ) {
        // Token icon with rail badge
        Box {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(if (isPrimary) colors.bgCanvas.copy(alpha = 0.2f) else iconBgColor),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = iconText,
                    style = WinoTypography.h3Medium,
                    color = if (isPrimary) colors.textOnBrand else iconColor
                )
            }
            // Rail badge
            Box(
                modifier = Modifier
                    .size(20.dp)
                    .clip(CircleShape)
                    .background(railBadgeBg)
                    .align(Alignment.BottomEnd),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = railBadgeText,
                    style = WinoTypography.micro,
                    color = railBadgeColor
                )
            }
        }

        // Method info
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(WinoSpacing.XXS)
        ) {
            Text(
                text = "Pay with ${method.symbol}",
                style = WinoTypography.h3Medium,
                color = if (isPrimary) colors.textOnBrand else colors.textPrimary
            )
            Text(
                text = method.networkDisplayName,
                style = WinoTypography.small,
                color = if (isPrimary) colors.textOnBrand.copy(alpha = 0.7f) else colors.textSecondary
            )
        }
    }
}

private fun formatPaymentAmount(amount: String): String {
    val value = amount.toDoubleOrNull() ?: return "0"
    if (amount.contains(".")) {
        val parts = amount.split(".")
        val intPart = parts[0].toLongOrNull() ?: 0
        val formatted = NumberFormat.getNumberInstance(Locale.US).format(intPart)
        return "$formatted.${parts.getOrElse(1) { "" }}"
    }
    val longValue = amount.toLongOrNull() ?: return "0"
    return NumberFormat.getNumberInstance(Locale.US).format(longValue)
}

// ═══════════════════════════════════════════════════════════════════════════════
// LEGACY SUPPORT
// ═══════════════════════════════════════════════════════════════════════════════

/**
 * Legacy SelectPayment screen signature for backwards compatibility.
 * @deprecated Use SelectPaymentScreenStateful instead
 */
@Composable
@Deprecated("Use SelectPaymentScreenStateful instead")
fun SelectPaymentScreen(
    amount: String,
    reference: String,
    onPayWithUSDC: () -> Unit,
    onPayWithUSDT: () -> Unit,
    onRevokeInvoice: () -> Unit
) {
    val app = WinoPayApplication.instance
    val currency by app.dataStoreManager.currency.collectAsState(initial = "USD")
    val businessIdentity by app.dataStoreManager.businessIdentity.collectAsState(initial = null)

    // Create legacy methods
    val legacyMethods = listOf(
        AvailablePosMethod(
            railId = "solana",
            tokenId = com.winopay.BuildConfig.USDC_MINT,
            symbol = "USDC",
            displayName = "USDC on Solana",
            networkDisplayName = "on Solana",
            isPrimary = true
        ),
        AvailablePosMethod(
            railId = "solana",
            tokenId = com.winopay.BuildConfig.USDT_MINT,
            symbol = "USDT",
            displayName = "USDT on Solana",
            networkDisplayName = "on Solana",
            isPrimary = false
        )
    )

    SelectPaymentScreen(
        amount = amount,
        currency = currency,
        businessName = businessIdentity?.name ?: "Business",
        availableMethods = legacyMethods,
        onSelectMethod = { method ->
            when (method.symbol) {
                "USDC" -> onPayWithUSDC()
                "USDT" -> onPayWithUSDT()
            }
        },
        onCancel = onRevokeInvoice
    )
}
