package com.winopay.ui.screens.dashboard

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.navigationBarsPadding
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.winopay.WinoPayApplication
import com.winopay.data.CurrencyConverter
import com.winopay.solana.balance.BalanceModel
import com.winopay.solana.balance.BalanceWarning
import com.winopay.ui.components.InvoiceList
import com.winopay.ui.components.PhosphorIcons
import com.winopay.ui.components.WinoPrimaryButton
import com.winopay.ui.theme.WinoRadius
import com.winopay.ui.theme.WinoSpacing
import com.winopay.ui.theme.WinoTheme
import com.winopay.ui.theme.WinoTypography
import kotlinx.coroutines.launch

private const val TAG = "DashboardScreen"

// Figma frame: 2.1 Dashboard (793:9230)
// MCP source: get_screenshot, get_design_context for node 793:9230

@Composable
fun DashboardScreen(
    onNewPayment: () -> Unit,
    onSettings: () -> Unit = {},
    onInvoiceClick: (String) -> Unit = {},
    onLogout: () -> Unit
) {
    val colors = WinoTheme.colors
    val app = WinoPayApplication.instance
    val scope = rememberCoroutineScope()

    val businessIdentity by app.dataStoreManager.businessIdentity.collectAsState(initial = null)
    val currency by app.dataStoreManager.currency.collectAsState(initial = "THB")
    var showLogoutDialog by remember { mutableStateOf(false) }

    // Observe invoices from Room
    val invoices by app.invoiceRepository.observeRecentInvoices(100).collectAsState(initial = emptyList())

    // Observe balance from BalanceRepository (connected via MWA wallet session)
    val balanceModel by app.balanceRepository.balance.collectAsState(initial = BalanceModel.empty())
    val balance = balanceModel.totalStablecoinBalanceDouble
    val balanceWarning = balanceModel.warning

    // Refresh balance when screen loads
    LaunchedEffect(Unit) {
        Log.d(TAG, "Refreshing USDC balance from BalanceRepository")
        app.balanceRepository.refresh()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.bgCanvas)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .verticalScroll(rememberScrollState())
        ) {
            // "header" (793:9266)
            // sticky top, pb=XS(8dp), px=LG(24dp), gap=LG(24dp)
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
                horizontalArrangement = Arrangement.spacedBy(WinoSpacing.LG)
            ) {
                // "userinfo" (793:9267): flex-1, gap=SM(12dp)
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(WinoSpacing.SM)
                ) {
                    // Business identity icon (sm): 32dp, rounded-XL(36dp)
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(RoundedCornerShape(WinoRadius.XL)),
                        contentAlignment = Alignment.Center
                    ) {
                        businessIdentity?.logoUri?.let { uri ->
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

                    // "text" (793:9269): flex-1, gap=0
                    Column(
                        modifier = Modifier.weight(1f)
                    ) {
                        // Name row: gap=XXS(4dp)
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(WinoSpacing.XXS)
                        ) {
                            // "Test Name" — H3 Medium (18/24 -2%), textPrimary
                            Text(
                                text = businessIdentity?.name ?: "Business",
                                style = WinoTypography.h3Medium,
                                color = colors.textPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            // SealCheck icon: 16dp, brandPrimary
                            PhosphorIcons.SealCheck(
                                size = 16.dp,
                                color = colors.brandPrimary
                            )
                        }
                        // "Verified business" — Micro, textSecondary
                        Text(
                            text = "Verified business",
                            style = WinoTypography.micro,
                            color = colors.textSecondary
                        )
                    }
                }

                // "icon button" (793:9275): 36dp circle, bgSurface, Gear 20dp
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(WinoRadius.XL))
                        .background(colors.bgSurface)
                        .clickable { onSettings() },
                    contentAlignment = Alignment.Center
                ) {
                    PhosphorIcons.GearSix(
                        size = 20.dp,
                        color = colors.textPrimary
                    )
                }
            }

            // "content" (793:9244): gap=LG(24dp), p=LG(24dp)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(WinoSpacing.LG),
                verticalArrangement = Arrangement.spacedBy(WinoSpacing.LG)
            ) {
                // Balance block
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(WinoSpacing.XS),
                    verticalArrangement = Arrangement.spacedBy(WinoSpacing.XXS)
                ) {
                    // Balance is in USD (stablecoins USDC/USDT = USD)
                    // Convert to selected display currency for main display
                    val displayBalance = CurrencyConverter.convertFromUsd(balance, currency) ?: balance
                    val formattedBalance = String.format("%,.2f", displayBalance)
                    val parts = formattedBalance.split(".")

                    // Balance amount (primary) - in selected currency
                    Text(
                        text = buildAnnotatedString {
                            withStyle(SpanStyle(
                                fontSize = 36.sp,
                                letterSpacing = (-1.08).sp
                            )) {
                                append(parts[0])
                            }
                            withStyle(SpanStyle(
                                fontSize = 36.sp,
                                letterSpacing = (-1.08).sp
                            )) {
                                append(".${parts.getOrElse(1) { "00" }} $currency")
                            }
                        },
                        style = WinoTypography.display,
                        color = colors.textPrimary
                    )

                    // USD equivalent (secondary) - only show if currency is NOT USD
                    if (currency != "USD") {
                        Text(
                            text = "= $${String.format("%,.2f", balance)} USD",
                            style = WinoTypography.body,
                            color = colors.textSecondary
                        )
                    }
                }

                // Mint mismatch warning banner
                if (balanceWarning is BalanceWarning.MintMismatch) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(WinoRadius.MD))
                            .background(colors.stateWarningSoft)
                            .padding(WinoSpacing.SM)
                    ) {
                        Row(
                            verticalAlignment = Alignment.Top,
                            horizontalArrangement = Arrangement.spacedBy(WinoSpacing.XS)
                        ) {
                            PhosphorIcons.Warning(
                                size = 20.dp,
                                color = colors.stateWarning
                            )
                            Column(
                                verticalArrangement = Arrangement.spacedBy(WinoSpacing.XXS)
                            ) {
                                Text(
                                    text = "Token Mint Mismatch",
                                    style = WinoTypography.smallSemiBold,
                                    color = colors.stateWarning
                                )
                                Text(
                                    text = balanceWarning.message,
                                    style = WinoTypography.micro,
                                    color = colors.textSecondary
                                )
                            }
                        }
                    }
                }
            }

            // "operation list" (793:9231): px=LG(24dp)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = WinoSpacing.LG)
            ) {
                // "Transactions" header (793:9232): pt=SM(12dp), pb=6dp
                Text(
                    text = "Transactions",
                    style = WinoTypography.h2Medium,
                    color = colors.textPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(
                        top = WinoSpacing.SM,
                        bottom = 6.dp
                    )
                )

                // Invoice rows from Room
                InvoiceList(
                    invoices = invoices,
                    onInvoiceClick = { invoice ->
                        onInvoiceClick(invoice.id)
                    }
                )
            }

            // Bottom padding for the floating button (button height 56dp + vertical padding 24dp + nav bar)
            Spacer(modifier = Modifier.height(80.dp))
            Spacer(modifier = Modifier.navigationBarsPadding())
        }

        // "bottom" (793:9262): absolute bottom
        // "Bottom Button Layout" (793:9263): px=LG(24dp), py=SM(12dp)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .background(colors.bgCanvas)
                .navigationBarsPadding()
                .padding(
                    start = WinoSpacing.LG,
                    end = WinoSpacing.LG,
                    top = WinoSpacing.SM,
                    bottom = WinoSpacing.SM
                )
        ) {
            // "POS Mode" button — primary, standard
            WinoPrimaryButton(
                text = "POS Mode",
                onClick = onNewPayment
            )
        }
    }

    // Logout confirmation dialog
    if (showLogoutDialog) {
        LogoutConfirmDialog(
            onConfirm = {
                showLogoutDialog = false
                // Data clearing is handled by Navigation.kt via app.logout()
                onLogout()
            },
            onDismiss = { showLogoutDialog = false }
        )
    }
}


