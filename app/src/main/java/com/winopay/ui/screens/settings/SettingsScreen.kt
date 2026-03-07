package com.winopay.ui.screens.settings

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.winopay.R
import com.winopay.WinoPayApplication
import com.winopay.solana.wallet.SessionState
import com.winopay.ui.components.PhosphorIcons
import com.winopay.update.UpdateChecker
import com.winopay.ui.theme.WinoRadius
import com.winopay.ui.theme.WinoSpacing
import com.winopay.ui.theme.WinoTheme
import com.winopay.ui.theme.WinoTypography

// Figma frames:
// 4.1 Settings - verified (967:5623)
// 4.1 Settings - unverified (973:6253)

@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onBusinessInfoClick: () -> Unit,
    onPaymentMethodsClick: () -> Unit,
    onConnectedWalletsClick: () -> Unit,
    onCurrencyClick: () -> Unit,
    onLanguageClick: () -> Unit,
    onAppearanceClick: () -> Unit,
    onAppInfoClick: () -> Unit,
    onCheckForUpdatesClick: () -> Unit,
    onLogout: () -> Unit
) {
    val colors = WinoTheme.colors
    val app = WinoPayApplication.instance

    val businessIdentity by app.dataStoreManager.businessIdentity.collectAsState(initial = null)
    val currency by app.dataStoreManager.currency.collectAsState(initial = "THB")
    val sessionState by app.sessionManager.sessionState.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.bgCanvas)
            .statusBarsPadding()
    ) {
        // "header" (967:5624): pb=XS(8dp), px=LG(24dp), gap=SM(12dp)
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
            // "Settings" — H1 Medium, textPrimary, flex-1
            Text(
                text = stringResource(R.string.settings),
                style = WinoTypography.h1Medium,
                color = colors.textPrimary,
                modifier = Modifier.weight(1f)
            )

            // X close button: 36dp, bgSurface, rounded-XL
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

        // Scrollable content: gap=MD(16dp), p=LG(24dp)
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding()
                .padding(WinoSpacing.LG),
            verticalArrangement = Arrangement.spacedBy(WinoSpacing.MD)
        ) {
            // "userinfo" card (967:5867): bgSurface, rounded-XL, p=MD(16dp), gap=SM(12dp)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(WinoRadius.XL))
                    .background(colors.bgSurface)
                    .clickable { onBusinessInfoClick() }
                    .padding(WinoSpacing.MD),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(WinoSpacing.SM)
            ) {
                // Business identity icon: 40dp, rounded-XL
                Box(
                    modifier = Modifier
                        .size(40.dp)
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
                            .background(colors.bgAccentSoft),
                        contentAlignment = Alignment.Center
                    ) {
                        PhosphorIcons.Storefront(
                            size = 20.dp,
                            color = colors.brandPrimary
                        )
                    }
                }

                // "text": flex-1, gap=0
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    // Business name
                    Text(
                        text = businessIdentity?.name ?: "Business",
                        style = WinoTypography.h3Medium,
                        color = colors.textPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    // Subtitle
                    Text(
                        text = stringResource(R.string.business_info_subtitle),
                        style = WinoTypography.micro,
                        color = colors.textSecondary
                    )
                }

                // CaretRight: 20dp
                PhosphorIcons.CaretRight(
                    size = 20.dp,
                    color = colors.textTertiary
                )
            }

            // Settings rows — flat list, no section headers
            // Figma: all icons have bgAccentSoft bg and brandPrimary color
            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                // Balance currency (967:5739) — CurrencyDollarSimple icon
                SettingsRow(
                    icon = { PhosphorIcons.CurrencyCircleDollar(size = 20.dp, color = colors.brandPrimary) },
                    iconBg = colors.bgAccentSoft,
                    title = stringResource(R.string.balance_currency),
                    value = currency,
                    onClick = onCurrencyClick
                )

                // Language (967:6014) — Translate icon
                SettingsRow(
                    icon = { PhosphorIcons.Globe(size = 20.dp, color = colors.brandPrimary) },
                    iconBg = colors.bgAccentSoft,
                    title = stringResource(R.string.language),
                    value = "ENG",
                    onClick = onLanguageClick
                )

                // Payment methods (967:5955) — Cardholder icon
                SettingsRow(
                    icon = { PhosphorIcons.CreditCard(size = 20.dp, color = colors.brandPrimary) },
                    iconBg = colors.bgAccentSoft,
                    title = stringResource(R.string.payment_methods),
                    onClick = onPaymentMethodsClick
                )

                // Connected wallet
                val walletStatusConnecting = stringResource(R.string.connecting_to_wallet)
                val walletStatusError = stringResource(R.string.error)
                val walletStatusNotConnected = stringResource(R.string.not_connected)
                val walletStatus = when (val state = sessionState) {
                    is SessionState.Active -> state.publicKey.take(4) + "..." + state.publicKey.takeLast(4)
                    is SessionState.Connecting -> walletStatusConnecting
                    is SessionState.SessionError -> walletStatusError
                    else -> walletStatusNotConnected
                }
                val walletIconColor = when (sessionState) {
                    is SessionState.Active -> colors.stateSuccess
                    is SessionState.SessionError -> colors.stateError
                    else -> colors.textMuted
                }
                SettingsRow(
                    icon = { PhosphorIcons.Wallet(size = 20.dp, color = walletIconColor) },
                    iconBg = if (sessionState is SessionState.Active) colors.stateSuccessSoft else colors.bgSurfaceAlt,
                    title = stringResource(R.string.connected_wallet),
                    value = walletStatus,
                    onClick = {
                        android.util.Log.d("SettingsScreen", "NAV|CLICK|ConnectedWallets")
                        onConnectedWalletsClick()
                    }
                )

                // Appearance (973:4541) — SunHorizon icon
                SettingsRow(
                    icon = { PhosphorIcons.Sun(size = 20.dp, color = colors.brandPrimary) },
                    iconBg = colors.bgAccentSoft,
                    title = stringResource(R.string.appearance),
                    onClick = onAppearanceClick
                )

                // Get help (967:6148) — ChatsTeardrop icon
                SettingsRow(
                    icon = { PhosphorIcons.ChatCircle(size = 20.dp, color = colors.brandPrimary) },
                    iconBg = colors.bgAccentSoft,
                    title = stringResource(R.string.get_help),
                    onClick = { }
                )

                // FAQ (973:5474) — ChatText icon
                SettingsRow(
                    icon = { PhosphorIcons.FileText(size = 20.dp, color = colors.brandPrimary) },
                    iconBg = colors.bgAccentSoft,
                    title = stringResource(R.string.faq),
                    onClick = { }
                )

                // App info (967:6137) — Info icon
                SettingsRow(
                    icon = { PhosphorIcons.Info(size = 20.dp, color = colors.brandPrimary) },
                    iconBg = colors.bgAccentSoft,
                    title = stringResource(R.string.app_info),
                    value = "v0.1",
                    onClick = onAppInfoClick
                )

                // Check for updates — DEBUG builds only
                if (UpdateChecker.isUpdateCheckEnabled()) {
                    SettingsRow(
                        icon = { PhosphorIcons.ArrowsClockwise(size = 20.dp, color = colors.brandPrimary) },
                        iconBg = colors.bgAccentSoft,
                        title = "Check for updates",
                        onClick = onCheckForUpdatesClick
                    )
                }
            }
        }
    }
}

@Composable
private fun SettingsRow(
    icon: @Composable () -> Unit,
    iconBg: Color,
    title: String,
    value: String? = null,
    onClick: () -> Unit
) {
    val colors = WinoTheme.colors

    // Row: gap=MD(16dp), py=MD(16dp), clickable
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = WinoSpacing.MD),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(WinoSpacing.MD)
    ) {
        // Icon circle: 36dp, colored bg, rounded-XL
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(WinoRadius.XL))
                .background(iconBg),
            contentAlignment = Alignment.Center
        ) {
            icon()
        }

        // Title: H3 Medium, textPrimary, flex-1, ellipsis
        Text(
            text = title,
            style = WinoTypography.h3Medium,
            color = colors.textPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )

        // Right side: value text + CaretRight
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(WinoSpacing.XXS)
        ) {
            if (value != null) {
                Text(
                    text = value,
                    style = WinoTypography.bodyMedium,
                    color = colors.textTertiary
                )
            }
            PhosphorIcons.CaretRight(
                size = 20.dp,
                color = colors.textTertiary
            )
        }
    }
}
