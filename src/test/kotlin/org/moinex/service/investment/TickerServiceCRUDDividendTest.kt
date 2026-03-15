package org.moinex.service.investment

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import jakarta.persistence.EntityNotFoundException
import org.moinex.factory.CategoryFactory
import org.moinex.factory.investment.DividendFactory
import org.moinex.factory.investment.TickerFactory
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

class TickerServiceCRUDDividendTest :
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

        Given("a valid dividend") {
            And("the ticker exists and amount is positive") {
                When("creating a new dividend") {
                    val ticker = TickerFactory.create(id = 1, symbol = "AAPL")
                    val walletTransactionContext =
                        WalletTransactionContextDTO(
                            wallet = WalletFactory.create(id = 1),
                            date = LocalDateTime.now(),
                            category = CategoryFactory.create(id = 1),
                            description = "Dividend from AAPL",
                            status = WalletTransactionStatus.CONFIRMED,
                            includeInAnalysis = true,
                        )
                    val dividend =
                        DividendFactory.create(
                            id = null,
                            ticker = ticker,
                        )
                    val amount = BigDecimal("50.00")
                    val walletTransaction = WalletTransactionFactory.create(id = 1)

                    every { tickerRepository.existsById(1) } returns true
                    every { walletService.createWalletTransaction(any()) } returns 1
                    every { walletService.getWalletTransactionById(1) } returns walletTransaction
                    every { dividendRepository.save(any()) } returns
                        DividendFactory.create(id = 1, ticker = ticker)

                    service.createDividend(dividend, amount, walletTransactionContext)

                    Then("should create wallet transaction") {
                        verify { walletService.createWalletTransaction(any()) }
                    }

                    Then("should save the dividend") {
                        verify { dividendRepository.save(any()) }
                    }
                }
            }

            And("the ticker does not exist") {
                When("creating a new dividend") {
                    val ticker = TickerFactory.create(id = 999, symbol = "INVALID")
                    val walletTransactionContext =
                        WalletTransactionContextDTO(
                            wallet = WalletFactory.create(id = 1),
                            date = LocalDateTime.now(),
                            category = CategoryFactory.create(id = 1),
                            description = "Dividend from INVALID",
                            status = WalletTransactionStatus.CONFIRMED,
                            includeInAnalysis = true,
                        )
                    val dividend =
                        DividendFactory.create(
                            id = null,
                            ticker = ticker,
                        )
                    val amount = BigDecimal("50.00")

                    every { tickerRepository.existsById(999) } returns false

                    Then("should throw IllegalStateException") {
                        shouldThrow<IllegalStateException> {
                            service.createDividend(dividend, amount, walletTransactionContext)
                        }
                    }
                }
            }

            And("the amount is zero or negative") {
                When("creating a new dividend with zero amount") {
                    val ticker = TickerFactory.create(id = 1, symbol = "AAPL")
                    val walletTransactionContext =
                        WalletTransactionContextDTO(
                            wallet = WalletFactory.create(id = 1),
                            date = LocalDateTime.now(),
                            category = CategoryFactory.create(id = 1),
                            description = "Dividend from AAPL",
                            status = WalletTransactionStatus.CONFIRMED,
                            includeInAnalysis = true,
                        )
                    val dividend =
                        DividendFactory.create(
                            id = null,
                            ticker = ticker,
                        )
                    val amount = BigDecimal.ZERO

                    every { tickerRepository.existsById(1) } returns true

                    Then("should throw IllegalStateException") {
                        shouldThrow<IllegalStateException> {
                            service.createDividend(dividend, amount, walletTransactionContext)
                        }
                    }
                }

                When("creating a new dividend with negative amount") {
                    val ticker = TickerFactory.create(id = 1, symbol = "AAPL")
                    val walletTransactionContext =
                        WalletTransactionContextDTO(
                            wallet = WalletFactory.create(id = 1),
                            date = LocalDateTime.now(),
                            category = CategoryFactory.create(id = 1),
                            description = "Dividend from AAPL",
                            status = WalletTransactionStatus.CONFIRMED,
                            includeInAnalysis = true,
                        )
                    val dividend =
                        DividendFactory.create(
                            id = null,
                            ticker = ticker,
                        )
                    val amount = BigDecimal("-10.00")

                    every { tickerRepository.existsById(1) } returns true

                    Then("should throw IllegalStateException") {
                        shouldThrow<IllegalStateException> {
                            service.createDividend(dividend, amount, walletTransactionContext)
                        }
                    }
                }
            }
        }

        Given("an existing dividend") {
            And("the dividend exists in the database") {
                When("updating the dividend") {
                    val ticker = TickerFactory.create(id = 1, symbol = "AAPL")
                    val newTicker = TickerFactory.create(id = 2, symbol = "GOOGL")
                    val walletTransaction = WalletTransactionFactory.create(id = 1)
                    val existingDividend =
                        DividendFactory.create(
                            id = 1,
                            ticker = ticker,
                            walletTransaction = walletTransaction,
                        )
                    val updatedDividend =
                        DividendFactory.create(
                            id = 1,
                            ticker = newTicker,
                            walletTransaction = walletTransaction,
                        )

                    every { dividendRepository.findById(1) } returns Optional.of(existingDividend)
                    every { tickerRepository.existsById(2) } returns true

                    service.updateDividend(updatedDividend)

                    Then("should update the dividend ticker") {
                        verify { dividendRepository.findById(1) }
                    }

                    Then("should update wallet transaction") {
                        verify { walletService.updateWalletTransaction(any()) }
                    }
                }
            }

            And("the dividend does not exist") {
                When("updating the dividend") {
                    val ticker = TickerFactory.create(id = 1, symbol = "AAPL")
                    val updatedDividend =
                        DividendFactory.create(
                            id = 999,
                            ticker = ticker,
                        )

                    every { dividendRepository.findById(999) } returns Optional.empty()

                    Then("should throw EntityNotFoundException") {
                        shouldThrow<EntityNotFoundException> {
                            service.updateDividend(updatedDividend)
                        }
                    }
                }
            }

            And("the ticker does not exist") {
                When("updating the dividend") {
                    val ticker = TickerFactory.create(id = 999, symbol = "INVALID")
                    val existingDividend =
                        DividendFactory.create(
                            id = 1,
                            ticker = ticker,
                        )
                    val updatedDividend =
                        DividendFactory.create(
                            id = 1,
                            ticker = ticker,
                        )

                    every { dividendRepository.findById(1) } returns Optional.of(existingDividend)
                    every { tickerRepository.existsById(999) } returns false

                    Then("should throw IllegalStateException") {
                        shouldThrow<IllegalStateException> {
                            service.updateDividend(updatedDividend)
                        }
                    }
                }
            }
        }

        Given("an existing dividend to delete") {
            And("the dividend exists in the database") {
                When("deleting the dividend") {
                    val ticker = TickerFactory.create(id = 1, symbol = "AAPL")
                    val walletTransaction = WalletTransactionFactory.create(id = 1)
                    val dividend =
                        DividendFactory.create(
                            id = 1,
                            ticker = ticker,
                            walletTransaction = walletTransaction,
                        )

                    every { dividendRepository.findById(1) } returns Optional.of(dividend)
                    every { dividendRepository.delete(dividend) } returns Unit

                    service.deleteDividend(1)

                    Then("should delete the dividend") {
                        verify { dividendRepository.delete(dividend) }
                    }

                    Then("should delete the wallet transaction") {
                        verify { walletService.deleteWalletTransaction(1) }
                    }
                }
            }

            And("the dividend does not exist") {
                When("deleting the dividend") {
                    every { dividendRepository.findById(999) } returns Optional.empty()

                    Then("should throw EntityNotFoundException") {
                        shouldThrow<EntityNotFoundException> {
                            service.deleteDividend(999)
                        }
                    }
                }
            }
        }
    })
