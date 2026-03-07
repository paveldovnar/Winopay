package com.winopay.ui.screens.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.winopay.WinoPayApplication
import com.winopay.ui.components.PhosphorIcons
import com.winopay.ui.components.WinoButton
import com.winopay.ui.components.WinoButtonSize
import com.winopay.ui.components.WinoButtonVariant
import com.winopay.ui.components.WinoDestructiveButton
import com.winopay.ui.components.WinoPrimaryButton
import com.winopay.ui.theme.WinoRadius
import com.winopay.ui.theme.WinoSpacing
import com.winopay.ui.theme.WinoTheme
import com.winopay.ui.theme.WinoTypography

// Figma frame: 4.7.2 Settings - Bussines info (973:6165)

@Composable
fun SettingsBusinessInfoScreen(
    onBack: () -> Unit,
    onLogout: () -> Unit
) {
    val colors = WinoTheme.colors
    val app = WinoPayApplication.instance
    val businessIdentity by app.dataStoreManager.businessIdentity.collectAsState(initial = null)

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
                text = "Business profile",
                style = WinoTypography.h1Medium,
                color = colors.textPrimary
            )
            Text(
                text = "Blah blah",
                style = WinoTypography.body,
                color = colors.textSecondary
            )
        }

        // Content
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = WinoSpacing.LG)
        ) {
            // Editable name field: bgSurface, rounded-XL, p=MD
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(WinoRadius.XL))
                    .background(colors.bgSurface)
                    .padding(
                        horizontal = WinoSpacing.MD,
                        vertical = WinoSpacing.SM
                    )
            ) {
                Text(
                    text = businessIdentity?.name ?: "Test Name",
                    style = WinoTypography.body,
                    color = colors.textPrimary
                )
            }

            Spacer(modifier = Modifier.size(WinoSpacing.SM))

            // Change Logo field: bgSurface, rounded-XL, p=MD
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(WinoRadius.XL))
                    .background(colors.bgSurface)
                    .clickable { /* Open image picker */ }
                    .padding(
                        horizontal = WinoSpacing.MD,
                        vertical = WinoSpacing.SM
                    ),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Change Logo",
                    style = WinoTypography.body,
                    color = colors.textPrimary,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = "Select",
                    style = WinoTypography.bodyMedium,
                    color = colors.brandPrimary
                )
            }

            // "Wallet address" divider
            SectionDivider(title = "Wallet address")

            // Business card: bgSurface, rounded-XL, p=MD
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(WinoRadius.XL))
                    .background(colors.bgSurface)
                    .padding(WinoSpacing.MD),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(WinoSpacing.MD)
            ) {
                // Logo: 40dp, bgAccentSoft, rounded-XL
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(WinoRadius.XL))
                        .background(colors.bgAccentSoft),
                    contentAlignment = Alignment.Center
                ) {
                    PhosphorIcons.Storefront(
                        size = 20.dp,
                        color = colors.brandPrimary
                    )
                }

                // Name + Wallet address
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = businessIdentity?.name ?: "Test Name",
                            style = WinoTypography.h3Medium,
                            color = colors.textPrimary
                        )
                        Spacer(modifier = Modifier.width(WinoSpacing.XXS))
                        PhosphorIcons.SealCheck(
                            size = 16.dp,
                            color = colors.brandPrimary
                        )
                    }
                    Text(
                        text = "Wallet address",
                        style = WinoTypography.small,
                        color = colors.textSecondary
                    )
                }

                // "Edit" button
                WinoButton(
                    text = "Edit",
                    onClick = { /* Edit profile */ },
                    variant = WinoButtonVariant.Primary,
                    size = WinoButtonSize.Small
                )
            }

            // Helper text
            SettingsHelper(
                text = "Your business profile is stored locally on this device.",
                link = "Learn more"
            )
        }

        // Bottom: "Go back" + "Log out" buttons
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(
                    horizontal = WinoSpacing.LG,
                    vertical = WinoSpacing.SM
                ),
            verticalArrangement = Arrangement.spacedBy(WinoSpacing.SM)
        ) {
            WinoPrimaryButton(
                text = "Go back",
                onClick = onBack
            )
            WinoDestructiveButton(
                text = "Log out",
                onClick = onLogout
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
