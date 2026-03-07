package com.winopay.ui.screens.pos

import android.util.Log
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import com.winopay.WinoPayApplication
import com.winopay.data.pos.PaymentMethod
import com.winopay.data.pos.PosState
import com.winopay.data.profile.MerchantProfileStore
import com.winopay.ui.screens.expire.SoftExpireScreen
import com.winopay.ui.screens.qr.QRScreenStateful
import com.winopay.ui.screens.status.TxStatusScreenStateful
import com.winopay.ui.theme.WinoSpacing
import com.winopay.ui.theme.WinoTheme
import com.winopay.ui.theme.WinoTypography
import kotlinx.coroutines.launch

private const val TAG = "PosFlowHost"

/**
 * Host composable for the entire POS flow.
 * Observes PosManager state and renders the appropriate screen.
 */
@Composable
fun PosFlowHost(
    onExit: () -> Unit
) {
    val app = WinoPayApplication.instance
    val posManager = app.posManager
    val context = LocalContext.current

    val state by posManager.state.collectAsState()
    val businessIdentity by app.dataStoreManager.businessIdentity.collectAsState(initial = null)
    val currency by app.dataStoreManager.currency.collectAsState(initial = "USD")
    val scope = rememberCoroutineScope()

    // Active rail (multichain support)
    val profileStore = remember { MerchantProfileStore(context) }
    val activeRail by profileStore.observeActiveRail().collectAsState(initial = null)

    // Legacy Solana wallet (for backwards compatibility)
    val solanaWalletPublicKey by app.dataStoreManager.walletPublicKey.collectAsState(initial = null)

    // Resolved recipient address from active rail
    val recipientAddress = activeRail?.accountId ?: solanaWalletPublicKey

    // ━━━━━━━━━━ CANCEL CONFIRMATION DIALOG STATE ━━━━━━━━━━
    // Shown when merchant tries to exit during active payment (QR/Pending)
    var showCancelDialog by remember { mutableStateOf(false) }

    // Check if payment is active (requires explicit cancel to exit)
    // ExpiredAwaitingDecision is also "active" because detection is still running
    val isPaymentActive = state is PosState.Qr ||
                         state is PosState.Pending ||
                         state is PosState.ExpiredAwaitingDecision

    // ━━━━━━━━━━ KEEP SCREEN ON DURING PAYMENT ━━━━━━━━━━
    // Screen stays on during QR, Pending, and Soft-expire states
    // This ensures merchant doesn't miss payment confirmation
    val view = LocalView.current
    DisposableEffect(isPaymentActive) {
        if (isPaymentActive) {
            view.keepScreenOn = true
            Log.i(TAG, "SCREEN|KEEP_ON|state=${state::class.simpleName}")
        } else {
            view.keepScreenOn = false
            Log.i(TAG, "SCREEN|NORMAL|state=${state::class.simpleName}")
        }
        onDispose {
            // Always restore normal behavior when leaving
            view.keepScreenOn = false
            Log.i(TAG, "SCREEN|DISPOSE|restored_normal")
        }
    }

    // ━━━━━━━━━━ BACK NAVIGATION LOCK ━━━━━━━━━━
    // Intercept system back button during active payment
    BackHandler(enabled = isPaymentActive) {
        Log.d(TAG, "Back pressed during active payment - showing cancel dialog")
        showCancelDialog = true
    }

    // Log state changes
    LaunchedEffect(state) {
        Log.d(TAG, "POS State changed: $state")
    }

    // Log wallet and business identity
    LaunchedEffect(businessIdentity, activeRail, solanaWalletPublicKey) {
        Log.d(TAG, "BusinessIdentity: ${businessIdentity?.let { "name=${it.name}" } ?: "NULL"}")
        Log.d(TAG, "ActiveRail: ${activeRail?.let { "railId=${it.railId}, networkId=${it.networkId}, addr=${it.accountId.take(12)}..." } ?: "NONE"}")
        Log.d(TAG, "SolanaWallet (legacy): ${solanaWalletPublicKey?.take(12) ?: "NOT CONNECTED"}")
        Log.d(TAG, "ResolvedRecipient: ${recipientAddress?.take(12) ?: "NULL"}")
    }

    // Check for active invoice when entering flow
    // If active invoice exists, restore that state instead of resetting
    LaunchedEffect(Unit) {
        Log.d(TAG, "PosFlowHost mounted, checking for active invoice")
        val restored = posManager.restoreActiveInvoice()
        if (!restored) {
            Log.d(TAG, "No active invoice, resetting to EnterAmount")
            posManager.reset()
        } else {
            Log.d(TAG, "Active invoice restored, continuing payment flow")
        }
    }

    // ━━━━━━━━━━ CANCEL PAYMENT DIALOG ━━━━━━━━━━
    // Blocking dialog - must explicitly confirm or dismiss
    if (showCancelDialog) {
        CancelPaymentDialog(
            onConfirmCancel = {
                Log.d(TAG, "Cancel confirmed by merchant")
                showCancelDialog = false
                posManager.cancelInvoice()
                onExit()
            },
            onDismiss = {
                Log.d(TAG, "Cancel dismissed - continuing payment")
                showCancelDialog = false
            }
        )
    }

    when (val currentState = state) {
        is PosState.EnterAmount -> {
            POSScreenStateful(
                onCreateInvoice = { amount ->
                    posManager.createInvoice(amount)
                },
                onBack = onExit
            )
        }

        is PosState.SelectPayment -> {
            SelectPaymentScreenStateful(
                amount = currentState.amount,
                invoiceId = currentState.invoiceId,
                onSelectMethod = { method ->
                    Log.d(TAG, "PAYMENT_METHOD|SELECT|method=$method|rail=${activeRail?.railId ?: "solana"}")

                    // MULTICHAIN: Use active rail address (TRON, Solana, etc.)
                    if (recipientAddress.isNullOrBlank()) {
                        Log.e(TAG, "PAYMENT_METHOD|ERROR|NO_WALLET|activeRail=${activeRail?.railId}")
                        Log.e(TAG, "User must connect wallet in Settings → Connected Wallets")
                        // TODO: Show error UI
                        return@SelectPaymentScreenStateful
                    }

                    // Rail-agnostic address validation
                    // Solana: 32-44 chars base58
                    // TRON: 34 chars starting with T
                    val railId = activeRail?.railId ?: "solana"
                    val isValidAddress = when (railId) {
                        "tron" -> recipientAddress.length == 34 && recipientAddress.startsWith("T")
                        else -> recipientAddress.length in 32..44
                    }

                    if (!isValidAddress) {
                        Log.e(TAG, "PAYMENT_METHOD|ERROR|INVALID_ADDRESS|rail=$railId|addr=${recipientAddress.take(8)}...|len=${recipientAddress.length}")
                        return@SelectPaymentScreenStateful
                    }

                    val businessName = businessIdentity?.name ?: "Merchant"
                    Log.d(TAG, "PAYMENT_METHOD|PROCEED|recipient=${recipientAddress.take(12)}...|rail=$railId|business=$businessName")

                    scope.launch {
                        Log.d(TAG, "Launching selectPaymentMethod coroutine")
                        try {
                            posManager.selectPaymentMethod(
                                method = method,
                                recipientPublicKey = recipientAddress,
                                businessName = businessName
                            )
                            Log.d(TAG, "selectPaymentMethod completed")
                        } catch (e: Exception) {
                            Log.e(TAG, "selectPaymentMethod failed", e)
                        }
                    }
                },
                onCancel = {
                    Log.d(TAG, "onCancel called")
                    posManager.cancelInvoice()
                }
            )
        }

        is PosState.Qr -> {
            // Observe Room invoice status for auto-transition
            val invoice by app.invoiceRepository.observeInvoice(currentState.invoiceId)
                .collectAsState(initial = null)

            // Sync UI state when Room status changes
            LaunchedEffect(invoice?.status) {
                invoice?.let { inv ->
                    Log.d(TAG, "Room invoice status: ${inv.status} for ${inv.id}")
                    posManager.syncFromRoomStatus(inv)
                }
            }

            QRScreenStateful(
                invoiceId = currentState.invoiceId,
                amount = currentState.amount,
                usdcAmount = currentState.usdcAmount,
                method = currentState.method,
                qrData = currentState.qrData,
                expiresAt = currentState.expiresAt,
                onOtherMethods = {
                    posManager.changePaymentMethod()
                },
                onCancel = {
                    // Show confirmation dialog instead of direct cancel
                    showCancelDialog = true
                }
            )
        }

        is PosState.Pending -> {
            // Observe Room invoice status for auto-transition to confirmed
            val invoice by app.invoiceRepository.observeInvoice(currentState.invoiceId)
                .collectAsState(initial = null)

            // Sync UI state when Room status changes
            LaunchedEffect(invoice?.status) {
                invoice?.let { inv ->
                    Log.d(TAG, "Room invoice status (pending): ${inv.status} for ${inv.id}")
                    posManager.syncFromRoomStatus(inv)
                }
            }

            TxStatusScreenStateful(
                statusType = "pending",
                amount = currentState.amount,
                signature = currentState.signature,
                onDone = onExit,
                onNewPayment = {
                    posManager.reset()
                },
                onCancel = {
                    // Show confirmation dialog before canceling pending payment
                    showCancelDialog = true
                }
            )
        }

        is PosState.Success -> {
            TxStatusScreenStateful(
                statusType = "success",
                amount = currentState.amount,
                signature = currentState.signature,
                onDone = onExit,
                onNewPayment = {
                    posManager.reset()
                }
            )
        }

        is PosState.Failed -> {
            TxStatusScreenStateful(
                statusType = "failed",
                amount = currentState.amount,
                signature = null,
                onDone = onExit,
                onNewPayment = {
                    posManager.reset()
                }
            )
        }

        is PosState.ExpiredAwaitingDecision -> {
            // SOFT-EXPIRE: Timeout reached, awaiting merchant decision
            // Detection is STILL RUNNING in background
            // BLOCKING: Cannot go back to Dashboard (BackHandler prevents it)
            Log.d(TAG, "ExpiredAwaitingDecision: invoice=${currentState.invoiceId}, elapsed=${currentState.elapsedSeconds}s")

            SoftExpireScreen(
                invoiceId = currentState.invoiceId,
                amount = currentState.amount,
                elapsedSeconds = currentState.elapsedSeconds,
                onWaitLonger = {
                    Log.d(TAG, "Merchant chose: Wait 5 more minutes")
                    posManager.extendTimeout()
                },
                onRevoke = {
                    Log.d(TAG, "Merchant chose: Revoke invoice")
                    posManager.revokeInvoice()
                }
            )
        }

        is PosState.Expired -> {
            TxStatusScreenStateful(
                statusType = "expired",
                amount = currentState.amount,
                signature = null,
                onDone = onExit,
                onNewPayment = {
                    posManager.reset()
                }
            )
        }

        is PosState.RateError -> {
            // Show rate error screen - exchange rates unavailable
            // Display as failed status with rate error message in log
            android.util.Log.e(TAG, "RateError state: ${currentState.message}")
            TxStatusScreenStateful(
                statusType = "failed",
                amount = 0.0,
                signature = null,
                onDone = onExit,
                onNewPayment = {
                    posManager.reset()
                }
            )
        }

        is PosState.BlockedByActiveInvoice -> {
            // STRICT BLOCKING: Cannot create new invoice while one is active
            // Show dialog forcing merchant to handle the active invoice first
            Log.d(TAG, "BlockedByActiveInvoice: ${currentState.activeInvoiceId}, status=${currentState.activeStatus}")

            BlockedByActiveInvoiceDialog(
                invoiceId = currentState.activeInvoiceId,
                amount = currentState.activeAmount,
                status = currentState.activeStatus,
                onResume = {
                    // Resume the blocked invoice
                    scope.launch {
                        posManager.resumeBlockedInvoice(currentState.activeInvoiceId)
                    }
                },
                onCancel = {
                    // Cancel the blocked invoice and allow new payment
                    posManager.cancelBlockedInvoice(currentState.activeInvoiceId)
                }
            )
        }
    }
}

