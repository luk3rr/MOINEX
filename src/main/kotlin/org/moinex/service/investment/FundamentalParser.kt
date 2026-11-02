/*
 * Filename: FundamentalParser.kt
 * Created on: November 1, 2026
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 */

package org.moinex.service.investment

import org.json.JSONObject
import org.moinex.model.dto.AssetKind
import org.moinex.model.dto.EpsSource
import org.moinex.model.dto.FundamentalMetric
import org.moinex.model.dto.FundamentalSnapshot
import org.moinex.model.dto.FxInfo
import org.moinex.model.dto.MonthlyDividend
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth

/**
 * Reads the JSON stored in `fundamental_analysis.data_json`. It accepts the current schema and the one cached before
 * `schema_version` existed: a key that is absent or null becomes a metric with
 * [org.moinex.model.dto.MetricReason.DATA_UNAVAILABLE].
 */
object FundamentalParser {
    const val SECTION_GROWTH = "growth"
    const val SECTION_VALUATION = "valuation"
    const val SECTION_INCOME = "income"
    const val REVENUE_GROWTH = "revenue_growth"
    private const val DIVIDEND_HISTORY = "dividends_12m"

    private const val LEGACY_SCHEMA_VERSION = 1

    private val SECTIONS =
        listOf(
            "profitability",
            SECTION_GROWTH,
            "debt",
            "efficiency",
            "cash_generation",
            SECTION_VALUATION,
            "price_performance",
            SECTION_INCOME,
        )

    fun parse(dataJson: String): FundamentalSnapshot = parse(JSONObject(dataJson))

    fun parse(json: JSONObject): FundamentalSnapshot {
        val valuation = json.optJSONObject(SECTION_VALUATION)

        return FundamentalSnapshot(
            symbol = json.optString("symbol", ""),
            kind = AssetKind.fromJson(json.optString("asset_kind", null)),
            currency = json.optString("currency", "BRL"),
            schemaVersion = json.optInt("schema_version", LEGACY_SCHEMA_VERSION),
            metrics = SECTIONS.associateWith { parseSection(it, json.optJSONObject(it)) },
            epsSource = EpsSource.fromJson(valuation?.optString("eps_source", null)),
            fx = parseFx(valuation),
            dividendHistory = parseDividendHistory(json.optJSONObject(SECTION_INCOME)),
            sector = json.optString("sector", "").ifBlank { null },
            lastUpdated =
                json
                    .optString(
                        "last_updated",
                        null,
                    )?.let { runCatching { LocalDateTime.parse(it) }.getOrNull() },
        )
    }

    private fun parseSection(
        section: String,
        data: JSONObject?,
    ): Map<String, FundamentalMetric> {
        if (data == null) return emptyMap()

        val metrics = mutableMapOf<String, FundamentalMetric>()
        data.keys().forEach { key ->
            val value = data.opt(key)
            if (section == SECTION_GROWTH && key == REVENUE_GROWTH) {
                val growth = value as? JSONObject
                listOf("yoy_growth", "cagr").forEach { metrics[it] = FundamentalMetric.fromJson(growth?.opt(it)) }
            } else if (value is JSONObject || value == JSONObject.NULL) {
                metrics[key] = FundamentalMetric.fromJson(value)
            }
        }
        return metrics
    }

    private fun parseDividendHistory(income: JSONObject?): List<MonthlyDividend> {
        val history = income?.optJSONArray(DIVIDEND_HISTORY) ?: return emptyList()

        return (0 until history.length()).mapNotNull { index ->
            val item = history.optJSONObject(index) ?: return@mapNotNull null
            runCatching {
                MonthlyDividend(YearMonth.parse(item.getString("month")), BigDecimal(item.get("value").toString()))
            }.getOrNull()
        }
    }

    private fun parseFx(valuation: JSONObject?): FxInfo? {
        val rate = valuation?.opt("fx_rate")
        if (rate == null || rate == JSONObject.NULL) return null

        return FxInfo(
            financialCurrency = valuation.optString("financial_currency", ""),
            rate = BigDecimal(rate.toString()),
            date = valuation.optString("fx_date", null)?.let { runCatching { LocalDate.parse(it) }.getOrNull() },
        )
    }
}
