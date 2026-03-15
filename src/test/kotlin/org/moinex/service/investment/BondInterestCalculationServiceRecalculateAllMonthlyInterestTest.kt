package org.moinex.service.investment

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import jakarta.persistence.EntityNotFoundException
import org.moinex.factory.investment.BondFactory
import org.moinex.factory.investment.BondInterestCalculationFactory
import org.moinex.factory.investment.BondOperationFactory
import org.moinex.factory.investment.MarketIndicatorHistoryFactory
import org.moinex.factory.wallet.WalletTransactionFactory
import org.moinex.model.enums.InterestIndex
import org.moinex.model.enums.InterestType
import org.moinex.model.enums.OperationType
import org.moinex.repository.investment.BondInterestCalculationRepository
import org.moinex.repository.investment.BondOperationRepository
import org.moinex.repository.investment.BondRepository
import java.math.BigDecimal
import java.time.LocalDate
import java.time.YearMonth
import java.util.Optional

class BondInterestCalculationServiceRecalculateAllMonthlyInterestTest :
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

        Given("a bond with existing calculations to recalculate") {
            When("recalculating all monthly interest") {
                val bond =
                    BondFactory.create(
                        id = 1,
                        interestType = InterestType.FIXED,
                        interestIndex = InterestIndex.CDI,
                        interestRate = BigDecimal("10.00"),
                    )

                val walletTransaction =
                    WalletTransactionFactory.create(
                        id = 1,
                        date = LocalDate.of(2026, 1, 15).atStartOfDay(),
                    )

                val operation =
                    BondOperationFactory.create(
                        id = 1,
                        bond = bond,
                        operationType = OperationType.BUY,
                        quantity = BigDecimal("100"),
                        unitPrice = BigDecimal("10.00"),
                        walletTransaction = walletTransaction,
                    )

                val existingCalculations =
                    listOf(
                        BondInterestCalculationFactory.create(
                            id = 1,
                            bond = bond,
                            referenceMonth = YearMonth.of(2026, 1),
                        ),
                        BondInterestCalculationFactory.create(
                            id = 2,
                            bond = bond,
                            referenceMonth = YearMonth.of(2026, 2),
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
                every { bondOperationRepository.findByBondOrderByOperationDateAsc(bond) } returns listOf(operation)
                every { bondInterestCalculationRepository.deleteByBond(bond) } returns Unit
                every {
                    marketIndicatorService.getIndicatorHistoryBetween(
                        InterestIndex.CDI,
                        any(),
                        any(),
                    )
                } returns marketData
                every { bondInterestCalculationRepository.findLastCalculatedMonth(bond) } returns null
                every { bondInterestCalculationRepository.findByBondAndReferenceMonth(any(), any()) } returns null
                every { bondInterestCalculationRepository.save(any()) } returnsArgument 0

                service.recalculateAllMonthlyInterest(1)

                Then("should delete existing calculations") {
                    verify { bondInterestCalculationRepository.deleteByBond(bond) }
                }

                Then("should recalculate from scratch") {
                    verify { bondOperationRepository.findByBondOrderByOperationDateAsc(bond) }
                }

                Then("should save new calculations") {
                    verify(atLeast = 1) { bondInterestCalculationRepository.save(any()) }
                }
            }
        }

        Given("a bond without operations") {
            When("trying to recalculate") {
                val bond =
                    BondFactory.create(
                        id = 1,
                        interestType = InterestType.FIXED,
                        interestIndex = InterestIndex.CDI,
                        interestRate = BigDecimal("10.00"),
                    )

                every { bondRepository.findById(1) } returns Optional.of(bond)
                every { bondOperationRepository.findByBondOrderByOperationDateAsc(bond) } returns emptyList()

                service.recalculateAllMonthlyInterest(1)

                Then("should not delete calculations") {
                    verify(exactly = 0) { bondInterestCalculationRepository.deleteByBond(any()) }
                }

                Then("should not save new calculations") {
                    verify(exactly = 0) { bondInterestCalculationRepository.save(any()) }
                }
            }
        }

        Given("a bond with manually adjusted calculations") {
            When("recalculating all monthly interest") {
                val bond =
                    BondFactory.create(
                        id = 1,
                        interestType = InterestType.FIXED,
                        interestIndex = InterestIndex.CDI,
                        interestRate = BigDecimal("10.00"),
                    )

                val walletTransaction =
                    WalletTransactionFactory.create(
                        id = 1,
                        date = LocalDate.of(2026, 1, 15).atStartOfDay(),
                    )

                val operation =
                    BondOperationFactory.create(
                        id = 1,
                        bond = bond,
                        operationType = OperationType.BUY,
                        quantity = BigDecimal("100"),
                        unitPrice = BigDecimal("10.00"),
                        walletTransaction = walletTransaction,
                    )

                val manuallyAdjustedCalculation =
                    BondInterestCalculationFactory.create(
                        id = 1,
                        bond = bond,
                        referenceMonth = YearMonth.of(2026, 1),
                        manuallyAdjusted = true,
                        monthlyInterest = BigDecimal("50.00"),
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
                every { bondOperationRepository.findByBondOrderByOperationDateAsc(bond) } returns listOf(operation)
                every { bondInterestCalculationRepository.deleteByBond(bond) } returns Unit
                every {
                    marketIndicatorService.getIndicatorHistoryBetween(
                        InterestIndex.CDI,
                        any(),
                        any(),
                    )
                } returns marketData
                every { bondInterestCalculationRepository.findLastCalculatedMonth(bond) } returns null
                every { bondInterestCalculationRepository.findByBondAndReferenceMonth(any(), any()) } returns null
                every { bondInterestCalculationRepository.save(any()) } returnsArgument 0

                service.recalculateAllMonthlyInterest(1)

                Then("should delete manually adjusted calculations") {
                    verify { bondInterestCalculationRepository.deleteByBond(bond) }
                }

                Then("should recalculate with automatic values") {
                    verify(atLeast = 1) {
                        bondInterestCalculationRepository.save(
                            match { !it.manuallyAdjusted },
                        )
                    }
                }
            }
        }

        Given("a bond with operations spanning multiple months") {
            When("recalculating all monthly interest") {
                val bond =
                    BondFactory.create(
                        id = 1,
                        interestType = InterestType.FIXED,
                        interestIndex = InterestIndex.CDI,
                        interestRate = BigDecimal("10.00"),
                    )

                val walletTransaction1 =
                    WalletTransactionFactory.create(
                        id = 1,
                        date = LocalDate.of(2026, 1, 15).atStartOfDay(),
                    )

                val walletTransaction2 =
                    WalletTransactionFactory.create(
                        id = 2,
                        date = LocalDate.of(2026, 2, 10).atStartOfDay(),
                    )

                val operation1 =
                    BondOperationFactory.create(
                        id = 1,
                        bond = bond,
                        operationType = OperationType.BUY,
                        quantity = BigDecimal("100"),
                        unitPrice = BigDecimal("10.00"),
                        walletTransaction = walletTransaction1,
                    )

                val operation2 =
                    BondOperationFactory.create(
                        id = 2,
                        bond = bond,
                        operationType = OperationType.BUY,
                        quantity = BigDecimal("50"),
                        unitPrice = BigDecimal("10.00"),
                        walletTransaction = walletTransaction2,
                    )

                val marketData =
                    (1..28).map { day ->
                        MarketIndicatorHistoryFactory.create(
                            indicatorType = InterestIndex.CDI,
                            referenceDate = LocalDate.of(2026, 1, day),
                            rateValue = BigDecimal("0.04"),
                        )
                    }

                every { bondRepository.findById(1) } returns Optional.of(bond)
                every { bondOperationRepository.findByBondOrderByOperationDateAsc(bond) } returns
                    listOf(operation1, operation2)
                every { bondInterestCalculationRepository.deleteByBond(bond) } returns Unit
                every {
                    marketIndicatorService.getIndicatorHistoryBetween(
                        InterestIndex.CDI,
                        any(),
                        any(),
                    )
                } returns marketData
                every { bondInterestCalculationRepository.findLastCalculatedMonth(bond) } returns null
                every { bondInterestCalculationRepository.findByBondAndReferenceMonth(any(), any()) } returns null
                every { bondInterestCalculationRepository.save(any()) } returnsArgument 0

                service.recalculateAllMonthlyInterest(1)

                Then("should calculate for all months with operations") {
                    verify(atLeast = 2) { bondInterestCalculationRepository.save(any()) }
                }
            }
        }

        Given("a non-existent bond") {
            When("trying to calculate interest") {
                every { bondRepository.findById(999) } returns Optional.empty()

                Then("should throw EntityNotFoundException") {
                    shouldThrow<EntityNotFoundException> {
                        service.recalculateAllMonthlyInterest(999)
                    }
                }
            }
        }
    })
