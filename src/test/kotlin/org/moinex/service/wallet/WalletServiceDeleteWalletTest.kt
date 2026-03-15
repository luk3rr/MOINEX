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
import org.moinex.model.wallettransaction.Wallet
import org.moinex.model.wallettransaction.WalletType
import org.moinex.repository.wallettransaction.TransferRepository
import org.moinex.repository.wallettransaction.WalletRepository
import org.moinex.repository.wallettransaction.WalletTransactionRepository
import org.moinex.repository.wallettransaction.WalletTypeRepository
import org.moinex.service.wallet.WalletService
import java.math.BigDecimal
import java.util.Optional

class WalletServiceDeleteWalletTest :
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

        Given("a master wallet without transactions or transfers") {
            val wallet = WalletFactory.create(id = 1, name = "Empty Wallet")

            When("deleting the master wallet with no virtual wallets") {
                every { walletRepository.findById(1) } returns Optional.of(wallet)
                every { walletTransactionRepository.getTransactionCountByWallet(1) } returns 0
                every { transfersRepository.getTransferCountByWallet(1) } returns 0
                every { walletRepository.findVirtualWalletsByMasterWallet(1) } returns emptyList()
                every { walletRepository.delete(wallet) } returns Unit

                service.deleteWallet(1)

                Then("should call repository delete method") {
                    verify { walletRepository.delete(wallet) }
                }

                Then("should verify transaction count") {
                    verify { walletTransactionRepository.getTransactionCountByWallet(1) }
                }

                Then("should verify transfer count") {
                    verify { transfersRepository.getTransferCountByWallet(1) }
                }

                Then("should check for master wallets") {
                    verify { walletRepository.findVirtualWalletsByMasterWallet(1) }
                }
            }
        }

        Given("a master wallet with pending transactions") {
            val wallet = WalletFactory.create(id = 2, name = "Wallet with Transactions")

            When("deleting the master wallet") {
                every { walletRepository.findById(2) } returns Optional.of(wallet)
                every { walletTransactionRepository.getTransactionCountByWallet(2) } returns 5
                every { transfersRepository.getTransferCountByWallet(2) } returns 0

                Then("should throw IllegalStateException") {
                    shouldThrow<IllegalStateException> {
                        service.deleteWallet(2)
                    }
                }

                Then("should not call delete method") {
                    verify(exactly = 0) { walletRepository.delete(any()) }
                }
            }
        }

        Given("a master wallet with pending transfers") {
            val wallet = WalletFactory.create(id = 3, name = "Wallet with Transfers")

            When("deleting the master wallet") {
                every { walletRepository.findById(3) } returns Optional.of(wallet)
                every { walletTransactionRepository.getTransactionCountByWallet(3) } returns 0
                every { transfersRepository.getTransferCountByWallet(3) } returns 2

                Then("should throw IllegalStateException") {
                    shouldThrow<IllegalStateException> {
                        service.deleteWallet(3)
                    }
                }

                Then("should not call delete method") {
                    verify(exactly = 0) { walletRepository.delete(any()) }
                }
            }
        }

        Given("a master wallet with both transactions and transfers") {
            val wallet = WalletFactory.create(id = 4, name = "Wallet with Both")

            When("deleting the master wallet") {
                every { walletRepository.findById(4) } returns Optional.of(wallet)
                every { walletTransactionRepository.getTransactionCountByWallet(4) } returns 3
                every { transfersRepository.getTransferCountByWallet(4) } returns 1
                every { walletRepository.findVirtualWalletsByMasterWallet(4) } returns emptyList()

                Then("should throw IllegalStateException") {
                    shouldThrow<IllegalStateException> {
                        service.deleteWallet(4)
                    }
                }

                Then("should not call delete method") {
                    verify(exactly = 0) { walletRepository.delete(any()) }
                }
            }
        }

        Given("a master wallet with associated virtual wallets") {
            val walletType = WalletType(1, "Master")
            val masterWallet = Wallet(6, walletType, "Master Wallet", BigDecimal("5000.00"), masterWallet = null)
            val virtualWallet1 = Wallet(7, walletType, "Virtual 1", BigDecimal("1000.00"), masterWallet = masterWallet)
            val virtualWallet2 = Wallet(8, walletType, "Virtual 2", BigDecimal("2000.00"), masterWallet = masterWallet)

            When("deleting the master wallet") {
                every { walletRepository.findById(6) } returns Optional.of(masterWallet)
                every { walletTransactionRepository.getTransactionCountByWallet(6) } returns 0
                every { transfersRepository.getTransferCountByWallet(6) } returns 0
                every { walletRepository.findVirtualWalletsByMasterWallet(6) } returns
                    listOf(virtualWallet1, virtualWallet2)
                every { walletRepository.delete(masterWallet) } returns Unit

                service.deleteWallet(6)

                Then("should call repository delete method") {
                    verify { walletRepository.delete(masterWallet) }
                }

                Then("should unlink virtual wallets from master") {
                    verify { walletRepository.findVirtualWalletsByMasterWallet(6) }

                    virtualWallet1.masterWallet shouldBe null
                    virtualWallet2.masterWallet shouldBe null
                }
            }
        }

        Given("a virtual wallet without transactions or transfers") {
            val walletType = WalletType(1, "Master")
            val masterWallet = Wallet(5, walletType, "Master Wallet", BigDecimal("5000.00"), masterWallet = null)
            val virtualWallet = Wallet(50, walletType, "Virtual Wallet", BigDecimal("1000.00"), masterWallet = masterWallet)

            When("deleting the virtual wallet") {
                every { walletRepository.findById(50) } returns Optional.of(virtualWallet)
                every { walletTransactionRepository.getTransactionCountByWallet(50) } returns 0
                every { transfersRepository.getTransferCountByWallet(50) } returns 0
                every { walletRepository.delete(virtualWallet) } returns Unit

                service.deleteWallet(50)

                Then("should call repository delete method") {
                    verify { walletRepository.delete(virtualWallet) }
                }

                Then("should not check for virtual wallets") {
                    verify(exactly = 0) { walletRepository.findVirtualWalletsByMasterWallet(any()) }
                }
            }
        }

        Given("a non-existent wallet") {
            When("deleting the wallet") {
                every { walletRepository.findById(999) } returns Optional.empty()

                Then("should throw EntityNotFoundException") {
                    shouldThrow<EntityNotFoundException> {
                        service.deleteWallet(999)
                    }
                }
            }
        }
    })
