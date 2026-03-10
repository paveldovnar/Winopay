package com.winopay.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.navigation.NavHostController
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
import com.winopay.ui.screens.onboarding.LoadingScreen
import com.winopay.ui.screens.PaymentMethodsScreen
import com.winopay.ui.screens.pos.PosFlowHost
import com.winopay.ui.screens.settings.SettingsAppInfoScreen
import com.winopay.ui.screens.settings.SettingsAppearanceScreen
import com.winopay.ui.screens.settings.SettingsBusinessInfoScreen
import com.winopay.ui.screens.settings.SettingsConnectedWalletsScreen
import com.winopay.ui.screens.settings.SettingsCurrencyScreen
import com.winopay.ui.screens.settings.SettingsLanguageScreen
import com.winopay.ui.screens.settings.SettingsScreen
import com.winopay.ui.screens.invoice.InvoiceDetailScreen
import com.winopay.ui.screens.status.TxStatusScreen
import com.winopay.ui.screens.welcome.WelcomeScreen
import com.winopay.ui.screens.debug.RpcMonitorScreen

sealed class Screen(val route: String) {
    // Onboarding
    data object Welcome : Screen("welcome")
    data object Connect : Screen("connect")
    data object TronConnect : Screen("tron_connect")

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

    // Debug
    data object RpcMonitor : Screen("debug/rpc_monitor")
}

@Composable
fun WinoNavHost(
    navController: NavHostController,
    startDestination: String,
    activityResultSender: ActivityResultSender
) {
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
                    // After TRON wallet connect, go to profile setup
                    navController.navigate(Screen.BusinessProfileSetup.createRoute(address))
                },
                onBack = {
                    navController.popBackStack()
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
                    navController.navigate(Screen.Loading.route)
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
                    navController.navigate(Screen.PosFlow.route)
                },
                onSettings = {
                    navController.navigate(Screen.Settings.route)
                },
                onInvoiceClick = { invoiceId ->
                    navController.navigate(Screen.InvoiceDetail.createRoute(invoiceId))
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

            // Auto-check for Telegram updates on Dashboard start
            com.winopay.update.AppUpdateChecker()
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

        // Invoice detail
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
            val scope = rememberCoroutineScope()
            val app = WinoPayApplication.instance
            val updateViewModel: com.winopay.update.UpdateViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
            val updateInfo by updateViewModel.updateInfo.collectAsState()
            val isChecking by updateViewModel.isChecking.collectAsState()
            val checkError by updateViewModel.checkError.collectAsState()
            val showNoUpdateDialog by updateViewModel.showNoUpdateDialog.collectAsState()

            SettingsScreen(
                onBack = { navController.popBackStack() },
                onBusinessInfoClick = { navController.navigate(Screen.SettingsBusinessInfo.route) },
                onPaymentMethodsClick = { navController.navigate(Screen.SettingsPaymentMethods.route) },
                onConnectedWalletsClick = { navController.navigate(Screen.SettingsConnectedWallets.route) },
                onCurrencyClick = { navController.navigate(Screen.SettingsCurrency.route) },
                onLanguageClick = { navController.navigate(Screen.SettingsLanguage.route) },
                onAppearanceClick = { navController.navigate(Screen.SettingsAppearance.route) },
                onAppInfoClick = { navController.navigate(Screen.SettingsAppInfo.route) },
                onCheckForUpdatesClick = {
                    updateViewModel.checkForUpdates(
                        botToken = com.winopay.BuildConfig.TG_BOT_TOKEN,
                        chatUsername = com.winopay.BuildConfig.TG_CHAT_ID
                    )
                },
                onRpcMonitorClick = { navController.navigate(Screen.RpcMonitor.route) },
                onLogout = {
                    scope.launch {
                        app.logout()
                        navController.navigate(Screen.Welcome.route) {
                            popUpTo(Screen.Dashboard.route) { inclusive = true }
                        }
                    }
                }
            )

            // Status dialog (checking / no updates / error)
            if (isChecking || checkError != null || showNoUpdateDialog) {
                com.winopay.update.UpdateCheckDialog(
                    isChecking = isChecking,
                    error = checkError,
                    showNoUpdate = showNoUpdateDialog,
                    onDismiss = { updateViewModel.dismissCheckStatus() }
                )
            }

            // Update available dialog
            updateInfo?.let { info ->
                com.winopay.update.UpdateDialog(
                    updateInfo = info,
                    onDismiss = { updateViewModel.dismissUpdate() }
                )
            }
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

        // ==================== DEBUG ====================

        composable(Screen.RpcMonitor.route) {
            RpcMonitorScreen(
                onBack = { navController.popBackStack() }
            )
        }
    }
}
