package com.winopay.ui.theme

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

// Spacing tokens from WinoUI
object WinoSpacing {
    val Zero: Dp = 0.dp
    val XXS: Dp = 4.dp
    val XS: Dp = 8.dp
    val SM: Dp = 12.dp
    val MD: Dp = 16.dp
    val LG: Dp = 24.dp
    val XL: Dp = 32.dp
    val XXL: Dp = 40.dp
    val XXXL: Dp = 48.dp
    val XXXXL: Dp = 64.dp
}

// Radius tokens from WinoUI
object WinoRadius {
    val XS: Dp = 6.dp
    val SM: Dp = 10.dp
    val MD: Dp = 14.dp
    val LG: Dp = 18.dp
    val XL: Dp = 24.dp
    val Full: Dp = 1000.dp
}

data class WinoButtonConfig(
    val Radius: Dp,
    val PaddingVertical: Dp,
    val PaddingHorizontal: Dp,
    val StrokeWidth: Dp
)

// Component-specific dimensions
object WinoComponents {
    val ButtonLG = WinoButtonConfig(
        Radius = 36.dp,
        PaddingVertical = 16.dp,
        PaddingHorizontal = 24.dp,
        StrokeWidth = 0.75.dp
    )

    val ButtonSM = WinoButtonConfig(
        Radius = 10.dp,
        PaddingVertical = 8.dp,
        PaddingHorizontal = 12.dp,
        StrokeWidth = 0.5.dp
    )

    // Input
    object Input {
        val Radius: Dp = 14.dp
        val PaddingVertical: Dp = 12.dp
        val PaddingHorizontal: Dp = 16.dp
        val StrokeWidth: Dp = 0.75.dp
    }
}
