package com.winopay

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.lifecycle.Lifecycle
import androidx.navigation.compose.rememberNavController
import com.solana.mobilewalletadapter.clientlib.ActivityResultSender
import com.winopay.data.profile.MerchantProfileStore
import com.winopay.navigation.Screen
import com.winopay.navigation.WinoNavHost
import com.winopay.ui.theme.WinoPayTheme
import com.winopay.ui.theme.WinoTheme

class MainActivity : ComponentActivity() {

    companion object {
        private const val TAG = "MainActivity"
    }

    // Must be created before STARTED state to avoid lifecycle crash
    private lateinit var activityResultSender: ActivityResultSender

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Initialize BEFORE setContent to ensure registration happens before RESUMED state
        activityResultSender = ActivityResultSender(this)

        // Defensive check: log if lifecycle is unexpectedly advanced
        val currentState = lifecycle.currentState
        Log.d(TAG, "ActivityResultSender created at lifecycle state: $currentState")
        if (currentState.isAtLeast(Lifecycle.State.STARTED)) {
            Log.e(TAG, "WARNING: ActivityResultSender created after STARTED state ($currentState). This may cause crashes!")
        }

        setContent {
            val app = WinoPayApplication.instance

            // SINGLE SOURCE OF TRUTH: MerchantProfileStore
            val profileStore = remember { MerchantProfileStore(this) }

            // Run migration from legacy data on startup
            LaunchedEffect(Unit) {
                profileStore.migrateFromLegacy()
            }

            // Observe theme mode (still from DataStoreManager - preferences only)
            val themeMode by app.dataStoreManager.themeMode.collectAsState(initial = "dark")

            // SINGLE SOURCE: Observe onboarding from MerchantProfileStore
            val isOnboardingComplete by profileStore.observeOnboardingCompleted().collectAsState(initial = null)

            WinoPayTheme(themeMode = themeMode) {
                val navController = rememberNavController()

                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = WinoTheme.colors.bgCanvas
                ) {
                    // Wait for onboarding state to load
                    isOnboardingComplete?.let { complete ->
                        val startDestination = if (complete) {
                            Screen.Dashboard.route
                        } else {
                            Screen.Welcome.route
                        }

                        Log.d(TAG, "NAVIGATION|START|onboardingComplete=$complete|destination=$startDestination")

                        WinoNavHost(
                            navController = navController,
                            startDestination = startDestination,
                            activityResultSender = activityResultSender
                        )
                    }
                }
            }
        }
    }
}
