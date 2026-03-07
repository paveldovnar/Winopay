package com.winopay.ui.screens.onboarding

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.winopay.BuildConfig
import com.winopay.R
import com.winopay.data.profile.MerchantProfileStore
import com.winopay.data.profile.RailConnection
import com.winopay.payments.TronPaymentRail
import com.winopay.tron.TronAddressUtils
import com.winopay.tron.TronConstants
import com.winopay.ui.components.PhosphorIcons
import com.winopay.ui.components.WinoButton
import com.winopay.ui.components.WinoButtonSize
import com.winopay.ui.components.WinoButtonVariant
import com.winopay.ui.theme.WinoRadius
import com.winopay.ui.theme.WinoSpacing
import com.winopay.ui.theme.WinoTheme
import com.winopay.ui.theme.WinoTypography
import kotlinx.coroutines.launch

private const val TAG = "TronConnectScreen"

@Composable
fun TronConnectScreen(
    onConnected: (address: String) -> Unit,
    onBack: () -> Unit
) {
    val colors = WinoTheme.colors
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current

    val profileStore = remember { MerchantProfileStore(context) }

    // State
    var address by remember { mutableStateOf("") }
    var isValid by remember { mutableStateOf<Boolean?>(null) }
    var isConnected by remember { mutableStateOf(false) }
    var isSaving by remember { mutableStateOf(false) }
    var isOnboardingCompleted by remember { mutableStateOf<Boolean?>(null) }

    // Network selection based on build flavor (devnet=nile, mainnet=tron-mainnet)
    val networkId = BuildConfig.TRON_NETWORK

    // Check onboarding state when screen loads
    LaunchedEffect(Unit) {
        isOnboardingCompleted = profileStore.isOnboardingCompleted()
        Log.d(TAG, "ONBOARD|INIT|completed=$isOnboardingCompleted")
    }

    fun validateAddress(input: String): Boolean {
        return TronAddressUtils.isValidAddress(input)
    }

    fun connectWallet() {
        if (!validateAddress(address)) {
            isValid = false
            return
        }

        isSaving = true
        scope.launch {
            // Check onboarding state BEFORE saving (to know if this is an existing user)
            val wasOnboarded = profileStore.isOnboardingCompleted()

            val connection = RailConnection(
                railId = TronPaymentRail.RAIL_ID,
                networkId = networkId,
                accountId = address.trim(),
                connectedAt = System.currentTimeMillis(),
                sessionId = null  // Manual entry, no WC session
            )
            profileStore.connectRail(connection)

            Log.i(TAG, "TRON_CONNECT|SAVE|address=${address.take(12)}...|networkId=$networkId")
            Log.i(TAG, "CONFIG|CONNECTED_RAILS|tron=${address.trim()}")
            Log.i(TAG, "ONBOARD|STATE|completed=$wasOnboarded|reason=${if (wasOnboarded) "existing_user_adding_rail" else "new_user_onboarding"}")
            isOnboardingCompleted = wasOnboarded
            isConnected = true
            isSaving = false
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.bgCanvas)
            .statusBarsPadding()
            .navigationBarsPadding()
            .verticalScroll(rememberScrollState())
    ) {
        // Header
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    start = WinoSpacing.LG,
                    end = WinoSpacing.LG,
                    top = WinoSpacing.MD,
                    bottom = WinoSpacing.MD
                ),
            verticalArrangement = Arrangement.spacedBy(WinoSpacing.XXS)
        ) {
            Text(
                text = stringResource(R.string.tron_connect_title),
                style = WinoTypography.h1Medium,
                color = colors.textPrimary
            )

            Text(
                text = stringResource(R.string.tron_connect_subtitle),
                style = WinoTypography.body,
                color = colors.textSecondary
            )
        }

        // Content
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(WinoSpacing.LG),
            verticalArrangement = Arrangement.spacedBy(WinoSpacing.MD)
        ) {
            if (!isConnected) {
                // Network indicator
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(WinoRadius.MD))
                        .background(colors.bgSurfaceAlt)
                        .padding(WinoSpacing.SM),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(WinoSpacing.SM)
                ) {
                    PhosphorIcons.Info(size = 20.dp, color = colors.brandPrimary)
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.tron_network_hint),
                            style = WinoTypography.smallMedium,
                            color = colors.textPrimary
                        )
                        Text(
                            text = stringResource(R.string.tron_paste_hint),
                            style = WinoTypography.micro,
                            color = colors.textSecondary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(WinoSpacing.SM))

                // Address input
                Text(
                    text = stringResource(R.string.tron_address_label),
                    style = WinoTypography.smallMedium,
                    color = colors.textPrimary,
                    modifier = Modifier.padding(bottom = WinoSpacing.XS)
                )

                OutlinedTextField(
                    value = address,
                    onValueChange = { newValue ->
                        address = newValue
                        // Clear validation error on typing
                        if (isValid == false) {
                            isValid = null
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = {
                        Text(
                            text = stringResource(R.string.tron_address_placeholder),
                            style = WinoTypography.body,
                            color = colors.textMuted
                        )
                    },
                    isError = isValid == false,
                    supportingText = if (isValid == false) {
                        {
                            Text(
                                text = stringResource(R.string.tron_address_invalid),
                                color = colors.stateError
                            )
                        }
                    } else null,
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Text,
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(
                        onDone = {
                            focusManager.clearFocus()
                            if (address.isNotBlank()) {
                                isValid = validateAddress(address)
                            }
                        }
                    ),
                    shape = RoundedCornerShape(WinoRadius.MD),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = colors.brandPrimary,
                        unfocusedBorderColor = colors.borderDefault,
                        errorBorderColor = colors.stateError,
                        focusedTextColor = colors.textPrimary,
                        unfocusedTextColor = colors.textPrimary,
                        cursorColor = colors.brandPrimary,
                        focusedContainerColor = colors.bgSurface,
                        unfocusedContainerColor = colors.bgSurface,
                        errorContainerColor = colors.bgSurface
                    )
                )

                Spacer(modifier = Modifier.height(WinoSpacing.MD))

                // Connect button
                WinoButton(
                    text = stringResource(R.string.tron_connect_button),
                    onClick = { connectWallet() },
                    modifier = Modifier.fillMaxWidth(),
                    variant = WinoButtonVariant.Primary,
                    size = WinoButtonSize.Large,
                    enabled = address.isNotBlank() && !isSaving,
                    loading = isSaving
                )
            } else {
                // Connected state
                TronConnectedCard(address = address)

                Spacer(modifier = Modifier.height(WinoSpacing.MD))

                // Continue button - navigate based on onboarding state
                WinoButton(
                    text = stringResource(R.string.continue_btn),
                    onClick = {
                        if (isOnboardingCompleted == true) {
                            // Existing user adding rail from Settings → go back
                            Log.i(TAG, "ONBOARD|NAV|action=back_to_settings|reason=already_onboarded")
                            onBack()
                        } else {
                            // New user → continue onboarding
                            Log.i(TAG, "ONBOARD|NAV|action=continue_onboarding|reason=new_user")
                            onConnected(address)
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    variant = WinoButtonVariant.Primary,
                    size = WinoButtonSize.Large
                )

                Spacer(modifier = Modifier.height(WinoSpacing.SM))

                // Change address button
                WinoButton(
                    text = stringResource(R.string.switch_wallet),
                    onClick = {
                        isConnected = false
                        address = ""
                        isValid = null
                    },
                    modifier = Modifier.fillMaxWidth(),
                    variant = WinoButtonVariant.Secondary,
                    size = WinoButtonSize.Large
                )
            }
        }
    }
}

@Composable
private fun TronConnectedCard(address: String) {
    val colors = WinoTheme.colors

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(WinoRadius.LG))
            .background(colors.stateSuccessSoft)
            .padding(WinoSpacing.MD),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(WinoSpacing.SM)
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(colors.stateSuccess),
            contentAlignment = Alignment.Center
        ) {
            PhosphorIcons.CheckCircle(
                size = 28.dp,
                color = colors.bgCanvas
            )
        }

        Text(
            text = "TRON Wallet Connected",
            style = WinoTypography.bodyMedium,
            color = colors.stateSuccess
        )

        Text(
            text = TronAddressUtils.formatForDisplay(address),
            style = WinoTypography.small,
            color = colors.textMuted,
            textAlign = TextAlign.Center
        )
    }
}
