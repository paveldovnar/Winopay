package com.winopay.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import com.winopay.ui.theme.WinoRadius
import com.winopay.ui.theme.WinoSpacing
import com.winopay.ui.theme.WinoTheme
import com.winopay.ui.theme.WinoTypography

sealed class NumpadKey {
    data class Digit(val value: String) : NumpadKey()
    data object Decimal : NumpadKey()
    data object Backspace : NumpadKey()
}

/**
 * Numpad button component matching Figma 1:1
 * Figma: 110x64dp, transparent bg, no border
 * Text: H1 Medium (28sp), text/primary
 */
@Composable
fun NumpadButton(
    content: @Composable () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val haptic = LocalHapticFeedback.current
    val colors = WinoTheme.colors

    // Figma: h=64dp, transparent bg, pressed shows bgSurfaceAlt
    Box(
        modifier = modifier
            .height(64.dp)
            .clip(RoundedCornerShape(WinoRadius.MD))
            .background(if (isPressed) colors.bgSurfaceAlt else Color.Transparent)
            .clickable(
                interactionSource = interactionSource,
                indication = ripple(color = colors.brandPrimary)
            ) {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                onClick()
            },
        contentAlignment = Alignment.Center
    ) {
        content()
    }
}

/**
 * Numpad component matching Figma 1:1
 * Figma node: 780:5536 (inside 793:19156)
 * Grid: 3 columns x 4 rows, gap=SM(12dp)
 * Button text: H1 Medium (28sp), text/primary
 */
@Composable
fun Numpad(
    onKeyPress: (NumpadKey) -> Unit,
    modifier: Modifier = Modifier,
    showDecimal: Boolean = true
) {
    val colors = WinoTheme.colors

    // Figma: gap=spacing/sm(12dp) between rows
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(WinoSpacing.SM)
    ) {
        // Row 1: 1, 2, 3
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(WinoSpacing.SM)
        ) {
            for (digit in listOf("1", "2", "3")) {
                NumpadButton(
                    content = {
                        Text(
                            text = digit,
                            style = WinoTypography.h1Medium,
                            color = colors.textPrimary
                        )
                    },
                    onClick = { onKeyPress(NumpadKey.Digit(digit)) },
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Row 2: 4, 5, 6
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(WinoSpacing.SM)
        ) {
            for (digit in listOf("4", "5", "6")) {
                NumpadButton(
                    content = {
                        Text(
                            text = digit,
                            style = WinoTypography.h1Medium,
                            color = colors.textPrimary
                        )
                    },
                    onClick = { onKeyPress(NumpadKey.Digit(digit)) },
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Row 3: 7, 8, 9
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(WinoSpacing.SM)
        ) {
            for (digit in listOf("7", "8", "9")) {
                NumpadButton(
                    content = {
                        Text(
                            text = digit,
                            style = WinoTypography.h1Medium,
                            color = colors.textPrimary
                        )
                    },
                    onClick = { onKeyPress(NumpadKey.Digit(digit)) },
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Row 4: ., 0, backspace
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(WinoSpacing.SM)
        ) {
            // Decimal button
            NumpadButton(
                content = {
                    if (showDecimal) {
                        Text(
                            text = ".",
                            style = WinoTypography.h1Medium,
                            color = colors.textPrimary
                        )
                    }
                },
                onClick = { if (showDecimal) onKeyPress(NumpadKey.Decimal) },
                modifier = Modifier.weight(1f)
            )

            // Zero
            NumpadButton(
                content = {
                    Text(
                        text = "0",
                        style = WinoTypography.h1Medium,
                        color = colors.textPrimary
                    )
                },
                onClick = { onKeyPress(NumpadKey.Digit("0")) },
                modifier = Modifier.weight(1f)
            )

            // Backspace — Figma: 32dp icon
            NumpadButton(
                content = {
                    PhosphorIcons.Backspace(
                        size = 32.dp,
                        color = colors.textPrimary
                    )
                },
                onClick = { onKeyPress(NumpadKey.Backspace) },
                modifier = Modifier.weight(1f)
            )
        }
    }
}

// Helper function to handle numpad input for amount
fun handleNumpadInput(
    currentValue: String,
    key: NumpadKey,
    maxDecimalPlaces: Int = 2,
    maxValue: Double = 999999.99
): String {
    return when (key) {
        is NumpadKey.Digit -> {
            val newValue = if (currentValue == "0" && key.value != "0") {
                key.value
            } else {
                currentValue + key.value
            }

            // Check decimal places
            val decimalIndex = newValue.indexOf('.')
            if (decimalIndex != -1 && newValue.length - decimalIndex - 1 > maxDecimalPlaces) {
                return currentValue
            }

            // Check max value
            val numericValue = newValue.toDoubleOrNull() ?: 0.0
            if (numericValue > maxValue) {
                return currentValue
            }

            newValue
        }
        is NumpadKey.Decimal -> {
            if (currentValue.contains('.')) {
                currentValue
            } else {
                "$currentValue."
            }
        }
        is NumpadKey.Backspace -> {
            if (currentValue.length <= 1) {
                "0"
            } else {
                currentValue.dropLast(1)
            }
        }
    }
}

// Format amount for display
fun formatAmountDisplay(value: String): String {
    val numericValue = value.toDoubleOrNull() ?: 0.0
    return if (value.contains('.')) {
        val parts = value.split('.')
        val intPart = parts[0].toLongOrNull()?.let {
            String.format("%,d", it)
        } ?: parts[0]
        val decPart = if (parts.size > 1) parts[1] else ""
        "$intPart.$decPart"
    } else {
        val intValue = value.toLongOrNull()
        if (intValue != null) {
            String.format("%,d", intValue)
        } else {
            value
        }
    }
}
