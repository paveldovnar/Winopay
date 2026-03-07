package com.winopay.ui.screens.invoice

import android.app.Activity
import android.content.Intent
import android.net.Uri
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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.solana.mobilewalletadapter.clientlib.ActivityResultSender
import com.winopay.WinoPayApplication
import com.winopay.payments.PaymentRailFactory
import com.winopay.payments.RefundTransactionResult
import com.winopay.solana.SolanaExplorer
import com.winopay.solana.wallet.WalletResult
import com.winopay.data.local.InvoiceEntity
import com.winopay.data.local.InvoiceStatus
import com.winopay.data.local.PaymentCurrency
import com.winopay.ui.components.PhosphorIcons
import com.winopay.ui.components.WinoButton
import com.winopay.ui.components.WinoButtonSize
import com.winopay.ui.components.WinoButtonVariant
import com.winopay.ui.theme.WinoRadius
import com.winopay.ui.theme.WinoSpacing
import com.winopay.ui.theme.WinoTheme
import com.winopay.ui.theme.WinoTypography
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private const val TAG = "InvoiceDetailScreen"

@Composable
fun InvoiceDetailScreen(
    invoiceId: String,
    activityResultSender: ActivityResultSender,
    onBack: () -> Unit
) {
    val app = WinoPayApplication.instance
    val invoice by app.invoiceRepository.observeInvoice(invoiceId).collectAsState(initial = null)
    val currency by app.dataStoreManager.currency.collectAsState(initial = "USD")
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val colors = WinoTheme.colors

    // Refund state
    var isRefunding by remember { mutableStateOf(false) }
    var showRefundConfirmDialog by remember { mutableStateOf(false) }
    var refundError by remember { mutableStateOf<String?>(null) }
    var refundSuccess by remember { mutableStateOf<String?>(null) }

    // Refund handler
    fun performRefund(inv: InvoiceEntity) {
        scope.launch {
            isRefunding = true
            refundError = null

            try {
                // Get payment rail for this invoice
                val paymentRail = PaymentRailFactory.getRail(inv.networkId)

                // Build refund transaction (rail logs START)
                when (val buildResult = paymentRail.buildRefundTransaction(inv)) {
                    is RefundTransactionResult.Success -> {
                        // Sign and send via MWA
                        val sendResult = app.walletConnector.signAndSendTransaction(
                            sender = activityResultSender,
                            transactionBytes = buildResult.transactionBytes
                        )

                        when (sendResult) {
                            is WalletResult.Success -> {
                                val txId = sendResult.data
                                Log.i(TAG, "REFUND|SIGN_OK|invoiceId=${inv.id}")
                                Log.i(TAG, "REFUND|SEND_OK|invoiceId=${inv.id}|txId=$txId|via=MWA")

                                // Update invoice with refund transaction ID
                                app.invoiceRepository.markRefundSent(inv.id, txId)

                                refundSuccess = txId
                            }
                            is WalletResult.Failure -> {
                                Log.e(TAG, "REFUND|FAIL|stage=SIGN|invoiceId=${inv.id}|reason=${sendResult.error}")
                                refundError = sendResult.error
                            }
                        }
                    }
                    is RefundTransactionResult.NotEligible -> {
                        // Already logged by rail
                        refundError = buildResult.reason
                    }
                    is RefundTransactionResult.Error -> {
                        // Already logged by rail
                        refundError = buildResult.message
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "REFUND|FAIL|stage=EXCEPTION|invoiceId=${inv.id}|reason=${e.message}", e)
                refundError = e.message ?: "Unknown error"
            } finally {
                isRefunding = false
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.bgCanvas)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            // Header with back button
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        horizontal = WinoSpacing.LG,
                        vertical = WinoSpacing.MD
                    ),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(WinoSpacing.SM)
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(WinoRadius.XL))
                        .background(colors.bgSurface)
                        .clickable { onBack() },
                    contentAlignment = Alignment.Center
                ) {
                    PhosphorIcons.CaretLeft(
                        size = 20.dp,
                        color = colors.textPrimary
                    )
                }

                Text(
                    text = "Invoice Details",
                    style = WinoTypography.h2Medium,
                    color = colors.textPrimary,
                    modifier = Modifier.weight(1f)
                )
            }

            invoice?.let { inv ->
                // Status banner
                StatusBanner(invoice = inv)

                Spacer(modifier = Modifier.height(WinoSpacing.LG))

                // Amount section - FIAT-first display
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = WinoSpacing.LG),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Primary: FIAT amount (if available)
                    val fiatAmount = inv.formatFiatAmount()
                    val fiatCurrency = inv.fiatCurrency

                    if (fiatAmount != null && fiatCurrency != null) {
                        // FIAT-first display
                        Text(
                            text = "$fiatAmount $fiatCurrency",
                            style = WinoTypography.display,
                            color = colors.textPrimary
                        )
                        Spacer(modifier = Modifier.height(WinoSpacing.XXS))
                        // Secondary: Stablecoin amount
                        Text(
                            text = "${inv.formatStablecoinAmount()} ${inv.getStablecoinLabel()}",
                            style = WinoTypography.body,
                            color = colors.textSecondary
                        )
                        // Tertiary: Rate display
                        val rateDisplay = inv.formatRateDisplay()
                        if (rateDisplay != null) {
                            Spacer(modifier = Modifier.height(WinoSpacing.XXS))
                            Text(
                                text = "Rate: $rateDisplay",
                                style = WinoTypography.small,
                                color = colors.textMuted
                            )
                        }
                    } else {
                        // Fallback: Stablecoin amount if no FIAT data
                        Text(
                            text = formatAmount(inv),
                            style = WinoTypography.display,
                            color = colors.textPrimary
                        )
                        Text(
                            text = inv.getStablecoinLabel(),
                            style = WinoTypography.body,
                            color = colors.textSecondary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(WinoSpacing.XL))

                // Details card
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = WinoSpacing.LG)
                        .clip(RoundedCornerShape(WinoRadius.LG))
                        .background(colors.bgSurface)
                        .padding(WinoSpacing.MD),
                    verticalArrangement = Arrangement.spacedBy(WinoSpacing.XS)
                ) {
                    DetailRow(label = "Invoice ID", value = inv.id)
                    HorizontalDivider(color = colors.borderSubtle, thickness = 0.5.dp)

                    DetailRow(label = "Reference", value = inv.reference.take(12) + "...")
                    HorizontalDivider(color = colors.borderSubtle, thickness = 0.5.dp)

                    DetailRow(label = "Status", value = inv.status.name)
                    HorizontalDivider(color = colors.borderSubtle, thickness = 0.5.dp)

                    DetailRow(label = "Created", value = formatDateTime(inv.createdAt))

                    if (inv.confirmedAt != null) {
                        HorizontalDivider(color = colors.borderSubtle, thickness = 0.5.dp)
                        DetailRow(label = "Confirmed", value = formatDateTime(inv.confirmedAt))
                    }

                    if (inv.foundSignature != null) {
                        HorizontalDivider(color = colors.borderSubtle, thickness = 0.5.dp)
                        DetailRow(
                            label = "Signature",
                            value = inv.foundSignature.take(16) + "...",
                            isMonospace = true
                        )
                    }

                    if (inv.failureReason != null) {
                        HorizontalDivider(color = colors.borderSubtle, thickness = 0.5.dp)
                        DetailRow(
                            label = "Failure Reason",
                            value = inv.failureReason,
                            valueColor = colors.stateError
                        )
                    }

                    // FX Rate details (if available)
                    if (inv.rateUsed != null && inv.fiatCurrency != null) {
                        HorizontalDivider(color = colors.borderSubtle, thickness = 0.5.dp)
                        DetailRow(
                            label = "Exchange Rate",
                            value = inv.formatRateDisplay() ?: ""
                        )

                        if (inv.rateProvider != null) {
                            HorizontalDivider(color = colors.borderSubtle, thickness = 0.5.dp)
                            DetailRow(
                                label = "Rate Provider",
                                value = inv.rateProvider
                            )
                        }

                        if (inv.rateDate != null) {
                            HorizontalDivider(color = colors.borderSubtle, thickness = 0.5.dp)
                            DetailRow(
                                label = "Rate Date",
                                value = inv.rateDate
                            )
                        }
                    }

                    // Payer info (for refund support)
                    if (inv.payerAddress != null) {
                        HorizontalDivider(color = colors.borderSubtle, thickness = 0.5.dp)
                        DetailRow(
                            label = "Payer Wallet",
                            value = "${inv.payerAddress.take(8)}...${inv.payerAddress.takeLast(8)}",
                            isMonospace = true
                        )
                    }

                    // Refund transaction (if issued)
                    if (inv.refundTxId != null) {
                        HorizontalDivider(color = colors.borderSubtle, thickness = 0.5.dp)
                        DetailRow(
                            label = "Refund TX",
                            value = "${inv.refundTxId.take(12)}...",
                            isMonospace = true,
                            valueColor = colors.stateSuccess
                        )
                    }
                }

                Spacer(modifier = Modifier.height(WinoSpacing.LG))

                // Action buttons for CONFIRMED invoices
                if (inv.status == InvoiceStatus.CONFIRMED && inv.foundSignature != null) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = WinoSpacing.LG),
                        verticalArrangement = Arrangement.spacedBy(WinoSpacing.SM)
                    ) {
                        // Open in Explorer button
                        WinoButton(
                            text = "Open in Explorer",
                            onClick = {
                                val explorerUrl = SolanaExplorer.buildTxUrl(inv.foundSignature)
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(explorerUrl)).apply {
                                    if (context !is Activity) addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                }
                                context.startActivity(intent)
                            },
                            modifier = Modifier.fillMaxWidth(),
                            variant = WinoButtonVariant.Secondary,
                            size = WinoButtonSize.Large
                        )

                        // Refund button - only if rail supports it
                        val paymentRail = PaymentRailFactory.getRail(inv.networkId)
                        val canRefund = paymentRail.canRefund(inv)
                        if (canRefund) {
                            WinoButton(
                                text = if (isRefunding) "Processing..." else "Issue Refund",
                                onClick = { showRefundConfirmDialog = true },
                                modifier = Modifier.fillMaxWidth(),
                                variant = WinoButtonVariant.Ghost,
                                size = WinoButtonSize.Large,
                                enabled = !isRefunding
                            )
                        }

                        // Show refund transaction link if already refunded
                        if (inv.refundTxId != null) {
                            WinoButton(
                                text = "View Refund Transaction",
                                onClick = {
                                    val explorerUrl = SolanaExplorer.buildTxUrl(inv.refundTxId)
                                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(explorerUrl)).apply {
                                        if (context !is Activity) addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                    }
                                    context.startActivity(intent)
                                },
                                modifier = Modifier.fillMaxWidth(),
                                variant = WinoButtonVariant.Ghost,
                                size = WinoButtonSize.Large
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(WinoSpacing.XL))

                // Timeline section
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = WinoSpacing.LG)
                ) {
                    Text(
                        text = "Status Timeline",
                        style = WinoTypography.h3Medium,
                        color = colors.textPrimary,
                        modifier = Modifier.padding(bottom = WinoSpacing.SM)
                    )

                    StatusTimeline(invoice = inv)
                }

                Spacer(modifier = Modifier.height(WinoSpacing.XL))
            } ?: run {
                // Loading state
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(WinoSpacing.XL),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Loading...",
                        style = WinoTypography.body,
                        color = colors.textMuted
                    )
                }
            }
        }

        // Loading overlay during refund
        if (isRefunding) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(colors.bgCanvas.copy(alpha = 0.7f)),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(WinoSpacing.MD)
                ) {
                    CircularProgressIndicator(color = colors.brandPrimary)
                    Text(
                        text = "Processing refund...",
                        style = WinoTypography.body,
                        color = colors.textPrimary
                    )
                }
            }
        }
    }

    // Refund confirmation dialog
    if (showRefundConfirmDialog && invoice != null) {
        AlertDialog(
            onDismissRequest = { showRefundConfirmDialog = false },
            title = {
                Text(
                    text = "Confirm Refund",
                    style = WinoTypography.h3Medium,
                    color = colors.textPrimary
                )
            },
            text = {
                Column {
                    Text(
                        text = "Are you sure you want to refund this payment?",
                        style = WinoTypography.body,
                        color = colors.textSecondary
                    )
                    Spacer(modifier = Modifier.height(WinoSpacing.SM))
                    Text(
                        text = "Amount: ${invoice!!.formatStablecoinAmount()} ${invoice!!.getStablecoinLabel()}",
                        style = WinoTypography.bodyMedium,
                        color = colors.textPrimary
                    )
                    Text(
                        text = "To: ${invoice!!.payerAddress?.take(8)}...${invoice!!.payerAddress?.takeLast(8)}",
                        style = WinoTypography.small,
                        color = colors.textMuted
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showRefundConfirmDialog = false
                        performRefund(invoice!!)
                    }
                ) {
                    Text("Refund", color = colors.stateError)
                }
            },
            dismissButton = {
                TextButton(onClick = { showRefundConfirmDialog = false }) {
                    Text("Cancel", color = colors.textSecondary)
                }
            },
            containerColor = colors.bgSurface
        )
    }

    // Refund error dialog
    if (refundError != null) {
        AlertDialog(
            onDismissRequest = { refundError = null },
            title = {
                Text(
                    text = "Refund Failed",
                    style = WinoTypography.h3Medium,
                    color = colors.stateError
                )
            },
            text = {
                Text(
                    text = refundError ?: "Unknown error",
                    style = WinoTypography.body,
                    color = colors.textSecondary
                )
            },
            confirmButton = {
                TextButton(onClick = { refundError = null }) {
                    Text("OK", color = colors.brandPrimary)
                }
            },
            containerColor = colors.bgSurface
        )
    }

    // Refund success dialog
    if (refundSuccess != null) {
        AlertDialog(
            onDismissRequest = { refundSuccess = null },
            title = {
                Text(
                    text = "Refund Sent",
                    style = WinoTypography.h3Medium,
                    color = colors.stateSuccess
                )
            },
            text = {
                Column {
                    Text(
                        text = "The refund has been sent successfully.",
                        style = WinoTypography.body,
                        color = colors.textSecondary
                    )
                    Spacer(modifier = Modifier.height(WinoSpacing.SM))
                    Text(
                        text = "Transaction: ${refundSuccess?.take(16)}...",
                        style = WinoTypography.small,
                        color = colors.textMuted
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val txId = refundSuccess
                        refundSuccess = null
                        if (txId != null) {
                            val explorerUrl = SolanaExplorer.buildTxUrl(txId)
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(explorerUrl)).apply {
                                if (context !is Activity) addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            }
                            context.startActivity(intent)
                        }
                    }
                ) {
                    Text("View in Explorer", color = colors.brandPrimary)
                }
            },
            dismissButton = {
                TextButton(onClick = { refundSuccess = null }) {
                    Text("Close", color = colors.textSecondary)
                }
            },
            containerColor = colors.bgSurface
        )
    }
}

