package com.winopay.ui.screens.settings

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
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.winopay.R
import com.winopay.WinoPayApplication
import com.winopay.ui.components.PhosphorIcons
import com.winopay.ui.components.WinoPrimaryButton
import com.winopay.ui.theme.WinoRadius
import com.winopay.ui.theme.WinoSpacing
import com.winopay.ui.theme.WinoTheme
import com.winopay.ui.theme.WinoTypography
import com.winopay.util.LocaleManager
import kotlinx.coroutines.launch

// Figma frame: 4.3 Select language (967:6479)

@Composable
fun SettingsLanguageScreen(
    onBack: () -> Unit
) {
    val colors = WinoTheme.colors
    val app = WinoPayApplication.instance
    val scope = rememberCoroutineScope()

    // Get saved language code from DataStore (for backwards compat, convert if needed)
    val savedLanguageName by app.dataStoreManager.language.collectAsState(initial = "English")
    val savedLanguageCode = LocaleManager.legacyNameToCode(savedLanguageName)

    var selectedLanguageCode by remember { mutableStateOf(savedLanguageCode) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.bgCanvas)
            .statusBarsPadding()
    ) {
        // Header
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
                text = stringResource(R.string.select_language),
                style = WinoTypography.h1Medium,
                color = colors.textPrimary
            )
            Text(
                text = stringResource(R.string.select_language_subtitle),
                style = WinoTypography.body,
                color = colors.textSecondary
            )
        }

        // Language list
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = WinoSpacing.LG)
        ) {
            LocaleManager.availableLanguages.forEach { language ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { selectedLanguageCode = language.code }
                        .padding(vertical = WinoSpacing.MD),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(WinoSpacing.MD)
                ) {
                    // Language icon
                    androidx.compose.foundation.layout.Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(WinoRadius.XL))
                            .background(colors.bgAccentSoft),
                        contentAlignment = Alignment.Center
                    ) {
                        PhosphorIcons.Globe(
                            size = 20.dp,
                            color = colors.brandPrimary
                        )
                    }

                    // Display name with native name
                    Column(
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = language.displayName,
                            style = WinoTypography.h3Medium,
                            color = colors.textPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (language.nativeName != language.displayName) {
                            Text(
                                text = language.nativeName,
                                style = WinoTypography.small,
                                color = colors.textSecondary
                            )
                        }
                    }

                    // Checkmark if selected
                    if (selectedLanguageCode == language.code) {
                        PhosphorIcons.Check(
                            size = 20.dp,
                            color = colors.brandPrimary
                        )
                    }
                }
            }
        }

        // Bottom: Save button
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
                text = stringResource(R.string.save_changes),
                onClick = {
                    scope.launch {
                        // Save to DataStore (for persistence)
                        app.dataStoreManager.setLanguage(
                            LocaleManager.codeToDisplayName(selectedLanguageCode)
                        )
                        // Apply locale immediately
                        LocaleManager.setAppLanguage(selectedLanguageCode)
                        onBack()
                    }
                }
            )
        }
    }
}
