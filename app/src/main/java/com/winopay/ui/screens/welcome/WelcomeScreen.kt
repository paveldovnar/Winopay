package com.winopay.ui.screens.welcome

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.winopay.ui.components.WinoPrimaryButton
import com.winopay.ui.theme.WinoSpacing
import com.winopay.ui.theme.WinoTheme
import com.winopay.ui.theme.WinoTypography

@Composable
fun WelcomeScreen(
    onGetStarted: () -> Unit
) {
    val colors = WinoTheme.colors

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.bgCanvas)
    ) {
        // "bottom" frame — pinned to bottom of parent
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .navigationBarsPadding(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // "Text" frame — vertical auto-layout, gap=4dp (XXS),
            // padding: top=12 bottom=12 left=24 right=24
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        start = WinoSpacing.LG,   // 24dp
                        end = WinoSpacing.LG,      // 24dp
                        top = WinoSpacing.SM,      // 12dp
                        bottom = WinoSpacing.SM    // 12dp
                    ),
                verticalArrangement = Arrangement.spacedBy(WinoSpacing.XXS), // gap: 4dp
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // "title" — "Wino Pay"
                // WinoUI/Display: Albert Sans, SemiBold 600, 36/44, -3% ls
                // Color: text/primary
                Text(
                    text = "Wino Pay",
                    style = WinoTypography.display,
                    color = colors.textPrimary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )

                // "subtitle" — "With no banks. With no custody."
                // WinoUI/Body: Albert Sans, Regular 400, 16/24, -2% ls
                // Color: text/secondary
                Text(
                    text = "With no banks. With no custody.",
                    style = WinoTypography.body,
                    color = colors.textSecondary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // "Bottom Button Layout" frame — vertical auto-layout,
            // padding: top=20 bottom=20 left=24 right=24
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        start = WinoSpacing.LG,   // 24dp
                        end = WinoSpacing.LG,      // 24dp
                        top = 20.dp,
                        bottom = 20.dp
                    ),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // "button" instance — Primary button, full width
                // bg: #7C5CFF (brandPrimary), radius: 36dp, height: 56dp
                // text: "Let's get started", WinoUI/Label (Medium 500, 16/24, -2% ls), white
                WinoPrimaryButton(
                    text = "Let's get started",
                    onClick = onGetStarted
                )
            }

            // "helper" frame — vertical auto-layout,
            // padding: left=32 right=32
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        start = WinoSpacing.XL,   // 32dp
                        end = WinoSpacing.XL       // 32dp
                    ),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // "content" > "text" — "By continuing, you agree to our Terms & Privacy."
                // WinoUI/Small: Albert Sans, Regular 400, 14/20, 0 ls
                // Color: text/tertiary
                Text(
                    text = "By continuing, you agree to our Terms & Privacy.",
                    style = WinoTypography.small,
                    color = colors.textTertiary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // Home Indicator — iOS system element (34dp)
            // On Android, handled by system navigation insets; omitted.
        }
    }
}
