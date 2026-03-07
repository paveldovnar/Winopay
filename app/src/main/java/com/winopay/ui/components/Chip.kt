package com.winopay.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.winopay.ui.theme.WinoRadius
import com.winopay.ui.theme.WinoSpacing
import com.winopay.ui.theme.WinoTheme
import com.winopay.ui.theme.WinoTypography

enum class ChipVariant {
    Default,
    Success,
    Warning,
    Error,
    Info,
    Brand
}

@Composable
fun WinoChip(
    text: String,
    modifier: Modifier = Modifier,
    variant: ChipVariant = ChipVariant.Default,
    onClick: (() -> Unit)? = null,
    leadingIcon: (@Composable () -> Unit)? = null,
    trailingIcon: (@Composable () -> Unit)? = null
) {
    val colors = WinoTheme.colors

    val (backgroundColor, textColor, borderColor) = when (variant) {
        ChipVariant.Default -> Triple(
            colors.bgSurfaceAlt,
            colors.textSecondary,
            colors.borderDefault
        )
        ChipVariant.Success -> Triple(
            colors.stateSuccessSoft,
            colors.stateSuccess,
            colors.stateSuccess.copy(alpha = 0.3f)
        )
        ChipVariant.Warning -> Triple(
            colors.stateWarningSoft,
            colors.stateWarning,
            colors.stateWarning.copy(alpha = 0.3f)
        )
        ChipVariant.Error -> Triple(
            colors.stateErrorSoft,
            colors.stateError,
            colors.stateError.copy(alpha = 0.3f)
        )
        ChipVariant.Info -> Triple(
            colors.stateInfoSoft,
            colors.stateInfo,
            colors.stateInfo.copy(alpha = 0.3f)
        )
        ChipVariant.Brand -> Triple(
            colors.bgAccentSoft,
            colors.brandPrimary,
            colors.brandPrimary.copy(alpha = 0.3f)
        )
    }

    val shape = RoundedCornerShape(WinoRadius.Full)

    Box(
        modifier = modifier
            .clip(shape)
            .background(backgroundColor)
            .border(width = 0.5.dp, color = borderColor, shape = shape)
            .then(
                if (onClick != null) {
                    Modifier.clickable { onClick() }
                } else {
                    Modifier
                }
            )
            .padding(horizontal = WinoSpacing.SM, vertical = WinoSpacing.XXS)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            leadingIcon?.let {
                it()
                Spacer(modifier = Modifier.width(WinoSpacing.XXS))
            }
            Text(
                text = text,
                style = WinoTypography.microMedium,
                color = textColor
            )
            trailingIcon?.let {
                Spacer(modifier = Modifier.width(WinoSpacing.XXS))
                it()
            }
        }
    }
}

@Composable
fun StatusChip(
    status: TransactionStatus,
    modifier: Modifier = Modifier
) {
    val (text, variant) = when (status) {
        TransactionStatus.Success -> "Completed" to ChipVariant.Success
        TransactionStatus.Pending -> "Pending" to ChipVariant.Warning
        TransactionStatus.Failed -> "Failed" to ChipVariant.Error
    }

    WinoChip(
        text = text,
        variant = variant,
        modifier = modifier
    )
}

enum class TransactionStatus {
    Success,
    Pending,
    Failed
}

/**
 * Quick amount chip for POS numpad (+10, +50, +100)
 * Figma node: 786:7274 (Chip component in numpad)
 * bg: bgSurface (#f7f7fb)
 * rounded: XL (36dp)
 * px: LG (24dp), py: SM (12dp)
 * text: bodyMedium, textPrimary
 */
@Composable
fun QuickAmountChip(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = WinoTheme.colors

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(WinoRadius.XL))
            .background(colors.bgSurface)
            .clickable(onClick = onClick)
            .padding(
                horizontal = WinoSpacing.LG,
                vertical = WinoSpacing.SM
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            style = WinoTypography.bodyMedium,
            color = colors.textPrimary
        )
    }
}