@Composable
private fun StatusBanner(invoice: InvoiceEntity) {
    val colors = WinoTheme.colors

    val bgColor = when (invoice.status) {
        InvoiceStatus.CREATED -> colors.stateInfoSoft
        InvoiceStatus.PENDING -> colors.stateWarningSoft
        InvoiceStatus.CONFIRMED -> colors.stateSuccessSoft
        InvoiceStatus.FAILED -> colors.stateErrorSoft
        InvoiceStatus.EXPIRED -> colors.bgSurfaceAlt
        InvoiceStatus.CANCELED -> colors.bgSurfaceAlt
    }

    val iconColor = when (invoice.status) {
        InvoiceStatus.CREATED -> colors.stateInfo
        InvoiceStatus.PENDING -> colors.stateWarning
        InvoiceStatus.CONFIRMED -> colors.stateSuccess
        InvoiceStatus.FAILED -> colors.stateError
        InvoiceStatus.EXPIRED -> colors.textMuted
        InvoiceStatus.CANCELED -> colors.textMuted
    }

    val label = when (invoice.status) {
        InvoiceStatus.CREATED -> "Awaiting Payment"
        InvoiceStatus.PENDING -> "Pending Confirmation"
        InvoiceStatus.CONFIRMED -> "Confirmed"
        InvoiceStatus.FAILED -> "Failed"
        InvoiceStatus.EXPIRED -> "Expired"
        InvoiceStatus.CANCELED -> "Canceled"
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = WinoSpacing.LG)
            .clip(RoundedCornerShape(WinoRadius.LG))
            .background(bgColor)
            .padding(WinoSpacing.MD),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(WinoSpacing.SM)
    ) {
        when (invoice.status) {
            InvoiceStatus.CREATED -> PhosphorIcons.QrCode(size = 24.dp, color = colors.stateInfo)
            InvoiceStatus.PENDING -> PhosphorIcons.Clock(size = 24.dp, color = colors.stateWarning)
            InvoiceStatus.CONFIRMED -> PhosphorIcons.CheckCircle(size = 24.dp, color = colors.stateSuccess)
            InvoiceStatus.FAILED -> PhosphorIcons.XCircle(size = 24.dp, color = colors.stateError)
            InvoiceStatus.EXPIRED -> PhosphorIcons.Clock(size = 24.dp, color = colors.textMuted)
            InvoiceStatus.CANCELED -> PhosphorIcons.X(size = 24.dp, color = colors.textMuted)
        }
        Text(
            text = label,
            style = WinoTypography.bodyMedium,
            color = iconColor
        )
    }
}

