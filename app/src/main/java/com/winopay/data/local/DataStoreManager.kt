package com.winopay.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.winopay.data.model.BusinessIdentity
import com.winopay.data.model.RatesSnapshot
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import org.json.JSONObject

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "wino_pay_prefs")

class DataStoreManager(private val context: Context) {

    companion object {
        private val KEY_BUSINESS_NAME = stringPreferencesKey("business_name")
        private val KEY_BUSINESS_LOGO = stringPreferencesKey("business_logo_uri")
        private val KEY_BUSINESS_HANDLE = stringPreferencesKey("business_handle")
        private val KEY_PUBLIC_KEY = stringPreferencesKey("public_key")
        private val KEY_PRIVATE_KEY = stringPreferencesKey("private_key_encrypted")
        private val KEY_CREATED_AT = longPreferencesKey("created_at")
        private val KEY_ONBOARDING_COMPLETE = stringPreferencesKey("onboarding_complete")
        private val KEY_THEME_MODE = stringPreferencesKey("theme_mode")
        private val KEY_CURRENCY = stringPreferencesKey("currency")
        private val KEY_LANGUAGE = stringPreferencesKey("language")
        private val KEY_RATES_CACHE = stringPreferencesKey("rates_cache_json")
        private val KEY_RATES_UPDATED = longPreferencesKey("rates_last_updated")

        // MWA Wallet session keys
        private val KEY_WALLET_PUBLIC_KEY = stringPreferencesKey("wallet_public_key")
        private val KEY_WALLET_AUTH_TOKEN = stringPreferencesKey("wallet_auth_token")
        private val KEY_WALLET_NAME = stringPreferencesKey("wallet_name")
    }

    /**
     * Data class for wallet session.
     */
    data class WalletSession(
        val publicKey: String,
        val authToken: String,
        val walletName: String?
    )

    val businessIdentity: Flow<BusinessIdentity?> = context.dataStore.data.map { prefs ->
        val name = prefs[KEY_BUSINESS_NAME]
        val publicKey = prefs[KEY_PUBLIC_KEY]

        if (name != null && publicKey != null) {
            BusinessIdentity(
                name = name,
                logoUri = prefs[KEY_BUSINESS_LOGO],
                handle = prefs[KEY_BUSINESS_HANDLE] ?: "@$name",
                publicKey = publicKey,
                createdAt = prefs[KEY_CREATED_AT] ?: System.currentTimeMillis()
            )
        } else {
            null
        }
    }

