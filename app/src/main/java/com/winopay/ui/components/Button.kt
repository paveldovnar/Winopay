package com.winopay.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.winopay.ui.theme.WinoComponents
import com.winopay.ui.theme.WinoTheme
import com.winopay.ui.theme.WinoTypography

enum class WinoButtonVariant {
    Primary,
    Secondary,
    Ghost,
    Destructive
}

enum class WinoButtonSize {
    Large,
    Small
}

@Composable
fun WinoButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    variant: WinoButtonVariant = WinoButtonVariant.Primary,
    size: WinoButtonSize = WinoButtonSize.Large,
    enabled: Boolean = true,
    loading: Boolean = false,
    leadingIcon: (@Composable () -> Unit)? = null,
    trailingIcon: (@Composable () -> Unit)? = null,
    textStyle: androidx.compose.ui.text.TextStyle? = null
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val colors = WinoTheme.colors
    val buttonConfig = when (size) {
        WinoButtonSize.Large -> WinoComponents.ButtonLG
        WinoButtonSize.Small -> WinoComponents.ButtonSM
    }

    val backgroundColor = when {
        !enabled -> colors.bgSurfaceAlt
        variant == WinoButtonVariant.Primary && isPressed -> colors.brandStrong
        variant == WinoButtonVariant.Primary -> colors.brandPrimary
        variant == WinoButtonVariant.Secondary && isPressed -> colors.bgSurfaceAlt
        variant == WinoButtonVariant.Secondary -> colors.bgSurface
        variant == WinoButtonVariant.Ghost && isPressed -> colors.bgSurfaceAlt
        variant == WinoButtonVariant.Ghost -> Color.Transparent
        variant == WinoButtonVariant.Destructive && isPressed -> colors.stateError
        variant == WinoButtonVariant.Destructive -> colors.stateError
        else -> colors.brandPrimary
    }

    val contentColor = when {
        !enabled -> colors.textDisabled
        variant == WinoButtonVariant.Primary -> colors.textOnBrand
        variant == WinoButtonVariant.Destructive -> colors.textOnCritical
        else -> colors.textPrimary
    }

    val borderColor = when {
        !enabled -> colors.borderSubtle
        variant == WinoButtonVariant.Secondary && isPressed -> colors.borderStrong
        variant == WinoButtonVariant.Secondary -> colors.borderDefault
        else -> Color.Transparent
    }

    val shape = RoundedCornerShape(buttonConfig.Radius)
    val contentPadding = PaddingValues(
        horizontal = buttonConfig.PaddingHorizontal,
        vertical = buttonConfig.PaddingVertical
    )

    val resolvedTextStyle = textStyle ?: when (size) {
        WinoButtonSize.Large -> WinoTypography.bodyMedium
        WinoButtonSize.Small -> WinoTypography.smallMedium
    }

    Button(
        onClick = { if (!loading) onClick() },
        modifier = modifier.height(if (size == WinoButtonSize.Large) 56.dp else 40.dp),
        enabled = enabled && !loading,
        shape = shape,
        colors = ButtonDefaults.buttonColors(
            containerColor = backgroundColor,
            contentColor = contentColor,
            disabledContainerColor = colors.bgSurfaceAlt,
            disabledContentColor = colors.textDisabled
        ),
        border = if (borderColor != Color.Transparent) {
            BorderStroke(buttonConfig.StrokeWidth, borderColor)
        } else null,
        contentPadding = contentPadding,
        interactionSource = interactionSource
    ) {
        Row(
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (loading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    color = contentColor,
                    strokeWidth = 2.dp
                )
            } else {
                leadingIcon?.let {
                    it()
                    Spacer(modifier = Modifier.width(8.dp))
                }
                Text(
                    text = text,
                    style = resolvedTextStyle,
                    color = contentColor
                )
                trailingIcon?.let {
                    Spacer(modifier = Modifier.width(8.dp))
                    it()
                }
            }
        }
    }
}

@Composable
fun WinoPrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    loading: Boolean = false,
    leadingIcon: (@Composable () -> Unit)? = null,
    trailingIcon: (@Composable () -> Unit)? = null
) {
    WinoButton(
        text = text,
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        variant = WinoButtonVariant.Primary,
        size = WinoButtonSize.Large,
        enabled = enabled,
        loading = loading,
        leadingIcon = leadingIcon,
        trailingIcon = trailingIcon
    )
}

@Composable
fun WinoSecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    loading: Boolean = false,
    leadingIcon: (@Composable () -> Unit)? = null,
    trailingIcon: (@Composable () -> Unit)? = null
) {
    WinoButton(
        text = text,
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        variant = WinoButtonVariant.Secondary,
        size = WinoButtonSize.Large,
        enabled = enabled,
        loading = loading,
        leadingIcon = leadingIcon,
        trailingIcon = trailingIcon
    )
}

@Composable
fun WinoGhostButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    loading: Boolean = false,
    leadingIcon: (@Composable () -> Unit)? = null,
    trailingIcon: (@Composable () -> Unit)? = null
) {
    WinoButton(
        text = text,
        onClick = onClick,
        modifier = modifier,
        variant = WinoButtonVariant.Ghost,
        size = WinoButtonSize.Large,
        enabled = enabled,
        loading = loading,
        leadingIcon = leadingIcon,
        trailingIcon = trailingIcon
    )
}

@Composable
fun WinoDestructiveButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    loading: Boolean = false,
    leadingIcon: (@Composable () -> Unit)? = null,
    trailingIcon: (@Composable () -> Unit)? = null
) {
    WinoButton(
        text = text,
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        variant = WinoButtonVariant.Destructive,
        size = WinoButtonSize.Large,
        enabled = enabled,
        loading = loading,
        leadingIcon = leadingIcon,
        trailingIcon = trailingIcon
    )
}
