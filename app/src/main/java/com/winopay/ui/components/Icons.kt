package com.winopay.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.winopay.ui.theme.WinoTheme

// Phosphor Icons - Vector implementations
object PhosphorIcons {

    @Composable
    fun ArrowRight(
        modifier: Modifier = Modifier,
        size: Dp = 24.dp,
        color: Color = WinoTheme.colors.textPrimary,
        strokeWidth: Float = 2f
    ) {
        Canvas(modifier = modifier.size(size)) {
            val scale = size.toPx() / 24f
            drawLine(
                color = color,
                start = Offset(5f * scale, 12f * scale),
                end = Offset(19f * scale, 12f * scale),
                strokeWidth = strokeWidth * scale,
                cap = StrokeCap.Round
            )
            drawLine(
                color = color,
                start = Offset(12f * scale, 5f * scale),
                end = Offset(19f * scale, 12f * scale),
                strokeWidth = strokeWidth * scale,
                cap = StrokeCap.Round
            )
            drawLine(
                color = color,
                start = Offset(12f * scale, 19f * scale),
                end = Offset(19f * scale, 12f * scale),
                strokeWidth = strokeWidth * scale,
                cap = StrokeCap.Round
            )
        }
    }

    @Composable
    fun ArrowLeft(
        modifier: Modifier = Modifier,
        size: Dp = 24.dp,
        color: Color = WinoTheme.colors.textPrimary,
        strokeWidth: Float = 2f
    ) {
        Canvas(modifier = modifier.size(size)) {
            val scale = size.toPx() / 24f
            drawLine(
                color = color,
                start = Offset(19f * scale, 12f * scale),
                end = Offset(5f * scale, 12f * scale),
                strokeWidth = strokeWidth * scale,
                cap = StrokeCap.Round
            )
            drawLine(
                color = color,
                start = Offset(12f * scale, 5f * scale),
                end = Offset(5f * scale, 12f * scale),
                strokeWidth = strokeWidth * scale,
                cap = StrokeCap.Round
            )
            drawLine(
                color = color,
                start = Offset(12f * scale, 19f * scale),
                end = Offset(5f * scale, 12f * scale),
                strokeWidth = strokeWidth * scale,
                cap = StrokeCap.Round
            )
        }
    }

    @Composable
    fun Backspace(
        modifier: Modifier = Modifier,
        size: Dp = 24.dp,
        color: Color = WinoTheme.colors.textPrimary,
        strokeWidth: Float = 2f
    ) {
        Canvas(modifier = modifier.size(size)) {
            val scale = size.toPx() / 24f
            val stroke = Stroke(width = strokeWidth * scale, cap = StrokeCap.Round, join = StrokeJoin.Round)

            // Outer shape
            val path = Path().apply {
                moveTo(8.5f * scale, 4f * scale)
                lineTo(21f * scale, 4f * scale)
                lineTo(21f * scale, 20f * scale)
                lineTo(8.5f * scale, 20f * scale)
                lineTo(2f * scale, 12f * scale)
                close()
            }
            drawPath(path, color, style = stroke)

            // X marks
            drawLine(
                color = color,
                start = Offset(11f * scale, 9f * scale),
                end = Offset(17f * scale, 15f * scale),
                strokeWidth = strokeWidth * scale,
                cap = StrokeCap.Round
            )
            drawLine(
                color = color,
                start = Offset(17f * scale, 9f * scale),
                end = Offset(11f * scale, 15f * scale),
                strokeWidth = strokeWidth * scale,
                cap = StrokeCap.Round
            )
        }
    }

    @Composable
    fun Check(
        modifier: Modifier = Modifier,
        size: Dp = 24.dp,
        color: Color = WinoTheme.colors.textPrimary,
        strokeWidth: Float = 2f
    ) {
        Canvas(modifier = modifier.size(size)) {
            val scale = size.toPx() / 24f
            drawLine(
                color = color,
                start = Offset(4f * scale, 12f * scale),
                end = Offset(9f * scale, 17f * scale),
                strokeWidth = strokeWidth * scale,
                cap = StrokeCap.Round
            )
            drawLine(
                color = color,
                start = Offset(9f * scale, 17f * scale),
                end = Offset(20f * scale, 6f * scale),
                strokeWidth = strokeWidth * scale,
                cap = StrokeCap.Round
            )
        }
    }

    @Composable
    fun CheckCircle(
        modifier: Modifier = Modifier,
        size: Dp = 24.dp,
        color: Color = WinoTheme.colors.stateSuccess,
        strokeWidth: Float = 2f
    ) {
        Canvas(modifier = modifier.size(size)) {
            val scale = size.toPx() / 24f
            val stroke = Stroke(width = strokeWidth * scale, cap = StrokeCap.Round)

            // Circle
            drawCircle(
                color = color,
                radius = 10f * scale,
                center = Offset(12f * scale, 12f * scale),
                style = stroke
            )

            // Check
            drawLine(
                color = color,
                start = Offset(8f * scale, 12f * scale),
                end = Offset(11f * scale, 15f * scale),
                strokeWidth = strokeWidth * scale,
                cap = StrokeCap.Round
            )
            drawLine(
                color = color,
                start = Offset(11f * scale, 15f * scale),
                end = Offset(16f * scale, 9f * scale),
                strokeWidth = strokeWidth * scale,
                cap = StrokeCap.Round
            )
        }
    }

    @Composable
    fun XCircle(
        modifier: Modifier = Modifier,
        size: Dp = 24.dp,
        color: Color = WinoTheme.colors.stateError,
        strokeWidth: Float = 2f
    ) {
        Canvas(modifier = modifier.size(size)) {
            val scale = size.toPx() / 24f
            val stroke = Stroke(width = strokeWidth * scale, cap = StrokeCap.Round)

            // Circle
            drawCircle(
                color = color,
                radius = 10f * scale,
                center = Offset(12f * scale, 12f * scale),
                style = stroke
            )

            // X
            drawLine(
                color = color,
                start = Offset(9f * scale, 9f * scale),
                end = Offset(15f * scale, 15f * scale),
                strokeWidth = strokeWidth * scale,
                cap = StrokeCap.Round
            )
            drawLine(
                color = color,
                start = Offset(15f * scale, 9f * scale),
                end = Offset(9f * scale, 15f * scale),
                strokeWidth = strokeWidth * scale,
                cap = StrokeCap.Round
            )
        }
    }

    @Composable
    fun Clock(
        modifier: Modifier = Modifier,
        size: Dp = 24.dp,
        color: Color = WinoTheme.colors.stateWarning,
        strokeWidth: Float = 2f
    ) {
        Canvas(modifier = modifier.size(size)) {
            val scale = size.toPx() / 24f
            val stroke = Stroke(width = strokeWidth * scale, cap = StrokeCap.Round)

            // Circle
            drawCircle(
                color = color,
                radius = 10f * scale,
                center = Offset(12f * scale, 12f * scale),
                style = stroke
            )

            // Hands
            drawLine(
                color = color,
                start = Offset(12f * scale, 12f * scale),
                end = Offset(12f * scale, 7f * scale),
                strokeWidth = strokeWidth * scale,
                cap = StrokeCap.Round
            )
            drawLine(
                color = color,
                start = Offset(12f * scale, 12f * scale),
                end = Offset(16f * scale, 14f * scale),
                strokeWidth = strokeWidth * scale,
                cap = StrokeCap.Round
            )
        }
    }

