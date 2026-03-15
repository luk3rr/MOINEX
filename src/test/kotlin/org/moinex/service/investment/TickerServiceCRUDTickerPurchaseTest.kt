package org.moinex.service.investment

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import jakarta.persistence.EntityNotFoundException
import org.moinex.factory.CategoryFactory
import org.moinex.factory.investment.TickerFactory
import org.moinex.factory.investment.TickerPurchaseFactory
import org.moinex.factory.wallet.WalletFactory
import org.moinex.factory.wallet.WalletTransactionFactory
import org.moinex.model.dto.WalletTransactionContextDTO
import org.moinex.model.enums.WalletTransactionStatus
import org.moinex.repository.investment.CryptoExchangeRepository
import org.moinex.repository.investment.DividendRepository
import org.moinex.repository.investment.TickerPurchaseRepository
import org.moinex.repository.investment.TickerRepository
import org.moinex.repository.investment.TickerSaleRepository
import org.moinex.service.wallet.WalletService
import java.math.BigDecimal
import java.time.LocalDateTime
import java.util.Optional

class TickerServiceCRUDTickerPurchaseTest :
    BehaviorSpec({
        val tickerRepository = mockk<TickerRepository>()
        val tickerPurchaseRepository = mockk<TickerPurchaseRepository>()
        val tickerSaleRepository = mockk<TickerSaleRepository>()
        val dividendRepository = mockk<DividendRepository>()
        val cryptoExchangeRepository = mockk<CryptoExchangeRepository>()
        val walletService = mockk<WalletService>(relaxed = true)

        val service =
            TickerService(
                tickerRepository,
                tickerPurchaseRepository,
                tickerSaleRepository,
                dividendRepository,
                cryptoExchangeRepository,
                walletService,
            )

        afterContainer { clearAllMocks(answers = true) }

        Given("a valid ticker purchase") {
            And("the ticker exists in the database") {
                When("creating a new purchase") {
                    val ticker = TickerFactory.create(id = 1, symbol = "AAPL")
                    val walletTransactionContext =
                        WalletTransactionContextDTO(
                            wallet = WalletFactory.create(id = 1),
                            date = LocalDateTime.now(),
                            category = CategoryFactory.create(id = 1),
                            description = "Purchase AAPL",
                            status = WalletTransactionStatus.CONFIRMED,
                            includeInAnalysis = true,
                        )
                    val purchase =
                        TickerPurchaseFactory.create(
                            id = null,
                            ticker = ticker,
                            quantity = BigDecimal("10"),
                            unitPrice = BigDecimal("150.00"),
                        )
                    val walletTransaction = WalletTransactionFactory.create(id = 1)

                    every { tickerRepository.findById(1) } returns Optional.of(ticker)
                    every { walletService.createWalletTransaction(any()) } returns 1
                    every { walletService.getWalletTransactionById(1) } returns walletTransaction
                    every { tickerPurchaseRepository.save(any()) } returns
                        TickerPurchaseFactory.create(id = 1, ticker = ticker)

                    service.createTickerPurchase(purchase, walletTransactionContext)

                    Then("should create wallet transaction") {
                        verify { walletService.createWalletTransaction(any()) }
                    }

                    Then("should save the purchase") {
                        verify { tickerPurchaseRepository.save(any()) }
                    }
                }
            }

            And("the ticker does not exist") {
                When("creating a new purchase") {
                    val ticker = TickerFactory.create(id = 999, symbol = "INVALID")
                    val walletTransactionContext =
                        WalletTransactionContextDTO(
                            wallet = WalletFactory.create(id = 1),
                            date = LocalDateTime.now(),
                            category = CategoryFactory.create(id = 1),
                            description = "Purchase INVALID",
                            status = WalletTransactionStatus.CONFIRMED,
                            includeInAnalysis = true,
                        )
                    val purchase =
                        TickerPurchaseFactory.create(
                            id = null,
                            ticker = ticker,
                        )

                    every { tickerRepository.findById(999) } returns Optional.empty()

                    Then("should throw EntityNotFoundException") {
                        shouldThrow<EntityNotFoundException> {
                            service.createTickerPurchase(purchase, walletTransactionContext)
                        }
                    }
                }
            }
        }

        Given("an existing ticker purchase") {
            And("the purchase exists in the database") {
                When("updating the purchase") {
                    val ticker = TickerFactory.create(id = 1, symbol = "AAPL")
                    val walletTransaction = WalletTransactionFactory.create(id = 1)
                    val existingPurchase =
                        TickerPurchaseFactory.create(
                            id = 1,
                            ticker = ticker,
                            quantity = BigDecimal("10"),
                            unitPrice = BigDecimal("150.00"),
                            walletTransaction = walletTransaction,
                        )
                    val updatedPurchase =
                        TickerPurchaseFactory.create(
                            id = 1,
                            ticker = ticker,
                            quantity = BigDecimal("15"),
                            unitPrice = BigDecimal("160.00"),
                            walletTransaction = walletTransaction,
                        )

                    every { tickerPurchaseRepository.findById(1) } returns Optional.of(existingPurchase)
                    every { tickerRepository.existsById(1) } returns true

                    service.updateTickerPurchase(updatedPurchase)

                    Then("should update the purchase properties") {
                        existingPurchase.quantity shouldBe BigDecimal("15")
                        existingPurchase.unitPrice shouldBe BigDecimal("160.00")
                    }

                    Then("should update wallet transaction") {
                        verify { walletService.updateWalletTransaction(any()) }
                    }
                }
            }

            And("the purchase does not exist") {
                When("updating the purchase") {
                    val ticker = TickerFactory.create(id = 1, symbol = "AAPL")
                    val updatedPurchase =
                        TickerPurchaseFactory.create(
                            id = 999,
                            ticker = ticker,
                        )

                    every { tickerPurchaseRepository.findById(999) } returns Optional.empty()

                    Then("should throw EntityNotFoundException") {
                        shouldThrow<EntityNotFoundException> {
                            service.updateTickerPurchase(updatedPurchase)
                        }
                    }
                }
            }

            And("the ticker does not exist") {
                When("updating the purchase") {
                    val ticker = TickerFactory.create(id = 999, symbol = "INVALID")
                    val existingPurchase =
                        TickerPurchaseFactory.create(
                            id = 1,
                            ticker = ticker,
                        )
                    val updatedPurchase =
                        TickerPurchaseFactory.create(
                            id = 1,
                            ticker = ticker,
                        )

                    every { tickerPurchaseRepository.findById(1) } returns Optional.of(existingPurchase)
                    every { tickerRepository.existsById(999) } returns false

                    Then("should throw IllegalStateException") {
                        shouldThrow<IllegalStateException> {
                            service.updateTickerPurchase(updatedPurchase)
                        }
                    }
                }
            }
        }

        Given("an existing ticker purchase to delete") {
            And("the purchase exists in the database") {
                When("deleting the purchase") {
                    val ticker = TickerFactory.create(id = 1, symbol = "AAPL")
                    val walletTransaction = WalletTransactionFactory.create(id = 1)
                    val purchase =
                        TickerPurchaseFactory.create(
                            id = 1,
                            ticker = ticker,
                            walletTransaction = walletTransaction,
                        )

                    every { tickerPurchaseRepository.findById(1) } returns Optional.of(purchase)
                    every { tickerPurchaseRepository.delete(purchase) } returns Unit

                    service.deleteTickerPurchase(1)

                    Then("should delete the purchase") {
                        verify { tickerPurchaseRepository.delete(purchase) }
                    }

                    Then("should delete the wallet transaction") {
                        verify { walletService.deleteWalletTransaction(1) }
                    }
                }
            }

            And("the purchase does not exist") {
                When("deleting the purchase") {
                    every { tickerPurchaseRepository.findById(999) } returns Optional.empty()

                    Then("should throw EntityNotFoundException") {
                        shouldThrow<EntityNotFoundException> {
                            service.deleteTickerPurchase(999)
                        }
                    }
                }
            }
        }

        Given("a ticker with zero average unit value count") {
            And("creating first purchase for the ticker") {
                When("creating a new purchase") {
                    val ticker =
                        TickerFactory.create(
                            id = 1,
                            symbol = "AAPL",
                            currentQuantity = BigDecimal.ZERO,
                            averageUnitValue = BigDecimal.ZERO,
                        )
                    val walletTransactionContext =
                        WalletTransactionContextDTO(
                            wallet = WalletFactory.create(id = 1),
                            date = LocalDateTime.now(),
                            category = CategoryFactory.create(id = 1),
                            description = "First Purchase AAPL",
                            status = WalletTransactionStatus.CONFIRMED,
                            includeInAnalysis = true,
                        )
                    val purchase =
                        TickerPurchaseFactory.create(
                            id = null,
                            ticker = ticker,
                            quantity = BigDecimal("10"),
                            unitPrice = BigDecimal("150.00"),
                        )
                    val walletTransaction = WalletTransactionFactory.create(id = 1)

                    every { tickerRepository.findById(1) } returns Optional.of(ticker)
                    every { walletService.createWalletTransaction(any()) } returns 1
                    every { walletService.getWalletTransactionById(1) } returns walletTransaction
                    every { tickerPurchaseRepository.save(any()) } returns
                        TickerPurchaseFactory.create(id = 1, ticker = ticker)

                    service.createTickerPurchase(purchase, walletTransactionContext)

                    Then("should set average unit value to purchase unit price") {
                        ticker.averageUnitValue shouldBe BigDecimal("150.00")
                    }

                    Then("should update average unit value count") {
                        ticker.averageUnitValueCount shouldBe BigDecimal("10")
                    }

                    Then("should update current quantity") {
                        ticker.currentQuantity shouldBe BigDecimal("10")
                    }
                }
            }
        }
    })
