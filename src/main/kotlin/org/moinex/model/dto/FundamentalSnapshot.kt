/*
 * Filename: FundamentalSnapshot.kt
 * Created on: November 1, 2026
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 */

package org.moinex.model.dto

import org.json.JSONObject
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth

enum class MetricType {
    PERCENT,
    CURRENCY,
    RATIO,
    NUMBER,
    ;

    companion object {
        fun fromJson(value: String?): MetricType =
            entries.firstOrNull { it.name.equals(value, ignoreCase = true) } ?: NUMBER
    }
}

enum class DataTemporality {
    REAL_TIME,
    HISTORICAL,
    CALCULATED,
    ;

    companion object {
        fun fromJson(value: String?): DataTemporality =
            entries.firstOrNull { it.name.equals(value, ignoreCase = true) } ?: CALCULATED
    }
}

/**
 * Why a fundamental metric has no value. The names match the `reason` strings written by
 * `get_fundamental_data.py`.
 */
enum class MetricReason {
    NEGATIVE_EPS,
    NEGATIVE_EQUITY,
    NEGATIVE_GROWTH,
    INSUFFICIENT_HISTORY,
    NOT_APPLICABLE,
    DATA_UNAVAILABLE,
    ;

    companion object {
        fun fromJson(value: String?): MetricReason? = entries.firstOrNull { it.name == value }
    }
}

enum class AssetKind {
    COMPANY,
    FUND,
    ETF,
    ;

    companion object {
        fun fromJson(value: String?): AssetKind =
            entries.firstOrNull { it.name.equals(value, ignoreCase = true) } ?: COMPANY
    }
}

enum class EpsSource {
    INFO,
    STATEMENT,
    ;

    companion object {
        fun fromJson(value: String?): EpsSource? = entries.firstOrNull { it.name.equals(value, ignoreCase = true) }
    }
}

data class MonthlyDividend(
    val month: YearMonth,
    val value: BigDecimal,
)

data class FxInfo(
    val financialCurrency: String,
    val rate: BigDecimal,
    val date: LocalDate?,
)

/**
 * A metric of the fundamental analysis. A metric without [value] always carries a [reason], even when the cached
 * JSON predates the reasons, in which case it is [MetricReason.DATA_UNAVAILABLE].
 */
data class FundamentalMetric(
    val value: BigDecimal?,
    val type: MetricType,
    val temporality: DataTemporality,
    val referenceDate: LocalDate?,
    val reason: MetricReason?,
) {
    fun hasValue(): Boolean = value != null

    companion object {
        fun unavailable(type: MetricType = MetricType.NUMBER): FundamentalMetric =
            FundamentalMetric(null, type, DataTemporality.CALCULATED, null, MetricReason.DATA_UNAVAILABLE)

        fun fromJson(json: Any?): FundamentalMetric {
            if (json !is JSONObject) return unavailable()

            val raw = json.opt("value")
            val value =
                if (raw == null || raw == JSONObject.NULL) {
                    null
                } else {
                    runCatching { BigDecimal(raw.toString()) }.getOrNull()
                }
            val reason =
                if (value == null) {
                    MetricReason.fromJson(json.optString("reason", null)) ?: MetricReason.DATA_UNAVAILABLE
                } else {
                    null
                }

            return FundamentalMetric(
                value = value,
                type = MetricType.fromJson(json.optString("type", null)),
                temporality = DataTemporality.fromJson(json.optString("data_temporality", null)),
                referenceDate =
                    json
                        .optString(
                            "reference_date",
                            null,
                        )?.let { runCatching { LocalDate.parse(it) }.getOrNull() },
                reason = reason,
            )
        }
    }
}

/**
 * The fundamental analysis of one ticker, read from the JSON the Python script produces. [metrics] maps a section
 * (profitability, valuation, ...) to its metrics by key.
 */
data class FundamentalSnapshot(
    val symbol: String,
    val kind: AssetKind,
    val currency: String,
    val schemaVersion: Int,
    val metrics: Map<String, Map<String, FundamentalMetric>>,
    val epsSource: EpsSource?,
    val fx: FxInfo?,
    val lastUpdated: LocalDateTime?,
    val dividendHistory: List<MonthlyDividend> = emptyList(),
    val sector: String? = null,
) {
    fun metric(
        section: String,
        key: String,
    ): FundamentalMetric = metrics[section]?.get(key) ?: FundamentalMetric.unavailable()
}