    @Composable
    fun Plus(
        modifier: Modifier = Modifier,
        size: Dp = 24.dp,
        color: Color = WinoTheme.colors.textPrimary,
        strokeWidth: Float = 2f
    ) {
        Canvas(modifier = modifier.size(size)) {
            val scale = size.toPx() / 24f
            drawLine(
                color = color,
                start = Offset(12f * scale, 5f * scale),
                end = Offset(12f * scale, 19f * scale),
                strokeWidth = strokeWidth * scale,
                cap = StrokeCap.Round
            )
            drawLine(
                color = color,
                start = Offset(5f * scale, 12f * scale),
                end = Offset(19f * scale, 12f * scale),
                strokeWidth = strokeWidth * scale,
                cap = StrokeCap.Round
            )
        }
    }

    @Composable
    fun Camera(
        modifier: Modifier = Modifier,
        size: Dp = 24.dp,
        color: Color = WinoTheme.colors.textPrimary,
        strokeWidth: Float = 2f
    ) {
        Canvas(modifier = modifier.size(size)) {
            val scale = size.toPx() / 24f
            val stroke = Stroke(width = strokeWidth * scale, cap = StrokeCap.Round, join = StrokeJoin.Round)

            // Body
            val path = Path().apply {
                moveTo(3f * scale, 7f * scale)
                lineTo(7f * scale, 7f * scale)
                lineTo(9f * scale, 4f * scale)
                lineTo(15f * scale, 4f * scale)
                lineTo(17f * scale, 7f * scale)
                lineTo(21f * scale, 7f * scale)
                lineTo(21f * scale, 19f * scale)
                lineTo(3f * scale, 19f * scale)
                close()
            }
            drawPath(path, color, style = stroke)

            // Lens
            drawCircle(
                color = color,
                radius = 4f * scale,
                center = Offset(12f * scale, 12.5f * scale),
                style = stroke
            )
        }
    }

    @Composable
    fun Image(
        modifier: Modifier = Modifier,
        size: Dp = 24.dp,
        color: Color = WinoTheme.colors.textPrimary,
        strokeWidth: Float = 2f
    ) {
        Canvas(modifier = modifier.size(size)) {
            val scale = size.toPx() / 24f
            val stroke = Stroke(width = strokeWidth * scale, cap = StrokeCap.Round, join = StrokeJoin.Round)

            // Frame
            drawRoundRect(
                color = color,
                topLeft = Offset(3f * scale, 4f * scale),
                size = androidx.compose.ui.geometry.Size(18f * scale, 16f * scale),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(2f * scale),
                style = stroke
            )

            // Mountain
            val path = Path().apply {
                moveTo(3f * scale, 17f * scale)
                lineTo(8f * scale, 12f * scale)
                lineTo(12f * scale, 16f * scale)
                lineTo(16f * scale, 11f * scale)
                lineTo(21f * scale, 17f * scale)
            }
            drawPath(path, color, style = stroke)

            // Sun
            drawCircle(
                color = color,
                radius = 2f * scale,
                center = Offset(7f * scale, 8f * scale),
                style = stroke
            )
        }
    }

    @Composable
    fun UploadSimple(
        modifier: Modifier = Modifier,
        size: Dp = 24.dp,
        color: Color = WinoTheme.colors.textPrimary,
        strokeWidth: Float = 2f
    ) {
        Canvas(modifier = modifier.size(size)) {
            val scale = size.toPx() / 24f

            // Arrow up
            drawLine(
                color = color,
                start = Offset(12f * scale, 3f * scale),
                end = Offset(12f * scale, 15f * scale),
                strokeWidth = strokeWidth * scale,
                cap = StrokeCap.Round
            )
            drawLine(
                color = color,
                start = Offset(7f * scale, 8f * scale),
                end = Offset(12f * scale, 3f * scale),
                strokeWidth = strokeWidth * scale,
                cap = StrokeCap.Round
            )
            drawLine(
                color = color,
                start = Offset(17f * scale, 8f * scale),
                end = Offset(12f * scale, 3f * scale),
                strokeWidth = strokeWidth * scale,
                cap = StrokeCap.Round
            )

            // Bottom line
            val stroke = Stroke(width = strokeWidth * scale, cap = StrokeCap.Round, join = StrokeJoin.Round)
            val path = Path().apply {
                moveTo(4f * scale, 14f * scale)
                lineTo(4f * scale, 20f * scale)
                lineTo(20f * scale, 20f * scale)
                lineTo(20f * scale, 14f * scale)
            }
            drawPath(path, color, style = stroke)
        }
    }

    @Composable
    fun Storefront(
        modifier: Modifier = Modifier,
        size: Dp = 24.dp,
        color: Color = WinoTheme.colors.textPrimary,
        strokeWidth: Float = 2f
    ) {
        Canvas(modifier = modifier.size(size)) {
            val scale = size.toPx() / 24f
            val stroke = Stroke(width = strokeWidth * scale, cap = StrokeCap.Round, join = StrokeJoin.Round)

            // Roof
            val roof = Path().apply {
                moveTo(2f * scale, 9f * scale)
                lineTo(4f * scale, 4f * scale)
                lineTo(20f * scale, 4f * scale)
                lineTo(22f * scale, 9f * scale)
            }
            drawPath(roof, color, style = stroke)

            // Awning curves
            drawArc(
                color = color,
                startAngle = 0f,
                sweepAngle = 180f,
                useCenter = false,
                topLeft = Offset(2f * scale, 7f * scale),
                size = androidx.compose.ui.geometry.Size(5f * scale, 5f * scale),
                style = stroke
            )
            drawArc(
                color = color,
                startAngle = 0f,
                sweepAngle = 180f,
                useCenter = false,
                topLeft = Offset(7f * scale, 7f * scale),
                size = androidx.compose.ui.geometry.Size(5f * scale, 5f * scale),
                style = stroke
            )
            drawArc(
                color = color,
                startAngle = 0f,
                sweepAngle = 180f,
                useCenter = false,
                topLeft = Offset(12f * scale, 7f * scale),
                size = androidx.compose.ui.geometry.Size(5f * scale, 5f * scale),
                style = stroke
            )
            drawArc(
                color = color,
                startAngle = 0f,
                sweepAngle = 180f,
                useCenter = false,
                topLeft = Offset(17f * scale, 7f * scale),
                size = androidx.compose.ui.geometry.Size(5f * scale, 5f * scale),
                style = stroke
            )

            // Walls
            drawLine(
                color = color,
                start = Offset(4f * scale, 12f * scale),
                end = Offset(4f * scale, 20f * scale),
                strokeWidth = strokeWidth * scale,
                cap = StrokeCap.Round
            )
            drawLine(
                color = color,
                start = Offset(20f * scale, 12f * scale),
                end = Offset(20f * scale, 20f * scale),
                strokeWidth = strokeWidth * scale,
                cap = StrokeCap.Round
            )
            drawLine(
                color = color,
                start = Offset(4f * scale, 20f * scale),
                end = Offset(20f * scale, 20f * scale),
                strokeWidth = strokeWidth * scale,
                cap = StrokeCap.Round
            )

            // Door
            drawRoundRect(
                color = color,
                topLeft = Offset(9f * scale, 14f * scale),
                size = androidx.compose.ui.geometry.Size(6f * scale, 6f * scale),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(1f * scale),
                style = stroke
            )
        }
    }