    val isOnboardingComplete: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[KEY_ONBOARDING_COMPLETE] == "true"
    }

    val privateKey: Flow<String?> = context.dataStore.data.map { prefs ->
        prefs[KEY_PRIVATE_KEY]
    }

    suspend fun saveBusinessIdentity(
        name: String,
        logoUri: String?,
        handle: String,
        publicKey: String,
        privateKeyEncrypted: String
    ) {
        context.dataStore.edit { prefs ->
            prefs[KEY_BUSINESS_NAME] = name
            logoUri?.let { prefs[KEY_BUSINESS_LOGO] = it }
            prefs[KEY_BUSINESS_HANDLE] = handle
            prefs[KEY_PUBLIC_KEY] = publicKey
            prefs[KEY_PRIVATE_KEY] = privateKeyEncrypted
            prefs[KEY_CREATED_AT] = System.currentTimeMillis()
            prefs[KEY_ONBOARDING_COMPLETE] = "true"
        }
    }

    suspend fun clearAllData() {
        context.dataStore.edit { prefs ->
            prefs.clear()
        }
    }

    suspend fun updateLogo(logoUri: String) {
        context.dataStore.edit { prefs ->
            prefs[KEY_BUSINESS_LOGO] = logoUri
        }
    }

    // Theme mode: "dark", "light", or "system"
    val themeMode: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[KEY_THEME_MODE] ?: "dark"
    }

    suspend fun setThemeMode(mode: String) {
        context.dataStore.edit { prefs ->
            prefs[KEY_THEME_MODE] = mode
        }
    }

    // Currency code: "USD", "EUR", etc.
    val currency: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[KEY_CURRENCY] ?: "USD"
    }

    suspend fun setCurrency(currency: String) {
        context.dataStore.edit { prefs ->
            prefs[KEY_CURRENCY] = currency
        }
    }

    // Language name: "English", "Spanish", etc.
    val language: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[KEY_LANGUAGE] ?: "English"
    }

    suspend fun setLanguage(language: String) {
        context.dataStore.edit { prefs ->
            prefs[KEY_LANGUAGE] = language
        }
    }

    // Exchange rates caching
    suspend fun saveRatesSnapshot(snapshot: RatesSnapshot) {
        val json = JSONObject().apply {
            put("base", snapshot.base)
            put("date", snapshot.date)
            put("lastUpdated", snapshot.lastUpdated)
            put("provider", snapshot.provider)
            val ratesJson = JSONObject()
            snapshot.rates.forEach { (k, v) -> ratesJson.put(k, v) }
            put("rates", ratesJson)
        }.toString()

        context.dataStore.edit { prefs ->
            prefs[KEY_RATES_CACHE] = json
            prefs[KEY_RATES_UPDATED] = snapshot.lastUpdated
        }
    }

    suspend fun getRatesSnapshot(): RatesSnapshot? {
        val prefs = context.dataStore.data.first()
        val json = prefs[KEY_RATES_CACHE] ?: return null

        return try {
            val obj = JSONObject(json)
            val ratesObj = obj.getJSONObject("rates")
            val rates = mutableMapOf<String, Double>()
            val keys = ratesObj.keys()
            while (keys.hasNext()) {
                val key = keys.next()
                rates[key] = ratesObj.getDouble(key)
            }
            RatesSnapshot(
                base = obj.getString("base"),
                date = obj.getString("date"),
                rates = rates,
                lastUpdated = obj.getLong("lastUpdated"),
                provider = obj.optString("provider", RatesSnapshot.PROVIDER_FRANKFURTER)
            )
        } catch (e: Exception) {
            null
        }
    }

    // ==================== MWA Wallet Session ====================

    /**
     * Save wallet session data for reconnection.
     */
    suspend fun saveWalletSession(
        publicKey: String,
        authToken: String,
        walletName: String?
    ) {
        context.dataStore.edit { prefs ->
            prefs[KEY_WALLET_PUBLIC_KEY] = publicKey
            prefs[KEY_WALLET_AUTH_TOKEN] = authToken
            walletName?.let { prefs[KEY_WALLET_NAME] = it }
        }
    }

    /**
     * Get persisted wallet session data.
     */
    suspend fun getWalletSession(): WalletSession? {
        val prefs = context.dataStore.data.first()
        val publicKey = prefs[KEY_WALLET_PUBLIC_KEY] ?: return null
        val authToken = prefs[KEY_WALLET_AUTH_TOKEN] ?: return null

        return WalletSession(
            publicKey = publicKey,
            authToken = authToken,
            walletName = prefs[KEY_WALLET_NAME]
        )
    }

    /**
     * Clear wallet session data.
     */
    suspend fun clearWalletSession() {
        context.dataStore.edit { prefs ->
            prefs.remove(KEY_WALLET_PUBLIC_KEY)
            prefs.remove(KEY_WALLET_AUTH_TOKEN)
            prefs.remove(KEY_WALLET_NAME)
        }
    }

    /**
     * Check if wallet session exists.
     */
    val hasWalletSession: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[KEY_WALLET_PUBLIC_KEY] != null && prefs[KEY_WALLET_AUTH_TOKEN] != null
    }

    /**
     * Get wallet public key as Flow.
     */
    val walletPublicKey: Flow<String?> = context.dataStore.data.map { prefs ->
        prefs[KEY_WALLET_PUBLIC_KEY]
    }
}
