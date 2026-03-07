package com.winopay.data.profile

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.winopay.payments.PaymentRailFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

/**
 * SINGLE SOURCE OF TRUTH for merchant configuration.
 *
 * UNIFIED DATA MODEL:
 * - connectedRails: Map<railId, RailConnection> - ALL connected rails (not just active)
 * - activeRailId: String? - which rail is currently active for POS
 * - onboardingCompleted: Boolean - GLOBAL flag (independent of rails)
 * - businessProfile: BusinessProfile - GLOBAL business identity
 * - enabledMethods: Set<String> - "{railId}:{tokenId}" format
 *
 * CRITICAL RULES:
 * 1. Connecting a rail NEVER resets onboardingCompleted or businessProfile
 * 2. Connecting a rail NEVER removes other rails
 * 3. If enabledMethods is empty → enable all supported tokens for all connected rails
 *
 * AUTO BACKUP:
 * This DataStore is configured for Android Auto Backup via backup_rules.xml.
 */
private val Context.merchantProfileDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "merchant_profile"
)

class MerchantProfileStore(private val context: Context) {

    companion object {
        private const val TAG = "MerchantProfileStore"
        private const val LOGO_DIR = "profile_logos"

        // ═══════════════════════════════════════════════════════════════════════════
        // UNIFIED MODEL KEYS (v2)
        // ═══════════════════════════════════════════════════════════════════════════

        // Connected rails (stored as Set of serialized RailConnection strings)
        private val KEY_CONNECTED_RAILS = stringSetPreferencesKey("connected_rails_v2")

        // Active rail ID (just the railId, connection details in connectedRails)
        private val KEY_ACTIVE_RAIL_ID = stringPreferencesKey("active_rail_id_v2")

        // Onboarding completed (GLOBAL, independent of rails)
        private val KEY_ONBOARDING_COMPLETED = booleanPreferencesKey("onboarding_completed_v2")

        // Business profile (GLOBAL)
        private val KEY_BUSINESS_NAME = stringPreferencesKey("business_name_v2")
        private val KEY_BUSINESS_LOGO_PATH = stringPreferencesKey("business_logo_path_v2")
        private val KEY_BUSINESS_UPDATED_AT = longPreferencesKey("business_updated_at_v2")

        // Enabled payment methods (Set of "{railId}:{tokenId}")
        private val KEY_ENABLED_METHODS = stringSetPreferencesKey("enabled_methods_v2")

        // ═══════════════════════════════════════════════════════════════════════════
        // LEGACY KEYS (for migration)
        // ═══════════════════════════════════════════════════════════════════════════
        private val KEY_ACTIVE_RAIL_ID_LEGACY = stringPreferencesKey("active_rail_id")
        private val KEY_ACTIVE_NETWORK_ID_LEGACY = stringPreferencesKey("active_network_id")
        private val KEY_ACTIVE_ACCOUNT_ID_LEGACY = stringPreferencesKey("active_account_id")
        private const val PREFIX_BUSINESS_NAME_LEGACY = "business_name:"
    }

    // ═══════════════════════════════════════════════════════════════════════════════
    // CONNECTED RAILS
    // ═══════════════════════════════════════════════════════════════════════════════

    /**
     * Connect a new rail WITHOUT removing existing rails.
     *
     * CRITICAL: This ADDS to connectedRails, never replaces.
     * Existing rails, businessProfile, and onboardingCompleted are preserved.
     *
     * @param connection Rail connection to add
     * @param setAsActive Whether to set this as the active rail for POS
     */
    suspend fun connectRail(connection: RailConnection, setAsActive: Boolean = true): Boolean {
        return try {
            context.merchantProfileDataStore.edit { prefs ->
                // Get existing rails
                val existingRails = prefs[KEY_CONNECTED_RAILS]?.toMutableSet() ?: mutableSetOf()

                // Remove any existing connection for this railId (update, not duplicate)
                existingRails.removeAll { it.startsWith("${connection.railId}|") }

                // Add new connection
                existingRails.add(RailConnection.serialize(connection))
                prefs[KEY_CONNECTED_RAILS] = existingRails

                // Set as active if requested
                if (setAsActive) {
                    prefs[KEY_ACTIVE_RAIL_ID] = connection.railId
                }
            }

            Log.i(TAG, "RAIL|CONNECT|railId=${connection.railId}|networkId=${connection.networkId}|account=${connection.accountId.take(12)}...|setAsActive=$setAsActive")
            true
        } catch (e: Exception) {
            Log.e(TAG, "RAIL|CONNECT|FAIL|error=${e.message}", e)
            false
        }
    }