    @Composable
    fun QrCode(
        modifier: Modifier = Modifier,
        size: Dp = 24.dp,
        color: Color = WinoTheme.colors.textPrimary,
        strokeWidth: Float = 2f
    ) {
        Canvas(modifier = modifier.size(size)) {
            val scale = size.toPx() / 24f
            val stroke = Stroke(width = strokeWidth * scale, cap = StrokeCap.Round, join = StrokeJoin.Round)

            // Top-left box
            drawRect(
                color = color,
                topLeft = Offset(3f * scale, 3f * scale),
                size = androidx.compose.ui.geometry.Size(7f * scale, 7f * scale),
                style = stroke
            )
            drawRect(
                color = color,
                topLeft = Offset(5f * scale, 5f * scale),
                size = androidx.compose.ui.geometry.Size(3f * scale, 3f * scale)
            )

            // Top-right box
            drawRect(
                color = color,
                topLeft = Offset(14f * scale, 3f * scale),
                size = androidx.compose.ui.geometry.Size(7f * scale, 7f * scale),
                style = stroke
            )
            drawRect(
                color = color,
                topLeft = Offset(16f * scale, 5f * scale),
                size = androidx.compose.ui.geometry.Size(3f * scale, 3f * scale)
            )

            // Bottom-left box
            drawRect(
                color = color,
                topLeft = Offset(3f * scale, 14f * scale),
                size = androidx.compose.ui.geometry.Size(7f * scale, 7f * scale),
                style = stroke
            )
            drawRect(
                color = color,
                topLeft = Offset(5f * scale, 16f * scale),
                size = androidx.compose.ui.geometry.Size(3f * scale, 3f * scale)
            )

            // Bottom-right pattern
            drawRect(
                color = color,
                topLeft = Offset(14f * scale, 14f * scale),
                size = androidx.compose.ui.geometry.Size(3f * scale, 3f * scale)
            )
            drawRect(
                color = color,
                topLeft = Offset(18f * scale, 14f * scale),
                size = androidx.compose.ui.geometry.Size(3f * scale, 3f * scale)
            )
            drawRect(
                color = color,
                topLeft = Offset(14f * scale, 18f * scale),
                size = androidx.compose.ui.geometry.Size(3f * scale, 3f * scale)
            )
            drawRect(
                color = color,
                topLeft = Offset(18f * scale, 18f * scale),
                size = androidx.compose.ui.geometry.Size(3f * scale, 3f * scale)
            )
        }
    }

    @Composable
    fun SignOut(
        modifier: Modifier = Modifier,
        size: Dp = 24.dp,
        color: Color = WinoTheme.colors.textPrimary,
        strokeWidth: Float = 2f
    ) {
        Canvas(modifier = modifier.size(size)) {
            val scale = size.toPx() / 24f
            val stroke = Stroke(width = strokeWidth * scale, cap = StrokeCap.Round, join = StrokeJoin.Round)

            // Door frame
            val path = Path().apply {
                moveTo(9f * scale, 3f * scale)
                lineTo(4f * scale, 3f * scale)
                lineTo(4f * scale, 21f * scale)
                lineTo(9f * scale, 21f * scale)
            }
            drawPath(path, color, style = stroke)

            // Arrow
            drawLine(
                color = color,
                start = Offset(9f * scale, 12f * scale),
                end = Offset(20f * scale, 12f * scale),
                strokeWidth = strokeWidth * scale,
                cap = StrokeCap.Round
            )
            drawLine(
                color = color,
                start = Offset(15f * scale, 7f * scale),
                end = Offset(20f * scale, 12f * scale),
                strokeWidth = strokeWidth * scale,
                cap = StrokeCap.Round
            )
            drawLine(
                color = color,
                start = Offset(15f * scale, 17f * scale),
                end = Offset(20f * scale, 12f * scale),
                strokeWidth = strokeWidth * scale,
                cap = StrokeCap.Round
            )
        }
    }

    @Composable
    fun Copy(
        modifier: Modifier = Modifier,
        size: Dp = 24.dp,
        color: Color = WinoTheme.colors.textPrimary,
        strokeWidth: Float = 2f
    ) {
        Canvas(modifier = modifier.size(size)) {
            val scale = size.toPx() / 24f
            val stroke = Stroke(width = strokeWidth * scale, cap = StrokeCap.Round, join = StrokeJoin.Round)

            // Front rect
            drawRoundRect(
                color = color,
                topLeft = Offset(8f * scale, 8f * scale),
                size = androidx.compose.ui.geometry.Size(12f * scale, 12f * scale),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(2f * scale),
                style = stroke
            )

            // Back rect (partial)
            val path = Path().apply {
                moveTo(16f * scale, 8f * scale)
                lineTo(16f * scale, 4f * scale)
                lineTo(4f * scale, 4f * scale)
                lineTo(4f * scale, 16f * scale)
                lineTo(8f * scale, 16f * scale)
            }
            drawPath(path, color, style = stroke)
        }
    }

    @Composable
    fun ArrowsClockwise(
        modifier: Modifier = Modifier,
        size: Dp = 24.dp,
        color: Color = WinoTheme.colors.textPrimary,
        strokeWidth: Float = 2f
    ) {
        Canvas(modifier = modifier.size(size)) {
            val scale = size.toPx() / 24f
            val stroke = Stroke(width = strokeWidth * scale, cap = StrokeCap.Round, join = StrokeJoin.Round)

            // Top arc
            drawArc(
                color = color,
                startAngle = 180f,
                sweepAngle = 180f,
                useCenter = false,
                topLeft = Offset(4f * scale, 4f * scale),
                size = androidx.compose.ui.geometry.Size(16f * scale, 16f * scale),
                style = stroke
            )

            // Arrow at top
            val arrow1 = Path().apply {
                moveTo(20f * scale, 8f * scale)
                lineTo(20f * scale, 12f * scale)
                lineTo(16f * scale, 12f * scale)
            }
            drawPath(arrow1, color, style = stroke)

            // Bottom arc
            drawArc(
                color = color,
                startAngle = 0f,
                sweepAngle = 180f,
                useCenter = false,
                topLeft = Offset(4f * scale, 4f * scale),
                size = androidx.compose.ui.geometry.Size(16f * scale, 16f * scale),
                style = stroke
            )

            // Arrow at bottom
            val arrow2 = Path().apply {
                moveTo(4f * scale, 16f * scale)
                lineTo(4f * scale, 12f * scale)
                lineTo(8f * scale, 12f * scale)
            }
            drawPath(arrow2, color, style = stroke)
        }
    }

