package com.winopay.ui.screens.onboarding

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.ClickableText
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
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
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.winopay.R
import com.winopay.WinoPayApplication
import com.winopay.ui.components.PhosphorIcons
import com.winopay.ui.components.WinoButton
import com.winopay.ui.components.WinoButtonSize
import com.winopay.ui.components.WinoButtonVariant
import com.winopay.ui.theme.WinoRadius
import com.winopay.ui.theme.WinoSpacing
import com.winopay.ui.theme.WinoTheme
import com.winopay.ui.theme.WinoTypography
import kotlinx.coroutines.launch

// Figma frame: 1.7 Balance currency (956:4814)
// MCP source: get_screenshot, get_design_context, get_variable_defs for node 956:4814

data class CurrencyOption(
    val code: String,
    val name: String,
    @DrawableRes val flagRes: Int
)

private val currencies = listOf(
    CurrencyOption("USD", "United States Dollar \u2022 $", R.drawable.flag_us),
    CurrencyOption("THB", "Thai Baht", R.drawable.flag_th),
    CurrencyOption("AED", "Arab Emirates Dirhams", R.drawable.flag_ae),
    CurrencyOption("RUB", "Russian Ruble \u2022 \u20BD", R.drawable.flag_ru),
    CurrencyOption("HKD", "Hong Kong Dollar", R.drawable.flag_hk)
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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.bgCanvas)
            .statusBarsPadding()
    ) {
        // Title + subtitle
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    start = WinoSpacing.LG,
                    end = WinoSpacing.LG,
                    top = WinoSpacing.MD,
                    bottom = WinoSpacing.MD
                ),
            verticalArrangement = Arrangement.spacedBy(WinoSpacing.XXS)
        ) {
            Text(
                text = "Select balance currency",
                style = WinoTypography.h1Medium,
                color = colors.textPrimary
            )

            Text(
                text = "Your balance will be displayed in this currency",
                style = WinoTypography.body,
                color = colors.textSecondary
            )
        }

        // Currency list — scrollable
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = WinoSpacing.LG)
        ) {
            currencies.forEach { currency ->
                CurrencyRow(
                    currency = currency,
                    isSelected = currency.code == selectedCurrency,
                    onClick = { selectedCurrency = currency.code }
                )
            }
        }

        // Bottom section with helper and continue button
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(WinoSpacing.LG),
            verticalArrangement = Arrangement.spacedBy(WinoSpacing.SM)
        ) {
            // Helper text with link
            val helperText = buildAnnotatedString {
                append("Learn more about currencies at ")
                pushStringAnnotation(tag = "URL", annotation = "https://winobank.com/currencies")
                withStyle(
                    style = SpanStyle(
                        color = colors.brandPrimary,
                        textDecoration = TextDecoration.Underline
                    )
                ) {
                    append("winobank.com")
                }
                pop()
            }

            ClickableText(
                text = helperText,
                style = WinoTypography.small.copy(color = colors.textSecondary),
                onClick = { offset ->
                    helperText.getStringAnnotations(tag = "URL", start = offset, end = offset)
                        .firstOrNull()?.let {
                            uriHandler.openUri(it.item)
                        }
                }
            )

            // Continue button
            WinoButton(
                text = "Continue",
                onClick = { saveAndContinue() },
                modifier = Modifier.fillMaxWidth(),
                variant = WinoButtonVariant.Primary,
                size = WinoButtonSize.Large
            )
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

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(WinoRadius.MD))
            .then(
                if (isSelected) {
                    Modifier
                        .background(colors.bgAccentSoft)
                        .border(2.dp, colors.brandPrimary, RoundedCornerShape(WinoRadius.MD))
                } else {
                    Modifier
                }
            )
            .clickable { onClick() }
            .padding(WinoSpacing.MD),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(WinoSpacing.MD)
    ) {
        // Flag: 36x36dp, circular clip
        Image(
            painter = painterResource(id = currency.flagRes),
            contentDescription = "${currency.code} flag",
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape),
            contentScale = ContentScale.Crop
        )

        // Currency name
        Text(
            text = currency.name,
            style = WinoTypography.h3Medium,
            color = colors.textPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )

        // Currency code or checkmark
        if (isSelected) {
            PhosphorIcons.CheckCircle(size = 24.dp, color = colors.brandPrimary)
        } else {
            Text(
                text = currency.code,
                style = WinoTypography.bodyMedium,
                color = colors.textTertiary
            )
        }
    }
}
