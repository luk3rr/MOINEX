package org.moinex.service.investment

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.collections.shouldHaveSize
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
import org.moinex.model.investment.BondInterestCalculation
import org.moinex.model.investment.BondOperation
import org.moinex.repository.investment.BondOperationRepository
import org.moinex.repository.investment.BondRepository
import org.moinex.service.NotificationService
import org.moinex.service.PreferencesService
import org.moinex.service.wallet.WalletService
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.YearMonth

class BondServiceDetailTest :
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

        val today = LocalDate.of(2026, 4, 15)

        fun purchase(
            bond: Bond,
            date: LocalDate,
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
                        date = LocalDateTime.of(date, LocalTime.NOON),
                    ),
            )

        fun row(
            bond: Bond,
            month: YearMonth,
            accumulated: String,
            final: String,
        ): BondInterestCalculation =
            BondInterestCalculationFactory.create(
                bond = bond,
                referenceMonth = month,
                accumulatedInterest = BigDecimal(accumulated),
                finalValue = BigDecimal(final),
            )

        Given(
            "a CDB with R$ 4.000,00 invested for 100 days, R$ 100,00 of interest held and R$ 21,30 earned this month",
        ) {
            val bond = BondFactory.create(id = 1, type = BondType.CDB)
            val months = (0 until 14).map { YearMonth.of(2025, 3).plusMonths(it.toLong()) }
            every { bondOperationRepository.findByBondOrderByOperationDateAsc(bond) } returns
                listOf(purchase(bond, today.minusDays(100), "4000"))
            every { interestService.getLatestCalculation(1) } returns row(bond, months.last(), "100.00", "4100.00")
            every { interestService.getCurrentMonthInterest(1) } returns BigDecimal("21.30")
            every { interestService.getMonthlyInterestHistory(bond) } returns
                months.mapIndexed { index, month -> row(bond, month, "100.00", (4000 + index).toString()) }

            When("asking for the detail") {
                val detail = service.detail(bond, today)

                Then("the position is the invested plus the interest held") {
                    detail.invested.compareTo(BigDecimal("4000.00")) shouldBe 0
                    detail.accumulatedInterest.compareTo(BigDecimal("100.00")) shouldBe 0
                    detail.currentValue.compareTo(BigDecimal("4100.00")) shouldBe 0
                    detail.monthInterest.compareTo(BigDecimal("21.30")) shouldBe 0
                }

                Then("the estimated net interest has the regressive tax: R$ 77.50") {
                    detail.netInterest.estimatedTax.compareTo(BigDecimal("22.50")) shouldBe 0
                    detail.netInterest.netInterest.compareTo(BigDecimal("77.50")) shouldBe 0
                }

                Then("the balance keeps every month in order") {
                    detail.monthlyBalance shouldHaveSize months.size
                    detail.monthlyBalance.first().month shouldBe months.first()
                    detail.monthlyBalance.last().month shouldBe months.last()
                    detail.monthlyBalance
                        .last()
                        .value
                        .compareTo(BigDecimal("4013")) shouldBe 0
                }

                Then("the accumulated interest of the detail is the one the table shows") {
                    detail.accumulatedInterest.compareTo(service.getTotalAccumulatedInterestByBondId(1)) shouldBe 0
                    detail.invested.compareTo(service.getTotalInvestedValue(bond)) shouldBe 0
                }
            }
        }

        Given("a bond with no operations and no interest") {
            val bond = BondFactory.create(id = 2, type = BondType.CDB)
            every { bondOperationRepository.findByBondOrderByOperationDateAsc(bond) } returns emptyList()
            every { interestService.getLatestCalculation(2) } returns null
            every { interestService.getCurrentMonthInterest(2) } returns null
            every { interestService.getMonthlyInterestHistory(bond) } returns emptyList()

            When("asking for the detail") {
                val detail = service.detail(bond, today)

                Then("everything is zero and the balance is empty") {
                    detail.invested.signum() shouldBe 0
                    detail.currentValue.signum() shouldBe 0
                    detail.monthInterest.signum() shouldBe 0
                    detail.monthlyBalance shouldHaveSize 0
                }
            }
        }

        Given("a LCI with interest held") {
            val bond = BondFactory.create(id = 3, type = BondType.LCI)
            every { bondOperationRepository.findByBondOrderByOperationDateAsc(bond) } returns
                listOf(purchase(bond, today.minusDays(100), "1000"))
            every { interestService.getLatestCalculation(3) } returns
                row(bond, YearMonth.of(2026, 4), "50.00", "1050.00")
            every { interestService.getCurrentMonthInterest(3) } returns BigDecimal("5.00")
            every { interestService.getMonthlyInterestHistory(bond) } returns emptyList()

            When("asking for the detail") {
                val detail = service.detail(bond, today)

                Then("there is no tax and the net equals the gross") {
                    detail.netInterest.estimatedTax.signum() shouldBe 0
                    detail.netInterest.netInterest.compareTo(BigDecimal("50.00")) shouldBe 0
                }
            }
        }
    })