    @Composable
    fun X(
        modifier: Modifier = Modifier,
        size: Dp = 24.dp,
        color: Color = WinoTheme.colors.textPrimary,
        strokeWidth: Float = 2f
    ) {
        Canvas(modifier = modifier.size(size)) {
            val scale = size.toPx() / 24f
            drawLine(
                color = color,
                start = Offset(6f * scale, 6f * scale),
                end = Offset(18f * scale, 18f * scale),
                strokeWidth = strokeWidth * scale,
                cap = StrokeCap.Round
            )
            drawLine(
                color = color,
                start = Offset(18f * scale, 6f * scale),
                end = Offset(6f * scale, 18f * scale),
                strokeWidth = strokeWidth * scale,
                cap = StrokeCap.Round
            )
        }
    }

    @Composable
    fun CaretDown(
        modifier: Modifier = Modifier,
        size: Dp = 24.dp,
        color: Color = WinoTheme.colors.textPrimary,
        strokeWidth: Float = 2f
    ) {
        Canvas(modifier = modifier.size(size)) {
            val scale = size.toPx() / 24f
            drawLine(
                color = color,
                start = Offset(6f * scale, 9f * scale),
                end = Offset(12f * scale, 15f * scale),
                strokeWidth = strokeWidth * scale,
                cap = StrokeCap.Round
            )
            drawLine(
                color = color,
                start = Offset(18f * scale, 9f * scale),
                end = Offset(12f * scale, 15f * scale),
                strokeWidth = strokeWidth * scale,
                cap = StrokeCap.Round
            )
        }
    }

    @Composable
    fun Info(
        modifier: Modifier = Modifier,
        size: Dp = 24.dp,
        color: Color = WinoTheme.colors.textPrimary,
        strokeWidth: Float = 2f
    ) {
        Canvas(modifier = modifier.size(size)) {
            val scale = size.toPx() / 24f
            val stroke = Stroke(width = strokeWidth * scale, cap = StrokeCap.Round)

            // Circle
            drawCircle(
                color = color,
                radius = 10f * scale,
                center = Offset(12f * scale, 12f * scale),
                style = stroke
            )

            // Dot
            drawCircle(
                color = color,
                radius = 1.5f * scale,
                center = Offset(12f * scale, 8f * scale)
            )

            // Line
            drawLine(
                color = color,
                start = Offset(12f * scale, 11f * scale),
                end = Offset(12f * scale, 16f * scale),
                strokeWidth = strokeWidth * scale,
                cap = StrokeCap.Round
            )
        }
    }

    @Composable
    fun Warning(
        modifier: Modifier = Modifier,
        size: Dp = 24.dp,
        color: Color = WinoTheme.colors.stateWarning,
        strokeWidth: Float = 2f
    ) {
        Canvas(modifier = modifier.size(size)) {
            val scale = size.toPx() / 24f
            val stroke = Stroke(width = strokeWidth * scale, cap = StrokeCap.Round, join = StrokeJoin.Round)

            // Triangle
            val path = Path().apply {
                moveTo(12f * scale, 3f * scale)
                lineTo(22f * scale, 20f * scale)
                lineTo(2f * scale, 20f * scale)
                close()
            }
            drawPath(path, color, style = stroke)

            // Exclamation
            drawLine(
                color = color,
                start = Offset(12f * scale, 10f * scale),
                end = Offset(12f * scale, 14f * scale),
                strokeWidth = strokeWidth * scale,
                cap = StrokeCap.Round
            )
            drawCircle(
                color = color,
                radius = 1.2f * scale,
                center = Offset(12f * scale, 17f * scale)
            )
        }
    }

    @Composable
    fun CaretRight(
        modifier: Modifier = Modifier,
        size: Dp = 24.dp,
        color: Color = WinoTheme.colors.textPrimary,
        strokeWidth: Float = 2f
    ) {
        Canvas(modifier = modifier.size(size)) {
            val scale = size.toPx() / 24f
            drawLine(
                color = color,
                start = Offset(9f * scale, 6f * scale),
                end = Offset(15f * scale, 12f * scale),
                strokeWidth = strokeWidth * scale,
                cap = StrokeCap.Round
            )
            drawLine(
                color = color,
                start = Offset(9f * scale, 18f * scale),
                end = Offset(15f * scale, 12f * scale),
                strokeWidth = strokeWidth * scale,
                cap = StrokeCap.Round
            )
        }
    }

    @Composable
    fun CaretLeft(
        modifier: Modifier = Modifier,
        size: Dp = 24.dp,
        color: Color = WinoTheme.colors.textPrimary,
        strokeWidth: Float = 2f
    ) {
        Canvas(modifier = modifier.size(size)) {
            val scale = size.toPx() / 24f
            drawLine(
                color = color,
                start = Offset(15f * scale, 6f * scale),
                end = Offset(9f * scale, 12f * scale),
                strokeWidth = strokeWidth * scale,
                cap = StrokeCap.Round
            )
            drawLine(
                color = color,
                start = Offset(15f * scale, 18f * scale),
                end = Offset(9f * scale, 12f * scale),
                strokeWidth = strokeWidth * scale,
                cap = StrokeCap.Round
            )
        }
    }

    @Composable
    fun Wallet(
        modifier: Modifier = Modifier,
        size: Dp = 24.dp,
        color: Color = WinoTheme.colors.textPrimary,
        strokeWidth: Float = 2f
    ) {
        Canvas(modifier = modifier.size(size)) {
            val scale = size.toPx() / 24f
            val stroke = Stroke(width = strokeWidth * scale, cap = StrokeCap.Round, join = StrokeJoin.Round)

            // Main body
            drawRoundRect(
                color = color,
                topLeft = Offset(2f * scale, 5f * scale),
                size = androidx.compose.ui.geometry.Size(20f * scale, 14f * scale),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(2f * scale),
                style = stroke
            )

            // Card slot
            drawLine(
                color = color,
                start = Offset(16f * scale, 10f * scale),
                end = Offset(19f * scale, 10f * scale),
                strokeWidth = strokeWidth * scale,
                cap = StrokeCap.Round
            )

            // Dot
            drawCircle(
                color = color,
                radius = 1.5f * scale,
                center = Offset(17f * scale, 13f * scale)
            )
        }
    }

    @Composable
    fun PlusCircle(
        modifier: Modifier = Modifier,
        size: Dp = 24.dp,
        color: Color = WinoTheme.colors.textPrimary,
        strokeWidth: Float = 2f
    ) {
        Canvas(modifier = modifier.size(size)) {
            val scale = size.toPx() / 24f
            val stroke = Stroke(width = strokeWidth * scale, cap = StrokeCap.Round)

            // Circle
            drawCircle(
                color = color,
                radius = 10f * scale,
                center = Offset(12f * scale, 12f * scale),
                style = stroke
            )

            // Plus
            drawLine(
                color = color,
                start = Offset(12f * scale, 8f * scale),
                end = Offset(12f * scale, 16f * scale),
                strokeWidth = strokeWidth * scale,
                cap = StrokeCap.Round
            )
            drawLine(
                color = color,
                start = Offset(8f * scale, 12f * scale),
                end = Offset(16f * scale, 12f * scale),
                strokeWidth = strokeWidth * scale,
                cap = StrokeCap.Round
            )
        }
    }

