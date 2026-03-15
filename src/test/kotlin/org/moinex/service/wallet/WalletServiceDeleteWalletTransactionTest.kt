package org.moinex.service.wallet

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import jakarta.persistence.EntityNotFoundException
import org.moinex.factory.wallet.WalletFactory
import org.moinex.factory.wallet.WalletTransactionFactory
import org.moinex.factory.wallet.WalletTypeFactory
import org.moinex.model.enums.WalletTransactionStatus
import org.moinex.model.enums.WalletTransactionType
import org.moinex.repository.wallettransaction.TransferRepository
import org.moinex.repository.wallettransaction.WalletRepository
import org.moinex.repository.wallettransaction.WalletTransactionRepository
import org.moinex.repository.wallettransaction.WalletTypeRepository
import org.moinex.service.wallet.WalletService
import java.math.BigDecimal
import java.util.Optional

class WalletServiceDeleteWalletTransactionTest :
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

        Given("a confirmed expense transaction") {
            val wallet = WalletFactory.create(id = 1, balance = BigDecimal("900.00"))
            val transaction =
                WalletTransactionFactory.create(
                    id = 1,
                    wallet = wallet,
                    type = WalletTransactionType.EXPENSE,
                    amount = BigDecimal("100.00"),
                    status = WalletTransactionStatus.CONFIRMED,
                )

            When("deleting the confirmed expense transaction") {
                every { walletTransactionRepository.findById(1) } returns Optional.of(transaction)
                every { walletRepository.getAllocatedBalanceByMasterWallet(1) } returns BigDecimal.ZERO
                every { walletTransactionRepository.delete(any()) } returns Unit

                service.deleteWalletTransaction(1)

                Then("should increment wallet balance") {
                    wallet.balance shouldBe BigDecimal("1000.00")
                }

                Then("should delete the transaction") {
                    verify { walletTransactionRepository.delete(transaction) }
                }
            }
        }

        Given("a confirmed income transaction") {
            val wallet = WalletFactory.create(id = 2, balance = BigDecimal("1100.00"))
            val transaction =
                WalletTransactionFactory.create(
                    id = 2,
                    wallet = wallet,
                    type = WalletTransactionType.INCOME,
                    amount = BigDecimal("100.00"),
                    status = WalletTransactionStatus.CONFIRMED,
                )

            When("deleting the confirmed income transaction") {
                every { walletTransactionRepository.findById(2) } returns Optional.of(transaction)
                every { walletRepository.getAllocatedBalanceByMasterWallet(2) } returns BigDecimal.ZERO
                every { walletTransactionRepository.delete(any()) } returns Unit

                service.deleteWalletTransaction(2)

                Then("should decrement wallet balance") {
                    wallet.balance shouldBe BigDecimal("1000.00")
                }

                Then("should delete the transaction") {
                    verify { walletTransactionRepository.delete(transaction) }
                }
            }
        }

        Given("a pending expense transaction") {
            val wallet = WalletFactory.create(id = 3, balance = BigDecimal("1000.00"))
            val transaction =
                WalletTransactionFactory.create(
                    id = 3,
                    wallet = wallet,
                    type = WalletTransactionType.EXPENSE,
                    amount = BigDecimal("100.00"),
                    status = WalletTransactionStatus.PENDING,
                )

            When("deleting the pending expense transaction") {
                every { walletTransactionRepository.findById(3) } returns Optional.of(transaction)
                every { walletTransactionRepository.delete(any()) } returns Unit

                service.deleteWalletTransaction(3)

                Then("should not modify wallet balance") {
                    wallet.balance shouldBe BigDecimal("1000.00")
                }

                Then("should delete the transaction") {
                    verify { walletTransactionRepository.delete(transaction) }
                }
            }
        }

        Given("a pending income transaction") {
            val wallet = WalletFactory.create(id = 4, balance = BigDecimal("1000.00"))
            val transaction =
                WalletTransactionFactory.create(
                    id = 4,
                    wallet = wallet,
                    type = WalletTransactionType.INCOME,
                    amount = BigDecimal("100.00"),
                    status = WalletTransactionStatus.PENDING,
                )

            When("deleting the pending income transaction") {
                every { walletTransactionRepository.findById(4) } returns Optional.of(transaction)
                every { walletTransactionRepository.delete(any()) } returns Unit

                service.deleteWalletTransaction(4)

                Then("should not modify wallet balance") {
                    wallet.balance shouldBe BigDecimal("1000.00")
                }

                Then("should delete the transaction") {
                    verify { walletTransactionRepository.delete(transaction) }
                }
            }
        }

        Given("a confirmed expense transaction with large amount") {
            val wallet = WalletFactory.create(id = 5, balance = BigDecimal("500.00"))
            val transaction =
                WalletTransactionFactory.create(
                    id = 5,
                    wallet = wallet,
                    type = WalletTransactionType.EXPENSE,
                    amount = BigDecimal("500.00"),
                    status = WalletTransactionStatus.CONFIRMED,
                )

            When("deleting the confirmed expense transaction with large amount") {
                every { walletTransactionRepository.findById(5) } returns Optional.of(transaction)
                every { walletRepository.getAllocatedBalanceByMasterWallet(5) } returns BigDecimal.ZERO
                every { walletTransactionRepository.delete(any()) } returns Unit

                service.deleteWalletTransaction(5)

                Then("should increment wallet balance by full amount") {
                    wallet.balance shouldBe BigDecimal("1000.00")
                }

                Then("should delete the transaction") {
                    verify { walletTransactionRepository.delete(transaction) }
                }
            }
        }

        Given("a confirmed income transaction with large amount") {
            val wallet = WalletFactory.create(id = 6, balance = BigDecimal("2000.00"))
            val transaction =
                WalletTransactionFactory.create(
                    id = 6,
                    wallet = wallet,
                    type = WalletTransactionType.INCOME,
                    amount = BigDecimal("1000.00"),
                    status = WalletTransactionStatus.CONFIRMED,
                )

            When("deleting the confirmed income transaction with large amount") {
                every { walletTransactionRepository.findById(6) } returns Optional.of(transaction)
                every { walletRepository.getAllocatedBalanceByMasterWallet(6) } returns BigDecimal.ZERO
                every { walletTransactionRepository.delete(any()) } returns Unit

                service.deleteWalletTransaction(6)

                Then("should decrement wallet balance by full amount") {
                    wallet.balance shouldBe BigDecimal("1000.00")
                }

                Then("should delete the transaction") {
                    verify { walletTransactionRepository.delete(transaction) }
                }
            }
        }

        Given("a non-existent transaction") {
            When("deleting a non-existent transaction") {
                every { walletTransactionRepository.findById(999) } returns Optional.empty()

                Then("should throw EntityNotFoundException") {
                    shouldThrow<EntityNotFoundException> {
                        service.deleteWalletTransaction(999)
                    }
                }
            }
        }

        Given("a confirmed expense transaction with decimal amount") {
            val wallet = WalletFactory.create(id = 7, balance = BigDecimal("950.50"))
            val transaction =
                WalletTransactionFactory.create(
                    id = 7,
                    wallet = wallet,
                    type = WalletTransactionType.EXPENSE,
                    amount = BigDecimal("150.75"),
                    status = WalletTransactionStatus.CONFIRMED,
                )

            When("deleting the confirmed expense transaction with decimal amount") {
                every { walletTransactionRepository.findById(7) } returns Optional.of(transaction)
                every { walletRepository.getAllocatedBalanceByMasterWallet(7) } returns BigDecimal.ZERO
                every { walletTransactionRepository.delete(any()) } returns Unit

                service.deleteWalletTransaction(7)

                Then("should increment wallet balance with exact decimal amount") {
                    wallet.balance shouldBe BigDecimal("1101.25")
                }

                Then("should delete the transaction") {
                    verify { walletTransactionRepository.delete(transaction) }
                }
            }
        }

        Given("a confirmed income transaction with decimal amount") {
            val wallet = WalletFactory.create(id = 8, balance = BigDecimal("1150.75"))
            val transaction =
                WalletTransactionFactory.create(
                    id = 8,
                    wallet = wallet,
                    type = WalletTransactionType.INCOME,
                    amount = BigDecimal("150.75"),
                    status = WalletTransactionStatus.CONFIRMED,
                )

            When("deleting the confirmed income transaction with decimal amount") {
                every { walletTransactionRepository.findById(8) } returns Optional.of(transaction)
                every { walletRepository.getAllocatedBalanceByMasterWallet(8) } returns BigDecimal.ZERO
                every { walletTransactionRepository.delete(any()) } returns Unit

                service.deleteWalletTransaction(8)

                Then("should decrement wallet balance with exact decimal amount") {
                    wallet.balance shouldBe BigDecimal("1000.00")
                }

                Then("should delete the transaction") {
                    verify { walletTransactionRepository.delete(transaction) }
                }
            }
        }

        Given("multiple confirmed transactions in same wallet") {
            val wallet = WalletFactory.create(id = 9, balance = BigDecimal("800.00"))
            val transaction1 =
                WalletTransactionFactory.create(
                    id = 9,
                    wallet = wallet,
                    type = WalletTransactionType.EXPENSE,
                    amount = BigDecimal("100.00"),
                    status = WalletTransactionStatus.CONFIRMED,
                )
            WalletTransactionFactory.create(
                id = 10,
                wallet = wallet,
                type = WalletTransactionType.EXPENSE,
                amount = BigDecimal("100.00"),
                status = WalletTransactionStatus.CONFIRMED,
            )

            When("deleting one of the confirmed expense transactions") {
                every { walletTransactionRepository.findById(9) } returns Optional.of(transaction1)
                every { walletRepository.getAllocatedBalanceByMasterWallet(9) } returns BigDecimal.ZERO
                every { walletTransactionRepository.delete(any()) } returns Unit

                service.deleteWalletTransaction(9)

                Then("should increment wallet balance by deleted transaction amount") {
                    wallet.balance shouldBe BigDecimal("900.00")
                }

                Then("should delete only the specified transaction") {
                    verify { walletTransactionRepository.delete(transaction1) }
                }
            }
        }

        Given("a confirmed expense transaction in virtual wallet") {
            val walletType = WalletTypeFactory.create(1, "Master")
            val masterWallet = WalletFactory.create(id = 10, balance = BigDecimal("1000.00"))
            val virtualWallet =
                WalletFactory.create(
                    11,
                    "Virtual Wallet",
                    BigDecimal("900.00"),
                    walletType,
                    masterWallet = masterWallet,
                )
            val transaction =
                WalletTransactionFactory.create(
                    id = 11,
                    wallet = virtualWallet,
                    type = WalletTransactionType.EXPENSE,
                    amount = BigDecimal("100.00"),
                    status = WalletTransactionStatus.CONFIRMED,
                )

            When("deleting confirmed expense from virtual wallet") {
                every { walletTransactionRepository.findById(11) } returns Optional.of(transaction)
                every { walletRepository.getAllocatedBalanceByMasterWallet(11) } returns BigDecimal.ZERO
                every { walletTransactionRepository.delete(any()) } returns Unit

                service.deleteWalletTransaction(11)

                Then("should increment virtual wallet balance") {
                    virtualWallet.balance shouldBe BigDecimal("1000.00")
                }

                Then("should also increment master wallet balance") {
                    masterWallet.balance shouldBe BigDecimal("1100.00")
                }

                Then("should delete the transaction") {
                    verify { walletTransactionRepository.delete(transaction) }
                }
            }
        }

        Given("a confirmed income transaction in virtual wallet") {
            val walletType = WalletTypeFactory.create(2, "Master")
            val masterWallet = WalletFactory.create(id = 12, balance = BigDecimal("1200.00"))
            val virtualWallet =
                WalletFactory.create(
                    13,
                    "Virtual Wallet",
                    BigDecimal("1100.00"),
                    walletType,
                    masterWallet = masterWallet,
                )
            val transaction =
                WalletTransactionFactory.create(
                    id = 12,
                    wallet = virtualWallet,
                    type = WalletTransactionType.INCOME,
                    amount = BigDecimal("100.00"),
                    status = WalletTransactionStatus.CONFIRMED,
                )

            When("deleting confirmed income from virtual wallet") {
                every { walletTransactionRepository.findById(12) } returns Optional.of(transaction)
                every { walletRepository.getAllocatedBalanceByMasterWallet(13) } returns BigDecimal.ZERO
                every { walletTransactionRepository.delete(any()) } returns Unit

                service.deleteWalletTransaction(12)

                Then("should decrement virtual wallet balance") {
                    virtualWallet.balance shouldBe BigDecimal("1000.00")
                }

                Then("should also decrement master wallet balance") {
                    masterWallet.balance shouldBe BigDecimal("1100.00")
                }

                Then("should delete the transaction") {
                    verify { walletTransactionRepository.delete(transaction) }
                }
            }
        }
    })