@Composable
private fun LogoutConfirmDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    val colors = WinoTheme.colors

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.bgCanvas.copy(alpha = 0.9f))
            .clickable { onDismiss() },
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .padding(WinoSpacing.LG)
                .clip(RoundedCornerShape(WinoRadius.XL))
                .background(colors.bgSurface)
                .padding(WinoSpacing.LG)
                .clickable(enabled = false) { },
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(colors.stateErrorSoft),
                contentAlignment = Alignment.Center
            ) {
                PhosphorIcons.Warning(
                    size = 32.dp,
                    color = colors.stateError
                )
            }

            Spacer(modifier = Modifier.height(WinoSpacing.MD))

            Text(
                text = "Log out?",
                style = WinoTypography.h2,
                color = colors.textPrimary
            )

            Spacer(modifier = Modifier.height(WinoSpacing.XS))

            Text(
                text = "This will clear all local data including your wallet keys.",
                style = WinoTypography.body,
                color = colors.textMuted
            )

            Spacer(modifier = Modifier.height(WinoSpacing.LG))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(WinoSpacing.SM)
            ) {
                com.winopay.ui.components.WinoButton(
                    text = "Cancel",
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f),
                    variant = com.winopay.ui.components.WinoButtonVariant.Secondary,
                    size = com.winopay.ui.components.WinoButtonSize.Large
                )

                com.winopay.ui.components.WinoButton(
                    text = "Log out",
                    onClick = onConfirm,
                    modifier = Modifier.weight(1f),
                    variant = com.winopay.ui.components.WinoButtonVariant.Destructive,
                    size = com.winopay.ui.components.WinoButtonSize.Large
                )
            }
        }
    }
}