    @Composable
    fun ShieldCheck(
        modifier: Modifier = Modifier,
        size: Dp = 24.dp,
        color: Color = WinoTheme.colors.stateSuccess,
        strokeWidth: Float = 2f
    ) {
        Canvas(modifier = modifier.size(size)) {
            val scale = size.toPx() / 24f
            val stroke = Stroke(width = strokeWidth * scale, cap = StrokeCap.Round, join = StrokeJoin.Round)

            // Shield
            val path = Path().apply {
                moveTo(12f * scale, 2f * scale)
                lineTo(3f * scale, 5f * scale)
                lineTo(3f * scale, 11f * scale)
                cubicTo(3f * scale, 16f * scale, 7f * scale, 20f * scale, 12f * scale, 22f * scale)
                cubicTo(17f * scale, 20f * scale, 21f * scale, 16f * scale, 21f * scale, 11f * scale)
                lineTo(21f * scale, 5f * scale)
                close()
            }
            drawPath(path, color, style = stroke)

            // Check
            drawLine(
                color = color,
                start = Offset(8f * scale, 12f * scale),
                end = Offset(11f * scale, 15f * scale),
                strokeWidth = strokeWidth * scale,
                cap = StrokeCap.Round
            )
            drawLine(
                color = color,
                start = Offset(11f * scale, 15f * scale),
                end = Offset(16f * scale, 9f * scale),
                strokeWidth = strokeWidth * scale,
                cap = StrokeCap.Round
            )
        }
    }

    @Composable
    fun Moon(
        modifier: Modifier = Modifier,
        size: Dp = 24.dp,
        color: Color = WinoTheme.colors.textPrimary,
        strokeWidth: Float = 2f
    ) {
        Canvas(modifier = modifier.size(size)) {
            val scale = size.toPx() / 24f
            val stroke = Stroke(width = strokeWidth * scale, cap = StrokeCap.Round, join = StrokeJoin.Round)

            val path = Path().apply {
                moveTo(20f * scale, 14f * scale)
                cubicTo(20f * scale, 8f * scale, 14f * scale, 3f * scale, 8f * scale, 5f * scale)
                cubicTo(3f * scale, 7f * scale, 2f * scale, 14f * scale, 6f * scale, 18f * scale)
                cubicTo(10f * scale, 22f * scale, 17f * scale, 21f * scale, 20f * scale, 14f * scale)
            }
            drawPath(path, color, style = stroke)
        }
    }

    @Composable
    fun Sun(
        modifier: Modifier = Modifier,
        size: Dp = 24.dp,
        color: Color = WinoTheme.colors.stateWarning,
        strokeWidth: Float = 2f
    ) {
        Canvas(modifier = modifier.size(size)) {
            val scale = size.toPx() / 24f
            val stroke = Stroke(width = strokeWidth * scale, cap = StrokeCap.Round)

            // Center circle
            drawCircle(
                color = color,
                radius = 5f * scale,
                center = Offset(12f * scale, 12f * scale),
                style = stroke
            )

            // Rays
            val rays = listOf(
                Pair(Offset(12f, 2f), Offset(12f, 4f)),
                Pair(Offset(12f, 20f), Offset(12f, 22f)),
                Pair(Offset(2f, 12f), Offset(4f, 12f)),
                Pair(Offset(20f, 12f), Offset(22f, 12f)),
                Pair(Offset(4.93f, 4.93f), Offset(6.34f, 6.34f)),
                Pair(Offset(17.66f, 17.66f), Offset(19.07f, 19.07f)),
                Pair(Offset(4.93f, 19.07f), Offset(6.34f, 17.66f)),
                Pair(Offset(17.66f, 6.34f), Offset(19.07f, 4.93f))
            )
            rays.forEach { (start, end) ->
                drawLine(
                    color = color,
                    start = Offset(start.x * scale, start.y * scale),
                    end = Offset(end.x * scale, end.y * scale),
                    strokeWidth = strokeWidth * scale,
                    cap = StrokeCap.Round
                )
            }
        }
    }

    @Composable
    fun DeviceMobile(
        modifier: Modifier = Modifier,
        size: Dp = 24.dp,
        color: Color = WinoTheme.colors.textPrimary,
        strokeWidth: Float = 2f
    ) {
        Canvas(modifier = modifier.size(size)) {
            val scale = size.toPx() / 24f
            val stroke = Stroke(width = strokeWidth * scale, cap = StrokeCap.Round, join = StrokeJoin.Round)

            // Phone body
            drawRoundRect(
                color = color,
                topLeft = Offset(6f * scale, 2f * scale),
                size = androidx.compose.ui.geometry.Size(12f * scale, 20f * scale),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(2f * scale),
                style = stroke
            )

            // Bottom button/indicator
            drawLine(
                color = color,
                start = Offset(10f * scale, 18f * scale),
                end = Offset(14f * scale, 18f * scale),
                strokeWidth = strokeWidth * scale,
                cap = StrokeCap.Round
            )
        }
    }

    @Composable
    fun IdentificationCard(
        modifier: Modifier = Modifier,
        size: Dp = 24.dp,
        color: Color = WinoTheme.colors.textPrimary,
        strokeWidth: Float = 2f
    ) {
        Canvas(modifier = modifier.size(size)) {
            val scale = size.toPx() / 24f
            val stroke = Stroke(width = strokeWidth * scale, cap = StrokeCap.Round, join = StrokeJoin.Round)

            // Card
            drawRoundRect(
                color = color,
                topLeft = Offset(2f * scale, 4f * scale),
                size = androidx.compose.ui.geometry.Size(20f * scale, 16f * scale),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(2f * scale),
                style = stroke
            )

            // Avatar circle
            drawCircle(
                color = color,
                radius = 2.5f * scale,
                center = Offset(7f * scale, 10f * scale),
                style = stroke
            )

            // Lines
            drawLine(
                color = color,
                start = Offset(12f * scale, 9f * scale),
                end = Offset(19f * scale, 9f * scale),
                strokeWidth = strokeWidth * scale,
                cap = StrokeCap.Round
            )
            drawLine(
                color = color,
                start = Offset(12f * scale, 13f * scale),
                end = Offset(17f * scale, 13f * scale),
                strokeWidth = strokeWidth * scale,
                cap = StrokeCap.Round
            )
        }
    }

