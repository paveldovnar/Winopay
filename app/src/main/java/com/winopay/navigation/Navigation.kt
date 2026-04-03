package com.winopay.navigation

import android.content.Intent
import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavHostController
import android.util.Log
import com.winopay.WinoPayApplication
import kotlinx.coroutines.launch
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.solana.mobilewalletadapter.clientlib.ActivityResultSender
import com.winopay.ui.screens.dashboard.DashboardScreen
import com.winopay.ui.screens.onboarding.AllSetScreen
import com.winopay.ui.screens.onboarding.BusinessProfileSetupScreen
import com.winopay.ui.screens.onboarding.ConnectScreen
import com.winopay.ui.screens.onboarding.CurrencySetupScreen
import com.winopay.ui.screens.onboarding.ReviewBusinessProfileScreen
import com.winopay.ui.screens.onboarding.TronConnectScreen
import com.winopay.ui.screens.onboarding.TronConnectedScreen
import com.winopay.ui.screens.onboarding.LoadingScreen
import com.winopay.ui.screens.PaymentMethodsScreen
import com.winopay.ui.screens.pos.PosFlowHost
import com.winopay.ui.screens.settings.SettingsAppInfoScreen
import com.winopay.ui.screens.settings.SettingsAppInfoDetailsScreen
import com.winopay.ui.screens.settings.SettingsAppearanceScreen
import com.winopay.ui.screens.settings.SettingsBusinessInfoScreen
import com.winopay.ui.screens.settings.SettingsConnectedWalletsScreen
import com.winopay.ui.screens.settings.SettingsCurrencyScreen
import com.winopay.ui.screens.settings.SettingsLanguageScreen
import com.winopay.ui.screens.settings.SettingsScreen
import com.winopay.ui.screens.settings.LogoutConfirmationScreen
import com.winopay.ui.screens.invoice.InvoiceDetailScreen
import com.winopay.ui.screens.transaction.TransactionDetailsScreen
import com.winopay.ui.screens.status.TxStatusScreen
import com.winopay.ui.screens.welcome.WelcomeScreen
import com.winopay.ui.screens.debug.RpcMonitorScreen
import com.winopay.ui.screens.refund.RefundConfirmationScreen
import com.winopay.ui.screens.refund.RefundRedirectScreen
import com.winopay.ui.screens.refund.RefundSentScreen
import com.winopay.ui.screens.refund.RefundFailedScreen
import com.winopay.ui.screens.refund.ManualRefundScreen
import com.winopay.ui.screens.update.UpdateRequiredScreen
import com.winopay.update.UpdateChecker
import com.winopay.update.UpdateViewModel
import androidx.lifecycle.viewmodel.compose.viewModel

sealed class Screen(val route: String) {
    // Onboarding
    data object Welcome : Screen("welcome")
    data object Connect : Screen("connect")
    data object TronConnect : Screen("tron_connect")
    data object TronConnected : Screen("tron_connected/{address}") {
        fun createRoute(address: String) = "tron_connected/$address"
    }

    // Business profile setup (simple, local-only)
    data object BusinessProfileSetup : Screen("profile/setup/{publicKey}") {
        fun createRoute(publicKey: String) = "profile/setup/$publicKey"
    }
    data object ReviewBusinessProfile : Screen("profile/review")

    data object PaymentMethodsSetup : Screen("setup/payment_methods")
    data object CurrencySetup : Screen("setup/currency")
    data object Loading : Screen("loading")
    data object AllSet : Screen("all_set")

    // Main App
    data object Dashboard : Screen("dashboard")
    data object PosFlow : Screen("pos_flow")
    data object InvoiceDetail : Screen("invoice/{invoiceId}") {
        fun createRoute(invoiceId: String) = "invoice/$invoiceId"
    }
    data object TransactionDetails : Screen("transaction/{invoiceId}") {
        fun createRoute(invoiceId: String) = "transaction/$invoiceId"
    }
    data object TxStatus : Screen("status/{type}/{amount}/{signature}") {
        fun createRoute(type: String, amount: String, signature: String?) =
            "status/$type/$amount/${signature ?: "null"}"
    }

    // Settings
    data object Settings : Screen("settings")
    data object SettingsBusinessInfo : Screen("settings/business_info")
    data object SettingsPaymentMethods : Screen("settings/payment_methods")
    data object SettingsConnectedWallets : Screen("settings/connected_wallets")
    data object SettingsCurrency : Screen("settings/currency")
    data object SettingsLanguage : Screen("settings/language")
    data object SettingsAppearance : Screen("settings/appearance")
    data object SettingsAppInfo : Screen("settings/app_info")
    data object SettingsAppInfoDetails : Screen("settings/app_info_details")
    data object LogoutConfirmation : Screen("settings/logout_confirmation")

