package com.winopay.data

import com.winopay.data.local.AppDatabase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import java.util.Calendar
import java.util.TimeZone

/**
 * Dashboard statistics model.
 */
data class DashboardStats(
    val totalRevenueRaw: Long,
    val revenueTodayRaw: Long,
    val invoiceCount: Int,
    val confirmedCount: Int,
    val invoiceCountToday: Int,
    val confirmedCountToday: Int
) {
    companion object {
        val EMPTY = DashboardStats(
            totalRevenueRaw = 0L,
            revenueTodayRaw = 0L,
            invoiceCount = 0,
            confirmedCount = 0,
            invoiceCountToday = 0,
            confirmedCountToday = 0
        )
    }

    /**
     * Total revenue in display units (USDC with 6 decimals).
     */
    val totalRevenue: Double
        get() = totalRevenueRaw.toDouble() / 1_000_000.0

    /**
     * Revenue today in display units.
     */
    val revenueToday: Double
        get() = revenueTodayRaw.toDouble() / 1_000_000.0

    /**
     * Success rate as percentage (0-100).
     */
    val successRate: Double
        get() = if (invoiceCount > 0) {
            (confirmedCount.toDouble() / invoiceCount.toDouble()) * 100.0
        } else {
            0.0
        }

    /**
     * Success rate today as percentage (0-100).
     */
    val successRateToday: Double
        get() = if (invoiceCountToday > 0) {
            (confirmedCountToday.toDouble() / invoiceCountToday.toDouble()) * 100.0
        } else {
            0.0
        }

    /**
     * Format total revenue for display.
     */
    fun formatTotalRevenue(decimals: Int = 2): String {
        return String.format("%.${decimals}f", totalRevenue)
    }

    /**
     * Format revenue today for display.
     */
    fun formatRevenueToday(decimals: Int = 2): String {
        return String.format("%.${decimals}f", revenueToday)
    }

    /**
     * Format success rate for display.
     */
    fun formatSuccessRate(decimals: Int = 1): String {
        return String.format("%.${decimals}f%%", successRate)
    }
}

/**
 * Repository for dashboard statistics.
 */
class StatsRepository(
    private val database: AppDatabase
) {
    private val invoiceDao = database.invoiceDao()

    /**
     * Observe dashboard statistics as a combined Flow.
     */
    fun observeStats(): Flow<DashboardStats> {
        val startOfDay = getStartOfDayMillis()

        return combine(
            invoiceDao.observeTotalRevenue(),
            invoiceDao.observeRevenueToday(startOfDay),
            invoiceDao.observeInvoiceCount(),
            invoiceDao.observeConfirmedCount(),
            invoiceDao.observeInvoiceCountToday(startOfDay),
            invoiceDao.observeConfirmedCountToday(startOfDay)
        ) { values ->
            DashboardStats(
                totalRevenueRaw = values[0] as Long,
                revenueTodayRaw = values[1] as Long,
                invoiceCount = values[2] as Int,
                confirmedCount = values[3] as Int,
                invoiceCountToday = values[4] as Int,
                confirmedCountToday = values[5] as Int
            )
        }
    }

    /**
     * Get start of current day in milliseconds.
     */
    private fun getStartOfDayMillis(): Long {
        val calendar = Calendar.getInstance(TimeZone.getDefault())
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        return calendar.timeInMillis
    }
}