    @Composable
    fun CreditCard(
        modifier: Modifier = Modifier,
        size: Dp = 24.dp,
        color: Color = WinoTheme.colors.textPrimary,
        strokeWidth: Float = 2f
    ) {
        Canvas(modifier = modifier.size(size)) {
            val scale = size.toPx() / 24f
            val stroke = Stroke(width = strokeWidth * scale, cap = StrokeCap.Round, join = StrokeJoin.Round)

            // Card
            drawRoundRect(
                color = color,
                topLeft = Offset(2f * scale, 4f * scale),
                size = androidx.compose.ui.geometry.Size(20f * scale, 16f * scale),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(2f * scale),
                style = stroke
            )

            // Stripe
            drawLine(
                color = color,
                start = Offset(2f * scale, 9f * scale),
                end = Offset(22f * scale, 9f * scale),
                strokeWidth = strokeWidth * scale,
                cap = StrokeCap.Round
            )

            // Bottom line
            drawLine(
                color = color,
                start = Offset(5f * scale, 15f * scale),
                end = Offset(10f * scale, 15f * scale),
                strokeWidth = strokeWidth * scale,
                cap = StrokeCap.Round
            )
        }
    }

    @Composable
    fun CurrencyCircleDollar(
        modifier: Modifier = Modifier,
        size: Dp = 24.dp,
        color: Color = WinoTheme.colors.stateSuccess,
        strokeWidth: Float = 2f
    ) {
        Canvas(modifier = modifier.size(size)) {
            val scale = size.toPx() / 24f
            val stroke = Stroke(width = strokeWidth * scale, cap = StrokeCap.Round, join = StrokeJoin.Round)

            // Circle
            drawCircle(
                color = color,
                radius = 10f * scale,
                center = Offset(12f * scale, 12f * scale),
                style = stroke
            )

            // Dollar sign
            drawLine(
                color = color,
                start = Offset(12f * scale, 6f * scale),
                end = Offset(12f * scale, 18f * scale),
                strokeWidth = strokeWidth * scale,
                cap = StrokeCap.Round
            )

            // S curves
            val sPath = Path().apply {
                moveTo(15f * scale, 9f * scale)
                cubicTo(15f * scale, 7.5f * scale, 13f * scale, 7f * scale, 12f * scale, 7.5f * scale)
                cubicTo(10f * scale, 8f * scale, 9f * scale, 9f * scale, 9f * scale, 10f * scale)
                cubicTo(9f * scale, 11f * scale, 10f * scale, 12f * scale, 12f * scale, 12f * scale)
                cubicTo(14f * scale, 12f * scale, 15f * scale, 13f * scale, 15f * scale, 14f * scale)
                cubicTo(15f * scale, 15f * scale, 14f * scale, 16.5f * scale, 12f * scale, 16.5f * scale)
                cubicTo(10f * scale, 16.5f * scale, 9f * scale, 15f * scale, 9f * scale, 15f * scale)
            }
            drawPath(sPath, color, style = stroke)
        }
    }

    @Composable
    fun Globe(
        modifier: Modifier = Modifier,
        size: Dp = 24.dp,
        color: Color = WinoTheme.colors.textPrimary,
        strokeWidth: Float = 2f
    ) {
        Canvas(modifier = modifier.size(size)) {
            val scale = size.toPx() / 24f
            val stroke = Stroke(width = strokeWidth * scale, cap = StrokeCap.Round)

            // Main circle
            drawCircle(
                color = color,
                radius = 10f * scale,
                center = Offset(12f * scale, 12f * scale),
                style = stroke
            )

            // Horizontal line
            drawLine(
                color = color,
                start = Offset(2f * scale, 12f * scale),
                end = Offset(22f * scale, 12f * scale),
                strokeWidth = strokeWidth * scale,
                cap = StrokeCap.Round
            )

            // Ellipse vertical
            drawOval(
                color = color,
                topLeft = Offset(8f * scale, 2f * scale),
                size = androidx.compose.ui.geometry.Size(8f * scale, 20f * scale),
                style = stroke
            )
        }
    }

    @Composable
    fun GearSix(
        modifier: Modifier = Modifier,
        size: Dp = 24.dp,
        color: Color = WinoTheme.colors.textPrimary,
        strokeWidth: Float = 2f
    ) {
        Canvas(modifier = modifier.size(size)) {
            val scale = size.toPx() / 24f
            val stroke = Stroke(width = strokeWidth * scale, cap = StrokeCap.Round, join = StrokeJoin.Round)

            // Center circle
            drawCircle(
                color = color,
                radius = 3f * scale,
                center = Offset(12f * scale, 12f * scale),
                style = stroke
            )

            // Outer gear shape (simplified as octagon with spokes)
            val outerRadius = 9f * scale
            val centerX = 12f * scale
            val centerY = 12f * scale

            for (i in 0..5) {
                val angle = Math.toRadians((i * 60.0) - 30)
                val x = centerX + (outerRadius * kotlin.math.cos(angle)).toFloat()
                val y = centerY + (outerRadius * kotlin.math.sin(angle)).toFloat()
                drawLine(
                    color = color,
                    start = Offset(centerX + (5f * scale * kotlin.math.cos(angle)).toFloat(),
                        centerY + (5f * scale * kotlin.math.sin(angle)).toFloat()),
                    end = Offset(x, y),
                    strokeWidth = strokeWidth * scale,
                    cap = StrokeCap.Round
                )
            }

            // Outer circle
            drawCircle(
                color = color,
                radius = 8f * scale,
                center = Offset(12f * scale, 12f * scale),
                style = stroke
            )
        }
    }

    @Composable
    fun SealCheck(
        modifier: Modifier = Modifier,
        size: Dp = 24.dp,
        color: Color = WinoTheme.colors.brandPrimary,
        strokeWidth: Float = 2f
    ) {
        Canvas(modifier = modifier.size(size)) {
            val scale = size.toPx() / 24f
            val stroke = Stroke(width = strokeWidth * scale, cap = StrokeCap.Round, join = StrokeJoin.Round)

            // Seal shape (wavy circle)
            drawCircle(
                color = color,
                radius = 9f * scale,
                center = Offset(12f * scale, 12f * scale),
                style = stroke
            )

            // Check mark
            drawLine(
                color = color,
                start = Offset(8f * scale, 12f * scale),
                end = Offset(11f * scale, 15f * scale),
                strokeWidth = strokeWidth * scale,
                cap = StrokeCap.Round
            )
            drawLine(
                color = color,
                start = Offset(11f * scale, 15f * scale),
                end = Offset(16f * scale, 9f * scale),
                strokeWidth = strokeWidth * scale,
                cap = StrokeCap.Round
            )
        }
    }

    @Composable
    fun ArrowSquareOut(
        modifier: Modifier = Modifier,
        size: Dp = 24.dp,
        color: Color = WinoTheme.colors.textPrimary,
        strokeWidth: Float = 2f
    ) {
        Canvas(modifier = modifier.size(size)) {
            val scale = size.toPx() / 24f
            val stroke = Stroke(width = strokeWidth * scale, cap = StrokeCap.Round, join = StrokeJoin.Round)

            // Square (partial)
            val path = Path().apply {
                moveTo(10f * scale, 4f * scale)
                lineTo(4f * scale, 4f * scale)
                lineTo(4f * scale, 20f * scale)
                lineTo(20f * scale, 20f * scale)
                lineTo(20f * scale, 14f * scale)
            }
            drawPath(path, color, style = stroke)

            // Arrow
            drawLine(
                color = color,
                start = Offset(10f * scale, 14f * scale),
                end = Offset(20f * scale, 4f * scale),
                strokeWidth = strokeWidth * scale,
                cap = StrokeCap.Round
            )
            drawLine(
                color = color,
                start = Offset(14f * scale, 4f * scale),
                end = Offset(20f * scale, 4f * scale),
                strokeWidth = strokeWidth * scale,
                cap = StrokeCap.Round
            )
            drawLine(
                color = color,
                start = Offset(20f * scale, 10f * scale),
                end = Offset(20f * scale, 4f * scale),
                strokeWidth = strokeWidth * scale,
                cap = StrokeCap.Round
            )
        }
    }

