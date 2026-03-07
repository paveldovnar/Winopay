package com.winopay.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.winopay.R
import com.winopay.ui.theme.WinoTheme
import com.winopay.ui.theme.WinoTypography

/**
 * Token type enum with associated colors from Figma
 * Figma node: 1182:10120
 */
enum class TokenType(
    val displayName: String,
    val symbol: String,
    val backgroundColor: Color
) {
    WINO("Wino", "W", Color(0xFF7C5CFF)),    // brandPrimary
    USDC("USDC", "$", Color(0xFF0088FF)),    // #08f
    USDT("USDT", "\u20AE", Color(0xFF1BA27A)) // #1ba27a, using Tenge symbol for USDT
}

/**
 * Network type enum with associated colors from Figma
 * Figma node: 1182:10111
 */
enum class NetworkType(
    val displayName: String,
    val symbol: String,
    val backgroundColor: Color,
    val textColor: Color
) {
    SOLANA("Solana", "S", Color(0xFF7C5CFF), Color.White),           // brandPrimary bg, white text
    BSC("BSC", "B", Color(0xFFF8BF34), Color(0xFFB88217)),           // #f8bf34 bg, darker text
    TON("TON", "T", Color(0xFF0098EA), Color.White),                 // #0098ea bg, white text
    TRC20("TRC-20", "T", Color(0xFFFE0A0E), Color.White)             // #fe0a0e bg, white text
}

/**
 * Token Icon component
 * Figma: 42dp circle with token-specific background color and symbol
 */
@Composable
fun TokenIcon(
    token: TokenType,
    modifier: Modifier = Modifier,
    size: Dp = 42.dp
) {
    val colors = WinoTheme.colors

    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(token.backgroundColor),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = token.symbol,
            style = WinoTypography.h3Medium,
            color = Color.White
        )
    }
}

/**
 * Network Badge Icon component
 * Figma: 20dp circle with 2.5dp border (bg/canvas color), network-specific background
 */
@Composable
fun NetworkIcon(
    network: NetworkType,
    modifier: Modifier = Modifier,
    size: Dp = 20.dp,
    borderWidth: Dp = 2.5.dp
) {
    val colors = WinoTheme.colors

    Box(
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(size / 2))
            .border(borderWidth, colors.bgCanvas, RoundedCornerShape(size / 2))
            .background(network.backgroundColor),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = network.symbol,
            style = WinoTypography.micro,
            color = network.textColor
        )
    }
}

/**
 * Token.net composite component - Token icon with Network badge overlay
 * Figma node: 1182:10241
 * Badge position: bottom-end with offset(6dp, 0dp)
 */
@Composable
fun TokenWithNetworkBadge(
    token: TokenType,
    network: NetworkType,
    modifier: Modifier = Modifier,
    tokenSize: Dp = 42.dp,
    badgeSize: Dp = 20.dp
) {
    Box(modifier = modifier.size(tokenSize)) {
        // Token icon
        TokenIcon(
            token = token,
            size = tokenSize
        )

        // Network badge at bottom-end with offset
        NetworkIcon(
            network = network,
            size = badgeSize,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .offset(x = 6.dp)
        )
    }
}

/**
 * Generic asset icon with custom colors (for backward compatibility)
 * Used in PaymentMethodsSetupScreen for custom icons
 */
@Composable
fun AssetIcon(
    symbol: String,
    backgroundColor: Color,
    textColor: Color,
    modifier: Modifier = Modifier,
    size: Dp = 42.dp
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(backgroundColor),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = symbol,
            style = WinoTypography.h3Medium,
            color = textColor
        )
    }
}

/**
 * Generic network badge with custom colors (for backward compatibility)
 */
@Composable
fun NetworkBadge(
    symbol: String,
    backgroundColor: Color,
    textColor: Color,
    modifier: Modifier = Modifier,
    size: Dp = 20.dp,
    borderWidth: Dp = 2.5.dp
) {
    val colors = WinoTheme.colors

    Box(
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(size / 2))
            .border(borderWidth, colors.bgCanvas, RoundedCornerShape(size / 2))
            .background(backgroundColor),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = symbol,
            style = WinoTypography.micro,
            color = textColor
        )
    }
}

/**
 * Asset icon with network badge overlay (generic version)
 * For use with custom colors
 */
@Composable
fun AssetWithNetworkBadge(
    assetSymbol: String,
    assetBgColor: Color,
    assetTextColor: Color,
    networkSymbol: String,
    networkBgColor: Color,
    networkTextColor: Color,
    modifier: Modifier = Modifier,
    assetSize: Dp = 42.dp,
    badgeSize: Dp = 20.dp
) {
    val colors = WinoTheme.colors

    Box(modifier = modifier.size(assetSize)) {
        // Asset icon
        AssetIcon(
            symbol = assetSymbol,
            backgroundColor = assetBgColor,
            textColor = assetTextColor,
            size = assetSize
        )

        // Network badge
        NetworkBadge(
            symbol = networkSymbol,
            backgroundColor = networkBgColor,
            textColor = networkTextColor,
            size = badgeSize,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .offset(x = 6.dp)
        )
    }
}
