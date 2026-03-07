package com.winopay.ui.screens.pos

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.winopay.WinoPayApplication
import com.winopay.ui.components.Numpad
import com.winopay.ui.components.NumpadKey
import com.winopay.ui.components.PhosphorIcons
import com.winopay.ui.components.QuickAmountChip
import com.winopay.ui.components.WinoPrimaryButton
import com.winopay.ui.components.handleNumpadInput
import com.winopay.ui.theme.WinoRadius
import com.winopay.ui.theme.WinoSpacing
import com.winopay.data.CurrencyConverter
import com.winopay.ui.theme.WinoTheme
import com.winopay.ui.theme.WinoTypography
import java.text.NumberFormat
import java.util.Locale

// Figma frame: 3.1 POS (793:19156)
// MCP source: get_screenshot, get_design_context for node 793:19156

private const val POS_TAG = "POSScreen"

/**
 * Stateful POS screen that integrates with PosManager.
 */
@Composable
fun POSScreenStateful(
    onCreateInvoice: (amount: Double) -> Unit,
    onBack: () -> Unit
) {
    val app = WinoPayApplication.instance
    val businessIdentity by app.dataStoreManager.businessIdentity.collectAsState(initial = null)
    val currency by app.dataStoreManager.currency.collectAsState(initial = "USD")
    var amount by remember { mutableStateOf("0") }

    // ━━━━━ USD BYPASS: Never require FX for USD merchants ━━━━━
    // USD merchants can ALWAYS create invoices (1 USD = 1 USDC)
    // CRITICAL: These must be computed fresh each recomposition, not cached!
    val requiresRates = CurrencyConverter.requiresRates(currency)
    val hasValidRates = CurrencyConverter.hasValidRates()
    // Only show rate error for non-USD currencies
    val rateError = CurrencyConverter.validateRatesForCurrency(currency)
    val rateInfo = CurrencyConverter.getSnapshotInfo()
    val rateDisplayString = CurrencyConverter.getRateDisplayString(currency)

    val isValidAmount = amount.toDoubleOrNull()?.let { it > 0 } ?: false
    // Block creation if no valid rates (BUT NOT FOR USD)
    val canCreate = isValidAmount && (!requiresRates || hasValidRates)

    // ━━━━━ DIAGNOSTIC LOGGING ━━━━━
    android.util.Log.d(POS_TAG, "━━━━━ POS SCREEN STATE ━━━━━")
    android.util.Log.d(POS_TAG, "  currency: $currency")
    android.util.Log.d(POS_TAG, "  requiresRates: $requiresRates")
    android.util.Log.d(POS_TAG, "  hasValidRates: $hasValidRates")
    android.util.Log.d(POS_TAG, "  rateError: ${rateError ?: "null"}")
    android.util.Log.d(POS_TAG, "  rateInfo: ${rateInfo?.let { "${it.provider} (${it.getAgeString()})" } ?: "NULL"}")
    android.util.Log.d(POS_TAG, "  isValidAmount: $isValidAmount")
    android.util.Log.d(POS_TAG, "  canCreate: $canCreate")
    android.util.Log.d(POS_TAG, "  FORMULA: canCreate = $isValidAmount && (!$requiresRates || $hasValidRates)")
    android.util.Log.d(POS_TAG, "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")

    POSScreenContent(
        businessName = businessIdentity?.name ?: "Business",
        businessLogoUri = businessIdentity?.logoUri,
        amount = amount,
        currency = currency,
        canCreate = canCreate,
        rateDisplayString = rateDisplayString,
        rateError = rateError,
        rateAge = rateInfo?.getAgeString(),
        onAmountChange = { amount = it },
        onCreate = {
            amount.toDoubleOrNull()?.let { onCreateInvoice(it) }
        },
        onBack = onBack
    )
}

/**
 * Pure UI content for POS screen.
 */
