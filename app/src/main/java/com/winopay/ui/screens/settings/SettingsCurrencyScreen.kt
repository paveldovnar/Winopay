package com.winopay.ui.screens.settings

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.winopay.R
import com.winopay.WinoPayApplication
import com.winopay.ui.components.PhosphorIcons
import com.winopay.ui.components.WinoPrimaryButton
import com.winopay.ui.theme.WinoSpacing
import com.winopay.ui.theme.WinoTheme
import com.winopay.ui.theme.WinoTypography
import kotlinx.coroutines.launch

// Figma frame: 4.2 Select balance currency (967:6210)

private data class CurrencyOption(
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
fun SettingsCurrencyScreen(
    onBack: () -> Unit
) {
    val colors = WinoTheme.colors
    val app = WinoPayApplication.instance
    val scope = rememberCoroutineScope()

    val savedCurrency by app.dataStoreManager.currency.collectAsState(initial = "USD")
    var selectedCurrency by remember { mutableStateOf(savedCurrency) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.bgCanvas)
            .statusBarsPadding()
    ) {
        // Header: px=LG(24dp), py=MD(16dp), gap=XXS(4dp)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = WinoSpacing.LG,
                    vertical = WinoSpacing.MD
                ),
            verticalArrangement = Arrangement.spacedBy(WinoSpacing.XXS)
        ) {
            Text(
                text = "Select balance currency",
                style = WinoTypography.h1Medium,
                color = colors.textPrimary
            )
            Text(
                text = "Blah blah",
                style = WinoTypography.body,
                color = colors.textSecondary
            )
        }

        // Currency list: scrollable, px=LG(24dp)
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = WinoSpacing.LG)
        ) {
            currencies.forEach { currency ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { selectedCurrency = currency.code }
                        .padding(vertical = WinoSpacing.MD),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(WinoSpacing.MD)
                ) {
                    // Flag: 36dp circle
                    Image(
                        painter = painterResource(id = currency.flagRes),
                        contentDescription = "${currency.code} flag",
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape),
                        contentScale = ContentScale.Crop
                    )

                    // Name: H3 Medium, textPrimary, flex-1
                    Text(
                        text = currency.name,
                        style = WinoTypography.h3Medium,
                        color = colors.textPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )

                    // Right: checkmark if selected, else currency code
                    if (selectedCurrency == currency.code) {
                        PhosphorIcons.Check(
                            size = 20.dp,
                            color = colors.textPrimary
                        )
                    } else {
                        Text(
                            text = currency.code,
                            style = WinoTypography.bodyMedium,
                            color = colors.textTertiary
                        )
                    }
                }
            }
        }

        // Bottom: "Save changes" button, px=LG(24dp), py=SM(12dp)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(
                    horizontal = WinoSpacing.LG,
                    vertical = WinoSpacing.SM
                )
        ) {
            WinoPrimaryButton(
                text = "Save changes",
                onClick = {
                    scope.launch {
                        app.dataStoreManager.setCurrency(selectedCurrency)
                        onBack()
                    }
                }
            )
        }
    }
}
