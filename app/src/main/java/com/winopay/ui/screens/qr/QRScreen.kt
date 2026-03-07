package com.winopay.ui.screens.qr

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.winopay.BuildConfig
import com.winopay.WinoPayApplication
import com.winopay.data.pos.PaymentMethod
import com.winopay.ui.components.PhosphorIcons
import com.winopay.ui.components.QrCodeGenerator
import com.winopay.ui.components.WinoSecondaryButton
import com.winopay.ui.theme.WinoRadius
import com.winopay.ui.theme.WinoSpacing
import com.winopay.ui.theme.WinoTheme
import com.winopay.ui.theme.WinoTypography
import kotlinx.coroutines.delay

private const val TAG = "QRScreen"

/**
 * DEBUG ONLY: Open Solana Pay URL in external wallet app.
 * Long-press on QR code to trigger.
 */
private fun openSolanaPayInWallet(context: Context, solanaPayUrl: String) {
    Log.d(TAG, "━━━━━ DEBUG: OPEN IN WALLET ━━━━━")
    Log.d(TAG, "Solana Pay URL: $solanaPayUrl")

    val uri = Uri.parse(solanaPayUrl)
    val intent = Intent(Intent.ACTION_VIEW, uri).apply {
        if (context !is Activity) addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }

    // Query which apps can handle this URI
    val packageManager = context.packageManager
    val resolveInfos = packageManager.queryIntentActivities(intent, 0)

    Log.d(TAG, "Resolved activities (${resolveInfos.size}):")
    resolveInfos.forEach { info ->
        Log.d(TAG, "  - ${info.activityInfo.packageName} / ${info.activityInfo.name}")
    }

    if (resolveInfos.isNotEmpty()) {
        Log.d(TAG, "Launching intent...")
        try {
            context.startActivity(intent)
            Toast.makeText(context, "Opening in wallet...", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to launch: ${e.message}", e)
            Toast.makeText(context, "Failed: ${e.message}", Toast.LENGTH_LONG).show()
        }
    } else {
        Log.w(TAG, "No wallet app found that handles solana: URIs")
        Toast.makeText(context, "No Solana wallet app found", Toast.LENGTH_LONG).show()
    }
    Log.d(TAG, "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
}

// Figma frames:
// 3.2.1 QR – Pay with USDC (793:19175)
// 3.2.2 QR – Pay by crypto (973:6345)

/**
 * Stateful QR screen that integrates with PosManager.
 * Observes state for timer and displays QR from provided data.
 */
@Composable
fun QRScreenStateful(
    invoiceId: String,
    amount: Double,
    usdcAmount: Double = amount,  // Actual USDC amount
    method: PaymentMethod,
    qrData: String,
    expiresAt: Long,
    onOtherMethods: () -> Unit,
    onCancel: () -> Unit
) {
    val app = WinoPayApplication.instance
    val businessIdentity by app.dataStoreManager.businessIdentity.collectAsState(initial = null)

    var qrBitmap by remember { mutableStateOf<Bitmap?>(null) }

    // Timer countdown based on expiresAt
    var remainingSeconds by remember { mutableIntStateOf(((expiresAt - System.currentTimeMillis()) / 1000).coerceAtLeast(0).toInt()) }

    LaunchedEffect(expiresAt) {
        while (remainingSeconds > 0) {
            delay(1000)
            remainingSeconds = ((expiresAt - System.currentTimeMillis()) / 1000).coerceAtLeast(0).toInt()
        }
    }

    // Generate QR bitmap from data using QrCodeGenerator
    LaunchedEffect(qrData) {
        qrBitmap = QrCodeGenerator.generate(qrData, 512)
    }

    val timerText = String.format("%d:%02d", remainingSeconds / 60, remainingSeconds % 60)

    val title = "Pay with ${method.symbol}"

    val headerSubtitle = method.networkDisplayName.removePrefix("on ") + " network"

    val shortRef = if (invoiceId.length > 8) {
        "${invoiceId.take(4)}\u2026${invoiceId.takeLast(4)}"
    } else invoiceId

    // For crypto payments, show the TOKEN amount (USDC), not merchant currency
    val tokenCurrency = method.symbol

    // Format USDC amount with proper precision (up to 6 decimals, but trim trailing zeros)
    val formattedUsdcAmount = String.format(java.util.Locale.US, "%.2f", usdcAmount)

    // Subtitle shows: "10.50 USDC • abc1...efgh"
    val amountSubtitle = "$formattedUsdcAmount $tokenCurrency \u2022 $shortRef"

    val context = LocalContext.current

    // Log Solana Pay URL for debugging
    LaunchedEffect(qrData) {
        Log.d(TAG, "━━━━━ QR SCREEN DEBUG ━━━━━")
        Log.d(TAG, "Solana Pay URL: $qrData")
        Log.d(TAG, "USDC Amount: $formattedUsdcAmount")
        Log.d(TAG, "Long-press QR to open in wallet app")
        Log.d(TAG, "━━━━━━━━━━━━━━━━━━━━━━━━━━━")
    }

    QRScreenContent(
        title = title,
        headerSubtitle = headerSubtitle,
        timerText = timerText,
        displayAmount = "$formattedUsdcAmount $tokenCurrency",
        amountSubtitle = amountSubtitle,
        qrBitmap = qrBitmap,
        onLongPressQr = {
            // DEBUG: Open Solana Pay URL in external wallet
            if (BuildConfig.DEBUG) {
                openSolanaPayInWallet(context, qrData)
            }
        },
        onOtherMethods = onOtherMethods,
        onCancel = onCancel
    )
}

/**
 * Pure UI content for QR screen.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun QRScreenContent(
    title: String,
    headerSubtitle: String,
    timerText: String,
    displayAmount: String,
    amountSubtitle: String,
    qrBitmap: Bitmap?,
    onLongPressQr: () -> Unit = {},
    onOtherMethods: () -> Unit,
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
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = WinoTypography.h3Medium,
                    color = colors.textPrimary
                )
                Text(
                    text = headerSubtitle,
                    style = WinoTypography.small,
                    color = colors.textSecondary
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(WinoSpacing.XS)
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(WinoRadius.XL))
                        .background(colors.bgSurface)
                        .padding(
                            horizontal = WinoSpacing.SM,
                            vertical = WinoSpacing.XS
                        )
                ) {
                    Text(
                        text = timerText,
                        style = WinoTypography.bodyMedium,
                        color = colors.textPrimary
                    )
                }

                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(WinoRadius.XL))
                        .background(colors.bgSurface)
                        .clickable { onCancel() },
                    contentAlignment = Alignment.Center
                ) {
                    PhosphorIcons.X(
                        size = 20.dp,
                        color = colors.textPrimary
                    )
                }
            }
        }

        // Content
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(horizontal = WinoSpacing.LG),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(WinoSpacing.MD)
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
                Text(
                    text = displayAmount,
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

            // QR Code card - Long-press to open in wallet (DEBUG)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .clip(RoundedCornerShape(WinoRadius.XL))
                    .background(colors.bgSurface)
                    .combinedClickable(
                        onClick = { /* normal tap does nothing */ },
                        onLongClick = onLongPressQr
                    )
                    .padding(WinoSpacing.XL),
                contentAlignment = Alignment.Center
            ) {
                if (qrBitmap != null) {
                    Image(
                        bitmap = qrBitmap.asImageBitmap(),
                        contentDescription = "Payment QR Code (Long-press to open in wallet)",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Fit
                    )
                } else {
                    CircularProgressIndicator(
                        modifier = Modifier.size(WinoSpacing.XXXL),
                        color = colors.brandPrimary
                    )
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
                text = "Wino cannot reverse or refund transactions.",
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
                WinoSecondaryButton(
                    text = "Pay with other methods",
                    onClick = onOtherMethods
                )
            }
        }
    }
}
