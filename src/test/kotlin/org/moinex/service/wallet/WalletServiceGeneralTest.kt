package org.moinex.service.wallet

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import jakarta.persistence.EntityNotFoundException
import org.moinex.factory.WalletFactory
import org.moinex.factory.WalletTypeFactory
import org.moinex.repository.wallettransaction.TransferRepository
import org.moinex.repository.wallettransaction.WalletRepository
import org.moinex.repository.wallettransaction.WalletTransactionRepository
import org.moinex.repository.wallettransaction.WalletTypeRepository
import org.moinex.service.WalletService
import java.math.BigDecimal
import java.util.Optional

class WalletServiceGeneralTest :
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

        Given("an archived wallet") {
            val wallet = WalletFactory.create(id = 1, name = "Archived Wallet", isArchived = true)

            When("unarchiving the wallet") {
                every { walletRepository.findById(1) } returns Optional.of(wallet)

                service.unarchiveWallet(1)

                Then("should set isArchived to false") {
                    wallet.isArchived shouldBe false
                }
            }
        }

        Given("a non-archived wallet") {
            val wallet = WalletFactory.create(id = 2, name = "Active Wallet", isArchived = false)

            When("unarchiving a non-archived wallet") {
                every { walletRepository.findById(2) } returns Optional.of(wallet)

                service.unarchiveWallet(2)

                Then("should remain non-archived") {
                    wallet.isArchived shouldBe false
                }
            }
        }

        Given("a non-existent wallet to unarchive") {
            When("unarchiving a non-existent wallet") {
                every { walletRepository.findById(999) } returns Optional.empty()

                Then("should throw EntityNotFoundException") {
                    shouldThrow<EntityNotFoundException> {
                        service.unarchiveWallet(999)
                    }
                }
            }
        }

        Given("a wallet with a valid new name") {
            val wallet = WalletFactory.create(id = 3, name = "Old Name")
            val updatedWallet = WalletFactory.create(id = 3, name = "New Name")

            When("renaming the wallet") {
                every { walletRepository.findById(3) } returns Optional.of(wallet)
                every { walletRepository.existsByNameAndIdNot("New Name", 3) } returns false

                service.renameWallet(updatedWallet)

                Then("should update wallet name") {
                    wallet.name shouldBe "New Name"
                }
            }
        }

        Given("a wallet with a name that already exists") {
            val wallet = WalletFactory.create(id = 4, name = "Wallet A")
            val updatedWallet = WalletFactory.create(id = 4, name = "Wallet B")

            When("attempting to rename wallet to an existing name") {
                every { walletRepository.findById(4) } returns Optional.of(wallet)
                every { walletRepository.existsByNameAndIdNot("Wallet B", 4) } returns true

                Then("should throw IllegalStateException") {
                    shouldThrow<IllegalStateException> {
                        service.renameWallet(updatedWallet)
                    }
                }
            }
        }

        Given("a non-existent wallet to rename") {
            val updatedWallet = WalletFactory.create(id = 999, name = "New Name")

            When("renaming a non-existent wallet") {
                every { walletRepository.findById(999) } returns Optional.empty()

                Then("should throw EntityNotFoundException") {
                    shouldThrow<EntityNotFoundException> {
                        service.renameWallet(updatedWallet)
                    }
                }
            }
        }

        Given("a wallet renaming to the same name") {
            val wallet = WalletFactory.create(id = 5, name = "Same Name")
            val updatedWallet = WalletFactory.create(id = 5, name = "Same Name")

            When("renaming wallet to its current name") {
                every { walletRepository.findById(5) } returns Optional.of(wallet)
                every { walletRepository.existsByNameAndIdNot("Same Name", 5) } returns false

                service.renameWallet(updatedWallet)

                Then("should keep the same name") {
                    wallet.name shouldBe "Same Name"
                }
            }
        }

        Given("a wallet with a valid wallet type") {
            val oldType = WalletTypeFactory.create(1, "Savings")
            val newType = WalletTypeFactory.create(2, "Checking")
            val wallet = WalletFactory.create(id = 6, name = "My Wallet", type = oldType)
            val updatedWallet = WalletFactory.create(id = 6, name = "My Wallet", type = newType)

            When("changing wallet type") {
                every { walletRepository.findById(6) } returns Optional.of(wallet)
                every { walletTypeRepository.existsById(2) } returns true

                service.changeWalletType(updatedWallet)

                Then("should update wallet type") {
                    wallet.type.id shouldBe 2
                }
            }
        }

        Given("a wallet with a non-existent wallet type") {
            val currentType = WalletTypeFactory.create(1, "Savings")
            val wallet = WalletFactory.create(id = 7, name = "My Wallet", type = currentType)
            val updatedWallet = WalletFactory.create(id = 7, name = "My Wallet", type = WalletTypeFactory.create(999, "Invalid"))

            When("attempting to change to non-existent wallet type") {
                every { walletRepository.findById(7) } returns Optional.of(wallet)
                every { walletTypeRepository.existsById(999) } returns false

                Then("should throw IllegalStateException") {
                    shouldThrow<IllegalStateException> {
                        service.changeWalletType(updatedWallet)
                    }
                }
            }
        }

        Given("a non-existent wallet to change type") {
            val newType = WalletTypeFactory.create(3, "Investment")
            val updatedWallet = WalletFactory.create(id = 999, name = "Non-existent", type = newType)

            When("changing type of non-existent wallet") {
                every { walletRepository.findById(999) } returns Optional.empty()

                Then("should throw EntityNotFoundException") {
                    shouldThrow<EntityNotFoundException> {
                        service.changeWalletType(updatedWallet)
                    }
                }
            }
        }

        Given("a wallet changing to the same type") {
            val type = WalletTypeFactory.create(4, "Current Type")
            val wallet = WalletFactory.create(id = 8, name = "My Wallet", type = type)
            val updatedWallet = WalletFactory.create(id = 8, name = "My Wallet", type = type)

            When("changing wallet to its current type") {
                every { walletRepository.findById(8) } returns Optional.of(wallet)
                every { walletTypeRepository.existsById(4) } returns true

                service.changeWalletType(updatedWallet)

                Then("should keep the same type") {
                    wallet.type.id shouldBe 4
                }
            }
        }

        Given("multiple non-archived wallets with different transaction counts") {
            val wallet1 = WalletFactory.create(id = 9, name = "Wallet 1", isArchived = false)
            val wallet2 = WalletFactory.create(id = 10, name = "Wallet 2", isArchived = false)
            val wallet3 = WalletFactory.create(id = 11, name = "Wallet 3", isArchived = false)

            When("getting non-archived wallets ordered by transaction count descending") {
                every { walletRepository.findAllByIsArchivedFalse() } returns listOf(wallet1, wallet2, wallet3)
                every { walletTransactionRepository.getTransactionCountByWallet(9) } returns 5
                every { transfersRepository.getTransferCountByWallet(9) } returns 2
                every { walletTransactionRepository.getTransactionCountByWallet(10) } returns 8
                every { transfersRepository.getTransferCountByWallet(10) } returns 4
                every { walletTransactionRepository.getTransactionCountByWallet(11) } returns 3
                every { transfersRepository.getTransferCountByWallet(11) } returns 1

                val result = service.getAllNonArchivedWalletsOrderedByTransactionCountDesc()

                Then("should return wallets ordered by total transaction count descending") {
                    result.size shouldBe 3
                    result[0].id shouldBe 10  // 8 + 4 = 12
                    result[1].id shouldBe 9   // 5 + 2 = 7
                    result[2].id shouldBe 11  // 3 + 1 = 4
                }
            }
        }

        Given("wallets with same transaction counts") {
            val wallet1 = WalletFactory.create(id = 12, name = "Wallet 1", isArchived = false)
            val wallet2 = WalletFactory.create(id = 13, name = "Wallet 2", isArchived = false)

            When("getting wallets with equal transaction counts") {
                every { walletRepository.findAllByIsArchivedFalse() } returns listOf(wallet1, wallet2)
                every { walletTransactionRepository.getTransactionCountByWallet(12) } returns 5
                every { transfersRepository.getTransferCountByWallet(12) } returns 0
                every { walletTransactionRepository.getTransactionCountByWallet(13) } returns 5
                every { transfersRepository.getTransferCountByWallet(13) } returns 0

                val result = service.getAllNonArchivedWalletsOrderedByTransactionCountDesc()

                Then("should return both wallets") {
                    result.size shouldBe 2
                }
            }
        }

        Given("wallets with zero transactions") {
            val wallet1 = WalletFactory.create(id = 14, name = "Empty Wallet 1", isArchived = false)
            val wallet2 = WalletFactory.create(id = 15, name = "Empty Wallet 2", isArchived = false)

            When("getting wallets with no transactions") {
                every { walletRepository.findAllByIsArchivedFalse() } returns listOf(wallet1, wallet2)
                every { walletTransactionRepository.getTransactionCountByWallet(14) } returns 0
                every { transfersRepository.getTransferCountByWallet(14) } returns 0
                every { walletTransactionRepository.getTransactionCountByWallet(15) } returns 0
                every { transfersRepository.getTransferCountByWallet(15) } returns 0

                val result = service.getAllNonArchivedWalletsOrderedByTransactionCountDesc()

                Then("should return all wallets") {
                    result.size shouldBe 2
                }
            }
        }

        Given("only archived wallets") {
            When("getting non-archived wallets when none exist") {
                every { walletRepository.findAllByIsArchivedFalse() } returns emptyList()

                val result = service.getAllNonArchivedWalletsOrderedByTransactionCountDesc()

                Then("should return empty list") {
                    result.isEmpty() shouldBe true
                }
            }
        }

        Given("single non-archived wallet") {
            val wallet = WalletFactory.create(id = 16, name = "Single Wallet", isArchived = false)

            When("getting single non-archived wallet") {
                every { walletRepository.findAllByIsArchivedFalse() } returns listOf(wallet)
                every { walletTransactionRepository.getTransactionCountByWallet(16) } returns 10
                every { transfersRepository.getTransferCountByWallet(16) } returns 5

                val result = service.getAllNonArchivedWalletsOrderedByTransactionCountDesc()

                Then("should return the single wallet") {
                    result.size shouldBe 1
                    result[0].id shouldBe 16
                }
            }
        }

        Given("wallets with only transactions and no transfers") {
            val wallet1 = WalletFactory.create(id = 17, name = "Wallet 1", isArchived = false)
            val wallet2 = WalletFactory.create(id = 18, name = "Wallet 2", isArchived = false)

            When("getting wallets with transactions but no transfers") {
                every { walletRepository.findAllByIsArchivedFalse() } returns listOf(wallet1, wallet2)
                every { walletTransactionRepository.getTransactionCountByWallet(17) } returns 7
                every { transfersRepository.getTransferCountByWallet(17) } returns 0
                every { walletTransactionRepository.getTransactionCountByWallet(18) } returns 3
                every { transfersRepository.getTransferCountByWallet(18) } returns 0

                val result = service.getAllNonArchivedWalletsOrderedByTransactionCountDesc()

                Then("should order by transaction count") {
                    result[0].id shouldBe 17
                    result[1].id shouldBe 18
                }
            }
        }

        Given("wallets with only transfers and no transactions") {
            val wallet1 = WalletFactory.create(id = 19, name = "Wallet 1", isArchived = false)
            val wallet2 = WalletFactory.create(id = 20, name = "Wallet 2", isArchived = false)

            When("getting wallets with transfers but no transactions") {
                every { walletRepository.findAllByIsArchivedFalse() } returns listOf(wallet1, wallet2)
                every { walletTransactionRepository.getTransactionCountByWallet(19) } returns 0
                every { transfersRepository.getTransferCountByWallet(19) } returns 6
                every { walletTransactionRepository.getTransactionCountByWallet(20) } returns 0
                every { transfersRepository.getTransferCountByWallet(20) } returns 2

                val result = service.getAllNonArchivedWalletsOrderedByTransactionCountDesc()

                Then("should order by transfer count") {
                    result[0].id shouldBe 19
                    result[1].id shouldBe 20
                }
            }
        }
    })
