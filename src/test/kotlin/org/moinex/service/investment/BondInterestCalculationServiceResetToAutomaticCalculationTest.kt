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
import org.moinex.factory.investment.BondOperationFactory
import org.moinex.factory.investment.MarketIndicatorHistoryFactory
import org.moinex.factory.wallet.WalletTransactionFactory
import org.moinex.model.enums.InterestIndex
import org.moinex.model.enums.InterestType
import org.moinex.model.enums.OperationType
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

class BondInterestCalculationServiceResetToAutomaticCalculationTest :
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

        Given("a manually adjusted calculation to reset") {
            When("resetting to automatic calculation") {
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
                        monthlyInterest = BigDecimal("15.00"),
                        accumulatedInterest = BigDecimal("15.00"),
                        manuallyAdjusted = true,
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

                every { bondRepository.findById(1) } returns Optional.of(bond)
                every {
                    bondInterestCalculationRepository.findByBondAndReferenceMonth(bond, month)
                } returns calculation
                every { bondOperationRepository.findByBondOrderByOperationDateAsc(bond) } returns listOf(operation)
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

                service.resetToAutomaticCalculation(1, month)

                Then("should mark as not manually adjusted") {
                    calculation.manuallyAdjusted shouldBe false
                }

                Then("should recalculate interest") {
                    verify { bondOperationRepository.findByBondOrderByOperationDateAsc(bond) }
                }

                Then("should save the calculation") {
                    verify { bondInterestCalculationRepository.save(calculation) }
                }
            }
        }

        Given("a non-manually adjusted calculation") {
            When("trying to reset") {
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

                every { bondRepository.findById(1) } returns Optional.of(bond)
                every {
                    bondInterestCalculationRepository.findByBondAndReferenceMonth(bond, month)
                } returns calculation

                service.resetToAutomaticCalculation(1, month)

                Then("should not recalculate") {
                    verify(exactly = 0) { bondOperationRepository.findByBondOrderByOperationDateAsc(any()) }
                }
            }
        }

        Given("trying to reset non-existent calculation") {
            When("calling resetToAutomaticCalculation") {
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
                        service.resetToAutomaticCalculation(1, month)
                    }
                }
            }
        }
    })
