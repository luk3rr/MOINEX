package org.moinex.service.wallet

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import jakarta.persistence.EntityNotFoundException
import org.moinex.factory.CategoryFactory
import org.moinex.factory.wallet.RecurringTransactionFactory
import org.moinex.factory.wallet.WalletFactory
import org.moinex.model.enums.RecurringTransactionFrequency
import org.moinex.model.enums.RecurringTransactionStatus
import org.moinex.model.enums.WalletTransactionStatus
import org.moinex.model.enums.WalletTransactionType
import org.moinex.repository.wallettransaction.RecurringTransactionRepository
import org.moinex.service.RecurringTransactionService
import org.moinex.service.WalletService
import org.moinex.util.Constants
import java.math.BigDecimal
import java.time.LocalDate
import java.time.Year
import java.time.YearMonth
import java.util.Optional

class RecurringTransactionServiceTest :
    BehaviorSpec({
        val recurringTransactionRepository = mockk<RecurringTransactionRepository>()
        val walletService = mockk<WalletService>()

        val service = RecurringTransactionService(recurringTransactionRepository, walletService)

        afterContainer { clearAllMocks(answers = true) }

        Given("a valid recurring transaction") {
            When("creating a new recurring transaction") {
                val recurringTransaction =
                    RecurringTransactionFactory.create(
                        id = null,
                        startDate = LocalDate.of(2024, 1, 1),
                        endDate = LocalDate.of(2024, 12, 31),
                        nextDueDate = LocalDate.of(2024, 1, 1),
                        frequency = RecurringTransactionFrequency.MONTHLY,
                        amount = BigDecimal("500.00"),
                    )

                every { recurringTransactionRepository.save(any()) } returns
                    RecurringTransactionFactory.create(
                        id = 1,
                        startDate = LocalDate.of(2024, 1, 1),
                        endDate = LocalDate.of(2024, 12, 31),
                        nextDueDate = LocalDate.of(2024, 1, 1),
                        frequency = RecurringTransactionFrequency.MONTHLY,
                        amount = BigDecimal("500.00"),
                    )

                val result = service.createRecurringTransaction(recurringTransaction)

                Then("should return the created recurring transaction id") {
                    result shouldBe 1
                }

                Then("should call repository save method") {
                    verify { recurringTransactionRepository.save(any()) }
                }
            }
        }

        Given("an existing recurring transaction") {
            And("the recurring transaction exists in the database") {
                When("deleting the recurring transaction") {
                    val recurringTransaction =
                        RecurringTransactionFactory.create(
                            id = 1,
                            startDate = LocalDate.of(2024, 1, 1),
                            endDate = LocalDate.of(2024, 12, 31),
                            nextDueDate = LocalDate.of(2024, 1, 1),
                        )

                    every { recurringTransactionRepository.findById(1) } returns Optional.of(recurringTransaction)
                    every { recurringTransactionRepository.delete(recurringTransaction) } returns Unit

                    service.deleteRecurringTransaction(1)

                    Then("should call repository delete method") {
                        verify { recurringTransactionRepository.delete(recurringTransaction) }
                    }
                }
            }

            And("the recurring transaction does not exist") {
                When("deleting the recurring transaction") {
                    every { recurringTransactionRepository.findById(999) } returns Optional.empty()

                    Then("should throw EntityNotFoundException") {
                        shouldThrow<EntityNotFoundException> {
                            service.deleteRecurringTransaction(999)
                        }
                    }
                }
            }
        }

        Given("an existing recurring transaction to update") {
            And("the recurring transaction exists in the database") {
                When("updating the recurring transaction") {
                    val wallet = WalletFactory.create(id = 1)
                    val category = CategoryFactory.create(id = 1)
                    val existingRecurringTransaction =
                        RecurringTransactionFactory.create(
                            id = 1,
                            startDate = LocalDate.of(2024, 1, 1),
                            endDate = LocalDate.of(2024, 12, 31),
                            nextDueDate = LocalDate.of(2024, 1, 1),
                            frequency = RecurringTransactionFrequency.MONTHLY,
                            amount = BigDecimal("100.00"),
                            description = "Old description",
                        )

                    val updatedRecurringTransaction =
                        RecurringTransactionFactory.create(
                            id = 1,
                            startDate = LocalDate.of(2024, 1, 1),
                            endDate = LocalDate.of(2024, 12, 31),
                            nextDueDate = LocalDate.of(2024, 2, 1),
                            frequency = RecurringTransactionFrequency.WEEKLY,
                            amount = BigDecimal("200.00"),
                            description = "New description",
                            wallet = wallet,
                            category = category,
                            type = WalletTransactionType.INCOME,
                            status = RecurringTransactionStatus.INACTIVE,
                            includeInAnalysis = false,
                            includeInNetWorth = true,
                        )

                    every { recurringTransactionRepository.findById(1) } returns Optional.of(existingRecurringTransaction)

                    service.updateRecurringTransaction(updatedRecurringTransaction)

                    Then("should update all fields") {
                        existingRecurringTransaction.wallet shouldBe wallet
                        existingRecurringTransaction.category shouldBe category
                        existingRecurringTransaction.type shouldBe WalletTransactionType.INCOME
                        existingRecurringTransaction.amount shouldBe BigDecimal("200.00")
                        existingRecurringTransaction.endDate shouldBe LocalDate.of(2024, 12, 31)
                        existingRecurringTransaction.nextDueDate shouldBe LocalDate.of(2024, 2, 1)
                        existingRecurringTransaction.description shouldBe "New description"
                        existingRecurringTransaction.frequency shouldBe RecurringTransactionFrequency.WEEKLY
                        existingRecurringTransaction.status shouldBe RecurringTransactionStatus.INACTIVE
                        existingRecurringTransaction.includeInAnalysis shouldBe false
                        existingRecurringTransaction.includeInNetWorth shouldBe true
                    }
                }
            }

            And("the recurring transaction does not exist") {
                When("updating the recurring transaction") {
                    val updatedRecurringTransaction =
                        RecurringTransactionFactory.create(
                            id = 999,
                            startDate = LocalDate.of(2024, 1, 1),
                            endDate = LocalDate.of(2024, 12, 31),
                            nextDueDate = LocalDate.of(2024, 1, 1),
                        )

                    every { recurringTransactionRepository.findById(999) } returns Optional.empty()

                    Then("should throw EntityNotFoundException") {
                        shouldThrow<EntityNotFoundException> {
                            service.updateRecurringTransaction(updatedRecurringTransaction)
                        }
                    }
                }
            }
        }

        Given("active recurring transactions to process") {
            And("there are transactions due") {
                When("processing recurring transactions") {
                    val now = LocalDate.now()
                    val recurringTransaction =
                        RecurringTransactionFactory.create(
                            id = 1,
                            startDate = now.minusMonths(2),
                            endDate = now.plusMonths(10),
                            nextDueDate = now.minusDays(5),
                            frequency = RecurringTransactionFrequency.MONTHLY,
                            amount = BigDecimal("500.00"),
                            status = RecurringTransactionStatus.ACTIVE,
                        )

                    every { recurringTransactionRepository.findByStatus(RecurringTransactionStatus.ACTIVE) } returns
                        listOf(recurringTransaction)
                    every { walletService.createWalletTransaction(any()) } returns 1

                    service.processRecurringTransactions()

                    Then("should create wallet transactions") {
                        verify(atLeast = 1) { walletService.createWalletTransaction(any()) }
                    }

                    Then("should update next due date") {
                        recurringTransaction.nextDueDate.isAfter(now.minusDays(5)) shouldBe true
                    }
                }
            }

            And("there are no active recurring transactions") {
                When("processing recurring transactions") {
                    every { recurringTransactionRepository.findByStatus(RecurringTransactionStatus.ACTIVE) } returns emptyList()

                    service.processRecurringTransactions()

                    Then("should not create any wallet transactions") {
                        verify(exactly = 0) { walletService.createWalletTransaction(any()) }
                    }
                }
            }
        }

        Given("a request to get future transactions by year") {
            When("getting future transactions for a year range") {
                val startYear = Year.of(2024)
                val endYear = Year.of(2025)
                val recurringTransaction =
                    RecurringTransactionFactory.create(
                        id = 1,
                        startDate = LocalDate.of(2024, 1, 1),
                        endDate = LocalDate.of(2025, 12, 31),
                        nextDueDate = LocalDate.of(2024, 1, 1),
                        frequency = RecurringTransactionFrequency.MONTHLY,
                        amount = BigDecimal("300.00"),
                        status = RecurringTransactionStatus.ACTIVE,
                    )

                every { recurringTransactionRepository.findByStatus(RecurringTransactionStatus.ACTIVE) } returns
                    listOf(recurringTransaction)

                val result = service.getFutureRecurringTransactionsByYear(startYear, endYear)

                Then("should return list of future transactions") {
                    result.size shouldBe 24
                }

                Then("should have pending status") {
                    result.all { it.status == WalletTransactionStatus.PENDING } shouldBe true
                }

                Then("should have correct amount") {
                    result.all { it.amount == BigDecimal("300.00") } shouldBe true
                }
            }
        }

        Given("a request to get future transactions by month for analysis") {
            And("transactions are marked for analysis") {
                When("getting future transactions for a month range") {
                    val startMonth = YearMonth.of(2024, 1)
                    val endMonth = YearMonth.of(2024, 3)
                    val recurringTransaction =
                        RecurringTransactionFactory.create(
                            id = 1,
                            startDate = LocalDate.of(2024, 1, 1),
                            endDate = LocalDate.of(2024, 12, 31),
                            nextDueDate = LocalDate.of(2024, 1, 1),
                            frequency = RecurringTransactionFrequency.MONTHLY,
                            amount = BigDecimal("200.00"),
                            status = RecurringTransactionStatus.ACTIVE,
                            includeInAnalysis = true,
                        )

                    every { recurringTransactionRepository.findByStatus(RecurringTransactionStatus.ACTIVE) } returns
                        listOf(recurringTransaction)

                    val result = service.getFutureRecurringTransactionsByMonthForAnalysis(startMonth, endMonth)

                    Then("should return list of future transactions") {
                        result.size shouldBe 3
                    }

                    Then("should include only transactions marked for analysis") {
                        result.all { it.includeInAnalysis } shouldBe true
                    }
                }
            }

            And("transactions are not marked for analysis") {
                When("getting future transactions for a month range") {
                    val startMonth = YearMonth.of(2024, 1)
                    val endMonth = YearMonth.of(2024, 3)
                    val recurringTransaction =
                        RecurringTransactionFactory.create(
                            id = 1,
                            startDate = LocalDate.of(2024, 1, 1),
                            endDate = LocalDate.of(2024, 12, 31),
                            nextDueDate = LocalDate.of(2024, 1, 1),
                            frequency = RecurringTransactionFrequency.MONTHLY,
                            includeInAnalysis = false,
                        )

                    every { recurringTransactionRepository.findByStatus(RecurringTransactionStatus.ACTIVE) } returns
                        listOf(recurringTransaction)

                    val result = service.getFutureRecurringTransactionsByMonthForAnalysis(startMonth, endMonth)

                    Then("should return empty list") {
                        result.size shouldBe 0
                    }
                }
            }
        }

        Given("a request to get all recurring transactions") {
            When("getting all recurring transactions") {
                val transactions =
                    listOf(
                        RecurringTransactionFactory.create(
                            id = 1,
                            startDate = LocalDate.of(2024, 1, 1),
                            endDate = LocalDate.of(2024, 12, 31),
                            nextDueDate = LocalDate.of(2024, 1, 1),
                        ),
                        RecurringTransactionFactory.create(
                            id = 2,
                            startDate = LocalDate.of(2024, 2, 1),
                            endDate = LocalDate.of(2024, 12, 31),
                            nextDueDate = LocalDate.of(2024, 2, 1),
                        ),
                    )

                every { recurringTransactionRepository.findAll() } returns transactions

                val result = service.getAllRecurringTransactions()

                Then("should return all recurring transactions") {
                    result.size shouldBe 2
                }

                Then("should call repository findAll method") {
                    verify { recurringTransactionRepository.findAll() }
                }
            }
        }

        Given("a request to get recurring transactions by type") {
            And("filtering by expense type") {
                When("getting recurring transactions by type") {
                    val expenseTransactions =
                        listOf(
                            RecurringTransactionFactory.create(
                                id = 1,
                                type = WalletTransactionType.EXPENSE,
                                startDate = LocalDate.of(2024, 1, 1),
                                endDate = LocalDate.of(2024, 12, 31),
                                nextDueDate = LocalDate.of(2024, 1, 1),
                            ),
                        )

                    every { recurringTransactionRepository.findByType(WalletTransactionType.EXPENSE) } returns expenseTransactions

                    val result = service.getAllRecurringTransactionsByType(WalletTransactionType.EXPENSE)

                    Then("should return only expense transactions") {
                        result.size shouldBe 1
                        result.all { it.type == WalletTransactionType.EXPENSE } shouldBe true
                    }
                }
            }

            And("filtering by income type") {
                When("getting recurring transactions by type") {
                    val incomeTransactions =
                        listOf(
                            RecurringTransactionFactory.create(
                                id = 2,
                                type = WalletTransactionType.INCOME,
                                startDate = LocalDate.of(2024, 1, 1),
                                endDate = LocalDate.of(2024, 12, 31),
                                nextDueDate = LocalDate.of(2024, 1, 1),
                            ),
                        )

                    every { recurringTransactionRepository.findByType(WalletTransactionType.INCOME) } returns incomeTransactions

                    val result = service.getAllRecurringTransactionsByType(WalletTransactionType.INCOME)

                    Then("should return only income transactions") {
                        result.size shouldBe 1
                        result.all { it.type == WalletTransactionType.INCOME } shouldBe true
                    }
                }
            }
        }

        Given("a recurring transaction with defined end date") {
            And("the end date is in the future") {
                When("calculating expected remaining amount") {
                    val today = LocalDate.now()
                    val recurringTransaction =
                        RecurringTransactionFactory.create(
                            id = 1,
                            startDate = today.minusMonths(1),
                            endDate = today.plusMonths(3),
                            nextDueDate = today.plusDays(1),
                            frequency = RecurringTransactionFrequency.MONTHLY,
                            amount = BigDecimal("100.00"),
                        )

                    every { recurringTransactionRepository.findById(1) } returns Optional.of(recurringTransaction)

                    val result = service.getExpectedRemainingAmountFromRecurringTransaction(1)

                    Then("should return expected remaining amount") {
                        result shouldBe BigDecimal("300.00")
                    }
                }
            }

            And("the end date is in the past") {
                When("calculating expected remaining amount") {
                    val today = LocalDate.now()
                    val recurringTransaction =
                        RecurringTransactionFactory.create(
                            id = 1,
                            startDate = today.minusMonths(6),
                            endDate = today.minusMonths(1),
                            nextDueDate = today.minusMonths(1),
                            frequency = RecurringTransactionFrequency.MONTHLY,
                            amount = BigDecimal("100.00"),
                        )

                    every { recurringTransactionRepository.findById(1) } returns Optional.of(recurringTransaction)

                    val result = service.getExpectedRemainingAmountFromRecurringTransaction(1)

                    Then("should return zero") {
                        result shouldBe BigDecimal.ZERO
                    }
                }
            }
        }

        Given("a recurring transaction with open-ended date") {
            When("calculating expected remaining amount") {
                val today = LocalDate.now()
                val recurringTransaction =
                    RecurringTransactionFactory.create(
                        id = 1,
                        startDate = today.minusMonths(1),
                        endDate = Constants.RECURRING_TRANSACTION_DEFAULT_END_DATE,
                        nextDueDate = today.plusDays(1),
                        frequency = RecurringTransactionFrequency.MONTHLY,
                        amount = BigDecimal("100.00"),
                    )

                every { recurringTransactionRepository.findById(1) } returns Optional.of(recurringTransaction)

                val result = service.getExpectedRemainingAmountFromRecurringTransaction(1)

                Then("should return null") {
                    result shouldBe null
                }
            }
        }

        Given("a request to calculate last transaction date") {
            And("frequency is monthly") {
                When("calculating last transaction date") {
                    val startDate = LocalDate.of(2024, 1, 1)
                    val endDate = LocalDate.of(2024, 6, 15)
                    val frequency = RecurringTransactionFrequency.MONTHLY

                    val result = service.getLastTransactionDate(startDate, endDate, frequency)

                    Then("should return the last transaction date before end date") {
                        result shouldBe LocalDate.of(2024, 6, 1)
                    }
                }
            }

            And("frequency is weekly") {
                When("calculating last transaction date") {
                    val startDate = LocalDate.of(2024, 1, 1)
                    val endDate = LocalDate.of(2024, 1, 25)
                    val frequency = RecurringTransactionFrequency.WEEKLY

                    val result = service.getLastTransactionDate(startDate, endDate, frequency)

                    Then("should return the last transaction date before end date") {
                        result shouldBe LocalDate.of(2024, 1, 22)
                    }
                }
            }

            And("frequency is yearly") {
                When("calculating last transaction date") {
                    val startDate = LocalDate.of(2020, 1, 1)
                    val endDate = LocalDate.of(2024, 6, 15)
                    val frequency = RecurringTransactionFrequency.YEARLY

                    val result = service.getLastTransactionDate(startDate, endDate, frequency)

                    Then("should return the last transaction date before end date") {
                        result shouldBe LocalDate.of(2024, 1, 1)
                    }
                }
            }

            And("end date equals start date") {
                When("calculating last transaction date") {
                    val startDate = LocalDate.of(2024, 1, 1)
                    val endDate = LocalDate.of(2024, 1, 1)
                    val frequency = RecurringTransactionFrequency.MONTHLY

                    val result = service.getLastTransactionDate(startDate, endDate, frequency)

                    Then("should return start date") {
                        result shouldBe startDate
                    }
                }
            }
        }

        Given("recurring transactions with different frequencies") {
            And("frequency is daily") {
                When("generating transactions") {
                    val startMonth = YearMonth.of(2024, 1)
                    val endMonth = YearMonth.of(2024, 1)
                    val recurringTransaction =
                        RecurringTransactionFactory.create(
                            id = 1,
                            startDate = LocalDate.of(2024, 1, 1),
                            endDate = LocalDate.of(2024, 12, 31),
                            nextDueDate = LocalDate.of(2024, 1, 1),
                            frequency = RecurringTransactionFrequency.DAILY,
                            amount = BigDecimal("10.00"),
                            status = RecurringTransactionStatus.ACTIVE,
                        )

                    every { recurringTransactionRepository.findByStatus(RecurringTransactionStatus.ACTIVE) } returns
                        listOf(recurringTransaction)

                    val result = service.getFutureRecurringTransactionsByMonthForAnalysis(startMonth, endMonth)

                    Then("should generate daily transactions") {
                        result.size shouldBe 31
                    }
                }
            }

            And("frequency is weekly") {
                When("generating transactions") {
                    val startMonth = YearMonth.of(2024, 1)
                    val endMonth = YearMonth.of(2024, 2)
                    val recurringTransaction =
                        RecurringTransactionFactory.create(
                            id = 1,
                            startDate = LocalDate.of(2024, 1, 1),
                            endDate = LocalDate.of(2024, 12, 31),
                            nextDueDate = LocalDate.of(2024, 1, 1),
                            frequency = RecurringTransactionFrequency.WEEKLY,
                            amount = BigDecimal("50.00"),
                            status = RecurringTransactionStatus.ACTIVE,
                        )

                    every { recurringTransactionRepository.findByStatus(RecurringTransactionStatus.ACTIVE) } returns
                        listOf(recurringTransaction)

                    val result = service.getFutureRecurringTransactionsByMonthForAnalysis(startMonth, endMonth)

                    Then("should generate weekly transactions") {
                        result.size shouldBe 9
                    }
                }
            }
        }

        Given("a recurring transaction that ends within processing period") {
            When("processing recurring transactions") {
                val today = LocalDate.now()
                val recurringTransaction =
                    RecurringTransactionFactory.create(
                        id = 1,
                        startDate = today.minusMonths(2),
                        endDate = today.minusDays(10),
                        nextDueDate = today.minusDays(15),
                        frequency = RecurringTransactionFrequency.MONTHLY,
                        amount = BigDecimal("500.00"),
                        status = RecurringTransactionStatus.ACTIVE,
                    )

                every { recurringTransactionRepository.findByStatus(RecurringTransactionStatus.ACTIVE) } returns
                    listOf(recurringTransaction)
                every { walletService.createWalletTransaction(any()) } returns 1

                service.processRecurringTransactions()

                Then("should mark transaction as inactive") {
                    recurringTransaction.status shouldBe RecurringTransactionStatus.INACTIVE
                }
            }
        }
    })
