package com.winopay.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.winopay.ui.theme.AlbertSansFontFamily
import com.winopay.ui.theme.WinoSpacing
import com.winopay.ui.theme.WinoTheme
import com.winopay.ui.theme.WinoTypography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight

@Composable
fun AmountDisplay(
    amount: String,
    modifier: Modifier = Modifier,
    currencySymbol: String = "$",
    currencyCode: String = "USDC",
    showCurrencyCode: Boolean = true,
    textColor: Color = WinoTheme.colors.textPrimary
) {
    val displayAmount = formatAmountDisplay(amount)

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.Center
        ) {
            Text(
                text = currencySymbol,
                style = TextStyle(
                    fontFamily = AlbertSansFontFamily,
                    fontWeight = FontWeight.Medium,
                    fontSize = 32.sp,
                    lineHeight = 40.sp
                ),
                color = textColor.copy(alpha = 0.7f)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = displayAmount,
                style = TextStyle(
                    fontFamily = AlbertSansFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 48.sp,
                    lineHeight = 56.sp
                ),
                color = textColor,
                textAlign = TextAlign.Center
            )
        }

        if (showCurrencyCode) {
            Spacer(modifier = Modifier.height(WinoSpacing.XXS))
            Text(
                text = currencyCode,
                style = WinoTypography.bodyMedium,
                color = WinoTheme.colors.textMuted
            )
        }
    }
}

@Composable
fun BalanceDisplay(
    balance: Double,
    modifier: Modifier = Modifier,
    label: String = "Available Balance"
) {
    val colors = WinoTheme.colors

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = label,
            style = WinoTypography.small,
            color = colors.textMuted
        )
        Spacer(modifier = Modifier.height(WinoSpacing.XXS))
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "$",
                style = WinoTypography.h2Medium,
                color = colors.textSecondary
            )
            Spacer(modifier = Modifier.width(2.dp))
            Text(
                text = String.format("%,.2f", balance),
                style = WinoTypography.h1,
                color = colors.textPrimary
            )
        }
        Spacer(modifier = Modifier.height(WinoSpacing.XXS))
        WinoChip(
            text = "USDC",
            variant = ChipVariant.Brand
        )
    }
}

@Composable
fun LargeAmountDisplay(
    amount: String,
    modifier: Modifier = Modifier,
    status: TransactionStatus? = null
) {
    val colors = WinoTheme.colors
    val displayAmount = formatAmountDisplay(amount)

    val textColor = when (status) {
        TransactionStatus.Success -> colors.financialPositive
        TransactionStatus.Pending -> colors.financialPending
        TransactionStatus.Failed -> colors.financialNegative
        null -> colors.textPrimary
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.Center
        ) {
            Text(
                text = "$",
                style = TextStyle(
                    fontFamily = AlbertSansFontFamily,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 40.sp,
                    lineHeight = 48.sp
                ),
                color = textColor.copy(alpha = 0.7f)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = displayAmount,
                style = TextStyle(
                    fontFamily = AlbertSansFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 56.sp,
                    lineHeight = 64.sp
                ),
                color = textColor,
                textAlign = TextAlign.Center
            )
        }
        Spacer(modifier = Modifier.height(WinoSpacing.XS))
        Text(
            text = "USDC",
            style = WinoTypography.bodyMedium,
            color = colors.textMuted
        )
    }
}
