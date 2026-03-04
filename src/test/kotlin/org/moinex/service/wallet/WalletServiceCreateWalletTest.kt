package org.moinex.service.wallet

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.moinex.factory.WalletFactory
import org.moinex.repository.wallettransaction.TransferRepository
import org.moinex.repository.wallettransaction.WalletRepository
import org.moinex.repository.wallettransaction.WalletTransactionRepository
import org.moinex.repository.wallettransaction.WalletTypeRepository
import org.moinex.service.WalletService
import java.math.BigDecimal

class WalletServiceCreateWalletTest :
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

        Given("a valid wallet with all required fields") {
            val wallet = WalletFactory.create(name = "My Savings")
            val savedWallet = WalletFactory.create(id = 1, name = "My Savings")

            When("creating the wallet") {
                every { walletRepository.existsByName("My Savings") } returns false
                every { walletRepository.save(any()) } returns savedWallet

                val result = service.createWallet(wallet)

                Then("should return the created wallet id") {
                    result shouldBe 1
                }

                Then("should call repository save method") {
                    verify { walletRepository.save(any()) }
                }
            }
        }

        Given("a valid wallet with custom balance") {
            val wallet = WalletFactory.create(name = "Investment Wallet", balance = BigDecimal("10000.00"))
            val savedWallet = WalletFactory.create(id = 2, name = "Investment Wallet", balance = BigDecimal("10000.00"))

            When("creating the wallet") {
                every { walletRepository.existsByName("Investment Wallet") } returns false
                every { walletRepository.save(any()) } returns savedWallet

                val result = service.createWallet(wallet)

                Then("should return the created wallet id") {
                    result shouldBe 2
                }

                Then("should preserve the wallet balance") {
                    verify { walletRepository.save(any()) }
                }
            }
        }

        Given("a wallet with a name that already exists") {
            val wallet = WalletFactory.create(name = "Existing Wallet")

            When("creating the wallet") {
                every { walletRepository.existsByName("Existing Wallet") } returns true

                Then("should throw IllegalStateException") {
                    shouldThrow<IllegalStateException> {
                        service.createWallet(wallet)
                    }
                }
            }
        }

        Given("a wallet with a duplicate name in different case") {
            val wallet = WalletFactory.create(name = "my wallet")

            When("creating the wallet") {
                every { walletRepository.existsByName("my wallet") } returns true

                Then("should throw IllegalStateException") {
                    shouldThrow<IllegalStateException> {
                        service.createWallet(wallet)
                    }
                }
            }
        }

        Given("multiple valid wallets") {
            When("creating multiple wallets sequentially") {
                val wallet1 = WalletFactory.create(name = "Wallet 1", balance = BigDecimal("1000.00"))
                val wallet2 = WalletFactory.create(name = "Wallet 2", balance = BigDecimal("2000.00"))
                val wallet3 = WalletFactory.create(name = "Wallet 3", balance = BigDecimal("3000.00"))

                val savedWallet1 = WalletFactory.create(id = 1, name = "Wallet 1", balance = BigDecimal("1000.00"))
                val savedWallet2 = WalletFactory.create(id = 2, name = "Wallet 2", balance = BigDecimal("2000.00"))
                val savedWallet3 = WalletFactory.create(id = 3, name = "Wallet 3", balance = BigDecimal("3000.00"))

                every { walletRepository.existsByName("Wallet 1") } returns false
                every { walletRepository.existsByName("Wallet 2") } returns false
                every { walletRepository.existsByName("Wallet 3") } returns false
                every { walletRepository.save(any()) } returnsMany
                    listOf(
                        savedWallet1,
                        savedWallet2,
                        savedWallet3,
                    )

                val result1 = service.createWallet(wallet1)
                val result2 = service.createWallet(wallet2)
                val result3 = service.createWallet(wallet3)

                Then("should return different ids for each wallet") {
                    result1 shouldBe 1
                    result2 shouldBe 2
                    result3 shouldBe 3
                    result1 shouldNotBe result2
                    result2 shouldNotBe result3
                }

                Then("should call save three times") {
                    verify(exactly = 3) { walletRepository.save(any()) }
                }
            }
        }

        Given("a wallet with zero balance") {
            val wallet = WalletFactory.create(name = "Empty Wallet", balance = BigDecimal.ZERO)
            val savedWallet = WalletFactory.create(id = 4, name = "Empty Wallet", balance = BigDecimal.ZERO)

            When("creating the wallet") {
                every { walletRepository.existsByName("Empty Wallet") } returns false
                every { walletRepository.save(any()) } returns savedWallet

                val result = service.createWallet(wallet)

                Then("should return the created wallet id") {
                    result shouldBe 4
                }

                Then("should preserve zero balance") {
                    verify { walletRepository.save(any()) }
                }
            }
        }
    })