@Composable
private fun DetailRow(
    label: String,
    value: String,
    isMonospace: Boolean = false,
    valueColor: androidx.compose.ui.graphics.Color? = null
) {
    val colors = WinoTheme.colors

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = WinoSpacing.XS),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = WinoTypography.body,
            color = colors.textSecondary
        )
        Text(
            text = value,
            style = if (isMonospace) WinoTypography.small else WinoTypography.bodyMedium,
            color = valueColor ?: colors.textPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun StatusTimeline(invoice: InvoiceEntity) {
    val colors = WinoTheme.colors

    val timelineItems = buildList {
        add(TimelineItem("Created", invoice.createdAt, true))

        when (invoice.status) {
            InvoiceStatus.PENDING -> {
                add(TimelineItem("Payment Detected", invoice.updatedAt, true))
                add(TimelineItem("Awaiting Confirmation", null, false))
            }
            InvoiceStatus.CONFIRMED -> {
                add(TimelineItem("Payment Detected", invoice.updatedAt, true))
                add(TimelineItem("Confirmed", invoice.confirmedAt, true))
            }
            InvoiceStatus.FAILED -> {
                add(TimelineItem("Failed", invoice.updatedAt, true, isError = true))
            }
            InvoiceStatus.EXPIRED -> {
                add(TimelineItem("Expired", invoice.updatedAt, true, isError = true))
            }
            else -> { /* CREATED - nothing extra */ }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(WinoRadius.LG))
            .background(colors.bgSurface)
            .padding(WinoSpacing.MD)
    ) {
        timelineItems.forEachIndexed { index, item ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = WinoSpacing.XS),
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.spacedBy(WinoSpacing.SM)
            ) {
                // Timeline dot and line
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .clip(CircleShape)
                            .background(
                                when {
                                    item.isError -> colors.stateError
                                    item.isCompleted -> colors.stateSuccess
                                    else -> colors.borderDefault
                                }
                            )
                    )
                    if (index < timelineItems.lastIndex) {
                        Box(
                            modifier = Modifier
                                .height(24.dp)
                                .background(colors.borderSubtle)
                                .padding(horizontal = 0.5.dp)
                        )
                    }
                }

                Column {
                    Text(
                        text = item.label,
                        style = WinoTypography.bodyMedium,
                        color = when {
                            item.isError -> colors.stateError
                            item.isCompleted -> colors.textPrimary
                            else -> colors.textMuted
                        }
                    )
                    if (item.timestamp != null) {
                        Text(
                            text = formatDateTime(item.timestamp),
                            style = WinoTypography.small,
                            color = colors.textMuted
                        )
                    }
                }
            }
        }
    }
}

private data class TimelineItem(
    val label: String,
    val timestamp: Long?,
    val isCompleted: Boolean,
    val isError: Boolean = false
)

private fun formatAmount(invoice: InvoiceEntity): String {
    val display = invoice.getDisplayAmount()
    return when (invoice.currency) {
        PaymentCurrency.SOL -> String.format("%.4f", display)
        PaymentCurrency.USDC -> String.format("%.2f", display)
        PaymentCurrency.USDT -> String.format("%.2f", display)
    }
}

private fun formatDateTime(timestamp: Long): String {
    val sdf = SimpleDateFormat("MMM d, yyyy 'at' HH:mm", Locale.getDefault())
    return sdf.format(Date(timestamp))
}
