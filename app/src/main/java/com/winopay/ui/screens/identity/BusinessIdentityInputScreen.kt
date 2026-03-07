package com.winopay.ui.screens.identity

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import com.winopay.ui.components.PhosphorIcons
import com.winopay.ui.components.WinoInputField
import com.winopay.ui.components.WinoLogoInput
import com.winopay.ui.components.WinoPrimaryButton
import com.winopay.ui.theme.WinoSpacing
import com.winopay.ui.theme.WinoTheme
import com.winopay.ui.theme.WinoTypography
import java.net.URLEncoder

@Composable
fun BusinessIdentityInputScreen(
    onContinue: (name: String, logoUri: String?) -> Unit,
    onBack: () -> Unit
) {
    val colors = WinoTheme.colors

    var businessName by remember { mutableStateOf("") }
    var logoUri by remember { mutableStateOf<Uri?>(null) }
    var nameError by remember { mutableStateOf<String?>(null) }

    val imagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { logoUri = it }
    }

    val isValid = businessName.trim().length >= 2

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.bgCanvas)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(WinoSpacing.LG)
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(colors.bgSurface)
                    .clickable { onBack() },
                contentAlignment = Alignment.Center
            ) {
                PhosphorIcons.ArrowLeft(
                    size = 20.dp,
                    color = colors.textPrimary
                )
            }
        }

        Spacer(modifier = Modifier.height(WinoSpacing.XL))

        // Title
        Text(
            text = "Create your\nbusiness identity",
            style = WinoTypography.h1,
            color = colors.textPrimary
        )

        Spacer(modifier = Modifier.height(WinoSpacing.XS))

        Text(
            text = "This information will be shown to your customers",
            style = WinoTypography.body,
            color = colors.textMuted
        )

        Spacer(modifier = Modifier.height(WinoSpacing.XL))

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
        ) {
            // Business Name Input
            WinoInputField(
                value = businessName,
                onValueChange = {
                    businessName = it
                    nameError = null
                },
                label = "Business Name",
                placeholder = "Enter your business name",
                errorText = nameError,
                helperText = "Minimum 2 characters",
                capitalization = KeyboardCapitalization.Words,
                imeAction = ImeAction.Done,
                leadingIcon = {
                    PhosphorIcons.Storefront(
                        size = 20.dp,
                        color = colors.textMuted
                    )
                }
            )

            Spacer(modifier = Modifier.height(WinoSpacing.LG))

            // Logo Input
            WinoLogoInput(
                label = "Business Logo (Optional)",
                onSelectLogo = { imagePicker.launch("image/*") },
                selectedUri = logoUri,
                helperText = "This will appear on payment requests"
            )
        }

        // Continue Button
        WinoPrimaryButton(
            text = "Continue",
            onClick = {
                if (isValid) {
                    val encodedUri = logoUri?.toString()?.let {
                        URLEncoder.encode(it, "UTF-8")
                    }
                    onContinue(businessName.trim(), encodedUri)
                } else {
                    nameError = "Please enter a valid business name"
                }
            },
            enabled = isValid,
            trailingIcon = {
                PhosphorIcons.ArrowRight(
                    size = 20.dp,
                    color = if (isValid) colors.textOnBrand else colors.textDisabled
                )
            }
        )

        Spacer(modifier = Modifier.height(WinoSpacing.MD))
    }
}