    @Composable
    fun FileText(
        modifier: Modifier = Modifier,
        size: Dp = 24.dp,
        color: Color = WinoTheme.colors.textPrimary,
        strokeWidth: Float = 2f
    ) {
        Canvas(modifier = modifier.size(size)) {
            val scale = size.toPx() / 24f
            val stroke = Stroke(width = strokeWidth * scale, cap = StrokeCap.Round, join = StrokeJoin.Round)

            // File shape
            val path = Path().apply {
                moveTo(14f * scale, 2f * scale)
                lineTo(4f * scale, 2f * scale)
                lineTo(4f * scale, 22f * scale)
                lineTo(20f * scale, 22f * scale)
                lineTo(20f * scale, 8f * scale)
                close()
            }
            drawPath(path, color, style = stroke)

            // Fold
            drawLine(
                color = color,
                start = Offset(14f * scale, 2f * scale),
                end = Offset(14f * scale, 8f * scale),
                strokeWidth = strokeWidth * scale,
                cap = StrokeCap.Round
            )
            drawLine(
                color = color,
                start = Offset(14f * scale, 8f * scale),
                end = Offset(20f * scale, 8f * scale),
                strokeWidth = strokeWidth * scale,
                cap = StrokeCap.Round
            )

            // Text lines
            drawLine(
                color = color,
                start = Offset(8f * scale, 13f * scale),
                end = Offset(16f * scale, 13f * scale),
                strokeWidth = strokeWidth * scale,
                cap = StrokeCap.Round
            )
            drawLine(
                color = color,
                start = Offset(8f * scale, 17f * scale),
                end = Offset(16f * scale, 17f * scale),
                strokeWidth = strokeWidth * scale,
                cap = StrokeCap.Round
            )
        }
    }

    @Composable
    fun Shield(
        modifier: Modifier = Modifier,
        size: Dp = 24.dp,
        color: Color = WinoTheme.colors.textPrimary,
        strokeWidth: Float = 2f
    ) {
        Canvas(modifier = modifier.size(size)) {
            val scale = size.toPx() / 24f
            val stroke = Stroke(width = strokeWidth * scale, cap = StrokeCap.Round, join = StrokeJoin.Round)

            val path = Path().apply {
                moveTo(12f * scale, 2f * scale)
                lineTo(3f * scale, 5f * scale)
                lineTo(3f * scale, 11f * scale)
                cubicTo(3f * scale, 16f * scale, 7f * scale, 20f * scale, 12f * scale, 22f * scale)
                cubicTo(17f * scale, 20f * scale, 21f * scale, 16f * scale, 21f * scale, 11f * scale)
                lineTo(21f * scale, 5f * scale)
                close()
            }
            drawPath(path, color, style = stroke)
        }
    }

    @Composable
    fun ChatCircle(
        modifier: Modifier = Modifier,
        size: Dp = 24.dp,
        color: Color = WinoTheme.colors.textPrimary,
        strokeWidth: Float = 2f
    ) {
        Canvas(modifier = modifier.size(size)) {
            val scale = size.toPx() / 24f
            val stroke = Stroke(width = strokeWidth * scale, cap = StrokeCap.Round, join = StrokeJoin.Round)

            // Chat bubble
            val path = Path().apply {
                moveTo(12f * scale, 21f * scale)
                lineTo(8f * scale, 17f * scale)
                cubicTo(4f * scale, 17f * scale, 2f * scale, 14f * scale, 2f * scale, 10f * scale)
                cubicTo(2f * scale, 5f * scale, 6f * scale, 2f * scale, 12f * scale, 2f * scale)
                cubicTo(18f * scale, 2f * scale, 22f * scale, 5f * scale, 22f * scale, 10f * scale)
                cubicTo(22f * scale, 14f * scale, 20f * scale, 17f * scale, 16f * scale, 17f * scale)
                close()
            }
            drawPath(path, color, style = stroke)
        }
    }

    @Composable
    fun Export(
        modifier: Modifier = Modifier,
        size: Dp = 24.dp,
        color: Color = WinoTheme.colors.textPrimary,
        strokeWidth: Float = 2f
    ) {
        Canvas(modifier = modifier.size(size)) {
            val scale = size.toPx() / 24f
            val stroke = Stroke(width = strokeWidth * scale, cap = StrokeCap.Round, join = StrokeJoin.Round)

            // Arrow up
            drawLine(
                color = color,
                start = Offset(12f * scale, 3f * scale),
                end = Offset(12f * scale, 14f * scale),
                strokeWidth = strokeWidth * scale,
                cap = StrokeCap.Round
            )
            drawLine(
                color = color,
                start = Offset(7f * scale, 8f * scale),
                end = Offset(12f * scale, 3f * scale),
                strokeWidth = strokeWidth * scale,
                cap = StrokeCap.Round
            )
            drawLine(
                color = color,
                start = Offset(17f * scale, 8f * scale),
                end = Offset(12f * scale, 3f * scale),
                strokeWidth = strokeWidth * scale,
                cap = StrokeCap.Round
            )

            // Box bottom
            val path = Path().apply {
                moveTo(4f * scale, 11f * scale)
                lineTo(4f * scale, 20f * scale)
                lineTo(20f * scale, 20f * scale)
                lineTo(20f * scale, 11f * scale)
            }
            drawPath(path, color, style = stroke)
        }
    }

    @Composable
    fun PencilSimple(
        modifier: Modifier = Modifier,
        size: Dp = 24.dp,
        color: Color = WinoTheme.colors.textPrimary,
        strokeWidth: Float = 2f
    ) {
        Canvas(modifier = modifier.size(size)) {
            val scale = size.toPx() / 24f
            val stroke = Stroke(width = strokeWidth * scale, cap = StrokeCap.Round, join = StrokeJoin.Round)

            // Pencil body
            val path = Path().apply {
                moveTo(14f * scale, 4f * scale)
                lineTo(20f * scale, 10f * scale)
                lineTo(8f * scale, 22f * scale)
                lineTo(2f * scale, 22f * scale)
                lineTo(2f * scale, 16f * scale)
                close()
            }
            drawPath(path, color, style = stroke)

            // Eraser line
            drawLine(
                color = color,
                start = Offset(14f * scale, 4f * scale),
                end = Offset(20f * scale, 10f * scale),
                strokeWidth = strokeWidth * scale,
                cap = StrokeCap.Round
            )
        }
    }

