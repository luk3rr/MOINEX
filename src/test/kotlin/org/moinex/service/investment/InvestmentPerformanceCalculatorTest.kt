package org.moinex.service.investment

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import org.moinex.service.investment.InvestmentPerformanceCalculator.MonthResult
import org.moinex.service.investment.InvestmentPerformanceCalculator.Payout
import org.moinex.service.investment.InvestmentPerformanceCalculator.TickerFacts
import org.moinex.service.investment.InvestmentPerformanceCalculator.Trade
import java.math.BigDecimal
import java.time.LocalDate
import java.time.YearMonth

class InvestmentPerformanceCalculatorTest :
    BehaviorSpec({
        val today = LocalDate.of(2026, 6, 20)
        val january = YearMonth.of(2026, 1)
        val february = YearMonth.of(2026, 2)
        val march = YearMonth.of(2026, 3)

        fun d(
            month: Int,
            day: Int,
        ) = LocalDate.of(2026, month, day)

        fun prices(vararg points: Pair<LocalDate, String>): (LocalDate) -> BigDecimal? =
            { date ->
                points
                    .filter { !it.first.isAfter(date) }
                    .maxByOrNull { it.first }
                    ?.second
                    ?.let(::BigDecimal)
            }

        fun BigDecimal.eq(value: String) = compareTo(BigDecimal(value)) == 0

        fun facts(
            purchases: List<Trade> = emptyList(),
            sales: List<Trade> = emptyList(),
            payouts: List<Payout> = emptyList(),
            opening: String = "0",
            openingCost: String = "0",
            priceAt: (LocalDate) -> BigDecimal?,
        ) = TickerFacts(BigDecimal(opening), BigDecimal(openingCost), purchases, sales, payouts, priceAt)

        Given("10 units bought at R$ 10, valued at R$ 15 in January and sold at R$ 15 in February") {
            val result =
                InvestmentPerformanceCalculator.tickerMonths(
                    facts(
                        purchases = listOf(Trade(d(1, 5), BigDecimal("10"), BigDecimal("10"))),
                        sales = listOf(Trade(d(2, 10), BigDecimal("10"), BigDecimal("15"))),
                        priceAt = prices(d(1, 1) to "10", d(1, 31) to "15", d(2, 28) to "15"),
                    ),
                    listOf(january, february, march),
                    today,
                )

            When("the monthly gains are computed") {
                Then("January gains R$ 50, February R$ 0 and the sale is not counted again") {
                    result.getValue(january).gain.eq("50") shouldBe true
                    result.getValue(february).gain.eq("0") shouldBe true
                    result.getValue(march).gain.eq("0") shouldBe true
                }

                Then("the sum of the months is the whole gain of R$ 50") {
                    listOf(january, february, march)
                        .fold(BigDecimal.ZERO) { acc, month -> acc.add(result.getValue(month).gain) }
                        .eq("50") shouldBe true
                }
            }
        }

        Given("10 units bought on the 20th when the price goes from 10 (day 1) to 12 (day 20)") {
            val result =
                InvestmentPerformanceCalculator.tickerMonths(
                    facts(
                        purchases = listOf(Trade(d(1, 20), BigDecimal("10"), BigDecimal("12"))),
                        priceAt = prices(d(1, 1) to "10", d(1, 20) to "12", d(1, 31) to "14"),
                    ),
                    listOf(january),
                    today,
                )

            When("the gain of the month is computed") {
                Then("the units only gain from the purchase on: 10 x (14 - 12) = R$ 20") {
                    result.getValue(january).gain.eq("20") shouldBe true
                }
            }
        }

        Given("purchases of 10 units at R$ 10 in January and 10 at R$ 20 in March") {
            val result =
                InvestmentPerformanceCalculator.tickerMonths(
                    facts(
                        purchases =
                            listOf(
                                Trade(d(1, 5), BigDecimal("10"), BigDecimal("10")),
                                Trade(d(3, 5), BigDecimal("10"), BigDecimal("20")),
                            ),
                        priceAt = prices(d(1, 1) to "10", d(3, 31) to "20"),
                    ),
                    listOf(january, february, march),
                    today,
                )

            When("the invested value of each month is computed") {
                Then("January costs R$ 100, February R$ 100 and March R$ 300") {
                    result.getValue(january).invested.eq("100") shouldBe true
                    result.getValue(february).invested.eq("100") shouldBe true
                    result.getValue(march).invested.eq("300") shouldBe true
                }
            }
        }

        Given("a payout of R$ 3 in February on units worth R$ 100 and unchanged") {
            val result =
                InvestmentPerformanceCalculator.tickerMonths(
                    facts(
                        purchases = listOf(Trade(d(1, 5), BigDecimal("10"), BigDecimal("10"))),
                        payouts = listOf(Payout(d(2, 15), BigDecimal("3"))),
                        priceAt = prices(d(1, 1) to "10"),
                    ),
                    listOf(january, february),
                    today,
                )

            When("the gain is computed") {
                Then("February gains the payout, R$ 3") {
                    result.getValue(february).gain.eq("3") shouldBe true
                    result.getValue(january).gain.eq("0") shouldBe true
                }
            }
        }

        Given("5 units already held before the first trade, valued at R$ 10 and then R$ 12") {
            val result =
                InvestmentPerformanceCalculator.tickerMonths(
                    facts(
                        opening = "5",
                        openingCost = "8",
                        priceAt = prices(d(1, 1) to "10", d(1, 31) to "12"),
                    ),
                    listOf(january),
                    today,
                )

            When("the gain of the first month is computed") {
                Then(
                    "the opening units do not count as a gain from zero: 5 x (12 - 10) = R$ 10, and the cost is R$ 40",
                ) {
                    result.getValue(january).gain.eq("10") shouldBe true
                    result.getValue(january).invested.eq("40") shouldBe true
                }
            }
        }

        fun bond(
            invested: Map<LocalDate, String>,
            held: Map<YearMonth, String>,
            vararg flows: Pair<LocalDate, String>,
        ) = InvestmentPerformanceCalculator.BondFacts(
            investedOn = { date ->
                invested
                    .filter { !it.key.isAfter(date) }
                    .maxByOrNull { it.key }
                    ?.value
                    ?.let(::BigDecimal)
                    ?: BigDecimal.ZERO
            },
            interestHeldAt = { month ->
                held
                    .filter { !it.key.isAfter(month) }
                    .maxByOrNull { it.key }
                    ?.value
                    ?.let(::BigDecimal)
                    ?: BigDecimal.ZERO
            },
            flows = flows.map { Payout(it.first, BigDecimal(it.second)) },
        )

        Given("a bond bought for R$ 1.000 in January that earned R$ 10 and then R$ 12") {
            val result =
                InvestmentPerformanceCalculator.bondMonths(
                    bond(
                        invested = mapOf(d(1, 5) to "1000"),
                        held = mapOf(january to "10", february to "22"),
                        d(1, 5) to "1000",
                    ),
                    listOf(january, february),
                    today,
                )

            When("the monthly results are computed") {
                Then("the gain of each month is its interest and the value includes the interest held") {
                    result.getValue(january).gain.eq("10") shouldBe true
                    result.getValue(february).gain.eq("12") shouldBe true
                    result.getValue(february).value.eq("1022") shouldBe true
                    result.getValue(february).invested.eq("1000") shouldBe true
                }
            }
        }

        Given("the same bond with R$ 500 of principal and R$ 6 of interest taken out in February") {
            val result =
                InvestmentPerformanceCalculator.bondMonths(
                    bond(
                        invested = mapOf(d(1, 5) to "1000", d(2, 10) to "500"),
                        held = mapOf(january to "10", february to "16"),
                        d(1, 5) to "1000",
                        d(2, 10) to "-506",
                    ),
                    listOf(january, february),
                    today,
                )

            When("the monthly results are computed") {
                Then("February gains only the R$ 12 earned, the interest taken out is not counted again") {
                    result.getValue(february).gain.eq("12") shouldBe true
                    result.getValue(february).value.eq("516") shouldBe true
                }
            }
        }

        Given("a bond sold above its cost: R$ 500 of principal received as R$ 600") {
            val result =
                InvestmentPerformanceCalculator.bondMonths(
                    bond(
                        invested = mapOf(d(1, 5) to "1000", d(2, 10) to "500"),
                        held = mapOf(january to "0", february to "0"),
                        d(1, 5) to "1000",
                        d(2, 10) to "-600",
                    ),
                    listOf(january, february),
                    today,
                )

            When("the monthly results are computed") {
                Then("the price gain of R$ 100 is counted in the month of the sale") {
                    result.getValue(february).gain.eq("100") shouldBe true
                }
            }
        }

        Given("two results") {
            When("added") {
                val total =
                    MonthResult(BigDecimal("1000"), BigDecimal("1010"), BigDecimal("10")) +
                        MonthResult(BigDecimal("500"), BigDecimal("520"), BigDecimal("20"))

                Then("every field is added: gain R$ 30, value R$ 1530, invested R$ 1500") {
                    total.gain.eq("30") shouldBe true
                    total.value.eq("1530") shouldBe true
                    total.invested.eq("1500") shouldBe true
                }
            }
        }
    })