/**
 * STRICT BLOCKING dialog when active invoice exists.
 *
 * SECURITY: This dialog is BLOCKING - cannot be dismissed by tapping outside.
 * Merchant must explicitly choose to Resume or Cancel the active payment.
 *
 * Shown when:
 * - Merchant tries to create a new invoice while one is active (CREATED/PENDING)
 * - This prevents accidental loss of in-progress payment state
 */
@Composable
private fun BlockedByActiveInvoiceDialog(
    invoiceId: String,
    amount: Double,
    status: String,
    onResume: () -> Unit,
    onCancel: () -> Unit
) {
    val colors = WinoTheme.colors

    AlertDialog(
        onDismissRequest = { /* BLOCKING - cannot dismiss */ },
        containerColor = colors.bgSurface,
        titleContentColor = colors.textPrimary,
        textContentColor = colors.textSecondary,
        title = {
            Text(
                text = "Payment in progress",
                style = WinoTypography.h3Medium
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(WinoSpacing.XS)) {
                Text(
                    text = "You have an active payment that must be completed or canceled first.",
                    style = WinoTypography.body
                )
                Text(
                    text = "Invoice #$invoiceId",
                    style = WinoTypography.bodyMedium,
                    color = colors.textPrimary
                )
                Text(
                    text = "Amount: ${"%.2f".format(amount)} • Status: $status",
                    style = WinoTypography.small,
                    color = colors.textMuted
                )
            }
        },
        confirmButton = {
            // Primary action - resume existing payment
            TextButton(onClick = onResume) {
                Text(
                    text = "Go to payment",
                    color = colors.brandPrimary,
                    style = WinoTypography.bodyMedium
                )
            }
        },
        dismissButton = {
            // Destructive action - cancel existing payment
            TextButton(onClick = onCancel) {
                Text(
                    text = "Cancel payment",
                    color = colors.stateError,
                    style = WinoTypography.bodyMedium
                )
            }
        }
    )
}

