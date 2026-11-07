package org.moinex.service.investment

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.moinex.common.ClockProvider
import org.moinex.factory.CategoryFactory
import org.moinex.factory.investment.TickerFactory
import org.moinex.factory.investment.TickerPurchaseFactory
import org.moinex.factory.investment.TickerSaleFactory
import org.moinex.factory.wallet.WalletFactory
import org.moinex.factory.wallet.WalletTransactionFactory
import org.moinex.model.dto.WalletTransactionContextDTO
import org.moinex.model.enums.AssetType
import org.moinex.model.enums.WalletTransactionStatus
import org.moinex.model.investment.Ticker
import org.moinex.model.investment.TickerPurchase
import org.moinex.model.investment.TickerSale
import org.moinex.repository.investment.CryptoExchangeRepository
import org.moinex.repository.investment.DividendRepository
import org.moinex.repository.investment.TickerPurchaseRepository
import org.moinex.repository.investment.TickerRepository
import org.moinex.repository.investment.TickerSaleRepository
import org.moinex.service.NotificationService
import org.moinex.service.PreferencesService
import org.moinex.service.wallet.WalletService
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDateTime
import java.util.Optional

class TickerServiceAveragePriceTest :
    BehaviorSpec({
        val tickerRepository = mockk<TickerRepository>()
        val tickerPurchaseRepository = mockk<TickerPurchaseRepository>()
        val tickerSaleRepository = mockk<TickerSaleRepository>()
        val dividendRepository = mockk<DividendRepository>()
        val cryptoExchangeRepository = mockk<CryptoExchangeRepository>()
        val walletService = mockk<WalletService>(relaxed = true)
        val notificationService = mockk<NotificationService>(relaxed = true)
        val preferencesService = mockk<PreferencesService>(relaxed = true)

        val service =
            TickerService(
                tickerRepository,
                tickerPurchaseRepository,
                tickerSaleRepository,
                dividendRepository,
                cryptoExchangeRepository,
                walletService,
                notificationService,
                preferencesService,
                ClockProvider(),
                TickerMarketDataService(tickerRepository),
            )

        val context =
            WalletTransactionContextDTO(
                wallet = WalletFactory.create(id = 1),
                date = LocalDateTime.now(),
                category = CategoryFactory.create(id = 1),
                description = "trade",
                status = WalletTransactionStatus.CONFIRMED,
                includeInAnalysis = true,
            )

        fun emptyTicker(type: AssetType = AssetType.STOCK): Ticker =
            TickerFactory.create(
                id = 1,
                type = type,
                currentQuantity = BigDecimal.ZERO,
                averageUnitValue = BigDecimal.ZERO,
            )

        fun stubRepositories(ticker: Ticker) {
            every { tickerRepository.findById(1) } returns Optional.of(ticker)
            every { walletService.createWalletTransaction(any()) } returns 1
            every { walletService.getWalletTransactionById(1) } returns WalletTransactionFactory.create(id = 1)
            every { tickerPurchaseRepository.save(any()) } returns TickerPurchaseFactory.create(id = 1, ticker = ticker)
            every { tickerSaleRepository.save(any()) } returns TickerSaleFactory.create(id = 1, ticker = ticker)
        }

        fun buy(
            ticker: Ticker,
            quantity: String,
            price: String,
        ) = service.createTickerPurchase(
            TickerPurchaseFactory.create(
                id = null,
                ticker = ticker,
                quantity = BigDecimal(quantity),
                unitPrice = BigDecimal(price),
            ),
            context,
        )

        fun sell(
            ticker: Ticker,
            quantity: String,
        ) = service.createTickerSale(
            TickerSaleFactory.create(id = null, ticker = ticker, quantity = BigDecimal(quantity)),
            context,
        )

        fun registered(
            ticker: Ticker,
            id: Int,
            quantity: String,
            price: String,
        ): TickerPurchase {
            val purchase =
                TickerPurchaseFactory.create(
                    id = id,
                    ticker = ticker,
                    quantity = BigDecimal(quantity),
                    unitPrice = BigDecimal(price),
                    walletTransaction = WalletTransactionFactory.create(id = 7),
                )
            every { tickerRepository.existsById(1) } returns true
            every { tickerPurchaseRepository.findById(id) } returns Optional.of(purchase)
            every { tickerPurchaseRepository.delete(purchase) } returns Unit
            return purchase
        }

        afterContainer { clearAllMocks(answers = true) }

        Given("a stock bought, partially sold and bought again at another price") {
            When("the second purchase is registered") {
                val ticker = emptyTicker()
                stubRepositories(ticker)

                buy(ticker, "10", "10.00")
                sell(ticker, "5")
                buy(ticker, "10", "20.00")

                Then("the average covers only the units still held: (5 x 10 + 10 x 20) / 15") {
                    ticker.averageUnitValue.setScale(2, RoundingMode.HALF_UP).compareTo(BigDecimal("16.67")) shouldBe 0
                    ticker.currentQuantity.compareTo(BigDecimal("15")) shouldBe 0
                }
            }
        }

        Given("a stock partially sold") {
            When("the sale is registered") {
                val ticker = emptyTicker()
                stubRepositories(ticker)

                buy(ticker, "10", "10.00")
                sell(ticker, "4")

                Then("the average price does not change") {
                    ticker.averageUnitValue.compareTo(BigDecimal("10.00")) shouldBe 0
                    ticker.currentQuantity.compareTo(BigDecimal("6")) shouldBe 0
                    ticker.averageUnitValueCount.compareTo(BigDecimal("6")) shouldBe 0
                }
            }
        }

        Given("a stock sold out and bought again at another price") {
            When("the new purchase is registered") {
                val ticker = emptyTicker()
                stubRepositories(ticker)

                buy(ticker, "10", "10.00")
                sell(ticker, "10")
                buy(ticker, "10", "20.00")

                Then("the average is the price of the new purchase") {
                    ticker.averageUnitValue.compareTo(BigDecimal("20.00")) shouldBe 0
                }
            }
        }

        Given("a ticker created already holding units") {
            When("a purchase is registered") {
                val ticker =
                    TickerFactory.create(
                        id = 1,
                        currentQuantity = BigDecimal("10"),
                        averageUnitValue = BigDecimal("5.54"),
                    )
                stubRepositories(ticker)

                buy(ticker, "10", "6.54")

                Then("the units it already held weigh in the average: (10 x 5.54 + 10 x 6.54) / 20") {
                    ticker.averageUnitValue.compareTo(BigDecimal("6.04")) shouldBe 0
                }
            }
        }

        Given("a cryptocurrency bought at a price below one cent") {
            When("a second purchase is registered") {
                val ticker = emptyTicker(AssetType.CRYPTOCURRENCY)
                stubRepositories(ticker)

                buy(ticker, "1000000", "0.000002")
                buy(ticker, "1000000", "0.000004")

                Then("the average keeps its precision") {
                    ticker.averageUnitValue.compareTo(BigDecimal("0.000003")) shouldBe 0
                }
            }
        }

        Given("a ticker with two purchases") {
            When("the second purchase is deleted") {
                val ticker = emptyTicker()
                stubRepositories(ticker)
                buy(ticker, "10", "10.00")
                buy(ticker, "10", "20.00")
                val second = registered(ticker, 2, "10", "20.00")

                service.deleteTickerPurchase(2)

                Then("the position goes back to the first purchase") {
                    ticker.currentQuantity.compareTo(BigDecimal("10")) shouldBe 0
                    ticker.averageUnitValue.setScale(2, RoundingMode.HALF_UP).compareTo(BigDecimal("10.00")) shouldBe 0
                    ticker.averageUnitValueCount.compareTo(BigDecimal("10")) shouldBe 0
                    verify { tickerPurchaseRepository.delete(second) }
                }
            }

            When("the second purchase is edited to 5 units at 30") {
                val ticker = emptyTicker()
                stubRepositories(ticker)
                buy(ticker, "10", "10.00")
                buy(ticker, "10", "20.00")
                registered(ticker, 2, "10", "20.00")

                service.updateTickerPurchase(
                    TickerPurchaseFactory.create(
                        id = 2,
                        ticker = ticker,
                        quantity = BigDecimal("5"),
                        unitPrice = BigDecimal("30.00"),
                        walletTransaction = WalletTransactionFactory.create(id = 7),
                    ),
                )

                Then("quantity and average follow the edit: (10 x 10 + 5 x 30) / 15") {
                    ticker.currentQuantity.compareTo(BigDecimal("15")) shouldBe 0
                    ticker.averageUnitValue.setScale(2, RoundingMode.HALF_UP).compareTo(BigDecimal("16.67")) shouldBe 0
                }
            }

            When("a purchase whose units were already sold is deleted") {
                val ticker = emptyTicker()
                stubRepositories(ticker)
                buy(ticker, "10", "10.00")
                sell(ticker, "10")
                registered(ticker, 1, "10", "10.00")

                Then("it is refused because the quantity held would be negative") {
                    shouldThrow<IllegalStateException> { service.deleteTickerPurchase(1) }
                }
            }
        }

        Given("a ticker with a partial sale") {
            When("the sale is deleted") {
                val ticker = emptyTicker()
                stubRepositories(ticker)
                buy(ticker, "10", "10.00")
                sell(ticker, "4")
                val sale: TickerSale =
                    TickerSaleFactory.create(
                        id = 1,
                        ticker = ticker,
                        quantity = BigDecimal("4"),
                        averageCost = BigDecimal("10.00"),
                        walletTransaction = WalletTransactionFactory.create(id = 8),
                    )
                every { tickerRepository.existsById(1) } returns true
                every { tickerSaleRepository.findById(1) } returns Optional.of(sale)
                every { tickerSaleRepository.delete(sale) } returns Unit

                service.deleteTickerSale(1)

                Then("the units return at their cost and the average does not change") {
                    ticker.currentQuantity.compareTo(BigDecimal("10")) shouldBe 0
                    ticker.averageUnitValue.setScale(2, RoundingMode.HALF_UP).compareTo(BigDecimal("10.00")) shouldBe 0
                    ticker.averageUnitValueCount.compareTo(BigDecimal("10")) shouldBe 0
                }
            }

            When("the sale is edited to 6 units") {
                val ticker = emptyTicker()
                stubRepositories(ticker)
                buy(ticker, "10", "10.00")
                sell(ticker, "4")
                val sale: TickerSale =
                    TickerSaleFactory.create(
                        id = 1,
                        ticker = ticker,
                        quantity = BigDecimal("4"),
                        averageCost = BigDecimal("10.00"),
                        walletTransaction = WalletTransactionFactory.create(id = 8),
                    )
                every { tickerRepository.existsById(1) } returns true
                every { tickerSaleRepository.findById(1) } returns Optional.of(sale)

                service.updateSale(
                    TickerSaleFactory.create(
                        id = 1,
                        ticker = ticker,
                        quantity = BigDecimal("6"),
                        walletTransaction = WalletTransactionFactory.create(id = 8),
                    ),
                )

                Then("the quantity held is the purchases minus the new sale") {
                    ticker.currentQuantity.compareTo(BigDecimal("4")) shouldBe 0
                    ticker.averageUnitValue.setScale(2, RoundingMode.HALF_UP).compareTo(BigDecimal("10.00")) shouldBe 0
                }
            }
        }
    })
