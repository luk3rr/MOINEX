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

class WalletServiceCreateWalletTransactionTest :
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

        Given("a confirmed income transaction") {
            val wallet = WalletFactory.create(id = 1, balance = BigDecimal("1000.00"))
            val transaction =
                WalletTransactionFactory.create(
                    wallet = wallet,
                    type = WalletTransactionType.INCOME,
                    amount = BigDecimal("500.00"),
                    status = WalletTransactionStatus.CONFIRMED,
                )
            val savedTransaction =
                WalletTransactionFactory.create(
                    id = 1,
                    wallet = wallet,
                    type = WalletTransactionType.INCOME,
                    amount = BigDecimal("500.00"),
                    status = WalletTransactionStatus.CONFIRMED,
                )

            When("creating the transaction") {
                every { walletRepository.findById(1) } returns Optional.of(wallet)
                every { walletTransactionRepository.save(any()) } returns savedTransaction

                val result = service.createWalletTransaction(transaction)

                Then("should return the created transaction id") {
                    result shouldBe 1
                }

                Then("should increment wallet balance") {
                    wallet.balance shouldBe BigDecimal("1500.00")
                }

                Then("should call repository save method") {
                    verify { walletTransactionRepository.save(any()) }
                }
            }
        }

        Given("a confirmed expense transaction") {
            val wallet = WalletFactory.create(id = 2, balance = BigDecimal("1000.00"))
            val transaction =
                WalletTransactionFactory.create(
                    wallet = wallet,
                    type = WalletTransactionType.EXPENSE,
                    amount = BigDecimal("300.00"),
                    status = WalletTransactionStatus.CONFIRMED,
                )
            val savedTransaction =
                WalletTransactionFactory.create(
                    id = 2,
                    wallet = wallet,
                    type = WalletTransactionType.EXPENSE,
                    amount = BigDecimal("300.00"),
                    status = WalletTransactionStatus.CONFIRMED,
                )

            When("creating the transaction") {
                every { walletRepository.findById(2) } returns Optional.of(wallet)
                every { walletRepository.getAllocatedBalanceByMasterWallet(2) } returns BigDecimal.ZERO
                every { walletTransactionRepository.save(any()) } returns savedTransaction

                val result = service.createWalletTransaction(transaction)

                Then("should return the created transaction id") {
                    result shouldBe 2
                }

                Then("should decrement wallet balance") {
                    wallet.balance shouldBe BigDecimal("700.00")
                }

                Then("should call repository save method") {
                    verify { walletTransactionRepository.save(any()) }
                }
            }
        }

        Given("a pending income transaction") {
            val wallet = WalletFactory.create(id = 3, balance = BigDecimal("1000.00"))
            val transaction =
                WalletTransactionFactory.create(
                    wallet = wallet,
                    type = WalletTransactionType.INCOME,
                    amount = BigDecimal("200.00"),
                    status = WalletTransactionStatus.PENDING,
                )
            val savedTransaction =
                WalletTransactionFactory.create(
                    id = 3,
                    wallet = wallet,
                    type = WalletTransactionType.INCOME,
                    amount = BigDecimal("200.00"),
                    status = WalletTransactionStatus.PENDING,
                )

            When("creating the transaction") {
                every { walletRepository.findById(3) } returns Optional.of(wallet)
                every { walletTransactionRepository.save(any()) } returns savedTransaction

                val result = service.createWalletTransaction(transaction)

                Then("should return the created transaction id") {
                    result shouldBe 3
                }

                Then("should not modify wallet balance") {
                    wallet.balance shouldBe BigDecimal("1000.00")
                }

                Then("should call repository save method") {
                    verify { walletTransactionRepository.save(any()) }
                }
            }
        }

        Given("a pending expense transaction") {
            val wallet = WalletFactory.create(id = 4, balance = BigDecimal("1000.00"))
            val transaction =
                WalletTransactionFactory.create(
                    wallet = wallet,
                    type = WalletTransactionType.EXPENSE,
                    amount = BigDecimal("150.00"),
                    status = WalletTransactionStatus.PENDING,
                )
            val savedTransaction =
                WalletTransactionFactory.create(
                    id = 4,
                    wallet = wallet,
                    type = WalletTransactionType.EXPENSE,
                    amount = BigDecimal("150.00"),
                    status = WalletTransactionStatus.PENDING,
                )

            When("creating the transaction") {
                every { walletRepository.findById(4) } returns Optional.of(wallet)
                every { walletTransactionRepository.save(any()) } returns savedTransaction
                every { walletRepository.getAllocatedBalanceByMasterWallet(4) } returns BigDecimal.ZERO

                val result = service.createWalletTransaction(transaction)

                Then("should return the created transaction id") {
                    result shouldBe 4
                }

                Then("should not modify wallet balance") {
                    wallet.balance shouldBe BigDecimal("1000.00")
                }

                Then("should call repository save method") {
                    verify { walletTransactionRepository.save(any()) }
                }
            }
        }

        Given("a confirmed transaction with description") {
            val wallet = WalletFactory.create(id = 5, balance = BigDecimal("2000.00"))
            val transaction =
                WalletTransactionFactory.create(
                    wallet = wallet,
                    type = WalletTransactionType.EXPENSE,
                    amount = BigDecimal("50.00"),
                    status = WalletTransactionStatus.CONFIRMED,
                    description = "Grocery shopping",
                )
            val savedTransaction =
                WalletTransactionFactory.create(
                    id = 5,
                    wallet = wallet,
                    type = WalletTransactionType.EXPENSE,
                    amount = BigDecimal("50.00"),
                    status = WalletTransactionStatus.CONFIRMED,
                    description = "Grocery shopping",
                )

            When("creating the transaction") {
                every { walletRepository.findById(5) } returns Optional.of(wallet)
                every { walletRepository.getAllocatedBalanceByMasterWallet(5) } returns BigDecimal.ZERO
                every { walletTransactionRepository.save(any()) } returns savedTransaction

                val result = service.createWalletTransaction(transaction)

                Then("should return the created transaction id") {
                    result shouldBe 5
                }

                Then("should decrement wallet balance") {
                    wallet.balance shouldBe BigDecimal("1950.00")
                }
            }
        }

        Given("a confirmed transaction with large amount") {
            val wallet = WalletFactory.create(id = 6, balance = BigDecimal("10000.00"))
            val transaction =
                WalletTransactionFactory.create(
                    wallet = wallet,
                    type = WalletTransactionType.EXPENSE,
                    amount = BigDecimal("5000.00"),
                    status = WalletTransactionStatus.CONFIRMED,
                )
            val savedTransaction =
                WalletTransactionFactory.create(
                    id = 6,
                    wallet = wallet,
                    type = WalletTransactionType.EXPENSE,
                    amount = BigDecimal("5000.00"),
                    status = WalletTransactionStatus.CONFIRMED,
                )

            When("creating the transaction") {
                every { walletRepository.findById(6) } returns Optional.of(wallet)
                every { walletRepository.getAllocatedBalanceByMasterWallet(6) } returns BigDecimal.ZERO
                every { walletTransactionRepository.save(any()) } returns savedTransaction

                val result = service.createWalletTransaction(transaction)

                Then("should return the created transaction id") {
                    result shouldBe 6
                }

                Then("should decrement wallet balance correctly") {
                    wallet.balance shouldBe BigDecimal("5000.00")
                }
            }
        }

        Given("a confirmed transaction with small amount") {
            val wallet = WalletFactory.create(id = 7, balance = BigDecimal("100.00"))
            val transaction =
                WalletTransactionFactory.create(
                    wallet = wallet,
                    type = WalletTransactionType.INCOME,
                    amount = BigDecimal("0.01"),
                    status = WalletTransactionStatus.CONFIRMED,
                )
            val savedTransaction =
                WalletTransactionFactory.create(
                    id = 7,
                    wallet = wallet,
                    type = WalletTransactionType.INCOME,
                    amount = BigDecimal("0.01"),
                    status = WalletTransactionStatus.CONFIRMED,
                )

            When("creating the transaction") {
                every { walletRepository.findById(7) } returns Optional.of(wallet)
                every { walletTransactionRepository.save(any()) } returns savedTransaction

                val result = service.createWalletTransaction(transaction)

                Then("should return the created transaction id") {
                    result shouldBe 7
                }

                Then("should increment wallet balance correctly") {
                    wallet.balance shouldBe BigDecimal("100.01")
                }
            }
        }

        Given("a transaction for a non-existent wallet") {
            val wallet = WalletFactory.create(id = 999)
            val transaction =
                WalletTransactionFactory.create(
                    wallet = wallet,
                    type = WalletTransactionType.EXPENSE,
                    amount = BigDecimal("100.00"),
                    status = WalletTransactionStatus.CONFIRMED,
                )

            When("creating the transaction") {
                every { walletRepository.findById(999) } returns Optional.empty()

                Then("should throw EntityNotFoundException") {
                    shouldThrow<EntityNotFoundException> {
                        service.createWalletTransaction(transaction)
                    }
                }
            }
        }

        Given("multiple confirmed transactions") {
            val wallet = WalletFactory.create(id = 8, balance = BigDecimal("5000.00"))

            When("creating multiple transactions sequentially") {
                val transaction1 =
                    WalletTransactionFactory.create(
                        wallet = wallet,
                        type = WalletTransactionType.EXPENSE,
                        amount = BigDecimal("100.00"),
                        status = WalletTransactionStatus.CONFIRMED,
                    )
                val transaction2 =
                    WalletTransactionFactory.create(
                        wallet = wallet,
                        type = WalletTransactionType.INCOME,
                        amount = BigDecimal("200.00"),
                        status = WalletTransactionStatus.CONFIRMED,
                    )
                val transaction3 =
                    WalletTransactionFactory.create(
                        wallet = wallet,
                        type = WalletTransactionType.EXPENSE,
                        amount = BigDecimal("50.00"),
                        status = WalletTransactionStatus.CONFIRMED,
                    )

                val savedTransaction1 =
                    WalletTransactionFactory.create(
                        id = 8,
                        wallet = wallet,
                        type = WalletTransactionType.EXPENSE,
                        amount = BigDecimal("100.00"),
                        status = WalletTransactionStatus.CONFIRMED,
                    )
                val savedTransaction2 =
                    WalletTransactionFactory.create(
                        id = 9,
                        wallet = wallet,
                        type = WalletTransactionType.INCOME,
                        amount = BigDecimal("200.00"),
                        status = WalletTransactionStatus.CONFIRMED,
                    )
                val savedTransaction3 =
                    WalletTransactionFactory.create(
                        id = 10,
                        wallet = wallet,
                        type = WalletTransactionType.EXPENSE,
                        amount = BigDecimal("50.00"),
                        status = WalletTransactionStatus.CONFIRMED,
                    )

                every { walletRepository.findById(8) } returns Optional.of(wallet)
                every { walletRepository.getAllocatedBalanceByMasterWallet(8) } returns BigDecimal.ZERO
                every { walletTransactionRepository.save(any()) } returnsMany
                    listOf(
                        savedTransaction1,
                        savedTransaction2,
                        savedTransaction3,
                    )

                val result1 = service.createWalletTransaction(transaction1)
                val result2 = service.createWalletTransaction(transaction2)
                val result3 = service.createWalletTransaction(transaction3)

                Then("should return different ids for each transaction") {
                    result1 shouldBe 8
                    result2 shouldBe 9
                    result3 shouldBe 10
                }

                Then("should apply all balance changes correctly") {
                    wallet.balance shouldBe BigDecimal("5050.00")
                }

                Then("should call save three times") {
                    verify(exactly = 3) { walletTransactionRepository.save(any()) }
                }
            }
        }

        Given("a confirmed transaction with includeInAnalysis false") {
            val wallet = WalletFactory.create(id = 9, balance = BigDecimal("1000.00"))
            val transaction =
                WalletTransactionFactory.create(
                    wallet = wallet,
                    type = WalletTransactionType.EXPENSE,
                    amount = BigDecimal("100.00"),
                    status = WalletTransactionStatus.CONFIRMED,
                    includeInAnalysis = false,
                )
            val savedTransaction =
                WalletTransactionFactory.create(
                    id = 11,
                    wallet = wallet,
                    type = WalletTransactionType.EXPENSE,
                    amount = BigDecimal("100.00"),
                    status = WalletTransactionStatus.CONFIRMED,
                    includeInAnalysis = false,
                )

            When("creating the transaction") {
                every { walletRepository.findById(9) } returns Optional.of(wallet)
                every { walletRepository.getAllocatedBalanceByMasterWallet(9) } returns BigDecimal.ZERO
                every { walletTransactionRepository.save(any()) } returns savedTransaction

                val result = service.createWalletTransaction(transaction)

                Then("should return the created transaction id") {
                    result shouldBe 11
                }

                Then("should still decrement wallet balance") {
                    wallet.balance shouldBe BigDecimal("900.00")
                }
            }
        }

        Given("a confirmed transaction with custom date") {
            val wallet = WalletFactory.create(id = 10, balance = BigDecimal("1000.00"))
            val customDate = LocalDateTime.of(2024, 1, 15, 10, 30, 0)
            val transaction =
                WalletTransactionFactory.create(
                    wallet = wallet,
                    type = WalletTransactionType.INCOME,
                    amount = BigDecimal("250.00"),
                    status = WalletTransactionStatus.CONFIRMED,
                    date = customDate,
                )
            val savedTransaction =
                WalletTransactionFactory.create(
                    id = 12,
                    wallet = wallet,
                    type = WalletTransactionType.INCOME,
                    amount = BigDecimal("250.00"),
                    status = WalletTransactionStatus.CONFIRMED,
                    date = customDate,
                )

            When("creating the transaction") {
                every { walletRepository.findById(10) } returns Optional.of(wallet)
                every { walletTransactionRepository.save(any()) } returns savedTransaction
                every { walletRepository.getAllocatedBalanceByMasterWallet(10) } returns BigDecimal.ZERO

                val result = service.createWalletTransaction(transaction)

                Then("should return the created transaction id") {
                    result shouldBe 12
                }

                Then("should increment wallet balance") {
                    wallet.balance shouldBe BigDecimal("1250.00")
                }
            }
        }
    })
