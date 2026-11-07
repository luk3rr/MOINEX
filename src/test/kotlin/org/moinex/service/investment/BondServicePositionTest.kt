package org.moinex.service.investment

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import org.moinex.factory.investment.BondFactory
import org.moinex.factory.investment.BondInterestCalculationFactory
import org.moinex.factory.investment.BondOperationFactory
import org.moinex.factory.wallet.WalletTransactionFactory
import org.moinex.model.enums.BondType
import org.moinex.model.enums.OperationType
import org.moinex.model.investment.Bond
import org.moinex.model.investment.BondOperation
import org.moinex.repository.investment.BondOperationRepository
import org.moinex.repository.investment.BondRepository
import org.moinex.service.NotificationService
import org.moinex.service.PreferencesService
import org.moinex.service.wallet.WalletService
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth

class BondServicePositionTest :
    BehaviorSpec({
        val bondOperationRepository = mockk<BondOperationRepository>()
        val interestService = mockk<BondInterestCalculationService>()

        val service =
            BondService(
                mockk<BondRepository>(),
                bondOperationRepository,
                mockk<WalletService>(relaxed = true),
                interestService,
                mockk<NotificationService>(relaxed = true),
                mockk<PreferencesService>(relaxed = true),
            )

        fun operation(
            bond: Bond,
            id: Int,
            type: OperationType,
            date: LocalDate,
            quantity: String,
            price: String = "1.00",
        ): BondOperation =
            BondOperationFactory.create(
                id = id,
                bond = bond,
                operationType = type,
                quantity = BigDecimal(quantity),
                unitPrice = BigDecimal(price),
                walletTransaction =
                    WalletTransactionFactory.create(
                        id = id,
                        date = LocalDateTime.of(date, java.time.LocalTime.NOON),
                    ),
            )

        Given("a bond bought in three lots of 1 unit at R$ 1, R$ 1 and R$ 2") {
            val bond = BondFactory.create(id = 1)
            val operations =
                listOf(
                    operation(bond, 1, OperationType.BUY, LocalDate.of(2026, 1, 5), "1", "1.00"),
                    operation(bond, 2, OperationType.BUY, LocalDate.of(2026, 1, 6), "1", "1.00"),
                    operation(bond, 3, OperationType.BUY, LocalDate.of(2026, 1, 7), "1", "2.00"),
                )
            every { bondOperationRepository.findByBondOrderByOperationDateAsc(bond) } returns operations

            When("asking for the average unit price") {
                val average = service.getAverageUnitPrice(bond)

                Then("it is R$ 1.33 and no exception is thrown") {
                    average.compareTo(BigDecimal("1.33")) shouldBe 0
                }
            }
        }

        Given("a bond bought at R$ 1 and R$ 3 and half of it sold") {
            val bond = BondFactory.create(id = 1)
            val operations =
                listOf(
                    operation(bond, 1, OperationType.BUY, LocalDate.of(2026, 1, 5), "10", "1.00"),
                    operation(bond, 2, OperationType.BUY, LocalDate.of(2026, 1, 6), "10", "3.00"),
                    operation(bond, 3, OperationType.SELL, LocalDate.of(2026, 1, 7), "10", "2.00"),
                )
            every { bondOperationRepository.findByBondOrderByOperationDateAsc(bond) } returns operations

            When("asking for the average price and the invested value of what is still held") {
                Then("they cover only the units held: R$ 2.00 and R$ 20.00") {
                    service.getAverageUnitPrice(bond).compareTo(BigDecimal("2.00")) shouldBe 0
                    service.getTotalInvestedValue(bond).compareTo(BigDecimal("20.00")) shouldBe 0
                }
            }
        }

        Given("a bond with R$ 1.000 of principal and R$ 100 of interest held for 100 days") {
            val bond = BondFactory.create(id = 1, type = BondType.CDB)
            val today = LocalDate.of(2026, 4, 15)
            val operations = listOf(operation(bond, 1, OperationType.BUY, today.minusDays(100), "1000"))
            every { bondOperationRepository.findByBondOrderByOperationDateAsc(bond) } returns operations
            every { interestService.getLatestCalculation(1) } returns
                BondInterestCalculationFactory.create(
                    bond = bond,
                    referenceMonth = YearMonth.of(2026, 4),
                    accumulatedInterest = BigDecimal("100.00"),
                )

            When("redeeming R$ 100 of principal") {
                val estimate = service.estimateRedemption(bond, BigDecimal("100"), BigDecimal("1.00"), today)

                Then("it takes R$ 10 of interest, with R$ 2.25 of estimated tax (22.5%)") {
                    estimate.interestShare.compareTo(BigDecimal("10.00")) shouldBe 0
                    estimate.estimatedTax.compareTo(BigDecimal("2.25")) shouldBe 0
                    estimate.netInterest.compareTo(BigDecimal("7.75")) shouldBe 0
                    estimate.taxApplicable shouldBe true
                }
            }

            When("redeeming a gross amount of R$ 110") {
                val estimate = service.estimateRedemptionByGross(bond, BigDecimal("110"), today)

                Then("it takes R$ 100 of principal and R$ 10 of interest, with R$ 2.25 of estimated tax") {
                    estimate.principal.compareTo(BigDecimal("100.00")) shouldBe 0
                    estimate.interestShare.compareTo(BigDecimal("10.00")) shouldBe 0
                    estimate.estimatedTax.compareTo(BigDecimal("2.25")) shouldBe 0
                }
            }

            When("redeeming more than the whole balance") {
                val estimate = service.estimateRedemptionByGross(bond, BigDecimal("5000"), today)

                Then("it is limited to the balance: R$ 1.000 of principal and R$ 100 of interest") {
                    estimate.principal.compareTo(BigDecimal("1000.00")) shouldBe 0
                    estimate.interestShare.compareTo(BigDecimal("100.00")) shouldBe 0
                }
            }

            When("estimating the net interest of the whole bond") {
                val estimate = service.estimateNetInterest(bond, today)

                Then("the tax is R$ 22.50 and the net interest R$ 77.50") {
                    estimate.estimatedTax.compareTo(BigDecimal("22.50")) shouldBe 0
                    estimate.netInterest.compareTo(BigDecimal("77.50")) shouldBe 0
                }
            }
        }

        Given("bonds of different types and unit prices") {
            val parCdb = BondFactory.create(id = 10, type = BondType.CDB)
            val oldCdb = BondFactory.create(id = 11, type = BondType.CDB)
            val treasury = BondFactory.create(id = 12, type = BondType.TREASURY_POSTFIXED)
            every { bondOperationRepository.findByBondOrderByOperationDateAsc(parCdb) } returns
                listOf(operation(parCdb, 1, OperationType.BUY, LocalDate.of(2026, 1, 5), "1000", "1.00"))
            every { bondOperationRepository.findByBondOrderByOperationDateAsc(oldCdb) } returns
                listOf(operation(oldCdb, 2, OperationType.BUY, LocalDate.of(2026, 1, 5), "2", "1000.00"))
            every { bondOperationRepository.findByBondOrderByOperationDateAsc(treasury) } returns
                listOf(operation(treasury, 3, OperationType.BUY, LocalDate.of(2026, 1, 5), "1", "1.00"))

            When("asking which are held as an amount in reais") {
                Then("only a CDB, LCI or LCA whose units cost one is, and an old CDB with another price is not") {
                    service.isAmountOnly(parCdb) shouldBe true
                    service.isAmountOnly(oldCdb) shouldBe false
                    service.isAmountOnly(treasury) shouldBe false
                }
            }
        }

        Given("a LCI with interest held") {
            val bond = BondFactory.create(id = 2, type = BondType.LCI)
            val today = LocalDate.of(2026, 4, 15)
            every { bondOperationRepository.findByBondOrderByOperationDateAsc(bond) } returns
                listOf(operation(bond, 1, OperationType.BUY, today.minusDays(100), "1000"))
            every { interestService.getLatestCalculation(2) } returns
                BondInterestCalculationFactory.create(bond = bond, accumulatedInterest = BigDecimal("100.00"))

            When("estimating the net interest") {
                val estimate = service.estimateNetInterest(bond, today)

                Then("there is no tax") {
                    estimate.estimatedTax.signum() shouldBe 0
                    estimate.netInterest.compareTo(BigDecimal("100.00")) shouldBe 0
                }
            }
        }
    })
