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
import org.moinex.factory.investment.TickerSaleFactory
import org.moinex.factory.wallet.WalletFactory
import org.moinex.factory.wallet.WalletTransactionFactory
import org.moinex.model.dto.WalletTransactionContextDTO
import org.moinex.repository.investment.CryptoExchangeRepository
import org.moinex.repository.investment.DividendRepository
import org.moinex.repository.investment.TickerPurchaseRepository
import org.moinex.repository.investment.TickerRepository
import org.moinex.repository.investment.TickerSaleRepository
import org.moinex.service.wallet.WalletService
import java.math.BigDecimal
import java.time.LocalDateTime
import java.util.Optional

class TickerServiceCRUDTickerSaleTest :
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

        Given("a valid ticker sale") {
            And("the ticker exists with sufficient quantity") {
                When("creating a new sale") {
                    val ticker =
                        TickerFactory.create(
                            id = 1,
                            symbol = "AAPL",
                            currentQuantity = BigDecimal("100"),
                        )
                    val walletTransactionContext =
                        WalletTransactionContextDTO(
                            wallet = WalletFactory.create(id = 1),
                            date = LocalDateTime.now(),
                            category = CategoryFactory.create(id = 1),
                            description = "Sale AAPL",
                            status = org.moinex.model.enums.WalletTransactionStatus.CONFIRMED,
                            includeInAnalysis = true,
                        )
                    val sale =
                        TickerSaleFactory.create(
                            id = null,
                            ticker = ticker,
                            quantity = BigDecimal("10"),
                            unitPrice = BigDecimal("160.00"),
                        )
                    val walletTransaction = WalletTransactionFactory.create(id = 1)

                    every { tickerRepository.findById(1) } returns Optional.of(ticker)
                    every { walletService.createWalletTransaction(any()) } returns 1
                    every { walletService.getWalletTransactionById(1) } returns walletTransaction
                    every { tickerSaleRepository.save(any()) } returns
                        TickerSaleFactory.create(id = 1, ticker = ticker)

                    service.createTickerSale(sale, walletTransactionContext)

                    Then("should create wallet transaction") {
                        verify { walletService.createWalletTransaction(any()) }
                    }

                    Then("should save the sale") {
                        verify { tickerSaleRepository.save(any()) }
                    }

                    Then("should update ticker quantity") {
                        ticker.currentQuantity shouldBe BigDecimal("90")
                    }
                }
            }

            And("the ticker exists but with insufficient quantity") {
                When("creating a new sale") {
                    val ticker =
                        TickerFactory.create(
                            id = 1,
                            symbol = "AAPL",
                            currentQuantity = BigDecimal("5"),
                        )
                    val walletTransactionContext =
                        WalletTransactionContextDTO(
                            wallet = WalletFactory.create(id = 1),
                            date = LocalDateTime.now(),
                            category = CategoryFactory.create(id = 1),
                            description = "Sale AAPL",
                            status = org.moinex.model.enums.WalletTransactionStatus.CONFIRMED,
                            includeInAnalysis = true,
                        )
                    val sale =
                        TickerSaleFactory.create(
                            id = null,
                            ticker = ticker,
                            quantity = BigDecimal("10"),
                            unitPrice = BigDecimal("160.00"),
                        )

                    every { tickerRepository.findById(1) } returns Optional.of(ticker)

                    Then("should throw IllegalStateException") {
                        shouldThrow<IllegalStateException> {
                            service.createTickerSale(sale, walletTransactionContext)
                        }
                    }
                }
            }

            And("the ticker does not exist") {
                When("creating a new sale") {
                    val ticker = TickerFactory.create(id = 999, symbol = "INVALID")
                    val walletTransactionContext =
                        WalletTransactionContextDTO(
                            wallet = WalletFactory.create(id = 1),
                            date = LocalDateTime.now(),
                            category = CategoryFactory.create(id = 1),
                            description = "Sale INVALID",
                            status = org.moinex.model.enums.WalletTransactionStatus.CONFIRMED,
                            includeInAnalysis = true,
                        )
                    val sale =
                        TickerSaleFactory.create(
                            id = null,
                            ticker = ticker,
                        )

                    every { tickerRepository.findById(999) } returns Optional.empty()

                    Then("should throw EntityNotFoundException") {
                        shouldThrow<EntityNotFoundException> {
                            service.createTickerSale(sale, walletTransactionContext)
                        }
                    }
                }
            }
        }

        Given("an existing ticker sale") {
            And("the sale exists in the database") {
                When("updating the sale") {
                    val ticker = TickerFactory.create(id = 1, symbol = "AAPL")
                    val walletTransaction = WalletTransactionFactory.create(id = 1)
                    val existingSale =
                        TickerSaleFactory.create(
                            id = 1,
                            ticker = ticker,
                            quantity = BigDecimal("10"),
                            unitPrice = BigDecimal("160.00"),
                            walletTransaction = walletTransaction,
                        )
                    val updatedSale =
                        TickerSaleFactory.create(
                            id = 1,
                            ticker = ticker,
                            quantity = BigDecimal("15"),
                            unitPrice = BigDecimal("170.00"),
                            walletTransaction = walletTransaction,
                        )

                    every { tickerSaleRepository.findById(1) } returns Optional.of(existingSale)
                    every { tickerRepository.existsById(1) } returns true

                    service.updateSale(updatedSale)

                    Then("should update the sale properties") {
                        existingSale.quantity shouldBe BigDecimal("15")
                        existingSale.unitPrice shouldBe BigDecimal("170.00")
                    }

                    Then("should update wallet transaction") {
                        verify { walletService.updateWalletTransaction(any()) }
                    }
                }
            }

            And("the sale does not exist") {
                When("updating the sale") {
                    val ticker = TickerFactory.create(id = 1, symbol = "AAPL")
                    val updatedSale =
                        TickerSaleFactory.create(
                            id = 999,
                            ticker = ticker,
                        )

                    every { tickerSaleRepository.findById(999) } returns Optional.empty()

                    Then("should throw EntityNotFoundException") {
                        shouldThrow<EntityNotFoundException> {
                            service.updateSale(updatedSale)
                        }
                    }
                }
            }

            And("the ticker does not exist") {
                When("updating the sale") {
                    val ticker = TickerFactory.create(id = 999, symbol = "INVALID")
                    val existingSale =
                        TickerSaleFactory.create(
                            id = 1,
                            ticker = ticker,
                        )
                    val updatedSale =
                        TickerSaleFactory.create(
                            id = 1,
                            ticker = ticker,
                        )

                    every { tickerSaleRepository.findById(1) } returns Optional.of(existingSale)
                    every { tickerRepository.existsById(999) } returns false

                    Then("should throw IllegalStateException") {
                        shouldThrow<IllegalStateException> {
                            service.updateSale(updatedSale)
                        }
                    }
                }
            }
        }

        Given("an existing ticker sale to delete") {
            And("the sale exists in the database") {
                When("deleting the sale") {
                    val ticker = TickerFactory.create(id = 1, symbol = "AAPL")
                    val walletTransaction = WalletTransactionFactory.create(id = 1)
                    val sale =
                        TickerSaleFactory.create(
                            id = 1,
                            ticker = ticker,
                            walletTransaction = walletTransaction,
                        )

                    every { tickerSaleRepository.findById(1) } returns Optional.of(sale)
                    every { tickerSaleRepository.delete(sale) } returns Unit

                    service.deleteTickerSale(1)

                    Then("should delete the sale") {
                        verify { tickerSaleRepository.delete(sale) }
                    }

                    Then("should delete the wallet transaction") {
                        verify { walletService.deleteWalletTransaction(1) }
                    }
                }
            }

            And("the sale does not exist") {
                When("deleting the sale") {
                    every { tickerSaleRepository.findById(999) } returns Optional.empty()

                    Then("should throw EntityNotFoundException") {
                        shouldThrow<EntityNotFoundException> {
                            service.deleteTickerSale(999)
                        }
                    }
                }
            }
        }
    })
