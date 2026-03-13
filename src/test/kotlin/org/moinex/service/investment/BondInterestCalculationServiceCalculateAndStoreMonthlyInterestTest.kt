package org.moinex.service.investment

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
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
import org.moinex.service.BondInterestCalculationService
import org.moinex.service.MarketIndicatorService
import java.math.BigDecimal
import java.time.LocalDate
import java.time.YearMonth

class BondInterestCalculationServiceCalculateAndStoreMonthlyInterestTest :
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

        Given("a bond without operations") {
            When("calculating interest") {
                val bond =
                    BondFactory.create(
                        id = 1,
                        interestType = InterestType.FIXED,
                        interestIndex = InterestIndex.CDI,
                        interestRate = BigDecimal("10.00"),
                    )

                every { bondOperationRepository.findByBondOrderByOperationDateAsc(bond) } returns emptyList()

                service.calculateAndStoreMonthlyInterest(bond)

                Then("should not save any calculations") {
                    verify(exactly = 0) { bondInterestCalculationRepository.save(any()) }
                }
            }
        }

        Given("a bond without interest configuration") {
            When("calculating interest") {
                val bond =
                    BondFactory.create(
                        id = 1,
                        interestType = null,
                        interestIndex = null,
                        interestRate = null,
                    )

                Then("should throw IllegalArgumentException") {
                    shouldThrow<IllegalArgumentException> {
                        service.calculateAndStoreMonthlyInterest(bond)
                    }
                }
            }
        }

        Given("a bond with fixed interest rate") {
            When("calculating monthly interest") {
                val bond =
                    BondFactory.create(
                        id = 1,
                        interestType = InterestType.FIXED,
                        interestIndex = InterestIndex.CDI,
                        interestRate = BigDecimal("12.00"),
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

                val marketData =
                    (1..20).map { day ->
                        MarketIndicatorHistoryFactory.create(
                            indicatorType = InterestIndex.CDI,
                            referenceDate = LocalDate.of(2026, 1, day),
                            rateValue = BigDecimal("0.04"),
                        )
                    }

                every { bondOperationRepository.findByBondOrderByOperationDateAsc(bond) } returns listOf(operation)
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

                service.calculateAndStoreMonthlyInterest(bond)

                Then("should save calculation with interest greater than zero") {
                    verify {
                        bondInterestCalculationRepository.save(
                            match { it.monthlyInterest > BigDecimal.ZERO },
                        )
                    }
                }
            }
        }

        Given("a bond with floating interest rate") {
            When("calculating monthly interest") {
                val bond =
                    BondFactory.create(
                        id = 1,
                        interestType = InterestType.FLOATING,
                        interestIndex = InterestIndex.CDI,
                        interestRate = BigDecimal("2.00"),
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
                        spread = BigDecimal.ZERO,
                        walletTransaction = walletTransaction,
                    )

                val marketData =
                    (1..20).map { day ->
                        MarketIndicatorHistoryFactory.create(
                            indicatorType = InterestIndex.CDI,
                            referenceDate = LocalDate.of(2026, 1, day),
                            rateValue = BigDecimal("0.04"),
                        )
                    }

                every { bondOperationRepository.findByBondOrderByOperationDateAsc(bond) } returns listOf(operation)
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

                service.calculateAndStoreMonthlyInterest(bond)

                Then("should save calculation with interest greater than zero") {
                    verify {
                        bondInterestCalculationRepository.save(
                            match { it.monthlyInterest > BigDecimal.ZERO },
                        )
                    }
                }
            }
        }

        Given("a bond with multiple operations in same month") {
            When("calculating monthly interest") {
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
                        date = LocalDate.of(2026, 1, 10).atStartOfDay(),
                    )

                val walletTransaction2 =
                    WalletTransactionFactory.create(
                        id = 2,
                        date = LocalDate.of(2026, 1, 20).atStartOfDay(),
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
                    (1..31).map { day ->
                        MarketIndicatorHistoryFactory.create(
                            indicatorType = InterestIndex.CDI,
                            referenceDate = LocalDate.of(2026, 1, day),
                            rateValue = BigDecimal("0.04"),
                        )
                    }

                every { bondOperationRepository.findByBondOrderByOperationDateAsc(bond) } returns
                    listOf(operation1, operation2)
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

                service.calculateAndStoreMonthlyInterest(bond)

                Then("should calculate interest considering both operations") {
                    verify {
                        bondInterestCalculationRepository.save(
                            match {
                                it.quantity.compareTo(BigDecimal("150")) == 0 &&
                                    it.investedAmount.compareTo(BigDecimal("1500.00")) == 0
                            },
                        )
                    }
                }
            }
        }

        Given("a bond with buy and sell operations") {
            When("calculating monthly interest") {
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
                        date = LocalDate.of(2026, 1, 10).atStartOfDay(),
                    )

                val walletTransaction2 =
                    WalletTransactionFactory.create(
                        id = 2,
                        date = LocalDate.of(2026, 1, 20).atStartOfDay(),
                    )

                val buyOperation =
                    BondOperationFactory.create(
                        id = 1,
                        bond = bond,
                        operationType = OperationType.BUY,
                        quantity = BigDecimal("100"),
                        unitPrice = BigDecimal("10.00"),
                        walletTransaction = walletTransaction1,
                    )

                val sellOperation =
                    BondOperationFactory.create(
                        id = 2,
                        bond = bond,
                        operationType = OperationType.SELL,
                        quantity = BigDecimal("30"),
                        unitPrice = BigDecimal("11.00"),
                        walletTransaction = walletTransaction2,
                    )

                val marketData =
                    (1..31).map { day ->
                        MarketIndicatorHistoryFactory.create(
                            indicatorType = InterestIndex.CDI,
                            referenceDate = LocalDate.of(2026, 1, day),
                            rateValue = BigDecimal("0.04"),
                        )
                    }

                every { bondOperationRepository.findByBondOrderByOperationDateAsc(bond) } returns
                    listOf(buyOperation, sellOperation)
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

                service.calculateAndStoreMonthlyInterest(bond)

                Then("should calculate interest with reduced quantity after sale") {
                    verify {
                        bondInterestCalculationRepository.save(
                            match { it.quantity.compareTo(BigDecimal("70")) == 0 },
                        )
                    }
                }
            }
        }

        Given("a bond with existing calculations") {
            When("calculating interest again") {
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

                val existingCalculation =
                    BondInterestCalculationFactory.create(
                        id = 1,
                        bond = bond,
                        referenceMonth = YearMonth.of(2026, 1),
                        monthlyInterest = BigDecimal("5.00"),
                        accumulatedInterest = BigDecimal("5.00"),
                    )

                val marketData =
                    (1..20).map { day ->
                        MarketIndicatorHistoryFactory.create(
                            indicatorType = InterestIndex.CDI,
                            referenceDate = LocalDate.of(2026, 1, day),
                            rateValue = BigDecimal("0.04"),
                        )
                    }

                every { bondOperationRepository.findByBondOrderByOperationDateAsc(bond) } returns listOf(operation)
                every {
                    marketIndicatorService.getIndicatorHistoryBetween(
                        InterestIndex.CDI,
                        any(),
                        any(),
                    )
                } returns marketData
                every { bondInterestCalculationRepository.findLastCalculatedMonth(bond) } returns existingCalculation
                every { bondInterestCalculationRepository.findByBondAndReferenceMonth(any(), any()) } returns null
                every { bondInterestCalculationRepository.save(any()) } returnsArgument 0

                service.calculateAndStoreMonthlyInterest(bond)

                Then("should use existing accumulated interest as base") {
                    verify {
                        bondInterestCalculationRepository.save(
                            match { it.accumulatedInterest > existingCalculation.accumulatedInterest },
                        )
                    }
                }
            }
        }

        Given("a bond without interestIndex for non-ZERO_COUPON type") {
            When("calculating interest") {
                val bond =
                    BondFactory.create(
                        id = 1,
                        interestType = InterestType.FIXED,
                        interestIndex = null,
                        interestRate = BigDecimal("10.00"),
                    )

                Then("should throw IllegalArgumentException") {
                    shouldThrow<IllegalArgumentException> {
                        service.calculateAndStoreMonthlyInterest(bond)
                    }
                }
            }
        }

        Given("a bond with existing calculation to update") {
            When("recalculating with new data") {
                val bond =
                    BondFactory.create(
                        id = 1,
                        interestType = InterestType.FIXED,
                        interestIndex = InterestIndex.CDI,
                        interestRate = BigDecimal("10.00"),
                    )

                val month = YearMonth.of(2026, 1)

                val existingCalculation =
                    BondInterestCalculationFactory.create(
                        id = 1,
                        bond = bond,
                        referenceMonth = month,
                        monthlyInterest = BigDecimal("5.00"),
                        accumulatedInterest = BigDecimal("5.00"),
                        manuallyAdjusted = false,
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

                val marketData =
                    (1..20).map { day ->
                        MarketIndicatorHistoryFactory.create(
                            indicatorType = InterestIndex.CDI,
                            referenceDate = LocalDate.of(2026, 1, day),
                            rateValue = BigDecimal("0.04"),
                        )
                    }

                every { bondOperationRepository.findByBondOrderByOperationDateAsc(bond) } returns listOf(operation)
                every {
                    marketIndicatorService.getIndicatorHistoryBetween(
                        InterestIndex.CDI,
                        any(),
                        any(),
                    )
                } returns marketData
                every { bondInterestCalculationRepository.findLastCalculatedMonth(bond) } returns null
                every {
                    bondInterestCalculationRepository.findByBondAndReferenceMonth(any(), any())
                } returns existingCalculation andThen null
                every { bondInterestCalculationRepository.save(any()) } returnsArgument 0

                service.calculateAndStoreMonthlyInterest(bond)

                Then("should update existing calculation") {
                    verify { bondInterestCalculationRepository.save(existingCalculation) }
                }
            }
        }

        Given("a manually adjusted calculation with incremental update needed") {
            When("calculating with new data available") {
                val bond =
                    BondFactory.create(
                        id = 1,
                        interestType = InterestType.FIXED,
                        interestIndex = InterestIndex.CDI,
                        interestRate = BigDecimal("10.00"),
                    )

                val month = YearMonth.now()
                val yesterday = LocalDate.now().minusDays(1)

                val existingCalculation =
                    BondInterestCalculationFactory.create(
                        id = 1,
                        bond = bond,
                        referenceMonth = month,
                        monthlyInterest = BigDecimal("10.00"),
                        accumulatedInterest = BigDecimal("10.00"),
                        calculatedUntilDate = yesterday.minusDays(5),
                        manuallyAdjusted = true,
                    )

                val walletTransaction =
                    WalletTransactionFactory.create(
                        id = 1,
                        date = month.atDay(1).atStartOfDay(),
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

                val marketData =
                    (1..LocalDate.now().dayOfMonth).map { day ->
                        MarketIndicatorHistoryFactory.create(
                            indicatorType = InterestIndex.CDI,
                            referenceDate = month.atDay(day),
                            rateValue = BigDecimal("0.04"),
                        )
                    }

                every { bondOperationRepository.findByBondOrderByOperationDateAsc(bond) } returns listOf(operation)
                every {
                    marketIndicatorService.getIndicatorHistoryBetween(
                        InterestIndex.CDI,
                        any(),
                        any(),
                    )
                } returns marketData
                every { bondInterestCalculationRepository.findLastCalculatedMonth(bond) } returns null
                every {
                    bondInterestCalculationRepository.findByBondAndReferenceMonth(bond, month)
                } returns existingCalculation
                every { bondInterestCalculationRepository.save(any()) } returnsArgument 0

                service.calculateAndStoreMonthlyInterest(bond)

                Then("should perform incremental update") {
                    verify { bondInterestCalculationRepository.save(existingCalculation) }
                }
            }
        }

        Given("a manually adjusted calculation already up to date") {
            When("trying to calculate again") {
                val bond =
                    BondFactory.create(
                        id = 1,
                        interestType = InterestType.FIXED,
                        interestIndex = InterestIndex.CDI,
                        interestRate = BigDecimal("10.00"),
                    )

                val month = YearMonth.now()
                val today = LocalDate.now()

                val existingCalculation =
                    BondInterestCalculationFactory.create(
                        id = 1,
                        bond = bond,
                        referenceMonth = month,
                        monthlyInterest = BigDecimal("10.00"),
                        accumulatedInterest = BigDecimal("10.00"),
                        calculatedUntilDate = today,
                        manuallyAdjusted = true,
                    )

                val walletTransaction =
                    WalletTransactionFactory.create(
                        id = 1,
                        date = month.atDay(1).atStartOfDay(),
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

                every { bondOperationRepository.findByBondOrderByOperationDateAsc(bond) } returns listOf(operation)
                every { bondInterestCalculationRepository.findLastCalculatedMonth(bond) } returns null
                every {
                    bondInterestCalculationRepository.findByBondAndReferenceMonth(bond, month)
                } returns existingCalculation

                service.calculateAndStoreMonthlyInterest(bond)

                Then("should skip recalculation") {
                    verify(exactly = 0) { bondInterestCalculationRepository.save(any()) }
                }
            }
        }

        Given("a bond with fixed interest and no business days") {
            When("calculating interest") {
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

                every { bondOperationRepository.findByBondOrderByOperationDateAsc(bond) } returns listOf(operation)
                every {
                    marketIndicatorService.getIndicatorHistoryBetween(
                        InterestIndex.CDI,
                        any(),
                        any(),
                    )
                } returns emptyList()
                every { bondInterestCalculationRepository.findLastCalculatedMonth(bond) } returns null
                every { bondInterestCalculationRepository.findByBondAndReferenceMonth(any(), any()) } returns null
                every { bondInterestCalculationRepository.save(any()) } returnsArgument 0

                service.calculateAndStoreMonthlyInterest(bond)

                Then("should save calculation with zero interest") {
                    verify {
                        bondInterestCalculationRepository.save(
                            match { it.monthlyInterest.compareTo(BigDecimal.ZERO) == 0 },
                        )
                    }
                }
            }
        }

        Given("a bond with floating interest and empty market data") {
            When("calculating interest") {
                val bond =
                    BondFactory.create(
                        id = 1,
                        interestType = InterestType.FLOATING,
                        interestIndex = InterestIndex.CDI,
                        interestRate = BigDecimal("2.00"),
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
                        spread = BigDecimal.ZERO,
                        walletTransaction = walletTransaction,
                    )

                every { bondOperationRepository.findByBondOrderByOperationDateAsc(bond) } returns listOf(operation)
                every {
                    marketIndicatorService.getIndicatorHistoryBetween(
                        InterestIndex.CDI,
                        any(),
                        any(),
                    )
                } returns emptyList()
                every { bondInterestCalculationRepository.findLastCalculatedMonth(bond) } returns null
                every { bondInterestCalculationRepository.findByBondAndReferenceMonth(any(), any()) } returns null
                every { bondInterestCalculationRepository.save(any()) } returnsArgument 0

                service.calculateAndStoreMonthlyInterest(bond)

                Then("should save calculation with zero interest when no market data") {
                    verify {
                        bondInterestCalculationRepository.save(
                            match { it.monthlyInterest.compareTo(BigDecimal.ZERO) == 0 },
                        )
                    }
                }
            }
        }

        Given("a bond with multiple operations in different dates for period calculation") {
            When("calculating interest with operations spread across month") {
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
                        date = LocalDate.of(2026, 1, 5).atStartOfDay(),
                    )

                val walletTransaction2 =
                    WalletTransactionFactory.create(
                        id = 2,
                        date = LocalDate.of(2026, 1, 15).atStartOfDay(),
                    )

                val walletTransaction3 =
                    WalletTransactionFactory.create(
                        id = 3,
                        date = LocalDate.of(2026, 1, 25).atStartOfDay(),
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
                        unitPrice = BigDecimal("10.50"),
                        walletTransaction = walletTransaction2,
                    )

                val operation3 =
                    BondOperationFactory.create(
                        id = 3,
                        bond = bond,
                        operationType = OperationType.SELL,
                        quantity = BigDecimal("30"),
                        unitPrice = BigDecimal("11.00"),
                        walletTransaction = walletTransaction3,
                    )

                val marketData =
                    (1..31).map { day ->
                        MarketIndicatorHistoryFactory.create(
                            indicatorType = InterestIndex.CDI,
                            referenceDate = LocalDate.of(2026, 1, day),
                            rateValue = BigDecimal("0.04"),
                        )
                    }

                every { bondOperationRepository.findByBondOrderByOperationDateAsc(bond) } returns
                    listOf(operation1, operation2, operation3)
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

                service.calculateAndStoreMonthlyInterest(bond)

                Then("should calculate interest considering all operations") {
                    verify {
                        bondInterestCalculationRepository.save(
                            match { it.quantity.compareTo(BigDecimal("120")) == 0 },
                        )
                    }
                }
            }
        }

        Given("a manually adjusted calculation with incremental update and operations") {
            When("updating with new operations in period") {
                val bond =
                    BondFactory.create(
                        id = 1,
                        interestType = InterestType.FLOATING,
                        interestIndex = InterestIndex.CDI,
                        interestRate = BigDecimal("2.00"),
                    )

                val month = YearMonth.now()
                val calculatedUntil = LocalDate.now().minusDays(10)

                val existingCalculation =
                    BondInterestCalculationFactory.create(
                        id = 1,
                        bond = bond,
                        referenceMonth = month,
                        monthlyInterest = BigDecimal("10.00"),
                        accumulatedInterest = BigDecimal("10.00"),
                        calculatedUntilDate = calculatedUntil,
                        manuallyAdjusted = true,
                    )

                val walletTransaction1 =
                    WalletTransactionFactory.create(
                        id = 1,
                        date = month.atDay(1).atStartOfDay(),
                    )

                val walletTransaction2 =
                    WalletTransactionFactory.create(
                        id = 2,
                        date = calculatedUntil.plusDays(2).atStartOfDay(),
                    )

                val operation1 =
                    BondOperationFactory.create(
                        id = 1,
                        bond = bond,
                        operationType = OperationType.BUY,
                        quantity = BigDecimal("100"),
                        unitPrice = BigDecimal("10.00"),
                        spread = BigDecimal("1.5"),
                        walletTransaction = walletTransaction1,
                    )

                val operation2 =
                    BondOperationFactory.create(
                        id = 2,
                        bond = bond,
                        operationType = OperationType.BUY,
                        quantity = BigDecimal("50"),
                        unitPrice = BigDecimal("10.50"),
                        spread = BigDecimal("1.5"),
                        walletTransaction = walletTransaction2,
                    )

                val marketData =
                    (1..LocalDate.now().dayOfMonth).map { day ->
                        MarketIndicatorHistoryFactory.create(
                            indicatorType = InterestIndex.CDI,
                            referenceDate = month.atDay(day),
                            rateValue = BigDecimal("0.04"),
                        )
                    }

                every { bondOperationRepository.findByBondOrderByOperationDateAsc(bond) } returns
                    listOf(operation1, operation2)
                every {
                    marketIndicatorService.getIndicatorHistoryBetween(
                        InterestIndex.CDI,
                        any(),
                        any(),
                    )
                } returns marketData
                every { bondInterestCalculationRepository.findLastCalculatedMonth(bond) } returns null
                every {
                    bondInterestCalculationRepository.findByBondAndReferenceMonth(bond, month)
                } returns existingCalculation
                every { bondInterestCalculationRepository.save(any()) } returnsArgument 0

                service.calculateAndStoreMonthlyInterest(bond)

                Then("should update calculation with new operation") {
                    verify { bondInterestCalculationRepository.save(existingCalculation) }
                }
            }
        }

        Given("a manually adjusted calculation with no new data") {
            When("trying incremental update") {
                val bond =
                    BondFactory.create(
                        id = 1,
                        interestType = InterestType.FIXED,
                        interestIndex = InterestIndex.CDI,
                        interestRate = BigDecimal("10.00"),
                    )

                val month = YearMonth.now()
                val calculatedUntil = LocalDate.now().minusDays(1)

                val existingCalculation =
                    BondInterestCalculationFactory.create(
                        id = 1,
                        bond = bond,
                        referenceMonth = month,
                        monthlyInterest = BigDecimal("10.00"),
                        accumulatedInterest = BigDecimal("10.00"),
                        calculatedUntilDate = calculatedUntil,
                        manuallyAdjusted = true,
                    )

                val walletTransaction =
                    WalletTransactionFactory.create(
                        id = 1,
                        date = month.atDay(1).atStartOfDay(),
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

                val marketData =
                    (1..calculatedUntil.dayOfMonth).map { day ->
                        MarketIndicatorHistoryFactory.create(
                            indicatorType = InterestIndex.CDI,
                            referenceDate = month.atDay(day),
                            rateValue = BigDecimal("0.04"),
                        )
                    }

                every { bondOperationRepository.findByBondOrderByOperationDateAsc(bond) } returns listOf(operation)
                every {
                    marketIndicatorService.getIndicatorHistoryBetween(
                        InterestIndex.CDI,
                        any(),
                        any(),
                    )
                } returns marketData
                every { bondInterestCalculationRepository.findLastCalculatedMonth(bond) } returns null
                every {
                    bondInterestCalculationRepository.findByBondAndReferenceMonth(bond, month)
                } returns existingCalculation

                service.calculateAndStoreMonthlyInterest(bond)

                Then("should skip update when no new data") {
                    verify(exactly = 0) { bondInterestCalculationRepository.save(any()) }
                }
            }
        }

        Given("a manually adjusted calculation with no market data in period") {
            When("trying incremental update") {
                val bond =
                    BondFactory.create(
                        id = 1,
                        interestType = InterestType.FLOATING,
                        interestIndex = InterestIndex.CDI,
                        interestRate = BigDecimal("2.00"),
                    )

                val month = YearMonth.now()
                val calculatedUntil = LocalDate.now().minusDays(5)

                val existingCalculation =
                    BondInterestCalculationFactory.create(
                        id = 1,
                        bond = bond,
                        referenceMonth = month,
                        monthlyInterest = BigDecimal("10.00"),
                        accumulatedInterest = BigDecimal("10.00"),
                        calculatedUntilDate = calculatedUntil,
                        manuallyAdjusted = true,
                    )

                val walletTransaction =
                    WalletTransactionFactory.create(
                        id = 1,
                        date = month.atDay(1).atStartOfDay(),
                    )

                val operation =
                    BondOperationFactory.create(
                        id = 1,
                        bond = bond,
                        operationType = OperationType.BUY,
                        quantity = BigDecimal("100"),
                        unitPrice = BigDecimal("10.00"),
                        spread = BigDecimal("1.5"),
                        walletTransaction = walletTransaction,
                    )

                every { bondOperationRepository.findByBondOrderByOperationDateAsc(bond) } returns listOf(operation)
                every {
                    marketIndicatorService.getIndicatorHistoryBetween(
                        InterestIndex.CDI,
                        any(),
                        any(),
                    )
                } returns emptyList()
                every { bondInterestCalculationRepository.findLastCalculatedMonth(bond) } returns null
                every {
                    bondInterestCalculationRepository.findByBondAndReferenceMonth(bond, month)
                } returns existingCalculation

                service.calculateAndStoreMonthlyInterest(bond)

                Then("should skip update when no market data available") {
                    verify(exactly = 0) { bondInterestCalculationRepository.save(any()) }
                }
            }
        }

        Given("a bond with SELL operation to test else branch") {
            When("calculating with mixed BUY and SELL operations") {
                val bond =
                    BondFactory.create(
                        id = 1,
                        interestType = InterestType.FIXED,
                        interestIndex = InterestIndex.CDI,
                        interestRate = BigDecimal("10.00"),
                    )

                val month = YearMonth.now()
                val calculatedUntil = LocalDate.now().minusDays(10)

                val existingCalculation =
                    BondInterestCalculationFactory.create(
                        id = 1,
                        bond = bond,
                        referenceMonth = month,
                        monthlyInterest = BigDecimal("5.00"),
                        accumulatedInterest = BigDecimal("5.00"),
                        calculatedUntilDate = calculatedUntil,
                        manuallyAdjusted = true,
                    )

                val walletTransaction1 =
                    WalletTransactionFactory.create(
                        id = 1,
                        date = month.atDay(1).atStartOfDay(),
                    )

                val walletTransaction2 =
                    WalletTransactionFactory.create(
                        id = 2,
                        date = calculatedUntil.plusDays(2).atStartOfDay(),
                    )

                val buyOperation =
                    BondOperationFactory.create(
                        id = 1,
                        bond = bond,
                        operationType = OperationType.BUY,
                        quantity = BigDecimal("100"),
                        unitPrice = BigDecimal("10.00"),
                        walletTransaction = walletTransaction1,
                    )

                val sellOperation =
                    BondOperationFactory.create(
                        id = 2,
                        bond = bond,
                        operationType = OperationType.SELL,
                        quantity = BigDecimal("30"),
                        unitPrice = BigDecimal("11.00"),
                        walletTransaction = walletTransaction2,
                    )

                val marketData =
                    (1..LocalDate.now().dayOfMonth).map { day ->
                        MarketIndicatorHistoryFactory.create(
                            indicatorType = InterestIndex.CDI,
                            referenceDate = month.atDay(day),
                            rateValue = BigDecimal("0.04"),
                        )
                    }

                every { bondOperationRepository.findByBondOrderByOperationDateAsc(bond) } returns
                    listOf(buyOperation, sellOperation)
                every {
                    marketIndicatorService.getIndicatorHistoryBetween(
                        InterestIndex.CDI,
                        any(),
                        any(),
                    )
                } returns marketData
                every { bondInterestCalculationRepository.findLastCalculatedMonth(bond) } returns null
                every {
                    bondInterestCalculationRepository.findByBondAndReferenceMonth(bond, month)
                } returns existingCalculation
                every { bondInterestCalculationRepository.save(any()) } returnsArgument 0

                service.calculateAndStoreMonthlyInterest(bond)

                Then("should process SELL operation correctly") {
                    verify { bondInterestCalculationRepository.save(existingCalculation) }
                }
            }
        }

        Given("a bond without interestIndex for period calculation") {
            When("calculating incrementally") {
                val bond =
                    BondFactory.create(
                        id = 1,
                        interestType = InterestType.FLOATING,
                        interestIndex = null,
                        interestRate = BigDecimal("2.00"),
                    )

                val month = YearMonth.now()
                val calculatedUntil = LocalDate.now().minusDays(5)

                val existingCalculation =
                    BondInterestCalculationFactory.create(
                        id = 1,
                        bond = bond,
                        referenceMonth = month,
                        monthlyInterest = BigDecimal("10.00"),
                        accumulatedInterest = BigDecimal("10.00"),
                        calculatedUntilDate = calculatedUntil,
                        manuallyAdjusted = true,
                    )

                val walletTransaction =
                    WalletTransactionFactory.create(
                        id = 1,
                        date = month.atDay(1).atStartOfDay(),
                    )

                val operation =
                    BondOperationFactory.create(
                        id = 1,
                        bond = bond,
                        operationType = OperationType.BUY,
                        quantity = BigDecimal("100"),
                        unitPrice = BigDecimal("10.00"),
                        spread = BigDecimal("1.5"),
                        walletTransaction = walletTransaction,
                    )

                every { bondOperationRepository.findByBondOrderByOperationDateAsc(bond) } returns listOf(operation)
                every { bondInterestCalculationRepository.findLastCalculatedMonth(bond) } returns null
                every {
                    bondInterestCalculationRepository.findByBondAndReferenceMonth(bond, month)
                } returns existingCalculation

                Then("should throw IllegalArgumentException") {
                    shouldThrow<IllegalArgumentException> {
                        service.calculateAndStoreMonthlyInterest(bond)
                    }
                }
            }
        }

        Given("a bond with high precision values") {
            When("calculating interest") {
                val bond =
                    BondFactory.create(
                        id = 1,
                        interestType = InterestType.FIXED,
                        interestIndex = InterestIndex.CDI,
                        interestRate = BigDecimal("10.123456"),
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
                        quantity = BigDecimal("100.123456"),
                        unitPrice = BigDecimal("10.123456"),
                        walletTransaction = walletTransaction,
                    )

                val marketData =
                    (1..20).map { day ->
                        MarketIndicatorHistoryFactory.create(
                            indicatorType = InterestIndex.CDI,
                            referenceDate = LocalDate.of(2026, 1, day),
                            rateValue = BigDecimal("0.041234"),
                        )
                    }

                every { bondOperationRepository.findByBondOrderByOperationDateAsc(bond) } returns listOf(operation)
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

                service.calculateAndStoreMonthlyInterest(bond)

                Then("should handle high precision values") {
                    verify { bondInterestCalculationRepository.save(any()) }
                }
            }
        }

        Given("a bond with zero coupon interest type") {
            When("calculating interest") {
                val bond =
                    BondFactory.create(
                        id = 1,
                        interestType = InterestType.ZERO_COUPON,
                        interestIndex = InterestIndex.CDI,
                        interestRate = BigDecimal.ZERO,
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

                val marketData =
                    (1..20).map { day ->
                        MarketIndicatorHistoryFactory.create(
                            indicatorType = InterestIndex.CDI,
                            referenceDate = LocalDate.of(2026, 1, day),
                            rateValue = BigDecimal("0.04"),
                        )
                    }

                every { bondOperationRepository.findByBondOrderByOperationDateAsc(bond) } returns listOf(operation)
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

                service.calculateAndStoreMonthlyInterest(bond)

                Then("should calculate with zero interest") {
                    verify {
                        bondInterestCalculationRepository.save(
                            match { it.monthlyInterest.compareTo(BigDecimal.ZERO) == 0 },
                        )
                    }
                }
            }
        }

        Given("a bond with operations on month boundaries") {
            When("calculating interest") {
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
                        date = LocalDate.of(2026, 1, 1).atStartOfDay(),
                    )

                val walletTransaction2 =
                    WalletTransactionFactory.create(
                        id = 2,
                        date = LocalDate.of(2026, 1, 31).atStartOfDay(),
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
                    (1..31).map { day ->
                        MarketIndicatorHistoryFactory.create(
                            indicatorType = InterestIndex.CDI,
                            referenceDate = LocalDate.of(2026, 1, day),
                            rateValue = BigDecimal("0.04"),
                        )
                    }

                every { bondOperationRepository.findByBondOrderByOperationDateAsc(bond) } returns
                    listOf(operation1, operation2)
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

                service.calculateAndStoreMonthlyInterest(bond)

                Then("should handle boundary dates correctly") {
                    verify {
                        bondInterestCalculationRepository.save(
                            match { it.quantity.compareTo(BigDecimal("150")) == 0 },
                        )
                    }
                }
            }
        }

        Given("a bond with zero interest rate") {
            When("calculating interest") {
                val bond =
                    BondFactory.create(
                        id = 1,
                        interestType = InterestType.FIXED,
                        interestIndex = InterestIndex.CDI,
                        interestRate = BigDecimal.ZERO,
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

                val marketData =
                    (1..20).map { day ->
                        MarketIndicatorHistoryFactory.create(
                            indicatorType = InterestIndex.CDI,
                            referenceDate = LocalDate.of(2026, 1, day),
                            rateValue = BigDecimal("0.04"),
                        )
                    }

                every { bondOperationRepository.findByBondOrderByOperationDateAsc(bond) } returns listOf(operation)
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

                service.calculateAndStoreMonthlyInterest(bond)

                Then("should calculate with zero rate") {
                    verify { bondInterestCalculationRepository.save(any()) }
                }
            }
        }

        Given("a bond with no market data available") {
            When("calculating floating interest") {
                val bond =
                    BondFactory.create(
                        id = 1,
                        interestType = InterestType.FLOATING,
                        interestIndex = InterestIndex.CDI,
                        interestRate = BigDecimal("2.00"),
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
                        spread = BigDecimal.ZERO,
                        walletTransaction = walletTransaction,
                    )

                every { bondOperationRepository.findByBondOrderByOperationDateAsc(bond) } returns listOf(operation)
                every {
                    marketIndicatorService.getIndicatorHistoryBetween(
                        InterestIndex.CDI,
                        any(),
                        any(),
                    )
                } returns emptyList()
                every { bondInterestCalculationRepository.findLastCalculatedMonth(bond) } returns null
                every { bondInterestCalculationRepository.findByBondAndReferenceMonth(any(), any()) } returns null
                every { bondInterestCalculationRepository.save(any()) } returnsArgument 0

                service.calculateAndStoreMonthlyInterest(bond)

                Then("should save calculation with zero interest") {
                    verify {
                        bondInterestCalculationRepository.save(
                            match { it.monthlyInterest.compareTo(BigDecimal.ZERO) == 0 },
                        )
                    }
                }
            }
        }

        Given("a bond with very small quantity") {
            When("calculating interest") {
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
                        quantity = BigDecimal("0.01"),
                        unitPrice = BigDecimal("10.00"),
                        walletTransaction = walletTransaction,
                    )

                val marketData =
                    (1..20).map { day ->
                        MarketIndicatorHistoryFactory.create(
                            indicatorType = InterestIndex.CDI,
                            referenceDate = LocalDate.of(2026, 1, day),
                            rateValue = BigDecimal("0.04"),
                        )
                    }

                every { bondOperationRepository.findByBondOrderByOperationDateAsc(bond) } returns listOf(operation)
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

                service.calculateAndStoreMonthlyInterest(bond)

                Then("should handle small quantities correctly") {
                    verify {
                        bondInterestCalculationRepository.save(
                            match { it.quantity == BigDecimal("0.01") },
                        )
                    }
                }
            }
        }

        Given("a bond sold completely in same month") {
            When("calculating interest") {
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
                        date = LocalDate.of(2026, 1, 10).atStartOfDay(),
                    )

                val walletTransaction2 =
                    WalletTransactionFactory.create(
                        id = 2,
                        date = LocalDate.of(2026, 1, 20).atStartOfDay(),
                    )

                val buyOperation =
                    BondOperationFactory.create(
                        id = 1,
                        bond = bond,
                        operationType = OperationType.BUY,
                        quantity = BigDecimal("100"),
                        unitPrice = BigDecimal("10.00"),
                        walletTransaction = walletTransaction1,
                    )

                val sellOperation =
                    BondOperationFactory.create(
                        id = 2,
                        bond = bond,
                        operationType = OperationType.SELL,
                        quantity = BigDecimal("100"),
                        unitPrice = BigDecimal("11.00"),
                        walletTransaction = walletTransaction2,
                    )

                val marketData =
                    (1..31).map { day ->
                        MarketIndicatorHistoryFactory.create(
                            indicatorType = InterestIndex.CDI,
                            referenceDate = LocalDate.of(2026, 1, day),
                            rateValue = BigDecimal("0.04"),
                        )
                    }

                every { bondOperationRepository.findByBondOrderByOperationDateAsc(bond) } returns
                    listOf(buyOperation, sellOperation)
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

                service.calculateAndStoreMonthlyInterest(bond)

                Then("should calculate interest only for period held") {
                    verify {
                        bondInterestCalculationRepository.save(
                            match { it.quantity.compareTo(BigDecimal.ZERO) == 0 },
                        )
                    }
                }
            }
        }
    })
