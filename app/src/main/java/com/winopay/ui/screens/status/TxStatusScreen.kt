package com.winopay.ui.screens.status

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.winopay.R
import com.winopay.WinoPayApplication
import com.winopay.data.CurrencyConverter
import com.winopay.ui.components.PhosphorIcons
import com.winopay.ui.components.WinoButton
import com.winopay.ui.components.WinoButtonSize
import com.winopay.ui.components.WinoButtonVariant
import com.winopay.ui.theme.WinoRadius
import com.winopay.ui.theme.WinoSpacing
import com.winopay.ui.theme.WinoTheme
import com.winopay.ui.theme.WinoTypography
import java.text.NumberFormat
import java.util.Locale

// Figma frames:
// 3.3.1 TX status - success (793:19195)
// 3.3.2 TX status - detected (793:9318)
// 3.3.3 TX status - failed (793:9336)

/**
 * Stateful TxStatus screen that integrates with PosManager.
 *
 * @param onCancel Optional callback for canceling pending payments.
 *                 When provided, shows a cancel button during pending state.
 */
@Composable
fun TxStatusScreenStateful(
    statusType: String,
    amount: Double,
    signature: String?,
    onDone: () -> Unit,
    onNewPayment: () -> Unit,
    onCancel: (() -> Unit)? = null
) {
    TxStatusScreen(
        statusType = statusType,
        amount = amount.toString(),
        signature = signature,
        onDone = onDone,
        onNewPayment = onNewPayment,
        onCancel = onCancel
    )
}

/**
 * Legacy TxStatus screen for backwards compatibility.
 *
 * @param onCancel Optional callback for canceling pending payments.
 */
