package org.moinex.service.wallet

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import jakarta.persistence.EntityNotFoundException
import org.moinex.factory.CategoryFactory
import org.moinex.factory.wallet.TransferFactory
import org.moinex.factory.wallet.WalletFactory
import org.moinex.factory.wallet.WalletTypeFactory
import org.moinex.repository.wallettransaction.TransferRepository
import org.moinex.repository.wallettransaction.WalletRepository
import org.moinex.repository.wallettransaction.WalletTransactionRepository
import org.moinex.repository.wallettransaction.WalletTypeRepository
import org.moinex.service.WalletService
import java.math.BigDecimal
import java.time.LocalDateTime
import java.util.Optional

class WalletServiceUpdateTransferTest :
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

        Given("a transfer with no changes") {
            val senderWallet = WalletFactory.create(id = 1, balance = BigDecimal("900.00"))
            val receiverWallet = WalletFactory.create(id = 2, balance = BigDecimal("600.00"))
            val category = CategoryFactory.create(id = 1)
            val transferFromDb =
                TransferFactory.create(
                    id = 1,
                    senderWallet = senderWallet,
                    receiverWallet = receiverWallet,
                    amount = BigDecimal("100.00"),
                    category = category,
                )
            val updatedTransfer =
                TransferFactory.create(
                    id = 1,
                    senderWallet = senderWallet,
                    receiverWallet = receiverWallet,
                    amount = BigDecimal("100.00"),
                    category = category,
                )

            When("updating transfer with same values") {
                every { transfersRepository.findById(1) } returns Optional.of(transferFromDb)

                service.updateTransfer(updatedTransfer)

                Then("should not modify wallet balances") {
                    senderWallet.balance shouldBe BigDecimal("900.00")
                    receiverWallet.balance shouldBe BigDecimal("600.00")
                }

                Then("should keep transfer properties unchanged") {
                    transferFromDb.amount shouldBe BigDecimal("100.00")
                    transferFromDb.senderWallet.id shouldBe 1
                    transferFromDb.receiverWallet.id shouldBe 2
                }
            }
        }

        Given("a transfer changing amount to higher value") {
            val senderWallet = WalletFactory.create(id = 3, balance = BigDecimal("900.00"))
            val receiverWallet = WalletFactory.create(id = 4, balance = BigDecimal("600.00"))
            val category = CategoryFactory.create(id = 1)
            val transferFromDb =
                TransferFactory.create(
                    id = 2,
                    senderWallet = senderWallet,
                    receiverWallet = receiverWallet,
                    amount = BigDecimal("100.00"),
                    category = category,
                )
            val updatedTransfer =
                TransferFactory.create(
                    id = 2,
                    senderWallet = senderWallet,
                    receiverWallet = receiverWallet,
                    amount = BigDecimal("150.00"),
                    category = category,
                )

            When("increasing transfer amount") {
                every { transfersRepository.findById(2) } returns Optional.of(transferFromDb)
                every { walletRepository.getAllocatedBalanceByMasterWallet(3) } returns BigDecimal.ZERO
                every { walletRepository.getAllocatedBalanceByMasterWallet(4) } returns BigDecimal.ZERO

                service.updateTransfer(updatedTransfer)

                Then("should decrement sender wallet by difference") {
                    senderWallet.balance shouldBe BigDecimal("850.00")
                }

                Then("should increment receiver wallet by difference") {
                    receiverWallet.balance shouldBe BigDecimal("650.00")
                }

                Then("should update transfer amount") {
                    transferFromDb.amount shouldBe BigDecimal("150.00")
                }
            }
        }

        Given("a transfer changing amount to lower value") {
            val senderWallet = WalletFactory.create(id = 5, balance = BigDecimal("850.00"))
            val receiverWallet = WalletFactory.create(id = 6, balance = BigDecimal("650.00"))
            val category = CategoryFactory.create(id = 1)
            val transferFromDb =
                TransferFactory.create(
                    id = 3,
                    senderWallet = senderWallet,
                    receiverWallet = receiverWallet,
                    amount = BigDecimal("150.00"),
                    category = category,
                )
            val updatedTransfer =
                TransferFactory.create(
                    id = 3,
                    senderWallet = senderWallet,
                    receiverWallet = receiverWallet,
                    amount = BigDecimal("100.00"),
                    category = category,
                )

            When("decreasing transfer amount") {
                every { transfersRepository.findById(3) } returns Optional.of(transferFromDb)
                every { walletRepository.getAllocatedBalanceByMasterWallet(5) } returns BigDecimal.ZERO
                every { walletRepository.getAllocatedBalanceByMasterWallet(6) } returns BigDecimal.ZERO

                service.updateTransfer(updatedTransfer)

                Then("should increment sender wallet by difference") {
                    senderWallet.balance shouldBe BigDecimal("900.00")
                }

                Then("should decrement receiver wallet by difference") {
                    receiverWallet.balance shouldBe BigDecimal("600.00")
                }

                Then("should update transfer amount") {
                    transferFromDb.amount shouldBe BigDecimal("100.00")
                }
            }
        }

        Given("a transfer changing sender wallet") {
            val oldSenderWallet = WalletFactory.create(id = 7, balance = BigDecimal("900.00"))
            val newSenderWallet = WalletFactory.create(id = 8, balance = BigDecimal("500.00"))
            val receiverWallet = WalletFactory.create(id = 9, balance = BigDecimal("600.00"))
            val category = CategoryFactory.create(id = 1)
            val transferFromDb =
                TransferFactory.create(
                    id = 4,
                    senderWallet = oldSenderWallet,
                    receiverWallet = receiverWallet,
                    amount = BigDecimal("100.00"),
                    category = category,
                )
            val updatedTransfer =
                TransferFactory.create(
                    id = 4,
                    senderWallet = newSenderWallet,
                    receiverWallet = receiverWallet,
                    amount = BigDecimal("100.00"),
                    category = category,
                )

            When("changing sender wallet") {
                every { transfersRepository.findById(4) } returns Optional.of(transferFromDb)
                every { walletRepository.getAllocatedBalanceByMasterWallet(7) } returns BigDecimal.ZERO
                every { walletRepository.getAllocatedBalanceByMasterWallet(8) } returns BigDecimal.ZERO
                every { walletRepository.getAllocatedBalanceByMasterWallet(9) } returns BigDecimal.ZERO

                service.updateTransfer(updatedTransfer)

                Then("should increment old sender wallet balance") {
                    oldSenderWallet.balance shouldBe BigDecimal("1000.00")
                }

                Then("should decrement new sender wallet balance") {
                    newSenderWallet.balance shouldBe BigDecimal("400.00")
                }

                Then("should keep receiver wallet balance unchanged") {
                    receiverWallet.balance shouldBe BigDecimal("600.00")
                }

                Then("should update transfer sender wallet") {
                    transferFromDb.senderWallet.id shouldBe 8
                }
            }
        }

        Given("a transfer changing receiver wallet") {
            val senderWallet = WalletFactory.create(id = 10, balance = BigDecimal("900.00"))
            val oldReceiverWallet = WalletFactory.create(id = 11, balance = BigDecimal("600.00"))
            val newReceiverWallet = WalletFactory.create(id = 12, balance = BigDecimal("400.00"))
            val category = CategoryFactory.create(id = 1)
            val transferFromDb =
                TransferFactory.create(
                    id = 5,
                    senderWallet = senderWallet,
                    receiverWallet = oldReceiverWallet,
                    amount = BigDecimal("100.00"),
                    category = category,
                )
            val updatedTransfer =
                TransferFactory.create(
                    id = 5,
                    senderWallet = senderWallet,
                    receiverWallet = newReceiverWallet,
                    amount = BigDecimal("100.00"),
                    category = category,
                )

            When("changing receiver wallet") {
                every { transfersRepository.findById(5) } returns Optional.of(transferFromDb)
                every { walletRepository.getAllocatedBalanceByMasterWallet(10) } returns BigDecimal.ZERO
                every { walletRepository.getAllocatedBalanceByMasterWallet(11) } returns BigDecimal.ZERO
                every { walletRepository.getAllocatedBalanceByMasterWallet(12) } returns BigDecimal.ZERO

                service.updateTransfer(updatedTransfer)

                Then("should keep sender wallet balance unchanged") {
                    senderWallet.balance shouldBe BigDecimal("900.00")
                }

                Then("should decrement old receiver wallet balance") {
                    oldReceiverWallet.balance shouldBe BigDecimal("500.00")
                }

                Then("should increment new receiver wallet balance") {
                    newReceiverWallet.balance shouldBe BigDecimal("500.00")
                }

                Then("should update transfer receiver wallet") {
                    transferFromDb.receiverWallet.id shouldBe 12
                }
            }
        }

        Given("a transfer changing both sender and receiver wallets") {
            val oldSenderWallet = WalletFactory.create(id = 13, balance = BigDecimal("900.00"))
            val newSenderWallet = WalletFactory.create(id = 14, balance = BigDecimal("500.00"))
            val oldReceiverWallet = WalletFactory.create(id = 15, balance = BigDecimal("600.00"))
            val newReceiverWallet = WalletFactory.create(id = 16, balance = BigDecimal("400.00"))
            val category = CategoryFactory.create(id = 1)
            val transferFromDb =
                TransferFactory.create(
                    id = 6,
                    senderWallet = oldSenderWallet,
                    receiverWallet = oldReceiverWallet,
                    amount = BigDecimal("100.00"),
                    category = category,
                )
            val updatedTransfer =
                TransferFactory.create(
                    id = 6,
                    senderWallet = newSenderWallet,
                    receiverWallet = newReceiverWallet,
                    amount = BigDecimal("100.00"),
                    category = category,
                )

            When("changing both sender and receiver wallets") {
                every { transfersRepository.findById(6) } returns Optional.of(transferFromDb)
                every { walletRepository.getAllocatedBalanceByMasterWallet(13) } returns BigDecimal.ZERO
                every { walletRepository.getAllocatedBalanceByMasterWallet(14) } returns BigDecimal.ZERO
                every { walletRepository.getAllocatedBalanceByMasterWallet(15) } returns BigDecimal.ZERO
                every { walletRepository.getAllocatedBalanceByMasterWallet(16) } returns BigDecimal.ZERO

                service.updateTransfer(updatedTransfer)

                Then("should increment old sender wallet balance") {
                    oldSenderWallet.balance shouldBe BigDecimal("1000.00")
                }

                Then("should decrement new sender wallet balance") {
                    newSenderWallet.balance shouldBe BigDecimal("400.00")
                }

                Then("should decrement old receiver wallet balance") {
                    oldReceiverWallet.balance shouldBe BigDecimal("500.00")
                }

                Then("should increment new receiver wallet balance") {
                    newReceiverWallet.balance shouldBe BigDecimal("500.00")
                }

                Then("should update both wallets") {
                    transferFromDb.senderWallet.id shouldBe 14
                    transferFromDb.receiverWallet.id shouldBe 16
                }
            }
        }

        Given("a transfer changing amount and sender wallet") {
            val oldSenderWallet = WalletFactory.create(id = 17, balance = BigDecimal("900.00"))
            val newSenderWallet = WalletFactory.create(id = 18, balance = BigDecimal("500.00"))
            val receiverWallet = WalletFactory.create(id = 19, balance = BigDecimal("600.00"))
            val category = CategoryFactory.create(id = 1)
            val transferFromDb =
                TransferFactory.create(
                    id = 7,
                    senderWallet = oldSenderWallet,
                    receiverWallet = receiverWallet,
                    amount = BigDecimal("100.00"),
                    category = category,
                )
            val updatedTransfer =
                TransferFactory.create(
                    id = 7,
                    senderWallet = newSenderWallet,
                    receiverWallet = receiverWallet,
                    amount = BigDecimal("150.00"),
                    category = category,
                )

            When("changing amount and sender wallet") {
                every { transfersRepository.findById(7) } returns Optional.of(transferFromDb)
                every { walletRepository.getAllocatedBalanceByMasterWallet(17) } returns BigDecimal.ZERO
                every { walletRepository.getAllocatedBalanceByMasterWallet(18) } returns BigDecimal.ZERO
                every { walletRepository.getAllocatedBalanceByMasterWallet(19) } returns BigDecimal.ZERO

                service.updateTransfer(updatedTransfer)

                Then("should increment old sender wallet by original amount") {
                    oldSenderWallet.balance shouldBe BigDecimal("1000.00")
                }

                Then("should decrement new sender wallet by new amount") {
                    newSenderWallet.balance shouldBe BigDecimal("350.00")
                }

                Then("should increment receiver wallet by difference") {
                    receiverWallet.balance shouldBe BigDecimal("650.00")
                }

                Then("should update transfer amount") {
                    transferFromDb.amount shouldBe BigDecimal("150.00")
                }
            }
        }

        Given("a transfer changing amount and receiver wallet") {
            val senderWallet = WalletFactory.create(id = 20, balance = BigDecimal("850.00"))
            val oldReceiverWallet = WalletFactory.create(id = 21, balance = BigDecimal("600.00"))
            val newReceiverWallet = WalletFactory.create(id = 22, balance = BigDecimal("400.00"))
            val category = CategoryFactory.create(id = 1)
            val transferFromDb =
                TransferFactory.create(
                    id = 8,
                    senderWallet = senderWallet,
                    receiverWallet = oldReceiverWallet,
                    amount = BigDecimal("150.00"),
                    category = category,
                )
            val updatedTransfer =
                TransferFactory.create(
                    id = 8,
                    senderWallet = senderWallet,
                    receiverWallet = newReceiverWallet,
                    amount = BigDecimal("100.00"),
                    category = category,
                )

            When("changing amount and receiver wallet") {
                every { transfersRepository.findById(8) } returns Optional.of(transferFromDb)
                every { walletRepository.getAllocatedBalanceByMasterWallet(20) } returns BigDecimal.ZERO
                every { walletRepository.getAllocatedBalanceByMasterWallet(21) } returns BigDecimal.ZERO
                every { walletRepository.getAllocatedBalanceByMasterWallet(22) } returns BigDecimal.ZERO

                service.updateTransfer(updatedTransfer)

                Then("should increment sender wallet by difference") {
                    senderWallet.balance shouldBe BigDecimal("900.00")
                }

                Then("should decrement old receiver wallet by original amount") {
                    oldReceiverWallet.balance shouldBe BigDecimal("450.00")
                }

                Then("should increment new receiver wallet by new amount") {
                    newReceiverWallet.balance shouldBe BigDecimal("500.00")
                }

                Then("should update transfer amount") {
                    transferFromDb.amount shouldBe BigDecimal("100.00")
                }
            }
        }

        Given("a transfer updating description and category") {
            val senderWallet = WalletFactory.create(id = 23, balance = BigDecimal("900.00"))
            val receiverWallet = WalletFactory.create(id = 24, balance = BigDecimal("600.00"))
            val oldCategory = CategoryFactory.create(id = 1, name = "Old Category")
            val newCategory = CategoryFactory.create(id = 2, name = "New Category")
            val transferFromDb =
                TransferFactory.create(
                    id = 9,
                    senderWallet = senderWallet,
                    receiverWallet = receiverWallet,
                    amount = BigDecimal("100.00"),
                    description = "Old description",
                    category = oldCategory,
                )
            val updatedTransfer =
                TransferFactory.create(
                    id = 9,
                    senderWallet = senderWallet,
                    receiverWallet = receiverWallet,
                    amount = BigDecimal("100.00"),
                    description = "New description",
                    category = newCategory,
                )

            When("updating description and category") {
                every { transfersRepository.findById(9) } returns Optional.of(transferFromDb)

                service.updateTransfer(updatedTransfer)

                Then("should update description") {
                    transferFromDb.description shouldBe "New description"
                }

                Then("should update category") {
                    transferFromDb.category.id shouldBe 2
                }

                Then("should not modify wallet balances") {
                    senderWallet.balance shouldBe BigDecimal("900.00")
                    receiverWallet.balance shouldBe BigDecimal("600.00")
                }
            }
        }

        Given("a transfer updating date") {
            val senderWallet = WalletFactory.create(id = 25, balance = BigDecimal("900.00"))
            val receiverWallet = WalletFactory.create(id = 26, balance = BigDecimal("600.00"))
            val category = CategoryFactory.create(id = 1)
            val oldDate = LocalDateTime.of(2024, 1, 1, 10, 0, 0)
            val newDate = LocalDateTime.of(2024, 2, 1, 15, 30, 0)
            val transferFromDb =
                TransferFactory.create(
                    id = 10,
                    senderWallet = senderWallet,
                    receiverWallet = receiverWallet,
                    amount = BigDecimal("100.00"),
                    date = oldDate,
                    category = category,
                )
            val updatedTransfer =
                TransferFactory.create(
                    id = 10,
                    senderWallet = senderWallet,
                    receiverWallet = receiverWallet,
                    amount = BigDecimal("100.00"),
                    date = newDate,
                    category = category,
                )

            When("updating transfer date") {
                every { transfersRepository.findById(10) } returns Optional.of(transferFromDb)

                service.updateTransfer(updatedTransfer)

                Then("should update date") {
                    transferFromDb.date shouldBe newDate
                }

                Then("should not modify wallet balances") {
                    senderWallet.balance shouldBe BigDecimal("900.00")
                    receiverWallet.balance shouldBe BigDecimal("600.00")
                }
            }
        }

        Given("a non-existent transfer") {
            val senderWallet = WalletFactory.create(id = 27)
            val receiverWallet = WalletFactory.create(id = 28)
            val transfer =
                TransferFactory.create(
                    id = 999,
                    senderWallet = senderWallet,
                    receiverWallet = receiverWallet,
                )

            When("updating non-existent transfer") {
                every { transfersRepository.findById(999) } returns Optional.empty()

                Then("should throw EntityNotFoundException") {
                    shouldThrow<EntityNotFoundException> {
                        service.updateTransfer(transfer)
                    }
                }
            }
        }

        Given("a transfer between virtual wallets with same master changing amount") {
            val walletType = WalletTypeFactory.create(7, "Master")
            val masterWallet = WalletFactory.create(id = 29, name = "Master", balance = BigDecimal("2000.00"), type = walletType)
            val virtualWallet1 =
                WalletFactory.create(
                    id = 30,
                    name = "Virtual1",
                    balance = BigDecimal("900.00"),
                    type = walletType,
                    masterWallet = masterWallet,
                )
            val virtualWallet2 =
                WalletFactory.create(
                    id = 31,
                    name = "Virtual2",
                    balance = BigDecimal("600.00"),
                    type = walletType,
                    masterWallet = masterWallet,
                )
            val category = CategoryFactory.create(id = 1)
            val transferFromDb =
                TransferFactory.create(
                    id = 11,
                    senderWallet = virtualWallet1,
                    receiverWallet = virtualWallet2,
                    amount = BigDecimal("100.00"),
                    category = category,
                )
            val updatedTransfer =
                TransferFactory.create(
                    id = 11,
                    senderWallet = virtualWallet1,
                    receiverWallet = virtualWallet2,
                    amount = BigDecimal("150.00"),
                    category = category,
                )

            When("updating transfer amount between virtual wallets with same master") {
                every { transfersRepository.findById(11) } returns Optional.of(transferFromDb)
                every { walletRepository.getAllocatedBalanceByMasterWallet(30) } returns BigDecimal.ZERO
                every { walletRepository.getAllocatedBalanceByMasterWallet(31) } returns BigDecimal.ZERO

                service.updateTransfer(updatedTransfer)

                Then("should decrement sender virtual wallet by difference") {
                    virtualWallet1.balance shouldBe BigDecimal("850.00")
                }

                Then("should increment receiver virtual wallet by difference") {
                    virtualWallet2.balance shouldBe BigDecimal("650.00")
                }

                Then("should keep master wallet balance unchanged") {
                    masterWallet.balance shouldBe BigDecimal("2000.00")
                }

                Then("should update transfer amount") {
                    transferFromDb.amount shouldBe BigDecimal("150.00")
                }
            }
        }
    })
