package org.moinex.service.investment

import io.kotest.core.spec.style.BehaviorSpec
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.moinex.factory.investment.BondFactory
import org.moinex.factory.investment.BondOperationFactory
import org.moinex.factory.investment.MarketIndicatorHistoryFactory
import org.moinex.factory.wallet.WalletTransactionFactory
import org.moinex.model.enums.BondType
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

class BondInterestCalculationServiceCalculateInterestForAllBondsIfNeededTest :
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

        Given("bonds with operations to calculate interest") {
            When("calculating interest for all bonds") {
                val bond1 =
                    BondFactory.create(
                        id = 1,
                        name = "CDB Test",
                        type = BondType.CDB,
                        interestType = InterestType.FIXED,
                        interestIndex = InterestIndex.CDI,
                        interestRate = BigDecimal("10.00"),
                    )

                val bond2 =
                    BondFactory.create(
                        id = 2,
                        name = "LCI Test",
                        type = BondType.LCI,
                        interestType = InterestType.FLOATING,
                        interestIndex = InterestIndex.CDI,
                        interestRate = BigDecimal.ZERO,
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
                        bond = bond1,
                        operationType = OperationType.BUY,
                        quantity = BigDecimal("100"),
                        unitPrice = BigDecimal("10.00"),
                        walletTransaction = walletTransaction1,
                    )

                val operation2 =
                    BondOperationFactory.create(
                        id = 2,
                        bond = bond2,
                        operationType = OperationType.BUY,
                        quantity = BigDecimal("200"),
                        unitPrice = BigDecimal("15.00"),
                        walletTransaction = walletTransaction2,
                    )

                val marketData =
                    listOf(
                        MarketIndicatorHistoryFactory.create(
                            indicatorType = InterestIndex.CDI,
                            referenceDate = LocalDate.of(2026, 1, 15),
                            rateValue = BigDecimal("0.04"),
                        ),
                        MarketIndicatorHistoryFactory.create(
                            indicatorType = InterestIndex.CDI,
                            referenceDate = LocalDate.of(2026, 1, 16),
                            rateValue = BigDecimal("0.04"),
                        ),
                    )

                every {
                    bondOperationRepository.findAllByNonArchivedBondsOrderByBondAndDate()
                } returns listOf(operation1, operation2)

                every { bondOperationRepository.findByBondOrderByOperationDateAsc(bond1) } returns listOf(operation1)
                every { bondOperationRepository.findByBondOrderByOperationDateAsc(bond2) } returns listOf(operation2)

                every {
                    marketIndicatorService.getIndicatorHistoryBetween(
                        InterestIndex.CDI,
                        any(),
                        any(),
                    )
                } returns marketData

                every { bondInterestCalculationRepository.findLastCalculatedMonth(any()) } returns null
                every { bondInterestCalculationRepository.findByBondAndReferenceMonth(any(), any()) } returns null
                every { bondInterestCalculationRepository.save(any()) } returnsArgument 0

                service.calculateInterestForAllBondsIfNeeded()

                Then("should process both bonds") {
                    verify(exactly = 2) { bondOperationRepository.findByBondOrderByOperationDateAsc(any()) }
                }

                Then("should save calculations for both bonds") {
                    verify(atLeast = 2) { bondInterestCalculationRepository.save(any()) }
                }
            }
        }

        Given("a bond that fails during synchronization") {
            When("calculating interest for all bonds") {
                val bond =
                    BondFactory.create(
                        id = 1,
                        name = "Failing Bond",
                        type = BondType.CDB,
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

                every {
                    bondOperationRepository.findAllByNonArchivedBondsOrderByBondAndDate()
                } returns listOf(operation)

                every { bondOperationRepository.findByBondOrderByOperationDateAsc(bond) } returns listOf(operation)
                every {
                    marketIndicatorService.getIndicatorHistoryBetween(
                        InterestIndex.CDI,
                        any(),
                        any(),
                    )
                } throws RuntimeException("Unexpected error")
                every { bondInterestCalculationRepository.findLastCalculatedMonth(any()) } returns null

                service.calculateInterestForAllBondsIfNeeded()

                Then("should continue processing despite error") {
                    verify { bondOperationRepository.findByBondOrderByOperationDateAsc(bond) }
                }
            }
        }

        Given("archived bonds") {
            When("calculating interest for all bonds") {
                val archivedBond =
                    BondFactory.create(
                        id = 1,
                        name = "Archived Bond",
                        archived = true,
                    )

                val activeBond =
                    BondFactory.create(
                        id = 2,
                        name = "Active Bond",
                        archived = false,
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
                        bond = activeBond,
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

                every {
                    bondOperationRepository.findAllByNonArchivedBondsOrderByBondAndDate()
                } returns listOf(operation)

                every { bondOperationRepository.findByBondOrderByOperationDateAsc(activeBond) } returns listOf(operation)
                every {
                    marketIndicatorService.getIndicatorHistoryBetween(
                        InterestIndex.CDI,
                        any(),
                        any(),
                    )
                } returns marketData
                every { bondInterestCalculationRepository.findLastCalculatedMonth(activeBond) } returns null
                every { bondInterestCalculationRepository.findByBondAndReferenceMonth(any(), any()) } returns null
                every { bondInterestCalculationRepository.save(any()) } returnsArgument 0

                service.calculateInterestForAllBondsIfNeeded()

                Then("should only process non-archived bonds") {
                    verify(exactly = 1) { bondOperationRepository.findByBondOrderByOperationDateAsc(any()) }
                }

                Then("should not process archived bonds") {
                    verify(exactly = 0) { bondOperationRepository.findByBondOrderByOperationDateAsc(archivedBond) }
                }
            }
        }
    })
