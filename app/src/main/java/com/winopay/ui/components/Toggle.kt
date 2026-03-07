package com.winopay.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.winopay.ui.theme.WinoTheme

/**
 * WinoUI Toggle (RadioSwitch) component
 * Figma: RadioSwitch component with state (on/off) and lock variants
 * Size: 52x32dp
 * Track: rounded corners (16dp radius)
 * Thumb: 24dp circle with 2dp padding from track edge
 */
@Composable
fun WinoToggle(
    checked: Boolean,
    onCheckedChange: ((Boolean) -> Unit)?,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    val colors = WinoTheme.colors

    // Track colors based on state
    val trackColor = when {
        !enabled && checked -> colors.brandPrimary.copy(alpha = 0.6f)
        !enabled -> colors.bgSurfaceAlt
        checked -> colors.brandPrimary
        else -> colors.bgSurfaceAlt
    }

    // Thumb colors
    val thumbColor = when {
        !enabled -> colors.textMuted
        checked -> colors.textOnBrand
        else -> colors.textMuted
    }

    // Track: 52x32dp, rounded-16dp
    Box(
        modifier = modifier
            .size(width = 52.dp, height = 32.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(trackColor)
            .then(
                if (onCheckedChange != null && enabled) {
                    Modifier.clickable { onCheckedChange(!checked) }
                } else {
                    Modifier
                }
            ),
        contentAlignment = if (checked) Alignment.CenterEnd else Alignment.CenterStart
    ) {
        // Thumb: 24dp circle, 4dp offset from edge
        Box(
            modifier = Modifier
                .padding(4.dp)
                .size(24.dp)
                .clip(CircleShape)
                .background(thumbColor)
        )
    }
}

/**
 * WinoUI Toggle with lock indicator (always on, not changeable)
 * Used for mandatory payment methods
 */
@Composable
fun WinoToggleLocked(
    checked: Boolean = true,
    modifier: Modifier = Modifier
) {
    WinoToggle(
        checked = checked,
        onCheckedChange = null,
        modifier = modifier,
        enabled = false
    )
}
