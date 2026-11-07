package org.moinex.service.investment

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import org.moinex.common.ClockProvider
import org.moinex.factory.investment.BondFactory
import org.moinex.factory.investment.BondOperationFactory
import org.moinex.factory.investment.MarketIndicatorHistoryFactory
import org.moinex.factory.wallet.WalletTransactionFactory
import org.moinex.model.enums.InterestIndex
import org.moinex.model.enums.InterestType
import org.moinex.model.enums.OperationType
import org.moinex.model.investment.Bond
import org.moinex.model.investment.BondInterestCalculation
import org.moinex.model.investment.BondOperation
import org.moinex.repository.investment.BondInterestCalculationRepository
import org.moinex.repository.investment.BondOperationRepository
import org.moinex.repository.investment.BondRepository
import org.moinex.service.NotificationService
import org.moinex.service.PreferencesService
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth

class BondInterestCalculationServiceAccrualTest :
    BehaviorSpec({
        val defaultToday = LocalDate.of(2026, 3, 20)

        fun harness(
            bond: Bond,
            operations: List<BondOperation>,
            stored: MutableMap<YearMonth, BondInterestCalculation> = mutableMapOf(),
            today: LocalDate = defaultToday,
            cdi: String = "0.05",
        ): BondInterestCalculationService {
            val bondRepository = mockk<BondRepository>()
            val operationRepository = mockk<BondOperationRepository>()
            val calculationRepository = mockk<BondInterestCalculationRepository>()
            val indicatorService = mockk<MarketIndicatorService>()
            val clockProvider = mockk<ClockProvider>()

            every { bondRepository.findById(bond.id!!) } returns java.util.Optional.of(bond)
            every { clockProvider.now() } returns today.atTime(12, 0)
            every { operationRepository.findByBondOrderByOperationDateAsc(bond) } returns operations
            every { calculationRepository.findLastCalculatedMonth(bond) } answers {
                stored.values.maxByOrNull { it.referenceMonth }
            }
            every { calculationRepository.findByBondAndReferenceMonth(bond, any()) } answers {
                stored[secondArg()]
            }
            every { calculationRepository.findByBondOrderByReferenceMonthAsc(bond) } answers {
                stored.values.sortedBy { it.referenceMonth }
            }
            every { calculationRepository.save(any()) } answers {
                val saved = firstArg<BondInterestCalculation>()
                stored[saved.referenceMonth] = saved
                saved
            }
            every { indicatorService.getIndicatorHistoryBetween(InterestIndex.CDI, any(), any()) } answers {
                val start = secondArg<LocalDate>()
                val end = thirdArg<LocalDate>()
                generateSequence(start) { it.plusDays(1) }
                    .takeWhile { !it.isAfter(minOf(end, today)) }
                    .filter { it.dayOfWeek != DayOfWeek.SATURDAY && it.dayOfWeek != DayOfWeek.SUNDAY }
                    .map {
                        MarketIndicatorHistoryFactory.create(
                            indicatorType = InterestIndex.CDI,
                            referenceDate = it,
                            rateValue = BigDecimal(cdi),
                        )
                    }.toList()
            }

            return BondInterestCalculationService(
                bondRepository,
                operationRepository,
                calculationRepository,
                indicatorService,
                mockk<NotificationService>(relaxed = true),
                mockk<PreferencesService>(relaxed = true),
                clockProvider,
            )
        }

        fun runMonths(
            bond: Bond,
            operations: List<BondOperation>,
            cdi: String = "0.05",
        ): Map<YearMonth, BondInterestCalculation> {
            val stored = mutableMapOf<YearMonth, BondInterestCalculation>()
            harness(bond, operations, stored, cdi = cdi).calculateAndStoreMonthlyInterest(bond)
            return stored
        }

        fun buy(
            bond: Bond,
            id: Int,
            date: LocalDate,
            amount: String,
            spread: BigDecimal? = null,
        ) = BondOperationFactory.create(
            id = id,
            bond = bond,
            operationType = OperationType.BUY,
            quantity = BigDecimal(amount),
            unitPrice = BigDecimal("1.00"),
            spread = spread,
            walletTransaction =
                WalletTransactionFactory.create(
                    id = id,
                    date = LocalDateTime.of(date, java.time.LocalTime.NOON),
                ),
        )

        fun BigDecimal.cents(): BigDecimal = setScale(2, RoundingMode.HALF_UP)

        Given("R$ 1.000 bought on 2026-01-05 in a bond at 110% of the CDI, with the CDI at 0.05% on every weekday") {
            val bond =
                BondFactory.create(
                    id = 1,
                    interestType = InterestType.FLOATING,
                    interestIndex = InterestIndex.CDI,
                    interestRate = BigDecimal("110"),
                )

            When("the interest is calculated up to 2026-03-20") {
                val result = runMonths(bond, listOf(buy(bond, 1, LocalDate.of(2026, 1, 5), "1000")))

                Then("each month earns on the whole balance, compounding from month to month") {
                    result[YearMonth.of(2026, 1)]!!.monthlyInterest.cents().compareTo(BigDecimal("10.50")) shouldBe 0
                    result[YearMonth.of(2026, 2)]!!.monthlyInterest.cents().compareTo(BigDecimal("11.17")) shouldBe 0
                    result[YearMonth.of(2026, 3)]!!.monthlyInterest.cents().compareTo(BigDecimal("8.46")) shouldBe 0
                    result[YearMonth.of(2026, 3)]!!.accumulatedInterest.cents().compareTo(BigDecimal("30.14")) shouldBe
                        0
                    result[YearMonth.of(2026, 3)]!!.finalValue.cents().compareTo(BigDecimal("1030.14")) shouldBe 0
                }
            }
        }

        Given("a bond at 110% of the CDI and a purchase that carries 100% as its own rate") {
            val bond =
                BondFactory.create(
                    id = 1,
                    interestType = InterestType.FLOATING,
                    interestIndex = InterestIndex.CDI,
                    interestRate = BigDecimal("110"),
                )

            When("the interest of a month with only that purchase is calculated") {
                val own = runMonths(bond, listOf(buy(bond, 1, LocalDate.of(2026, 1, 5), "1000", BigDecimal("100"))))
                val inherited = runMonths(bond, listOf(buy(bond, 1, LocalDate.of(2026, 1, 5), "1000")))

                Then("the purchase uses its rate and the others use the rate of the bond") {
                    own[YearMonth.of(2026, 1)]!!.monthlyInterest.cents().compareTo(BigDecimal("9.54")) shouldBe 0
                    inherited[YearMonth.of(2026, 1)]!!.monthlyInterest.cents().compareTo(BigDecimal("10.50")) shouldBe 0
                }
            }
        }

        Given("a purchase and a sale inside the same month") {
            val bond =
                BondFactory.create(
                    id = 1,
                    interestType = InterestType.FLOATING,
                    interestIndex = InterestIndex.CDI,
                    interestRate = BigDecimal("100"),
                )

            When("the interest of the month is calculated") {
                val withMidMonthSale =
                    runMonths(
                        bond,
                        listOf(
                            buy(bond, 1, LocalDate.of(2026, 1, 5), "1000"),
                            BondOperationFactory.create(
                                id = 2,
                                bond = bond,
                                operationType = OperationType.SELL,
                                quantity = BigDecimal("1000"),
                                unitPrice = BigDecimal("1.00"),
                                walletTransaction =
                                    WalletTransactionFactory.create(
                                        id = 2,
                                        date = LocalDateTime.of(2026, 1, 15, 12, 0),
                                    ),
                            ),
                        ),
                    )

                Then(
                    "the principal earns through the day of the sale and the interest already earned keeps earning: R$ 4.03",
                ) {
                    withMidMonthSale[
                        YearMonth.of(
                            2026,
                            1,
                        ),
                    ]!!.monthlyInterest.cents().compareTo(BigDecimal("4.03")) shouldBe
                        0
                }
            }
        }

        Given("the interest of the current month calculated on two different days") {
            val bond =
                BondFactory.create(
                    id = 1,
                    interestType = InterestType.FLOATING,
                    interestIndex = InterestIndex.CDI,
                    interestRate = BigDecimal("110"),
                )
            val operations = listOf(buy(bond, 1, LocalDate.of(2026, 1, 5), "1000"))

            When("the month is calculated again five days later") {
                val stored = mutableMapOf<YearMonth, BondInterestCalculation>()
                harness(bond, operations, stored).calculateAndStoreMonthlyInterest(bond)
                harness(
                    bond,
                    operations,
                    stored,
                    today = LocalDate.of(2026, 3, 25),
                ).calculateAndStoreMonthlyInterest(bond)

                Then("the month is not added twice: accumulated is the sum of the months, R$ 31.84 at 2026-03-25") {
                    val march = stored[YearMonth.of(2026, 3)]!!
                    val sumOfMonths =
                        stored.values.fold(BigDecimal.ZERO) { acc, row -> acc.add(row.monthlyInterest) }

                    march.accumulatedInterest.cents().compareTo(BigDecimal("31.84")) shouldBe 0
                    (march.accumulatedInterest.subtract(sumOfMonths).abs() < BigDecimal("0.02")) shouldBe true
                }
            }
        }

        Given("a sale that takes R$ 500 of principal and R$ 6 of interest out of R$ 1.000 held at 100% of the CDI") {
            val bond =
                BondFactory.create(
                    id = 1,
                    interestType = InterestType.FLOATING,
                    interestIndex = InterestIndex.CDI,
                    interestRate = BigDecimal("100"),
                )
            val sale =
                BondOperationFactory.create(
                    id = 2,
                    bond = bond,
                    operationType = OperationType.SELL,
                    quantity = BigDecimal("500"),
                    unitPrice = BigDecimal("1.00"),
                    taxes = BigDecimal("1.00"),
                    netProfit = BigDecimal("5.00"),
                    walletTransaction =
                        WalletTransactionFactory.create(
                            id = 2,
                            date = LocalDateTime.of(2026, 1, 30, 12, 0),
                        ),
                )

            When("the interest to the end of the month is calculated") {
                val result = runMonths(bond, listOf(buy(bond, 1, LocalDate.of(2026, 1, 5), "1000"), sale))

                Then("the final value is the money still in the bond: R$ 1.000 grown, less R$ 506 taken out") {
                    result[YearMonth.of(2026, 1)]!!.finalValue.cents().compareTo(BigDecimal("503.54")) shouldBe 0
                    result[YearMonth.of(2026, 1)]!!.monthlyInterest.cents().compareTo(BigDecimal("9.54")) shouldBe 0
                    result[YearMonth.of(2026, 1)]!!.accumulatedInterest.cents().compareTo(BigDecimal("3.54")) shouldBe 0
                }
            }
        }

        Given("a stored history with a wrong value in a month calculated by the automatic rule") {
            val bond =
                BondFactory.create(
                    id = 1,
                    interestType = InterestType.FLOATING,
                    interestIndex = InterestIndex.CDI,
                    interestRate = BigDecimal("100"),
                )
            val operations = listOf(buy(bond, 1, LocalDate.of(2026, 1, 5), "1000"))
            val stored = mutableMapOf<YearMonth, BondInterestCalculation>()
            val service = harness(bond, operations, stored)
            service.calculateAndStoreMonthlyInterest(bond)
            val january = stored[YearMonth.of(2026, 1)]!!
            val correct = january.monthlyInterest
            january.monthlyInterest = BigDecimal("999.00")

            When("previewing the recalculation") {
                val rows = service.previewRecalculation(1)
                val row = rows.first { it.month == YearMonth.of(2026, 1) }

                Then("it shows the value stored before and the value the rule gives after") {
                    row.monthlyBefore!!.compareTo(BigDecimal("999.00")) shouldBe 0
                    row.monthlyAfter.cents().compareTo(correct.cents()) shouldBe 0
                }
            }
        }
    })
