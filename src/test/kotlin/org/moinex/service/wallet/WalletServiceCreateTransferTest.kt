package org.moinex.service.wallet

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import jakarta.persistence.EntityNotFoundException
import org.moinex.common.extension.isZero
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

class WalletServiceCreateTransferTest :
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

        Given("a valid transfer between two regular wallets") {
            val senderWallet = WalletFactory.create(id = 1, balance = BigDecimal("1000.00"))
            val receiverWallet = WalletFactory.create(id = 2, balance = BigDecimal("500.00"))
            val category = CategoryFactory.create(id = 1)
            val transfer =
                TransferFactory.create(
                    senderWallet = senderWallet,
                    receiverWallet = receiverWallet,
                    amount = BigDecimal("100.00"),
                    category = category,
                )
            val savedTransfer =
                TransferFactory.create(
                    id = 1,
                    senderWallet = senderWallet,
                    receiverWallet = receiverWallet,
                    amount = BigDecimal("100.00"),
                    category = category,
                )

            When("creating the transfer") {
                every { walletRepository.findById(1) } returns Optional.of(senderWallet)
                every { walletRepository.findById(2) } returns Optional.of(receiverWallet)
                every { walletRepository.getAllocatedBalanceByMasterWallet(1) } returns BigDecimal.ZERO
                every { walletRepository.getAllocatedBalanceByMasterWallet(2) } returns BigDecimal.ZERO
                every { transfersRepository.save(any()) } returns savedTransfer

                val result = service.createTransfer(transfer)

                Then("should return the created transfer id") {
                    result shouldBe 1
                }

                Then("should decrement sender wallet balance") {
                    senderWallet.balance shouldBe BigDecimal("900.00")
                }

                Then("should increment receiver wallet balance") {
                    receiverWallet.balance shouldBe BigDecimal("600.00")
                }

                Then("should save the transfer") {
                    verify { transfersRepository.save(any()) }
                }
            }
        }

        Given("a transfer with large amount") {
            val senderWallet = WalletFactory.create(id = 3, balance = BigDecimal("5000.00"))
            val receiverWallet = WalletFactory.create(id = 4, balance = BigDecimal("1000.00"))
            val category = CategoryFactory.create(id = 1)
            val transfer =
                TransferFactory.create(
                    senderWallet = senderWallet,
                    receiverWallet = receiverWallet,
                    amount = BigDecimal("2000.00"),
                    category = category,
                )
            val savedTransfer =
                TransferFactory.create(
                    id = 2,
                    senderWallet = senderWallet,
                    receiverWallet = receiverWallet,
                    amount = BigDecimal("2000.00"),
                    category = category,
                )

            When("creating transfer with large amount") {
                every { walletRepository.findById(3) } returns Optional.of(senderWallet)
                every { walletRepository.findById(4) } returns Optional.of(receiverWallet)
                every { walletRepository.getAllocatedBalanceByMasterWallet(3) } returns BigDecimal.ZERO
                every { walletRepository.getAllocatedBalanceByMasterWallet(4) } returns BigDecimal.ZERO
                every { transfersRepository.save(any()) } returns savedTransfer

                val result = service.createTransfer(transfer)

                Then("should return the created transfer id") {
                    result shouldBe 2
                }

                Then("should decrement sender wallet balance by full amount") {
                    senderWallet.balance shouldBe BigDecimal("3000.00")
                }

                Then("should increment receiver wallet balance by full amount") {
                    receiverWallet.balance shouldBe BigDecimal("3000.00")
                }
            }
        }

        Given("a transfer with decimal amount") {
            val senderWallet = WalletFactory.create(id = 5, balance = BigDecimal("1000.50"))
            val receiverWallet = WalletFactory.create(id = 6, balance = BigDecimal("500.75"))
            val category = CategoryFactory.create(id = 1)
            val transfer =
                TransferFactory.create(
                    senderWallet = senderWallet,
                    receiverWallet = receiverWallet,
                    amount = BigDecimal("150.25"),
                    category = category,
                )
            val savedTransfer =
                TransferFactory.create(
                    id = 3,
                    senderWallet = senderWallet,
                    receiverWallet = receiverWallet,
                    amount = BigDecimal("150.25"),
                    category = category,
                )

            When("creating transfer with decimal amount") {
                every { walletRepository.findById(5) } returns Optional.of(senderWallet)
                every { walletRepository.findById(6) } returns Optional.of(receiverWallet)
                every { walletRepository.getAllocatedBalanceByMasterWallet(5) } returns BigDecimal.ZERO
                every { walletRepository.getAllocatedBalanceByMasterWallet(6) } returns BigDecimal.ZERO
                every { transfersRepository.save(any()) } returns savedTransfer

                val result = service.createTransfer(transfer)

                Then("should return the created transfer id") {
                    result shouldBe 3
                }

                Then("should decrement sender wallet balance with exact decimal") {
                    senderWallet.balance shouldBe BigDecimal("850.25")
                }

                Then("should increment receiver wallet balance with exact decimal") {
                    receiverWallet.balance shouldBe BigDecimal("651.00")
                }
            }
        }

        Given("a transfer from non-existent sender wallet") {
            val senderWallet = WalletFactory.create(id = 999)
            val receiverWallet = WalletFactory.create(id = 7)
            val transfer =
                TransferFactory.create(
                    senderWallet = senderWallet,
                    receiverWallet = receiverWallet,
                    amount = BigDecimal("100.00"),
                )

            When("creating transfer from non-existent sender") {
                every { walletRepository.findById(999) } returns Optional.empty()

                Then("should throw EntityNotFoundException") {
                    shouldThrow<EntityNotFoundException> {
                        service.createTransfer(transfer)
                    }
                }
            }
        }

        Given("a transfer to non-existent receiver wallet") {
            val senderWallet = WalletFactory.create(id = 8, balance = BigDecimal("1000.00"))
            val receiverWallet = WalletFactory.create(id = 999)
            val transfer =
                TransferFactory.create(
                    senderWallet = senderWallet,
                    receiverWallet = receiverWallet,
                    amount = BigDecimal("100.00"),
                )

            When("creating transfer to non-existent receiver") {
                every { walletRepository.findById(8) } returns Optional.of(senderWallet)
                every { walletRepository.findById(999) } returns Optional.empty()

                Then("should throw EntityNotFoundException") {
                    shouldThrow<EntityNotFoundException> {
                        service.createTransfer(transfer)
                    }
                }
            }
        }

        Given("a transfer from master wallet to its virtual wallet") {
            val walletType = WalletTypeFactory.create(1, "Master")
            val masterWallet = WalletFactory.create(id = 9, name = "Master", balance = BigDecimal("1000.00"), type = walletType)
            val virtualWallet =
                WalletFactory.create(
                    id = 10,
                    name = "Virtual",
                    balance = BigDecimal("500.00"),
                    type = walletType,
                    masterWallet = masterWallet,
                )
            val transfer =
                TransferFactory.create(
                    senderWallet = masterWallet,
                    receiverWallet = virtualWallet,
                    amount = BigDecimal("100.00"),
                )

            When("creating transfer from master to its virtual wallet") {
                every { walletRepository.findById(9) } returns Optional.of(masterWallet)
                every { walletRepository.findById(10) } returns Optional.of(virtualWallet)

                Then("should throw IllegalStateException") {
                    shouldThrow<IllegalStateException> {
                        service.createTransfer(transfer)
                    }
                }
            }
        }

        Given("a transfer with insufficient sender balance") {
            val senderWallet = WalletFactory.create(id = 11, balance = BigDecimal("50.00"))
            val receiverWallet = WalletFactory.create(id = 12, balance = BigDecimal("500.00"))
            val transfer =
                TransferFactory.create(
                    senderWallet = senderWallet,
                    receiverWallet = receiverWallet,
                    amount = BigDecimal("100.00"),
                )

            When("creating transfer with insufficient balance") {
                every { walletRepository.findById(11) } returns Optional.of(senderWallet)
                every { walletRepository.findById(12) } returns Optional.of(receiverWallet)

                Then("should throw IllegalStateException") {
                    shouldThrow<IllegalStateException> {
                        service.createTransfer(transfer)
                    }
                }
            }
        }

        Given("a transfer from virtual wallet to another wallet") {
            val walletType = WalletTypeFactory.create(2, "Master")
            val masterWallet = WalletFactory.create(id = 13, name = "Master", balance = BigDecimal("2000.00"), type = walletType)
            val virtualWallet =
                WalletFactory.create(
                    id = 14,
                    name = "Virtual",
                    balance = BigDecimal("1000.00"),
                    type = walletType,
                    masterWallet = masterWallet,
                )
            val receiverWallet = WalletFactory.create(id = 15, balance = BigDecimal("500.00"))
            val category = CategoryFactory.create(id = 1)
            val transfer =
                TransferFactory.create(
                    senderWallet = virtualWallet,
                    receiverWallet = receiverWallet,
                    amount = BigDecimal("100.00"),
                    category = category,
                )
            val savedTransfer =
                TransferFactory.create(
                    id = 4,
                    senderWallet = virtualWallet,
                    receiverWallet = receiverWallet,
                    amount = BigDecimal("100.00"),
                    category = category,
                )

            When("creating transfer from virtual wallet") {
                every { walletRepository.findById(14) } returns Optional.of(virtualWallet)
                every { walletRepository.findById(15) } returns Optional.of(receiverWallet)
                every { walletRepository.getAllocatedBalanceByMasterWallet(14) } returns BigDecimal.ZERO
                every { walletRepository.getAllocatedBalanceByMasterWallet(15) } returns BigDecimal.ZERO
                every { transfersRepository.save(any()) } returns savedTransfer

                val result = service.createTransfer(transfer)

                Then("should return the created transfer id") {
                    result shouldBe 4
                }

                Then("should decrement virtual wallet balance") {
                    virtualWallet.balance shouldBe BigDecimal("900.00")
                }

                Then("should also decrement master wallet balance") {
                    masterWallet.balance shouldBe BigDecimal("1900.00")
                }

                Then("should increment receiver wallet balance") {
                    receiverWallet.balance shouldBe BigDecimal("600.00")
                }
            }
        }

        Given("a transfer to virtual wallet from another wallet") {
            val walletType = WalletTypeFactory.create(3, "Master")
            val masterWallet = WalletFactory.create(id = 16, name = "Master", balance = BigDecimal("1000.00"), type = walletType)
            val virtualWallet =
                WalletFactory.create(
                    id = 17,
                    name = "Virtual",
                    balance = BigDecimal("500.00"),
                    type = walletType,
                    masterWallet = masterWallet,
                )
            val senderWallet = WalletFactory.create(id = 18, balance = BigDecimal("1000.00"))
            val category = CategoryFactory.create(id = 1)
            val transfer =
                TransferFactory.create(
                    senderWallet = senderWallet,
                    receiverWallet = virtualWallet,
                    amount = BigDecimal("100.00"),
                    category = category,
                )
            val savedTransfer =
                TransferFactory.create(
                    id = 5,
                    senderWallet = senderWallet,
                    receiverWallet = virtualWallet,
                    amount = BigDecimal("100.00"),
                    category = category,
                )

            When("creating transfer to virtual wallet") {
                every { walletRepository.findById(18) } returns Optional.of(senderWallet)
                every { walletRepository.findById(17) } returns Optional.of(virtualWallet)
                every { walletRepository.getAllocatedBalanceByMasterWallet(18) } returns BigDecimal.ZERO
                every { walletRepository.getAllocatedBalanceByMasterWallet(17) } returns BigDecimal.ZERO
                every { transfersRepository.save(any()) } returns savedTransfer

                val result = service.createTransfer(transfer)

                Then("should return the created transfer id") {
                    result shouldBe 5
                }

                Then("should decrement sender wallet balance") {
                    senderWallet.balance shouldBe BigDecimal("900.00")
                }

                Then("should increment virtual wallet balance") {
                    virtualWallet.balance shouldBe BigDecimal("600.00")
                }

                Then("should also increment master wallet balance") {
                    masterWallet.balance shouldBe BigDecimal("1100.00")
                }
            }
        }

        Given("a transfer between two virtual wallets of different masters") {
            val walletType1 = WalletTypeFactory.create(4, "Master")
            val walletType2 = WalletTypeFactory.create(5, "Master")
            val masterWallet1 = WalletFactory.create(id = 19, name = "Master1", balance = BigDecimal("1000.00"), type = walletType1)
            val virtualWallet1 =
                WalletFactory.create(
                    id = 20,
                    name = "Virtual1",
                    balance = BigDecimal("500.00"),
                    type = walletType1,
                    masterWallet = masterWallet1,
                )
            val masterWallet2 = WalletFactory.create(id = 21, name = "Master2", balance = BigDecimal("1000.00"), type = walletType2)
            val virtualWallet2 =
                WalletFactory.create(
                    id = 22,
                    name = "Virtual2",
                    balance = BigDecimal("500.00"),
                    type = walletType2,
                    masterWallet = masterWallet2,
                )
            val category = CategoryFactory.create(id = 1)
            val transfer =
                TransferFactory.create(
                    senderWallet = virtualWallet1,
                    receiverWallet = virtualWallet2,
                    amount = BigDecimal("100.00"),
                    category = category,
                )
            val savedTransfer =
                TransferFactory.create(
                    id = 6,
                    senderWallet = virtualWallet1,
                    receiverWallet = virtualWallet2,
                    amount = BigDecimal("100.00"),
                    category = category,
                )

            When("creating transfer between virtual wallets of different masters") {
                every { walletRepository.findById(20) } returns Optional.of(virtualWallet1)
                every { walletRepository.findById(22) } returns Optional.of(virtualWallet2)
                every { walletRepository.getAllocatedBalanceByMasterWallet(20) } returns BigDecimal.ZERO
                every { walletRepository.getAllocatedBalanceByMasterWallet(22) } returns BigDecimal.ZERO
                every { transfersRepository.save(any()) } returns savedTransfer

                val result = service.createTransfer(transfer)

                Then("should return the created transfer id") {
                    result shouldBe 6
                }

                Then("should decrement sender virtual wallet balance") {
                    virtualWallet1.balance shouldBe BigDecimal("400.00")
                }

                Then("should decrement sender master wallet balance") {
                    masterWallet1.balance shouldBe BigDecimal("900.00")
                }

                Then("should increment receiver virtual wallet balance") {
                    virtualWallet2.balance shouldBe BigDecimal("600.00")
                }

                Then("should increment receiver master wallet balance") {
                    masterWallet2.balance shouldBe BigDecimal("1100.00")
                }
            }
        }

        Given("a transfer between two virtual wallets with same master") {
            val walletType = WalletTypeFactory.create(6, "Master")
            val masterWallet = WalletFactory.create(id = 23, name = "Master", balance = BigDecimal("2000.00"), type = walletType)
            val virtualWallet1 =
                WalletFactory.create(
                    id = 24,
                    name = "Virtual1",
                    balance = BigDecimal("1000.00"),
                    type = walletType,
                    masterWallet = masterWallet,
                )
            val virtualWallet2 =
                WalletFactory.create(
                    id = 25,
                    name = "Virtual2",
                    balance = BigDecimal("500.00"),
                    type = walletType,
                    masterWallet = masterWallet,
                )
            val category = CategoryFactory.create(id = 1)
            val transfer =
                TransferFactory.create(
                    senderWallet = virtualWallet1,
                    receiverWallet = virtualWallet2,
                    amount = BigDecimal("100.00"),
                    category = category,
                )
            val savedTransfer =
                TransferFactory.create(
                    id = 7,
                    senderWallet = virtualWallet1,
                    receiverWallet = virtualWallet2,
                    amount = BigDecimal("100.00"),
                    category = category,
                )

            When("creating transfer between virtual wallets with same master") {
                every { walletRepository.findById(24) } returns Optional.of(virtualWallet1)
                every { walletRepository.findById(25) } returns Optional.of(virtualWallet2)
                every { walletRepository.getAllocatedBalanceByMasterWallet(24) } returns BigDecimal.ZERO
                every { walletRepository.getAllocatedBalanceByMasterWallet(25) } returns BigDecimal.ZERO
                every { transfersRepository.save(any()) } returns savedTransfer

                val result = service.createTransfer(transfer)

                Then("should return the created transfer id") {
                    result shouldBe 7
                }

                Then("should decrement sender virtual wallet balance") {
                    virtualWallet1.balance shouldBe BigDecimal("900.00")
                }

                Then("should increment receiver virtual wallet balance") {
                    virtualWallet2.balance shouldBe BigDecimal("600.00")
                }

                Then("should keep master wallet balance unchanged") {
                    masterWallet.balance shouldBe BigDecimal("2000.00")
                }
            }
        }

        Given("a transfer with description and category") {
            val senderWallet = WalletFactory.create(id = 26, balance = BigDecimal("1000.00"))
            val receiverWallet = WalletFactory.create(id = 27, balance = BigDecimal("500.00"))
            val category = CategoryFactory.create(id = 2, name = "Transfers")
            val transfer =
                TransferFactory.create(
                    senderWallet = senderWallet,
                    receiverWallet = receiverWallet,
                    amount = BigDecimal("100.00"),
                    description = "Monthly allowance",
                    category = category,
                )
            val savedTransfer =
                TransferFactory.create(
                    id = 8,
                    senderWallet = senderWallet,
                    receiverWallet = receiverWallet,
                    amount = BigDecimal("100.00"),
                    description = "Monthly allowance",
                    category = category,
                )

            When("creating transfer with description and category") {
                every { walletRepository.findById(26) } returns Optional.of(senderWallet)
                every { walletRepository.findById(27) } returns Optional.of(receiverWallet)
                every { walletRepository.getAllocatedBalanceByMasterWallet(26) } returns BigDecimal.ZERO
                every { walletRepository.getAllocatedBalanceByMasterWallet(27) } returns BigDecimal.ZERO
                every { transfersRepository.save(any()) } returns savedTransfer

                val result = service.createTransfer(transfer)

                Then("should return the created transfer id") {
                    result shouldBe 8
                }

                Then("should save transfer with description") {
                    verify { transfersRepository.save(any()) }
                }
            }
        }

        Given("a transfer with exact sender balance") {
            val senderWallet = WalletFactory.create(id = 28, balance = BigDecimal("100.00"))
            val receiverWallet = WalletFactory.create(id = 29, balance = BigDecimal("500.00"))
            val category = CategoryFactory.create(id = 1)
            val transfer =
                TransferFactory.create(
                    senderWallet = senderWallet,
                    receiverWallet = receiverWallet,
                    amount = BigDecimal("100.00"),
                    category = category,
                )
            val savedTransfer =
                TransferFactory.create(
                    id = 9,
                    senderWallet = senderWallet,
                    receiverWallet = receiverWallet,
                    amount = BigDecimal("100.00"),
                    category = category,
                )

            When("creating transfer with exact sender balance") {
                every { walletRepository.findById(28) } returns Optional.of(senderWallet)
                every { walletRepository.findById(29) } returns Optional.of(receiverWallet)
                every { walletRepository.getAllocatedBalanceByMasterWallet(28) } returns BigDecimal.ZERO
                every { walletRepository.getAllocatedBalanceByMasterWallet(29) } returns BigDecimal.ZERO
                every { transfersRepository.save(any()) } returns savedTransfer

                val result = service.createTransfer(transfer)

                Then("should return the created transfer id") {
                    result shouldBe 9
                }

                Then("should reduce sender wallet balance to zero") {
                    senderWallet.balance.isZero() shouldBe true
                }

                Then("should increment receiver wallet balance") {
                    receiverWallet.balance shouldBe BigDecimal("600.00")
                }
            }
        }
    })
