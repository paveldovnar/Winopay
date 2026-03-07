package com.winopay.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.winopay.data.local.InvoiceEntity
import com.winopay.data.local.InvoiceStatus
import com.winopay.data.local.PaymentCurrency
import com.winopay.data.model.Transaction
import com.winopay.ui.theme.WinoRadius
import com.winopay.ui.theme.WinoSpacing
import com.winopay.ui.theme.WinoTheme
import com.winopay.ui.theme.WinoTypography
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// ==================== Invoice-based OperationList ====================

/**
 * Invoice list item with FIAT-first display:
 * - Primary: "+{FIAT_AMOUNT} {DISPLAY_CURRENCY} (hh:mm a, dd.MM.yyyy)"
 * - Secondary: "+{STABLECOIN_AMOUNT} {USDC/USDT}"
 * - Tertiary: "Rate: 1 USD = {rateUsed} {DISPLAY_CURRENCY}"
 */
@Composable
fun InvoiceListItem(
    invoice: InvoiceEntity,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = WinoTheme.colors

    val iconBgColor = when (invoice.status) {
        InvoiceStatus.CONFIRMED -> colors.stateSuccessSoft
        InvoiceStatus.PENDING -> colors.stateWarningSoft
        InvoiceStatus.CREATED -> colors.stateInfoSoft
        InvoiceStatus.FAILED -> colors.stateErrorSoft
        InvoiceStatus.EXPIRED -> colors.bgSurfaceAlt
        InvoiceStatus.CANCELED -> colors.bgSurfaceAlt
    }

    val amountColor = when (invoice.status) {
        InvoiceStatus.CONFIRMED -> colors.financialPositive
        InvoiceStatus.PENDING -> colors.financialPending
        InvoiceStatus.CREATED -> colors.textSecondary
        InvoiceStatus.FAILED -> colors.financialNegative
        InvoiceStatus.EXPIRED -> colors.textMuted
        InvoiceStatus.CANCELED -> colors.textMuted
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = WinoSpacing.SM, horizontal = WinoSpacing.MD),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Icon
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(iconBgColor),
            contentAlignment = Alignment.Center
        ) {
            when (invoice.status) {
                InvoiceStatus.CONFIRMED -> PhosphorIcons.CheckCircle(size = 20.dp, color = colors.stateSuccess)
                InvoiceStatus.PENDING -> PhosphorIcons.Clock(size = 20.dp, color = colors.stateWarning)
                InvoiceStatus.CREATED -> PhosphorIcons.QrCode(size = 20.dp, color = colors.stateInfo)
                InvoiceStatus.FAILED -> PhosphorIcons.XCircle(size = 20.dp, color = colors.stateError)
                InvoiceStatus.EXPIRED -> PhosphorIcons.Clock(size = 20.dp, color = colors.textMuted)
                InvoiceStatus.CANCELED -> PhosphorIcons.X(size = 20.dp, color = colors.textMuted)
            }
        }

        Spacer(modifier = Modifier.width(WinoSpacing.SM))

        // Left side: Primary + Secondary amounts
        Column(modifier = Modifier.weight(1f)) {
            // Primary: Local currency amount with timestamp (from FX at payment time)
            val prefix = if (invoice.status == InvoiceStatus.CONFIRMED) "+" else ""
            val fiatAmount = invoice.formatFiatAmount()
            val fiatCurrency = invoice.fiatCurrency
            val timestamp = invoice.confirmedAt ?: invoice.createdAt
            val timeFormatted = formatConfirmedTime(timestamp)

            if (fiatAmount != null && fiatCurrency != null) {
                // Local currency display (using FX rate from payment time)
                Text(
                    text = "$prefix$fiatAmount $fiatCurrency ($timeFormatted)",
                    style = WinoTypography.bodyMedium,
                    color = amountColor
                )
            } else {
                // Fallback: USD amount if no FX data (USDC/USDT = USD 1:1)
                Text(
                    text = "$prefix$${invoice.formatStablecoinAmount()} USD ($timeFormatted)",
                    style = WinoTypography.bodyMedium,
                    color = amountColor
                )
            }

            Spacer(modifier = Modifier.height(2.dp))

            // Secondary: USD equivalent (USDC/USDT treated as USD, NO token labels in list)
            if (fiatAmount != null && fiatCurrency != null) {
                Text(
                    text = "= $${invoice.formatStablecoinAmount()} USD",
                    style = WinoTypography.small,
                    color = colors.textSecondary
                )
            }
        }

        // Right side: Status icon indicator
        Box(
            modifier = Modifier.size(24.dp),
            contentAlignment = Alignment.Center
        ) {
            PhosphorIcons.CaretRight(size = 16.dp, color = colors.textMuted)
        }
    }
}

@Composable
fun InvoiceList(
    invoices: List<InvoiceEntity>,
    onInvoiceClick: (InvoiceEntity) -> Unit,
    modifier: Modifier = Modifier,
    emptyContent: @Composable () -> Unit = { EmptyTransactionsList() }
) {
    val colors = WinoTheme.colors

    if (invoices.isEmpty()) {
        emptyContent()
    } else {
        Column(
            modifier = modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(WinoRadius.LG))
                .background(colors.bgSurface)
        ) {
            invoices.forEachIndexed { index, invoice ->
                InvoiceListItem(
                    invoice = invoice,
                    onClick = { onInvoiceClick(invoice) }
                )
                if (index < invoices.lastIndex) {
                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = WinoSpacing.MD),
                        color = colors.borderSubtle,
                        thickness = 0.5.dp
                    )
                }
            }
        }
    }
}

