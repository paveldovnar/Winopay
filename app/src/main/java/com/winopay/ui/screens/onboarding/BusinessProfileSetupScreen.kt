package com.winopay.ui.screens.onboarding

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.winopay.WinoPayApplication
import com.winopay.data.profile.MerchantProfileStore
import com.winopay.ui.components.PhosphorIcons
import androidx.compose.ui.platform.LocalContext
import com.winopay.ui.components.WinoButton
import com.winopay.ui.components.WinoButtonSize
import com.winopay.ui.components.WinoButtonVariant
import com.winopay.ui.components.WinoInputField
import com.winopay.ui.theme.WinoRadius
import com.winopay.ui.theme.WinoSpacing
import com.winopay.ui.theme.WinoTheme
import com.winopay.ui.theme.WinoTypography
import kotlinx.coroutines.launch

/**
 * Simple business profile setup screen.
 *
 * LOCAL-ONLY:
 * - No "claim", "verify", or "Wino ID" concepts
 * - Just business name + optional logo
 * - Saves to DataStore offline
 *
 * @param publicKey Wallet public key (for context, not stored here)
 * @param onContinue Called after profile is saved
 * @param onBack Go back to previous screen
 */
@Composable
fun BusinessProfileSetupScreen(
    publicKey: String,
    onContinue: () -> Unit,
    onBack: () -> Unit
) {
    val colors = WinoTheme.colors
    val scope = rememberCoroutineScope()
    val app = WinoPayApplication.instance
    val context = LocalContext.current

    // SINGLE SOURCE OF TRUTH: MerchantProfileStore
    val profileStore = remember { MerchantProfileStore(context) }

    var businessName by remember { mutableStateOf("") }
    var logoUri by remember { mutableStateOf<Uri?>(null) }
    var isSaving by remember { mutableStateOf(false) }

    val isValid = businessName.isNotBlank()

    // Image picker
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let { logoUri = it }
    }

    fun saveAndContinue() {
        if (!isValid || isSaving) return

        isSaving = true
        scope.launch {
            // Save keypair to DataStore (legacy, still used for some features)
            app.dataStoreManager.saveBusinessIdentity(
                name = businessName,
                logoUri = logoUri?.toString(),
                handle = "@${businessName.lowercase().replace(" ", "_")}",
                publicKey = publicKey,
                privateKeyEncrypted = "" // No private key needed for connected wallet
            )

            // SINGLE SOURCE: Save business profile to MerchantProfileStore
            profileStore.saveBusinessProfile(
                businessName = businessName,
                logoUri = logoUri
            )

            // SINGLE SOURCE: Mark onboarding completed
            profileStore.setOnboardingCompleted(true)

            onContinue()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.bgCanvas)
            .statusBarsPadding()
    ) {
        // Scrollable content
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(WinoSpacing.LG),
            verticalArrangement = Arrangement.spacedBy(WinoSpacing.MD)
        ) {
            // Title
            Text(
                text = "Enter business info",
                style = WinoTypography.h1Medium,
                color = colors.textPrimary
            )

            Text(
                text = "This info will be shown to customers when they pay you.",
                style = WinoTypography.body,
                color = colors.textSecondary
            )

            Spacer(modifier = Modifier.height(WinoSpacing.MD))

            // Logo picker (optional)
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(96.dp)
                        .clip(CircleShape)
                        .background(colors.bgSurfaceAlt)
                        .clickable { imagePickerLauncher.launch("image/*") },
                    contentAlignment = Alignment.Center
                ) {
                    if (logoUri != null) {
                        AsyncImage(
                            model = logoUri,
                            contentDescription = "Business logo",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(WinoSpacing.XXS)
                        ) {
                            PhosphorIcons.Camera(
                                size = 32.dp,
                                color = colors.textMuted
                            )
                            Text(
                                text = "Add logo",
                                style = WinoTypography.micro,
                                color = colors.textMuted
                            )
                        }
                    }
                }

                if (logoUri != null) {
                    Spacer(modifier = Modifier.height(WinoSpacing.XS))
                    Text(
                        text = "Tap to change",
                        style = WinoTypography.micro,
                        color = colors.textMuted,
                        modifier = Modifier.clickable { imagePickerLauncher.launch("image/*") }
                    )
                }
            }

            Spacer(modifier = Modifier.height(WinoSpacing.MD))

            // Business name input
            WinoInputField(
                value = businessName,
                onValueChange = { businessName = it },
                label = "Business name",
                placeholder = "Enter your business name",
                modifier = Modifier.fillMaxWidth()
            )

            // Helper text
            Text(
                text = "Logo is optional. You can add or change it later in Settings.",
                style = WinoTypography.small,
                color = colors.textMuted
            )
        }

        // Bottom button area
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(WinoSpacing.LG),
            verticalArrangement = Arrangement.spacedBy(WinoSpacing.SM)
        ) {
            WinoButton(
                text = if (isSaving) "Saving..." else "Continue",
                onClick = { saveAndContinue() },
                modifier = Modifier.fillMaxWidth(),
                variant = WinoButtonVariant.Primary,
                size = WinoButtonSize.Large,
                enabled = isValid && !isSaving
            )

            WinoButton(
                text = "Back",
                onClick = onBack,
                modifier = Modifier.fillMaxWidth(),
                variant = WinoButtonVariant.Ghost,
                size = WinoButtonSize.Large,
                enabled = !isSaving
            )
        }
    }
}
