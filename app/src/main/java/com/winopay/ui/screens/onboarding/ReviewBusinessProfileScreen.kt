package com.winopay.ui.screens.onboarding

import android.net.Uri
import androidx.compose.foundation.background
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.winopay.data.profile.MerchantProfileStore
import com.winopay.ui.components.WinoButton
import com.winopay.ui.components.WinoButtonSize
import com.winopay.ui.components.WinoButtonVariant
import com.winopay.ui.theme.WinoSpacing
import com.winopay.ui.theme.WinoTheme
import com.winopay.ui.theme.WinoTypography
import kotlinx.coroutines.launch

/**
 * Review business profile screen.
 *
 * Reads the saved profile from MerchantProfileStore and shows preview.
 * User can go back to edit or continue to payment methods setup.
 * On continue, marks onboarding as completed.
 */
@Composable
fun ReviewBusinessProfileScreen(
    onContinue: () -> Unit,
    onBack: () -> Unit
) {
    val colors = WinoTheme.colors
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val profileStore = remember { MerchantProfileStore(context) }

    // Load profile data
    var businessName by remember { mutableStateOf("") }
    var logoPath by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        val profile = profileStore.getBusinessProfile()
        businessName = profile?.businessName ?: ""
        logoPath = profile?.logoLocalPath
    }

    // Convert path to Uri for AsyncImage
    val logoUri = logoPath?.let { Uri.parse("file://$it") }

    fun confirmAndContinue() {
        scope.launch {
            // Mark onboarding as completed
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
                text = "Review your profile",
                style = WinoTypography.h1Medium,
                color = colors.textPrimary
            )

            Text(
                text = "This is how customers will see your business.",
                style = WinoTypography.body,
                color = colors.textSecondary
            )

            Spacer(modifier = Modifier.height(WinoSpacing.XL))

            // Logo + Name preview (centered)
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(WinoSpacing.MD)
            ) {
                // Logo
                Box(
                    modifier = Modifier
                        .size(96.dp)
                        .clip(CircleShape)
                        .background(colors.bgSurfaceAlt),
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
                        // Default placeholder with first letter
                        Text(
                            text = businessName.firstOrNull()?.uppercase() ?: "B",
                            style = WinoTypography.h1Medium,
                            color = colors.textMuted
                        )
                    }
                }

                // Business name
                Text(
                    text = businessName,
                    style = WinoTypography.h2Medium,
                    color = colors.textPrimary
                )
            }
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
                text = "Continue",
                onClick = { confirmAndContinue() },
                modifier = Modifier.fillMaxWidth(),
                variant = WinoButtonVariant.Primary,
                size = WinoButtonSize.Large
            )

            WinoButton(
                text = "Back",
                onClick = onBack,
                modifier = Modifier.fillMaxWidth(),
                variant = WinoButtonVariant.Ghost,
                size = WinoButtonSize.Large
            )
        }
    }
}