@Composable
fun POSScreenContent(
    businessName: String,
    businessLogoUri: String?,
    amount: String,
    currency: String,
    canCreate: Boolean,
    rateDisplayString: String? = null,
    rateError: String? = null,
    rateAge: String? = null,
    onAmountChange: (String) -> Unit,
    onCreate: () -> Unit,
    onBack: () -> Unit
) {
    val colors = WinoTheme.colors

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
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(WinoSpacing.SM)
            ) {
                Box(
                    modifier = Modifier
                        .size(WinoSpacing.XL)
                        .clip(RoundedCornerShape(WinoRadius.XL)),
                    contentAlignment = Alignment.Center
                ) {
                    businessLogoUri?.let { uri ->
                        AsyncImage(
                            model = Uri.parse(uri),
                            contentDescription = "Business logo",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } ?: Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(colors.bgSurface),
                        contentAlignment = Alignment.Center
                    ) {
                        PhosphorIcons.Storefront(
                            size = 16.dp,
                            color = colors.brandPrimary
                        )
                    }
                }

                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(WinoSpacing.XXS)
                    ) {
                        Text(
                            text = businessName,
                            style = WinoTypography.h3Medium,
                            color = colors.textPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        PhosphorIcons.SealCheck(
                            size = 16.dp,
                            color = colors.brandPrimary
                        )
                    }
                    Text(
                        text = "Verified business",
                        style = WinoTypography.micro,
                        color = colors.textSecondary
                    )
                }
            }

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

        // Input area
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(
                    horizontal = WinoSpacing.LG,
                    vertical = WinoSpacing.XXL
                ),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(WinoSpacing.MD, Alignment.CenterVertically)
        ) {
            Text(
                text = "You\u2019ll receive",
                style = WinoTypography.body,
                color = colors.textTertiary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(WinoSpacing.XXS)
            ) {
                val displayAmount = formatAmount(amount)
                Text(
                    text = "$displayAmount $currency",
                    style = WinoTypography.display,
                    color = colors.textPrimary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )

                // Convert to USD with proper precision (2 decimal places)
                val usdValue = amount.toDoubleOrNull()?.let {
                    CurrencyConverter.convertToUsd(it, currency)
                }
                // Show 2 decimal places for accurate preview
                val usdDisplay = usdValue?.let { String.format(Locale.US, "%.2f", it) }

                if (usdDisplay != null) {
                    Text(
                        text = "≈ $$usdDisplay",
                        style = WinoTypography.h3Medium,
                        color = colors.textTertiary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // Show rate info for transparency
                if (rateDisplayString != null && currency != "USD") {
                    Text(
                        text = "Rate: $rateDisplayString" + (rateAge?.let { " ($it)" } ?: ""),
                        style = WinoTypography.small,
                        color = colors.textMuted,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // Show rate error if no rates available
                if (rateError != null) {
                    Text(
                        text = "⚠️ $rateError",
                        style = WinoTypography.small,
                        color = colors.stateError,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }

        // Numpad
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = WinoSpacing.LG),
            verticalArrangement = Arrangement.spacedBy(WinoSpacing.MD)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(WinoSpacing.SM)
            ) {
                listOf(10, 50, 100).forEach { quickAmount ->
                    QuickAmountChip(
                        label = "+$quickAmount",
                        onClick = {
                            val current = amount.toDoubleOrNull() ?: 0.0
                            val newAmount = (current + quickAmount).let {
                                if (it == it.toLong().toDouble()) it.toLong().toString()
                                else it.toString()
                            }
                            onAmountChange(newAmount)
                        },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            Numpad(
                onKeyPress = { key ->
                    onAmountChange(handleNumpadInput(amount, key))
                },
                showDecimal = true
            )
        }

        // Bottom button
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(
                    start = WinoSpacing.LG,
                    end = WinoSpacing.LG,
                    top = WinoSpacing.SM,
                    bottom = WinoSpacing.SM
                )
        ) {
            WinoPrimaryButton(
                text = "Create",
                onClick = onCreate,
                enabled = canCreate
            )
        }
    }
}

private fun formatAmount(amount: String): String {
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
