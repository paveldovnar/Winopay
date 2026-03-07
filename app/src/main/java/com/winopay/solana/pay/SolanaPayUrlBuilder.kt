package com.winopay.solana.pay

import com.winopay.data.local.InvoiceEntity
import com.winopay.data.local.PaymentCurrency
import java.net.URLEncoder
import java.security.SecureRandom

/**
 * Builds Solana Pay transfer request URLs.
 *
 * Format: solana:<recipient>?amount=<amount>&spl-token=<mint>&reference=<ref>&memo=<memo>&label=<label>&message=<msg>
 *
 * See: https://docs.solanapay.com/spec#transfer-request
 */
object SolanaPayUrlBuilder {

    private const val SOLANA_PAY_SCHEME = "solana:"

    /**
     * Generate a unique reference public key for payment detection.
     * Returns a base58-encoded 32-byte public key.
     */
    fun generateReference(): String {
        val random = SecureRandom()
        val bytes = ByteArray(32)
        random.nextBytes(bytes)
        return base58Encode(bytes)
    }

    /**
     * Build Solana Pay URL from invoice entity.
     */
    fun buildUrl(invoice: InvoiceEntity): String {
        return buildUrl(
            recipient = invoice.recipientAddress,
            amount = invoice.getAmountString(),
            splTokenMint = invoice.splTokenMint,
            reference = invoice.reference,
            memo = invoice.memo,
            label = invoice.label,
            message = invoice.message
        )
    }

    /**
     * Build Solana Pay transfer request URL.
     *
     * @param recipient Merchant wallet address (NOT an ATA)
     * @param amount Amount as decimal string (e.g., "10.50")
     * @param splTokenMint Token mint for SPL transfers (null for native SOL)
     * @param reference Unique public key for payment detection
     * @param memo Optional memo (usually invoiceId)
     * @param label Optional label (business name)
     * @param message Optional message
     */
    fun buildUrl(
        recipient: String,
        amount: String,
        splTokenMint: String? = null,
        reference: String,
        memo: String? = null,
        label: String? = null,
        message: String? = null
    ): String {
        val params = mutableListOf<String>()

        // Amount is required
        params.add("amount=$amount")

        // SPL token mint (for USDC, etc.)
        if (splTokenMint != null) {
            params.add("spl-token=$splTokenMint")
        }

        // Reference for payment detection (required for our flow)
        params.add("reference=$reference")

        // Optional memo
        if (!memo.isNullOrBlank()) {
            params.add("memo=${urlEncode(memo)}")
        }

        // Optional label
        if (!label.isNullOrBlank()) {
            params.add("label=${urlEncode(label)}")
        }

        // Optional message
        if (!message.isNullOrBlank()) {
            params.add("message=${urlEncode(message)}")
        }

        return "$SOLANA_PAY_SCHEME$recipient?${params.joinToString("&")}"
    }

    /**
     * Build URL for USDC payment.
     *
     * USDC amounts are pre-rounded to 1 decimal place (CEILING) for clean
     * customer-facing amounts.
     */
    fun buildUsdcUrl(
        recipient: String,
        amountUsdc: Double,
        usdcMint: String,
        reference: String,
        label: String? = null,
        memo: String? = null
    ): String {
        return buildUrl(
            recipient = recipient,
            amount = String.format("%.1f", amountUsdc),
            splTokenMint = usdcMint,
            reference = reference,
            memo = memo,
            label = label
        )
    }

    /**
     * Build URL for SOL payment.
     */
    fun buildSolUrl(
        recipient: String,
        amountSol: Double,
        reference: String,
        label: String? = null,
        memo: String? = null
    ): String {
        return buildUrl(
            recipient = recipient,
            amount = String.format("%.9f", amountSol),
            splTokenMint = null,
            reference = reference,
            memo = memo,
            label = label
        )
    }

    private fun urlEncode(value: String): String {
        return URLEncoder.encode(value, "UTF-8")
    }
}

/**
 * Base58 alphabet.
 */
private const val BASE58_ALPHABET = "123456789ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz"

/**
 * Encode bytes to Base58 string.
 */
private fun base58Encode(bytes: ByteArray): String {
    var num = java.math.BigInteger(1, bytes)
    val sb = StringBuilder()
    val base = java.math.BigInteger.valueOf(58)

    while (num > java.math.BigInteger.ZERO) {
        val divRem = num.divideAndRemainder(base)
        num = divRem[0]
        sb.insert(0, BASE58_ALPHABET[divRem[1].toInt()])
    }

    // Handle leading zeros
    for (byte in bytes) {
        if (byte.toInt() == 0) {
            sb.insert(0, '1')
        } else {
            break
        }
    }

    return sb.toString()
}
