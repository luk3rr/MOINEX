/*
 * Filename: FinancialInsightGeneratorTest.kt
 * Created on: September 5, 2026
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 */

package org.moinex.service.summary

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import org.moinex.model.dto.AnnualSummaryDTO
import org.moinex.model.dto.CategoryBreakdownDTO
import org.moinex.model.dto.MonthlyFlowDTO
import org.moinex.model.dto.TopLineItemDTO
import org.moinex.model.enums.FinancialInsightSeverity
import org.moinex.model.enums.FinancialInsightType
import org.moinex.model.enums.SpendAccountingMode
import org.moinex.model.enums.TransactionSource
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth

class FinancialInsightGeneratorTest :
    BehaviorSpec({
        val generator = FinancialInsightGenerator()

        fun category(
            name: String,
            percentage: String,
        ) = CategoryBreakdownDTO(name, BigDecimal("100.00"), BigDecimal(percentage), BigDecimal("10.00"))

        fun flow(
            month: Int,
            income: String,
            net: String,
        ): MonthlyFlowDTO {
            val incomeValue = BigDecimal(income)
            val netValue = BigDecimal(net)
            return MonthlyFlowDTO(YearMonth.of(2026, month), incomeValue, incomeValue - netValue, netValue)
        }

        fun summary(
            savingsRate: String = "10.00",
            monthlyFlows: List<MonthlyFlowDTO> = emptyList(),
            expenseByCategory: List<CategoryBreakdownDTO> = emptyList(),
            incomeByCategory: List<CategoryBreakdownDTO> = emptyList(),
            topExpenses: List<TopLineItemDTO> = emptyList(),
        ) = AnnualSummaryDTO(
            startDate = LocalDate.of(2026, 1, 1),
            endDate = LocalDate.of(2026, 12, 31),
            mode = SpendAccountingMode.ACCRUAL,
            monthCount = monthlyFlows.size,
            totalIncome = BigDecimal.ZERO,
            totalExpense = BigDecimal.ZERO,
            netBalance = BigDecimal.ZERO,
            savingsRatePercentage = BigDecimal(savingsRate),
            expenseByCategory = expenseByCategory,
            incomeByCategory = incomeByCategory,
            monthlyFlows = monthlyFlows,
            topExpenses = topExpenses,
            insights = emptyList(),
        )

        afterContainer { }

        Given("different savings rates") {
            When("the rate is healthy") {
                val insights = generator.generate(summary(savingsRate = "30.00"))
                Then("a positive savings-rate insight is produced") {
                    val insight = insights.first { it.type == FinancialInsightType.SAVINGS_RATE }
                    insight.severity shouldBe FinancialInsightSeverity.POSITIVE
                }
            }
            When("the rate is low") {
                val insights = generator.generate(summary(savingsRate = "2.00"))
                Then("a warning savings-rate insight is produced") {
                    val insight = insights.first { it.type == FinancialInsightType.SAVINGS_RATE }
                    insight.severity shouldBe FinancialInsightSeverity.WARNING
                }
            }
            When("the rate is moderate") {
                val insights = generator.generate(summary(savingsRate = "12.00"))
                Then("a neutral savings-rate insight is produced") {
                    val insight = insights.first { it.type == FinancialInsightType.SAVINGS_RATE }
                    insight.severity shouldBe FinancialInsightSeverity.NEUTRAL
                }
            }
        }

        Given("income rising while savings ratio does not") {
            val flows =
                listOf(
                    flow(1, "1000.00", "500.00"),
                    flow(2, "1000.00", "500.00"),
                    flow(3, "2000.00", "100.00"),
                    flow(4, "2000.00", "100.00"),
                )
            When("generating insights") {
                val insights = generator.generate(summary(monthlyFlows = flows))
                Then("a lifestyle-inflation warning is produced") {
                    val insight = insights.firstOrNull { it.type == FinancialInsightType.LIFESTYLE_INFLATION }
                    insight.shouldNotBeNull()
                    insight.severity shouldBe FinancialInsightSeverity.WARNING
                }
            }
        }

        Given("fewer than four months of data") {
            val flows =
                listOf(
                    flow(1, "1000.00", "100.00"),
                    flow(2, "3000.00", "100.00"),
                    flow(3, "3000.00", "100.00"),
                )
            When("generating insights") {
                val insights = generator.generate(summary(monthlyFlows = flows))
                Then("no lifestyle-inflation insight is produced") {
                    insights.none { it.type == FinancialInsightType.LIFESTYLE_INFLATION } shouldBe true
                }
            }
        }

        Given("spending concentrated in the top categories") {
            When("the top three exceed the threshold") {
                val insights =
                    generator.generate(
                        summary(
                            expenseByCategory =
                                listOf(
                                    category("Eletrônicos", "30.00"),
                                    category("Vestuário", "20.00"),
                                    category("Casa", "10.00"),
                                ),
                        ),
                    )
                Then("a spending-concentration warning is produced") {
                    insights.any { it.type == FinancialInsightType.SPENDING_CONCENTRATION } shouldBe true
                }
            }
            When("the top three stay under the threshold") {
                val insights =
                    generator.generate(
                        summary(
                            expenseByCategory =
                                listOf(
                                    category("Eletrônicos", "20.00"),
                                    category("Vestuário", "10.00"),
                                    category("Casa", "5.00"),
                                ),
                        ),
                    )
                Then("no spending-concentration insight is produced") {
                    insights.none { it.type == FinancialInsightType.SPENDING_CONCENTRATION } shouldBe true
                }
            }
        }

        Given("income concentrated in a single source") {
            When("the largest source exceeds the threshold") {
                val insights =
                    generator.generate(summary(incomeByCategory = listOf(category("Salário", "90.00"))))
                Then("an income-concentration warning is produced") {
                    insights.any { it.type == FinancialInsightType.INCOME_CONCENTRATION } shouldBe true
                }
            }
        }

        Given("a month with negative balance and a large purchase") {
            val flows = listOf(flow(1, "1000.00", "-500.00"))
            val topExpenses =
                listOf(
                    TopLineItemDTO(
                        LocalDateTime.of(2026, 1, 10, 0, 0),
                        "Guitarra",
                        BigDecimal("400.00"),
                        "Eletrônicos",
                        TransactionSource.WALLET,
                    ),
                )
            When("generating insights") {
                val insights = generator.generate(summary(monthlyFlows = flows, topExpenses = topExpenses))
                Then("a spending-spike warning is produced") {
                    val insight = insights.firstOrNull { it.type == FinancialInsightType.SPENDING_SPIKE }
                    insight.shouldNotBeNull()
                    insight.messageArgs.contains("Guitarra") shouldBe true
                }
            }
        }
    })
