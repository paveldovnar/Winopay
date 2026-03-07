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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.winopay.R
import com.winopay.WinoPayApplication
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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.bgCanvas)
            .statusBarsPadding()
    ) {
        // "Text" frame — title + subtitle
        // padding: top=MD(16dp), bottom=MD(16dp), horizontal=LG(24dp), gap=XXS(4dp)
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
            // "title" — H1 Medium, text/primary
            Text(
                text = "Select balance currency",
                style = WinoTypography.h1Medium,
                color = colors.textPrimary
            )

            // "subtitle" — Body, text/secondary
            Text(
                text = "Blah blah",
                style = WinoTypography.body,
                color = colors.textSecondary
            )
        }

        // Currency list — scrollable, px=LG(24dp)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding()
                .padding(horizontal = WinoSpacing.LG)
        ) {
            currencies.forEach { currency ->
                CurrencyRow(
                    currency = currency,
                    onClick = {
                        scope.launch {
                            app.dataStoreManager.setCurrency(currency.code)
                            onContinue()
                        }
                    }
                )
            }
        }
    }
}

@Composable
private fun CurrencyRow(
    currency: CurrencyOption,
    onClick: () -> Unit
) {
    val colors = WinoTheme.colors

    // Row: py=MD(16dp), gap=MD(16dp), items-center, clickable
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = WinoSpacing.MD),
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

        // Currency name: H3 Medium, text/primary, flex-1, single line, ellipsis
        Text(
            text = currency.name,
            style = WinoTypography.h3Medium,
            color = colors.textPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )

        // Currency code: bodyMedium, text/tertiary
        Text(
            text = currency.code,
            style = WinoTypography.bodyMedium,
            color = colors.textTertiary
        )
    }
}
