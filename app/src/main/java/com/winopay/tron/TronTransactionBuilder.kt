package com.winopay.tron

import android.util.Log

/**
 * Builder for TRON TRC20 transfer transactions.
 *
 * TRON TRANSACTION STRUCTURE:
 * For TRC20 transfers, we use triggerSmartContract with:
 * - owner_address: sender (merchant) address
 * - contract_address: TRC20 token contract (USDT)
 * - function_selector: "transfer(address,uint256)"
 * - parameter: ABI-encoded recipient address + amount
 * - fee_limit: max energy to spend (in SUN)
 *
 * ENCODING:
 * - Addresses: 21 bytes (41 prefix + 20 bytes hex)
 * - For ABI encoding: strip 41 prefix, pad to 32 bytes
 * - Amounts: uint256, pad to 32 bytes
 *
 * REFERENCE:
 * https://developers.tron.network/reference/triggersmartcontract
 */
object TronTransactionBuilder {

    private const val TAG = "TronTxBuilder"

    /** TRC20 transfer method selector */
    const val TRC20_TRANSFER_SELECTOR = "transfer(address,uint256)"

    /** Default fee limit: 30 TRX (enough for TRC20 transfer) */
    const val DEFAULT_FEE_LIMIT_SUN = 30_000_000L

    /**
     * Result of building a transaction.
     */
    sealed class BuildResult {
        /**
         * Successfully built transaction parameters.
         *
         * @property ownerAddressHex Sender address in hex (with 41 prefix)
         * @property contractAddressHex TRC20 contract address in hex (with 41 prefix)
         * @property functionSelector Method selector string
         * @property parameter ABI-encoded parameters (hex string, no 0x prefix)
         * @property feeLimit Fee limit in SUN
         */
        data class Success(
            val ownerAddressHex: String,
            val contractAddressHex: String,
            val functionSelector: String,
            val parameter: String,
            val feeLimit: Long
        ) : BuildResult()

        data class Error(val message: String) : BuildResult()
    }

    /**
     * Build TRC20 transfer transaction parameters.
     *
     * @param ownerAddress Sender address (base58check)
     * @param recipientAddress Recipient address (base58check)
     * @param tokenContract TRC20 token contract (base58check)
     * @param amountMinor Amount in minor units (6 decimals for USDT)
     * @param feeLimit Fee limit in SUN (default: 30 TRX)
     * @return BuildResult with transaction parameters
     */
    fun buildTrc20Transfer(
        ownerAddress: String,
        recipientAddress: String,
        tokenContract: String,
        amountMinor: Long,
        feeLimit: Long = DEFAULT_FEE_LIMIT_SUN
    ): BuildResult {
        Log.d(TAG, "BUILD|TRC20_TRANSFER|owner=${ownerAddress.take(8)}...|to=${recipientAddress.take(8)}...|amount=$amountMinor")

        // Validate addresses
        if (!TronAddressUtils.isValidAddress(ownerAddress)) {
            return BuildResult.Error("Invalid owner address: $ownerAddress")
        }
        if (!TronAddressUtils.isValidAddress(recipientAddress)) {
            return BuildResult.Error("Invalid recipient address: $recipientAddress")
        }
        if (!TronAddressUtils.isValidAddress(tokenContract)) {
            return BuildResult.Error("Invalid token contract: $tokenContract")
        }

        // Validate amount
        if (amountMinor <= 0) {
            return BuildResult.Error("Amount must be positive: $amountMinor")
        }

        // Convert addresses to hex
        val ownerHex = TronAddressUtils.toHexAddress(ownerAddress)
        val recipientHex = TronAddressUtils.toHexAddress(recipientAddress)
        val contractHex = TronAddressUtils.toHexAddress(tokenContract)

        if (ownerHex == null || recipientHex == null || contractHex == null) {
            return BuildResult.Error("Failed to convert addresses to hex")
        }

        // Build ABI-encoded parameter
        // transfer(address,uint256) expects:
        // - address: 32 bytes (20 byte address, left-padded with zeros)
        // - uint256: 32 bytes (amount, left-padded with zeros)
        val parameter = encodeTransferParameter(recipientHex, amountMinor)

        Log.d(TAG, "BUILD|SUCCESS|ownerHex=$ownerHex|contractHex=$contractHex|paramLen=${parameter.length}")

        return BuildResult.Success(
            ownerAddressHex = ownerHex,
            contractAddressHex = contractHex,
            functionSelector = TRC20_TRANSFER_SELECTOR,
            parameter = parameter,
            feeLimit = feeLimit
        )
    }

    /**
     * Encode transfer(address,uint256) parameters.
     *
     * ABI Encoding:
     * - Address: 20 bytes, left-padded to 32 bytes
     * - Amount: uint256, left-padded to 32 bytes
     *
     * @param recipientHex Recipient address in hex (with 41 prefix)
     * @param amountMinor Amount in minor units
     * @return Hex string of encoded parameters (no 0x prefix)
     */
    fun encodeTransferParameter(recipientHex: String, amountMinor: Long): String {
        // Strip 41 prefix from TRON address to get 20-byte address
        val addressBytes = if (recipientHex.startsWith("41")) {
            recipientHex.substring(2)
        } else {
            recipientHex
        }

        // Pad address to 32 bytes (64 hex chars)
        val addressPadded = addressBytes.padStart(64, '0')

        // Convert amount to hex and pad to 32 bytes (64 hex chars)
        val amountHex = amountMinor.toString(16).padStart(64, '0')

        return addressPadded + amountHex
    }

    /**
     * Decode transfer parameter for verification.
     *
     * @param parameter Hex string of encoded parameters
     * @return Pair of (recipientHex, amount) or null if invalid
     */
    fun decodeTransferParameter(parameter: String): Pair<String, Long>? {
        return try {
            if (parameter.length != 128) {
                Log.w(TAG, "Invalid parameter length: ${parameter.length} (expected 128)")
                return null
            }

            // First 64 chars = address (last 40 chars are the actual address)
            val addressPart = parameter.substring(0, 64)
            val addressHex = "41" + addressPart.takeLast(40) // Add TRON prefix

            // Last 64 chars = amount
            val amountPart = parameter.substring(64, 128)
            val amount = amountPart.toLong(16)

            Pair(addressHex, amount)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to decode parameter: ${e.message}")
            null
        }
    }

    /**
     * Calculate required bandwidth for a TRC20 transfer.
     * Estimated value based on typical TRC20 transfer.
     *
     * @return Estimated bandwidth in bytes
     */
    fun estimateBandwidth(): Int {
        // TRC20 transfer typically uses ~300 bytes of bandwidth
        return 350
    }

    /**
     * Calculate required energy for a TRC20 transfer.
     * Estimated value based on typical USDT transfer.
     *
     * @return Estimated energy units
     */
    fun estimateEnergy(): Long {
        // USDT transfer typically uses ~30,000-65,000 energy
        return 65_000L
    }

    /**
     * Calculate fee limit in SUN based on energy estimate and current energy price.
     *
     * @param energyPrice Current energy price in SUN (default: 420 SUN per energy unit)
     * @return Fee limit in SUN
     */
    fun calculateFeeLimit(energyPrice: Long = 420): Long {
        val energy = estimateEnergy()
        // Add 20% buffer for safety
        return (energy * energyPrice * 120) / 100
    }
}
