/*
 * Filename: FundamentalPriceOverlay.kt
 * Created on: November 6, 2026
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 */

package org.moinex.service.investment

import org.json.JSONObject
import java.math.BigDecimal
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

/**
 * Puts the ticker's own current price into the stored fundamental analysis before it is shown or scored. The
 * analysis keeps the price it read when it was fetched (up to seven days ago); the rest of the app reads the
 * ticker's price, so the analysis shows that one too and every screen agrees on a single price.
 */
object FundamentalPriceOverlay {
    private val SECTIONS = listOf("valuation", "price_performance")
    private const val PRICE_KEY = "current_price"

    /**
     * Returns [dataJson] with the current price of the valuation and price performance sections replaced by [price],
     * stamped with [at]. A section the analysis does not have is left out, and a [dataJson] that cannot be read is
     * returned untouched.
     */
    fun apply(
        dataJson: String,
        price: BigDecimal,
        at: LocalDateTime,
    ): String =
        runCatching {
            val root = JSONObject(dataJson)

            SECTIONS.forEach { section ->
                root.optJSONObject(section)?.put(
                    PRICE_KEY,
                    JSONObject()
                        .put("value", price)
                        .put("type", "currency")
                        .put("data_temporality", "real_time")
                        .put(
                            "reference_date",
                            at.truncatedTo(ChronoUnit.SECONDS).format(DateTimeFormatter.ISO_LOCAL_DATE_TIME),
                        ),
                )
            }

            root.toString()
        }.getOrDefault(dataJson)
}
