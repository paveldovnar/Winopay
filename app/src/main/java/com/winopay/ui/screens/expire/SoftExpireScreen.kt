package com.winopay.ui.screens.expire

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.winopay.ui.components.PhosphorIcons
import com.winopay.ui.components.WinoButton
import com.winopay.ui.components.WinoButtonSize
import com.winopay.ui.components.WinoButtonVariant
import com.winopay.ui.theme.WinoRadius
import com.winopay.ui.theme.WinoSpacing
import com.winopay.ui.theme.WinoTheme
import com.winopay.ui.theme.WinoTypography

/**
 * Full-screen soft-expire state.
 *
 * PRODUCT BEHAVIOR:
 * - Shown when invoice timeout is reached
 * - Detection CONTINUES in background
 * - Merchant must choose:
 *   - "Wait 5 more minutes" → extends detection, returns to QR
 *   - "Revoke invoice" → marks as EXPIRED, stops detection
 *
 * STRICT BLOCKING:
 * - Cannot go back to Dashboard (BackHandler in PosFlowHost)
 * - Must explicitly choose Wait or Revoke
 */
@Composable
fun SoftExpireScreen(
    invoiceId: String,
    amount: Double,
    elapsedSeconds: Int,
    onWaitLonger: () -> Unit,
    onRevoke: () -> Unit
) {
    val colors = WinoTheme.colors
    val elapsedMinutes = elapsedSeconds / 60

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.bgCanvas)
            .statusBarsPadding()
    ) {
        // Header with invoice info
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
                text = "Payment timeout",
                style = WinoTypography.h1Medium,
                color = colors.textPrimary
            )
            Text(
                text = "Invoice #${invoiceId.take(8)}",
                style = WinoTypography.body,
                color = colors.textSecondary
            )
        }

        // Content area
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(horizontal = WinoSpacing.LG),
            verticalArrangement = Arrangement.spacedBy(WinoSpacing.MD)
        ) {
            // Amount card
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(WinoRadius.XL))
                    .background(colors.bgSurface)
                    .padding(
                        horizontal = WinoSpacing.MD,
                        vertical = WinoSpacing.XL
                    ),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(WinoSpacing.XXS)
            ) {
                Text(
                    text = "${"%.2f".format(amount)} USD",
                    style = WinoTypography.display,
                    color = colors.textPrimary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
                Text(
                    text = "Elapsed: $elapsedMinutes ${if (elapsedMinutes == 1) "minute" else "minutes"}",
                    style = WinoTypography.body,
                    color = colors.textSecondary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // Status card with icon and message
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .clip(RoundedCornerShape(WinoRadius.XL))
                    .background(colors.bgSurface)
                    .padding(
                        horizontal = WinoSpacing.MD,
                        vertical = WinoSpacing.XL
                    ),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(WinoSpacing.LG, Alignment.CenterVertically)
            ) {
                // Clock icon
                Box(
                    modifier = Modifier
                        .size(WinoSpacing.XXXXL)
                        .clip(RoundedCornerShape(WinoRadius.XL))
                        .background(colors.stateWarningSoft),
                    contentAlignment = Alignment.Center
                ) {
                    PhosphorIcons.Clock(
                        size = WinoSpacing.XL,
                        color = colors.stateWarning
                    )
                }

                // Message
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(WinoSpacing.XXS)
                ) {
                    Text(
                        text = "Time expired",
                        style = WinoTypography.h1Medium,
                        color = colors.textPrimary,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text(
                        text = "No payment detected after $elapsedMinutes ${if (elapsedMinutes == 1) "minute" else "minutes"}.",
                        style = WinoTypography.body,
                        color = colors.textSecondary,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text(
                        text = "The customer may still be completing the transaction. Would you like to wait longer?",
                        style = WinoTypography.body,
                        color = colors.textTertiary,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = WinoSpacing.SM)
                    )
                }
            }
        }

        // Bottom section with buttons
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Helper text
            Text(
                text = "Detection continues in background",
                style = WinoTypography.small,
                color = colors.textSecondary,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        horizontal = WinoSpacing.XL,
                        vertical = WinoSpacing.XS
                    )
            )

            // Action buttons
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        horizontal = WinoSpacing.LG,
                        vertical = WinoSpacing.SM
                    ),
                verticalArrangement = Arrangement.spacedBy(WinoSpacing.XS)
            ) {
                // Primary action - wait longer
                WinoButton(
                    text = "Wait 5 more minutes",
                    onClick = onWaitLonger,
                    modifier = Modifier.fillMaxWidth(),
                    variant = WinoButtonVariant.Primary,
                    size = WinoButtonSize.Large
                )

                // Destructive action - revoke
                WinoButton(
                    text = "Revoke invoice",
                    onClick = onRevoke,
                    modifier = Modifier.fillMaxWidth(),
                    variant = WinoButtonVariant.Destructive,
                    size = WinoButtonSize.Large
                )
            }
        }
    }
}
