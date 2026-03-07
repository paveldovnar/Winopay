package com.winopay.solana

import android.util.Base64
import java.security.SecureRandom

/**
 * Utility object for generating cryptographic keypairs.
 * Used for business identity creation during onboarding.
 *
 * Note: These are identifier keys stored locally, not actual Solana wallet keys.
 * Real wallet connections use MWA (Mobile Wallet Adapter).
 */
object KeypairGenerator {

    /**
     * Generate a random keypair for business identity.
     *
     * @return Pair of (publicKey, privateKey) as Base64-encoded strings
     */
    fun generateKeypair(): Pair<String, String> {
        val seed = ByteArray(32)
        SecureRandom().nextBytes(seed)

        val publicKey = Base64.encodeToString(seed, Base64.NO_WRAP)
        val privateKey = Base64.encodeToString(seed, Base64.NO_WRAP)

        return Pair(publicKey, privateKey)
    }
}
