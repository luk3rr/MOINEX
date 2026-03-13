package org.moinex.service.investment

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.moinex.factory.investment.BondFactory
import org.moinex.factory.investment.BondInterestCalculationFactory
import org.moinex.factory.investment.MarketIndicatorHistoryFactory
import org.moinex.model.enums.InterestIndex
import org.moinex.model.enums.InterestType
import org.moinex.model.investment.BondInterestCalculation
import org.moinex.repository.investment.BondInterestCalculationRepository
import org.moinex.repository.investment.BondOperationRepository
import org.moinex.repository.investment.BondRepository
import org.moinex.service.BondInterestCalculationService
import org.moinex.service.MarketIndicatorService
import java.math.BigDecimal
import java.time.LocalDate
import java.time.YearMonth
import java.util.Optional

class BondInterestCalculationServiceAdjustMonthlyInterestTest :
    BehaviorSpec({
        val bondRepository = mockk<BondRepository>()
        val bondOperationRepository = mockk<BondOperationRepository>()
        val bondInterestCalculationRepository = mockk<BondInterestCalculationRepository>()
        val marketIndicatorService = mockk<MarketIndicatorService>()

        val service =
            BondInterestCalculationService(
                bondRepository,
                bondOperationRepository,
                bondInterestCalculationRepository,
                marketIndicatorService,
            )

        afterContainer { clearAllMocks(answers = true) }

        Given("a bond with existing calculation to adjust") {
            When("adjusting monthly interest") {
                val bond =
                    BondFactory.create(
                        id = 1,
                        interestType = InterestType.FIXED,
                        interestIndex = InterestIndex.CDI,
                        interestRate = BigDecimal("10.00"),
                    )

                val month = YearMonth.of(2026, 1)

                val calculation =
                    BondInterestCalculationFactory.create(
                        id = 1,
                        bond = bond,
                        referenceMonth = month,
                        monthlyInterest = BigDecimal("10.00"),
                        accumulatedInterest = BigDecimal("10.00"),
                        manuallyAdjusted = false,
                    )

                val marketData =
                    (1..20).map { day ->
                        MarketIndicatorHistoryFactory.create(
                            indicatorType = InterestIndex.CDI,
                            referenceDate = LocalDate.of(2026, 1, day),
                            rateValue = BigDecimal("0.04"),
                        )
                    }

                every { bondRepository.findById(1) } returns Optional.of(bond)
                every {
                    bondInterestCalculationRepository.findByBondAndReferenceMonth(bond, month)
                } returns calculation
                every {
                    marketIndicatorService.getIndicatorHistoryBetween(
                        InterestIndex.CDI,
                        any(),
                        any(),
                    )
                } returns marketData
                every { bondInterestCalculationRepository.findByBondAndReferenceMonthAfter(bond, month) } returns emptyList()
                every { bondInterestCalculationRepository.save(any()) } returnsArgument 0
                every { bondInterestCalculationRepository.saveAll(any<List<BondInterestCalculation>>()) } returnsArgument 0

                val newInterest = BigDecimal("15.00")
                service.adjustMonthlyInterest(1, month, newInterest)

                Then("should update monthly interest") {
                    calculation.monthlyInterest shouldBe newInterest
                }

                Then("should mark as manually adjusted") {
                    calculation.manuallyAdjusted shouldBe true
                }

                Then("should update accumulated interest") {
                    calculation.accumulatedInterest shouldBe BigDecimal("15.00")
                }

                Then("should save the calculation") {
                    verify { bondInterestCalculationRepository.save(calculation) }
                }
            }
        }

        Given("a bond with calculation and future months") {
            When("adjusting monthly interest") {
                val bond =
                    BondFactory.create(
                        id = 1,
                        interestType = InterestType.FIXED,
                        interestIndex = InterestIndex.CDI,
                        interestRate = BigDecimal("10.00"),
                    )

                val adjustMonth = YearMonth.of(2026, 1)
                val futureMonth = YearMonth.of(2026, 2)

                val calculation =
                    BondInterestCalculationFactory.create(
                        id = 1,
                        bond = bond,
                        referenceMonth = adjustMonth,
                        monthlyInterest = BigDecimal("10.00"),
                        accumulatedInterest = BigDecimal("10.00"),
                    )

                val futureCalculation =
                    BondInterestCalculationFactory.create(
                        id = 2,
                        bond = bond,
                        referenceMonth = futureMonth,
                        monthlyInterest = BigDecimal("12.00"),
                        accumulatedInterest = BigDecimal("22.00"),
                    )

                val marketData =
                    (1..20).map { day ->
                        MarketIndicatorHistoryFactory.create(
                            indicatorType = InterestIndex.CDI,
                            referenceDate = LocalDate.of(2026, 1, day),
                            rateValue = BigDecimal("0.04"),
                        )
                    }

                every { bondRepository.findById(1) } returns Optional.of(bond)
                every {
                    bondInterestCalculationRepository.findByBondAndReferenceMonth(bond, adjustMonth)
                } returns calculation
                every {
                    bondInterestCalculationRepository.findByBondAndReferenceMonthAfter(bond, adjustMonth)
                } returns listOf(futureCalculation)
                every {
                    marketIndicatorService.getIndicatorHistoryBetween(
                        InterestIndex.CDI,
                        any(),
                        any(),
                    )
                } returns marketData
                every { bondInterestCalculationRepository.save(any()) } returnsArgument 0
                every { bondInterestCalculationRepository.saveAll(any<List<BondInterestCalculation>>()) } returnsArgument 0

                service.adjustMonthlyInterest(1, adjustMonth, BigDecimal("15.00"))

                Then("should propagate delta to future months") {
                    futureCalculation.accumulatedInterest shouldBe BigDecimal("27.00")
                }

                Then("should save all affected calculations") {
                    verify { bondInterestCalculationRepository.saveAll(listOf(futureCalculation)) }
                }
            }
        }

        Given("a non-existent calculation to adjust") {
            When("trying to adjust") {
                val bond =
                    BondFactory.create(
                        id = 1,
                        interestType = InterestType.FIXED,
                        interestIndex = InterestIndex.CDI,
                        interestRate = BigDecimal("10.00"),
                    )

                val month = YearMonth.of(2026, 1)

                every { bondRepository.findById(1) } returns Optional.of(bond)
                every {
                    bondInterestCalculationRepository.findByBondAndReferenceMonth(bond, month)
                } returns null

                Then("should throw IllegalArgumentException") {
                    shouldThrow<IllegalArgumentException> {
                        service.adjustMonthlyInterest(1, month, BigDecimal("15.00"))
                    }
                }
            }
        }

        Given("multiple future months after adjustment") {
            When("adjusting monthly interest") {
                val bond =
                    BondFactory.create(
                        id = 1,
                        interestType = InterestType.FIXED,
                        interestIndex = InterestIndex.CDI,
                        interestRate = BigDecimal("10.00"),
                    )

                val adjustMonth = YearMonth.of(2026, 1)

                val calculation =
                    BondInterestCalculationFactory.create(
                        id = 1,
                        bond = bond,
                        referenceMonth = adjustMonth,
                        monthlyInterest = BigDecimal("10.00"),
                        accumulatedInterest = BigDecimal("10.00"),
                    )

                val futureCalculations =
                    listOf(
                        BondInterestCalculationFactory.create(
                            id = 2,
                            bond = bond,
                            referenceMonth = YearMonth.of(2026, 2),
                            monthlyInterest = BigDecimal("12.00"),
                            accumulatedInterest = BigDecimal("22.00"),
                        ),
                        BondInterestCalculationFactory.create(
                            id = 3,
                            bond = bond,
                            referenceMonth = YearMonth.of(2026, 3),
                            monthlyInterest = BigDecimal("11.00"),
                            accumulatedInterest = BigDecimal("33.00"),
                        ),
                    )

                val marketData =
                    (1..20).map { day ->
                        MarketIndicatorHistoryFactory.create(
                            indicatorType = InterestIndex.CDI,
                            referenceDate = LocalDate.of(2026, 1, day),
                            rateValue = BigDecimal("0.04"),
                        )
                    }

                every { bondRepository.findById(1) } returns Optional.of(bond)
                every {
                    bondInterestCalculationRepository.findByBondAndReferenceMonth(bond, adjustMonth)
                } returns calculation
                every {
                    bondInterestCalculationRepository.findByBondAndReferenceMonthAfter(bond, adjustMonth)
                } returns futureCalculations
                every {
                    marketIndicatorService.getIndicatorHistoryBetween(
                        InterestIndex.CDI,
                        any(),
                        any(),
                    )
                } returns marketData
                every { bondInterestCalculationRepository.save(any()) } returnsArgument 0
                every { bondInterestCalculationRepository.saveAll(any<List<BondInterestCalculation>>()) } returnsArgument 0

                service.adjustMonthlyInterest(1, adjustMonth, BigDecimal("20.00"))

                Then("should propagate delta to all future months") {
                    futureCalculations[0].accumulatedInterest shouldBe BigDecimal("32.00")
                    futureCalculations[1].accumulatedInterest shouldBe BigDecimal("43.00")
                }
            }
        }

        Given("a bond with negative adjustment") {
            When("adjusting monthly interest to lower value") {
                val bond =
                    BondFactory.create(
                        id = 1,
                        interestType = InterestType.FIXED,
                        interestIndex = InterestIndex.CDI,
                        interestRate = BigDecimal("10.00"),
                    )

                val month = YearMonth.of(2026, 1)

                val calculation =
                    BondInterestCalculationFactory.create(
                        id = 1,
                        bond = bond,
                        referenceMonth = month,
                        monthlyInterest = BigDecimal("20.00"),
                        accumulatedInterest = BigDecimal("20.00"),
                    )

                val futureCalculation =
                    BondInterestCalculationFactory.create(
                        id = 2,
                        bond = bond,
                        referenceMonth = YearMonth.of(2026, 2),
                        monthlyInterest = BigDecimal("15.00"),
                        accumulatedInterest = BigDecimal("35.00"),
                    )

                val marketData =
                    (1..20).map { day ->
                        MarketIndicatorHistoryFactory.create(
                            indicatorType = InterestIndex.CDI,
                            referenceDate = LocalDate.of(2026, 1, day),
                            rateValue = BigDecimal("0.04"),
                        )
                    }

                every { bondRepository.findById(1) } returns Optional.of(bond)
                every {
                    bondInterestCalculationRepository.findByBondAndReferenceMonth(bond, month)
                } returns calculation
                every {
                    bondInterestCalculationRepository.findByBondAndReferenceMonthAfter(bond, month)
                } returns listOf(futureCalculation)
                every {
                    marketIndicatorService.getIndicatorHistoryBetween(
                        InterestIndex.CDI,
                        any(),
                        any(),
                    )
                } returns marketData
                every { bondInterestCalculationRepository.save(any()) } returnsArgument 0
                every { bondInterestCalculationRepository.saveAll(any<List<BondInterestCalculation>>()) } returnsArgument 0

                service.adjustMonthlyInterest(1, month, BigDecimal("10.00"))

                Then("should reduce accumulated interest") {
                    calculation.accumulatedInterest shouldBe BigDecimal("10.00")
                }

                Then("should propagate negative delta to future months") {
                    futureCalculation.accumulatedInterest shouldBe BigDecimal("25.00")
                }
            }
        }

        Given("trying to adjust non-existent calculation") {
            When("adjusting monthly interest") {
                val bond =
                    BondFactory.create(
                        id = 1,
                        interestType = InterestType.FIXED,
                        interestIndex = InterestIndex.CDI,
                        interestRate = BigDecimal("10.00"),
                    )

                val month = YearMonth.of(2026, 1)

                every { bondRepository.findById(1) } returns Optional.of(bond)
                every {
                    bondInterestCalculationRepository.findByBondAndReferenceMonth(bond, month)
                } returns null

                Then("should throw IllegalArgumentException") {
                    shouldThrow<IllegalArgumentException> {
                        service.adjustMonthlyInterest(1, month, BigDecimal("15.00"))
                    }
                }
            }
        }
    })
