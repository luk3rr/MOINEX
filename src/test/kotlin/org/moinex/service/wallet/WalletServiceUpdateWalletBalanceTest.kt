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
import org.moinex.service.WalletService
import java.math.BigDecimal
import java.util.Optional

class WalletServiceUpdateWalletBalanceTest :
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

        Given("a wallet with balance that needs to be incremented") {
            val walletFromDb = WalletFactory.create(id = 1, name = "Wallet", balance = BigDecimal("1000.00"))
            val updatedWallet = WalletFactory.create(id = 1, name = "Wallet", balance = BigDecimal("1500.00"))

            When("updating wallet balance with higher value") {
                every { walletRepository.findById(1) } returns Optional.of(walletFromDb)

                service.updateWalletBalance(updatedWallet)

                Then("should increment wallet balance") {
                    walletFromDb.balance shouldBe BigDecimal("1500.00")
                }
            }
        }

        Given("a wallet with balance that needs to be decremented") {
            val walletFromDb = WalletFactory.create(id = 2, name = "Wallet", balance = BigDecimal("2000.00"))
            val updatedWallet = WalletFactory.create(id = 2, name = "Wallet", balance = BigDecimal("1500.00"))

            When("updating wallet balance with lower value") {
                every { walletRepository.findById(2) } returns Optional.of(walletFromDb)
                every { walletRepository.getAllocatedBalanceByMasterWallet(2) } returns BigDecimal.ZERO

                service.updateWalletBalance(updatedWallet)

                Then("should decrement wallet balance") {
                    walletFromDb.balance shouldBe BigDecimal("1500.00")
                }
            }
        }

        Given("a wallet with balance that remains unchanged") {
            val walletFromDb = WalletFactory.create(id = 3, name = "Wallet", balance = BigDecimal("1000.00"))
            val updatedWallet = WalletFactory.create(id = 3, name = "Wallet", balance = BigDecimal("1000.00"))

            When("updating wallet with same balance") {
                every { walletRepository.findById(3) } returns Optional.of(walletFromDb)

                service.updateWalletBalance(updatedWallet)

                Then("should keep balance unchanged") {
                    walletFromDb.balance shouldBe BigDecimal("1000.00")
                }
            }
        }

        Given("a master wallet being decremented") {
            val walletFromDb = WalletFactory.create(id = 4, name = "Master Wallet", balance = BigDecimal("5000.00"))
            val updatedWallet = WalletFactory.create(id = 4, name = "Master Wallet", balance = BigDecimal("4000.00"))

            When("decrementing master wallet with sufficient unallocated balance") {
                every { walletRepository.findById(4) } returns Optional.of(walletFromDb)
                every { walletRepository.getAllocatedBalanceByMasterWallet(4) } returns BigDecimal("500.00")

                service.updateWalletBalance(updatedWallet)

                Then("should decrement wallet balance") {
                    walletFromDb.balance shouldBe BigDecimal("4000.00")
                }

                Then("should verify unallocated balance") {
                    verify { walletRepository.getAllocatedBalanceByMasterWallet(4) }
                }
            }
        }

        Given("a master wallet being decremented below unallocated balance") {
            val walletFromDb = WalletFactory.create(id = 5, name = "Master Wallet", balance = BigDecimal("5000.00"))
            val updatedWallet = WalletFactory.create(id = 5, name = "Master Wallet", balance = BigDecimal("2499.00"))

            When("decrementing master wallet below unallocated balance") {
                every { walletRepository.findById(5) } returns Optional.of(walletFromDb)
                every { walletRepository.getAllocatedBalanceByMasterWallet(5) } returns BigDecimal("2500.00")

                Then("should throw IllegalStateException") {
                    shouldThrow<IllegalStateException> {
                        service.updateWalletBalance(updatedWallet)
                    }
                }

                Then("should not modify wallet balance") {
                    walletFromDb.balance shouldBe BigDecimal("5000.00")
                }
            }
        }

        Given("a virtual wallet being incremented") {
            val walletType = WalletType(1, "Master")
            val masterWallet = Wallet(6, walletType, "Master Wallet", BigDecimal("5000.00"), masterWallet = null)
            val virtualWallet = Wallet(7, walletType, "Virtual Wallet", BigDecimal("1000.00"), masterWallet = masterWallet)
            val updatedVirtual = Wallet(7, walletType, "Virtual Wallet", BigDecimal("1500.00"), masterWallet = masterWallet)

            When("incrementing virtual wallet") {
                every { walletRepository.findById(7) } returns Optional.of(virtualWallet)

                service.updateWalletBalance(updatedVirtual)

                Then("should increment virtual wallet balance") {
                    virtualWallet.balance shouldBe BigDecimal("1500.00")
                }

                Then("should increment master wallet balance") {
                    masterWallet.balance shouldBe BigDecimal("5500.00")
                }
            }
        }

        Given("a virtual wallet being decremented") {
            val walletType = WalletType(1, "Master")
            val masterWallet = Wallet(8, walletType, "Master Wallet", BigDecimal("5000.00"), masterWallet = null)
            val virtualWallet = Wallet(9, walletType, "Virtual Wallet", BigDecimal("2000.00"), masterWallet = masterWallet)
            val updatedVirtual = Wallet(9, walletType, "Virtual Wallet", BigDecimal("1500.00"), masterWallet = masterWallet)

            When("decrementing virtual wallet") {
                every { walletRepository.findById(9) } returns Optional.of(virtualWallet)

                service.updateWalletBalance(updatedVirtual)

                Then("should decrement virtual wallet balance") {
                    virtualWallet.balance shouldBe BigDecimal("1500.00")
                }

                Then("should decrement master wallet balance") {
                    masterWallet.balance shouldBe BigDecimal("4500.00")
                }
            }
        }

        Given("a virtual wallet being decremented below zero") {
            val walletType = WalletType(1, "Master")
            val masterWallet = Wallet(10, walletType, "Master Wallet", BigDecimal("5000.00"), masterWallet = null)
            val virtualWallet = Wallet(11, walletType, "Virtual Wallet", BigDecimal("500.00"), masterWallet = masterWallet)
            val updatedVirtual = Wallet(11, walletType, "Virtual Wallet", BigDecimal("-100.00"), masterWallet = masterWallet)

            When("decrementing virtual wallet below zero") {
                every { walletRepository.findById(11) } returns Optional.of(virtualWallet)

                Then("should throw IllegalStateException") {
                    shouldThrow<IllegalStateException> {
                        service.updateWalletBalance(updatedVirtual)
                    }
                }

                Then("should not modify virtual wallet balance") {
                    virtualWallet.balance shouldBe BigDecimal("500.00")
                }

                Then("should not modify master wallet balance") {
                    masterWallet.balance shouldBe BigDecimal("5000.00")
                }
            }
        }

        Given("a non-existent wallet") {
            val updatedWallet = WalletFactory.create(id = 999, name = "Wallet", balance = BigDecimal("1000.00"))

            When("updating non-existent wallet") {
                every { walletRepository.findById(999) } returns Optional.empty()

                Then("should throw EntityNotFoundException") {
                    shouldThrow<EntityNotFoundException> {
                        service.updateWalletBalance(updatedWallet)
                    }
                }
            }
        }

        Given("a master wallet being incremented") {
            val walletFromDb = WalletFactory.create(id = 12, name = "Master Wallet", balance = BigDecimal("5000.00"))
            val updatedWallet = WalletFactory.create(id = 12, name = "Master Wallet", balance = BigDecimal("6000.00"))

            When("incrementing master wallet") {
                every { walletRepository.findById(12) } returns Optional.of(walletFromDb)

                service.updateWalletBalance(updatedWallet)

                Then("should increment master wallet balance") {
                    walletFromDb.balance shouldBe BigDecimal("6000.00")
                }

                Then("should not check unallocated balance") {
                    verify(exactly = 0) { walletRepository.getAllocatedBalanceByMasterWallet(any()) }
                }
            }
        }

        Given("a large balance increment") {
            val walletFromDb = WalletFactory.create(id = 13, name = "Wallet", balance = BigDecimal("1000.00"))
            val updatedWallet = WalletFactory.create(id = 13, name = "Wallet", balance = BigDecimal("1000000.00"))

            When("incrementing wallet with large amount") {
                every { walletRepository.findById(13) } returns Optional.of(walletFromDb)

                service.updateWalletBalance(updatedWallet)

                Then("should handle large balance increment") {
                    walletFromDb.balance shouldBe BigDecimal("1000000.00")
                }
            }
        }

        Given("a small balance decrement") {
            val walletFromDb = WalletFactory.create(id = 14, name = "Wallet", balance = BigDecimal("100.00"))
            val updatedWallet = WalletFactory.create(id = 14, name = "Wallet", balance = BigDecimal("99.99"))

            When("decrementing wallet with small amount") {
                every { walletRepository.findById(14) } returns Optional.of(walletFromDb)
                every { walletRepository.getAllocatedBalanceByMasterWallet(14) } returns BigDecimal.ZERO

                service.updateWalletBalance(updatedWallet)

                Then("should handle small balance decrement") {
                    walletFromDb.balance shouldBe BigDecimal("99.99")
                }
            }
        }
    })
