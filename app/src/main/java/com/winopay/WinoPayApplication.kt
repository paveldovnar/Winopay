package com.winopay

import android.app.Application
import com.winopay.data.CurrencyConverter
import com.winopay.data.InvoiceRepository
import com.winopay.data.worker.DetectionScheduler
import com.winopay.data.RatesRepository
import com.winopay.data.StatsRepository
import com.winopay.data.local.AppDatabase
import com.winopay.data.local.DataStoreManager
import com.winopay.data.pos.PosManager
import com.winopay.solana.balance.BalanceRepository
import com.winopay.solana.rpc.SolanaRpcClient
import com.winopay.solana.wallet.SessionState
import com.winopay.solana.wallet.SolanaSessionManager
import com.winopay.solana.wallet.WalletConnector
import com.winopay.wallet.ReownManager
import com.winopay.wallet.UnifiedWalletConnector
import com.winopay.data.profile.MerchantProfileStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class WinoPayApplication : Application() {

    lateinit var dataStoreManager: DataStoreManager
        private set

    lateinit var ratesRepository: RatesRepository
        private set

    lateinit var posManager: PosManager
        private set

    lateinit var walletConnector: WalletConnector
        private set

    lateinit var sessionManager: SolanaSessionManager
        private set

    lateinit var balanceRepository: BalanceRepository
        private set

    lateinit var database: AppDatabase
        private set

    lateinit var invoiceRepository: InvoiceRepository
        private set

    lateinit var statsRepository: StatsRepository
        private set

    lateinit var unifiedWalletConnector: UnifiedWalletConnector
        private set

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    override fun onCreate() {
        super.onCreate()
        instance = this

        // ━━━━━ STARTUP CONFIG LOG ━━━━━
        android.util.Log.i("WinoPay", "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
        android.util.Log.i("WinoPay", "WINOPAY STARTUP")
        android.util.Log.i("WinoPay", "  cluster:   ${BuildConfig.SOLANA_CLUSTER}")
        android.util.Log.i("WinoPay", "  USDC_MINT: ${BuildConfig.USDC_MINT}")
        android.util.Log.i("WinoPay", "  USDT_MINT: ${BuildConfig.USDT_MINT.ifBlank { "(not configured)" }}")
        android.util.Log.i("WinoPay", "  RPC:       ${BuildConfig.SOLANA_RPC_URL}")
        android.util.Log.i("WinoPay", "  TRON:      ${BuildConfig.TRON_NETWORK}")
        android.util.Log.i("WinoPay", "  REOWN:     ${if (BuildConfig.REOWN_PROJECT_ID.isNotBlank()) "configured" else "not configured"}")
        android.util.Log.i("WinoPay", "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")

        // Initialize Reown (WalletConnect v2) SDK
        // CRITICAL: Must be done early before any wallet connection
        val reownInitialized = ReownManager.initialize(this)
        if (!reownInitialized) {
            android.util.Log.w("WinoPay", "Reown SDK not initialized - WalletConnect disabled")
        }

        dataStoreManager = DataStoreManager(this)
        ratesRepository = RatesRepository(dataStoreManager)
        posManager = PosManager(this)

        // Initialize wallet components
        walletConnector = WalletConnector()
        sessionManager = SolanaSessionManager(walletConnector, dataStoreManager)
        balanceRepository = BalanceRepository(SolanaRpcClient())

        // Initialize database and repositories
        database = AppDatabase.getInstance(this)
        invoiceRepository = InvoiceRepository(database)
        statsRepository = StatsRepository(database)

        // Initialize unified wallet connector (Reown/WalletConnect)
        val profileStore = MerchantProfileStore(this)
        unifiedWalletConnector = UnifiedWalletConnector(this, profileStore)

        // Resume background detection for pending invoices
        applicationScope.launch(Dispatchers.IO) {
            val pendingInvoices = invoiceRepository.getPendingInvoices()
            if (pendingInvoices.isNotEmpty()) {
                DetectionScheduler.resumePendingDetections(
                    context = this@WinoPayApplication,
                    invoices = pendingInvoices
                )
            }
        }

        // Initialize currency converter with cached/network rates
        // CRITICAL: This must complete before any invoice creation
        applicationScope.launch(Dispatchers.IO) {
            android.util.Log.d("WinoPayApplication", "━━━━━ FX INIT START ━━━━━")
            android.util.Log.d("WinoPayApplication", "  Starting CurrencyConverter.initialize()...")

            val startTime = System.currentTimeMillis()
            val success = CurrencyConverter.initialize(ratesRepository)
            val duration = System.currentTimeMillis() - startTime

            android.util.Log.d("WinoPayApplication", "━━━━━ FX INIT COMPLETE ━━━━━")
            android.util.Log.d("WinoPayApplication", "  success: $success")
            android.util.Log.d("WinoPayApplication", "  duration: ${duration}ms")
            android.util.Log.d("WinoPayApplication", "  hasValidRates: ${CurrencyConverter.hasValidRates()}")
            android.util.Log.d("WinoPayApplication", "  snapshot: ${CurrencyConverter.getSnapshotInfo()?.let { "${it.provider}, ${it.date}" } ?: "NULL"}")
            android.util.Log.d("WinoPayApplication", "━━━━━━━━━━━━━━━━━━━━━━━━━━━")

            // Run sanity checks in debug builds
            if (BuildConfig.DEBUG) {
                val passed = CurrencyConverter.runSanityChecks()
                if (!passed) {
                    android.util.Log.e("WinoPayApplication", "⚠️ CurrencyConverter sanity checks FAILED!")
                }
            }
        }

        // Initialize session manager (restores persisted wallet session)
        sessionManager.initialize()

        // Observe session state and update balance repository
        applicationScope.launch {
            sessionManager.sessionState.collectLatest { state ->
                when (state) {
                    is SessionState.Active -> {
                        balanceRepository.setWalletAddress(state.publicKey)
                    }
                    is SessionState.NoSession,
                    is SessionState.SessionError -> {
                        balanceRepository.setWalletAddress(null)
                    }
                    is SessionState.Initializing,
                    is SessionState.Connecting -> {
                        // Wait for operation to complete
                    }
                }
            }
        }
    }

    companion object {
        lateinit var instance: WinoPayApplication
            private set
    }
}