    /**
     * Disconnect a rail.
     *
     * If disconnecting the active rail, activeRailId becomes null.
     * Other rails, businessProfile, and onboardingCompleted are preserved.
     */
    suspend fun disconnectRail(railId: String): Boolean {
        return try {
            context.merchantProfileDataStore.edit { prefs ->
                val existingRails = prefs[KEY_CONNECTED_RAILS]?.toMutableSet() ?: mutableSetOf()
                existingRails.removeAll { it.startsWith("$railId|") }
                prefs[KEY_CONNECTED_RAILS] = existingRails

                // Clear active if this was the active rail
                if (prefs[KEY_ACTIVE_RAIL_ID] == railId) {
                    prefs.remove(KEY_ACTIVE_RAIL_ID)
                }
            }

            Log.i(TAG, "RAIL|DISCONNECT|railId=$railId")
            true
        } catch (e: Exception) {
            Log.e(TAG, "RAIL|DISCONNECT|FAIL|error=${e.message}", e)
            false
        }
    }

    /**
     * Get all connected rails.
     */
    suspend fun getConnectedRails(): Map<String, RailConnection> {
        return try {
            val prefs = context.merchantProfileDataStore.data.first()
            parseConnectedRails(prefs)
        } catch (e: Exception) {
            Log.e(TAG, "RAIL|GET_ALL|ERROR|error=${e.message}", e)
            emptyMap()
        }
    }

    /**
     * Observe all connected rails as Flow.
     */
    fun observeConnectedRails(): Flow<Map<String, RailConnection>> {
        return context.merchantProfileDataStore.data.map { prefs ->
            parseConnectedRails(prefs)
        }
    }

    /**
     * Get connection for a specific rail.
     */
    suspend fun getRailConnection(railId: String): RailConnection? {
        return getConnectedRails()[railId]
    }

    private fun parseConnectedRails(prefs: Preferences): Map<String, RailConnection> {
        val railStrings = prefs[KEY_CONNECTED_RAILS] ?: emptySet()
        return railStrings.mapNotNull { RailConnection.deserialize(it) }
            .associateBy { it.railId }
    }

    // ═══════════════════════════════════════════════════════════════════════════════
    // ACTIVE RAIL
    // ═══════════════════════════════════════════════════════════════════════════════

    /**
     * Data class for active rail information.
     * For backward compatibility with existing code.
     */
    data class ActiveRail(
        val railId: String,
        val networkId: String,
        val accountId: String
    )

    /**
     * Set active rail for POS.
     *
     * @param railId The rail ID to activate (must be in connectedRails)
     * @return true if successful, false if rail not connected
     */
    suspend fun setActiveRail(railId: String): Boolean {
        val connection = getRailConnection(railId)
        if (connection == null) {
            Log.w(TAG, "RAIL|SET_ACTIVE|FAIL|railId=$railId not connected")
            return false
        }

        return try {
            context.merchantProfileDataStore.edit { prefs ->
                prefs[KEY_ACTIVE_RAIL_ID] = railId
            }
            Log.i(TAG, "RAIL|SET_ACTIVE|railId=$railId|networkId=${connection.networkId}|account=${connection.accountId.take(12)}...")
            true
        } catch (e: Exception) {
            Log.e(TAG, "RAIL|SET_ACTIVE|FAIL|error=${e.message}", e)
            false
        }
    }

    /**
     * Get current active rail.
     *
     * @return ActiveRail or null if not set
     */
    suspend fun getActiveRail(): ActiveRail? {
        return try {
            val prefs = context.merchantProfileDataStore.data.first()
            getActiveRailFromPrefs(prefs)
        } catch (e: Exception) {
            Log.e(TAG, "RAIL|GET_ACTIVE|ERROR|error=${e.message}", e)
            null
        }
    }

    /**
     * Observe active rail changes as Flow.
     */
    fun observeActiveRail(): Flow<ActiveRail?> {
        return context.merchantProfileDataStore.data.map { prefs ->
            getActiveRailFromPrefs(prefs)
        }
    }

