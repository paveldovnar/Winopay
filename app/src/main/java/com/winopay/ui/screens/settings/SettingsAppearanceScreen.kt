package com.winopay.ui.screens.settings

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
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.winopay.WinoPayApplication
import com.winopay.ui.components.PhosphorIcons
import com.winopay.ui.components.WinoPrimaryButton
import com.winopay.ui.theme.WinoRadius
import com.winopay.ui.theme.WinoSpacing
import com.winopay.ui.theme.WinoTheme
import com.winopay.ui.theme.WinoTypography
import kotlinx.coroutines.launch

// Figma frame: 4.4 Appearence (973:4650)

@Composable
fun SettingsAppearanceScreen(
    onBack: () -> Unit
) {
    val colors = WinoTheme.colors
    val app = WinoPayApplication.instance
    val scope = rememberCoroutineScope()

    val savedTheme by app.dataStoreManager.themeMode.collectAsState(initial = "system")
    var selectedTheme by remember { mutableStateOf(savedTheme) }

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
                text = "Appearence",
                style = WinoTypography.h1Medium,
                color = colors.textPrimary
            )
            Text(
                text = "Blah blah",
                style = WinoTypography.body,
                color = colors.textSecondary
            )
        }

        // Options list: px=LG(24dp)
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = WinoSpacing.LG)
        ) {
            // "Same as system" — SunHorizon icon per Figma 973:4781
            AppearanceRow(
                icon = {
                    PhosphorIcons.SunHorizon(
                        size = 20.dp,
                        color = colors.brandPrimary
                    )
                },
                title = "Same as system",
                isSelected = selectedTheme == "system",
                onClick = { selectedTheme = "system" }
            )

            // "Light"
            AppearanceRow(
                icon = {
                    PhosphorIcons.Sun(
                        size = 20.dp,
                        color = colors.brandPrimary
                    )
                },
                title = "Light",
                isSelected = selectedTheme == "light",
                onClick = { selectedTheme = "light" }
            )

            // "Dark"
            AppearanceRow(
                icon = {
                    PhosphorIcons.Moon(
                        size = 20.dp,
                        color = colors.brandPrimary
                    )
                },
                title = "Dark",
                isSelected = selectedTheme == "dark",
                onClick = { selectedTheme = "dark" }
            )
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
                        app.dataStoreManager.setThemeMode(selectedTheme)
                        onBack()
                    }
                }
            )
        }
    }
}

@Composable
private fun AppearanceRow(
    icon: @Composable () -> Unit,
    title: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val colors = WinoTheme.colors

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
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

        // Checkmark if selected
        if (isSelected) {
            PhosphorIcons.Check(
                size = 20.dp,
                color = colors.textPrimary
            )
        }
    }
}