/**
 * Blocking confirmation dialog for canceling an active payment.
 *
 * SECURITY: This dialog is BLOCKING - cannot be dismissed by tapping outside.
 * Merchant must explicitly choose "Cancel payment" or "Keep waiting".
 *
 * Shown when:
 * - User presses system back button during QR/Pending state
 * - User taps X button on QR screen
 */
@Composable
private fun CancelPaymentDialog(
    onConfirmCancel: () -> Unit,
    onDismiss: () -> Unit
) {
    val colors = WinoTheme.colors

    AlertDialog(
        onDismissRequest = { /* BLOCKING - don't dismiss on outside tap */ },
        containerColor = colors.bgSurface,
        titleContentColor = colors.textPrimary,
        textContentColor = colors.textSecondary,
        title = {
            Text(
                text = "Cancel payment?",
                style = WinoTypography.h3Medium
            )
        },
        text = {
            Text(
                text = "This payment is still pending. Canceling will stop detection and the customer will need to scan a new QR code.",
                style = WinoTypography.body
            )
        },
        confirmButton = {
            // Destructive action - cancel payment
            TextButton(onClick = onConfirmCancel) {
                Text(
                    text = "Cancel payment",
                    color = colors.stateError,
                    style = WinoTypography.bodyMedium
                )
            }
        },
        dismissButton = {
            // Safe action - keep waiting
            TextButton(onClick = onDismiss) {
                Text(
                    text = "Keep waiting",
                    color = colors.brandPrimary,
                    style = WinoTypography.bodyMedium
                )
            }
        }
    )
}