    private fun getActiveRailFromPrefs(prefs: Preferences): ActiveRail? {
        val activeRailId = prefs[KEY_ACTIVE_RAIL_ID] ?: return null
        val rails = parseConnectedRails(prefs)
        val connection = rails[activeRailId] ?: return null

        return ActiveRail(
            railId = connection.railId,
            networkId = connection.networkId,
            accountId = connection.accountId
        )
    }

    /**
     * Check if an active rail is configured.
     */
    suspend fun hasActiveRail(): Boolean {
        return getActiveRail() != null
    }

    /**
     * Legacy method for backward compatibility.
     * Use connectRail() instead for new code.
     */
    suspend fun saveActiveRail(railId: String, networkId: String, accountId: String): Boolean {
        val connection = RailConnection(
            railId = railId,
            networkId = networkId,
            accountId = accountId
        )
        return connectRail(connection, setAsActive = true)
    }

    // ═══════════════════════════════════════════════════════════════════════════════
    // ONBOARDING STATE (GLOBAL)
    // ═══════════════════════════════════════════════════════════════════════════════

    /**
     * Check if onboarding is completed.
     *
     * CRITICAL: This is GLOBAL and independent of rails.
     * Connecting/disconnecting rails does NOT affect this flag.
     */
    suspend fun isOnboardingCompleted(): Boolean {
        return try {
            val prefs = context.merchantProfileDataStore.data.first()
            prefs[KEY_ONBOARDING_COMPLETED] ?: false
        } catch (e: Exception) {
            Log.e(TAG, "ONBOARDING|CHECK|ERROR|error=${e.message}", e)
            false
        }
    }

    /**
     * Observe onboarding state as Flow.
     */
    fun observeOnboardingCompleted(): Flow<Boolean> {
        return context.merchantProfileDataStore.data.map { prefs ->
            prefs[KEY_ONBOARDING_COMPLETED] ?: false
        }
    }

