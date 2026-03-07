package com.winopay.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.winopay.ui.theme.WinoRadius
import com.winopay.ui.theme.WinoSpacing
import com.winopay.ui.theme.WinoTheme
import com.winopay.ui.theme.WinoTypography

/**
 * Statistics card component matching Figma 1:1
 * Figma nodes: 793:9249 (Today), 793:9257 (30 days average)
 *
 * Structure:
 * - bg: bgSurface, p: MD(16dp), rounded: LG(18dp), gap: XS(8dp)
 * - Text block: label (Micro, textTertiary) + value row
 * - Progress bar: h=14dp, bg: borderDefault, fill: borderBrand, rounded-XS
 */
@Composable
fun WinoStatCard(
    label: String,
    value: String,
    unit: String,
    progress: Float,
    modifier: Modifier = Modifier,
    suffix: String? = null
) {
    val colors = WinoTheme.colors

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(WinoRadius.LG))
            .background(colors.bgSurface)
            .padding(WinoSpacing.MD),
        verticalArrangement = Arrangement.spacedBy(WinoSpacing.XS)
    ) {
        // Text block: gap=0
        Column {
            // Label — Micro, textTertiary
            Text(
                text = label,
                style = WinoTypography.micro,
                color = colors.textTertiary
            )

            // Value row: gap=XS(8dp)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(WinoSpacing.XS)
            ) {
                // Value: H3 Medium (18sp) + Micro unit (12sp)
                Text(
                    text = buildAnnotatedString {
                        withStyle(
                            SpanStyle(
                                fontSize = 18.sp,
                                letterSpacing = (-0.36).sp
                            )
                        ) {
                            append("$value ")
                        }
                        withStyle(
                            SpanStyle(
                                fontSize = 12.sp,
                                letterSpacing = 0.sp
                            )
                        ) {
                            append(unit)
                        }
                    },
                    style = WinoTypography.h3Medium,
                    color = colors.textPrimary
                )

                // Suffix (e.g. "~40%") — Small, textTertiary
                if (suffix != null) {
                    Text(
                        text = suffix,
                        style = WinoTypography.small,
                        color = colors.textTertiary
                    )
                }
            }
        }

        // Progress bar: h=14dp, rounded-XS(6dp)
        // Background: borderDefault, Fill: borderBrand
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(14.dp)
                .clip(RoundedCornerShape(WinoRadius.XS))
                .background(colors.borderDefault)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(progress.coerceIn(0f, 1f))
                    .height(14.dp)
                    .clip(
                        RoundedCornerShape(
                            topStart = WinoRadius.XS,
                            bottomStart = WinoRadius.XS,
                            topEnd = if (progress >= 1f) WinoRadius.XS else 0.dp,
                            bottomEnd = if (progress >= 1f) WinoRadius.XS else 0.dp
                        )
                    )
                    .background(colors.borderBrand)
            )
        }
    }
}