@Composable
fun TxStatusScreen(
    statusType: String,
    amount: String,
    signature: String?,
    onDone: () -> Unit,
    onNewPayment: () -> Unit,
    onCancel: (() -> Unit)? = null
) {
    val colors = WinoTheme.colors
    val app = WinoPayApplication.instance

    val businessIdentity by app.dataStoreManager.businessIdentity.collectAsState(initial = null)
    val currency by app.dataStoreManager.currency.collectAsState(initial = "THB")

    val isSuccess = statusType == "success"
    val isPending = statusType == "pending"
    val isExpired = statusType == "expired"
    val isFailed = statusType == "failed" || (!isSuccess && !isPending && !isExpired)

    // Status card config
    val statusTitle = when {
        isSuccess -> "Payment succesful"
        isPending -> "Payment detected"
        isExpired -> "Invoice expired"
        else -> "Payment failed"
    }

    val statusSubtitle = when {
        isSuccess -> {
            val shortSig = signature?.let {
                if (it.length > 8) "${it.take(4)}\u2026${it.takeLast(4)}" else it
            } ?: ""
            val businessName = businessIdentity?.name ?: "Business"
            "$businessName \u2022 $shortSig"
        }
        isPending -> "Confirming transaction..."
        isExpired -> "The invoice has expired."
        else -> "Please try again."
    }

    val helperText = when {
        isSuccess -> "Paid with..."
        isPending -> "Please wait..."
        isExpired -> "Create a new invoice to continue."
        else -> "Something went wrong."
    }

    val buttonText = when {
        isSuccess -> "Close"
        isPending -> "Close"
        isExpired -> "New payment"
        else -> "Try again"
    }

    // Amount subtitle
    val shortRef = signature?.let {
        if (it.length > 8) "${it.take(4)}\u2026${it.takeLast(4)}" else it
    } ?: ""
    val usdValue = amount.toDoubleOrNull()?.let {
        CurrencyConverter.convertToUsd(it, currency)
    } ?: 0.0
    val businessName = businessIdentity?.name ?: "Test name"
    val amountSubtitle = "\$${String.format("%.0f", usdValue)} \u2022 $businessName \u2022 $shortRef"

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.bgCanvas)
            .statusBarsPadding()
    ) {
        // "header" (973:5035): pb=XS(8dp), px=LG(24dp), gap=0
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    start = WinoSpacing.LG,
                    end = WinoSpacing.LG,
                    top = WinoSpacing.XS,
                    bottom = WinoSpacing.XS
                ),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // "text": flex-1, gap=0
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = "Pay with USDC",
                    style = WinoTypography.h3Medium,
                    color = colors.textPrimary
                )
                Text(
                    text = "9:41 AM",
                    style = WinoTypography.small,
                    color = colors.textSecondary
                )
            }

            // X close button — only shown on failed state (793:9336)
            if (isFailed) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(WinoRadius.XL))
                        .background(colors.bgSurface)
                        .clickable { onDone() },
                    contentAlignment = Alignment.Center
                ) {
                    PhosphorIcons.X(
                        size = 20.dp,
                        color = colors.textPrimary
                    )
                }
            }
        }

        // "content" (973:4995): flex-1, gap=MD(16dp), px=24dp
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(horizontal = WinoSpacing.LG),
            verticalArrangement = Arrangement.spacedBy(WinoSpacing.MD)
        ) {
            // Amount card: bgSurface, rounded-XL, px=MD(16dp), py=XL(32dp), gap=XXS(4dp), centered
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
                val displayAmount = formatStatusAmount(amount)
                Text(
                    text = "$displayAmount $currency",
                    style = WinoTypography.display,
                    color = colors.textPrimary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
                Text(
                    text = amountSubtitle,
                    style = WinoTypography.body,
                    color = colors.textSecondary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // Status card: bgSurface, rounded-XL, flex-1, gap=LG(24dp), centered, px=MD, py=XL
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .clip(RoundedCornerShape(WinoRadius.XL))
                    .background(colors.bgSurface)
                    .padding(
                        horizontal = WinoSpacing.MD,
                        vertical = WinoSpacing.XL
                    ),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(WinoSpacing.LG, Alignment.CenterVertically)
            ) {
                // Icon: 64dp, rounded-XL
                Box(
                    modifier = Modifier
                        .size(WinoSpacing.XXXXL)
                        .clip(RoundedCornerShape(WinoRadius.XL))
                        .background(
                            when {
                                isSuccess -> colors.brandPrimary
                                isPending -> colors.financialPending
                                else -> colors.bgDestructiveSoft
                            }
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    when {
                        isSuccess -> Icon(
                            painter = painterResource(id = R.drawable.ic_check_fat),
                            contentDescription = "Success",
                            modifier = Modifier.size(WinoSpacing.XL),
                            tint = colors.textOnBrand
                        )
                        isPending -> {
                            val infiniteTransition = rememberInfiniteTransition(label = "spin")
                            val rotation by infiniteTransition.animateFloat(
                                initialValue = 0f,
                                targetValue = 360f,
                                animationSpec = infiniteRepeatable(
                                    animation = tween(1000, easing = LinearEasing),
                                    repeatMode = RepeatMode.Restart
                                ),
                                label = "rotation"
                            )
                            Icon(
                                painter = painterResource(id = R.drawable.ic_spinner),
                                contentDescription = "Loading",
                                modifier = Modifier
                                    .size(WinoSpacing.XL)
                                    .rotate(rotation),
                                tint = colors.textOnBrand
                            )
                        }
                        else -> Icon(
                            painter = painterResource(id = R.drawable.ic_receipt_x),
                            contentDescription = "Failed",
                            modifier = Modifier.size(WinoSpacing.XL),
                            tint = colors.stateError
                        )
                    }
                }

                // Text: gap=0, centered
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Title — H1 Medium, textPrimary
                    Text(
                        text = statusTitle,
                        style = WinoTypography.h1Medium,
                        color = colors.textPrimary,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                    // Subtitle — Body, textTertiary
                    Text(
                        text = statusSubtitle,
                        style = WinoTypography.body,
                        color = colors.textTertiary,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }

        // "bottom" section
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // "helper" text: px=XL(32dp), py=XS(8dp)
            Text(
                text = helperText,
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

            // "Bottom Button Layout": px=LG(24dp), py=SM(12dp)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        horizontal = WinoSpacing.LG,
                        vertical = WinoSpacing.SM
                    ),
                verticalArrangement = Arrangement.spacedBy(WinoSpacing.XS)
            ) {
                WinoButton(
                    text = buttonText,
                    onClick = if (isFailed || isExpired) onNewPayment else onDone,
                    modifier = Modifier.fillMaxWidth(),
                    variant = WinoButtonVariant.Primary,
                    size = WinoButtonSize.Large,
                    enabled = !isPending
                )

                // Cancel button for pending state (when callback provided)
                if (isPending && onCancel != null) {
                    WinoButton(
                        text = "Cancel payment",
                        onClick = onCancel,
                        modifier = Modifier.fillMaxWidth(),
                        variant = WinoButtonVariant.Secondary,
                        size = WinoButtonSize.Large
                    )
                }
            }
        }
    }
}

private fun formatStatusAmount(amount: String): String {
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
