/*
 * Filename: InvestmentPerformanceServiceTest.kt
 * Created on: March 15, 2026
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 */

package org.moinex.service.investment

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.moinex.common.ClockProvider
import org.moinex.common.constant.Constants
import org.moinex.common.extension.isEqual
import org.moinex.factory.investment.BondFactory
import org.moinex.factory.investment.BondInterestCalculationFactory
import org.moinex.factory.investment.BondOperationFactory
import org.moinex.factory.investment.DividendFactory
import org.moinex.factory.investment.InvestmentPerformanceSnapshotFactory
import org.moinex.factory.investment.TickerFactory
import org.moinex.factory.investment.TickerPurchaseFactory
import org.moinex.factory.investment.TickerSaleFactory
import org.moinex.factory.wallet.WalletTransactionFactory
import org.moinex.model.enums.AssetType
import org.moinex.model.enums.OperationType
import org.moinex.repository.investment.InvestmentPerformanceSnapshotRepository
import org.moinex.service.NotificationService
import org.moinex.service.PreferencesService
import java.math.BigDecimal
import java.time.LocalDateTime
import java.time.YearMonth

class InvestmentPerformanceServiceTest :
    BehaviorSpec({
        val snapshotRepository = mockk<InvestmentPerformanceSnapshotRepository>()
        val tickerService = mockk<TickerService>()
        val bondService = mockk<BondService>()
        val tickerPriceHistoryService = mockk<TickerPriceHistoryService>()
        val bondInterestCalculationService = mockk<BondInterestCalculationService>()
        val notificationService = mockk<NotificationService>(relaxed = true)
        val preferencesService = mockk<PreferencesService>(relaxed = true)
        val clockProvider = ClockProvider()

        val service =
            InvestmentPerformanceService(
                snapshotRepository,
                tickerService,
                bondService,
                tickerPriceHistoryService,
                bondInterestCalculationService,
                notificationService,
                preferencesService,
                clockProvider,
            )

        afterContainer { clearAllMocks(answers = true) }

        Given("no snapshots in database") {
            When("recalculating all snapshots") {
                val currentMonth = YearMonth.now()

                every { snapshotRepository.deleteAll() } returns Unit
                every { snapshotRepository.findByReferenceMonth(any()) } returns null
                every { tickerService.getAllNonArchivedTickers() } returns emptyList()
                every { tickerService.getAllPurchases() } returns emptyList()
                every { tickerService.getAllSales() } returns emptyList()
                every { tickerService.getAllDividends() } returns emptyList()
                every { tickerService.getAllNonArchivedSales() } returns emptyList()
                every { tickerService.getAllPurchasesByTicker(any()) } returns emptyList()
                every { bondService.getAllNonArchivedBonds() } returns emptyList()
                every { bondService.getAllOperations() } returns emptyList()
                every { bondService.getOperationsByBond(any()) } returns emptyList()
                every { snapshotRepository.save(any()) } returnsArgument 0

                service.recalculateAllSnapshots()

                Then("should delete all existing snapshots") {
                    verify { snapshotRepository.deleteAll() }
                }

                Then("should save snapshots for all months") {
                    verify(exactly = Constants.XYBAR_CHART_MONTHS + 1) {
                        snapshotRepository.save(any())
                    }
                }
            }
        }

        Given("ticker with purchases and current quantity") {
            When("calculating monthly invested value") {
                val ticker =
                    TickerFactory.create(
                        id = 1,
                        symbol = "AAPL",
                        currentQuantity = BigDecimal("150"),
                        averageUnitValue = BigDecimal("100.00"),
                        createdAt = LocalDateTime.now().minusMonths(3),
                    )

                val purchase1 =
                    TickerPurchaseFactory.create(
                        id = 1,
                        ticker = ticker,
                        quantity = BigDecimal("100"),
                        unitPrice = BigDecimal("100.00"),
                        walletTransaction =
                            WalletTransactionFactory.create(
                                date = LocalDateTime.now().minusMonths(2),
                            ),
                    )

                val purchase2 =
                    TickerPurchaseFactory.create(
                        id = 2,
                        ticker = ticker,
                        quantity = BigDecimal("50"),
                        unitPrice = BigDecimal("100.00"),
                        walletTransaction =
                            WalletTransactionFactory.create(
                                date = LocalDateTime.now().minusMonths(1),
                            ),
                    )

                every { snapshotRepository.count() } returns 0
                every { snapshotRepository.findByReferenceMonth(any()) } returns null
                every { tickerService.getAllNonArchivedTickers() } returns listOf(ticker)
                every { tickerService.getAllPurchases() } returns listOf(purchase1, purchase2)
                every { tickerService.getAllSales() } returns emptyList()
                every { tickerService.getAllDividends() } returns emptyList()
                every { tickerService.getAllNonArchivedSales() } returns emptyList()
                every { tickerService.getAllPurchasesByTicker(ticker.id!!) } returns listOf(purchase1, purchase2)
                every { bondService.getAllNonArchivedBonds() } returns emptyList()
                every { bondService.getAllOperations() } returns emptyList()
                every { tickerPriceHistoryService.getClosestPriceBeforeDate(any(), any()) } returns BigDecimal("110.00")
                every { snapshotRepository.save(any()) } returnsArgument 0

                val result = service.getPerformanceData()

                Then("should calculate invested value correctly") {
                    result.monthlyInvested.values.any { it > BigDecimal.ZERO } shouldBe true
                }

                Then("should save snapshots") {
                    verify(atLeast = 1) { snapshotRepository.save(any()) }
                }
            }
        }

        Given("ticker without purchases but with current quantity") {
            When("calculating monthly invested value") {
                val ticker =
                    TickerFactory.create(
                        id = 1,
                        symbol = "GOOGL",
                        currentQuantity = BigDecimal("50"),
                        averageUnitValue = BigDecimal("200.00"),
                        createdAt = LocalDateTime.now().minusMonths(6),
                    )

                every { snapshotRepository.count() } returns 0
                every { snapshotRepository.findByReferenceMonth(any()) } returns null
                every { tickerService.getAllNonArchivedTickers() } returns listOf(ticker)
                every { tickerService.getAllPurchases() } returns emptyList()
                every { tickerService.getAllSales() } returns emptyList()
                every { tickerService.getAllDividends() } returns emptyList()
                every { tickerService.getAllNonArchivedSales() } returns emptyList()
                every { tickerService.getAllPurchasesByTicker(ticker.id!!) } returns emptyList()
                every { bondService.getAllNonArchivedBonds() } returns emptyList()
                every { bondService.getAllOperations() } returns emptyList()
                every { tickerPriceHistoryService.getClosestPriceBeforeDate(any(), any()) } returns BigDecimal("220.00")
                every { snapshotRepository.save(any()) } returnsArgument 0

                val result = service.getPerformanceData()

                Then("should include ticker in invested value calculation") {
                    result.monthlyInvested.values.any { it.isEqual(10000) } shouldBe true
                }
            }
        }

        Given("ticker with purchases and sales") {
            When("calculating monthly portfolio value") {
                val ticker =
                    TickerFactory.create(
                        id = 1,
                        symbol = "MSFT",
                        currentQuantity = BigDecimal("80"),
                        currentUnitValue = BigDecimal("120.00"),
                        averageUnitValue = BigDecimal("100.00"),
                        createdAt = LocalDateTime.now().minusMonths(4),
                    )

                val purchase =
                    TickerPurchaseFactory.create(
                        id = 1,
                        ticker = ticker,
                        quantity = BigDecimal("100"),
                        unitPrice = BigDecimal("100.00"),
                        walletTransaction =
                            WalletTransactionFactory.create(
                                date = LocalDateTime.now().minusMonths(3),
                            ),
                    )

                val sale =
                    TickerSaleFactory.create(
                        id = 1,
                        ticker = ticker,
                        quantity = BigDecimal("20"),
                        unitPrice = BigDecimal("120.00"),
                        averageCost = BigDecimal("100.00"),
                        walletTransaction =
                            WalletTransactionFactory.create(
                                date = LocalDateTime.now().minusMonths(1),
                            ),
                    )

                every { snapshotRepository.count() } returns 0
                every { snapshotRepository.findByReferenceMonth(any()) } returns null
                every { tickerService.getAllNonArchivedTickers() } returns listOf(ticker)
                every { tickerService.getAllPurchases() } returns listOf(purchase)
                every { tickerService.getAllSales() } returns listOf(sale)
                every { tickerService.getAllDividends() } returns emptyList()
                every { tickerService.getAllNonArchivedSales() } returns listOf(sale)
                every { tickerService.getAllPurchasesByTicker(ticker.id!!) } returns listOf(purchase)
                every { bondService.getAllNonArchivedBonds() } returns emptyList()
                every { bondService.getAllOperations() } returns emptyList()
                every { tickerPriceHistoryService.getClosestPriceBeforeDate(any(), any()) } returns BigDecimal("120.00")
                every { snapshotRepository.save(any()) } returnsArgument 0

                val result = service.getPerformanceData()

                Then("should calculate portfolio value with sales considered") {
                    result.portfolioValues.values.any { it > BigDecimal.ZERO } shouldBe true
                }

                Then("should calculate monthly gains from sales") {
                    result.monthlyGains.values.any { it > BigDecimal.ZERO } shouldBe true
                }
            }
        }

        Given("ticker with dividends") {
            When("calculating monthly capital gains") {
                val ticker =
                    TickerFactory.create(
                        id = 1,
                        symbol = "DIVIDEND_STOCK",
                        currentQuantity = BigDecimal("100"),
                        averageUnitValue = BigDecimal("50.00"),
                    )

                val dividend1 =
                    DividendFactory.create(
                        id = 1,
                        ticker = ticker,
                        walletTransaction =
                            WalletTransactionFactory.create(
                                amount = BigDecimal("100.00"),
                                date = LocalDateTime.now().minusMonths(2),
                            ),
                    )

                val dividend2 =
                    DividendFactory.create(
                        id = 2,
                        ticker = ticker,
                        walletTransaction =
                            WalletTransactionFactory.create(
                                amount = BigDecimal("150.00"),
                                date = LocalDateTime.now().minusMonths(1),
                            ),
                    )

                every { snapshotRepository.count() } returns 0
                every { snapshotRepository.findByReferenceMonth(any()) } returns null
                every { tickerService.getAllNonArchivedTickers() } returns listOf(ticker)
                every { tickerService.getAllPurchases() } returns emptyList()
                every { tickerService.getAllSales() } returns emptyList()
                every { tickerService.getAllDividends() } returns listOf(dividend1, dividend2)
                every { tickerService.getAllNonArchivedSales() } returns emptyList()
                every { tickerService.getAllPurchasesByTicker(ticker.id!!) } returns emptyList()
                every { bondService.getAllNonArchivedBonds() } returns emptyList()
                every { bondService.getAllOperations() } returns emptyList()
                every { tickerPriceHistoryService.getClosestPriceBeforeDate(any(), any()) } returns BigDecimal("55.00")
                every { snapshotRepository.save(any()) } returnsArgument 0

                val result = service.getPerformanceData()

                Then("should include dividends in monthly gains") {
                    result.monthlyGains.values.any { it.isEqual(100) || it.isEqual(150) } shouldBe true
                }

                Then("should include dividends in accumulated gains") {
                    result.accumulatedGains.values.any { it >= BigDecimal("100.00") } shouldBe true
                }
            }
        }

        Given("bond with operations") {
            When("calculating monthly invested value with bonds") {
                val bond =
                    BondFactory.create(
                        id = 1,
                        name = "CDB Test",
                    )

                val bondOperation =
                    BondOperationFactory.create(
                        id = 1,
                        bond = bond,
                        operationType = OperationType.BUY,
                        quantity = BigDecimal("100"),
                        unitPrice = BigDecimal("10.00"),
                        walletTransaction =
                            WalletTransactionFactory.create(
                                date = LocalDateTime.now().minusMonths(2),
                            ),
                    )

                every { snapshotRepository.count() } returns 0
                every { snapshotRepository.findByReferenceMonth(any()) } returns null
                every { tickerService.getAllNonArchivedTickers() } returns emptyList()
                every { tickerService.getAllPurchases() } returns emptyList()
                every { tickerService.getAllSales() } returns emptyList()
                every { tickerService.getAllDividends() } returns emptyList()
                every { tickerService.getAllNonArchivedSales() } returns emptyList()
                every { tickerService.getAllPurchasesByTicker(any()) } returns emptyList()
                every { bondService.getAllNonArchivedBonds() } returns listOf(bond)
                every { bondService.getOperationsByBond(bond) } returns listOf(bondOperation)
                every { bondService.getAllOperations() } returns listOf(bondOperation)
                every { bondService.getMonthlyInterestHistory(bond.id!!) } returns emptyList()
                every { bondInterestCalculationService.getMonthlyInterestHistory(bond) } returns emptyList()
                every { snapshotRepository.save(any()) } returnsArgument 0

                val result = service.getPerformanceData()

                Then("should include bond investment in monthly invested value") {
                    result.monthlyInvested.values.any { it.isEqual(1000) } shouldBe true
                }
            }
        }

        Given("bond with interest calculations") {
            When("calculating portfolio value with bond interest") {
                val bond =
                    BondFactory.create(
                        id = 1,
                        name = "CDB Interest",
                    )

                val bondOperation =
                    BondOperationFactory.create(
                        id = 1,
                        bond = bond,
                        operationType = OperationType.BUY,
                        quantity = BigDecimal("100"),
                        unitPrice = BigDecimal("10.00"),
                        walletTransaction =
                            WalletTransactionFactory.create(
                                date = LocalDateTime.now().minusMonths(3),
                            ),
                    )

                val interestCalc1 =
                    BondInterestCalculationFactory.create(
                        id = 1,
                        bond = bond,
                        referenceMonth = YearMonth.now().minusMonths(2),
                        monthlyInterest = BigDecimal("10.00"),
                        accumulatedInterest = BigDecimal("10.00"),
                    )

                val interestCalc2 =
                    BondInterestCalculationFactory.create(
                        id = 2,
                        bond = bond,
                        referenceMonth = YearMonth.now().minusMonths(1),
                        monthlyInterest = BigDecimal("10.50"),
                        accumulatedInterest = BigDecimal("20.50"),
                    )

                every { snapshotRepository.count() } returns 0
                every { snapshotRepository.findByReferenceMonth(any()) } returns null
                every { tickerService.getAllNonArchivedTickers() } returns emptyList()
                every { tickerService.getAllPurchases() } returns emptyList()
                every { tickerService.getAllSales() } returns emptyList()
                every { tickerService.getAllDividends() } returns emptyList()
                every { tickerService.getAllNonArchivedSales() } returns emptyList()
                every { tickerService.getAllPurchasesByTicker(any()) } returns emptyList()
                every { bondService.getAllNonArchivedBonds() } returns listOf(bond)
                every { bondService.getOperationsByBond(bond) } returns listOf(bondOperation)
                every { bondService.getAllOperations() } returns listOf(bondOperation)
                every { bondService.getMonthlyInterestHistory(bond.id!!) } returns listOf(interestCalc1, interestCalc2)
                every { bondInterestCalculationService.getMonthlyInterestHistory(bond) } returns
                    listOf(
                        interestCalc1,
                        interestCalc2,
                    )
                every { snapshotRepository.save(any()) } returnsArgument 0

                val result = service.getPerformanceData()

                Then("should include bond interest in portfolio value") {
                    result.portfolioValues.values.any { it > BigDecimal("1000.00") } shouldBe true
                }

                Then("should include bond interest in monthly gains") {
                    result.monthlyGains.values.any { it.isEqual(10) || it.isEqual(10.5) } shouldBe true
                }
            }
        }

        Given("bond with sell operation and profit") {
            When("calculating monthly capital gains") {
                val bond =
                    BondFactory.create(
                        id = 1,
                        name = "CDB Sold",
                    )

                val buyOperation =
                    BondOperationFactory.create(
                        id = 1,
                        bond = bond,
                        operationType = OperationType.BUY,
                        quantity = BigDecimal("100"),
                        unitPrice = BigDecimal("10.00"),
                        walletTransaction =
                            WalletTransactionFactory.create(
                                date = LocalDateTime.now().minusMonths(3),
                            ),
                    )

                val sellOperation =
                    BondOperationFactory.create(
                        id = 2,
                        bond = bond,
                        operationType = OperationType.SELL,
                        quantity = BigDecimal("50"),
                        unitPrice = BigDecimal("12.00"),
                        netProfit = BigDecimal("100.00"),
                        walletTransaction =
                            WalletTransactionFactory.create(
                                date = LocalDateTime.now().minusMonths(1),
                            ),
                    )

                every { snapshotRepository.count() } returns 0
                every { snapshotRepository.findByReferenceMonth(any()) } returns null
                every { tickerService.getAllNonArchivedTickers() } returns emptyList()
                every { tickerService.getAllPurchases() } returns emptyList()
                every { tickerService.getAllSales() } returns emptyList()
                every { tickerService.getAllDividends() } returns emptyList()
                every { tickerService.getAllNonArchivedSales() } returns emptyList()
                every { tickerService.getAllPurchasesByTicker(any()) } returns emptyList()
                every { bondService.getAllNonArchivedBonds() } returns listOf(bond)
                every { bondService.getOperationsByBond(bond) } returns listOf(buyOperation, sellOperation)
                every { bondService.getAllOperations() } returns listOf(buyOperation, sellOperation)
                every { bondService.getMonthlyInterestHistory(bond.id!!) } returns emptyList()
                every { bondInterestCalculationService.getMonthlyInterestHistory(bond) } returns emptyList()
                every { snapshotRepository.save(any()) } returnsArgument 0

                val result = service.getPerformanceData()

                Then("should include bond sale profit in monthly gains") {
                    result.monthlyGains.values.any { it.isEqual(100) } shouldBe true
                }
            }
        }

        Given("existing snapshots in cache") {
            When("getting performance data with complete cache") {
                val currentMonth = YearMonth.now()
                val snapshot1 =
                    InvestmentPerformanceSnapshotFactory.create(
                        id = 1,
                        referenceMonth = currentMonth,
                        investedValue = BigDecimal("5000.00"),
                        portfolioValue = BigDecimal("5500.00"),
                        accumulatedCapitalGains = BigDecimal("500.00"),
                        monthlyCapitalGains = BigDecimal("100.00"),
                    )

                val snapshot2 =
                    InvestmentPerformanceSnapshotFactory.create(
                        id = 2,
                        referenceMonth = currentMonth.minusMonths(1),
                        investedValue = BigDecimal("4800.00"),
                        portfolioValue = BigDecimal("5200.00"),
                        accumulatedCapitalGains = BigDecimal("400.00"),
                        monthlyCapitalGains = BigDecimal("80.00"),
                    )

                every { snapshotRepository.count() } returns 2
                every { snapshotRepository.findByReferenceMonth(currentMonth) } returns snapshot1
                every { snapshotRepository.findByReferenceMonth(currentMonth.minusMonths(1)) } returns snapshot2
                every {
                    snapshotRepository.findByReferenceMonth(
                        match { it != currentMonth && it != currentMonth.minusMonths(1) },
                    )
                } returns null
                every { tickerService.getAllNonArchivedTickers() } returns emptyList()
                every { tickerService.getAllPurchases() } returns emptyList()
                every { tickerService.getAllSales() } returns emptyList()
                every { tickerService.getAllDividends() } returns emptyList()
                every { tickerService.getAllNonArchivedSales() } returns emptyList()
                every { tickerService.getAllPurchasesByTicker(any()) } returns emptyList()
                every { bondService.getAllNonArchivedBonds() } returns emptyList()
                every { bondService.getAllOperations() } returns emptyList()
                every { bondService.getOperationsByBond(any()) } returns emptyList()
                every { snapshotRepository.save(any()) } returnsArgument 0

                val result = service.getPerformanceData()

                Then("should return cached data for existing months") {
                    result.monthlyInvested[currentMonth] shouldBe BigDecimal("5000.00")
                    result.portfolioValues[currentMonth] shouldBe BigDecimal("5500.00")
                    result.accumulatedGains[currentMonth] shouldBe BigDecimal("500.00")
                    result.monthlyGains[currentMonth] shouldBe BigDecimal("100.00")
                }

                Then("should calculate and save missing months") {
                    verify(atLeast = Constants.XYBAR_CHART_MONTHS - 1) {
                        snapshotRepository.save(any())
                    }
                }
            }
        }

        Given("partial cache with missing months") {
            When("getting performance data") {
                val currentMonth = YearMonth.now()
                val cachedSnapshot =
                    InvestmentPerformanceSnapshotFactory.create(
                        id = 1,
                        referenceMonth = currentMonth,
                        investedValue = BigDecimal("3000.00"),
                        portfolioValue = BigDecimal("3300.00"),
                    )

                every { snapshotRepository.count() } returns 1
                every { snapshotRepository.findByReferenceMonth(currentMonth) } returns cachedSnapshot
                every { snapshotRepository.findByReferenceMonth(match { it != currentMonth }) } returns null
                every { tickerService.getAllNonArchivedTickers() } returns emptyList()
                every { tickerService.getAllPurchases() } returns emptyList()
                every { tickerService.getAllSales() } returns emptyList()
                every { tickerService.getAllDividends() } returns emptyList()
                every { tickerService.getAllNonArchivedSales() } returns emptyList()
                every { tickerService.getAllPurchasesByTicker(any()) } returns emptyList()
                every { bondService.getAllNonArchivedBonds() } returns emptyList()
                every { bondService.getAllOperations() } returns emptyList()
                every { bondService.getOperationsByBond(any()) } returns emptyList()
                every { snapshotRepository.save(any()) } returnsArgument 0

                val result = service.getPerformanceData()

                Then("should use cached data for existing month") {
                    result.monthlyInvested[currentMonth] shouldBe BigDecimal("3000.00")
                }

                Then("should calculate missing months") {
                    verify(exactly = Constants.XYBAR_CHART_MONTHS) {
                        snapshotRepository.save(any())
                    }
                }
            }
        }

        Given("existing snapshot for a month") {
            When("saving new snapshot for same month") {
                val referenceMonth = YearMonth.now()
                val existingSnapshot =
                    InvestmentPerformanceSnapshotFactory.create(
                        id = 1,
                        referenceMonth = referenceMonth,
                        investedValue = BigDecimal("1000.00"),
                        portfolioValue = BigDecimal("1100.00"),
                    )

                every { snapshotRepository.deleteAll() } returns Unit
                every { snapshotRepository.findByReferenceMonth(any()) } returns existingSnapshot
                every { tickerService.getAllNonArchivedTickers() } returns emptyList()
                every { tickerService.getAllPurchases() } returns emptyList()
                every { tickerService.getAllSales() } returns emptyList()
                every { tickerService.getAllDividends() } returns emptyList()
                every { tickerService.getAllNonArchivedSales() } returns emptyList()
                every { tickerService.getAllPurchasesByTicker(any()) } returns emptyList()
                every { bondService.getAllNonArchivedBonds() } returns emptyList()
                every { bondService.getAllOperations() } returns emptyList()
                every { bondService.getOperationsByBond(any()) } returns emptyList()
                every { snapshotRepository.save(any()) } returnsArgument 0

                service.recalculateAllSnapshots()

                Then("should delete all snapshots before recalculating") {
                    verify { snapshotRepository.deleteAll() }
                }
            }
        }

        Given("multiple tickers with different asset types") {
            When("calculating performance metrics") {
                val stockTicker =
                    TickerFactory.create(
                        id = 1,
                        type = AssetType.STOCK,
                        symbol = "STOCK1",
                        currentQuantity = BigDecimal("100"),
                        averageUnitValue = BigDecimal("50.00"),
                    )

                val cryptoTicker =
                    TickerFactory.create(
                        id = 2,
                        type = AssetType.CRYPTOCURRENCY,
                        symbol = "BTC",
                        currentQuantity = BigDecimal("2"),
                        averageUnitValue = BigDecimal("30000.00"),
                    )

                every { snapshotRepository.count() } returns 0
                every { snapshotRepository.findByReferenceMonth(any()) } returns null
                every { tickerService.getAllNonArchivedTickers() } returns listOf(stockTicker, cryptoTicker)
                every { tickerService.getAllPurchases() } returns emptyList()
                every { tickerService.getAllSales() } returns emptyList()
                every { tickerService.getAllDividends() } returns emptyList()
                every { tickerService.getAllNonArchivedSales() } returns emptyList()
                every { tickerService.getAllPurchasesByTicker(stockTicker.id!!) } returns emptyList()
                every { tickerService.getAllPurchasesByTicker(cryptoTicker.id!!) } returns emptyList()
                every { bondService.getAllNonArchivedBonds() } returns emptyList()
                every { bondService.getAllOperations() } returns emptyList()
                every { tickerPriceHistoryService.getClosestPriceBeforeDate(stockTicker, any()) } returns
                    BigDecimal("55.00")
                every { tickerPriceHistoryService.getClosestPriceBeforeDate(cryptoTicker, any()) } returns
                    BigDecimal("32000.00")
                every { snapshotRepository.save(any()) } returnsArgument 0

                val result = service.getPerformanceData()

                Then("should calculate invested value for all tickers") {
                    result.monthlyInvested.values.any { it >= BigDecimal("5000.00") } shouldBe true
                }

                Then("should calculate portfolio value for all tickers") {
                    result.portfolioValues.values.any { it > BigDecimal.ZERO } shouldBe true
                }
            }
        }

        Given("ticker with zero current quantity and no purchases") {
            When("calculating monthly invested value") {
                val ticker =
                    TickerFactory.create(
                        id = 1,
                        symbol = "EMPTY",
                        currentQuantity = BigDecimal.ZERO,
                        averageUnitValue = BigDecimal("100.00"),
                    )

                every { snapshotRepository.count() } returns 0
                every { snapshotRepository.findByReferenceMonth(any()) } returns null
                every { tickerService.getAllNonArchivedTickers() } returns listOf(ticker)
                every { tickerService.getAllPurchases() } returns emptyList()
                every { tickerService.getAllSales() } returns emptyList()
                every { tickerService.getAllDividends() } returns emptyList()
                every { tickerService.getAllNonArchivedSales() } returns emptyList()
                every { tickerService.getAllPurchasesByTicker(ticker.id!!) } returns emptyList()
                every { bondService.getAllNonArchivedBonds() } returns emptyList()
                every { bondService.getAllOperations() } returns emptyList()
                every { snapshotRepository.save(any()) } returnsArgument 0

                val result = service.getPerformanceData()

                Then("should not include ticker in calculations") {
                    result.monthlyInvested.values.all { it.isEqual(0) } shouldBe true
                }
            }
        }

        Given("bond without operations") {
            When("calculating monthly invested value") {
                val bond =
                    BondFactory.create(
                        id = 1,
                        name = "Empty Bond",
                    )

                every { snapshotRepository.count() } returns 0
                every { snapshotRepository.findByReferenceMonth(any()) } returns null
                every { tickerService.getAllNonArchivedTickers() } returns emptyList()
                every { tickerService.getAllPurchases() } returns emptyList()
                every { tickerService.getAllSales() } returns emptyList()
                every { tickerService.getAllDividends() } returns emptyList()
                every { tickerService.getAllNonArchivedSales() } returns emptyList()
                every { tickerService.getAllPurchasesByTicker(any()) } returns emptyList()
                every { bondService.getAllNonArchivedBonds() } returns listOf(bond)
                every { bondService.getOperationsByBond(bond) } returns emptyList()
                every { bondService.getAllOperations() } returns emptyList()
                every { bondService.getMonthlyInterestHistory(any()) } returns emptyList()
                every { bondInterestCalculationService.getMonthlyInterestHistory(bond) } returns emptyList()
                every { snapshotRepository.save(any()) } returnsArgument 0

                val result = service.getPerformanceData()

                Then("should not include bond in calculations") {
                    result.monthlyInvested.values.all { it.isEqual(0) } shouldBe true
                }
            }
        }
    })
