package org.moinex.service.investment

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import org.moinex.factory.investment.FundamentalAnalysisFactory
import org.moinex.factory.investment.TickerFactory
import org.moinex.model.dto.AssetKind
import org.moinex.model.dto.DataTemporality
import org.moinex.model.dto.FundamentalMetric
import org.moinex.model.dto.FundamentalSnapshot
import org.moinex.model.dto.MetricReason
import org.moinex.model.dto.MetricType
import org.moinex.model.dto.QualityDimensionType
import org.moinex.model.dto.QualityLabel
import org.moinex.model.dto.ValuationLevel
import org.moinex.model.enums.PeriodType
import org.moinex.repository.investment.FundamentalAnalysisRepository
import java.math.BigDecimal

class FundamentalScoringServiceTest :
    BehaviorSpec({
        val repository = mockk<FundamentalAnalysisRepository>()
        val service = FundamentalScoringService(repository)

        fun metric(value: String?) =
            FundamentalMetric(
                value?.let { BigDecimal(it) },
                MetricType.NUMBER,
                DataTemporality.CALCULATED,
                null,
                if (value == null) MetricReason.DATA_UNAVAILABLE else null,
            )

        fun missing(reason: MetricReason) =
            FundamentalMetric(null, MetricType.NUMBER, DataTemporality.CALCULATED, null, reason)

        fun snapshot(vararg entries: Triple<String, String, FundamentalMetric>): FundamentalSnapshot =
            FundamentalSnapshot(
                symbol = "T",
                kind = AssetKind.COMPANY,
                currency = "BRL",
                schemaVersion = 2,
                metrics = entries.groupBy({ it.first }, { it.second to it.third }).mapValues { it.value.toMap() },
                epsSource = null,
                fx = null,
                lastUpdated = null,
            )

        fun valuationSnapshot(
            price: String?,
            fair: FundamentalMetric,
            graham: FundamentalMetric = metric(null),
        ) = snapshot(
            Triple("valuation", "current_price", metric(price)),
            Triple("valuation", "graham_fair_value", fair),
            Triple("valuation", "graham_number", graham),
        )

        Given("a price and a fair value") {
            Then("a margin of 20% or more is cheap, with the boundary included") {
                val verdict = service.valuation(valuationSnapshot("80", metric("100")))

                verdict.level shouldBe ValuationLevel.CHEAP
                verdict.marginOfSafetyPct shouldBe BigDecimal("20.00")
            }

            Then("a margin of -20% or less is expensive, with the boundary included") {
                val verdict = service.valuation(valuationSnapshot("120", metric("100")))

                verdict.level shouldBe ValuationLevel.EXPENSIVE
                verdict.marginOfSafetyPct shouldBe BigDecimal("-20.00")
            }

            Then("anything in between is fair") {
                service.valuation(valuationSnapshot("100", metric("100"))).level shouldBe ValuationLevel.FAIR
                service.valuation(valuationSnapshot("85", metric("100"))).level shouldBe ValuationLevel.FAIR
                service.valuation(valuationSnapshot("115", metric("100"))).level shouldBe ValuationLevel.FAIR
            }

            Then("the revised fair value wins over the Graham number") {
                val verdict = service.valuation(valuationSnapshot("50", metric("100"), graham = metric("60")))

                verdict.fairValue shouldBe BigDecimal("100")
                verdict.grahamNumber shouldBe BigDecimal("60")
            }

            Then("the Graham number is used when there is no revised fair value") {
                val verdict = service.valuation(valuationSnapshot("50", metric(null), graham = metric("100")))

                verdict.fairValue shouldBe BigDecimal("100")
                verdict.level shouldBe ValuationLevel.CHEAP
            }
        }

        Given("no usable fair value") {
            Then("the verdict is unavailable and keeps the reason of the metric") {
                val verdict =
                    service.valuation(
                        valuationSnapshot(
                            "50",
                            missing(MetricReason.NEGATIVE_EPS),
                            graham = missing(MetricReason.NEGATIVE_EPS),
                        ),
                    )

                verdict.level shouldBe ValuationLevel.UNAVAILABLE
                verdict.reason shouldBe MetricReason.NEGATIVE_EPS
                verdict.marginOfSafetyPct shouldBe null
            }

            Then("a fair value that is not positive is not applicable") {
                service.valuation(valuationSnapshot("50", metric("0"))).reason shouldBe MetricReason.NOT_APPLICABLE
            }

            Then("a missing price is data unavailable") {
                service.valuation(valuationSnapshot(null, metric("100"))).reason shouldBe MetricReason.DATA_UNAVAILABLE
            }
        }

        Given("a strong company in every dimension") {
            val strong =
                snapshot(
                    Triple("profitability", "roe", metric("20")),
                    Triple("profitability", "net_margin", metric("20")),
                    Triple("profitability", "ebitda_margin", metric("30")),
                    Triple("debt", "net_debt_to_ebitda", metric("0.5")),
                    Triple("debt", "current_ratio", metric("2.5")),
                    Triple("growth", "cagr", metric("20")),
                    Triple("valuation", "eps_cagr", metric("20")),
                    Triple("cash_generation", "fcf_to_net_income", metric("1.2")),
                    Triple("valuation", "fcf_yield", metric("9")),
                )

            Then("the score is 100 and excellent, with the agreed weights") {
                val score = service.quality(strong)

                score.total shouldBe 100
                score.label shouldBe QualityLabel.EXCELLENT
                score.dimensions.map { it.weight } shouldBe listOf(35, 25, 20, 20)
                score.dimensions.map { it.type } shouldBe QualityDimensionType.entries
            }
        }

        Given("a bank, where debt and cash flow do not apply") {
            val bank =
                snapshot(
                    Triple("profitability", "roe", metric("20")),
                    Triple("growth", "cagr", metric("20")),
                    Triple("debt", "net_debt_to_ebitda", metric("5")),
                    Triple("cash_generation", "fcf_to_net_income", metric("11")),
                )

            Then("debt and cash are left out of the score") {
                val score = service.quality(bank.copy(sector = "Financial Services"))

                score.dimensions.first { it.type == QualityDimensionType.DEBT }.score shouldBe null
                score.dimensions.first { it.type == QualityDimensionType.CASH }.score shouldBe null
                score.total shouldBe 100
            }

            Then("another sector keeps all four dimensions") {
                val score = service.quality(bank.copy(sector = "Industrials"))

                score.dimensions.first { it.type == QualityDimensionType.DEBT }.score shouldBe 0
                score.dimensions.first { it.type == QualityDimensionType.CASH }.score shouldBe 100
            }
        }

        Given("a company with data in only some dimensions") {
            Then("the missing dimension is left out and the weights are redistributed") {
                val partial =
                    snapshot(
                        Triple("profitability", "roe", metric("20")),
                        Triple("growth", "cagr", metric("0")),
                    )

                val score = service.quality(partial)

                score.dimensions.first { it.type == QualityDimensionType.PROFITABILITY }.score shouldBe 100
                score.dimensions.first { it.type == QualityDimensionType.GROWTH }.score shouldBe 25
                score.dimensions.first { it.type == QualityDimensionType.DEBT }.score shouldBe null
                score.total shouldBe 73
                score.label shouldBe QualityLabel.GOOD
            }

            Then("with fewer than two dimensions there is no score") {
                val score = service.quality(snapshot(Triple("profitability", "roe", metric("20"))))

                score.total shouldBe null
                score.label shouldBe null
            }
        }

        Given("the individual bands") {
            Then("a negative ROE scores zero and a ROE of exactly 15% scores 100") {
                val negative =
                    service.quality(
                        snapshot(Triple("profitability", "roe", metric("-5")), Triple("growth", "cagr", metric("20"))),
                    )
                val boundary =
                    service.quality(
                        snapshot(Triple("profitability", "roe", metric("15")), Triple("growth", "cagr", metric("20"))),
                    )

                negative.dimensions.first { it.type == QualityDimensionType.PROFITABILITY }.score shouldBe 0
                boundary.dimensions.first { it.type == QualityDimensionType.PROFITABILITY }.score shouldBe 100
            }

            Then("net cash scores 100 on debt and a non-positive EBITDA scores 0") {
                val netCash =
                    service.quality(
                        snapshot(
                            Triple("debt", "net_debt_to_ebitda", metric("-1")),
                            Triple("growth", "cagr", metric("20")),
                        ),
                    )
                val noEbitda =
                    service.quality(
                        snapshot(
                            Triple("debt", "net_debt_to_ebitda", missing(MetricReason.NOT_APPLICABLE)),
                            Triple("growth", "cagr", metric("20")),
                        ),
                    )

                netCash.dimensions.first { it.type == QualityDimensionType.DEBT }.score shouldBe 100
                noEbitda.dimensions.first { it.type == QualityDimensionType.DEBT }.score shouldBe 0
            }

            Then("a negative EPS history scores zero on growth") {
                val score =
                    service.quality(
                        snapshot(
                            Triple("valuation", "eps_cagr", missing(MetricReason.NEGATIVE_EPS)),
                            Triple("profitability", "roe", metric("20")),
                        ),
                    )

                score.dimensions.first { it.type == QualityDimensionType.GROWTH }.score shouldBe 0
            }

            Then("labels change at 40, 60 and 80") {
                service.labelFor(39) shouldBe QualityLabel.WEAK
                service.labelFor(40) shouldBe QualityLabel.REGULAR
                service.labelFor(59) shouldBe QualityLabel.REGULAR
                service.labelFor(60) shouldBe QualityLabel.GOOD
                service.labelFor(79) shouldBe QualityLabel.GOOD
                service.labelFor(80) shouldBe QualityLabel.EXCELLENT
            }
        }

        Given("the analyses of other tickers") {
            fun peerJson(roe: String?) =
                """{"symbol":"P","schema_version":2,"asset_kind":"company",
                    "profitability":{"roe":{"value":${roe ?: "null"},"type":"percent"}}}"""

            fun analysis(
                id: Int,
                sector: String?,
                roe: String?,
                period: PeriodType = PeriodType.ANNUAL,
                json: String = peerJson(roe),
            ) = FundamentalAnalysisFactory.create(
                ticker = TickerFactory.create(id = id, symbol = "T$id"),
                sector = sector,
                periodType = period,
                dataJson = json,
            )

            val subject = analysis(1, "Energy", "10")
            val roeKey = FundamentalScoringService.sectorKey("profitability", "roe")

            Then("the median uses only the other tickers of the same sector and period") {
                every { repository.findAllForActiveTickers() } returns
                    listOf(
                        subject,
                        analysis(2, "Energy", "4"),
                        analysis(3, "Energy", "8"),
                        analysis(4, "Energy", "20"),
                        analysis(5, "Banks", "99"),
                        analysis(6, "Energy", "99", period = PeriodType.QUARTERLY),
                    )

                val comparison = service.sectorComparisons(subject).getValue(roeKey)

                comparison.peers shouldBe 3
                comparison.median shouldBe BigDecimal("8")
                comparison.enough shouldBe true
            }

            Then("with fewer than three peers the comparison is not enough") {
                every { repository.findAllForActiveTickers() } returns
                    listOf(subject, analysis(2, "Energy", "4"), analysis(3, "Energy", "8"))

                val comparison = service.sectorComparisons(subject).getValue(roeKey)

                comparison.peers shouldBe 2
                comparison.median shouldBe BigDecimal("6.0000")
                comparison.enough shouldBe false
            }

            Then("a peer without the metric does not count") {
                every { repository.findAllForActiveTickers() } returns
                    listOf(subject, analysis(2, "Energy", null), analysis(3, "Energy", "8"))

                service.sectorComparisons(subject).getValue(roeKey).peers shouldBe 1
            }

            Then("a peer with an unreadable JSON is ignored") {
                every { repository.findAllForActiveTickers() } returns
                    listOf(subject, analysis(2, "Energy", "4", json = "not json"), analysis(3, "Energy", "8"))

                service.sectorComparisons(subject).getValue(roeKey).peers shouldBe 1
            }

            Then("a ticker without sector compares with nobody") {
                every { repository.findAllForActiveTickers() } returns listOf(analysis(2, "Energy", "4"))

                val comparison = service.sectorComparisons(analysis(9, null, "10")).getValue(roeKey)

                comparison.peers shouldBe 0
                comparison.median shouldBe null
                comparison.enough shouldBe false
            }
        }

        Given("a list of values") {
            Then("the median of an even list is the mean of the two middle values") {
                service.median(listOf(BigDecimal("1"), BigDecimal("2"), BigDecimal("3"), BigDecimal("10"))) shouldBe
                    BigDecimal("2.5000")
            }

            Then("an empty list has no median") {
                service.median(emptyList()) shouldBe null
            }
        }
    })
