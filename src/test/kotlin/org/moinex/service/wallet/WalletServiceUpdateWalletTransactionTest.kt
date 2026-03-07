package org.moinex.service.wallet

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import jakarta.persistence.EntityNotFoundException
import org.moinex.factory.CategoryFactory
import org.moinex.factory.WalletFactory
import org.moinex.factory.WalletTransactionFactory
import org.moinex.model.enums.WalletTransactionStatus
import org.moinex.model.enums.WalletTransactionType
import org.moinex.repository.wallettransaction.TransferRepository
import org.moinex.repository.wallettransaction.WalletRepository
import org.moinex.repository.wallettransaction.WalletTransactionRepository
import org.moinex.repository.wallettransaction.WalletTypeRepository
import org.moinex.service.WalletService
import java.math.BigDecimal
import java.time.LocalDateTime
import java.util.Optional

class WalletServiceUpdateWalletTransactionTest :
    BehaviorSpec({
        val walletRepository = mockk<WalletRepository>()
        val transfersRepository = mockk<TransferRepository>()
        val walletTransactionRepository = mockk<WalletTransactionRepository>()
        val walletTypeRepository = mockk<WalletTypeRepository>()

        val service =
            WalletService(
                walletRepository,
                transfersRepository,
                walletTransactionRepository,
                walletTypeRepository,
            )

        afterContainer { clearAllMocks(answers = true) }

        Given("a confirmed expense transaction with no changes") {
            val wallet = WalletFactory.create(id = 1, balance = BigDecimal("1000.00"))
            val category = CategoryFactory.create(id = 1)
            val transactionFromDb =
                WalletTransactionFactory.create(
                    id = 1,
                    wallet = wallet,
                    category = category,
                    type = WalletTransactionType.EXPENSE,
                    amount = BigDecimal("100.00"),
                    status = WalletTransactionStatus.CONFIRMED,
                )
            val updatedTransaction =
                WalletTransactionFactory.create(
                    id = 1,
                    wallet = wallet,
                    category = category,
                    type = WalletTransactionType.EXPENSE,
                    amount = BigDecimal("100.00"),
                    status = WalletTransactionStatus.CONFIRMED,
                )

            When("updating with same values") {
                every { walletTransactionRepository.findById(1) } returns Optional.of(transactionFromDb)
                every { walletRepository.getAllocatedBalanceByMasterWallet(1) } returns BigDecimal.ZERO

                service.updateWalletTransaction(updatedTransaction)

                Then("should not modify wallet balance") {
                    wallet.balance shouldBe BigDecimal("1000.00")
                }

                Then("should update transaction attributes") {
                    transactionFromDb.type shouldBe WalletTransactionType.EXPENSE
                    transactionFromDb.amount shouldBe BigDecimal("100.00")
                    transactionFromDb.status shouldBe WalletTransactionStatus.CONFIRMED
                }
            }
        }

        Given("a confirmed expense transaction changing wallet") {
            val oldWallet = WalletFactory.create(id = 1, balance = BigDecimal("1000.00"))
            val newWallet = WalletFactory.create(id = 2, balance = BigDecimal("500.00"))
            val category = CategoryFactory.create(id = 1)
            val transactionFromDb =
                WalletTransactionFactory.create(
                    id = 1,
                    wallet = oldWallet,
                    category = category,
                    type = WalletTransactionType.EXPENSE,
                    amount = BigDecimal("100.00"),
                    status = WalletTransactionStatus.CONFIRMED,
                )
            val updatedTransaction =
                WalletTransactionFactory.create(
                    id = 1,
                    wallet = newWallet,
                    category = category,
                    type = WalletTransactionType.EXPENSE,
                    amount = BigDecimal("100.00"),
                    status = WalletTransactionStatus.CONFIRMED,
                )

            When("changing wallet for confirmed expense") {
                every { walletTransactionRepository.findById(1) } returns Optional.of(transactionFromDb)
                every { walletRepository.getAllocatedBalanceByMasterWallet(1) } returns BigDecimal.ZERO
                every { walletRepository.getAllocatedBalanceByMasterWallet(2) } returns BigDecimal.ZERO

                service.updateWalletTransaction(updatedTransaction)

                Then("should increment old wallet balance") {
                    oldWallet.balance shouldBe BigDecimal("1100.00")
                }

                Then("should decrement new wallet balance") {
                    newWallet.balance shouldBe BigDecimal("400.00")
                }

                Then("should update transaction wallet") {
                    transactionFromDb.wallet.id shouldBe 2
                }
            }
        }

        Given("a confirmed income transaction changing wallet") {
            val oldWallet = WalletFactory.create(id = 3, balance = BigDecimal("1000.00"))
            val newWallet = WalletFactory.create(id = 4, balance = BigDecimal("500.00"))
            val category = CategoryFactory.create(id = 1)
            val transactionFromDb =
                WalletTransactionFactory.create(
                    id = 2,
                    wallet = oldWallet,
                    category = category,
                    type = WalletTransactionType.INCOME,
                    amount = BigDecimal("200.00"),
                    status = WalletTransactionStatus.CONFIRMED,
                )
            val updatedTransaction =
                WalletTransactionFactory.create(
                    id = 2,
                    wallet = newWallet,
                    category = category,
                    type = WalletTransactionType.INCOME,
                    amount = BigDecimal("200.00"),
                    status = WalletTransactionStatus.CONFIRMED,
                )

            When("changing wallet for confirmed income") {
                every { walletTransactionRepository.findById(2) } returns Optional.of(transactionFromDb)
                every { walletRepository.getAllocatedBalanceByMasterWallet(3) } returns BigDecimal.ZERO
                every { walletRepository.getAllocatedBalanceByMasterWallet(4) } returns BigDecimal.ZERO

                service.updateWalletTransaction(updatedTransaction)

                Then("should decrement old wallet balance") {
                    oldWallet.balance shouldBe BigDecimal("800.00")
                }

                Then("should increment new wallet balance") {
                    newWallet.balance shouldBe BigDecimal("700.00")
                }

                Then("should update transaction wallet") {
                    transactionFromDb.wallet.id shouldBe 4
                }
            }
        }

        Given("a pending transaction changing wallet") {
            val oldWallet = WalletFactory.create(id = 5, balance = BigDecimal("1000.00"))
            val newWallet = WalletFactory.create(id = 6, balance = BigDecimal("500.00"))
            val category = CategoryFactory.create(id = 1)
            val transactionFromDb =
                WalletTransactionFactory.create(
                    id = 3,
                    wallet = oldWallet,
                    category = category,
                    type = WalletTransactionType.EXPENSE,
                    amount = BigDecimal("100.00"),
                    status = WalletTransactionStatus.PENDING,
                )
            val updatedTransaction =
                WalletTransactionFactory.create(
                    id = 3,
                    wallet = newWallet,
                    category = category,
                    type = WalletTransactionType.EXPENSE,
                    amount = BigDecimal("100.00"),
                    status = WalletTransactionStatus.PENDING,
                )

            When("changing wallet for pending transaction") {
                every { walletTransactionRepository.findById(3) } returns Optional.of(transactionFromDb)

                service.updateWalletTransaction(updatedTransaction)

                Then("should not modify wallet balances") {
                    oldWallet.balance shouldBe BigDecimal("1000.00")
                    newWallet.balance shouldBe BigDecimal("500.00")
                }

                Then("should update transaction wallet") {
                    transactionFromDb.wallet.id shouldBe 6
                }
            }
        }

        Given("a confirmed expense transaction changing type to income") {
            val wallet = WalletFactory.create(id = 7, balance = BigDecimal("1000.00"))
            val category = CategoryFactory.create(id = 1)
            val transactionFromDb =
                WalletTransactionFactory.create(
                    id = 4,
                    wallet = wallet,
                    category = category,
                    type = WalletTransactionType.EXPENSE,
                    amount = BigDecimal("100.00"),
                    status = WalletTransactionStatus.CONFIRMED,
                )
            val updatedTransaction =
                WalletTransactionFactory.create(
                    id = 4,
                    wallet = wallet,
                    category = category,
                    type = WalletTransactionType.INCOME,
                    amount = BigDecimal("100.00"),
                    status = WalletTransactionStatus.CONFIRMED,
                )

            When("changing type from expense to income") {
                every { walletTransactionRepository.findById(4) } returns Optional.of(transactionFromDb)
                every { walletRepository.getAllocatedBalanceByMasterWallet(7) } returns BigDecimal.ZERO

                service.updateWalletTransaction(updatedTransaction)

                Then("should adjust wallet balance by double the amount") {
                    wallet.balance shouldBe BigDecimal("1200.00")
                }

                Then("should update transaction type") {
                    transactionFromDb.type shouldBe WalletTransactionType.INCOME
                }
            }
        }

        Given("a confirmed income transaction changing type to expense") {
            val wallet = WalletFactory.create(id = 8, balance = BigDecimal("1000.00"))
            val category = CategoryFactory.create(id = 1)
            val transactionFromDb =
                WalletTransactionFactory.create(
                    id = 5,
                    wallet = wallet,
                    category = category,
                    type = WalletTransactionType.INCOME,
                    amount = BigDecimal("100.00"),
                    status = WalletTransactionStatus.CONFIRMED,
                )
            val updatedTransaction =
                WalletTransactionFactory.create(
                    id = 5,
                    wallet = wallet,
                    category = category,
                    type = WalletTransactionType.EXPENSE,
                    amount = BigDecimal("100.00"),
                    status = WalletTransactionStatus.CONFIRMED,
                )

            When("changing type from income to expense") {
                every { walletTransactionRepository.findById(5) } returns Optional.of(transactionFromDb)
                every { walletRepository.getAllocatedBalanceByMasterWallet(8) } returns BigDecimal.ZERO

                service.updateWalletTransaction(updatedTransaction)

                Then("should adjust wallet balance by double the amount") {
                    wallet.balance shouldBe BigDecimal("800.00")
                }

                Then("should update transaction type") {
                    transactionFromDb.type shouldBe WalletTransactionType.EXPENSE
                }
            }
        }

        Given("a pending transaction changing type") {
            val wallet = WalletFactory.create(id = 9, balance = BigDecimal("1000.00"))
            val category = CategoryFactory.create(id = 1)
            val transactionFromDb =
                WalletTransactionFactory.create(
                    id = 6,
                    wallet = wallet,
                    category = category,
                    type = WalletTransactionType.EXPENSE,
                    amount = BigDecimal("100.00"),
                    status = WalletTransactionStatus.PENDING,
                )
            val updatedTransaction =
                WalletTransactionFactory.create(
                    id = 6,
                    wallet = wallet,
                    category = category,
                    type = WalletTransactionType.INCOME,
                    amount = BigDecimal("100.00"),
                    status = WalletTransactionStatus.PENDING,
                )

            When("changing type for pending transaction") {
                every { walletTransactionRepository.findById(6) } returns Optional.of(transactionFromDb)

                service.updateWalletTransaction(updatedTransaction)

                Then("should not modify wallet balance") {
                    wallet.balance shouldBe BigDecimal("1000.00")
                }

                Then("should update transaction type") {
                    transactionFromDb.type shouldBe WalletTransactionType.INCOME
                }
            }
        }

        Given("a confirmed expense transaction with amount increase") {
            val wallet = WalletFactory.create(id = 10, balance = BigDecimal("1000.00"))
            val category = CategoryFactory.create(id = 1)
            val transactionFromDb =
                WalletTransactionFactory.create(
                    id = 7,
                    wallet = wallet,
                    category = category,
                    type = WalletTransactionType.EXPENSE,
                    amount = BigDecimal("100.00"),
                    status = WalletTransactionStatus.CONFIRMED,
                )
            val updatedTransaction =
                WalletTransactionFactory.create(
                    id = 7,
                    wallet = wallet,
                    category = category,
                    type = WalletTransactionType.EXPENSE,
                    amount = BigDecimal("150.00"),
                    status = WalletTransactionStatus.CONFIRMED,
                )

            When("increasing expense amount") {
                every { walletTransactionRepository.findById(7) } returns Optional.of(transactionFromDb)
                every { walletRepository.getAllocatedBalanceByMasterWallet(10) } returns BigDecimal.ZERO

                service.updateWalletTransaction(updatedTransaction)

                Then("should decrement wallet balance by difference") {
                    wallet.balance shouldBe BigDecimal("950.00")
                }

                Then("should update transaction amount") {
                    transactionFromDb.amount shouldBe BigDecimal("150.00")
                }
            }
        }

        Given("a confirmed expense transaction with amount decrease") {
            val wallet = WalletFactory.create(id = 11, balance = BigDecimal("1000.00"))
            val category = CategoryFactory.create(id = 1)
            val transactionFromDb =
                WalletTransactionFactory.create(
                    id = 8,
                    wallet = wallet,
                    category = category,
                    type = WalletTransactionType.EXPENSE,
                    amount = BigDecimal("100.00"),
                    status = WalletTransactionStatus.CONFIRMED,
                )
            val updatedTransaction =
                WalletTransactionFactory.create(
                    id = 8,
                    wallet = wallet,
                    category = category,
                    type = WalletTransactionType.EXPENSE,
                    amount = BigDecimal("50.00"),
                    status = WalletTransactionStatus.CONFIRMED,
                )

            When("decreasing expense amount") {
                every { walletTransactionRepository.findById(8) } returns Optional.of(transactionFromDb)
                every { walletRepository.getAllocatedBalanceByMasterWallet(11) } returns BigDecimal.ZERO

                service.updateWalletTransaction(updatedTransaction)

                Then("should increment wallet balance by difference") {
                    wallet.balance shouldBe BigDecimal("1050.00")
                }

                Then("should update transaction amount") {
                    transactionFromDb.amount shouldBe BigDecimal("50.00")
                }
            }
        }

        Given("a confirmed income transaction with amount increase") {
            val wallet = WalletFactory.create(id = 12, balance = BigDecimal("1000.00"))
            val category = CategoryFactory.create(id = 1)
            val transactionFromDb =
                WalletTransactionFactory.create(
                    id = 9,
                    wallet = wallet,
                    category = category,
                    type = WalletTransactionType.INCOME,
                    amount = BigDecimal("100.00"),
                    status = WalletTransactionStatus.CONFIRMED,
                )
            val updatedTransaction =
                WalletTransactionFactory.create(
                    id = 9,
                    wallet = wallet,
                    category = category,
                    type = WalletTransactionType.INCOME,
                    amount = BigDecimal("150.00"),
                    status = WalletTransactionStatus.CONFIRMED,
                )

            When("increasing income amount") {
                every { walletTransactionRepository.findById(9) } returns Optional.of(transactionFromDb)
                every { walletRepository.getAllocatedBalanceByMasterWallet(12) } returns BigDecimal.ZERO

                service.updateWalletTransaction(updatedTransaction)

                Then("should increment wallet balance by difference") {
                    wallet.balance shouldBe BigDecimal("1050.00")
                }

                Then("should update transaction amount") {
                    transactionFromDb.amount shouldBe BigDecimal("150.00")
                }
            }
        }

        Given("a confirmed income transaction with amount decrease") {
            val wallet = WalletFactory.create(id = 13, balance = BigDecimal("1000.00"))
            val category = CategoryFactory.create(id = 1)
            val transactionFromDb =
                WalletTransactionFactory.create(
                    id = 10,
                    wallet = wallet,
                    category = category,
                    type = WalletTransactionType.INCOME,
                    amount = BigDecimal("100.00"),
                    status = WalletTransactionStatus.CONFIRMED,
                )
            val updatedTransaction =
                WalletTransactionFactory.create(
                    id = 10,
                    wallet = wallet,
                    category = category,
                    type = WalletTransactionType.INCOME,
                    amount = BigDecimal("50.00"),
                    status = WalletTransactionStatus.CONFIRMED,
                )

            When("decreasing income amount") {
                every { walletTransactionRepository.findById(10) } returns Optional.of(transactionFromDb)
                every { walletRepository.getAllocatedBalanceByMasterWallet(13) } returns BigDecimal.ZERO

                service.updateWalletTransaction(updatedTransaction)

                Then("should decrement wallet balance by difference") {
                    wallet.balance shouldBe BigDecimal("950.00")
                }

                Then("should update transaction amount") {
                    transactionFromDb.amount shouldBe BigDecimal("50.00")
                }
            }
        }

        Given("a pending transaction with amount change") {
            val wallet = WalletFactory.create(id = 14, balance = BigDecimal("1000.00"))
            val category = CategoryFactory.create(id = 1)
            val transactionFromDb =
                WalletTransactionFactory.create(
                    id = 11,
                    wallet = wallet,
                    category = category,
                    type = WalletTransactionType.EXPENSE,
                    amount = BigDecimal("100.00"),
                    status = WalletTransactionStatus.PENDING,
                )
            val updatedTransaction =
                WalletTransactionFactory.create(
                    id = 11,
                    wallet = wallet,
                    category = category,
                    type = WalletTransactionType.EXPENSE,
                    amount = BigDecimal("200.00"),
                    status = WalletTransactionStatus.PENDING,
                )

            When("changing amount for pending transaction") {
                every { walletTransactionRepository.findById(11) } returns Optional.of(transactionFromDb)

                service.updateWalletTransaction(updatedTransaction)

                Then("should not modify wallet balance") {
                    wallet.balance shouldBe BigDecimal("1000.00")
                }

                Then("should update transaction amount") {
                    transactionFromDb.amount shouldBe BigDecimal("200.00")
                }
            }
        }

        Given("a confirmed expense transaction changing status to pending") {
            val wallet = WalletFactory.create(id = 15, balance = BigDecimal("900.00"))
            val category = CategoryFactory.create(id = 1)
            val transactionFromDb =
                WalletTransactionFactory.create(
                    id = 12,
                    wallet = wallet,
                    category = category,
                    type = WalletTransactionType.EXPENSE,
                    amount = BigDecimal("100.00"),
                    status = WalletTransactionStatus.CONFIRMED,
                )
            val updatedTransaction =
                WalletTransactionFactory.create(
                    id = 12,
                    wallet = wallet,
                    category = category,
                    type = WalletTransactionType.EXPENSE,
                    amount = BigDecimal("100.00"),
                    status = WalletTransactionStatus.PENDING,
                )

            When("changing confirmed expense to pending") {
                every { walletTransactionRepository.findById(12) } returns Optional.of(transactionFromDb)
                every { walletRepository.getAllocatedBalanceByMasterWallet(15) } returns BigDecimal.ZERO

                service.updateWalletTransaction(updatedTransaction)

                Then("should increment wallet balance") {
                    wallet.balance shouldBe BigDecimal("1000.00")
                }

                Then("should update transaction status") {
                    transactionFromDb.status shouldBe WalletTransactionStatus.PENDING
                }
            }
        }

        Given("a confirmed income transaction changing status to pending") {
            val wallet = WalletFactory.create(id = 16, balance = BigDecimal("1100.00"))
            val category = CategoryFactory.create(id = 1)
            val transactionFromDb =
                WalletTransactionFactory.create(
                    id = 13,
                    wallet = wallet,
                    category = category,
                    type = WalletTransactionType.INCOME,
                    amount = BigDecimal("100.00"),
                    status = WalletTransactionStatus.CONFIRMED,
                )
            val updatedTransaction =
                WalletTransactionFactory.create(
                    id = 13,
                    wallet = wallet,
                    category = category,
                    type = WalletTransactionType.INCOME,
                    amount = BigDecimal("100.00"),
                    status = WalletTransactionStatus.PENDING,
                )

            When("changing confirmed income to pending") {
                every { walletTransactionRepository.findById(13) } returns Optional.of(transactionFromDb)
                every { walletRepository.getAllocatedBalanceByMasterWallet(16) } returns BigDecimal.ZERO

                service.updateWalletTransaction(updatedTransaction)

                Then("should decrement wallet balance") {
                    wallet.balance shouldBe BigDecimal("1000.00")
                }

                Then("should update transaction status") {
                    transactionFromDb.status shouldBe WalletTransactionStatus.PENDING
                }
            }
        }

        Given("a pending expense transaction changing status to confirmed") {
            val wallet = WalletFactory.create(id = 17, balance = BigDecimal("1000.00"))
            val category = CategoryFactory.create(id = 1)
            val transactionFromDb =
                WalletTransactionFactory.create(
                    id = 14,
                    wallet = wallet,
                    category = category,
                    type = WalletTransactionType.EXPENSE,
                    amount = BigDecimal("100.00"),
                    status = WalletTransactionStatus.PENDING,
                )
            val updatedTransaction =
                WalletTransactionFactory.create(
                    id = 14,
                    wallet = wallet,
                    category = category,
                    type = WalletTransactionType.EXPENSE,
                    amount = BigDecimal("100.00"),
                    status = WalletTransactionStatus.CONFIRMED,
                )

            When("changing pending expense to confirmed") {
                every { walletTransactionRepository.findById(14) } returns Optional.of(transactionFromDb)
                every { walletRepository.getAllocatedBalanceByMasterWallet(17) } returns BigDecimal.ZERO

                service.updateWalletTransaction(updatedTransaction)

                Then("should decrement wallet balance") {
                    wallet.balance shouldBe BigDecimal("900.00")
                }

                Then("should update transaction status") {
                    transactionFromDb.status shouldBe WalletTransactionStatus.CONFIRMED
                }
            }
        }

        Given("a pending income transaction changing status to confirmed") {
            val wallet = WalletFactory.create(id = 18, balance = BigDecimal("1000.00"))
            val category = CategoryFactory.create(id = 1)
            val transactionFromDb =
                WalletTransactionFactory.create(
                    id = 15,
                    wallet = wallet,
                    category = category,
                    type = WalletTransactionType.INCOME,
                    amount = BigDecimal("100.00"),
                    status = WalletTransactionStatus.PENDING,
                )
            val updatedTransaction =
                WalletTransactionFactory.create(
                    id = 15,
                    wallet = wallet,
                    category = category,
                    type = WalletTransactionType.INCOME,
                    amount = BigDecimal("100.00"),
                    status = WalletTransactionStatus.CONFIRMED,
                )

            When("changing pending income to confirmed") {
                every { walletTransactionRepository.findById(15) } returns Optional.of(transactionFromDb)
                every { walletRepository.getAllocatedBalanceByMasterWallet(18) } returns BigDecimal.ZERO

                service.updateWalletTransaction(updatedTransaction)

                Then("should increment wallet balance") {
                    wallet.balance shouldBe BigDecimal("1100.00")
                }

                Then("should update transaction status") {
                    transactionFromDb.status shouldBe WalletTransactionStatus.CONFIRMED
                }
            }
        }

        Given("a transaction updating date and description") {
            val wallet = WalletFactory.create(id = 19, balance = BigDecimal("1000.00"))
            val category = CategoryFactory.create(id = 1)
            val oldDate = LocalDateTime.of(2024, 1, 1, 10, 0, 0)
            val newDate = LocalDateTime.of(2024, 2, 1, 15, 30, 0)
            val transactionFromDb =
                WalletTransactionFactory.create(
                    id = 16,
                    wallet = wallet,
                    category = category,
                    type = WalletTransactionType.EXPENSE,
                    amount = BigDecimal("100.00"),
                    status = WalletTransactionStatus.PENDING,
                    date = oldDate,
                    description = "Old description",
                )
            val updatedTransaction =
                WalletTransactionFactory.create(
                    id = 16,
                    wallet = wallet,
                    category = category,
                    type = WalletTransactionType.EXPENSE,
                    amount = BigDecimal("100.00"),
                    status = WalletTransactionStatus.PENDING,
                    date = newDate,
                    description = "New description",
                )

            When("updating date and description") {
                every { walletTransactionRepository.findById(16) } returns Optional.of(transactionFromDb)

                service.updateWalletTransaction(updatedTransaction)

                Then("should update date") {
                    transactionFromDb.date shouldBe newDate
                }

                Then("should update description") {
                    transactionFromDb.description shouldBe "New description"
                }

                Then("should not modify wallet balance") {
                    wallet.balance shouldBe BigDecimal("1000.00")
                }
            }
        }

        Given("a transaction updating category") {
            val wallet = WalletFactory.create(id = 20, balance = BigDecimal("1000.00"))
            val oldCategory = CategoryFactory.create(id = 1, name = "Old Category")
            val newCategory = CategoryFactory.create(id = 2, name = "New Category")
            val transactionFromDb =
                WalletTransactionFactory.create(
                    id = 17,
                    wallet = wallet,
                    category = oldCategory,
                    type = WalletTransactionType.EXPENSE,
                    amount = BigDecimal("100.00"),
                    status = WalletTransactionStatus.PENDING,
                )
            val updatedTransaction =
                WalletTransactionFactory.create(
                    id = 17,
                    wallet = wallet,
                    category = newCategory,
                    type = WalletTransactionType.EXPENSE,
                    amount = BigDecimal("100.00"),
                    status = WalletTransactionStatus.PENDING,
                )

            When("updating category") {
                every { walletTransactionRepository.findById(17) } returns Optional.of(transactionFromDb)

                service.updateWalletTransaction(updatedTransaction)

                Then("should update category") {
                    transactionFromDb.category.id shouldBe 2
                }

                Then("should not modify wallet balance") {
                    wallet.balance shouldBe BigDecimal("1000.00")
                }
            }
        }

        Given("a transaction updating includeInAnalysis flag") {
            val wallet = WalletFactory.create(id = 21, balance = BigDecimal("1000.00"))
            val category = CategoryFactory.create(id = 1)
            val transactionFromDb =
                WalletTransactionFactory.create(
                    id = 18,
                    wallet = wallet,
                    category = category,
                    type = WalletTransactionType.EXPENSE,
                    amount = BigDecimal("100.00"),
                    status = WalletTransactionStatus.CONFIRMED,
                    includeInAnalysis = true,
                )
            val updatedTransaction =
                WalletTransactionFactory.create(
                    id = 18,
                    wallet = wallet,
                    category = category,
                    type = WalletTransactionType.EXPENSE,
                    amount = BigDecimal("100.00"),
                    status = WalletTransactionStatus.CONFIRMED,
                    includeInAnalysis = false,
                )

            When("updating includeInAnalysis flag") {
                every { walletTransactionRepository.findById(18) } returns Optional.of(transactionFromDb)
                every { walletRepository.getAllocatedBalanceByMasterWallet(21) } returns BigDecimal.ZERO

                service.updateWalletTransaction(updatedTransaction)

                Then("should update includeInAnalysis flag") {
                    transactionFromDb.includeInAnalysis shouldBe false
                }

                Then("should not modify wallet balance") {
                    wallet.balance shouldBe BigDecimal("1000.00")
                }
            }
        }

        Given("a non-existent transaction") {
            val wallet = WalletFactory.create(id = 22)
            val category = CategoryFactory.create(id = 1)
            val updatedTransaction =
                WalletTransactionFactory.create(
                    id = 999,
                    wallet = wallet,
                    category = category,
                    type = WalletTransactionType.EXPENSE,
                    amount = BigDecimal("100.00"),
                    status = WalletTransactionStatus.CONFIRMED,
                )

            When("updating non-existent transaction") {
                every { walletTransactionRepository.findById(999) } returns Optional.empty()

                Then("should throw EntityNotFoundException") {
                    shouldThrow<EntityNotFoundException> {
                        service.updateWalletTransaction(updatedTransaction)
                    }
                }
            }
        }

        Given("a confirmed transaction with multiple changes") {
            val oldWallet = WalletFactory.create(id = 23, balance = BigDecimal("1000.00"))
            val newWallet = WalletFactory.create(id = 24, balance = BigDecimal("500.00"))
            val category = CategoryFactory.create(id = 1)
            val transactionFromDb =
                WalletTransactionFactory.create(
                    id = 19,
                    wallet = oldWallet,
                    category = category,
                    type = WalletTransactionType.EXPENSE,
                    amount = BigDecimal("100.00"),
                    status = WalletTransactionStatus.CONFIRMED,
                )
            val updatedTransaction =
                WalletTransactionFactory.create(
                    id = 19,
                    wallet = newWallet,
                    category = category,
                    type = WalletTransactionType.INCOME,
                    amount = BigDecimal("150.00"),
                    status = WalletTransactionStatus.PENDING,
                )

            When("updating wallet, type, amount and status") {
                every { walletTransactionRepository.findById(19) } returns Optional.of(transactionFromDb)
                every { walletRepository.getAllocatedBalanceByMasterWallet(23) } returns BigDecimal.ZERO
                every { walletRepository.getAllocatedBalanceByMasterWallet(24) } returns BigDecimal.ZERO

                service.updateWalletTransaction(updatedTransaction)

                Then("should update wallet") {
                    transactionFromDb.wallet.id shouldBe 24
                }

                Then("should update type") {
                    transactionFromDb.type shouldBe WalletTransactionType.INCOME
                }

                Then("should update amount") {
                    transactionFromDb.amount shouldBe BigDecimal("150.00")
                }

                Then("should update status") {
                    transactionFromDb.status shouldBe WalletTransactionStatus.PENDING
                }

                Then("should apply all balance changes") {
                    oldWallet.balance shouldBe BigDecimal("1100.00")
                    newWallet.balance shouldBe BigDecimal("500.00")
                }
            }
        }
    })
