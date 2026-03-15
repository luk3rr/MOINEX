package org.moinex.service.financialplanning

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import jakarta.persistence.EntityNotFoundException
import org.moinex.factory.CategoryFactory
import org.moinex.factory.financialplanning.BudgetGroupFactory
import org.moinex.factory.financialplanning.FinancialPlanFactory
import org.moinex.model.enums.BudgetGroupTransactionFilter
import org.moinex.model.enums.WalletTransactionType
import org.moinex.repository.financialplanning.FinancialPlanRepository
import org.moinex.service.CategoryService
import org.moinex.service.creditcard.CreditCardService
import org.moinex.service.financialplanning.FinancialPlanningService
import org.moinex.service.wallet.WalletService
import java.math.BigDecimal
import java.time.YearMonth
import java.util.Optional

class FinancialPlanningServiceTest :
    BehaviorSpec({
        val financialPlanRepository = mockk<FinancialPlanRepository>()
        val creditCardService = mockk<CreditCardService>()
        val categoryService = mockk<CategoryService>()
        val walletService = mockk<WalletService>()

        val service =
            FinancialPlanningService(
                financialPlanRepository,
                creditCardService,
                categoryService,
                walletService,
            )

        afterContainer { clearAllMocks(answers = true) }

        Given("a valid financial plan with budget groups") {
            val category1 = CategoryFactory.create(id = 1, name = "Food")
            val category2 = CategoryFactory.create(id = 2, name = "Transport")

            val budgetGroup1 =
                BudgetGroupFactory.create(
                    id = 1,
                    name = "Food",
                    targetPercentage = BigDecimal("40.00"),
                    categories = mutableSetOf(category1),
                    transactionTypeFilter = BudgetGroupTransactionFilter.EXPENSE,
                )

            val budgetGroup2 =
                BudgetGroupFactory.create(
                    id = 2,
                    name = "Transport",
                    targetPercentage = BigDecimal("60.00"),
                    categories = mutableSetOf(category2),
                    transactionTypeFilter = BudgetGroupTransactionFilter.EXPENSE,
                )

            val plan =
                FinancialPlanFactory.create(
                    id = 1,
                    name = "Monthly Budget",
                    baseIncome = BigDecimal("5000.00"),
                    budgetGroups = mutableListOf(budgetGroup1, budgetGroup2),
                    archived = false,
                )

            When("creating the financial plan") {
                every { financialPlanRepository.existsByName("Monthly Budget") } returns false
                every { categoryService.existsById(1) } returns true
                every { categoryService.existsById(2) } returns true
                every { financialPlanRepository.findByArchivedFalse() } returns null
                every { financialPlanRepository.save(any()) } returns
                    FinancialPlanFactory.create(
                        id = 1,
                        name = "Monthly Budget",
                        baseIncome = BigDecimal("5000.00"),
                        budgetGroups = mutableListOf(budgetGroup1, budgetGroup2),
                    )

                val result = service.createPlan(plan)

                Then("should return the plan id") {
                    result shouldBe 1
                }

                Then("should save the plan") {
                    verify { financialPlanRepository.save(any()) }
                }
            }
        }

        Given("a plan with duplicate name") {
            val category = CategoryFactory.create(id = 1, name = "Food")
            val budgetGroup =
                BudgetGroupFactory.create(
                    id = 1,
                    name = "Food",
                    targetPercentage = BigDecimal("100.00"),
                    categories = mutableSetOf(category),
                )

            val plan =
                FinancialPlanFactory.create(
                    name = "Existing Plan",
                    budgetGroups = mutableListOf(budgetGroup),
                )

            When("attempting to create a plan with duplicate name") {
                every { financialPlanRepository.existsByName("Existing Plan") } returns true

                Then("should throw IllegalStateException") {
                    shouldThrow<IllegalStateException> {
                        service.createPlan(plan)
                    }
                }
            }
        }

        Given("a plan with non-existent category") {
            val category = CategoryFactory.create(id = 999, name = "Non-existent")
            val budgetGroup =
                BudgetGroupFactory.create(
                    id = 1,
                    name = "Food",
                    targetPercentage = BigDecimal("100.00"),
                    categories = mutableSetOf(category),
                )

            val plan =
                FinancialPlanFactory.create(
                    name = "Invalid Plan",
                    budgetGroups = mutableListOf(budgetGroup),
                )

            When("attempting to create a plan with non-existent category") {
                every { financialPlanRepository.existsByName("Invalid Plan") } returns false
                every { categoryService.existsById(999) } returns false

                Then("should throw IllegalStateException") {
                    shouldThrow<IllegalStateException> {
                        service.createPlan(plan)
                    }
                }
            }
        }

        Given("an archived category in budget group") {
            val category = CategoryFactory.create(id = 1, name = "Food", isArchived = true)
            val budgetGroup =
                BudgetGroupFactory.create(
                    id = 1,
                    name = "Food",
                    targetPercentage = BigDecimal("100.00"),
                    categories = mutableSetOf(category),
                )

            val plan =
                FinancialPlanFactory.create(
                    name = "Plan with Archived Category",
                    budgetGroups = mutableListOf(budgetGroup),
                )

            When("attempting to create a plan with archived category") {
                every { financialPlanRepository.existsByName("Plan with Archived Category") } returns false
                every { categoryService.existsById(1) } returns true

                Then("should throw IllegalStateException") {
                    shouldThrow<IllegalStateException> {
                        service.createPlan(plan)
                    }
                }
            }
        }

        Given("an existing financial plan to update") {
            val category = CategoryFactory.create(id = 1, name = "Food")
            val budgetGroup =
                BudgetGroupFactory.create(
                    id = 1,
                    name = "Food",
                    targetPercentage = BigDecimal("100.00"),
                    categories = mutableSetOf(category),
                )

            val existingPlan =
                FinancialPlanFactory.create(
                    id = 1,
                    name = "Old Name",
                    baseIncome = BigDecimal("5000.00"),
                    budgetGroups = mutableListOf(budgetGroup),
                )

            val updatedPlan =
                FinancialPlanFactory.create(
                    id = 1,
                    name = "New Name",
                    baseIncome = BigDecimal("6000.00"),
                    budgetGroups = mutableListOf(budgetGroup),
                )

            When("updating the plan") {
                every { financialPlanRepository.findById(1) } returns Optional.of(existingPlan)
                every { financialPlanRepository.existsByNameAndIdNot("New Name", 1) } returns false
                every { categoryService.existsById(1) } returns true

                service.updatePlan(updatedPlan)

                Then("should update plan name") {
                    existingPlan.name shouldBe "New Name"
                }

                Then("should update base income") {
                    existingPlan.baseIncome shouldBe BigDecimal("6000.00")
                }
            }
        }

        Given("a non-existent plan to update") {
            val category = CategoryFactory.create(id = 1, name = "Food")
            val budgetGroup =
                BudgetGroupFactory.create(
                    id = 1,
                    name = "Food",
                    targetPercentage = BigDecimal("100.00"),
                    categories = mutableSetOf(category),
                )

            val plan =
                FinancialPlanFactory.create(
                    id = 999,
                    name = "Non-existent",
                    budgetGroups = mutableListOf(budgetGroup),
                )

            When("attempting to update a non-existent plan") {
                every { financialPlanRepository.findById(999) } returns Optional.empty()

                Then("should throw EntityNotFoundException") {
                    shouldThrow<EntityNotFoundException> {
                        service.updatePlan(plan)
                    }
                }
            }
        }

        Given("a plan update with duplicate name for different plan") {
            val category = CategoryFactory.create(id = 1, name = "Food")
            val budgetGroup =
                BudgetGroupFactory.create(
                    id = 1,
                    name = "Food",
                    targetPercentage = BigDecimal("100.00"),
                    categories = mutableSetOf(category),
                )

            val existingPlan =
                FinancialPlanFactory.create(
                    id = 1,
                    name = "Plan 1",
                    budgetGroups = mutableListOf(budgetGroup),
                )

            val updatedPlan =
                FinancialPlanFactory.create(
                    id = 1,
                    name = "Duplicate Name",
                    budgetGroups = mutableListOf(budgetGroup),
                )

            When("attempting to update plan with name that already exists for another plan") {
                every { financialPlanRepository.findById(1) } returns Optional.of(existingPlan)
                every { financialPlanRepository.existsByNameAndIdNot("Duplicate Name", 1) } returns true

                Then("should throw IllegalStateException") {
                    shouldThrow<IllegalStateException> {
                        service.updatePlan(updatedPlan)
                    }
                }
            }
        }

        Given("a financial plan with no active plan") {
            When("getting non-archived plan when none exists") {
                every { financialPlanRepository.findByArchivedFalse() } returns null

                val result = service.getNonArchivedPlan()

                Then("should return null") {
                    result shouldBe null
                }
            }
        }

        Given("a financial plan with active plan") {
            val category = CategoryFactory.create(id = 1, name = "Food")
            val budgetGroup =
                BudgetGroupFactory.create(
                    id = 1,
                    name = "Food",
                    targetPercentage = BigDecimal("100.00"),
                    categories = mutableSetOf(category),
                )

            val plan =
                FinancialPlanFactory.create(
                    id = 1,
                    name = "Active Plan",
                    budgetGroups = mutableListOf(budgetGroup),
                    archived = false,
                )

            When("getting non-archived plan") {
                every { financialPlanRepository.findByArchivedFalse() } returns plan

                val result = service.getNonArchivedPlan()

                Then("should return the active plan") {
                    result shouldBe plan
                }
            }
        }

        Given("a plan with budget groups and transactions") {
            val category = CategoryFactory.create(id = 1, name = "Food")
            val category2 = CategoryFactory.create(id = 2, name = "Transport")

            val budgetGroup =
                BudgetGroupFactory.create(
                    id = 1,
                    name = "Food",
                    targetPercentage = BigDecimal("50.00"),
                    categories = mutableSetOf(category),
                    transactionTypeFilter = BudgetGroupTransactionFilter.EXPENSE,
                )

            val budgetGroup2 =
                BudgetGroupFactory.create(
                    id = 2,
                    name = "Transport",
                    targetPercentage = BigDecimal("50.00"),
                    categories = mutableSetOf(category2),
                    transactionTypeFilter = BudgetGroupTransactionFilter.EXPENSE,
                )

            val plan =
                FinancialPlanFactory.create(
                    id = 1,
                    name = "Test Plan",
                    baseIncome = BigDecimal("5000.00"),
                    budgetGroups = mutableListOf(budgetGroup, budgetGroup2),
                )

            When("getting plan status for a period") {
                val period = YearMonth.of(2026, 3)

                every { financialPlanRepository.findById(1) } returns Optional.of(plan)
                every {
                    walletService.getTotalWalletTransactionAmountByCategoriesAndTypeAndDateTimeBetween(
                        listOf(1),
                        WalletTransactionType.EXPENSE,
                        any(),
                        any(),
                    )
                } returns BigDecimal("1500.00")
                every {
                    creditCardService.getTotalPaymentsByCategoriesAndDateTimeBetween(
                        listOf(1),
                        any(),
                        any(),
                    )
                } returns BigDecimal("500.00")
                every {
                    walletService.getTotalWalletTransactionAmountByCategoriesAndTypeAndDateTimeBetween(
                        listOf(2),
                        WalletTransactionType.EXPENSE,
                        any(),
                        any(),
                    )
                } returns BigDecimal("800.00")
                every {
                    creditCardService.getTotalPaymentsByCategoriesAndDateTimeBetween(
                        listOf(2),
                        any(),
                        any(),
                    )
                } returns BigDecimal("200.00")

                val result = service.getPlanStatus(1, period)

                Then("should return plan status with correct spent amount") {
                    result.size shouldBe 2
                    result[0].spentAmount shouldBe BigDecimal("2000.00")
                }

                Then("should return correct budget group") {
                    result[0].group.name shouldBe "Food"
                }
            }
        }

        Given("a plan with income and expense budget groups") {
            val expenseCategory = CategoryFactory.create(id = 1, name = "Food")
            val incomeCategory = CategoryFactory.create(id = 2, name = "Salary")

            val expenseGroup =
                BudgetGroupFactory.create(
                    id = 1,
                    name = "Expenses",
                    targetPercentage = BigDecimal("60.00"),
                    categories = mutableSetOf(expenseCategory),
                    transactionTypeFilter = BudgetGroupTransactionFilter.EXPENSE,
                )

            val incomeGroup =
                BudgetGroupFactory.create(
                    id = 2,
                    name = "Income",
                    targetPercentage = BigDecimal("40.00"),
                    categories = mutableSetOf(incomeCategory),
                    transactionTypeFilter = BudgetGroupTransactionFilter.INCOME,
                )

            val plan =
                FinancialPlanFactory.create(
                    id = 1,
                    name = "Income and Expense Plan",
                    baseIncome = BigDecimal("5000.00"),
                    budgetGroups = mutableListOf(expenseGroup, incomeGroup),
                )

            When("getting plan status with both income and expenses") {
                val period = YearMonth.of(2026, 3)

                every { financialPlanRepository.findById(1) } returns Optional.of(plan)
                every {
                    walletService.getTotalWalletTransactionAmountByCategoriesAndTypeAndDateTimeBetween(
                        listOf(1),
                        WalletTransactionType.EXPENSE,
                        any(),
                        any(),
                    )
                } returns BigDecimal("2000.00")
                every {
                    creditCardService.getTotalPaymentsByCategoriesAndDateTimeBetween(
                        listOf(1),
                        any(),
                        any(),
                    )
                } returns BigDecimal("500.00")
                every {
                    walletService.getTotalWalletTransactionAmountByCategoriesAndTypeAndDateTimeBetween(
                        listOf(2),
                        WalletTransactionType.INCOME,
                        any(),
                        any(),
                    )
                } returns BigDecimal("5000.00")

                val result = service.getPlanStatus(1, period)

                Then("should return both expense and income groups") {
                    result.size shouldBe 2
                }

                Then("should calculate expense group amount correctly") {
                    result[0].spentAmount shouldBe BigDecimal("2500.00")
                }

                Then("should calculate income group amount correctly") {
                    result[1].spentAmount shouldBe BigDecimal("5000.00")
                }
            }
        }

        Given("an existing active plan when creating a new one") {
            val category = CategoryFactory.create(id = 1, name = "Food")
            val budgetGroup =
                BudgetGroupFactory.create(
                    id = 1,
                    name = "Food",
                    targetPercentage = BigDecimal("100.00"),
                    categories = mutableSetOf(category),
                )

            val existingPlan =
                FinancialPlanFactory.create(
                    id = 1,
                    name = "Old Plan",
                    budgetGroups = mutableListOf(budgetGroup),
                    archived = false,
                )

            val newPlan =
                FinancialPlanFactory.create(
                    name = "New Plan",
                    budgetGroups = mutableListOf(budgetGroup),
                    archived = false,
                )

            When("creating a new plan when an active plan exists") {
                every { financialPlanRepository.existsByName("New Plan") } returns false
                every { categoryService.existsById(1) } returns true
                every { financialPlanRepository.findByArchivedFalse() } returns existingPlan
                every { financialPlanRepository.save(any()) } returns
                    FinancialPlanFactory.create(
                        id = 2,
                        name = "New Plan",
                        budgetGroups = mutableListOf(budgetGroup),
                    )

                service.createPlan(newPlan)

                Then("should archive the previous plan") {
                    existingPlan.archived shouldBe true
                }
            }
        }

        Given("a plan with historical data spanning multiple months") {
            val category1 = CategoryFactory.create(id = 1, name = "Food")
            val category2 = CategoryFactory.create(id = 2, name = "Transport")

            val budgetGroup1 =
                BudgetGroupFactory.create(
                    id = 1,
                    name = "Food",
                    targetPercentage = BigDecimal("50.00"),
                    categories = mutableSetOf(category1),
                    transactionTypeFilter = BudgetGroupTransactionFilter.EXPENSE,
                )

            val budgetGroup2 =
                BudgetGroupFactory.create(
                    id = 2,
                    name = "Transport",
                    targetPercentage = BigDecimal("50.00"),
                    categories = mutableSetOf(category2),
                    transactionTypeFilter = BudgetGroupTransactionFilter.EXPENSE,
                )

            val plan =
                FinancialPlanFactory.create(
                    id = 1,
                    name = "Test Plan",
                    baseIncome = BigDecimal("5000.00"),
                    budgetGroups = mutableListOf(budgetGroup1, budgetGroup2),
                )

            When("getting historical data for multiple months") {
                val startPeriod = YearMonth.of(2026, 1)
                val endPeriod = YearMonth.of(2026, 3)

                every { financialPlanRepository.findById(1) } returns Optional.of(plan)
                every {
                    walletService.getTotalWalletTransactionAmountByCategoriesAndTypeAndDateTimeBetween(
                        listOf(1),
                        WalletTransactionType.EXPENSE,
                        any(),
                        any(),
                    )
                } returns BigDecimal("1000.00")
                every {
                    creditCardService.getTotalPaymentsByCategoriesAndDateTimeBetween(
                        listOf(1),
                        any(),
                        any(),
                    )
                } returns BigDecimal("500.00")

                every {
                    walletService.getTotalWalletTransactionAmountByCategoriesAndTypeAndDateTimeBetween(
                        listOf(2),
                        WalletTransactionType.EXPENSE,
                        any(),
                        any(),
                    )
                } returns BigDecimal("800.00")
                every {
                    creditCardService.getTotalPaymentsByCategoriesAndDateTimeBetween(
                        listOf(2),
                        any(),
                        any(),
                    )
                } returns BigDecimal("200.00")

                val result = service.getHistoricalData(1, startPeriod, endPeriod)

                Then("should return historical data for all months and groups") {
                    result.size shouldBe 6
                }

                Then("should have correct group names") {
                    result.filter { it.groupName == "Food" }.size shouldBe 3
                    result.filter { it.groupName == "Transport" }.size shouldBe 3
                }

                Then("should have correct target amounts") {
                    result.filter { it.groupName == "Food" }.all { it.targetAmount == BigDecimal("2500.00") } shouldBe true
                    result.filter { it.groupName == "Transport" }.all { it.targetAmount == BigDecimal("2500.00") } shouldBe true
                }

                Then("should have correct spent amounts for each group") {
                    result.filter { it.groupName == "Food" }.all { it.spentAmount == BigDecimal("1500.00") } shouldBe true
                    result.filter { it.groupName == "Transport" }.all { it.spentAmount == BigDecimal("1000.00") } shouldBe true
                }
            }
        }
    })
