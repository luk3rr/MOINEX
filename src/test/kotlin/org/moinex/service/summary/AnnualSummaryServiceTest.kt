/*
 * Filename: AnnualSummaryServiceTest.kt
 * Created on: September 5, 2026
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 */

package org.moinex.service.summary

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import org.moinex.factory.CategoryFactory
import org.moinex.factory.creditcard.CreditCardDebtFactory
import org.moinex.factory.creditcard.CreditCardPaymentFactory
import org.moinex.factory.wallet.WalletFactory
import org.moinex.factory.wallet.WalletTransactionFactory
import org.moinex.model.enums.SpendAccountingMode
import org.moinex.model.enums.WalletTransactionStatus
import org.moinex.model.enums.WalletTransactionType
import org.moinex.service.PreferencesService
import org.moinex.service.creditcard.CreditCardService
import org.moinex.service.wallet.WalletService
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth

class AnnualSummaryServiceTest :
    BehaviorSpec({
        val walletService = mockk<WalletService>()
        val creditCardService = mockk<CreditCardService>()
        val insightGenerator = mockk<FinancialInsightGenerator>(relaxed = true)
        val preferencesService = mockk<PreferencesService>()

        val service = AnnualSummaryService(walletService, creditCardService, insightGenerator, preferencesService)

        beforeContainer { every { preferencesService.savingsRateTarget } returns BigDecimal("20") }
        afterContainer { clearAllMocks(answers = true) }

        val salary = CategoryFactory.create(id = 1, name = "Salário")
        val food = CategoryFactory.create(id = 2, name = "Alimentação")
        val electronics = CategoryFactory.create(id = 3, name = "Eletrônicos")
        val wallet = WalletFactory.create()

        fun income(
            date: LocalDateTime,
            amount: String,
        ) = WalletTransactionFactory.create(
            date = date,
            status = WalletTransactionStatus.CONFIRMED,
            wallet = wallet,
            category = salary,
            type = WalletTransactionType.INCOME,
            amount = BigDecimal(amount),
        )

        fun expense(
            date: LocalDateTime,
            amount: String,
            category: org.moinex.model.Category = food,
        ) = WalletTransactionFactory.create(
            date = date,
            status = WalletTransactionStatus.CONFIRMED,
            wallet = wallet,
            category = category,
            type = WalletTransactionType.EXPENSE,
            amount = BigDecimal(amount),
        )

        Given("wallet transactions and credit card debts over two months in ACCRUAL mode") {
            every {
                walletService.getAllNonArchivedConfirmedWalletTransactionsByMonthForAnalysis(YearMonth.of(2026, 1))
            } returns
                listOf(
                    income(LocalDateTime.of(2026, 1, 5, 10, 0), "1000.00"),
                    expense(LocalDateTime.of(2026, 1, 6, 10, 0), "200.00"),
                )
            every {
                walletService.getAllNonArchivedConfirmedWalletTransactionsByMonthForAnalysis(YearMonth.of(2026, 2))
            } returns
                listOf(
                    income(LocalDateTime.of(2026, 2, 5, 10, 0), "1000.00"),
                    expense(LocalDateTime.of(2026, 2, 6, 10, 0), "100.00"),
                )
            every { creditCardService.getDebtsBetweenForAnalysis(any(), any()) } returns
                listOf(
                    CreditCardDebtFactory.create(
                        category = electronics,
                        date = LocalDateTime.of(2026, 1, 10, 0, 0),
                        amount = BigDecimal("300.00"),
                    ),
                    CreditCardDebtFactory.create(
                        category = electronics,
                        date = LocalDateTime.of(2026, 2, 12, 0, 0),
                        amount = BigDecimal("150.00"),
                    ),
                )

            When("building the summary") {
                val summary =
                    service.buildSummary(
                        LocalDate.of(2026, 1, 1),
                        LocalDate.of(2026, 2, 28),
                        SpendAccountingMode.ACCRUAL,
                    )

                Then("income, expense and net totals combine wallet and credit card") {
                    summary.totalIncome shouldBe BigDecimal("2000.00")
                    summary.totalExpense shouldBe BigDecimal("750.00")
                    summary.netBalance shouldBe BigDecimal("1250.00")
                }

                Then("savings rate is net over income as a percentage") {
                    summary.savingsRatePercentage shouldBe BigDecimal("62.50")
                }

                Then("expense-by-category is ranked with percentage and monthly average") {
                    val top = summary.expenseByCategory.first()
                    top.categoryName shouldBe "Eletrônicos"
                    top.total shouldBe BigDecimal("450.00")
                    top.percentage shouldBe BigDecimal("60.00")
                    top.monthlyAverage shouldBe BigDecimal("225.00")
                }

                Then("monthly flow is computed per month") {
                    summary.monthlyFlows.size shouldBe 2
                    summary.monthlyFlows[0].expense shouldBe BigDecimal("500.00")
                    summary.monthlyFlows[1].net shouldBe BigDecimal("750.00")
                }

                Then("monthly savings rate is net over income for that month") {
                    summary.monthlyFlows[0].savingsRatePercentage shouldBe BigDecimal("50.00")
                    summary.monthlyFlows[1].savingsRatePercentage shouldBe BigDecimal("75.00")
                }

                Then("top expenses are ordered by amount descending") {
                    summary.topExpenses.first().amount shouldBe BigDecimal("300.00")
                }
            }
        }

        Given("credit card payments in CASH_FLOW mode with a refunded payment") {
            every {
                walletService.getAllNonArchivedConfirmedWalletTransactionsByMonthForAnalysis(any())
            } returns emptyList()
            every { creditCardService.getAllPaidPaymentsByMonth(YearMonth.of(2026, 1)) } returns
                listOf(
                    CreditCardPaymentFactory.create(
                        wallet = wallet,
                        creditCardDebt = CreditCardDebtFactory.create(category = electronics),
                        paidAmount = BigDecimal("300.00"),
                        date = LocalDateTime.of(2026, 1, 20, 0, 0),
                    ),
                    CreditCardPaymentFactory.create(
                        wallet = wallet,
                        creditCardDebt = CreditCardDebtFactory.create(category = electronics),
                        paidAmount = BigDecimal("999.00"),
                        refunded = true,
                        date = LocalDateTime.of(2026, 1, 22, 0, 0),
                    ),
                )

            When("building the summary for that month") {
                val summary =
                    service.buildSummary(
                        LocalDate.of(2026, 1, 1),
                        LocalDate.of(2026, 1, 31),
                        SpendAccountingMode.CASH_FLOW,
                    )

                Then("uses paidAmount and excludes refunded payments") {
                    summary.totalExpense shouldBe BigDecimal("300.00")
                }
            }
        }

        Given("a transaction whose date falls outside the exact range boundary") {
            every {
                walletService.getAllNonArchivedConfirmedWalletTransactionsByMonthForAnalysis(YearMonth.of(2026, 1))
            } returns
                listOf(
                    expense(LocalDateTime.of(2026, 1, 10, 10, 0), "50.00"),
                    expense(LocalDateTime.of(2026, 1, 20, 10, 0), "70.00"),
                )
            every { creditCardService.getDebtsBetweenForAnalysis(any(), any()) } returns emptyList()

            When("building the summary starting mid-month") {
                val summary =
                    service.buildSummary(
                        LocalDate.of(2026, 1, 15),
                        LocalDate.of(2026, 1, 31),
                        SpendAccountingMode.ACCRUAL,
                    )

                Then("transactions before the start date are excluded") {
                    summary.totalExpense shouldBe BigDecimal("70.00")
                }
            }
        }
    })
