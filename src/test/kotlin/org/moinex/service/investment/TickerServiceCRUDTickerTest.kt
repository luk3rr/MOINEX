package org.moinex.service.investment

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import jakarta.persistence.EntityNotFoundException
import org.moinex.factory.investment.TickerFactory
import org.moinex.repository.investment.CryptoExchangeRepository
import org.moinex.repository.investment.DividendRepository
import org.moinex.repository.investment.TickerPurchaseRepository
import org.moinex.repository.investment.TickerRepository
import org.moinex.repository.investment.TickerSaleRepository
import org.moinex.service.wallet.WalletService
import java.util.Optional

class TickerServiceCRUDTickerTest :
    BehaviorSpec({
        val tickerRepository = mockk<TickerRepository>()
        val tickerPurchaseRepository = mockk<TickerPurchaseRepository>()
        val tickerSaleRepository = mockk<TickerSaleRepository>()
        val dividendRepository = mockk<DividendRepository>()
        val cryptoExchangeRepository = mockk<CryptoExchangeRepository>()
        val walletService = mockk<WalletService>()

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

        Given("a valid ticker") {
            And("the ticker symbol does not already exist") {
                When("creating a new ticker") {
                    val ticker = TickerFactory.create(id = null, symbol = "AAPL")
                    every { tickerRepository.existsBySymbol("AAPL") } returns false
                    every { tickerRepository.save(any()) } returns
                        TickerFactory.create(id = 1, symbol = "AAPL")

                    val result = service.createTicker(ticker)

                    Then("should return the created ticker id") {
                        result shouldBe 1
                    }

                    Then("should call repository save method") {
                        verify { tickerRepository.save(any()) }
                    }
                }
            }

            And("the ticker symbol already exists") {
                When("creating a new ticker") {
                    val ticker = TickerFactory.create(id = null, symbol = "GOOGL")
                    every { tickerRepository.existsBySymbol("GOOGL") } returns true

                    Then("should throw IllegalStateException") {
                        shouldThrow<IllegalStateException> {
                            service.createTicker(ticker)
                        }
                    }
                }
            }
        }

        Given("an existing ticker") {
            And("the ticker exists in the database") {
                When("updating the ticker") {
                    val existingTicker = TickerFactory.create(id = 1, symbol = "AAPL", name = "Apple Inc.")
                    val updatedTicker = TickerFactory.create(id = 1, symbol = "AAPL", name = "Apple Inc. Updated")
                    every { tickerRepository.findById(1) } returns Optional.of(existingTicker)

                    service.updateTicker(updatedTicker)

                    Then("should update the ticker properties") {
                        existingTicker.name shouldBe "Apple Inc. Updated"
                        existingTicker.symbol shouldBe "AAPL"
                    }
                }
            }

            And("the ticker does not exist") {
                When("updating the ticker") {
                    val updatedTicker = TickerFactory.create(id = 999, symbol = "INVALID")
                    every { tickerRepository.findById(999) } returns Optional.empty()

                    Then("should throw EntityNotFoundException") {
                        shouldThrow<EntityNotFoundException> {
                            service.updateTicker(updatedTicker)
                        }
                    }
                }
            }
        }

        Given("an existing ticker with no associated transactions") {
            And("the ticker exists in the database") {
                When("deleting the ticker") {
                    val ticker = TickerFactory.create(id = 1, symbol = "AAPL")
                    every { tickerRepository.findById(1) } returns Optional.of(ticker)
                    every { tickerRepository.getPurchaseCountByTicker(1) } returns 0
                    every { tickerRepository.getSaleCountByTicker(1) } returns 0
                    every { tickerRepository.getDividendCountByTicker(1) } returns 0
                    every { tickerRepository.getCryptoExchangeCountByTicker(1) } returns 0
                    every { tickerRepository.delete(ticker) } returns Unit

                    service.deleteTicker(1)

                    Then("should call repository delete method") {
                        verify { tickerRepository.delete(ticker) }
                    }
                }
            }

            And("the ticker does not exist") {
                When("deleting the ticker") {
                    every { tickerRepository.findById(999) } returns Optional.empty()

                    Then("should throw EntityNotFoundException") {
                        shouldThrow<EntityNotFoundException> {
                            service.deleteTicker(999)
                        }
                    }
                }
            }
        }

        Given("an existing ticker with associated transactions") {
            And("the ticker has purchases") {
                When("deleting the ticker") {
                    val ticker = TickerFactory.create(id = 1, symbol = "AAPL")
                    every { tickerRepository.findById(1) } returns Optional.of(ticker)
                    every { tickerRepository.getPurchaseCountByTicker(1) } returns 5
                    every { tickerRepository.getSaleCountByTicker(1) } returns 0
                    every { tickerRepository.getDividendCountByTicker(1) } returns 0
                    every { tickerRepository.getCryptoExchangeCountByTicker(1) } returns 0

                    Then("should throw IllegalStateException") {
                        shouldThrow<IllegalStateException> {
                            service.deleteTicker(1)
                        }
                    }
                }
            }

            And("the ticker has sales") {
                When("deleting the ticker") {
                    val ticker = TickerFactory.create(id = 2, symbol = "GOOGL")
                    every { tickerRepository.findById(2) } returns Optional.of(ticker)
                    every { tickerRepository.getPurchaseCountByTicker(2) } returns 0
                    every { tickerRepository.getSaleCountByTicker(2) } returns 3
                    every { tickerRepository.getDividendCountByTicker(2) } returns 0
                    every { tickerRepository.getCryptoExchangeCountByTicker(2) } returns 0

                    Then("should throw IllegalStateException") {
                        shouldThrow<IllegalStateException> {
                            service.deleteTicker(2)
                        }
                    }
                }
            }

            And("the ticker has dividends") {
                When("deleting the ticker") {
                    val ticker = TickerFactory.create(id = 3, symbol = "MSFT")
                    every { tickerRepository.findById(3) } returns Optional.of(ticker)
                    every { tickerRepository.getPurchaseCountByTicker(3) } returns 0
                    every { tickerRepository.getSaleCountByTicker(3) } returns 0
                    every { tickerRepository.getDividendCountByTicker(3) } returns 2
                    every { tickerRepository.getCryptoExchangeCountByTicker(3) } returns 0

                    Then("should throw IllegalStateException") {
                        shouldThrow<IllegalStateException> {
                            service.deleteTicker(3)
                        }
                    }
                }
            }

            And("the ticker has crypto exchanges") {
                When("deleting the ticker") {
                    val ticker = TickerFactory.create(id = 4, symbol = "BTC")
                    every { tickerRepository.findById(4) } returns Optional.of(ticker)
                    every { tickerRepository.getPurchaseCountByTicker(4) } returns 0
                    every { tickerRepository.getSaleCountByTicker(4) } returns 0
                    every { tickerRepository.getDividendCountByTicker(4) } returns 0
                    every { tickerRepository.getCryptoExchangeCountByTicker(4) } returns 1

                    Then("should throw IllegalStateException") {
                        shouldThrow<IllegalStateException> {
                            service.deleteTicker(4)
                        }
                    }
                }
            }

            And("the ticker has multiple types of transactions") {
                When("deleting the ticker") {
                    val ticker = TickerFactory.create(id = 5, symbol = "TSLA")
                    every { tickerRepository.findById(5) } returns Optional.of(ticker)
                    every { tickerRepository.getPurchaseCountByTicker(5) } returns 2
                    every { tickerRepository.getSaleCountByTicker(5) } returns 1
                    every { tickerRepository.getDividendCountByTicker(5) } returns 1
                    every { tickerRepository.getCryptoExchangeCountByTicker(5) } returns 0

                    Then("should throw IllegalStateException") {
                        shouldThrow<IllegalStateException> {
                            service.deleteTicker(5)
                        }
                    }
                }
            }
        }

        Given("an existing archived ticker") {
            And("the ticker exists in the database") {
                When("unarchiving the ticker") {
                    val ticker = TickerFactory.create(id = 1, symbol = "AAPL", isArchived = true)
                    every { tickerRepository.findById(1) } returns Optional.of(ticker)

                    service.unarchiveTicker(1)

                    Then("should set isArchived to false") {
                        ticker.isArchived shouldBe false
                    }
                }
            }

            And("the ticker is already unarchived") {
                When("unarchiving the ticker") {
                    val ticker = TickerFactory.create(id = 1, symbol = "AAPL", isArchived = false)
                    every { tickerRepository.findById(1) } returns Optional.of(ticker)

                    service.unarchiveTicker(1)

                    Then("should not update the ticker") {
                        ticker.isArchived shouldBe false
                    }
                }
            }

            And("the ticker does not exist") {
                When("unarchiving the ticker") {
                    every { tickerRepository.findById(999) } returns Optional.empty()

                    Then("should throw EntityNotFoundException") {
                        shouldThrow<EntityNotFoundException> {
                            service.unarchiveTicker(999)
                        }
                    }
                }
            }
        }
    })
