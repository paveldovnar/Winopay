package com.winopay.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.winopay.ui.theme.WinoComponents
import com.winopay.ui.theme.WinoTheme
import com.winopay.ui.theme.WinoTypography

@Composable
fun WinoInputField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    label: String? = null,
    placeholder: String = "",
    helperText: String? = null,
    errorText: String? = null,
    enabled: Boolean = true,
    readOnly: Boolean = false,
    singleLine: Boolean = true,
    maxLines: Int = 1,
    leadingIcon: (@Composable () -> Unit)? = null,
    trailingIcon: (@Composable () -> Unit)? = null,
    keyboardType: KeyboardType = KeyboardType.Text,
    imeAction: ImeAction = ImeAction.Done,
    capitalization: KeyboardCapitalization = KeyboardCapitalization.Sentences,
    onImeAction: () -> Unit = {},
    visualTransformation: VisualTransformation = VisualTransformation.None
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()

    val colors = WinoTheme.colors
    val isError = errorText != null

    val borderColor = when {
        !enabled -> colors.borderSubtle
        isError -> colors.borderCritical
        isFocused -> colors.borderBrand
        else -> colors.borderDefault
    }

    val backgroundColor = when {
        !enabled -> colors.bgSurfaceAlt
        readOnly -> colors.bgSurfaceAlt
        else -> colors.bgSurface
    }

    val textColor = when {
        !enabled -> colors.textDisabled
        else -> colors.textPrimary
    }

    val placeholderColor = colors.textMuted

    Column(modifier = modifier) {
        // Label
        label?.let {
            Text(
                text = it,
                style = WinoTypography.smallMedium,
                color = colors.textSecondary
            )
            Spacer(modifier = Modifier.height(8.dp))
        }

        // Input Container
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(WinoComponents.Input.Radius))
                .background(backgroundColor)
                .border(
                    width = WinoComponents.Input.StrokeWidth,
                    color = borderColor,
                    shape = RoundedCornerShape(WinoComponents.Input.Radius)
                )
                .padding(
                    horizontal = WinoComponents.Input.PaddingHorizontal,
                    vertical = WinoComponents.Input.PaddingVertical
                )
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                leadingIcon?.let {
                    it()
                    Spacer(modifier = Modifier.width(12.dp))
                }

                Box(modifier = Modifier.weight(1f)) {
                    if (value.isEmpty()) {
                        Text(
                            text = placeholder,
                            style = WinoTypography.body,
                            color = placeholderColor
                        )
                    }
                    BasicTextField(
                        value = value,
                        onValueChange = onValueChange,
                        modifier = Modifier.fillMaxWidth(),
                        enabled = enabled && !readOnly,
                        readOnly = readOnly,
                        textStyle = WinoTypography.body.copy(color = textColor),
                        cursorBrush = SolidColor(colors.brandPrimary),
                        singleLine = singleLine,
                        maxLines = maxLines,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = keyboardType,
                            imeAction = imeAction,
                            capitalization = capitalization
                        ),
                        keyboardActions = KeyboardActions(
                            onDone = { onImeAction() },
                            onGo = { onImeAction() },
                            onNext = { onImeAction() },
                            onSearch = { onImeAction() },
                            onSend = { onImeAction() }
                        ),
                        visualTransformation = visualTransformation,
                        interactionSource = interactionSource
                    )
                }

                trailingIcon?.let {
                    Spacer(modifier = Modifier.width(12.dp))
                    it()
                }
            }
        }

        // Helper/Error Text
        val bottomText = errorText ?: helperText
        bottomText?.let {
            Spacer(modifier = Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (isError) {
                    PhosphorIcons.Warning(
                        size = 16.dp,
                        color = colors.stateError
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                }
                Text(
                    text = it,
                    style = WinoTypography.small,
                    color = if (isError) colors.stateError else colors.textMuted
                )
            }
        }
    }
}

@Composable
fun WinoTextArea(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    label: String? = null,
    placeholder: String = "",
    helperText: String? = null,
    errorText: String? = null,
    enabled: Boolean = true,
    minLines: Int = 3,
    maxLines: Int = 5
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()

    val colors = WinoTheme.colors
    val isError = errorText != null

    val borderColor = when {
        !enabled -> colors.borderSubtle
        isError -> colors.borderCritical
        isFocused -> colors.borderBrand
        else -> colors.borderDefault
    }

    val backgroundColor = if (!enabled) colors.bgSurfaceAlt else colors.bgSurface
    val textColor = if (!enabled) colors.textDisabled else colors.textPrimary

    Column(modifier = modifier) {
        label?.let {
            Text(
                text = it,
                style = WinoTypography.smallMedium,
                color = colors.textSecondary
            )
            Spacer(modifier = Modifier.height(8.dp))
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(WinoComponents.Input.Radius))
                .background(backgroundColor)
                .border(
                    width = WinoComponents.Input.StrokeWidth,
                    color = borderColor,
                    shape = RoundedCornerShape(WinoComponents.Input.Radius)
                )
                .padding(WinoComponents.Input.PaddingHorizontal)
        ) {
            if (value.isEmpty()) {
                Text(
                    text = placeholder,
                    style = WinoTypography.body,
                    color = colors.textMuted,
                    modifier = Modifier.padding(vertical = WinoComponents.Input.PaddingVertical)
                )
            }
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = WinoComponents.Input.PaddingVertical),
                enabled = enabled,
                textStyle = WinoTypography.body.copy(color = textColor),
                cursorBrush = SolidColor(colors.brandPrimary),
                singleLine = false,
                minLines = minLines,
                maxLines = maxLines,
                interactionSource = interactionSource
            )
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
