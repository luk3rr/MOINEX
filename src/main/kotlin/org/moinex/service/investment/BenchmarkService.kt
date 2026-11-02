/*
 * Filename: BenchmarkService.kt
 * Created on: November 1, 2026
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 */

package org.moinex.service.investment

import org.json.JSONObject
import org.moinex.common.ClockProvider
import org.moinex.common.extension.throwIfError
import org.moinex.common.extension.toNoTimeFormat
import org.moinex.common.util.APIUtils
import org.moinex.model.investment.BenchmarkPrice
import org.moinex.repository.investment.BenchmarkPriceRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.math.BigDecimal
import java.time.LocalDate
import java.time.temporal.ChronoUnit

/**
 * Keeps the daily level of the market indexes the portfolio is compared with. Only the last [WINDOW_MONTHS] months are
 * kept up to date, which is as far as the portfolio price history goes.
 */
@Service
class BenchmarkService(
    private val benchmarkPriceRepository: BenchmarkPriceRepository,
    private val clockProvider: ClockProvider,
) {
    companion object {
        const val IBOVESPA = "^BVSP"
        const val WINDOW_MONTHS = 12L
        const val STALE_AFTER_DAYS = 3L
    }

    private val logger = LoggerFactory.getLogger(BenchmarkService::class.java)

    /**
     * Downloads the Ibovespa closes missing from the last [WINDOW_MONTHS] months. A failure is logged and keeps what is
     * stored.
     * @return how many rows were written
     */
    suspend fun syncIbovespa(): Int =
        runCatching {
            val today = clockProvider.today()
            val windowStart = today.minusMonths(WINDOW_MONTHS)
            val latest = benchmarkPriceRepository.findLatestBySymbol(IBOVESPA)?.priceDate

            if (latest != null && ChronoUnit.DAYS.between(latest, today) <= STALE_AFTER_DAYS) return@runCatching 0

            val from = if (latest == null || latest.isBefore(windowStart)) windowStart else latest
            val prices =
                APIUtils
                    .fetchStockPriceHistory(IBOVESPA, from.toNoTimeFormat(), today.plusDays(1).toNoTimeFormat())
                    .throwIfError("Fetch the Ibovespa history")
                    .getJSONArray("prices")

            val existing =
                benchmarkPriceRepository
                    .findBySymbolAndDateRange(IBOVESPA, from, today.plusDays(1))
                    .associateBy { it.priceDate }
            val rows =
                (0 until prices.length()).map { toRow(prices.getJSONObject(it), existing) }

            benchmarkPriceRepository.saveAll(rows)
            logger.info("Synced {} Ibovespa closes starting {}", rows.size, from)

            rows.size
        }.getOrElse {
            logger.error("Failed to sync the Ibovespa history: {}", it.message)
            0
        }

    fun getIbovespa(
        startDate: LocalDate,
        endDate: LocalDate,
    ): List<BenchmarkPrice> = benchmarkPriceRepository.findBySymbolAndDateRange(IBOVESPA, startDate, endDate)

    private fun toRow(
        priceData: JSONObject,
        existing: Map<LocalDate, BenchmarkPrice>,
    ): BenchmarkPrice {
        val date = priceData.getString("date").toNoTimeFormat()
        val price = BigDecimal(priceData.getDouble("price"))

        return existing[date]?.apply { closingPrice = price }
            ?: BenchmarkPrice(symbol = IBOVESPA, priceDate = date, closingPrice = price)
    }
}
