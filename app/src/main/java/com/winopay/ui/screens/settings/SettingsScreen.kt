package com.winopay.ui.screens.settings

import android.content.Intent
import android.net.Uri
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.winopay.WinoPayApplication
import com.winopay.ui.components.PhosphorIcons
import com.winopay.ui.components.WinoButton
import com.winopay.ui.components.WinoButtonSize
import com.winopay.ui.components.WinoButtonVariant
import com.winopay.ui.components.WinoListItemLabel
import com.winopay.ui.components.WinoListItemNav
import com.winopay.i18n.L
import com.winopay.ui.components.safeClickable
import com.winopay.ui.theme.WinoSpacing
import com.winopay.ui.theme.WinoTheme
import com.winopay.ui.theme.WinoTypography

/**
 * Settings screen - Figma node 1343-14908
 *
 * Structure:
 * - Header (Settings title + X close)
 * - Profile section (avatar + name + address)
 * - Account list (Account, Payment methods, Export transactions)
 * - App settings section (Language, Balance currency, Appearence, App info)
 * - Get help section (FAQ, Support, Mail us)
 * - Legal documents section (Terms, Privacy, Risk)
 */
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onAccountClick: () -> Unit,
    onPaymentMethodsClick: () -> Unit,
    onLanguageClick: () -> Unit,
    onCurrencyClick: () -> Unit,
    onAppearanceClick: () -> Unit,
    onAppInfoClick: () -> Unit,
    onTermsClick: () -> Unit = {},
    onPrivacyClick: () -> Unit = {},
    onRiskDisclosureClick: () -> Unit = {},
    onLogout: () -> Unit = {}
) {
    val colors = WinoTheme.colors
    val app = WinoPayApplication.instance
    val context = LocalContext.current

    val businessIdentity by app.dataStoreManager.businessIdentity.collectAsState(initial = null)
    val walletPublicKey by app.dataStoreManager.walletPublicKey.collectAsState(initial = null)

    // Mask address: first 4 + ... + last 4
    val maskedAddress = walletPublicKey?.let { addr: String ->
        if (addr.length > 10) "${addr.substring(0, 4)}...${addr.substring(addr.length - 4)}" else addr
    } ?: ""

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.bgCanvas)
    ) {
        // ==================== SCROLLABLE CONTENT ====================
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(
                    top = 80.dp,  // Space for header
                    bottom = WinoSpacing.XL
                )
        ) {
            // ==================== PROFILE SECTION ====================
            // Figma: (1343:14913) - px=24, py=24, gap=24, centered
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(WinoSpacing.LG),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Avatar with edit button overlay
                // Figma: avatar (1343:14915) - 96dp, rounded-36dp
                Box(
                    modifier = Modifier.size(96.dp)
                ) {
                    // Avatar image
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(36.dp)),
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
                                .background(colors.brandPrimary),
                            contentAlignment = Alignment.Center
                        ) {
                            PhosphorIcons.Storefront(
                                size = 40.dp,
                                color = colors.textOnBrand
                            )
                        }
                    }

                    // Edit button overlay - Figma: (1343:14916) - 28dp, bgSurface, bottom-right
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(colors.bgSurface)
                            .clickable { onAccountClick() },
                        contentAlignment = Alignment.Center
                    ) {
                        PhosphorIcons.PencilSimple(
                            size = 16.dp,
                            color = colors.textPrimary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(WinoSpacing.LG))

                // Info section - Figma: (1343:14918) - gap=4, center text
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Merchant name - H2 Medium
                    Text(
                        text = businessIdentity?.name ?: L("settings.merchant_name"),
                        style = WinoTypography.h2Medium,
                        color = colors.textPrimary,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(WinoSpacing.XXS))

                    // Masked address - Small, secondary
                    Text(
                        text = maskedAddress,
                        style = WinoTypography.small,
                        color = colors.textSecondary,
                        textAlign = TextAlign.Center
                    )
                }
            }

            // ==================== ACCOUNT LIST ====================
            // Figma: (1343:14921) - px=24, no section title
            SettingsList(
                modifier = Modifier.padding(horizontal = WinoSpacing.LG)
            ) {
                // Account
                WinoListItemNav(
                    title = L("settings.account.title"),
                    onClick = onAccountClick,
                    leadingIcon = { PhosphorIcons.User(size = 20.dp, color = colors.textPrimary) }
                )

                // Payment methods
                WinoListItemNav(
                    title = L("settings.account.payment_methods"),
                    onClick = onPaymentMethodsClick,
                    leadingIcon = { PhosphorIcons.CreditCard(size = 20.dp, color = colors.textPrimary) }
                )

                // Export transactions - has "Soon" label
                WinoListItemLabel(
                    title = L("settings.account.export"),
                    label = L("common.soon"),
                    leadingIcon = { PhosphorIcons.Export(size = 20.dp, color = colors.textPrimary) }
                )
            }

            // ==================== APP SETTINGS SECTION ====================
            // Section title - Figma: (1343:14938) - px=40, pt=24, pb=16
            SectionTitle(title = L("settings.app.title"))

            SettingsList(
                modifier = Modifier.padding(horizontal = WinoSpacing.LG)
            ) {
                // Language
                WinoListItemNav(
                    title = L("settings.app.language"),
                    onClick = onLanguageClick,
                    leadingIcon = { PhosphorIcons.Globe(size = 20.dp, color = colors.textPrimary) }
                )

                // Balance currency
                WinoListItemNav(
                    title = L("settings.app.currency"),
                    onClick = onCurrencyClick,
                    leadingIcon = { PhosphorIcons.CurrencyDollarSimple(size = 20.dp, color = colors.textPrimary) }
                )

                // Appearence (spelling matches Figma)
                WinoListItemNav(
                    title = L("settings.app.appearance"),
                    onClick = onAppearanceClick,
                    leadingIcon = { PhosphorIcons.Palette(size = 20.dp, color = colors.textPrimary) }
                )

                // App info
                WinoListItemNav(
                    title = L("settings.app.app_info"),
                    onClick = onAppInfoClick,
                    leadingIcon = { PhosphorIcons.Info(size = 20.dp, color = colors.textPrimary) }
                )
            }

            // ==================== GET HELP SECTION ====================
            SectionTitle(title = L("settings.help.title"))

            SettingsList(
                modifier = Modifier.padding(horizontal = WinoSpacing.LG)
            ) {
                // Support (Telegram)
                WinoListItemNav(
                    title = L("settings.help.support"),
                    onClick = {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://t.me/Winobank_support"))
                        context.startActivity(intent)
                    },
                    leadingIcon = { PhosphorIcons.ChatCircle(size = 20.dp, color = colors.textPrimary) }
                )
            }

            // ==================== LEGAL DOCUMENTS SECTION ====================
            SectionTitle(title = L("legal.title"))

            // Document cards - Figma: (1343:14983) - horizontal row, gap=12, px=24
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = WinoSpacing.LG),
                horizontalArrangement = Arrangement.spacedBy(WinoSpacing.SM)
            ) {
                // Terms of use
                LegalDocCard(
                    title = L("legal.terms"),
                    onClick = onTermsClick
                )

                // Privacy policy
                LegalDocCard(
                    title = L("legal.privacy"),
                    onClick = onPrivacyClick
                )

                // Risk disclosure
                LegalDocCard(
                    title = L("legal.risk"),
                    onClick = onRiskDisclosureClick
                )
            }

            // ==================== LOG OUT BUTTON ====================
            // Figma: (1384:3666) - p=24dp
            WinoButton(
                text = L("common.logout"),
                onClick = onLogout,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(WinoSpacing.LG),
                variant = WinoButtonVariant.Destructive,
                size = WinoButtonSize.Large
            )
        }

        // ==================== HEADER WITH GRADIENT ====================
        // Positioned at top with gradient fade effect (solid at top, transparent at bottom)
        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        colorStops = arrayOf(
                            0f to colors.bgCanvas,
                            0.7f to colors.bgCanvas,
                            1f to colors.bgCanvas.copy(alpha = 0f)
                        )
                    )
                )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(
                        start = WinoSpacing.LG,
                        end = WinoSpacing.LG,
                        top = WinoSpacing.XS,
                        bottom = WinoSpacing.MD
                    ),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(WinoSpacing.SM)
            ) {
                // "Settings" - H1 Medium
                Text(
                    text = L("settings.title"),
                    style = WinoTypography.h1Medium,
                    color = colors.textPrimary,
                    modifier = Modifier.weight(1f)
                )

                // X close button - 36dp, bgSurface, rounded full
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(colors.bgSurface)
                        .safeClickable { onBack() },
                    contentAlignment = Alignment.Center
                ) {
                    PhosphorIcons.X(
                        size = 20.dp,
                        color = colors.textPrimary
                    )
                }
            }
            // Extra gradient space
            Spacer(modifier = Modifier.height(WinoSpacing.MD))
        }
    }
}

