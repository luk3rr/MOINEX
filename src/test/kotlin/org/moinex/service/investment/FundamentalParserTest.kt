package org.moinex.service.investment

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import org.moinex.model.dto.AssetKind
import org.moinex.model.dto.EpsSource
import org.moinex.model.dto.MetricReason
import org.moinex.model.dto.MetricType
import java.math.BigDecimal
import java.time.LocalDate
import java.time.YearMonth

class FundamentalParserTest :
    BehaviorSpec({
        val currentJson =
            """
            {
              "symbol": "PETR4.SA", "currency": "BRL", "schema_version": 2, "asset_kind": "company",
              "last_updated": "2026-11-01T15:23:23.123456",
              "growth": {"revenue_growth": {
                "yoy_growth": {"value": -2.43, "type": "percent", "data_temporality": "calculated", "reference_date": "2025-12-31"},
                "cagr": {"value": null, "type": "percent", "data_temporality": "calculated", "reason": "INSUFFICIENT_HISTORY"},
                "years": 2}},
              "valuation": {
                "graham_number": {"value": null, "type": "currency", "data_temporality": "calculated",
                                  "reference_date": "2025-12-31", "reason": "NEGATIVE_EPS"},
                "eps": {"value": 1.52, "type": "currency", "data_temporality": "historical", "reference_date": "2025-12-31"},
                "eps_source": "statement", "financial_currency": "USD", "fx_rate": 5.0, "fx_date": "2025-12-31"}
            }
            """.trimIndent()

        Given("a snapshot in the current schema") {
            val snapshot = FundamentalParser.parse(currentJson)

            Then("it reads the identification fields") {
                snapshot.symbol shouldBe "PETR4.SA"
                snapshot.kind shouldBe AssetKind.COMPANY
                snapshot.schemaVersion shouldBe 2
                snapshot.lastUpdated?.toLocalDate() shouldBe LocalDate.of(2026, 11, 1)
            }

            Then("a metric with value keeps its type, temporality and reference date") {
                val eps = snapshot.metric("valuation", "eps")
                eps.value shouldBe BigDecimal("1.52")
                eps.type shouldBe MetricType.CURRENCY
                eps.referenceDate shouldBe LocalDate.of(2025, 12, 31)
                eps.reason shouldBe null
            }

            Then("a metric without value carries its reason") {
                val graham = snapshot.metric("valuation", "graham_number")
                graham.value shouldBe null
                graham.reason shouldBe MetricReason.NEGATIVE_EPS
                graham.referenceDate shouldBe LocalDate.of(2025, 12, 31)
            }

            Then("the growth metrics are flattened out of revenue_growth") {
                snapshot.metric("growth", "yoy_growth").value shouldBe BigDecimal("-2.43")
                snapshot.metric("growth", "cagr").reason shouldBe MetricReason.INSUFFICIENT_HISTORY
            }

            Then("the EPS source and the exchange rate are read") {
                snapshot.epsSource shouldBe EpsSource.STATEMENT
                snapshot.fx?.financialCurrency shouldBe "USD"
                snapshot.fx?.rate shouldBe BigDecimal("5.0")
                snapshot.fx?.date shouldBe LocalDate.of(2025, 12, 31)
            }
        }

        Given("a snapshot cached before reasons existed") {
            val legacy =
                """
                {"symbol": "ITUB4.SA", "currency": "BRL",
                 "valuation": {"graham_number": null,
                               "eps": {"value": 4.14, "type": "currency", "data_temporality": "historical"}},
                 "growth": {"revenue_growth": null}}
                """.trimIndent()
            val snapshot = FundamentalParser.parse(legacy)

            Then("it is read as schema 1 of a company") {
                snapshot.schemaVersion shouldBe 1
                snapshot.kind shouldBe AssetKind.COMPANY
                snapshot.epsSource shouldBe null
                snapshot.fx shouldBe null
            }

            Then("a null metric becomes unavailable instead of failing") {
                val graham = snapshot.metric("valuation", "graham_number")
                graham.value shouldBe null
                graham.reason shouldBe MetricReason.DATA_UNAVAILABLE
            }

            Then("a null revenue growth gives unavailable growth metrics") {
                snapshot.metric("growth", "cagr").reason shouldBe MetricReason.DATA_UNAVAILABLE
                snapshot.metric("growth", "yoy_growth").reason shouldBe MetricReason.DATA_UNAVAILABLE
            }

            Then("a key that is absent is unavailable too") {
                val metric = snapshot.metric("debt", "net_debt")
                metric.value shouldBe null
                metric.reason shouldBe MetricReason.DATA_UNAVAILABLE
            }
        }

        Given("the snapshot of a real estate fund") {
            val fundJson =
                """
                {"symbol": "FIIA11.SA", "schema_version": 2, "asset_kind": "fund",
                 "income": {
                   "dividend_yield": {"value": 8.23, "type": "percent", "data_temporality": "calculated"},
                   "last_dividend": {"value": 1.17, "type": "currency", "data_temporality": "historical",
                                     "reference_date": "2026-10-01"},
                   "dividends_12m": [{"month": "2026-09", "value": 1.17}, {"month": "2026-10", "value": 1.2},
                                     {"month": "bad", "value": 1}, {"month": "2026-11", "value": 0.0}]},
                 "valuation": {"price_to_book": {"value": 0.945, "type": "ratio", "data_temporality": "calculated"}}}
                """.trimIndent()
            val snapshot = FundamentalParser.parse(fundJson)

            Then("it is a fund with income and book value metrics") {
                snapshot.kind shouldBe AssetKind.FUND
                snapshot.metric("income", "dividend_yield").value shouldBe BigDecimal("8.23")
                snapshot.metric("income", "last_dividend").referenceDate shouldBe LocalDate.of(2026, 10, 1)
                snapshot.metric("valuation", "price_to_book").value shouldBe BigDecimal("0.945")
            }

            Then("the monthly dividends are read in order and malformed entries are skipped") {
                snapshot.dividendHistory.map { it.month } shouldBe
                    listOf(YearMonth.of(2026, 9), YearMonth.of(2026, 10), YearMonth.of(2026, 11))
                snapshot.dividendHistory[1].value shouldBe BigDecimal("1.2")
            }

            Then("a snapshot without income has no dividend history") {
                FundamentalParser.parse("""{"symbol": "X"}""").dividendHistory shouldBe emptyList()
            }
        }

        Given("a reason string the app does not know") {
            val json = """{"valuation": {"x": {"value": null, "type": "ratio", "reason": "SOMETHING_NEW"}}}"""

            Then("it falls back to unavailable") {
                FundamentalParser.parse(json).metric("valuation", "x").reason shouldBe MetricReason.DATA_UNAVAILABLE
            }
        }
    })
