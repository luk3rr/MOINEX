package org.moinex.ai.tools

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import org.json.JSONObject
import org.moinex.factory.CategoryFactory
import org.moinex.factory.NetWorthSnapshotFactory
import org.moinex.factory.creditcard.CreditCardDebtFactory
import org.moinex.factory.creditcard.CreditCardFactory
import org.moinex.factory.financialplanning.BudgetGroupFactory
import org.moinex.factory.financialplanning.FinancialPlanFactory
import org.moinex.factory.goal.GoalFactory
import org.moinex.factory.wallet.WalletTransactionFactory
import org.moinex.model.dto.InvestmentPerformanceDTO
import org.moinex.model.dto.PlanStatusDTO
import org.moinex.model.enums.WalletTransactionType
import org.moinex.service.PreferencesService
import org.moinex.service.creditcard.CreditCardService
import org.moinex.service.financialplanning.FinancialPlanningService
import org.moinex.service.goal.GoalService
import org.moinex.service.investment.InvestmentPerformanceService
import org.moinex.service.networth.NetWorthService
import org.moinex.service.wallet.WalletService
import java.math.BigDecimal
import java.time.YearMonth

class QueryFinanceToolTest :
    BehaviorSpec({
        val walletService = mockk<WalletService>()
        val creditCardService = mockk<CreditCardService>()
        val netWorthService = mockk<NetWorthService>()
        val goalService = mockk<GoalService>()
        val financialPlanningService = mockk<FinancialPlanningService>()
        val investmentPerformanceService = mockk<InvestmentPerformanceService>()
        val preferencesService = mockk<PreferencesService>(relaxed = true)

        val queryFinanceTool =
            QueryFinanceTool(
                walletService,
                creditCardService,
                netWorthService,
                goalService,
                financialPlanningService,
                investmentPerformanceService,
                preferencesService,
            )

        afterContainer { clearAllMocks() }

        Given("availableTools") {
            When("listing the exposed tools") {
                val tools = queryFinanceTool.availableTools()

                Then("it exposes the read-only finance query tools") {
                    tools.map { it.name }.toSet() shouldBe
                        setOf(
                            "get_net_worth",
                            "get_spending_by_category",
                            "get_transactions_in_period",
                            "get_credit_card_invoice_payment_status",
                            "get_income_vs_expenses",
                            "compare_periods",
                            "get_goals_progress",
                            "get_budget_vs_actual",
                            "get_portfolio_summary",
                        )
                }
            }
        }

        Given("a month with a saved net worth snapshot") {
            val month = YearMonth.of(2026, 9)
            every { netWorthService.getSnapshot(month) } returns
                NetWorthSnapshotFactory.create(referenceMonth = month, netWorth = BigDecimal("3500.00"))

            When("calling get_net_worth for that month") {
                val tool = queryFinanceTool.availableTools().first { it.name == "get_net_worth" }
                val result = JSONObject(tool.execute("""{"month":"2026-09"}"""))

                Then("it returns the stored net worth") {
                    result.getBoolean("available") shouldBe true
                    result.getBigDecimal("netWorth").compareTo(BigDecimal("3500.00")) shouldBe 0
                }
            }
        }

        Given("a month with no saved net worth snapshot") {
            val month = YearMonth.of(2026, 8)
            every { netWorthService.getSnapshot(month) } returns null

            When("calling get_net_worth for that month") {
                val tool = queryFinanceTool.availableTools().first { it.name == "get_net_worth" }
                val result = JSONObject(tool.execute("""{"month":"2026-08"}"""))

                Then("it reports the data as unavailable instead of failing") {
                    result.getBoolean("available") shouldBe false
                }
            }
        }

        Given("wallet expenses and a credit card purchase in the same category, in the same month") {
            val month = YearMonth.of(2026, 9)
            val foodCategory = CategoryFactory.create(id = 1, name = "Alimentação")

            every { walletService.getAllNonArchivedConfirmedWalletTransactionsByMonthForAnalysis(month) } returns
                listOf(
                    WalletTransactionFactory.create(
                        type = WalletTransactionType.EXPENSE,
                        category = foodCategory,
                        amount = BigDecimal("50.00"),
                    ),
                )
            every { creditCardService.getDebtsBetweenForAnalysis(any(), any()) } returns
                listOf(CreditCardDebtFactory.create(category = foodCategory, amount = BigDecimal("30.00")))

            When("calling get_spending_by_category for that month") {
                val tool = queryFinanceTool.availableTools().first { it.name == "get_spending_by_category" }
                val result = JSONObject(tool.execute("""{"month":"2026-09"}"""))

                Then("wallet expenses and credit card debts for the category are summed together") {
                    result
                        .getJSONObject(
                            "byCategory",
                        ).getBigDecimal("Alimentação")
                        .compareTo(BigDecimal("80.00")) shouldBe
                        0
                    result.getBigDecimal("total").compareTo(BigDecimal("80.00")) shouldBe 0
                }
            }
        }

        Given("wallet income in the month (not an expense)") {
            val month = YearMonth.of(2026, 9)
            val salaryCategory = CategoryFactory.create(id = 2, name = "Salário")

            every { walletService.getAllNonArchivedConfirmedWalletTransactionsByMonthForAnalysis(month) } returns
                listOf(
                    WalletTransactionFactory.create(
                        type = WalletTransactionType.INCOME,
                        category = salaryCategory,
                        amount = BigDecimal("5000.00"),
                    ),
                )
            every { creditCardService.getDebtsBetweenForAnalysis(any(), any()) } returns emptyList()

            When("calling get_spending_by_category for that month") {
                val tool = queryFinanceTool.availableTools().first { it.name == "get_spending_by_category" }
                val result = JSONObject(tool.execute("""{"month":"2026-09"}"""))

                Then("income is excluded from the spending breakdown") {
                    result.getJSONObject("byCategory").keySet().contains("Salário") shouldBe false
                    result.getBigDecimal("total") shouldBe BigDecimal.ZERO
                }
            }
        }

        Given("existing wallet transactions and credit card debts within a date range") {
            val foodCategory = CategoryFactory.create(id = 1, name = "Alimentação")

            every { walletService.getAllNonArchivedWalletTransactionsBetweenDates(any(), any()) } returns
                listOf(
                    WalletTransactionFactory.create(
                        type = WalletTransactionType.EXPENSE,
                        category = foodCategory,
                        amount = BigDecimal("42.00"),
                    ),
                )
            every { creditCardService.getDebtsBetweenForAnalysis(any(), any()) } returns
                listOf(CreditCardDebtFactory.create(category = foodCategory, amount = BigDecimal("99.00")))

            When("calling get_transactions_in_period") {
                val tool = queryFinanceTool.availableTools().first { it.name == "get_transactions_in_period" }
                val result = JSONObject(tool.execute("""{"startDate":"2026-09-01","endDate":"2026-09-30"}"""))

                Then("it lists both wallet transactions and credit card debts") {
                    result.getJSONArray("walletTransactions").length() shouldBe 1
                    result.getJSONArray("creditCardDebts").length() shouldBe 1
                }
            }
        }

        Given("a credit card whose invoice for a month is fully paid") {
            val card = CreditCardFactory.create(id = 5, name = "Nubank Mastercard")
            every { creditCardService.getAllNonArchivedCreditCardsOrderedByName() } returns listOf(card)
            every { creditCardService.isInvoicePaid(5, YearMonth.of(2026, 9)) } returns true

            When("calling get_credit_card_invoice_payment_status, matching the card name case-insensitively") {
                val tool =
                    queryFinanceTool.availableTools().first {
                        it.name == "get_credit_card_invoice_payment_status"
                    }
                val result =
                    JSONObject(tool.execute("""{"creditCardName":"nubank mastercard","month":"2026-09"}"""))

                Then("it reports the invoice as paid") {
                    result.getBoolean("paid") shouldBe true
                }
            }
        }

        Given("a credit card name that doesn't match any registered card") {
            every { creditCardService.getAllNonArchivedCreditCardsOrderedByName() } returns emptyList()

            When("calling get_credit_card_invoice_payment_status") {
                val tool =
                    queryFinanceTool.availableTools().first {
                        it.name == "get_credit_card_invoice_payment_status"
                    }
                val result = JSONObject(tool.execute("""{"creditCardName":"Unknown Card","month":"2026-09"}"""))

                Then("it returns an error instead of throwing") {
                    result.has("error") shouldBe true
                }
            }
        }

        Given("income and expenses for a month") {
            val month = YearMonth.of(2026, 9)
            val salaryCategory = CategoryFactory.create(id = 2, name = "Salário")
            val foodCategory = CategoryFactory.create(id = 1, name = "Alimentação")

            every { walletService.getAllNonArchivedConfirmedWalletTransactionsByMonthForAnalysis(month) } returns
                listOf(
                    WalletTransactionFactory.create(
                        type = WalletTransactionType.INCOME,
                        category = salaryCategory,
                        amount = BigDecimal("5000.00"),
                    ),
                    WalletTransactionFactory.create(
                        type = WalletTransactionType.EXPENSE,
                        category = foodCategory,
                        amount = BigDecimal("2000.00"),
                    ),
                )
            every { creditCardService.getDebtsBetweenForAnalysis(any(), any()) } returns
                listOf(CreditCardDebtFactory.create(category = foodCategory, amount = BigDecimal("500.00")))

            When("calling get_income_vs_expenses for that month") {
                val tool = queryFinanceTool.availableTools().first { it.name == "get_income_vs_expenses" }
                val result = JSONObject(tool.execute("""{"month":"2026-09"}"""))

                Then("it returns income, expenses (accrual) and the resulting savings rate") {
                    result.getBigDecimal("income").compareTo(BigDecimal("5000.00")) shouldBe 0
                    result.getBigDecimal("expenses").compareTo(BigDecimal("2500.00")) shouldBe 0
                    result.getBigDecimal("savings").compareTo(BigDecimal("2500.00")) shouldBe 0
                    result.getBigDecimal("savingsRatePercent").compareTo(BigDecimal("50.0")) shouldBe 0
                }
            }
        }

        Given("no income in the month") {
            val month = YearMonth.of(2026, 9)
            every { walletService.getAllNonArchivedConfirmedWalletTransactionsByMonthForAnalysis(month) } returns
                emptyList()
            every { creditCardService.getDebtsBetweenForAnalysis(any(), any()) } returns emptyList()

            When("calling get_income_vs_expenses") {
                val tool = queryFinanceTool.availableTools().first { it.name == "get_income_vs_expenses" }
                val result = JSONObject(tool.execute("""{"month":"2026-09"}"""))

                Then("the savings rate is reported as unavailable instead of dividing by zero") {
                    result.isNull("savingsRatePercent") shouldBe true
                }
            }
        }

        Given("expenses in the same category across two different months") {
            val month1 = YearMonth.of(2026, 8)
            val month2 = YearMonth.of(2026, 9)
            val foodCategory = CategoryFactory.create(id = 1, name = "Alimentação")

            every { walletService.getAllNonArchivedConfirmedWalletTransactionsByMonthForAnalysis(month1) } returns
                listOf(
                    WalletTransactionFactory.create(
                        type = WalletTransactionType.EXPENSE,
                        category = foodCategory,
                        amount = BigDecimal("1000.00"),
                    ),
                )
            every { walletService.getAllNonArchivedConfirmedWalletTransactionsByMonthForAnalysis(month2) } returns
                listOf(
                    WalletTransactionFactory.create(
                        type = WalletTransactionType.EXPENSE,
                        category = foodCategory,
                        amount = BigDecimal("1500.00"),
                    ),
                )
            every { creditCardService.getDebtsBetweenForAnalysis(any(), any()) } returns emptyList()

            When("calling compare_periods for those two months") {
                val tool = queryFinanceTool.availableTools().first { it.name == "compare_periods" }
                val result = JSONObject(tool.execute("""{"month1":"2026-08","month2":"2026-09"}"""))

                Then("it reports the per-category delta as month2 minus month1") {
                    result
                        .getJSONObject("byCategoryDelta")
                        .getBigDecimal("Alimentação")
                        .compareTo(BigDecimal("500.00")) shouldBe
                        0
                    result.getBigDecimal("expensesDelta").compareTo(BigDecimal("500.00")) shouldBe 0
                }
            }
        }

        Given("an active, non-archived goal partway to its target") {
            val goal =
                GoalFactory.create(
                    name = "Viagem",
                    initialBalance = BigDecimal("1000.00"),
                    targetBalance = BigDecimal("10000.00"),
                )
            goal.balance = BigDecimal("3700.00")
            every { goalService.getAllGoals() } returns listOf(goal)

            When("calling get_goals_progress") {
                val tool = queryFinanceTool.availableTools().first { it.name == "get_goals_progress" }
                val result = JSONObject(tool.execute("{}"))

                Then("it reports the goal's completion percentage") {
                    val goalJson = result.getJSONArray("goals").getJSONObject(0)
                    goalJson.getString("name") shouldBe "Viagem"
                    goalJson.getBigDecimal("progressPercentage").compareTo(BigDecimal("30.0")) shouldBe 0
                }
            }
        }

        Given("an archived goal") {
            val archivedGoal = GoalFactory.create(name = "Antiga", isArchived = true)
            every { goalService.getAllGoals() } returns listOf(archivedGoal)

            When("calling get_goals_progress") {
                val tool = queryFinanceTool.availableTools().first { it.name == "get_goals_progress" }
                val result = JSONObject(tool.execute("{}"))

                Then("archived goals are excluded") {
                    result.getJSONArray("goals").length() shouldBe 0
                }
            }
        }

        Given("an active financial plan with a budget group") {
            val month = YearMonth.of(2026, 9)
            val group = BudgetGroupFactory.create(name = "Food", targetPercentage = BigDecimal("100.00"))
            val plan =
                FinancialPlanFactory.create(
                    id = 1,
                    baseIncome = BigDecimal("5000.00"),
                    budgetGroups = mutableListOf(group),
                )
            every { financialPlanningService.getActivePlan() } returns plan
            every { financialPlanningService.getPlanStatus(1, month) } returns
                listOf(PlanStatusDTO(group, BigDecimal("1200.00")))

            When("calling get_budget_vs_actual for that month") {
                val tool = queryFinanceTool.availableTools().first { it.name == "get_budget_vs_actual" }
                val result = JSONObject(tool.execute("""{"month":"2026-09"}"""))

                Then("it compares the target amount (percentage of base income) against what was spent") {
                    val groupJson = result.getJSONArray("groups").getJSONObject(0)
                    groupJson.getBigDecimal("targetAmount").compareTo(BigDecimal("5000.00")) shouldBe 0
                    groupJson.getBigDecimal("remaining").compareTo(BigDecimal("3800.00")) shouldBe 0
                    groupJson.getBoolean("overBudget") shouldBe false
                }
            }
        }

        Given("no active financial plan") {
            every { financialPlanningService.getActivePlan() } returns null

            When("calling get_budget_vs_actual") {
                val tool = queryFinanceTool.availableTools().first { it.name == "get_budget_vs_actual" }
                val result = JSONObject(tool.execute("{}"))

                Then("it reports the data as unavailable instead of failing") {
                    result.getBoolean("available") shouldBe false
                }
            }
        }

        Given("investment performance snapshots across several months") {
            val latestMonth = YearMonth.of(2026, 9)
            every { investmentPerformanceService.getPerformanceData() } returns
                InvestmentPerformanceDTO(
                    monthlyInvested = mapOf(latestMonth to BigDecimal("500.00")),
                    portfolioValues = mapOf(latestMonth to BigDecimal("15000.00")),
                    accumulatedGains = mapOf(latestMonth to BigDecimal("2000.00")),
                    monthlyGains = mapOf(latestMonth to BigDecimal("300.00")),
                )

            When("calling get_portfolio_summary") {
                val tool = queryFinanceTool.availableTools().first { it.name == "get_portfolio_summary" }
                val result = JSONObject(tool.execute("{}"))

                Then("it returns the latest month's snapshot") {
                    result.getString("month") shouldBe "2026-09"
                    result.getBigDecimal("portfolioValue").compareTo(BigDecimal("15000.00")) shouldBe 0
                    result.getBigDecimal("accumulatedGains").compareTo(BigDecimal("2000.00")) shouldBe 0
                }
            }
        }

        Given("no investment performance snapshots available") {
            every { investmentPerformanceService.getPerformanceData() } returns
                InvestmentPerformanceDTO(emptyMap(), emptyMap(), emptyMap(), emptyMap())

            When("calling get_portfolio_summary") {
                val tool = queryFinanceTool.availableTools().first { it.name == "get_portfolio_summary" }
                val result = JSONObject(tool.execute("{}"))

                Then("it reports the data as unavailable instead of failing") {
                    result.getBoolean("available") shouldBe false
                }
            }
        }

        Given("an invalid month argument") {
            When("calling get_net_worth with a malformed month") {
                val tool = queryFinanceTool.availableTools().first { it.name == "get_net_worth" }
                val result = JSONObject(tool.execute("""{"month":"not-a-month"}"""))

                Then("it returns an error instead of throwing") {
                    result.has("error") shouldBe true
                }
            }
        }
    })
