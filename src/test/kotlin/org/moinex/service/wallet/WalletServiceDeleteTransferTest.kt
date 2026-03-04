package org.moinex.service.wallet

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import jakarta.persistence.EntityNotFoundException
import org.moinex.common.isZero
import org.moinex.factory.CategoryFactory
import org.moinex.factory.TransferFactory
import org.moinex.factory.WalletFactory
import org.moinex.factory.WalletTypeFactory
import org.moinex.repository.wallettransaction.TransferRepository
import org.moinex.repository.wallettransaction.WalletRepository
import org.moinex.repository.wallettransaction.WalletTransactionRepository
import org.moinex.repository.wallettransaction.WalletTypeRepository
import org.moinex.service.WalletService
import java.math.BigDecimal
import java.util.Optional

class WalletServiceDeleteTransferTest :
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

        Given("a transfer between two regular wallets") {
            val senderWallet = WalletFactory.create(id = 1, balance = BigDecimal("900.00"))
            val receiverWallet = WalletFactory.create(id = 2, balance = BigDecimal("600.00"))
            val category = CategoryFactory.create(id = 1)
            val transfer =
                TransferFactory.create(
                    id = 1,
                    senderWallet = senderWallet,
                    receiverWallet = receiverWallet,
                    amount = BigDecimal("100.00"),
                    category = category,
                )

            When("deleting the transfer") {
                every { transfersRepository.findById(1) } returns Optional.of(transfer)
                every { walletRepository.getAllocatedBalanceByMasterWallet(1) } returns BigDecimal.ZERO
                every { walletRepository.getAllocatedBalanceByMasterWallet(2) } returns BigDecimal.ZERO
                every { transfersRepository.delete(any()) } returns Unit

                service.deleteTransfer(1)

                Then("should increment sender wallet balance") {
                    senderWallet.balance shouldBe BigDecimal("1000.00")
                }

                Then("should decrement receiver wallet balance") {
                    receiverWallet.balance shouldBe BigDecimal("500.00")
                }

                Then("should delete the transfer") {
                    verify { transfersRepository.delete(transfer) }
                }
            }
        }

        Given("a transfer with large amount") {
            val senderWallet = WalletFactory.create(id = 3, balance = BigDecimal("3000.00"))
            val receiverWallet = WalletFactory.create(id = 4, balance = BigDecimal("3000.00"))
            val category = CategoryFactory.create(id = 1)
            val transfer =
                TransferFactory.create(
                    id = 2,
                    senderWallet = senderWallet,
                    receiverWallet = receiverWallet,
                    amount = BigDecimal("2000.00"),
                    category = category,
                )

            When("deleting transfer with large amount") {
                every { transfersRepository.findById(2) } returns Optional.of(transfer)
                every { walletRepository.getAllocatedBalanceByMasterWallet(3) } returns BigDecimal.ZERO
                every { walletRepository.getAllocatedBalanceByMasterWallet(4) } returns BigDecimal.ZERO
                every { transfersRepository.delete(any()) } returns Unit

                service.deleteTransfer(2)

                Then("should increment sender wallet balance by full amount") {
                    senderWallet.balance shouldBe BigDecimal("5000.00")
                }

                Then("should decrement receiver wallet balance by full amount") {
                    receiverWallet.balance shouldBe BigDecimal("1000.00")
                }
            }
        }

        Given("a transfer with decimal amount") {
            val senderWallet = WalletFactory.create(id = 5, balance = BigDecimal("850.25"))
            val receiverWallet = WalletFactory.create(id = 6, balance = BigDecimal("651.00"))
            val category = CategoryFactory.create(id = 1)
            val transfer =
                TransferFactory.create(
                    id = 3,
                    senderWallet = senderWallet,
                    receiverWallet = receiverWallet,
                    amount = BigDecimal("150.25"),
                    category = category,
                )

            When("deleting transfer with decimal amount") {
                every { transfersRepository.findById(3) } returns Optional.of(transfer)
                every { walletRepository.getAllocatedBalanceByMasterWallet(5) } returns BigDecimal.ZERO
                every { walletRepository.getAllocatedBalanceByMasterWallet(6) } returns BigDecimal.ZERO
                every { transfersRepository.delete(any()) } returns Unit

                service.deleteTransfer(3)

                Then("should increment sender wallet balance with exact decimal") {
                    senderWallet.balance shouldBe BigDecimal("1000.50")
                }

                Then("should decrement receiver wallet balance with exact decimal") {
                    receiverWallet.balance shouldBe BigDecimal("500.75")
                }
            }
        }

        Given("a non-existent transfer") {
            When("deleting non-existent transfer") {
                every { transfersRepository.findById(999) } returns Optional.empty()

                Then("should throw EntityNotFoundException") {
                    shouldThrow<EntityNotFoundException> {
                        service.deleteTransfer(999)
                    }
                }
            }
        }

        Given("a transfer from virtual wallet to regular wallet") {
            val walletType = WalletTypeFactory.create(1, "Master")
            val masterWallet = WalletFactory.create(id = 7, name = "Master", balance = BigDecimal("1900.00"), type = walletType)
            val virtualWallet = WalletFactory.create(
                id = 8,
                name = "Virtual",
                balance = BigDecimal("900.00"),
                type = walletType,
                masterWallet = masterWallet,
            )
            val receiverWallet = WalletFactory.create(id = 9, balance = BigDecimal("600.00"))
            val category = CategoryFactory.create(id = 1)
            val transfer =
                TransferFactory.create(
                    id = 4,
                    senderWallet = virtualWallet,
                    receiverWallet = receiverWallet,
                    amount = BigDecimal("100.00"),
                    category = category,
                )

            When("deleting transfer from virtual wallet") {
                every { transfersRepository.findById(4) } returns Optional.of(transfer)
                every { walletRepository.getAllocatedBalanceByMasterWallet(8) } returns BigDecimal.ZERO
                every { walletRepository.getAllocatedBalanceByMasterWallet(9) } returns BigDecimal.ZERO
                every { transfersRepository.delete(any()) } returns Unit

                service.deleteTransfer(4)

                Then("should increment virtual wallet balance") {
                    virtualWallet.balance shouldBe BigDecimal("1000.00")
                }

                Then("should also increment master wallet balance") {
                    masterWallet.balance shouldBe BigDecimal("2000.00")
                }

                Then("should decrement receiver wallet balance") {
                    receiverWallet.balance shouldBe BigDecimal("500.00")
                }

                Then("should delete the transfer") {
                    verify { transfersRepository.delete(transfer) }
                }
            }
        }

        Given("a transfer to virtual wallet from regular wallet") {
            val walletType = WalletTypeFactory.create(2, "Master")
            val masterWallet = WalletFactory.create(id = 10, name = "Master", balance = BigDecimal("1100.00"), type = walletType)
            val virtualWallet = WalletFactory.create(
                id = 11,
                name = "Virtual",
                balance = BigDecimal("600.00"),
                type = walletType,
                masterWallet = masterWallet,
            )
            val senderWallet = WalletFactory.create(id = 12, balance = BigDecimal("900.00"))
            val category = CategoryFactory.create(id = 1)
            val transfer =
                TransferFactory.create(
                    id = 5,
                    senderWallet = senderWallet,
                    receiverWallet = virtualWallet,
                    amount = BigDecimal("100.00"),
                    category = category,
                )

            When("deleting transfer to virtual wallet") {
                every { transfersRepository.findById(5) } returns Optional.of(transfer)
                every { walletRepository.getAllocatedBalanceByMasterWallet(12) } returns BigDecimal.ZERO
                every { walletRepository.getAllocatedBalanceByMasterWallet(11) } returns BigDecimal.ZERO
                every { transfersRepository.delete(any()) } returns Unit

                service.deleteTransfer(5)

                Then("should increment sender wallet balance") {
                    senderWallet.balance shouldBe BigDecimal("1000.00")
                }

                Then("should decrement virtual wallet balance") {
                    virtualWallet.balance shouldBe BigDecimal("500.00")
                }

                Then("should also decrement master wallet balance") {
                    masterWallet.balance shouldBe BigDecimal("1000.00")
                }

                Then("should delete the transfer") {
                    verify { transfersRepository.delete(transfer) }
                }
            }
        }

        Given("a transfer between two virtual wallets with same master") {
            val walletType = WalletTypeFactory.create(3, "Master")
            val masterWallet = WalletFactory.create(id = 13, name = "Master", balance = BigDecimal("2000.00"), type = walletType)
            val virtualWallet1 = WalletFactory.create(
                id = 14,
                name = "Virtual1",
                balance = BigDecimal("900.00"),
                type = walletType,
                masterWallet = masterWallet,
            )
            val virtualWallet2 = WalletFactory.create(
                id = 15,
                name = "Virtual2",
                balance = BigDecimal("600.00"),
                type = walletType,
                masterWallet = masterWallet,
            )
            val category = CategoryFactory.create(id = 1)
            val transfer =
                TransferFactory.create(
                    id = 6,
                    senderWallet = virtualWallet1,
                    receiverWallet = virtualWallet2,
                    amount = BigDecimal("100.00"),
                    category = category,
                )

            When("deleting transfer between virtual wallets with same master") {
                every { transfersRepository.findById(6) } returns Optional.of(transfer)
                every { walletRepository.getAllocatedBalanceByMasterWallet(14) } returns BigDecimal.ZERO
                every { walletRepository.getAllocatedBalanceByMasterWallet(15) } returns BigDecimal.ZERO
                every { transfersRepository.delete(any()) } returns Unit

                service.deleteTransfer(6)

                Then("should increment sender virtual wallet balance") {
                    virtualWallet1.balance shouldBe BigDecimal("1000.00")
                }

                Then("should decrement receiver virtual wallet balance") {
                    virtualWallet2.balance shouldBe BigDecimal("500.00")
                }

                Then("should keep master wallet balance unchanged") {
                    masterWallet.balance shouldBe BigDecimal("2000.00")
                }

                Then("should delete the transfer") {
                    verify { transfersRepository.delete(transfer) }
                }
            }
        }

        Given("a transfer between two virtual wallets of different masters") {
            val walletType1 = WalletTypeFactory.create(4, "Master")
            val walletType2 = WalletTypeFactory.create(5, "Master")
            val masterWallet1 = WalletFactory.create(id = 16, name = "Master1", balance = BigDecimal("900.00"), type = walletType1)
            val virtualWallet1 = WalletFactory.create(
                id = 17,
                name = "Virtual1",
                balance = BigDecimal("400.00"),
                type = walletType1,
                masterWallet = masterWallet1,
            )
            val masterWallet2 = WalletFactory.create(id = 18, name = "Master2", balance = BigDecimal("1100.00"), type = walletType2)
            val virtualWallet2 = WalletFactory.create(
                id = 19,
                name = "Virtual2",
                balance = BigDecimal("600.00"),
                type = walletType2,
                masterWallet = masterWallet2,
            )
            val category = CategoryFactory.create(id = 1)
            val transfer =
                TransferFactory.create(
                    id = 7,
                    senderWallet = virtualWallet1,
                    receiverWallet = virtualWallet2,
                    amount = BigDecimal("100.00"),
                    category = category,
                )

            When("deleting transfer between virtual wallets of different masters") {
                every { transfersRepository.findById(7) } returns Optional.of(transfer)
                every { walletRepository.getAllocatedBalanceByMasterWallet(17) } returns BigDecimal.ZERO
                every { walletRepository.getAllocatedBalanceByMasterWallet(19) } returns BigDecimal.ZERO
                every { transfersRepository.delete(any()) } returns Unit

                service.deleteTransfer(7)

                Then("should increment sender virtual wallet balance") {
                    virtualWallet1.balance shouldBe BigDecimal("500.00")
                }

                Then("should increment sender master wallet balance") {
                    masterWallet1.balance shouldBe BigDecimal("1000.00")
                }

                Then("should decrement receiver virtual wallet balance") {
                    virtualWallet2.balance shouldBe BigDecimal("500.00")
                }

                Then("should decrement receiver master wallet balance") {
                    masterWallet2.balance shouldBe BigDecimal("1000.00")
                }

                Then("should delete the transfer") {
                    verify { transfersRepository.delete(transfer) }
                }
            }
        }

        Given("a transfer with exact receiver balance") {
            val senderWallet = WalletFactory.create(id = 20, balance = BigDecimal("900.00"))
            val receiverWallet = WalletFactory.create(id = 21, balance = BigDecimal("100.00"))
            val category = CategoryFactory.create(id = 1)
            val transfer =
                TransferFactory.create(
                    id = 8,
                    senderWallet = senderWallet,
                    receiverWallet = receiverWallet,
                    amount = BigDecimal("100.00"),
                    category = category,
                )

            When("deleting transfer that reduces receiver to zero") {
                every { transfersRepository.findById(8) } returns Optional.of(transfer)
                every { walletRepository.getAllocatedBalanceByMasterWallet(20) } returns BigDecimal.ZERO
                every { walletRepository.getAllocatedBalanceByMasterWallet(21) } returns BigDecimal.ZERO
                every { transfersRepository.delete(any()) } returns Unit

                service.deleteTransfer(8)

                Then("should increment sender wallet balance") {
                    senderWallet.balance shouldBe BigDecimal("1000.00")
                }

                Then("should reduce receiver wallet balance to zero") {
                    receiverWallet.balance.isZero() shouldBe true
                }

                Then("should delete the transfer") {
                    verify { transfersRepository.delete(transfer) }
                }
            }
        }

        Given("multiple transfers in same wallets") {
            val senderWallet = WalletFactory.create(id = 22, balance = BigDecimal("800.00"))
            val receiverWallet = WalletFactory.create(id = 23, balance = BigDecimal("700.00"))
            val category = CategoryFactory.create(id = 1)
            val transfer1 =
                TransferFactory.create(
                    id = 9,
                    senderWallet = senderWallet,
                    receiverWallet = receiverWallet,
                    amount = BigDecimal("100.00"),
                    category = category,
                )
            TransferFactory.create(
                id = 10,
                senderWallet = senderWallet,
                receiverWallet = receiverWallet,
                amount = BigDecimal("100.00"),
                category = category,
            )

            When("deleting one of multiple transfers") {
                every { transfersRepository.findById(9) } returns Optional.of(transfer1)
                every { walletRepository.getAllocatedBalanceByMasterWallet(22) } returns BigDecimal.ZERO
                every { walletRepository.getAllocatedBalanceByMasterWallet(23) } returns BigDecimal.ZERO
                every { transfersRepository.delete(any()) } returns Unit

                service.deleteTransfer(9)

                Then("should increment sender wallet balance by deleted transfer amount") {
                    senderWallet.balance shouldBe BigDecimal("900.00")
                }

                Then("should decrement receiver wallet balance by deleted transfer amount") {
                    receiverWallet.balance shouldBe BigDecimal("600.00")
                }

                Then("should delete only the specified transfer") {
                    verify { transfersRepository.delete(transfer1) }
                }
            }
        }
    })
