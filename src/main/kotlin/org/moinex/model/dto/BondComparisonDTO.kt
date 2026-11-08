/*
 * Filename: BondComparisonDTO.kt
 * Created on: November 7, 2026
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 */

package org.moinex.model.dto

import org.moinex.model.investment.Bond
import java.math.BigDecimal
import java.time.YearMonth

/**
 * Period of the comparison between bonds; [ALL] goes from the first purchase of the bonds compared until today.
 */
enum class ComparisonPeriod(
    val months: Int?,
) {
    M3(3),
    M6(6),
    M12(12),
    ALL(null),
}

data class BondComparisonRequest(
    val period: ComparisonPeriod,
    val net: Boolean,
    val includeArchived: Boolean,
)

/**
 * Accumulated return of one bond, day by day, in percent; [resultPct] is the last point.
 */
data class BondLineDTO(
    val bond: Bond,
    val points: List<SeriesPointDTO>,
    val resultPct: BigDecimal,
)

/**
 * The lines of the bonds that could be drawn, the CDI over the same days, and the bonds left out for lack of data.
 */
data class BondComparisonDTO(
    val lines: List<BondLineDTO>,
    val cdi: List<SeriesPointDTO>,
    val excluded: List<Bond>,
)

data class BondMonthlySeriesDTO(
    val bond: Bond,
    val values: Map<YearMonth, BigDecimal>,
)

data class BondMonthlyDTO(
    val months: List<YearMonth>,
    val series: List<BondMonthlySeriesDTO>,
    val excluded: List<Bond>,
)
