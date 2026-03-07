package com.winopay.ui.screens.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.winopay.ui.components.PhosphorIcons
import com.winopay.ui.theme.WinoSpacing
import com.winopay.ui.theme.WinoTheme
import com.winopay.ui.theme.WinoTypography
import kotlinx.coroutines.delay

// Figma frame: 1.8.2 We are all set (973:6534)
// MCP source: get_screenshot, get_design_context for node 973:6534

@Composable
fun AllSetScreen(
    onStart: () -> Unit
) {
    val colors = WinoTheme.colors

    // Auto-navigate after a short delay
    LaunchedEffect(Unit) {
        delay(2500)
        onStart()
    }

    // Root: bg=brand/primary, items-center, justify-center, gap=MD(16dp)
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.brandPrimary)
            .safeDrawingPadding(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(WinoSpacing.MD, Alignment.CenterVertically)
    ) {
        // "Check" icon (973:6545): 96x96dp, white checkmark
        PhosphorIcons.Check(
            size = 96.dp,
            color = colors.textOnBrand
        )

        // "Text" frame (973:6541)
        // gap=XXS(4dp), px=LG(24dp), py=SM(12dp), text-center, text/onbrand
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = WinoSpacing.LG,
                    vertical = WinoSpacing.SM
                ),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(WinoSpacing.XXS)
        ) {
            // "title" (973:6542) — Display (36/44 SemiBold -3%), text/onbrand
            Text(
                text = "We are all set",
                style = WinoTypography.display,
                color = colors.textOnBrand,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )

            // "subtitle" (973:6543) — Body, text/onbrand
            Text(
                text = "With no banks. With no custody.",
                style = WinoTypography.body,
                color = colors.textOnBrand,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}
