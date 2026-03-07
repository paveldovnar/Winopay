package com.winopay.util

import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.text.NumberFormat
import java.util.Currency
import java.util.Locale

/**
 * Utility for consistent money formatting across the app.
 *
 * Rules:
 * - Fiat: Uses locale-aware formatting with currency symbol
 * - Token: Fixed decimals per token type (USDC = 2 display, SOL = 4 display)
 */
object MoneyFormatter {

    /**
     * Format fiat amount with locale-aware formatting.
     *
     * Examples:
     * - formatFiat(1000.50, "USD", Locale.US) -> "$1,000.50"
     * - formatFiat(1000.50, "RUB", Locale("ru", "RU")) -> "1 000,50 ₽"
     * - formatFiat(1000.50, "THB", Locale.US) -> "฿1,000.50"
     *
     * @param amount Amount to format
     * @param currencyCode ISO 4217 currency code (USD, EUR, RUB, etc.)
     * @param locale Locale for number formatting
     * @return Formatted string with currency symbol
     */
    fun formatFiat(
        amount: Double,
        currencyCode: String,
        locale: Locale = Locale.getDefault()
    ): String {
        return try {
            val currency = Currency.getInstance(currencyCode)
            val formatter = NumberFormat.getCurrencyInstance(locale)
            formatter.currency = currency
            formatter.minimumFractionDigits = 2
            formatter.maximumFractionDigits = 2
            formatter.format(amount)
        } catch (e: Exception) {
            // Fallback: simple formatting with currency code
            String.format(locale, "%.2f %s", amount, currencyCode)
        }
    }

    /**
     * Format fiat amount without currency symbol.
     *
     * Examples:
     * - formatFiatAmount(1000.50, Locale.US) -> "1,000.50"
     * - formatFiatAmount(1000.50, Locale("ru", "RU")) -> "1 000,50"
     *
     * @param amount Amount to format
     * @param locale Locale for number formatting
     * @return Formatted number string
     */
    fun formatFiatAmount(
        amount: Double,
        locale: Locale = Locale.getDefault()
    ): String {
        val formatter = NumberFormat.getNumberInstance(locale)
        formatter.minimumFractionDigits = 2
        formatter.maximumFractionDigits = 2
        return formatter.format(amount)
    }

    /**
     * Format token amount with fixed decimals.
     *
     * Examples:
     * - formatToken(10.5, "USDC") -> "10.50 USDC"
     * - formatToken(0.001234, "SOL") -> "0.0012 SOL"
     *
     * @param amount Amount to format
     * @param tokenSymbol Token symbol (USDC, SOL, USDT)
     * @return Formatted string with token symbol
     */
    fun formatToken(
        amount: Double,
        tokenSymbol: String
    ): String {
        val decimals = getTokenDisplayDecimals(tokenSymbol)
        val formatted = formatTokenAmount(amount, tokenSymbol)
        return "$formatted $tokenSymbol"
    }

    /**
     * Format token amount without symbol.
     *
     * @param amount Amount to format
     * @param tokenSymbol Token symbol to determine decimal precision
     * @return Formatted number string
     */
    fun formatTokenAmount(
        amount: Double,
        tokenSymbol: String
    ): String {
        val decimals = getTokenDisplayDecimals(tokenSymbol)
        // Use US locale for consistent decimal separator in crypto
        val symbols = DecimalFormatSymbols(Locale.US)
        val pattern = buildString {
            append("#,##0")
            if (decimals > 0) {
                append(".")
                repeat(decimals) { append("0") }
            }
        }
        val formatter = DecimalFormat(pattern, symbols)
        return formatter.format(amount)
    }

    /**
     * Get display decimals for a token.
     *
     * USDC/USDT: 2 decimals (stablecoins, like fiat)
     * SOL: 4 decimals (native token)
     */
    private fun getTokenDisplayDecimals(tokenSymbol: String): Int {
        return when (tokenSymbol.uppercase()) {
            "USDC", "USDT" -> 2
            "SOL" -> 4
            else -> 2
        }
    }

    /**
     * Format display amount for POS input.
     * Used when user is typing amount.
     *
     * @param rawInput Raw string input from numpad
     * @param locale Locale for formatting
     * @return Formatted display string
     */
    fun formatPosInput(
        rawInput: String,
        locale: Locale = Locale.US
    ): String {
        val value = rawInput.toDoubleOrNull() ?: return "0"
        if (rawInput.contains(".")) {
            val parts = rawInput.split(".")
            val intPart = parts[0].toLongOrNull() ?: 0
            val formatted = NumberFormat.getNumberInstance(locale).format(intPart)
            return "$formatted.${parts.getOrElse(1) { "" }}"
        }
        val longValue = rawInput.toLongOrNull() ?: return "0"
        return NumberFormat.getNumberInstance(locale).format(longValue)
    }

    /**
     * Get currency symbol for a currency code.
     *
     * @param currencyCode ISO 4217 currency code
     * @return Currency symbol or code if not found
     */
    fun getCurrencySymbol(currencyCode: String): String {
        return try {
            Currency.getInstance(currencyCode).symbol
        } catch (e: Exception) {
            currencyCode
        }
    }
}
