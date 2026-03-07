package com.winopay.ui.screens.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.winopay.ui.theme.WinoSpacing
import com.winopay.ui.theme.WinoTheme
import com.winopay.ui.theme.WinoTypography

/**
 * Helper text component for settings screens.
 */
@Composable
fun SettingsHelper(
    text: String,
    link: String? = null
) {
    val colors = WinoTheme.colors

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = WinoSpacing.XS),
        verticalArrangement = Arrangement.spacedBy(WinoSpacing.XXS)
    ) {
        Text(
            text = text,
            style = WinoTypography.small,
            color = colors.textSecondary
        )
        if (link != null) {
            Text(
                text = link,
                style = WinoTypography.smallMedium,
                color = colors.brandPrimary
            )
        }
    }
}