    // Debug
    data object RpcMonitor : Screen("debug/rpc_monitor")

    // Update
    data object UpdateRequired : Screen("update_required")

    // Refund Flow
    data object RefundConfirmation : Screen("refund/confirm/{invoiceId}") {
        fun createRoute(invoiceId: String) = "refund/confirm/$invoiceId"
    }
    data object RefundRedirect : Screen("refund/redirect/{invoiceId}") {
        fun createRoute(invoiceId: String) = "refund/redirect/$invoiceId"
    }
    data object RefundSent : Screen("refund/sent/{invoiceId}/{txSignature}") {
        fun createRoute(invoiceId: String, txSignature: String?) =
            "refund/sent/$invoiceId/${txSignature ?: "null"}"
    }
    data object RefundFailed : Screen("refund/failed/{invoiceId}/{errorMessage}") {
        fun createRoute(invoiceId: String, errorMessage: String) =
            "refund/failed/$invoiceId/${java.net.URLEncoder.encode(errorMessage, "UTF-8")}"
    }
    data object ManualRefund : Screen("refund/manual/{invoiceId}") {
        fun createRoute(invoiceId: String) = "refund/manual/$invoiceId"
    }
}

@Composable
fun WinoNavHost(
    navController: NavHostController,
    startDestination: String,
    activityResultSender: ActivityResultSender
) {
    // Shared UpdateViewModel across all screens
    val updateViewModel: UpdateViewModel = viewModel()

    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        // ==================== ONBOARDING ====================

        composable(Screen.Welcome.route) {
            WelcomeScreen(
                onGetStarted = {
                    navController.navigate(Screen.Connect.route) {
                        popUpTo(Screen.Welcome.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.Connect.route) {
            ConnectScreen(
                activityResultSender = activityResultSender,
                onConnected = { publicKey ->
                    // After wallet connect, go to simple profile setup
                    navController.navigate(Screen.BusinessProfileSetup.createRoute(publicKey))
                },
                onTronConnect = {
                    // Navigate to TRON connect screen
                    navController.navigate(Screen.TronConnect.route)
                },
                onBack = {
                    navController.popBackStack()
                }
            )
        }

        composable(Screen.TronConnect.route) {
            TronConnectScreen(
                onConnected = { address ->
                    // After TRON wallet connect, show success screen
                    navController.navigate(Screen.TronConnected.createRoute(address)) {
                        // Replace TronConnect with TronConnected so back goes to previous screen
                        popUpTo(Screen.TronConnect.route) { inclusive = true }
                    }
                },
                onBack = {
                    navController.popBackStack()
                }
            )
        }

        composable(
            route = Screen.TronConnected.route,
            arguments = listOf(
                navArgument("address") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val address = backStackEntry.arguments?.getString("address") ?: ""
            val previousRoute = navController.previousBackStackEntry?.destination?.route

            TronConnectedScreen(
                address = address,
                onContinue = {
                    // If we came from onboarding flow (Connect screen), continue to profile setup
                    // Otherwise (from Settings), go back to payment methods
                    val isOnboardingFlow = previousRoute == Screen.Connect.route ||
                            previousRoute == Screen.PaymentMethodsSetup.route

                    if (isOnboardingFlow && previousRoute == Screen.Connect.route) {
                        // New user onboarding - go to profile setup
                        navController.navigate(Screen.BusinessProfileSetup.createRoute(address)) {
                            popUpTo(Screen.TronConnected.route) { inclusive = true }
                        }
                    } else {
                        // Settings flow - go back to payment methods
                        navController.popBackStack()
                    }
                }
            )
        }

        composable(
            route = Screen.BusinessProfileSetup.route,
            arguments = listOf(
                navArgument("publicKey") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val publicKey = backStackEntry.arguments?.getString("publicKey") ?: ""

            BusinessProfileSetupScreen(
                publicKey = publicKey,
                onContinue = {
                    // Go to review screen before payment methods
                    navController.navigate(Screen.ReviewBusinessProfile.route)
                },
                onBack = {
                    navController.popBackStack()
                }
            )
        }

        composable(Screen.ReviewBusinessProfile.route) {
            ReviewBusinessProfileScreen(
                onContinue = {
                    navController.navigate(Screen.PaymentMethodsSetup.route) {
                        popUpTo(Screen.Connect.route) { inclusive = true }
                    }
                },
                onBack = {
                    navController.popBackStack()
                }
            )
        }

        composable(Screen.PaymentMethodsSetup.route) {
            // UNIFIED: Same PaymentMethodsScreen for onboarding
            PaymentMethodsScreen(
                isOnboarding = true,
                onContinue = {
                    navController.navigate(Screen.CurrencySetup.route)
                },
                onConnectSolana = {
                    navController.navigate(Screen.Connect.route)
                },
                onConnectTron = {
                    navController.navigate(Screen.TronConnect.route)
                }
            )
        }

        composable(Screen.CurrencySetup.route) {
            CurrencySetupScreen(
                onContinue = {
                    // Skip Loading screen, go directly to AllSet
                    navController.navigate(Screen.AllSet.route) {
                        popUpTo(Screen.Welcome.route) { inclusive = true }
                    }
                },
                onBack = {
                    navController.popBackStack()
                }
            )
        }

        composable(Screen.Loading.route) {
            LoadingScreen(
                onComplete = {
                    navController.navigate(Screen.AllSet.route) {
                        popUpTo(Screen.Welcome.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.AllSet.route) {
            AllSetScreen(
                onStart = {
                    navController.navigate(Screen.Dashboard.route) {
                        popUpTo(Screen.AllSet.route) { inclusive = true }
                    }
                }
            )
        }

        // ==================== MAIN APP ====================

        composable(Screen.Dashboard.route) {
            val scope = rememberCoroutineScope()
            val app = WinoPayApplication.instance

            DashboardScreen(
                onNewPayment = {
                    navController.navigate(Screen.PosFlow.route) {
                        launchSingleTop = true
                    }
                },
                onSettings = {
                    navController.navigate(Screen.Settings.route) {
                        launchSingleTop = true
                    }
                },
                onInvoiceClick = { invoiceId ->
                    navController.navigate(Screen.TransactionDetails.createRoute(invoiceId))
                },
                onLogout = {
                    scope.launch {
                        app.logout()
                        navController.navigate(Screen.Welcome.route) {
                            popUpTo(Screen.Dashboard.route) { inclusive = true }
                        }
                    }
                }
            )

            // Auto-check for updates on Dashboard start
            com.winopay.update.AppUpdateChecker(
                updateViewModel = updateViewModel,
                onUpdateRequired = {
                    navController.navigate(Screen.UpdateRequired.route) {
                        launchSingleTop = true
                    }
                }
            )
        }

        // State-driven POS flow
        composable(Screen.PosFlow.route) {
            PosFlowHost(
                onExit = {
                    navController.navigate(Screen.Dashboard.route) {
                        popUpTo(Screen.Dashboard.route) { inclusive = true }
                    }
                }
            )
        }

        // Invoice detail (legacy - redirects to transaction details)
        composable(
            route = Screen.InvoiceDetail.route,
            arguments = listOf(
                navArgument("invoiceId") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val invoiceId = backStackEntry.arguments?.getString("invoiceId") ?: ""
            InvoiceDetailScreen(
                invoiceId = invoiceId,
                onBack = { navController.popBackStack() }
            )
        }

        // Transaction details (new Figma-based design)
        composable(
            route = Screen.TransactionDetails.route,
            arguments = listOf(
                navArgument("invoiceId") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val invoiceId = backStackEntry.arguments?.getString("invoiceId") ?: ""
            // Prevent double-click during exit animation
            var isClosing by remember { mutableStateOf(false) }

            // Observe invoice to check railId for refund routing
            val app = WinoPayApplication.instance
            val invoice by app.invoiceRepository.observeInvoice(invoiceId)
                .collectAsState(initial = null)

            TransactionDetailsScreen(
                invoiceId = invoiceId,
                onClose = {
                    if (!isClosing) {
                        isClosing = true
                        Log.d("Navigation", "CLOSE|TransactionDetails|invoiceId=$invoiceId")
                        navController.popBackStack()
                    } else {
                        Log.d("Navigation", "CLOSE|BLOCKED|already_closing|invoiceId=$invoiceId")
                    }
                },
                onRefund = {
                    // Route to ManualRefund for TRON (WalletConnect doesn't support TRON signing)
                    // Route to RefundConfirmation for other rails (Solana, EVM)
                    val isTron = invoice?.railId == "tron"
                    if (isTron) {
                        Log.d("Navigation", "REFUND|MANUAL|railId=tron|invoiceId=$invoiceId")
                        navController.navigate(Screen.ManualRefund.createRoute(invoiceId))
                    } else {
                        Log.d("Navigation", "REFUND|AUTO|railId=${invoice?.railId}|invoiceId=$invoiceId")
                        navController.navigate(Screen.RefundConfirmation.createRoute(invoiceId))
                    }
                },
                onRefundSuccess = { txSignature ->
                    // Navigate to refund sent screen
                    navController.navigate(Screen.RefundSent.createRoute(invoiceId, txSignature)) {
                        popUpTo(Screen.TransactionDetails.route) { inclusive = true }
                    }
                },
                onRefundFailed = { errorMessage ->
                    // Navigate to refund failed screen
                    navController.navigate(Screen.RefundFailed.createRoute(invoiceId, errorMessage)) {
                        popUpTo(Screen.TransactionDetails.route) { inclusive = true }
                    }
                }
            )
        }

        composable(
            route = Screen.TxStatus.route,
            arguments = listOf(
                navArgument("type") { type = NavType.StringType },
                navArgument("amount") { type = NavType.StringType },
                navArgument("signature") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val type = backStackEntry.arguments?.getString("type") ?: "success"
            val amount = backStackEntry.arguments?.getString("amount") ?: "0"
            val signature = backStackEntry.arguments?.getString("signature")?.takeIf { it != "null" }

            TxStatusScreen(
                statusType = type,
                amount = amount,
                signature = signature,
                onDone = {
                    navController.navigate(Screen.Dashboard.route) {
                        popUpTo(Screen.Dashboard.route) { inclusive = true }
                    }
                },
                onNewPayment = {
                    navController.navigate(Screen.PosFlow.route) {
                        popUpTo(Screen.Dashboard.route)
                    }
                }
            )
        }

        // ==================== SETTINGS ====================

        composable(Screen.Settings.route) {
            val context = LocalContext.current

            // URL opener helper
            fun openUrl(url: String) {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                context.startActivity(intent)
            }

            SettingsScreen(
                onBack = { navController.popBackStack() },
                onAccountClick = { navController.navigate(Screen.SettingsBusinessInfo.route) },
                onPaymentMethodsClick = { navController.navigate(Screen.SettingsPaymentMethods.route) },
                onLanguageClick = { navController.navigate(Screen.SettingsLanguage.route) },
                onCurrencyClick = { navController.navigate(Screen.SettingsCurrency.route) },
                onAppearanceClick = { navController.navigate(Screen.SettingsAppearance.route) },
                onAppInfoClick = { navController.navigate(Screen.SettingsAppInfo.route) },
                onTermsClick = { openUrl("https://winobank.com/docs/legal/termsofuse") },
                onPrivacyClick = { openUrl("https://winobank.com/docs/legal/privacypolicy") },
                onRiskDisclosureClick = { openUrl("https://winobank.com/docs/legal/riskdisclosure/pay") },
                onLogout = { navController.navigate(Screen.LogoutConfirmation.route) }
            )
        }

        composable(Screen.SettingsBusinessInfo.route) {
            val scope = rememberCoroutineScope()
            val app = WinoPayApplication.instance

            SettingsBusinessInfoScreen(
                onBack = { navController.popBackStack() },
                onLogout = {
                    scope.launch {
                        app.logout()
                        navController.navigate(Screen.Welcome.route) {
                            popUpTo(Screen.Dashboard.route) { inclusive = true }
                        }
                    }
                }
            )
        }

        composable(Screen.SettingsPaymentMethods.route) {
            // UNIFIED: Same PaymentMethodsScreen for settings
            PaymentMethodsScreen(
                isOnboarding = false,
                onContinue = { navController.popBackStack() },
                onConnectSolana = {
                    navController.navigate(Screen.Connect.route)
                },
                onConnectTron = {
                    navController.navigate(Screen.TronConnect.route)
                }
            )
        }

        composable(Screen.SettingsConnectedWallets.route) {
            SettingsConnectedWalletsScreen(
                onBack = { navController.popBackStack() },
                onConnectSolana = {
                    // Navigate to Solana connect screen
                    navController.navigate(Screen.Connect.route)
                },
                onConnectTron = {
                    // Navigate to TRON connect screen
                    navController.navigate(Screen.TronConnect.route)
                }
            )
        }

        composable(Screen.SettingsCurrency.route) {
            SettingsCurrencyScreen(
                onBack = { navController.popBackStack() }
            )
        }

        composable(Screen.SettingsLanguage.route) {
            SettingsLanguageScreen(
                onBack = { navController.popBackStack() }
            )
        }

        composable(Screen.SettingsAppearance.route) {
            SettingsAppearanceScreen(
                onBack = { navController.popBackStack() }
            )
        }

        composable(Screen.SettingsAppInfo.route) {
            SettingsAppInfoScreen(
                onBack = { navController.popBackStack() }
            )
        }

        composable(Screen.SettingsAppInfoDetails.route) {
            SettingsAppInfoDetailsScreen(
                onBack = { navController.popBackStack() }
            )
        }

        composable(Screen.LogoutConfirmation.route) {
            val scope = rememberCoroutineScope()
            val app = WinoPayApplication.instance

            LogoutConfirmationScreen(
                onLogout = {
                    scope.launch {
                        app.logout()
                        navController.navigate(Screen.Welcome.route) {
                            popUpTo(Screen.Dashboard.route) { inclusive = true }
                        }
                    }
                },
                onCancel = {
                    navController.popBackStack()
                }
            )
        }

        // ==================== REFUND FLOW ====================

        composable(
            route = Screen.RefundConfirmation.route,
            arguments = listOf(
                navArgument("invoiceId") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val invoiceId = backStackEntry.arguments?.getString("invoiceId") ?: ""
            val app = WinoPayApplication.instance
            val invoice by app.invoiceRepository.observeInvoice(invoiceId)
                .collectAsState(initial = null)

            invoice?.let { inv ->
                RefundConfirmationScreen(
                    invoice = inv,
                    onCancel = {
                        navController.popBackStack()
                    },
                    onConfirm = {
                        // Navigate to redirect screen which will initiate the refund
                        navController.navigate(Screen.RefundRedirect.createRoute(invoiceId)) {
                            popUpTo(Screen.RefundConfirmation.route) { inclusive = true }
                        }
                    }
                )
            }
        }

        composable(
            route = Screen.RefundRedirect.route,
            arguments = listOf(
                navArgument("invoiceId") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val invoiceId = backStackEntry.arguments?.getString("invoiceId") ?: ""
            val app = WinoPayApplication.instance
            val scope = rememberCoroutineScope()
            var refundStarted by remember { mutableStateOf(false) }

            RefundRedirectScreen(
                onCancel = {
                    // Go back to transaction details
                    navController.navigate(Screen.TransactionDetails.createRoute(invoiceId)) {
                        popUpTo(Screen.TransactionDetails.route) { inclusive = true }
                    }
                }
            )

            // Initiate refund when screen appears
            LaunchedEffect(invoiceId) {
                if (!refundStarted) {
                    refundStarted = true
                    Log.d("RefundRedirect", "REFUND|START|invoiceId=$invoiceId")

                    val invoice = app.invoiceRepository.getInvoice(invoiceId)
                    if (invoice == null) {
                        navController.navigate(
                            Screen.RefundFailed.createRoute(invoiceId, "Invoice not found")
                        ) {
                            popUpTo(Screen.RefundRedirect.route) { inclusive = true }
                        }
                        return@LaunchedEffect
                    }

                    try {
                        val result = app.refundManager.performRefund(invoice)
                        when (result) {
                            is com.winopay.data.RefundResult.Success -> {
                                Log.d("RefundRedirect", "REFUND|SUCCESS|txId=${result.transactionId}")
                                navController.navigate(
                                    Screen.RefundSent.createRoute(invoiceId, result.transactionId)
                                ) {
                                    popUpTo(Screen.RefundRedirect.route) { inclusive = true }
                                }
                            }
                            is com.winopay.data.RefundResult.Failure -> {
                                Log.d("RefundRedirect", "REFUND|FAILED|error=${result.errorMessage}")
                                navController.navigate(
                                    Screen.RefundFailed.createRoute(invoiceId, result.errorMessage)
                                ) {
                                    popUpTo(Screen.RefundRedirect.route) { inclusive = true }
                                }
                            }
                        }
                    } catch (e: Exception) {
                        Log.e("RefundRedirect", "REFUND|EXCEPTION|error=${e.message}", e)
                        navController.navigate(
                            Screen.RefundFailed.createRoute(invoiceId, e.message ?: "Unknown error")
                        ) {
                            popUpTo(Screen.RefundRedirect.route) { inclusive = true }
                        }
                    }
                }
            }
        }

        composable(
            route = Screen.RefundSent.route,
            arguments = listOf(
                navArgument("invoiceId") { type = NavType.StringType },
                navArgument("txSignature") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val invoiceId = backStackEntry.arguments?.getString("invoiceId") ?: ""
            val txSignature = backStackEntry.arguments?.getString("txSignature")?.takeIf { it != "null" }
            val app = WinoPayApplication.instance
            val context = androidx.compose.ui.platform.LocalContext.current

            RefundSentScreen(
                txSignature = txSignature,
                onOpenExplorer = {
                    txSignature?.let { sig ->
                        val url = com.winopay.solana.SolanaExplorer.buildTxUrl(sig)
                        val intent = android.content.Intent(
                            android.content.Intent.ACTION_VIEW,
                            android.net.Uri.parse(url)
                        )
                        context.startActivity(intent)
                    }
                },
                onGoHome = {
                    navController.navigate(Screen.Dashboard.route) {
                        popUpTo(Screen.Dashboard.route) { inclusive = true }
                    }
                }
            )
        }

        composable(
            route = Screen.RefundFailed.route,
            arguments = listOf(
                navArgument("invoiceId") { type = NavType.StringType },
                navArgument("errorMessage") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val invoiceId = backStackEntry.arguments?.getString("invoiceId") ?: ""
            val errorMessage = try {
                java.net.URLDecoder.decode(
                    backStackEntry.arguments?.getString("errorMessage") ?: "Unknown error",
                    "UTF-8"
                )
            } catch (e: Exception) {
                "Unknown error"
            }

            RefundFailedScreen(
                errorMessage = errorMessage,
                onCancel = {
                    navController.navigate(Screen.Dashboard.route) {
                        popUpTo(Screen.Dashboard.route) { inclusive = true }
                    }
                },
                onTryAgain = {
                    // Go back to confirmation to retry
                    navController.navigate(Screen.RefundConfirmation.createRoute(invoiceId)) {
                        popUpTo(Screen.RefundFailed.route) { inclusive = true }
                    }
                }
            )
        }

        // Manual Refund (for TRON - WalletConnect doesn't support TRON signing)
        composable(
            route = Screen.ManualRefund.route,
            arguments = listOf(
                navArgument("invoiceId") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val invoiceId = backStackEntry.arguments?.getString("invoiceId") ?: ""
            val app = WinoPayApplication.instance
            val invoice by app.invoiceRepository.observeInvoice(invoiceId)
                .collectAsState(initial = null)
            val scope = rememberCoroutineScope()

            invoice?.let { inv ->
                ManualRefundScreen(
                    invoice = inv,
                    onCancel = {
                        navController.popBackStack()
                    },
                    onConfirm = {
                        // Mark refund as sent manually (user confirmed they sent it)
                        scope.launch {
                            // Update invoice status to REFUND_SENT
                            app.invoiceRepository.markAsRefundSent(
                                invoiceId = invoiceId,
                                refundTxId = "manual_refund_${System.currentTimeMillis()}"
                            )
                            // Navigate to RefundSent screen
                            navController.navigate(Screen.RefundSent.createRoute(invoiceId, null)) {
                                popUpTo(Screen.ManualRefund.route) { inclusive = true }
                            }
                        }
                    }
                )
            }
        }

        // ==================== DEBUG ====================

        composable(Screen.RpcMonitor.route) {
            RpcMonitorScreen(
                onBack = { navController.popBackStack() }
            )
        }

        // ==================== UPDATE ====================

        composable(Screen.UpdateRequired.route) {
            val updateInfo by updateViewModel.updateInfo.collectAsState()

            // Keep local copy to prevent blank screen during navigation
            var localUpdateInfo by remember { mutableStateOf(updateInfo) }
            LaunchedEffect(updateInfo) {
                if (updateInfo != null) {
                    localUpdateInfo = updateInfo
                }
            }

            // If no pending update on entry (navigated directly without update), go back
            LaunchedEffect(Unit) {
                if (updateInfo == null && localUpdateInfo == null) {
                    Log.d("Navigation", "UPDATE|NO_PENDING|navigating_back")
                    navController.popBackStack()
                }
            }

            localUpdateInfo?.let { info ->
                UpdateRequiredScreen(
                    updateInfo = info,
                    onRemindLater = {
                        // Navigate first, then clear state
                        navController.popBackStack()
                        updateViewModel.remindLater()
                    }
                )
            }
        }
    }
}
