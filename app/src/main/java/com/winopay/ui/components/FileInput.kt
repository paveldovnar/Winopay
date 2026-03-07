package com.winopay.ui.components

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.winopay.ui.theme.WinoComponents
import com.winopay.ui.theme.WinoRadius
import com.winopay.ui.theme.WinoSpacing
import com.winopay.ui.theme.WinoTheme
import com.winopay.ui.theme.WinoTypography

@Composable
fun WinoFileInput(
    label: String,
    onSelectFile: () -> Unit,
    modifier: Modifier = Modifier,
    selectedUri: Uri? = null,
    helperText: String? = null,
    errorText: String? = null
) {
    val colors = WinoTheme.colors
    val isError = errorText != null
    val hasImage = selectedUri != null

    val borderColor = when {
        isError -> colors.borderCritical
        hasImage -> colors.borderBrand
        else -> colors.borderDefault
    }

    Column(modifier = modifier) {
        Text(
            text = label,
            style = WinoTypography.smallMedium,
            color = colors.textSecondary
        )
        Spacer(modifier = Modifier.height(8.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp)
                .clip(RoundedCornerShape(WinoComponents.Input.Radius))
                .background(colors.bgSurface)
                .border(
                    width = WinoComponents.Input.StrokeWidth,
                    color = borderColor,
                    shape = RoundedCornerShape(WinoComponents.Input.Radius)
                )
                .clickable { onSelectFile() },
            contentAlignment = Alignment.Center
        ) {
            if (hasImage && selectedUri != null) {
                AsyncImage(
                    model = selectedUri,
                    contentDescription = "Selected image",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
                // Overlay for re-select
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.3f)),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        PhosphorIcons.Camera(
                            size = 24.dp,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Change",
                            style = WinoTypography.smallMedium,
                            color = Color.White
                        )
                    }
                }
            } else {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(colors.bgSurfaceAlt),
                        contentAlignment = Alignment.Center
                    ) {
                        PhosphorIcons.UploadSimple(
                            size = 24.dp,
                            color = colors.textMuted
                        )
                    }
                    Spacer(modifier = Modifier.height(WinoSpacing.XS))
                    Text(
                        text = "Upload image",
                        style = WinoTypography.smallMedium,
                        color = colors.textSecondary
                    )
                    Spacer(modifier = Modifier.height(WinoSpacing.XXS))
                    Text(
                        text = "PNG, JPG up to 5MB",
                        style = WinoTypography.micro,
                        color = colors.textMuted
                    )
                }
            }
        }

        val bottomText = errorText ?: helperText
        bottomText?.let {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = it,
                style = WinoTypography.small,
                color = if (isError) colors.stateError else colors.textMuted
            )
        }
    }
}

@Composable
fun WinoLogoInput(
    label: String,
    onSelectLogo: () -> Unit,
    modifier: Modifier = Modifier,
    selectedUri: Uri? = null,
    helperText: String? = null,
    errorText: String? = null
) {
    val colors = WinoTheme.colors
    val isError = errorText != null
    val hasImage = selectedUri != null

    val borderColor = when {
        isError -> colors.borderCritical
        hasImage -> colors.borderBrand
        else -> colors.borderDefault
    }

    Column(modifier = modifier) {
        Text(
            text = label,
            style = WinoTypography.smallMedium,
            color = colors.textSecondary
        )
        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Logo preview/placeholder
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(RoundedCornerShape(WinoRadius.MD))
                    .background(colors.bgSurface)
                    .border(
                        width = WinoComponents.Input.StrokeWidth,
                        color = borderColor,
                        shape = RoundedCornerShape(WinoRadius.MD)
                    )
                    .clickable { onSelectLogo() },
                contentAlignment = Alignment.Center
            ) {
                if (hasImage && selectedUri != null) {
                    AsyncImage(
                        model = selectedUri,
                        contentDescription = "Business logo",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    PhosphorIcons.Image(
                        size = 32.dp,
                        color = colors.textMuted
                    )
                }
            }

            Spacer(modifier = Modifier.width(WinoSpacing.MD))

            Column(modifier = Modifier.weight(1f)) {
                WinoButton(
                    text = if (hasImage) "Change logo" else "Upload logo",
                    onClick = onSelectLogo,
                    variant = WinoButtonVariant.Secondary,
                    size = WinoButtonSize.Small,
                    leadingIcon = {
                        PhosphorIcons.UploadSimple(
                            size = 16.dp,
                            color = colors.textPrimary
                        )
                    }
                )
                Spacer(modifier = Modifier.height(WinoSpacing.XXS))
                Text(
                    text = "Square format recommended",
                    style = WinoTypography.micro,
                    color = colors.textMuted
                )
            }
        }

        val bottomText = errorText ?: helperText
        bottomText?.let {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = it,
                style = WinoTypography.small,
                color = if (isError) colors.stateError else colors.textMuted
            )
        }
    }
}
