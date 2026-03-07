package com.winopay.ui.screens.settings

import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.winopay.ui.components.PhosphorIcons
import com.winopay.ui.components.WinoSecondaryButton
import com.winopay.ui.theme.WinoRadius
import com.winopay.ui.theme.WinoSpacing
import com.winopay.ui.theme.WinoTheme
import com.winopay.ui.theme.WinoTypography

// Figma frame: 4.6 Settings – App info (973:5194)

@Composable
fun SettingsAppInfoScreen(
    onBack: () -> Unit
) {
    val colors = WinoTheme.colors

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
                text = "App info",
                style = WinoTypography.h1Medium,
                color = colors.textPrimary
            )
            Text(
                text = "Blah blah",
                style = WinoTypography.body,
                color = colors.textSecondary
            )
        }

        // Info rows: px=LG(24dp)
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = WinoSpacing.LG)
        ) {
            // Version
            AppInfoRow(
                icon = {
                    PhosphorIcons.Info(
                        size = 20.dp,
                        color = colors.brandPrimary
                    )
                },
                title = "Version",
                value = "0.1"
            )

            // Connection — WifiMedium icon per Figma 973:5300
            AppInfoRow(
                icon = {
                    PhosphorIcons.WifiMedium(
                        size = 20.dp,
                        color = colors.brandPrimary
                    )
                },
                title = "Connection",
                value = "Good"
            )

            // Device info
            AppInfoRow(
                icon = {
                    PhosphorIcons.DeviceMobile(
                        size = 20.dp,
                        color = colors.brandPrimary
                    )
                },
                title = "Device info",
                value = "Phone Name"
            )

            // "Legal" divider
            SectionDivider(title = "Legal")

            // Term of Use — File icon per Figma 973:5304
            AppInfoRow(
                icon = {
                    PhosphorIcons.File(
                        size = 20.dp,
                        color = colors.brandPrimary
                    )
                },
                title = "Term of Use"
            )

            // Privacy policy — File icon per Figma 973:5423
            AppInfoRow(
                icon = {
                    PhosphorIcons.File(
                        size = 20.dp,
                        color = colors.brandPrimary
                    )
                },
                title = "Privacy policy"
            )

            // Risk Disclosure — File icon per Figma 1131:1317
            AppInfoRow(
                icon = {
                    PhosphorIcons.File(
                        size = 20.dp,
                        color = colors.brandPrimary
                    )
                },
                title = "Risk Disclosure"
            )
        }

        // Bottom: helper + "Close" button
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = WinoSpacing.LG)
        ) {
            SettingsHelper(
                text = "Blah blah about crypto assets",
                link = "Learn more"
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = WinoSpacing.SM)
            ) {
                WinoSecondaryButton(
                    text = "Close",
                    onClick = onBack
                )
            }
        }
    }
}

@Composable
private fun AppInfoRow(
    icon: @Composable () -> Unit,
    title: String,
    value: String? = null
) {
    val colors = WinoTheme.colors

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = WinoSpacing.MD),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(WinoSpacing.MD)
    ) {
        // Icon: 36dp, bgAccentSoft, rounded-XL
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(WinoRadius.XL))
                .background(colors.bgAccentSoft),
            contentAlignment = Alignment.Center
        ) {
            icon()
        }

        // Title: H3 Medium, textPrimary, flex-1
        Text(
            text = title,
            style = WinoTypography.h3Medium,
            color = colors.textPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )

        // Value if present
        if (value != null) {
            Text(
                text = value,
                style = WinoTypography.bodyMedium,
                color = colors.textTertiary
            )
        }
    }
}

@Composable
private fun SectionDivider(title: String) {
    val colors = WinoTheme.colors

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = WinoSpacing.MD),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(WinoSpacing.SM)
    ) {
        Text(
            text = title,
            style = WinoTypography.small,
            color = colors.textTertiary
        )
        Box(
            modifier = Modifier
                .weight(1f)
                .size(1.dp)
                .background(colors.borderSubtle)
        )
    }
}