private fun formatInvoiceAmount(invoice: InvoiceEntity): String {
    val amount = invoice.getDisplayAmount()
    return when (invoice.currency) {
        PaymentCurrency.SOL -> String.format("%.4f", amount)
        PaymentCurrency.USDC -> String.format("%.2f", amount)
        PaymentCurrency.USDT -> String.format("%.2f", amount)
    }
}

// ==================== Legacy Transaction-based OperationList ====================

@Composable
fun OperationListItem(
    transaction: Transaction,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = WinoTheme.colors

    val iconBgColor = when (transaction.status) {
        TransactionStatus.Success -> colors.stateSuccessSoft
        TransactionStatus.Pending -> colors.stateWarningSoft
        TransactionStatus.Failed -> colors.stateErrorSoft
    }

    val amountColor = when (transaction.status) {
        TransactionStatus.Success -> colors.financialPositive
        TransactionStatus.Pending -> colors.financialPending
        TransactionStatus.Failed -> colors.financialNegative
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = WinoSpacing.SM, horizontal = WinoSpacing.MD),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Icon
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(iconBgColor),
            contentAlignment = Alignment.Center
        ) {
            when (transaction.status) {
                TransactionStatus.Success -> PhosphorIcons.CheckCircle(size = 20.dp, color = colors.stateSuccess)
                TransactionStatus.Pending -> PhosphorIcons.Clock(size = 20.dp, color = colors.stateWarning)
                TransactionStatus.Failed -> PhosphorIcons.XCircle(size = 20.dp, color = colors.stateError)
            }
        }

        Spacer(modifier = Modifier.width(WinoSpacing.SM))

        // Details
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "Payment received",
                style = WinoTypography.bodyMedium,
                color = colors.textPrimary
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = formatTransactionTime(transaction.timestamp),
                style = WinoTypography.small,
                color = colors.textMuted
            )
        }

        // Amount
        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = "+$${formatAmount(transaction.amount)}",
                style = WinoTypography.bodyMedium,
                color = amountColor
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "USDC",
                style = WinoTypography.micro,
                color = colors.textMuted
            )
        }
    }
}

@Composable
fun OperationList(
    transactions: List<Transaction>,
    onTransactionClick: (Transaction) -> Unit,
    modifier: Modifier = Modifier,
    emptyContent: @Composable () -> Unit = { EmptyTransactionsList() }
) {
    val colors = WinoTheme.colors

    if (transactions.isEmpty()) {
        emptyContent()
    } else {
        Column(
            modifier = modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(WinoRadius.LG))
                .background(colors.bgSurface)
        ) {
            transactions.forEachIndexed { index, transaction ->
                OperationListItem(
                    transaction = transaction,
                    onClick = { onTransactionClick(transaction) }
                )
                if (index < transactions.lastIndex) {
                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = WinoSpacing.MD),
                        color = colors.borderSubtle,
                        thickness = 0.5.dp
                    )
                }
            }
        }
    }
}

@Composable
fun EmptyTransactionsList(
    modifier: Modifier = Modifier
) {
    val colors = WinoTheme.colors

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(WinoSpacing.XL),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(CircleShape)
                .background(colors.bgSurfaceAlt),
            contentAlignment = Alignment.Center
        ) {
            PhosphorIcons.Storefront(
                size = 32.dp,
                color = colors.textMuted
            )
        }
        Spacer(modifier = Modifier.height(WinoSpacing.MD))
        Text(
            text = "No transactions yet",
            style = WinoTypography.bodyMedium,
            color = colors.textSecondary
        )
        Spacer(modifier = Modifier.height(WinoSpacing.XXS))
        Text(
            text = "Your received payments will appear here",
            style = WinoTypography.small,
            color = colors.textMuted
        )
    }
}

private fun formatTransactionTime(timestamp: Long): String {
    val now = System.currentTimeMillis()
    val diff = now - timestamp

    return when {
        diff < 60_000 -> "Just now"
        diff < 3600_000 -> "${diff / 60_000}m ago"
        diff < 86400_000 -> "${diff / 3600_000}h ago"
        else -> {
            val sdf = SimpleDateFormat("MMM d, HH:mm", Locale.getDefault())
            sdf.format(Date(timestamp))
        }
    }
}

/**
 * Format timestamp for confirmed transactions.
 * Format: "hh:mm a, dd.MM.yyyy" (e.g. "02:30 PM, 15.03.2024")
 */
private fun formatConfirmedTime(timestamp: Long): String {
    val sdf = SimpleDateFormat("hh:mm a, dd.MM.yyyy", Locale.getDefault())
    return sdf.format(Date(timestamp))
}

private fun formatAmount(amount: Double): String {
    return if (amount == amount.toLong().toDouble()) {
        String.format("%,.0f", amount)
    } else {
        String.format("%,.2f", amount)
    }
}
