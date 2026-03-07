plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
}

import java.util.Properties
import java.io.FileInputStream

// Release signing config from environment, gradle.properties, or local.properties
// CI/local: set WINOPAY_KEYSTORE_FILE, WINOPAY_KEYSTORE_PASSWORD, WINOPAY_KEY_ALIAS, WINOPAY_KEY_PASSWORD

// Load local.properties (if exists)
val localProperties = Properties()
val localPropsFile = rootProject.file("local.properties")
if (localPropsFile.exists()) {
    FileInputStream(localPropsFile).use { localProperties.load(it) }
}

fun getPropertyOrEnv(name: String): String? =
    project.findProperty(name)?.toString()
        ?: localProperties.getProperty(name)
        ?: System.getenv(name)

// Keystore config - evaluated once at configuration time
val keystorePath: String? = getPropertyOrEnv("WINOPAY_KEYSTORE_FILE")
val hasKeystore: Boolean = keystorePath != null && file(keystorePath).exists()

println("═══════════════════════════════════════════════════════════")
println("WinoPay Release Signing: hasKeystore=$hasKeystore")
if (hasKeystore) {
    println("  Keystore: $keystorePath")
    println("  Signing config: RELEASE (signed APK)")
} else {
    println("  Signing config: NONE (unsigned APK)")
    println("  Set WINOPAY_KEYSTORE_FILE to enable signing")
}
println("═══════════════════════════════════════════════════════════")

