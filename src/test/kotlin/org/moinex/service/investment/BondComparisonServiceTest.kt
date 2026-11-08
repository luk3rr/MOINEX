package org.moinex.service.investment

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import org.moinex.common.ClockProvider
import org.moinex.factory.investment.BondFactory
import org.moinex.factory.investment.BondInterestCalculationFactory
import org.moinex.factory.investment.BondOperationFactory
import org.moinex.factory.investment.MarketIndicatorHistoryFactory
import org.moinex.factory.wallet.WalletTransactionFactory
import org.moinex.model.dto.BondComparisonRequest
import org.moinex.model.dto.ComparisonPeriod
import org.moinex.model.enums.BondType
import org.moinex.model.enums.InterestIndex
import org.moinex.model.enums.InterestType
import org.moinex.model.enums.OperationType
import org.moinex.model.investment.Bond
import org.moinex.model.investment.BondOperation
import org.moinex.repository.investment.BondOperationRepository
import org.moinex.repository.investment.BondRepository
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.YearMonth

class BondComparisonServiceTest :
    BehaviorSpec({
        val today = LocalDate.of(2026, 3, 20)

        class Harness(
            val active: List<Bond>,
            val archived: List<Bond> = emptyList(),
            val operations: Map<Bond, List<BondOperation>>,
            val histories: Map<Bond, List<org.moinex.model.investment.BondInterestCalculation>> = emptyMap(),
            val indexHasData: (InterestIndex) -> Boolean = { true },
        ) {
            val service: BondComparisonService

            init {
                val bondRepository = mockk<BondRepository>()
                val operationRepository = mockk<BondOperationRepository>()
                val indicatorService = mockk<MarketIndicatorService>()
                val interestService = mockk<BondInterestCalculationService>()
                val clock = mockk<ClockProvider>()

                every { clock.today() } returns today
                every { bondRepository.findByArchivedFalseOrderByNameAsc() } returns active
                every { bondRepository.findByArchivedTrueOrderByNameAsc() } returns archived
                (active + archived).forEach { bond ->
                    every { operationRepository.findByBondOrderByOperationDateAsc(bond) } returns
                        operations.getValue(bond)
                    every { interestService.getMonthlyInterestHistory(bond) } returns histories[bond].orEmpty()
                }
                every { indicatorService.getIndicatorHistoryBetween(any(), any(), any()) } answers {
                    val index = firstArg<InterestIndex>()
                    val start = secondArg<LocalDate>()
                    val end = thirdArg<LocalDate>()
                    if (!indexHasData(index)) {
                        emptyList()
                    } else {
                        generateSequence(start) { it.plusDays(1) }
                            .takeWhile { !it.isAfter(minOf(end, today)) }
                            .filter { it.dayOfWeek != DayOfWeek.SATURDAY && it.dayOfWeek != DayOfWeek.SUNDAY }
                            .map {
                                MarketIndicatorHistoryFactory.create(
                                    indicatorType = index,
                                    referenceDate = it,
                                    rateValue = BigDecimal("0.05"),
                                )
                            }.toList()
                    }
                }

                service =
                    BondComparisonService(
                        bondRepository,
                        operationRepository,
                        BondDailySeries(operationRepository, indicatorService),
                        interestService,
                        indicatorService,
                        clock,
                    )
            }
        }

        fun bond(
            id: Int,
            rate: String = "100",
            type: BondType = BondType.CDB,
            index: InterestIndex = InterestIndex.CDI,
        ): Bond =
            BondFactory.create(
                id = id,
                name = "Bond $id",
                type = type,
                interestType = InterestType.FLOATING,
                interestIndex = index,
                interestRate = BigDecimal(rate),
            )

        fun buy(
            bond: Bond,
            id: Int,
            date: LocalDate,
            amount: String,
        ): BondOperation =
            BondOperationFactory.create(
                id = id,
                bond = bond,
                operationType = OperationType.BUY,
                quantity = BigDecimal(amount),
                unitPrice = BigDecimal("1.00"),
                walletTransaction =
                    WalletTransactionFactory.create(
                        id = id,
                        date = LocalDateTime.of(date, LocalTime.NOON),
                    ),
            )

        val request = BondComparisonRequest(ComparisonPeriod.M12, net = false, includeArchived = false)

        fun BigDecimal.cents() = setScale(2, RoundingMode.HALF_UP)

        Given("a bond at 100% of the CDI bought on 2026-01-05, with the CDI at 0.05% on every weekday") {
            val a = bond(1)
            val harness =
                Harness(listOf(a), operations = mapOf(a to listOf(buy(a, 1, LocalDate.of(2026, 1, 5), "1000"))))

            When("comparing over 12 months") {
                val result = harness.service.comparison(request)
                val line = result.lines.single()

                Then("the line starts at 0% on the day of the purchase and ends at 2.74%") {
                    line.points.first().date shouldBe LocalDate.of(2026, 1, 5)
                    line.points
                        .first()
                        .pct
                        .signum() shouldBe 0
                    line.resultPct.cents().compareTo(BigDecimal("2.74")) shouldBe 0
                }

                Then("the CDI line covers the same days and ends at the same 2.74%") {
                    result.cdi.first().date shouldBe line.points.first().date
                    result.cdi
                        .last()
                        .pct
                        .cents()
                        .compareTo(BigDecimal("2.74")) shouldBe 0
                }
            }
        }

        Given("a bond at 120% of the CDI") {
            val a = bond(1, rate = "120")
            val harness =
                Harness(listOf(a), operations = mapOf(a to listOf(buy(a, 1, LocalDate.of(2026, 1, 5), "1000"))))

            When("comparing over 12 months") {
                val result = harness.service.comparison(request)

                Then("it ends at 3.29% against 2.74% of the CDI") {
                    result.lines
                        .single()
                        .resultPct
                        .cents()
                        .compareTo(BigDecimal("3.29")) shouldBe 0
                    result.cdi
                        .last()
                        .pct
                        .cents()
                        .compareTo(BigDecimal("2.74")) shouldBe 0
                }
            }
        }

        Given("a second purchase of the same amount a month later") {
            val a = bond(1)
            val operations =
                listOf(buy(a, 1, LocalDate.of(2026, 1, 5), "1000"), buy(a, 2, LocalDate.of(2026, 2, 5), "1000"))
            val harness = Harness(listOf(a), operations = mapOf(a to operations))

            When("comparing over 12 months") {
                val line =
                    harness.service
                        .comparison(request)
                        .lines
                        .single()

                Then("the money put in is not a gain: the return is still 2.74%") {
                    line.resultPct.cents().compareTo(BigDecimal("2.74")) shouldBe 0
                }
            }
        }

        Given("two bonds, one bought in January and one in February") {
            val a = bond(1)
            val b = bond(2)
            val harness =
                Harness(
                    listOf(a, b),
                    operations =
                        mapOf(
                            a to listOf(buy(a, 1, LocalDate.of(2026, 1, 5), "1000")),
                            b to listOf(buy(b, 2, LocalDate.of(2026, 2, 5), "1000")),
                        ),
                )

            When("comparing over 12 months") {
                val result = harness.service.comparison(request)

                Then("each line starts at 0% on its own first purchase and both end on the same day") {
                    val first = result.lines.first { it.bond == a }
                    val second = result.lines.first { it.bond == b }
                    first.points.first().date shouldBe LocalDate.of(2026, 1, 5)
                    second.points.first().date shouldBe LocalDate.of(2026, 2, 5)
                    second.points
                        .first()
                        .pct
                        .signum() shouldBe 0
                    first.points.last().date shouldBe second.points.last().date
                }
            }
        }

        Given("a bond whose index has no history") {
            val a = bond(1)
            val orphan = bond(2, index = InterestIndex.IPCA)
            val harness =
                Harness(
                    listOf(a, orphan),
                    operations =
                        mapOf(
                            a to listOf(buy(a, 1, LocalDate.of(2026, 1, 5), "1000")),
                            orphan to listOf(buy(orphan, 2, LocalDate.of(2026, 1, 5), "1000")),
                        ),
                    indexHasData = { it == InterestIndex.CDI },
                )

            When("comparing") {
                val result = harness.service.comparison(request)

                Then("it is left out and reported") {
                    result.lines shouldHaveSize 1
                    result.excluded shouldBe listOf(orphan)
                }
            }
        }

        Given("an archived bond") {
            val a = bond(1)
            val old = bond(2)
            val harness =
                Harness(
                    listOf(a),
                    archived = listOf(old),
                    operations =
                        mapOf(
                            a to listOf(buy(a, 1, LocalDate.of(2026, 1, 5), "1000")),
                            old to listOf(buy(old, 2, LocalDate.of(2026, 1, 5), "1000")),
                        ),
                )

            When("comparing without and with archived bonds") {
                val without = harness.service.comparison(request)
                val with = harness.service.comparison(request.copy(includeArchived = true))

                Then("it only appears when asked") {
                    without.lines shouldHaveSize 1
                    with.lines shouldHaveSize 2
                }
            }
        }

        Given("a CDB held for less than 180 days, in net terms") {
            val a = bond(1)
            val harness =
                Harness(listOf(a), operations = mapOf(a to listOf(buy(a, 1, LocalDate.of(2026, 1, 5), "1000"))))

            When("comparing net of the estimated tax") {
                val gross =
                    harness.service
                        .comparison(request)
                        .lines
                        .single()
                val net =
                    harness.service
                        .comparison(request.copy(net = true))
                        .lines
                        .single()

                Then("the interest is taxed at 22.5%: 2.74% gross becomes 2.12% net") {
                    gross.resultPct.cents().compareTo(BigDecimal("2.74")) shouldBe 0
                    net.resultPct.cents().compareTo(BigDecimal("2.12")) shouldBe 0
                }
            }
        }

        Given("a LCI, in net terms") {
            val a = bond(1, type = BondType.LCI)
            val harness =
                Harness(listOf(a), operations = mapOf(a to listOf(buy(a, 1, LocalDate.of(2026, 1, 5), "1000"))))

            When("comparing net and gross") {
                val gross =
                    harness.service
                        .comparison(request)
                        .lines
                        .single()
                val net =
                    harness.service
                        .comparison(request.copy(net = true))
                        .lines
                        .single()

                Then("there is no tax, so they are the same") {
                    net.resultPct.compareTo(gross.resultPct) shouldBe 0
                }
            }
        }

        Given("the period ALL and two bonds") {
            val a = bond(1)
            val b = bond(2)
            val harness =
                Harness(
                    listOf(a, b),
                    operations =
                        mapOf(
                            a to listOf(buy(a, 1, LocalDate.of(2026, 1, 5), "1000")),
                            b to listOf(buy(b, 2, LocalDate.of(2026, 2, 5), "1000")),
                        ),
                )

            When("comparing from the first purchase") {
                val result = harness.service.comparison(request.copy(period = ComparisonPeriod.ALL))

                Then("the CDI starts on the first purchase of any bond") {
                    result.cdi.first().date shouldBe LocalDate.of(2026, 1, 5)
                }
            }
        }

        Given("no bonds") {
            val harness = Harness(emptyList(), operations = emptyMap())

            When("comparing") {
                val result = harness.service.comparison(request)

                Then("everything is empty") {
                    result.lines shouldHaveSize 0
                    result.cdi shouldHaveSize 0
                    result.excluded shouldHaveSize 0
                }
            }
        }

        Given("two bonds that earned R$ 20 and R$ 10 in March, a CDB held for 100 days and a LCI") {
            val cdb = bond(1)
            val lci = bond(2, type = BondType.LCI)
            val march = YearMonth.of(2026, 3)
            val harness =
                Harness(
                    listOf(cdb, lci),
                    operations =
                        mapOf(
                            cdb to listOf(buy(cdb, 1, today.minusDays(100), "1000")),
                            lci to listOf(buy(lci, 2, today.minusDays(100), "1000")),
                        ),
                    histories =
                        mapOf(
                            cdb to
                                listOf(
                                    BondInterestCalculationFactory.create(
                                        bond = cdb,
                                        referenceMonth = march,
                                        monthlyInterest = BigDecimal("20.00"),
                                        accumulatedInterest = BigDecimal("100.00"),
                                    ),
                                ),
                            lci to
                                listOf(
                                    BondInterestCalculationFactory.create(
                                        bond = lci,
                                        referenceMonth = march,
                                        monthlyInterest = BigDecimal("10.00"),
                                        accumulatedInterest = BigDecimal("50.00"),
                                    ),
                                ),
                        ),
                )

            When("asking for the monthly interest, gross and net") {
                val gross = harness.service.monthly(request)
                val net = harness.service.monthly(request.copy(net = true))

                Then("gross shows R$ 20.00 and R$ 10.00 in March") {
                    gross.series
                        .first { it.bond == cdb }
                        .values
                        .getValue(march)
                        .compareTo(BigDecimal("20.00")) shouldBe
                        0
                    gross.series
                        .first { it.bond == lci }
                        .values
                        .getValue(march)
                        .compareTo(BigDecimal("10.00")) shouldBe
                        0
                }

                Then("net takes the tax of the CDB, 22.5%, and none of the LCI") {
                    net.series
                        .first { it.bond == cdb }
                        .values
                        .getValue(march)
                        .compareTo(BigDecimal("15.50")) shouldBe 0
                    net.series
                        .first { it.bond == lci }
                        .values
                        .getValue(march)
                        .compareTo(BigDecimal("10.00")) shouldBe 0
                }
            }
        }

        Given("20 bonds bought on the same day") {
            val bonds = (1..20).map { bond(it) }
            val harness =
                Harness(
                    bonds,
                    operations = bonds.associateWith { listOf(buy(it, it.id!!, LocalDate.of(2026, 1, 5), "1000")) },
                )

            When("comparing over 12 months, gross and net") {
                val startedAt = System.nanoTime()
                val gross = harness.service.comparison(request)
                val net = harness.service.comparison(request.copy(net = true))
                val seconds = (System.nanoTime() - startedAt) / 1_000_000_000.0

                Then("both are built in less than two seconds") {
                    gross.lines shouldHaveSize 20
                    net.lines shouldHaveSize 20
                    (seconds < 2.0) shouldBe true
                }
            }
        }
    })
