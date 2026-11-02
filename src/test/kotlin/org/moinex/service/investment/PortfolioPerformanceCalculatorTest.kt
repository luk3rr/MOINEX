package org.moinex.service.investment

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.shouldBe
import org.moinex.factory.investment.DividendFactory
import org.moinex.factory.investment.TickerFactory
import org.moinex.factory.wallet.WalletTransactionFactory
import org.moinex.model.dto.DailyPricePointDTO
import org.moinex.service.investment.PortfolioPerformanceCalculator.Position
import org.moinex.service.investment.PortfolioPerformanceCalculator.Trade
import java.math.BigDecimal
import java.time.LocalDate

class PortfolioPerformanceCalculatorTest :
    BehaviorSpec({
        fun day(text: String) = LocalDate.parse(text)

        fun price(
            id: Int,
            date: String,
            value: String,
        ) = DailyPricePointDTO(id, day(date), BigDecimal(value))

        fun position(
            quantity: String,
            prices: List<DailyPricePointDTO>,
            trades: List<Trade> = emptyList(),
            id: Int = 1,
        ) = Position(
            TickerFactory.create(id = id, symbol = "T$id", currentQuantity = BigDecimal(quantity)),
            trades,
            prices,
        )

        val start = day("2026-10-01")
        val end = day("2026-10-31")

        Given("a position held the whole period") {
            val prices =
                listOf(price(1, "2026-10-01", "10"), price(1, "2026-10-02", "11"), price(1, "2026-10-05", "12.1"))

            Then("the accumulated return follows the price") {
                val series = PortfolioPerformanceCalculator.portfolioReturn(listOf(position("100", prices)), start, end)

                series.map { it.date.toString() } shouldBe listOf("2026-10-01", "2026-10-02", "2026-10-05")
                series.map { it.pct } shouldBe listOf(BigDecimal("0.00"), BigDecimal("10.00"), BigDecimal("21.00"))
            }
        }

        Given("money put in during the period") {
            val prices =
                listOf(price(1, "2026-10-01", "10"), price(1, "2026-10-02", "11"), price(1, "2026-10-05", "12.1"))
            val buy = Trade(day("2026-10-02"), BigDecimal("100"), BigDecimal("11"))

            Then("the purchase does not count as a gain, and its day is a return on the capital at work") {
                val series =
                    PortfolioPerformanceCalculator.portfolioReturn(
                        listOf(position("200", prices, listOf(buy))),
                        start,
                        end,
                    )

                series.map { it.pct } shouldBe listOf(BigDecimal("0.00"), BigDecimal("4.76"), BigDecimal("15.24"))
            }

            Then("a purchase far bigger than the portfolio does not blow up the return") {
                val small = listOf(price(1, "2026-10-01", "10"), price(1, "2026-10-02", "10.2"))
                val huge = Trade(day("2026-10-02"), BigDecimal("1000"), BigDecimal("10"))

                PortfolioPerformanceCalculator
                    .portfolioReturn(listOf(position("1010", small, listOf(huge))), start, end)
                    .map { it.pct } shouldBe listOf(BigDecimal("0.00"), BigDecimal("2.00"))
            }
        }

        Given("money taken out during the period") {
            val prices =
                listOf(price(1, "2026-10-01", "10"), price(1, "2026-10-02", "11"), price(1, "2026-10-05", "12.1"))
            val sell = Trade(day("2026-10-02"), BigDecimal("-50"), BigDecimal("11"))

            Then("the sale does not count as a loss") {
                val series =
                    PortfolioPerformanceCalculator.portfolioReturn(
                        listOf(position("50", prices, listOf(sell))),
                        start,
                        end,
                    )

                series.map { it.pct } shouldBe listOf(BigDecimal("0.00"), BigDecimal("10.00"), BigDecimal("21.00"))
            }
        }

        Given("trades made on days without prices") {
            val prices =
                listOf(
                    price(1, "2026-10-02", "10"),
                    price(1, "2026-10-05", "10"),
                    price(1, "2026-10-06", "10"),
                    price(1, "2026-10-07", "10"),
                )

            Then("a sale on a weekend is money taken out on the next trading day, not a loss") {
                val sale = Trade(day("2026-10-03"), BigDecimal("-50"), BigDecimal("10"))

                PortfolioPerformanceCalculator
                    .portfolioReturn(listOf(position("50", prices, listOf(sale))), start, end)
                    .map { it.pct } shouldBe
                    listOf(BigDecimal("0.00"), BigDecimal("0.00"), BigDecimal("0.00"), BigDecimal("0.00"))
            }

            Then("a purchase on a weekend is money put in on the next trading day, not a gain") {
                val buy = Trade(day("2026-10-04"), BigDecimal("100"), BigDecimal("10"))

                PortfolioPerformanceCalculator
                    .portfolioReturn(listOf(position("200", prices, listOf(buy))), start, end)
                    .map { it.pct } shouldBe
                    listOf(BigDecimal("0.00"), BigDecimal("0.00"), BigDecimal("0.00"), BigDecimal("0.00"))
            }

            Then("a trade after the last price day changes the earlier quantities and counts as no flow") {
                val later = Trade(day("2026-10-20"), BigDecimal("100"), BigDecimal("10"))

                PortfolioPerformanceCalculator
                    .portfolioReturn(listOf(position("200", prices, listOf(later))), start, end)
                    .map { it.pct } shouldBe
                    listOf(BigDecimal("0.00"), BigDecimal("0.00"), BigDecimal("0.00"), BigDecimal("0.00"))
            }

            Then("the profit of the period does not count that weekend sale as a loss either") {
                val sale = Trade(day("2026-10-03"), BigDecimal("-50"), BigDecimal("10"))

                PortfolioPerformanceCalculator
                    .contributions(listOf(position("50", prices, listOf(sale))), emptyList(), start, end)
                    .single()
                    .amount
                    .compareTo(BigDecimal.ZERO) shouldBe 0
            }
        }

        Given("two positions") {
            val a = position("10", listOf(price(1, "2026-10-01", "10"), price(1, "2026-10-02", "20")), id = 1)
            val b = position("10", listOf(price(2, "2026-10-01", "10"), price(2, "2026-10-02", "10")), id = 2)

            Then("the return is the one of their combined value") {
                PortfolioPerformanceCalculator
                    .portfolioReturn(listOf(a, b), start, end)
                    .map { it.pct } shouldBe listOf(BigDecimal("0.00"), BigDecimal("50.00"))
            }

            Then("a position without prices is ignored") {
                val noPrices = position("1000", emptyList(), id = 3)

                PortfolioPerformanceCalculator
                    .portfolioReturn(listOf(a, noPrices), start, end)
                    .map { it.pct } shouldBe listOf(BigDecimal("0.00"), BigDecimal("100.00"))
            }
        }

        Given("fewer than two days of prices") {
            Then("there is no series") {
                PortfolioPerformanceCalculator
                    .portfolioReturn(listOf(position("10", listOf(price(1, "2026-10-01", "10")))), start, end)
                    .shouldBeEmpty()
                PortfolioPerformanceCalculator.portfolioReturn(emptyList(), start, end).shouldBeEmpty()
            }
        }

        Given("prices outside the period") {
            Then("they are ignored") {
                val prices =
                    listOf(price(1, "2026-09-15", "5"), price(1, "2026-10-02", "10"), price(1, "2026-10-03", "11"))

                PortfolioPerformanceCalculator
                    .portfolioReturn(listOf(position("10", prices)), start, end)
                    .map { it.date.toString() } shouldBe listOf("2026-10-02", "2026-10-03")
            }
        }

        Given("the profit of each position over a period") {
            val prices =
                listOf(price(1, "2026-10-01", "10"), price(1, "2026-10-02", "11"), price(1, "2026-10-05", "12.1"))

            Then("a position held the whole period earns its change in value") {
                PortfolioPerformanceCalculator
                    .contributions(listOf(position("100", prices)), emptyList(), start, end)
                    .single()
                    .amount
                    .compareTo(BigDecimal("210")) shouldBe 0
            }

            Then("a purchase during the period is not profit") {
                val buy = Trade(day("2026-10-02"), BigDecimal("100"), BigDecimal("11"))

                PortfolioPerformanceCalculator
                    .contributions(listOf(position("200", prices, listOf(buy))), emptyList(), start, end)
                    .single()
                    .amount
                    .compareTo(BigDecimal("320")) shouldBe 0
            }

            Then("a sale during the period keeps the gain it realized") {
                val sell = Trade(day("2026-10-02"), BigDecimal("-50"), BigDecimal("11"))

                PortfolioPerformanceCalculator
                    .contributions(listOf(position("50", prices, listOf(sell))), emptyList(), start, end)
                    .single()
                    .amount
                    .compareTo(BigDecimal("155")) shouldBe 0
            }

            Then("dividends paid inside the period add to the profit and the ones outside do not") {
                val ticker = TickerFactory.create(id = 1, currentQuantity = BigDecimal("100"))
                val inside =
                    DividendFactory.create(
                        ticker = ticker,
                        walletTransaction =
                            WalletTransactionFactory.create(
                                amount = BigDecimal("30"),
                                date = day("2026-10-03").atStartOfDay(),
                            ),
                    )
                val outside =
                    DividendFactory.create(
                        ticker = ticker,
                        walletTransaction =
                            WalletTransactionFactory.create(
                                amount = BigDecimal("999"),
                                date = day("2026-09-03").atStartOfDay(),
                            ),
                    )

                PortfolioPerformanceCalculator
                    .contributions(listOf(Position(ticker, emptyList(), prices)), listOf(inside, outside), start, end)
                    .single()
                    .amount
                    .compareTo(BigDecimal("240")) shouldBe 0
            }

            Then("the biggest profit comes first, a loss comes last, and positions without prices are left out") {
                val loser = position("10", listOf(price(2, "2026-10-01", "10"), price(2, "2026-10-05", "8")), id = 2)
                val noPrices = position("10", emptyList(), id = 3)

                PortfolioPerformanceCalculator
                    .contributions(listOf(loser, position("100", prices), noPrices), emptyList(), start, end)
                    .map { it.ticker.id } shouldBe listOf(1, 2)
            }
        }

        Given("a daily rate like the CDI") {
            val days = listOf(day("2026-10-01"), day("2026-10-02"), day("2026-10-05"), day("2026-10-06"))
            val rates = days.map { it to BigDecimal("0.1") }

            Then("the rate of the first day is not counted and the others compound") {
                PortfolioPerformanceCalculator
                    .cumulativeFromDailyRates(rates, days)
                    .map { it.pct } shouldBe
                    listOf(BigDecimal("0.00"), BigDecimal("0.10"), BigDecimal("0.20"), BigDecimal("0.30"))
            }

            Then("without rates there is no series") {
                PortfolioPerformanceCalculator.cumulativeFromDailyRates(emptyList(), days).shouldBeEmpty()
            }
        }

        Given("a monthly rate like the IPCA") {
            val rates =
                listOf(
                    day("2026-01-01") to BigDecimal("1.00"),
                    day("2026-02-01") to BigDecimal("2.00"),
                    day("2026-03-01") to BigDecimal("3.00"),
                )
            val days = listOf(day("2026-02-01"), day("2026-02-27"), day("2026-02-28"), day("2026-03-31"))

            Then("a month counts once it is over and months before the start never count") {
                PortfolioPerformanceCalculator
                    .cumulativeFromMonthlyRates(rates, days)
                    .map { it.pct } shouldBe
                    listOf(BigDecimal("0.00"), BigDecimal("0.00"), BigDecimal("2.00"), BigDecimal("5.06"))
            }
        }

        Given("index levels like the Ibovespa") {
            val levels =
                listOf(
                    day("2026-10-01") to BigDecimal("100"),
                    day("2026-10-03") to BigDecimal("110"),
                    day("2026-10-04") to BigDecimal("105"),
                )
            val days = listOf(day("2026-10-02"), day("2026-10-03"), day("2026-10-05"))

            Then("the return is relative to the level of the first day, filling days without a quote") {
                PortfolioPerformanceCalculator
                    .cumulativeFromLevels(levels, days)
                    .map { it.pct } shouldBe listOf(BigDecimal("0.00"), BigDecimal("10.00"), BigDecimal("5.00"))
            }

            Then("without levels there is no series") {
                PortfolioPerformanceCalculator.cumulativeFromLevels(emptyList(), days).shouldBeEmpty()
            }
        }
    })
