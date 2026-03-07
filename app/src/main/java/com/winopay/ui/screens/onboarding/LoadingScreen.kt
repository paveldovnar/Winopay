package com.winopay.ui.screens.onboarding

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.winopay.ui.theme.WinoSpacing
import com.winopay.ui.theme.WinoTheme
import com.winopay.ui.theme.WinoTypography
import kotlinx.coroutines.delay

// Figma frame: 1.8 Loading (973:6393)
// MCP source: get_screenshot, get_design_context for node 973:6393

@Composable
fun LoadingScreen(
    title: String = "Getting things done",
    message: String = "Please wait",
    onComplete: () -> Unit
) {
    val colors = WinoTheme.colors

    // Rotating arc animation
    val infiniteTransition = rememberInfiniteTransition(label = "loading")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing)
        ),
        label = "rotation"
    )

    LaunchedEffect(Unit) {
        delay(2000)
        onComplete()
    }

    // Root: bg=bg/canvas, items-center, justify-center, gap=XL(32dp)
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.bgCanvas)
            .safeDrawingPadding(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(WinoSpacing.XL, Alignment.CenterVertically)
    ) {
        // "Loader" (973:6527): 192x192dp — circular arc spinner
        Box(
            modifier = Modifier.size(192.dp),
            contentAlignment = Alignment.Center
        ) {
            val brandColor = colors.brandPrimary
            Canvas(modifier = Modifier.fillMaxSize()) {
                drawArc(
                    color = brandColor,
                    startAngle = rotation - 90f,
                    sweepAngle = 270f,
                    useCenter = false,
                    style = Stroke(
                        width = 8.dp.toPx(),
                        cap = StrokeCap.Round
                    )
                )
            }
        }

        // "Text" frame (973:6530)
        // gap=XXS(4dp), px=LG(24dp), py=MD(16dp), text-center, w-full
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = WinoSpacing.LG,
                    vertical = WinoSpacing.MD
                ),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(WinoSpacing.XXS)
        ) {
            // "title" (973:6531) — H1 Medium, text/primary
            Text(
                text = title,
                style = WinoTypography.h1Medium,
                color = colors.textPrimary,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )

            // "subtitle" (973:6532) — Body, text/secondary
            Text(
                text = message,
                style = WinoTypography.body,
                color = colors.textSecondary,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}
