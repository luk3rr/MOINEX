package org.moinex.service.investment

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import jakarta.persistence.EntityNotFoundException
import org.moinex.factory.investment.CryptoExchangeFactory
import org.moinex.factory.investment.TickerFactory
import org.moinex.model.enums.AssetType
import org.moinex.repository.investment.CryptoExchangeRepository
import org.moinex.repository.investment.DividendRepository
import org.moinex.repository.investment.TickerPurchaseRepository
import org.moinex.repository.investment.TickerRepository
import org.moinex.repository.investment.TickerSaleRepository
import org.moinex.service.wallet.WalletService
import java.math.BigDecimal
import java.util.Optional

class TickerServiceCRUDCryptoExchangeTest :
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

        Given("a valid crypto exchange") {
            And("both tickers exist with sufficient quantity") {
                When("creating a new crypto exchange") {
                    val soldCrypto =
                        TickerFactory.create(
                            id = 1,
                            symbol = "BTC",
                            type = AssetType.CRYPTOCURRENCY,
                            currentQuantity = BigDecimal("10.0"),
                        )
                    val receivedCrypto =
                        TickerFactory.create(
                            id = 2,
                            symbol = "ETH",
                            type = AssetType.CRYPTOCURRENCY,
                            currentQuantity = BigDecimal("100.0"),
                        )
                    val exchange =
                        CryptoExchangeFactory.create(
                            id = null,
                            soldCrypto = soldCrypto,
                            receivedCrypto = receivedCrypto,
                            soldQuantity = BigDecimal("1.0"),
                            receivedQuantity = BigDecimal("15.0"),
                        )

                    every { tickerRepository.findById(1) } returns Optional.of(soldCrypto)
                    every { tickerRepository.findById(2) } returns Optional.of(receivedCrypto)
                    every { cryptoExchangeRepository.save(any()) } returns
                        CryptoExchangeFactory.create(id = 1, soldCrypto = soldCrypto, receivedCrypto = receivedCrypto)

                    service.createCryptoExchange(exchange)

                    Then("should save the crypto exchange") {
                        verify { cryptoExchangeRepository.save(any()) }
                    }

                    Then("should update sold crypto quantity") {
                        soldCrypto.currentQuantity shouldBe BigDecimal("9.00")
                    }

                    Then("should update received crypto quantity") {
                        receivedCrypto.currentQuantity shouldBe BigDecimal("115.00")
                    }
                }
            }

            And("sold crypto has insufficient quantity") {
                When("creating a new crypto exchange") {
                    val soldCrypto =
                        TickerFactory.create(
                            id = 1,
                            symbol = "BTC",
                            type = AssetType.CRYPTOCURRENCY,
                            currentQuantity = BigDecimal("0.5"),
                        )
                    val receivedCrypto =
                        TickerFactory.create(
                            id = 2,
                            symbol = "ETH",
                            type = AssetType.CRYPTOCURRENCY,
                            currentQuantity = BigDecimal("100.0"),
                        )
                    val exchange =
                        CryptoExchangeFactory.create(
                            id = null,
                            soldCrypto = soldCrypto,
                            receivedCrypto = receivedCrypto,
                            soldQuantity = BigDecimal("1.0"),
                            receivedQuantity = BigDecimal("15.0"),
                        )

                    every { tickerRepository.findById(1) } returns Optional.of(soldCrypto)
                    every { tickerRepository.findById(2) } returns Optional.of(receivedCrypto)

                    Then("should throw IllegalStateException") {
                        shouldThrow<IllegalStateException> {
                            service.createCryptoExchange(exchange)
                        }
                    }
                }
            }

            And("sold crypto does not exist") {
                When("creating a new crypto exchange") {
                    val soldCrypto =
                        TickerFactory.create(
                            id = 999,
                            symbol = "BTC",
                            type = AssetType.CRYPTOCURRENCY,
                        )
                    val receivedCrypto =
                        TickerFactory.create(
                            id = 2,
                            symbol = "ETH",
                            type = AssetType.CRYPTOCURRENCY,
                        )
                    val exchange =
                        CryptoExchangeFactory.create(
                            id = null,
                            soldCrypto = soldCrypto,
                            receivedCrypto = receivedCrypto,
                        )

                    every { tickerRepository.findById(999) } returns Optional.empty()

                    Then("should throw EntityNotFoundException") {
                        shouldThrow<EntityNotFoundException> {
                            service.createCryptoExchange(exchange)
                        }
                    }
                }
            }

            And("received crypto does not exist") {
                When("creating a new crypto exchange") {
                    val soldCrypto =
                        TickerFactory.create(
                            id = 1,
                            symbol = "BTC",
                            type = AssetType.CRYPTOCURRENCY,
                        )
                    val receivedCrypto =
                        TickerFactory.create(
                            id = 999,
                            symbol = "ETH",
                            type = AssetType.CRYPTOCURRENCY,
                        )
                    val exchange =
                        CryptoExchangeFactory.create(
                            id = null,
                            soldCrypto = soldCrypto,
                            receivedCrypto = receivedCrypto,
                        )

                    every { tickerRepository.findById(1) } returns Optional.of(soldCrypto)
                    every { tickerRepository.findById(999) } returns Optional.empty()

                    Then("should throw EntityNotFoundException") {
                        shouldThrow<EntityNotFoundException> {
                            service.createCryptoExchange(exchange)
                        }
                    }
                }
            }
        }

        Given("an existing crypto exchange") {
            And("the exchange exists in the database") {
                When("updating the exchange") {
                    val soldCrypto =
                        TickerFactory.create(
                            id = 1,
                            symbol = "BTC",
                            type = AssetType.CRYPTOCURRENCY,
                        )
                    val receivedCrypto =
                        TickerFactory.create(
                            id = 2,
                            symbol = "ETH",
                            type = AssetType.CRYPTOCURRENCY,
                        )
                    val existingExchange =
                        CryptoExchangeFactory.create(
                            id = 1,
                            soldCrypto = soldCrypto,
                            receivedCrypto = receivedCrypto,
                        )
                    val updatedExchange =
                        CryptoExchangeFactory.create(
                            id = 1,
                            soldCrypto = soldCrypto,
                            receivedCrypto = receivedCrypto,
                            soldQuantity = BigDecimal("2.0"),
                            receivedQuantity = BigDecimal("30.0"),
                        )

                    every { cryptoExchangeRepository.findById(1) } returns Optional.of(existingExchange)
                    every { tickerRepository.existsById(1) } returns true
                    every { tickerRepository.existsById(2) } returns true

                    service.updateCryptoExchange(updatedExchange)

                    Then("should update the exchange") {
                        verify { cryptoExchangeRepository.findById(1) }
                    }
                }
            }

            And("the exchange does not exist") {
                When("updating the exchange") {
                    val soldCrypto =
                        TickerFactory.create(
                            id = 1,
                            symbol = "BTC",
                            type = AssetType.CRYPTOCURRENCY,
                        )
                    val receivedCrypto =
                        TickerFactory.create(
                            id = 2,
                            symbol = "ETH",
                            type = AssetType.CRYPTOCURRENCY,
                        )
                    val updatedExchange =
                        CryptoExchangeFactory.create(
                            id = 999,
                            soldCrypto = soldCrypto,
                            receivedCrypto = receivedCrypto,
                        )

                    every { cryptoExchangeRepository.findById(999) } returns Optional.empty()

                    Then("should throw EntityNotFoundException") {
                        shouldThrow<EntityNotFoundException> {
                            service.updateCryptoExchange(updatedExchange)
                        }
                    }
                }
            }

            And("sold crypto does not exist") {
                When("updating the exchange") {
                    val soldCrypto =
                        TickerFactory.create(
                            id = 999,
                            symbol = "BTC",
                            type = AssetType.CRYPTOCURRENCY,
                        )
                    val receivedCrypto =
                        TickerFactory.create(
                            id = 2,
                            symbol = "ETH",
                            type = AssetType.CRYPTOCURRENCY,
                        )
                    val existingExchange =
                        CryptoExchangeFactory.create(
                            id = 1,
                            soldCrypto = soldCrypto,
                            receivedCrypto = receivedCrypto,
                        )
                    val updatedExchange =
                        CryptoExchangeFactory.create(
                            id = 1,
                            soldCrypto = soldCrypto,
                            receivedCrypto = receivedCrypto,
                        )

                    every { cryptoExchangeRepository.findById(1) } returns Optional.of(existingExchange)
                    every { tickerRepository.existsById(999) } returns false

                    Then("should throw IllegalStateException") {
                        shouldThrow<IllegalStateException> {
                            service.updateCryptoExchange(updatedExchange)
                        }
                    }
                }
            }

            And("received crypto does not exist") {
                When("updating the exchange") {
                    val soldCrypto =
                        TickerFactory.create(
                            id = 1,
                            symbol = "BTC",
                            type = AssetType.CRYPTOCURRENCY,
                        )
                    val receivedCrypto =
                        TickerFactory.create(
                            id = 999,
                            symbol = "ETH",
                            type = AssetType.CRYPTOCURRENCY,
                        )
                    val existingExchange =
                        CryptoExchangeFactory.create(
                            id = 1,
                            soldCrypto = soldCrypto,
                            receivedCrypto = receivedCrypto,
                        )
                    val updatedExchange =
                        CryptoExchangeFactory.create(
                            id = 1,
                            soldCrypto = soldCrypto,
                            receivedCrypto = receivedCrypto,
                        )

                    every { cryptoExchangeRepository.findById(1) } returns Optional.of(existingExchange)
                    every { tickerRepository.existsById(1) } returns true
                    every { tickerRepository.existsById(999) } returns false

                    Then("should throw IllegalStateException") {
                        shouldThrow<IllegalStateException> {
                            service.updateCryptoExchange(updatedExchange)
                        }
                    }
                }
            }
        }

        Given("an existing crypto exchange to delete") {
            And("the exchange exists in the database") {
                When("deleting the exchange") {
                    val soldCrypto =
                        TickerFactory.create(
                            id = 1,
                            symbol = "BTC",
                            type = AssetType.CRYPTOCURRENCY,
                            currentQuantity = BigDecimal("9.0"),
                        )
                    val receivedCrypto =
                        TickerFactory.create(
                            id = 2,
                            symbol = "ETH",
                            type = AssetType.CRYPTOCURRENCY,
                            currentQuantity = BigDecimal("115.0"),
                        )
                    val exchange =
                        CryptoExchangeFactory.create(
                            id = 1,
                            soldCrypto = soldCrypto,
                            receivedCrypto = receivedCrypto,
                            soldQuantity = BigDecimal("1.0"),
                            receivedQuantity = BigDecimal("15.0"),
                        )

                    every { cryptoExchangeRepository.findById(1) } returns Optional.of(exchange)
                    every { tickerRepository.save(soldCrypto) } returns soldCrypto
                    every { tickerRepository.save(receivedCrypto) } returns receivedCrypto
                    every { cryptoExchangeRepository.delete(exchange) } returns Unit

                    service.deleteCryptoExchange(1)

                    Then("should restore sold crypto quantity") {
                        soldCrypto.currentQuantity shouldBe BigDecimal("10.00")
                    }

                    Then("should restore received crypto quantity") {
                        receivedCrypto.currentQuantity shouldBe BigDecimal("100.00")
                    }

                    Then("should save both tickers") {
                        verify { tickerRepository.save(soldCrypto) }
                        verify { tickerRepository.save(receivedCrypto) }
                    }

                    Then("should delete the exchange") {
                        verify { cryptoExchangeRepository.delete(exchange) }
                    }
                }
            }

            And("the exchange does not exist") {
                When("deleting the exchange") {
                    every { cryptoExchangeRepository.findById(999) } returns Optional.empty()

                    Then("should throw EntityNotFoundException") {
                        shouldThrow<EntityNotFoundException> {
                            service.deleteCryptoExchange(999)
                        }
                    }
                }
            }
        }

        Given("an existing crypto exchange with different sold crypto") {
            When("updating the exchange to change sold crypto") {
                val oldSoldCrypto =
                    TickerFactory.create(
                        id = 1,
                        symbol = "BTC",
                        type = AssetType.CRYPTOCURRENCY,
                        currentQuantity = BigDecimal("9.0"),
                    )
                val newSoldCrypto =
                    TickerFactory.create(
                        id = 3,
                        symbol = "LTC",
                        type = AssetType.CRYPTOCURRENCY,
                        currentQuantity = BigDecimal("100.0"),
                    )
                val receivedCrypto =
                    TickerFactory.create(
                        id = 2,
                        symbol = "ETH",
                        type = AssetType.CRYPTOCURRENCY,
                        currentQuantity = BigDecimal("115.0"),
                    )
                val existingExchange =
                    CryptoExchangeFactory.create(
                        id = 1,
                        soldCrypto = oldSoldCrypto,
                        receivedCrypto = receivedCrypto,
                        soldQuantity = BigDecimal("1.0"),
                        receivedQuantity = BigDecimal("15.0"),
                    )
                val updatedExchange =
                    CryptoExchangeFactory.create(
                        id = 1,
                        soldCrypto = newSoldCrypto,
                        receivedCrypto = receivedCrypto,
                        soldQuantity = BigDecimal("1.0"),
                        receivedQuantity = BigDecimal("15.0"),
                    )

                every { cryptoExchangeRepository.findById(1) } returns Optional.of(existingExchange)
                every { tickerRepository.existsById(3) } returns true
                every { tickerRepository.existsById(2) } returns true

                service.updateCryptoExchange(updatedExchange)

                Then("should restore old sold crypto quantity") {
                    oldSoldCrypto.currentQuantity shouldBe BigDecimal("10.00")
                }

                Then("should subtract from new sold crypto quantity") {
                    newSoldCrypto.currentQuantity shouldBe BigDecimal("99.00")
                }

                Then("should update exchange sold crypto reference") {
                    existingExchange.soldCrypto.id shouldBe 3
                }
            }
        }

        Given("an existing crypto exchange with different received crypto") {
            When("updating the exchange to change received crypto") {
                val soldCrypto =
                    TickerFactory.create(
                        id = 1,
                        symbol = "BTC",
                        type = AssetType.CRYPTOCURRENCY,
                        currentQuantity = BigDecimal("9.0"),
                    )
                val oldReceivedCrypto =
                    TickerFactory.create(
                        id = 2,
                        symbol = "ETH",
                        type = AssetType.CRYPTOCURRENCY,
                        currentQuantity = BigDecimal("115.0"),
                    )
                val newReceivedCrypto =
                    TickerFactory.create(
                        id = 4,
                        symbol = "ADA",
                        type = AssetType.CRYPTOCURRENCY,
                        currentQuantity = BigDecimal("500.0"),
                    )
                val existingExchange =
                    CryptoExchangeFactory.create(
                        id = 1,
                        soldCrypto = soldCrypto,
                        receivedCrypto = oldReceivedCrypto,
                        soldQuantity = BigDecimal("1.0"),
                        receivedQuantity = BigDecimal("15.0"),
                    )
                val updatedExchange =
                    CryptoExchangeFactory.create(
                        id = 1,
                        soldCrypto = soldCrypto,
                        receivedCrypto = newReceivedCrypto,
                        soldQuantity = BigDecimal("1.0"),
                        receivedQuantity = BigDecimal("15.0"),
                    )

                every { cryptoExchangeRepository.findById(1) } returns Optional.of(existingExchange)
                every { tickerRepository.existsById(1) } returns true
                every { tickerRepository.existsById(4) } returns true

                service.updateCryptoExchange(updatedExchange)

                Then("should restore old received crypto quantity") {
                    oldReceivedCrypto.currentQuantity shouldBe BigDecimal("100.00")
                }

                Then("should add to new received crypto quantity") {
                    newReceivedCrypto.currentQuantity shouldBe BigDecimal("515.00")
                }

                Then("should update exchange received crypto reference") {
                    existingExchange.receivedCrypto.id shouldBe 4
                }
            }
        }
    })
