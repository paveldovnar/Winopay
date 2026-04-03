package com.winopay.ui.screens.onboarding

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import com.winopay.ui.components.BlurredGradientOverlay
import com.winopay.ui.components.TopGradientFade
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.winopay.R
import com.winopay.WinoPayApplication
import com.winopay.i18n.L
import com.winopay.ui.components.PhosphorIcons
import com.winopay.ui.components.WinoButton
import com.winopay.ui.components.WinoButtonSize
import com.winopay.ui.components.WinoButtonVariant
import com.winopay.ui.theme.WinoSpacing
import com.winopay.ui.theme.WinoTheme
import com.winopay.ui.theme.WinoTypography
import androidx.compose.foundation.shape.RoundedCornerShape
import kotlinx.coroutines.launch

// Figma frame: 1.7 Balance currency (1343:13974)

data class CurrencyOption(
    val code: String,
    val name: String,
    val symbol: String,
    @DrawableRes val flagRes: Int? = null  // null = use placeholder
)

// Currencies matching Figma design (1343:13974) - all 27 currencies
// Flags exported as PNG from Figma MCP
private val currencies = listOf(
    CurrencyOption("USD", "United States Dollar", "$", R.drawable.flag_us),
    CurrencyOption("EUR", "Euro", "€", R.drawable.flag_eu),
    CurrencyOption("AED", "Dirham", "د.إ", R.drawable.flag_ae),
    CurrencyOption("THB", "Thai Baht", "฿", R.drawable.flag_th),
    CurrencyOption("VND", "Vietnamese Dong", "₫", R.drawable.flag_vn),
    CurrencyOption("CAD", "Canadian Dollar", "C$", R.drawable.flag_ca),
    CurrencyOption("JPY", "Japanese Yen", "¥", R.drawable.flag_jp),
    CurrencyOption("KRW", "South Korean Won", "₩", R.drawable.flag_kr),
    CurrencyOption("SGD", "Singapore Dollar", "S$", R.drawable.flag_sg),
    CurrencyOption("AUD", "Australian Dollar", "A$", R.drawable.flag_au),
    CurrencyOption("IDR", "Indonesian Rupiah", "Rp", R.drawable.flag_id),
    CurrencyOption("TRY", "Turkish Lira", "₺", R.drawable.flag_tr),
    CurrencyOption("MXN", "Mexican Peso", "$", R.drawable.flag_mx),
    CurrencyOption("BRL", "Brazilian Real", "R$", R.drawable.flag_br),
    CurrencyOption("ARS", "Argentine Peso", "$", R.drawable.flag_ar),
    CurrencyOption("PHP", "Philippine Peso", "₱", R.drawable.flag_ph),
    CurrencyOption("MYR", "Malaysian Ringgit", "RM", R.drawable.flag_my),
    CurrencyOption("COP", "Colombian Peso", "$", R.drawable.flag_co),
    CurrencyOption("PEN", "Peruvian Sol", "S/", R.drawable.flag_pe),
    CurrencyOption("CLP", "Chilean Peso", "$", R.drawable.flag_cl),
    CurrencyOption("EGP", "Egyptian Pound", "£", R.drawable.flag_eg),
    CurrencyOption("MAD", "Moroccan Dirham", "د.م.", R.drawable.flag_ma),
    CurrencyOption("GEL", "Georgian Lari", "₾", R.drawable.flag_ge),
    CurrencyOption("KZT", "Kazakhstani Tenge", "₸", R.drawable.flag_kz),
    CurrencyOption("UZS", "Uzbekistani Som", "SOM", R.drawable.flag_uz),
    CurrencyOption("KES", "Kenyan Shilling", "KSh", R.drawable.flag_ke),
    CurrencyOption("ZAR", "South African Rand", "R", R.drawable.flag_za)
)

