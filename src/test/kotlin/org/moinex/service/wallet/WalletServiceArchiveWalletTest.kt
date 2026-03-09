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
import org.moinex.factory.wallet.WalletFactory
import org.moinex.model.wallettransaction.Wallet
import org.moinex.model.wallettransaction.WalletType
import org.moinex.repository.wallettransaction.TransferRepository
import org.moinex.repository.wallettransaction.WalletRepository
import org.moinex.repository.wallettransaction.WalletTransactionRepository
import org.moinex.repository.wallettransaction.WalletTypeRepository
import org.moinex.service.WalletService
import java.math.BigDecimal
import java.util.Optional

class WalletServiceArchiveWalletTest :
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

        Given("a non-archived master wallet without virtual wallets") {
            val wallet = WalletFactory.create(id = 1, name = "Master Wallet", balance = BigDecimal("5000.00"))

            When("archiving the master wallet") {
                every { walletRepository.findById(1) } returns Optional.of(wallet)
                every { walletRepository.getAllocatedBalanceByMasterWallet(1) } returns BigDecimal.ZERO
                every { walletRepository.findVirtualWalletsByMasterWallet(1) } returns emptyList()

                service.archiveWallet(1)

                Then("should set isArchived to true") {
                    wallet.isArchived shouldBe true
                }

                Then("should calculate unallocated balance") {
                    verify { walletRepository.getAllocatedBalanceByMasterWallet(1) }
                }

                Then("should check virtual wallets from master") {
                    verify { walletRepository.findVirtualWalletsByMasterWallet(1) }
                }
            }
        }

        Given("a non-archived master wallet with virtual wallets") {
            val walletType = WalletType(1, "Master")
            val masterWallet = Wallet(2, walletType, "Master Wallet", BigDecimal("5000.00"), masterWallet = null)
            val virtualWallet1 = Wallet(3, walletType, "Virtual 1", BigDecimal("1000.00"), masterWallet = masterWallet)
            val virtualWallet2 = Wallet(4, walletType, "Virtual 2", BigDecimal("2000.00"), masterWallet = masterWallet)

            When("archiving the master wallet") {
                every { walletRepository.findById(2) } returns Optional.of(masterWallet)
                every { walletRepository.getAllocatedBalanceByMasterWallet(2) } returns BigDecimal("3000.00")
                every { walletRepository.findVirtualWalletsByMasterWallet(2) } returns
                    listOf(virtualWallet1, virtualWallet2)

                service.archiveWallet(2)

                Then("should set isArchived to true") {
                    masterWallet.isArchived shouldBe true
                }

                Then("should update balance to unallocated balance") {
                    masterWallet.balance shouldBe BigDecimal("2000.00")
                }

                Then("should unlink virtual wallets from master") {
                    virtualWallet1.masterWallet shouldBe null
                    virtualWallet2.masterWallet shouldBe null
                }

                Then("should verify virtual wallets were found") {
                    verify { walletRepository.findVirtualWalletsByMasterWallet(2) }
                }
            }
        }

        Given("a non-archived virtual wallet") {
            val walletType = WalletType(1, "Master")
            val masterWallet = Wallet(5, walletType, "Master Wallet", BigDecimal("5000.00"), masterWallet = null)
            val virtualWallet = Wallet(6, walletType, "Virtual Wallet", BigDecimal("1000.00"), masterWallet = masterWallet)

            When("archiving the virtual wallet") {
                every { walletRepository.findById(6) } returns Optional.of(virtualWallet)

                service.archiveWallet(6)

                Then("should set isArchived to true") {
                    virtualWallet.isArchived shouldBe true
                }

                Then("should unlink from master wallet") {
                    virtualWallet.masterWallet shouldBe null
                }

                Then("should not check for virtual wallets") {
                    verify(exactly = 0) { walletRepository.findVirtualWalletsByMasterWallet(any()) }
                }

                Then("should not check allocated balance") {
                    verify(exactly = 0) { walletRepository.getAllocatedBalanceByMasterWallet(any()) }
                }
            }
        }

        Given("a non-existent wallet") {
            When("archiving the wallet") {
                every { walletRepository.findById(999) } returns Optional.empty()

                Then("should throw EntityNotFoundException") {
                    shouldThrow<EntityNotFoundException> {
                        service.archiveWallet(999)
                    }
                }
            }
        }

        Given("a master wallet with full allocated balance") {
            val wallet = WalletFactory.create(id = 7, name = "Master Wallet", balance = BigDecimal("5000.00"))

            When("archiving the master wallet when all balance is allocated") {
                every { walletRepository.findById(7) } returns Optional.of(wallet)
                every { walletRepository.getAllocatedBalanceByMasterWallet(7) } returns BigDecimal("5000.00")
                every { walletRepository.findVirtualWalletsByMasterWallet(7) } returns emptyList()

                service.archiveWallet(7)

                Then("should set balance to zero") {
                    wallet.balance.isZero() shouldBe true
                }

                Then("should set isArchived to true") {
                    wallet.isArchived shouldBe true
                }
            }
        }

        Given("a master wallet with partial allocated balance") {
            val wallet = WalletFactory.create(id = 8, name = "Master Wallet", balance = BigDecimal("10000.00"))

            When("archiving the master wallet with partial allocation") {
                every { walletRepository.findById(8) } returns Optional.of(wallet)
                every { walletRepository.getAllocatedBalanceByMasterWallet(8) } returns BigDecimal("6000.00")
                every { walletRepository.findVirtualWalletsByMasterWallet(8) } returns emptyList()

                service.archiveWallet(8)

                Then("should set balance to unallocated amount") {
                    wallet.balance shouldBe BigDecimal("4000.00")
                }

                Then("should set isArchived to true") {
                    wallet.isArchived shouldBe true
                }
            }
        }

        Given("an already archived wallet") {
            val wallet = WalletFactory.create(id = 9, name = "Archived Wallet")
            wallet.isArchived = true

            When("archiving an already archived wallet") {
                every { walletRepository.findById(9) } returns Optional.of(wallet)
                every { walletRepository.getAllocatedBalanceByMasterWallet(9) } returns BigDecimal.ZERO
                every { walletRepository.findVirtualWalletsByMasterWallet(9) } returns emptyList()

                service.archiveWallet(9)

                Then("should remain archived") {
                    wallet.isArchived shouldBe true
                }
            }
        }
    })
