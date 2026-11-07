package org.moinex.service.investment

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import org.moinex.model.enums.InterestIndex
import org.moinex.model.enums.InterestType
import org.moinex.service.investment.BondAccrualCalculator.Flow
import org.moinex.service.investment.BondAccrualCalculator.IndexPoint
import org.moinex.service.investment.BondAccrualCalculator.OpeningLot
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate

class BondAccrualCalculatorTest :
    BehaviorSpec({
        val firstDay = LocalDate.of(2026, 3, 2)

        fun rows(
            count: Int,
            value: String,
        ) = (0 until count).map { IndexPoint(firstDay.plusDays(it.toLong()), BigDecimal(value)) }

        fun interest(
            type: InterestType,
            index: InterestIndex,
            rate: String,
            opening: String,
            points: List<IndexPoint>,
            flows: List<Flow> = emptyList(),
            lots: List<OpeningLot> = emptyList(),
        ): BigDecimal =
            BondAccrualCalculator.periodInterest(
                type = type,
                index = index,
                bondRate = BigDecimal(rate),
                opening = BigDecimal(opening),
                openingLots = lots,
                flows = flows,
                points = points,
            )

        fun BigDecimal.cents(): BigDecimal = setScale(2, RoundingMode.HALF_UP)

        Given("R$ 1.000 at 110% of the CDI") {
            When("three business days pass with the CDI at 0.05% a day") {
                val result = interest(InterestType.FLOATING, InterestIndex.CDI, "110", "1000", rows(3, "0.05"))

                Then("the interest is R$ 1.65") {
                    result.cents().compareTo(BigDecimal("1.65")) shouldBe 0
                }
            }
        }

        Given("R$ 1.000 at the CDI plus 2% a year") {
            When("one business day passes with the CDI at 0.05%") {
                val result = interest(InterestType.FLOATING_SPREAD, InterestIndex.CDI, "2", "1000", rows(1, "0.05"))

                Then("the interest is R$ 0.58") {
                    result.cents().compareTo(BigDecimal("0.58")) shouldBe 0
                }
            }
        }

        Given("R$ 1.000 at 100% of the CDI for a year") {
            When("252 business days pass with the CDI at 0.05% a day") {
                val result = interest(InterestType.FLOATING, InterestIndex.CDI, "100", "1000", rows(252, "0.05"))

                Then("the interest compounds to R$ 134.25 and the balance is R$ 1134.25") {
                    result.cents().compareTo(BigDecimal("134.25")) shouldBe 0
                    BigDecimal("1000").add(result).cents().compareTo(BigDecimal("1134.25")) shouldBe 0
                }
            }
        }

        Given("R$ 1.000 at a fixed 10% a year") {
            When("252 business days pass") {
                val result = interest(InterestType.FIXED, InterestIndex.CDI, "10", "1000", rows(252, "0.05"))

                Then("the interest is R$ 100.00") {
                    result.cents().compareTo(BigDecimal("100.00")) shouldBe 0
                }
            }
        }

        Given("R$ 1.000 at the IPCA plus 6% a year") {
            When("twelve monthly rows of 0.5% pass") {
                val monthly =
                    (0 until 12).map {
                        IndexPoint(
                            LocalDate.of(2026, 1, 1).plusMonths(it.toLong()),
                            BigDecimal("0.5"),
                        )
                    }
                val result = interest(InterestType.FLOATING_SPREAD, InterestIndex.IPCA, "6", "1000", monthly)

                Then("the interest is R$ 125.38") {
                    result.cents().compareTo(BigDecimal("125.38")) shouldBe 0
                }
            }
        }

        Given("R$ 1.000 at 100% of the Selic target, a yearly rate of 15%") {
            When("252 business days pass") {
                val result = interest(InterestType.FLOATING, InterestIndex.SELIC, "100", "1000", rows(252, "15"))

                Then("the interest is R$ 150.00") {
                    result.cents().compareTo(BigDecimal("150.00")) shouldBe 0
                }
            }
        }

        Given("a sale on the second business day and a purchase on the third") {
            val points = rows(5, "0.05")
            val flows =
                listOf(
                    Flow(points[1].date, BigDecimal("-500")),
                    Flow(points[2].date, BigDecimal("500")),
                )

            When("the interest is computed over five business days") {
                val result = interest(InterestType.FLOATING, InterestIndex.CDI, "100", "1000", points, flows)

                Then("the day of the sale earns and the day of the purchase does not: R$ 2.25") {
                    result.cents().compareTo(BigDecimal("2.25")) shouldBe 0
                }
            }
        }

        Given("a balance split in two lots at 100% and 110% of the CDI") {
            val lots = listOf(OpeningLot(BigDecimal("600")), OpeningLot(BigDecimal("400"), BigDecimal("110")))

            When("two business days pass") {
                val result =
                    interest(InterestType.FLOATING, InterestIndex.CDI, "100", "1000", rows(2, "0.05"), lots = lots)

                Then("each lot earns at its own rate: R$ 1.04") {
                    result.cents().compareTo(BigDecimal("1.04")) shouldBe 0
                }
            }
        }

        Given("a purchase dated on the last business day of the period") {
            val points = rows(3, "0.05")

            When("the interest is computed") {
                val withPurchase =
                    interest(
                        InterestType.FLOATING,
                        InterestIndex.CDI,
                        "100",
                        "1000",
                        points,
                        listOf(Flow(points.last().date, BigDecimal("500"))),
                    )
                val without = interest(InterestType.FLOATING, InterestIndex.CDI, "100", "1000", points)

                Then("it does not earn in that period") {
                    withPurchase.compareTo(without) shouldBe 0
                }
            }
        }

        Given("no business days") {
            When("the interest is computed") {
                Then("it is zero") {
                    interest(InterestType.FLOATING, InterestIndex.CDI, "100", "1000", emptyList()).signum() shouldBe 0
                }
            }
        }

        Given("five years of daily CDI rows and a purchase every month") {
            val points = (0 until 1260).map { IndexPoint(firstDay.plusDays(it.toLong()), BigDecimal("0.05")) }
            val flows = (0 until 60).map { Flow(firstDay.plusDays(21L * it), BigDecimal("500")) }

            When("the interest is computed") {
                val startedAt = System.nanoTime()
                val result = interest(InterestType.FLOATING, InterestIndex.CDI, "110", "1000", points, flows)
                val seconds = (System.nanoTime() - startedAt) / 1_000_000_000.0

                Then("it takes less than five seconds and earns something") {
                    (seconds < 5.0) shouldBe true
                    (result.signum() > 0) shouldBe true
                }
            }
        }

        Given("R$ 1.000 bought on the first business day at 100% of the CDI (1000.5 then 1001.00025)") {
            val points = rows(3, "0.05")

            When("the daily values are computed") {
                val values =
                    BondAccrualCalculator.dailyValues(
                        InterestType.FLOATING,
                        InterestIndex.CDI,
                        BigDecimal("100"),
                        listOf(Flow(points[0].date, BigDecimal("1000"))),
                        points,
                    )

                Then("the purchase is in the close of its day without earning, and each next day earns 0.05%") {
                    values[0].value.compareTo(BigDecimal("1000")) shouldBe 0
                    values[1].value.setScale(4, RoundingMode.HALF_UP).compareTo(BigDecimal("1000.5000")) shouldBe 0
                    values[2].value.setScale(4, RoundingMode.HALF_UP).compareTo(BigDecimal("1001.0003")) shouldBe 0
                }
            }
        }
    })