@Composable
fun CurrencySetupScreen(
    onContinue: () -> Unit,
    onBack: () -> Unit
) {
    val colors = WinoTheme.colors
    val app = WinoPayApplication.instance
    val scope = rememberCoroutineScope()
    val uriHandler = LocalUriHandler.current

    // Selected currency state (default to USD)
    var selectedCurrency by remember { mutableStateOf("USD") }

    fun saveAndContinue() {
        scope.launch {
            app.dataStoreManager.setCurrency(selectedCurrency)
            onContinue()
        }
    }

    // Figma: 1.7 Balance currency (1343:13974)
    // Structure: Fixed header + scrollable list + overlay at bottom
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.bgCanvas)
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // Header: FIXED (stops at edge, doesn't scroll)
            // pt=32dp (safe area), px=24dp, py=16dp, gap=4dp (Figma: 1343:13975/1343:13976)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = WinoSpacing.XL) // Safe area top = 32dp per Figma
                    .padding(horizontal = WinoSpacing.LG, vertical = WinoSpacing.MD),
                verticalArrangement = Arrangement.spacedBy(WinoSpacing.XXS)
            ) {
                // Title: H1 Medium (Figma: 1343:13977)
                Text(
                    text = L("onboarding.currency_setup.title"),
                    style = WinoTypography.h1Medium,
                    color = colors.textPrimary
                )

                // Subtitle: Body Regular (Figma: 1343:13978)
                Text(
                    text = L("onboarding.currency_setup.subtitle"),
                    style = WinoTypography.body,
                    color = colors.textSecondary
                )
            }

            // Scrollable list area with top gradient fade
            Box(modifier = Modifier.weight(1f)) {
                // Scrollable list ONLY (list goes UNDER the bottom overlay)
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                ) {
                    // Currency list: hugs content, all corners 36dp
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = WinoSpacing.LG)
                            .padding(top = WinoSpacing.MD)
                            .clip(RoundedCornerShape(36.dp))
                            .background(colors.bgSurface)
                    ) {
                        currencies.forEach { currency ->
                            CurrencyRow(
                                currency = currency,
                                isSelected = currency.code == selectedCurrency,
                                onClick = { selectedCurrency = currency.code }
                            )
                        }
                    }

                    // Space for bottom overlay (OUTSIDE list shape)
                    Box(modifier = Modifier.height(240.dp))
                }

                // Top gradient fade (inverse of bottom overlay)
                TopGradientFade(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .height(24.dp),
                    canvasColor = colors.bgCanvas
                )
            }
        }

        // Bottom overlay - positioned at bottom, ON TOP of the list (Figma: 1343:14089)
        // Gradient from transparent to canvas color with blur
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
        ) {
            BlurredGradientOverlay(
                canvasColor = colors.bgCanvas
            ) {
                // Content
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = WinoSpacing.MD) // Safe area bottom
                ) {
                    // Helper section: pt=24dp, pb=16dp, px=32dp, gap=4dp (Figma: 1343:14091)
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(
                                top = WinoSpacing.LG,
                                bottom = WinoSpacing.MD,
                                start = WinoSpacing.XL,
                                end = WinoSpacing.XL
                            ),
                        verticalArrangement = Arrangement.spacedBy(WinoSpacing.XXS)
                    ) {
                        // Helper text: Small Regular
                        Text(
                            text = L("onboarding.currency_setup.disclaimer"),
                            style = WinoTypography.small,
                            color = colors.textSecondary
                        )
                        // "Learn more" link: Small Medium, brand color
                        Text(
                            text = L("common.learn_more"),
                            style = WinoTypography.smallMedium,
                            color = colors.brandPrimary,
                            modifier = Modifier.clickable {
                                uriHandler.openUri("https://winobank.com/docs/legal/termsofuse")
                            }
                        )
                    }

                    // Button layout: p=24dp (Figma: 1343:14092)
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(WinoSpacing.LG)
                    ) {
                        // Continue button - Primary (Figma: 1343:14093)
                        WinoButton(
                            text = L("common.continue"),
                            onClick = { saveAndContinue() },
                            modifier = Modifier.fillMaxWidth(),
                            variant = WinoButtonVariant.Primary,
                            size = WinoButtonSize.Large
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CurrencyRow(
    currency: CurrencyOption,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val colors = WinoTheme.colors

    // Row: px=16dp, py=16dp (68dp height total) (Figma: 1343:13981)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = WinoSpacing.MD, vertical = WinoSpacing.MD),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(WinoSpacing.MD)
    ) {
        // Flag: 36x36dp, circular clip (Figma: 1343:13982)
        if (currency.flagRes != null) {
            Image(
                painter = painterResource(id = currency.flagRes),
                contentDescription = "${currency.code} flag",
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape),
                contentScale = ContentScale.Crop
            )
        } else {
            // Placeholder for missing flags
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(colors.bgSurfaceAlt),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = currency.code.take(2),
                    style = WinoTypography.microMedium,
                    color = colors.textMuted
                )
            }
        }

        // Currency name: H3 Medium (Figma: 1343:13983) - 18sp, -2% tracking
        Text(
            text = currency.name,
            style = WinoTypography.h3Medium,
            color = colors.textPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )

        // Trailing: checkmark for selected, symbol for others (Figma: 1343:13984 / 1343:13988)
        if (isSelected) {
            // Checkmark: 20dp (Figma)
            PhosphorIcons.Check(size = 20.dp, color = colors.textPrimary)
        } else {
            // Currency symbol: Body Medium, tertiary color
            Text(
                text = currency.symbol,
                style = WinoTypography.bodyMedium,
                color = colors.textTertiary
            )
        }
    }
}
