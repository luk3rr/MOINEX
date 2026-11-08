package org.moinex.service.investment

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import org.moinex.factory.investment.BondFactory
import org.moinex.factory.investment.BondInterestCalculationFactory
import org.moinex.factory.investment.BondOperationFactory
import org.moinex.factory.wallet.WalletTransactionFactory
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

class BondServiceYieldTest :
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

        fun purchase(
            bond: Bond,
            amount: String,
        ): BondOperation =
            BondOperationFactory.create(
                id = 1,
                bond = bond,
                operationType = OperationType.BUY,
                quantity = BigDecimal(amount),
                unitPrice = BigDecimal("1.00"),
                walletTransaction =
                    WalletTransactionFactory.create(
                        id = 1,
                        date = LocalDateTime.of(LocalDate.of(2026, 1, 5), java.time.LocalTime.NOON),
                    ),
            )

        fun bondWith(
            id: Int,
            invested: String?,
            accumulated: String,
        ): Bond {
            val bond = BondFactory.create(id = id)
            every { bondOperationRepository.findByBondOrderByOperationDateAsc(bond) } returns
                listOfNotNull(invested?.let { purchase(bond, it) })
            every { interestService.getLatestCalculation(id) } returns
                BondInterestCalculationFactory.create(
                    bond = bond,
                    referenceMonth = YearMonth.of(2026, 10),
                    accumulatedInterest = BigDecimal(accumulated),
                )
            return bond
        }

        Given("R$ 150,20 of accumulated interest on R$ 4.000,00 invested") {
            val bond = bondWith(1, "4000", "150.20")

            When("asking for the yield percentage") {
                Then("it is 3.76%") {
                    service.yieldPercentage(bond)!!.compareTo(BigDecimal("3.76")) shouldBe 0
                }
            }
        }

        Given("a loss of R$ 10,00 on R$ 1.000,00 invested") {
            val bond = bondWith(2, "1000", "-10.00")

            When("asking for the yield percentage") {
                Then("it is -1.00%") {
                    service.yieldPercentage(bond)!!.compareTo(BigDecimal("-1.00")) shouldBe 0
                }
            }
        }

        Given("a bond with nothing invested") {
            val bond = bondWith(3, null, "0.00")

            When("asking for the yield percentage") {
                Then("there is no percentage") {
                    service.yieldPercentage(bond) shouldBe null
                }
            }
        }
    })