    /**
     * Mark onboarding as completed.
     *
     * Called once after initial setup is done.
     * Rails can be added/removed without affecting this.
     */
    suspend fun setOnboardingCompleted(completed: Boolean = true): Boolean {
        return try {
            context.merchantProfileDataStore.edit { prefs ->
                prefs[KEY_ONBOARDING_COMPLETED] = completed
            }
            Log.i(TAG, "ONBOARDING|SET|completed=$completed")
            true
        } catch (e: Exception) {
            Log.e(TAG, "ONBOARDING|SET|FAIL|error=${e.message}", e)
            false
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════════
    // BUSINESS PROFILE (GLOBAL)
    // ═══════════════════════════════════════════════════════════════════════════════

    /**
     * Save business profile.
     *
     * GLOBAL: Same business identity for all rails.
     * Does NOT affect connectedRails or onboardingCompleted.
     *
     * @param businessName Business name
     * @param logoUri Optional content:// URI to copy to internal storage
     */
    suspend fun saveBusinessProfile(businessName: String, logoUri: Uri? = null): Boolean {
        return try {
            var localLogoPath: String? = null

            // Copy logo to internal storage if URI provided
            if (logoUri != null) {
                localLogoPath = copyLogoToInternal(logoUri, "global")
                if (localLogoPath != null) {
                    Log.i(TAG, "PROFILE|LOGO_COPY|OK|path=$localLogoPath")
                } else {
                    Log.w(TAG, "PROFILE|LOGO_COPY|FAIL|uri=$logoUri")
                }
            }

            context.merchantProfileDataStore.edit { prefs ->
                prefs[KEY_BUSINESS_NAME] = businessName
                if (localLogoPath != null) {
                    prefs[KEY_BUSINESS_LOGO_PATH] = localLogoPath
                }
                prefs[KEY_BUSINESS_UPDATED_AT] = System.currentTimeMillis()
            }

            Log.i(TAG, "PROFILE|SAVE|businessName=$businessName|hasLogo=${localLogoPath != null}")
            true
        } catch (e: Exception) {
            Log.e(TAG, "PROFILE|SAVE|FAIL|error=${e.message}", e)
            false
        }
    }

    /**
     * Get business profile.
     */
    suspend fun getBusinessProfile(): BusinessProfile? {
        return try {
            val prefs = context.merchantProfileDataStore.data.first()
            getBusinessProfileFromPrefs(prefs)
        } catch (e: Exception) {
            Log.e(TAG, "PROFILE|GET|ERROR|error=${e.message}", e)
            null
        }
    }

    /**
     * Observe business profile changes as Flow.
     */
    fun observeBusinessProfile(): Flow<BusinessProfile?> {
        return context.merchantProfileDataStore.data.map { prefs ->
            getBusinessProfileFromPrefs(prefs)
        }
    }

    private fun getBusinessProfileFromPrefs(prefs: Preferences): BusinessProfile? {
        val businessName = prefs[KEY_BUSINESS_NAME] ?: return null
        val logoPath = prefs[KEY_BUSINESS_LOGO_PATH]?.takeIf { File(it).exists() }
        val updatedAt = prefs[KEY_BUSINESS_UPDATED_AT] ?: System.currentTimeMillis()

        return BusinessProfile(
            businessName = businessName,
            logoLocalPath = logoPath,
            updatedAt = updatedAt
        )
    }

    // ═══════════════════════════════════════════════════════════════════════════════
    // ENABLED METHODS
    // ═══════════════════════════════════════════════════════════════════════════════

    /**
     * Get enabled payment methods.
     *
     * Format: "{railId}:{tokenId}" (e.g., "solana:EPjFWdd5AufqSSqeM2qN1xzybapC8G4wEGGkZwyTDt1v")
     *
     * If empty set is returned → enable all supported tokens for all connected rails (default behavior).
     */
    suspend fun getEnabledMethods(): Set<String> {
        return try {
            val prefs = context.merchantProfileDataStore.data.first()
            prefs[KEY_ENABLED_METHODS] ?: emptySet()
        } catch (e: Exception) {
            Log.e(TAG, "METHODS|GET|ERROR|error=${e.message}", e)
            emptySet()
        }
    }

    /**
     * Observe enabled methods as Flow.
     */
    fun observeEnabledMethods(): Flow<Set<String>> {
        return context.merchantProfileDataStore.data.map { prefs ->
            prefs[KEY_ENABLED_METHODS] ?: emptySet()
        }
    }

    /**
     * Set enabled payment methods.
     *
     * @param methods Set of "{railId}:{tokenId}" strings
     */
    suspend fun setEnabledMethods(methods: Set<String>): Boolean {
        return try {
            context.merchantProfileDataStore.edit { prefs ->
                prefs[KEY_ENABLED_METHODS] = methods
            }
            Log.i(TAG, "METHODS|SET|count=${methods.size}|methods=$methods")
            true
        } catch (e: Exception) {
            Log.e(TAG, "METHODS|SET|FAIL|error=${e.message}", e)
            false
        }
    }

    /**
     * Enable a specific method.
     */
    suspend fun enableMethod(railId: String, tokenId: String): Boolean {
        val methodId = "$railId:$tokenId"
        val current = getEnabledMethods().toMutableSet()
        current.add(methodId)
        return setEnabledMethods(current)
    }

    /**
     * Disable a specific method.
     */
    suspend fun disableMethod(railId: String, tokenId: String): Boolean {
        val methodId = "$railId:$tokenId"
        val current = getEnabledMethods().toMutableSet()
        current.remove(methodId)
        return setEnabledMethods(current)
    }

    /**
     * Get all enabled methods for connected rails.
     *
     * CRITICAL: Methods are only considered "enabled" if:
     * 1. The method is in enabledMethods set (or enabledMethods is empty = all enabled)
     * 2. AND the rail is connected
     *
     * If enabledMethods is empty → returns ALL supported tokens for ALL connected rails.
     * This is the DEFAULT behavior: "enable everything supported".
     */
    suspend fun getEffectiveEnabledMethods(): Set<String> {
        val connectedRails = getConnectedRails()
        val connectedRailIds = connectedRails.keys

        Log.i(TAG, "CONFIG|CONNECTED_RAILS|rails=$connectedRailIds")

        val explicit = getEnabledMethods()

        if (explicit.isNotEmpty()) {
            // Filter explicit methods to only include methods for CONNECTED rails
            val filtered = explicit.filter { methodId ->
                val railId = methodId.substringBefore(":")
                val isConnected = railId in connectedRailIds
                if (!isConnected) {
                    Log.d(TAG, "METHODS|FILTER|$methodId|rail_not_connected")
                }
                isConnected
            }.toSet()

            Log.i(TAG, "METHODS|EXPLICIT|total=${explicit.size}|connected=${filtered.size}|filtered=${explicit.size - filtered.size}")
            return filtered
        }

        // Default: enable all supported tokens for all connected rails
        val result = mutableSetOf<String>()

        for ((railId, connection) in connectedRails) {
            try {
                val rail = PaymentRailFactory.getRailByRailId(railId, connection.networkId)
                val tokens = rail.getSupportedTokens().filter { it.isEnabled }
                for (token in tokens) {
                    result.add("$railId:${token.tokenId}")
                }
            } catch (e: Exception) {
                Log.w(TAG, "METHODS|DEFAULT|SKIP|railId=$railId|error=${e.message}")
            }
        }

        Log.i(TAG, "METHODS|DEFAULT|count=${result.size}")
        return result
    }

    // ═══════════════════════════════════════════════════════════════════════════════
    // LEGACY SUPPORT & MIGRATION
    // ═══════════════════════════════════════════════════════════════════════════════

    /**
     * Migrate from legacy data format (v1) to unified model (v2).
     *
     * Called on app startup to ensure data consistency.
     */
    suspend fun migrateFromLegacy(): Boolean {
        return try {
            val prefs = context.merchantProfileDataStore.data.first()

            // Check if migration needed (v2 data not present, legacy data exists)
            val hasV2Data = prefs[KEY_CONNECTED_RAILS] != null || prefs[KEY_ONBOARDING_COMPLETED] != null
            val hasLegacyData = prefs[KEY_ACTIVE_RAIL_ID_LEGACY] != null

            if (hasV2Data || !hasLegacyData) {
                Log.d(TAG, "MIGRATION|SKIP|hasV2=$hasV2Data|hasLegacy=$hasLegacyData")
                return true
            }

            Log.i(TAG, "MIGRATION|START|legacy data found")

            context.merchantProfileDataStore.edit { mutablePrefs ->
                // Migrate active rail to connected rails
                val legacyRailId = mutablePrefs[KEY_ACTIVE_RAIL_ID_LEGACY]
                val legacyNetworkId = mutablePrefs[KEY_ACTIVE_NETWORK_ID_LEGACY]
                val legacyAccountId = mutablePrefs[KEY_ACTIVE_ACCOUNT_ID_LEGACY]

                if (legacyRailId != null && legacyNetworkId != null && legacyAccountId != null) {
                    val connection = RailConnection(
                        railId = legacyRailId,
                        networkId = legacyNetworkId,
                        accountId = legacyAccountId,
                        connectedAt = System.currentTimeMillis()
                    )
                    mutablePrefs[KEY_CONNECTED_RAILS] = setOf(RailConnection.serialize(connection))
                    mutablePrefs[KEY_ACTIVE_RAIL_ID] = legacyRailId

                    Log.i(TAG, "MIGRATION|RAIL|railId=$legacyRailId|networkId=$legacyNetworkId")
                }

                // Migrate business profile from legacy per-rail storage
                val legacyProfileKey = "profile:$legacyRailId:$legacyNetworkId:$legacyAccountId"
                val legacyBusinessName = mutablePrefs[stringPreferencesKey(PREFIX_BUSINESS_NAME_LEGACY + legacyProfileKey)]

                if (legacyBusinessName != null) {
                    mutablePrefs[KEY_BUSINESS_NAME] = legacyBusinessName
                    mutablePrefs[KEY_BUSINESS_UPDATED_AT] = System.currentTimeMillis()

                    // Migrate logo path if exists
                    val legacyLogoPath = mutablePrefs[stringPreferencesKey("logo_path:$legacyProfileKey")]
                    if (legacyLogoPath != null && File(legacyLogoPath).exists()) {
                        mutablePrefs[KEY_BUSINESS_LOGO_PATH] = legacyLogoPath
                    }

                    // Set onboarding completed (if business name exists, onboarding was done)
                    mutablePrefs[KEY_ONBOARDING_COMPLETED] = true

                    Log.i(TAG, "MIGRATION|PROFILE|businessName=$legacyBusinessName")
                }
            }

            Log.i(TAG, "MIGRATION|COMPLETE")
            true
        } catch (e: Exception) {
            Log.e(TAG, "MIGRATION|FAIL|error=${e.message}", e)
            false
        }
    }

    /**
     * Check if any profile exists (for onboarding flow).
     * Combines legacy and v2 checks.
     */
    suspend fun hasAnyProfile(): Boolean {
        return try {
            val prefs = context.merchantProfileDataStore.data.first()
            // Check v2 first
            if (prefs[KEY_BUSINESS_NAME] != null) return true
            // Check legacy
            prefs.asMap().keys.any { it.name.startsWith(PREFIX_BUSINESS_NAME_LEGACY) }
        } catch (e: Exception) {
            false
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════════
    // LOGO HANDLING (internal utility)
    // ═══════════════════════════════════════════════════════════════════════════════

    /**
     * Copy logo from content URI to internal storage.
     */
    private suspend fun copyLogoToInternal(uri: Uri, profileKey: String): String? {
        return withContext(Dispatchers.IO) {
            try {
                val logoDir = File(context.filesDir, LOGO_DIR)
                if (!logoDir.exists()) {
                    logoDir.mkdirs()
                }

                val safeKey = profileKey.replace(":", "_").replace("/", "_")
                val extension = getFileExtension(uri)
                val filename = "logo_${safeKey}_${UUID.randomUUID().toString().take(8)}.$extension"
                val destFile = File(logoDir, filename)

                // Delete old logos for this profile
                logoDir.listFiles()?.filter { it.name.startsWith("logo_${safeKey}_") }?.forEach {
                    it.delete()
                }

                // Copy file
                context.contentResolver.openInputStream(uri)?.use { input ->
                    FileOutputStream(destFile).use { output ->
                        input.copyTo(output)
                    }
                }

                if (destFile.exists() && destFile.length() > 0) {
                    destFile.absolutePath
                } else {
                    null
                }
            } catch (e: Exception) {
                Log.e(TAG, "LOGO|COPY|ERROR|error=${e.message}", e)
                null
            }
        }
    }

    private fun getFileExtension(uri: Uri): String {
        return try {
            val mimeType = context.contentResolver.getType(uri)
            when (mimeType) {
                "image/jpeg" -> "jpg"
                "image/png" -> "png"
                "image/webp" -> "webp"
                else -> "jpg"
            }
        } catch (e: Exception) {
            "jpg"
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════════
    // LEGACY METHODS (deprecated, for backward compatibility)
    // ═══════════════════════════════════════════════════════════════════════════════

    @Deprecated("Use saveBusinessProfile() instead", ReplaceWith("saveBusinessProfile(profile.businessName, logoUri)"))
    suspend fun save(profile: MerchantProfile, logoUri: Uri? = null): Boolean {
        // Save business profile globally
        val businessSaved = saveBusinessProfile(profile.businessName, logoUri)

        // Also connect the rail
        val connection = RailConnection(
            railId = profile.railId,
            networkId = profile.networkId,
            accountId = profile.accountId
        )
        val railConnected = connectRail(connection, setAsActive = true)

        return businessSaved && railConnected
    }

    @Deprecated("Use getBusinessProfile() and getRailConnection() instead")
    suspend fun load(railId: String, networkId: String, accountId: String): MerchantProfile? {
        val profile = getBusinessProfile() ?: return null
        val connection = getRailConnection(railId) ?: return null

        if (connection.networkId != networkId || connection.accountId != accountId) {
            return null
        }

        return MerchantProfile(
            railId = railId,
            networkId = networkId,
            accountId = accountId,
            businessName = profile.businessName,
            logoLocalPath = profile.logoLocalPath,
            updatedAt = profile.updatedAt
        )
    }

    @Deprecated("Use observeBusinessProfile() instead")
    fun observe(railId: String, networkId: String, accountId: String): Flow<MerchantProfile?> {
        return observeBusinessProfile().map { profile ->
            if (profile == null) return@map null

            MerchantProfile(
                railId = railId,
                networkId = networkId,
                accountId = accountId,
                businessName = profile.businessName,
                logoLocalPath = profile.logoLocalPath,
                updatedAt = profile.updatedAt
            )
        }
    }

    @Deprecated("Use disconnectRail() instead")
    suspend fun delete(railId: String, networkId: String, accountId: String) {
        disconnectRail(railId)
    }

    @Deprecated("Use getCurrentProfileKey() on businessProfile instead")
    suspend fun getCurrentProfileKey(): String? {
        val profile = getBusinessProfile() ?: return null
        val activeRail = getActiveRail() ?: return null
        return "profile:${activeRail.railId}:${activeRail.networkId}:${activeRail.accountId}"
    }
}