    @Composable
    fun Lock(
        modifier: Modifier = Modifier,
        size: Dp = 24.dp,
        color: Color = WinoTheme.colors.textPrimary,
        strokeWidth: Float = 2f
    ) {
        Canvas(modifier = modifier.size(size)) {
            val scale = size.toPx() / 24f
            val stroke = Stroke(width = strokeWidth * scale, cap = StrokeCap.Round, join = StrokeJoin.Round)

            // Lock body
            drawRoundRect(
                color = color,
                topLeft = Offset(5f * scale, 10f * scale),
                size = androidx.compose.ui.geometry.Size(14f * scale, 11f * scale),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(2f * scale),
                style = stroke
            )

            // Shackle
            val shackle = Path().apply {
                moveTo(8f * scale, 10f * scale)
                lineTo(8f * scale, 7f * scale)
                cubicTo(8f * scale, 4f * scale, 10f * scale, 2f * scale, 12f * scale, 2f * scale)
                cubicTo(14f * scale, 2f * scale, 16f * scale, 4f * scale, 16f * scale, 7f * scale)
                lineTo(16f * scale, 10f * scale)
            }
            drawPath(shackle, color, style = stroke)
        }
    }

    @Composable
    fun ChartLine(
        modifier: Modifier = Modifier,
        size: Dp = 24.dp,
        color: Color = WinoTheme.colors.textPrimary,
        strokeWidth: Float = 2f
    ) {
        Canvas(modifier = modifier.size(size)) {
            val scale = size.toPx() / 24f
            val stroke = Stroke(width = strokeWidth * scale, cap = StrokeCap.Round, join = StrokeJoin.Round)

            // Chart line
            val path = Path().apply {
                moveTo(4f * scale, 18f * scale)
                lineTo(8f * scale, 12f * scale)
                lineTo(13f * scale, 15f * scale)
                lineTo(20f * scale, 6f * scale)
            }
            drawPath(path, color, style = stroke)
        }
    }

    @Composable
    fun Confetti(
        modifier: Modifier = Modifier,
        size: Dp = 24.dp,
        color: Color = WinoTheme.colors.stateSuccess,
        strokeWidth: Float = 2f
    ) {
        Canvas(modifier = modifier.size(size)) {
            val scale = size.toPx() / 24f
            val stroke = Stroke(width = strokeWidth * scale, cap = StrokeCap.Round)

            // Party popper shape
            drawLine(
                color = color,
                start = Offset(4f * scale, 20f * scale),
                end = Offset(11f * scale, 13f * scale),
                strokeWidth = strokeWidth * scale,
                cap = StrokeCap.Round
            )

            // Confetti pieces
            drawCircle(color = color, radius = 1.5f * scale, center = Offset(14f * scale, 6f * scale))
            drawCircle(color = color, radius = 1.5f * scale, center = Offset(18f * scale, 10f * scale))
            drawCircle(color = color, radius = 1.5f * scale, center = Offset(16f * scale, 14f * scale))
            drawCircle(color = color, radius = 1.5f * scale, center = Offset(8f * scale, 8f * scale))
            drawCircle(color = color, radius = 1.5f * scale, center = Offset(20f * scale, 6f * scale))
        }
    }

    @Composable
    fun WifiMedium(
        modifier: Modifier = Modifier,
        size: Dp = 24.dp,
        color: Color = WinoTheme.colors.textPrimary,
        strokeWidth: Float = 2f
    ) {
        Canvas(modifier = modifier.size(size)) {
            val scale = size.toPx() / 24f
            val stroke = Stroke(width = strokeWidth * scale, cap = StrokeCap.Round, join = StrokeJoin.Round)

            // Bottom dot
            drawCircle(
                color = color,
                radius = 1.5f * scale,
                center = Offset(12f * scale, 19f * scale)
            )

            // First arc (smallest)
            drawArc(
                color = color,
                startAngle = 225f,
                sweepAngle = 90f,
                useCenter = false,
                topLeft = Offset(8f * scale, 13f * scale),
                size = androidx.compose.ui.geometry.Size(8f * scale, 8f * scale),
                style = stroke
            )

            // Second arc (medium)
            drawArc(
                color = color,
                startAngle = 225f,
                sweepAngle = 90f,
                useCenter = false,
                topLeft = Offset(5f * scale, 10f * scale),
                size = androidx.compose.ui.geometry.Size(14f * scale, 14f * scale),
                style = stroke
            )
        }
    }

    @Composable
    fun File(
        modifier: Modifier = Modifier,
        size: Dp = 24.dp,
        color: Color = WinoTheme.colors.textPrimary,
        strokeWidth: Float = 2f
    ) {
        Canvas(modifier = modifier.size(size)) {
            val scale = size.toPx() / 24f
            val stroke = Stroke(width = strokeWidth * scale, cap = StrokeCap.Round, join = StrokeJoin.Round)

            // File shape with folded corner
            val path = Path().apply {
                moveTo(14f * scale, 2f * scale)
                lineTo(4f * scale, 2f * scale)
                lineTo(4f * scale, 22f * scale)
                lineTo(20f * scale, 22f * scale)
                lineTo(20f * scale, 8f * scale)
                close()
            }
            drawPath(path, color, style = stroke)

            // Fold
            drawLine(
                color = color,
                start = Offset(14f * scale, 2f * scale),
                end = Offset(14f * scale, 8f * scale),
                strokeWidth = strokeWidth * scale,
                cap = StrokeCap.Round
            )
            drawLine(
                color = color,
                start = Offset(14f * scale, 8f * scale),
                end = Offset(20f * scale, 8f * scale),
                strokeWidth = strokeWidth * scale,
                cap = StrokeCap.Round
            )
        }
    }

    @Composable
    fun SunHorizon(
        modifier: Modifier = Modifier,
        size: Dp = 24.dp,
        color: Color = WinoTheme.colors.textPrimary,
        strokeWidth: Float = 2f
    ) {
        Canvas(modifier = modifier.size(size)) {
            val scale = size.toPx() / 24f
            val stroke = Stroke(width = strokeWidth * scale, cap = StrokeCap.Round)

            // Horizon line
            drawLine(
                color = color,
                start = Offset(2f * scale, 16f * scale),
                end = Offset(22f * scale, 16f * scale),
                strokeWidth = strokeWidth * scale,
                cap = StrokeCap.Round
            )

            // Half sun (arc above horizon)
            drawArc(
                color = color,
                startAngle = 180f,
                sweepAngle = 180f,
                useCenter = false,
                topLeft = Offset(6f * scale, 10f * scale),
                size = androidx.compose.ui.geometry.Size(12f * scale, 12f * scale),
                style = stroke
            )

            // Rays
            val rays = listOf(
                Pair(Offset(12f, 4f), Offset(12f, 6f)),
                Pair(Offset(6.5f, 6.5f), Offset(7.9f, 7.9f)),
                Pair(Offset(17.5f, 6.5f), Offset(16.1f, 7.9f))
            )
            rays.forEach { (start, end) ->
                drawLine(
                    color = color,
                    start = Offset(start.x * scale, start.y * scale),
                    end = Offset(end.x * scale, end.y * scale),
                    strokeWidth = strokeWidth * scale,
                    cap = StrokeCap.Round
                )
            }
        }
    }

}