/**
 * Section title - Figma: px=40, pt=24, pb=16
 */
@Composable
private fun SectionTitle(title: String) {
    val colors = WinoTheme.colors

    Text(
        text = title,
        style = WinoTypography.h3Medium,
        color = colors.textPrimary,
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                start = WinoSpacing.XXL,
                end = WinoSpacing.XXL,
                top = WinoSpacing.LG,
                bottom = WinoSpacing.MD
            )
    )
}

/**
 * Settings list container - Figma: bgSurface, rounded-36dp, px=16
 */
@Composable
private fun SettingsList(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val colors = WinoTheme.colors

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(36.dp))
            .background(colors.bgSurface)
            .padding(horizontal = WinoSpacing.MD)
    ) {
        content()
    }
}

/**
 * Legal document card - Figma: (1343:14984)
 * w=160px, p=16, gap=24, rounded=24
 */
@Composable
private fun LegalDocCard(
    title: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = WinoTheme.colors

    Column(
        modifier = modifier
            .width(160.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(colors.bgSurface)
            .clickable { onClick() }
            .padding(WinoSpacing.MD),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        // File icon - 28dp
        PhosphorIcons.File(
            size = 28.dp,
            color = colors.textPrimary
        )

        // Title - H3 Medium
        Text(
            text = title,
            style = WinoTypography.h3Medium,
            color = colors.textPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