android {
    namespace = "com.winopay"
    compileSdk = 35

    signingConfigs {
        if (hasKeystore) {
            create("release") {
                storeFile = file(keystorePath!!)
                storePassword = getPropertyOrEnv("WINOPAY_KEYSTORE_PASSWORD") ?: ""
                keyAlias = getPropertyOrEnv("WINOPAY_KEY_ALIAS") ?: ""
                keyPassword = getPropertyOrEnv("WINOPAY_KEY_PASSWORD") ?: ""
            }
        }
    }

    defaultConfig {
        applicationId = "com.winopay"
        minSdk = 26
        targetSdk = 35
        versionCode = 6
        versionName = "1.0.5"

        vectorDrawables {
            useSupportLibrary = true
        }

        // In-app update endpoint (debug builds only)
        // Override via UPDATE_ENDPOINT env var or gradle property
        val updateEndpoint = getPropertyOrEnv("UPDATE_ENDPOINT") ?: ""
        buildConfigField("String", "UPDATE_ENDPOINT", "\"$updateEndpoint\"")

        // Reown (WalletConnect v2) Project ID
        // Get from https://cloud.reown.com - set in local.properties or env
        val reownProjectId = getPropertyOrEnv("REOWN_PROJECT_ID") ?: ""
        buildConfigField("String", "REOWN_PROJECT_ID", "\"$reownProjectId\"")

        // Telegram update checker credentials
        val tgBotToken = getPropertyOrEnv("TG_BOT_TOKEN") ?: ""
        val tgChatId = getPropertyOrEnv("TG_CHAT_ID") ?: ""
        buildConfigField("String", "TG_BOT_TOKEN", "\"$tgBotToken\"")
        buildConfigField("String", "TG_CHAT_ID", "\"$tgChatId\"")
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            // Use release signing if keystore is configured
            if (hasKeystore) {
                signingConfig = signingConfigs.getByName("release")
            }
        }
        debug {
            isDebuggable = true
            // Limit to arm64-v8a for debug builds to reduce APK size (~15MB savings)
            // Most modern devices (>95% market) are arm64-v8a
            // For x86 emulators, use ARM translation or build release
            ndk {
                abiFilters.add("arm64-v8a")
            }
        }
    }

    flavorDimensions += "network"
    productFlavors {
        create("devnet") {
            dimension = "network"
            applicationIdSuffix = ".devnet"
            versionNameSuffix = "-devnet"
            buildConfigField("String", "SOLANA_CLUSTER", "\"devnet\"")
            buildConfigField("String", "SOLANA_RPC_URL", "\"https://api.devnet.solana.com\"")
            buildConfigField("String", "SOLANA_RPC_URL_BACKUP", "\"https://api.devnet.solana.com\"")
            // Circle faucet USDC on devnet (faucet.circle.com)
            buildConfigField("String", "USDC_MINT", "\"4zMMC9srt5Ri5X14GAgXhaHii3GnPAEERYPJgZJDncDU\"")
            // Devnet USDT: override via USDT_MINT_DEVNET property/env for testing
            // Default: spl-token-faucet test USDT (https://spl-token-faucet.com)
            val devnetUsdtMint = getPropertyOrEnv("USDT_MINT_DEVNET") ?: "EJwZgeZrdC8TXTQbQBoL6bfuAnFUUy1PVCMB4DYPzVaS"
            buildConfigField("String", "USDT_MINT", "\"$devnetUsdtMint\"")
            // TRON network: Nile testnet for devnet builds
            buildConfigField("String", "TRON_NETWORK", "\"tron-nile\"")
        }
        create("mainnet") {
            dimension = "network"
            buildConfigField("String", "SOLANA_CLUSTER", "\"mainnet-beta\"")
            // Primary RPC: override via SOLANA_RPC_URL_MAINNET property for paid providers (Helius, QuickNode, etc.)
            val rpcUrl = getPropertyOrEnv("SOLANA_RPC_URL_MAINNET") ?: "https://api.mainnet-beta.solana.com"
            buildConfigField("String", "SOLANA_RPC_URL", "\"$rpcUrl\"")
            // Backup RPC for failover (always public endpoint)
            buildConfigField("String", "SOLANA_RPC_URL_BACKUP", "\"https://api.mainnet-beta.solana.com\"")
            buildConfigField("String", "USDC_MINT", "\"EPjFWdd5AufqSSqeM2qN1xzybapC8G4wEGGkZwyTDt1v\"")
            // Official Tether USDT on Solana mainnet
            buildConfigField("String", "USDT_MINT", "\"Es9vMFrzaCERmJfrF4H2FYD4KCoNkY11McCe8BenwNYB\"")
            // TRON network: mainnet for production builds
            buildConfigField("String", "TRON_NETWORK", "\"tron-mainnet\"")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlin {
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
        }
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
            // Reown SDK dependencies conflict resolution
            excludes += "META-INF/versions/9/OSGI-INF/MANIFEST.MF"
            excludes += "META-INF/DEPENDENCIES"
            excludes += "META-INF/LICENSE.md"
            excludes += "META-INF/NOTICE.md"
            // BouncyCastle: exclude Android's stripped BC to avoid ChaCha20Poly1305 conflicts
            excludes += "META-INF/versions/9/module-info.class"
        }
        // Ensure single BouncyCastle provider (Reown's bcprov-jdk18on)
        jniLibs {
            useLegacyPackaging = true
        }
    }
    testOptions {
        unitTests.isReturnDefaultValues = true
    }

    // Lint configuration - don't fail release build on lint errors
    lint {
        abortOnError = false
        checkReleaseBuilds = false
    }
}

dependencies {
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.zxing.core)
    implementation(libs.coil.compose)
    implementation(libs.solana.mwa.clientlib)
    // Reown (WalletConnect v2) for multi-chain wallet connection
    implementation(platform(libs.reown.bom))
    implementation(libs.reown.android.core)
    implementation(libs.reown.sign)
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)
    implementation(libs.androidx.work.runtime.ktx)
    // TRON wallet signing - stub implementation
    // WalletConnect v2 integration pending TronLink support
    debugImplementation(libs.androidx.ui.tooling)
    testImplementation(libs.junit)
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.8.1")
    androidTestImplementation("androidx.room:room-testing:2.8.4")
    androidTestImplementation("androidx.test:runner:1.5.2")
    androidTestImplementation("androidx.test:rules:1.5.0")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
}
