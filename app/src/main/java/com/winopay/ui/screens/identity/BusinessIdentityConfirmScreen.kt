package com.winopay.ui.screens.identity

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.winopay.WinoPayApplication
import com.winopay.data.profile.MerchantProfileStore
import com.winopay.solana.KeypairGenerator
import com.winopay.ui.components.PhosphorIcons
import com.winopay.ui.components.WinoChip
import com.winopay.ui.components.ChipVariant
import com.winopay.ui.components.WinoPrimaryButton
import com.winopay.ui.theme.WinoRadius
import com.winopay.ui.theme.WinoSpacing
import com.winopay.ui.theme.WinoTheme
import com.winopay.ui.theme.WinoTypography
import kotlinx.coroutines.launch
import java.net.URLDecoder
import androidx.compose.ui.platform.LocalContext

@Composable
fun BusinessIdentityConfirmScreen(
    businessName: String,
    logoUri: String?,
    onConfirm: () -> Unit,
    onBack: () -> Unit
) {
    val colors = WinoTheme.colors
    val scope = rememberCoroutineScope()
    val app = WinoPayApplication.instance
    val context = LocalContext.current

    // SINGLE SOURCE OF TRUTH: MerchantProfileStore
    val profileStore = remember { MerchantProfileStore(context) }

    var isLoading by remember { mutableStateOf(false) }

    val decodedLogoUri = logoUri?.let {
        try {
            Uri.parse(URLDecoder.decode(it, "UTF-8"))
        } catch (e: Exception) {
            null
        }
    }

    val handle = "@${businessName.lowercase().replace(" ", "_")}"

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.bgCanvas)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(WinoSpacing.LG),
        horizontalAlignment = Alignment.CenterHorizontally
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
            text = "Confirm your\nbusiness identity",
            style = WinoTypography.h1,
            color = colors.textPrimary,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(WinoSpacing.XS))

        Text(
            text = "Review your information before continuing",
            style = WinoTypography.body,
            color = colors.textMuted,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.weight(1f))

        // Business Card Preview
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(WinoRadius.XL))
                .background(colors.bgSurface)
                .border(
                    width = 1.dp,
                    color = colors.borderDefault,
                    shape = RoundedCornerShape(WinoRadius.XL)
                )
                .padding(WinoSpacing.LG),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Logo
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(RoundedCornerShape(WinoRadius.MD))
                    .background(colors.bgSurfaceAlt)
                    .border(
                        width = 1.dp,
                        color = colors.borderDefault,
                        shape = RoundedCornerShape(WinoRadius.MD)
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (decodedLogoUri != null) {
                    AsyncImage(
                        model = decodedLogoUri,
                        contentDescription = "Business logo",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    PhosphorIcons.Storefront(
                        size = 40.dp,
                        color = colors.textMuted
                    )
                }
            }

            Spacer(modifier = Modifier.height(WinoSpacing.MD))

            // Business Name
            Text(
                text = businessName,
                style = WinoTypography.h2,
                color = colors.textPrimary,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(WinoSpacing.XS))

            // Handle
            WinoChip(
                text = handle,
                variant = ChipVariant.Brand
            )

            Spacer(modifier = Modifier.height(WinoSpacing.LG))

            // Info
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(WinoRadius.SM))
                    .background(colors.bgSurfaceAlt)
                    .padding(WinoSpacing.SM),
                verticalAlignment = Alignment.CenterVertically
            ) {
                PhosphorIcons.Info(
                    size = 20.dp,
                    color = colors.textMuted
                )
                Spacer(modifier = Modifier.width(WinoSpacing.XS))
                Text(
                    text = "A new wallet will be created for your business",
                    style = WinoTypography.small,
                    color = colors.textMuted
                )
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        // Confirm Button
        WinoPrimaryButton(
            text = if (isLoading) "Creating..." else "Create Business",
            onClick = {
                scope.launch {
                    isLoading = true
                    try {
                        // Generate keypair
                        val (publicKey, privateKey) = KeypairGenerator.generateKeypair()

                        // Save keypair to DataStore (separate from profile)
                        app.dataStoreManager.saveBusinessIdentity(
                            name = businessName,
                            logoUri = decodedLogoUri?.toString(),
                            handle = handle,
                            publicKey = publicKey,
                            privateKeyEncrypted = privateKey
                        )

                        // SINGLE SOURCE: Save business profile to MerchantProfileStore
                        profileStore.saveBusinessProfile(
                            businessName = businessName,
                            logoUri = decodedLogoUri
                        )

                        // SINGLE SOURCE: Mark onboarding completed
                        profileStore.setOnboardingCompleted(true)

                        onConfirm()
                    } catch (e: Exception) {
                        isLoading = false
                    }
                }
            },
            loading = isLoading,
            enabled = !isLoading,
            leadingIcon = if (!isLoading) {
                { PhosphorIcons.Check(size = 20.dp, color = colors.textOnBrand) }
            } else null
        )

        Spacer(modifier = Modifier.height(WinoSpacing.MD))
    }
}
