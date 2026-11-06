/*
 * Filename: TickerMarketDataService.kt
 * Created on: November 6, 2026
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 */

package org.moinex.service.investment

import org.moinex.model.dto.TickerMetadata
import org.moinex.model.investment.Ticker
import org.moinex.repository.investment.TickerRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.time.LocalDateTime

/**
 * The only writer of a ticker's market data. The price, its moment and the website go to the ticker's columns; the
 * currency and the moment of the last history sync go to its metadata. Everything changes together, so every screen
 * reads the same price and the same moment.
 */
@Service
class TickerMarketDataService(
    private val tickerRepository: TickerRepository,
) {
    /**
     * Stores [price] on [ticker], stamped with [at]. A non-null [website] replaces the ticker's domain and a non-null
     * [currency] replaces the one in the metadata; a null one keeps what is stored, so a partial update never erases
     * what an earlier one learned.
     */
    @Transactional
    fun record(
        ticker: Ticker,
        price: BigDecimal,
        at: LocalDateTime,
        currency: String? = null,
        website: String? = null,
    ): Ticker {
        val previous = read(ticker)

        ticker.currentUnitValue = price
        ticker.lastUpdate = at
        website?.let { ticker.domain = it }
        ticker.metadata = previous.copy(currency = currency ?: previous.currency).toJson()

        return tickerRepository.save(ticker)
    }

    /**
     * Stamps the moment the daily price history of [ticker] was last synced, and keeps the [currency] the script
     * reported when there is one. The price columns are not touched.
     */
    @Transactional
    fun recordHistorySync(
        ticker: Ticker,
        at: LocalDateTime,
        currency: String? = null,
    ): Ticker {
        val previous = read(ticker)

        ticker.metadata =
            previous
                .copy(
                    currency = currency ?: previous.currency,
                    historyUpdatedAt = at,
                ).toJson()

        return tickerRepository.save(ticker)
    }

    /**
     * Reads the metadata of [ticker]; a ticker without one, or with an unreadable one, has none of it set.
     */
    fun read(ticker: Ticker): TickerMetadata = TickerMetadata.fromJson(ticker.metadata)
}
