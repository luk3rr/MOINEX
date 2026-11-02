package org.moinex.service.investment

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import org.moinex.common.ClockProvider
import org.moinex.factory.investment.DividendFactory
import org.moinex.factory.investment.TickerFactory
import org.moinex.factory.investment.TickerPurchaseFactory
import org.moinex.factory.investment.TickerSaleFactory
import org.moinex.factory.wallet.WalletTransactionFactory
import org.moinex.model.dto.AllocationDTO
import org.moinex.model.dto.DailyPricePointDTO
import org.moinex.model.dto.RebalanceAction
import org.moinex.model.dto.TickerSort
import org.moinex.model.dto.TradeSide
import org.moinex.model.enums.AssetType
import org.moinex.repository.investment.DividendRepository
import org.moinex.repository.investment.TickerPurchaseRepository
import org.moinex.repository.investment.TickerRepository
import org.moinex.repository.investment.TickerSaleRepository
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime

class PortfolioAnalyticsServiceTest :
    BehaviorSpec({
        val today = LocalDate.of(2026, 11, 1)

        fun ticker(
            id: Int,
            name: String,
            type: AssetType = AssetType.STOCK,
            quantity: String,
            price: String,
            average: String,
        ) = TickerFactory.create(
            id = id,
            name = name,
            symbol = "T$id",
            type = type,
            currentQuantity = BigDecimal(quantity),
            currentUnitValue = BigDecimal(price),
            averageUnitValue = BigDecimal(average),
        )

        fun dividend(
            ticker: org.moinex.model.investment.Ticker,
            amount: String,
            date: LocalDate,
        ) = DividendFactory.create(
            ticker = ticker,
            walletTransaction =
                WalletTransactionFactory.create(amount = BigDecimal(amount), date = date.atStartOfDay()),
        )

        fun point(
            id: Int,
            date: String,
            price: String,
        ) = DailyPricePointDTO(id, LocalDate.parse(date), BigDecimal(price))

        val petr = ticker(1, "Petrobras", quantity = "100", price = "40", average = "30")
        val itub = ticker(2, "Itau", quantity = "50", price = "20", average = "25")
        val bitcoin = ticker(3, "Bitcoin", AssetType.CRYPTOCURRENCY, quantity = "1", price = "160", average = "100")
        val closed = ticker(4, "Closed", quantity = "0", price = "10", average = "10")
        val all = listOf(petr, itub, bitcoin, closed)

        Given("tickers with positions") {
            val rows = PortfolioAnalyticsService.buildRows(all, emptyList(), emptyMap(), today)
            val byId = rows.associateBy { it.ticker.id }

            Then("the return is the profit over the cost, and negative returns keep their sign") {
                byId.getValue(1).returnPct shouldBe BigDecimal("33.33")
                byId.getValue(1).profitLoss.compareTo(BigDecimal("1000")) shouldBe 0
                byId.getValue(2).returnPct shouldBe BigDecimal("-20.00")
                byId.getValue(2).profitLoss.compareTo(BigDecimal("-250")) shouldBe 0
            }

            Then("a closed position has no return") {
                byId.getValue(4).returnPct shouldBe null
                byId.getValue(4).weightPct shouldBe BigDecimal("0.00")
            }

            Then("the weights are shares of the whole portfolio and add up to 100") {
                // values: 4000, 1000, 160, 0 -> total 5160
                byId.getValue(1).weightPct shouldBe BigDecimal("77.52")
                byId.getValue(2).weightPct shouldBe BigDecimal("19.38")
                byId.getValue(3).weightPct shouldBe BigDecimal("3.10")
            }
        }

        Given("dividends received") {
            val recent = dividend(petr, "300", today.minusMonths(3))
            val old = dividend(petr, "999", today.minusMonths(13))
            val other = dividend(itub, "50", today.minusDays(10))

            Then("the yield on cost sums only the last 12 months over the current cost") {
                val rows = PortfolioAnalyticsService.buildRows(all, listOf(recent, old, other), emptyMap(), today)
                val byId = rows.associateBy { it.ticker.id }

                byId.getValue(1).dyOnCostPct shouldBe BigDecimal("10.00")
                byId.getValue(2).dyOnCostPct shouldBe BigDecimal("4.00")
                byId.getValue(3).dyOnCostPct shouldBe BigDecimal("0.00")
                byId.getValue(4).dyOnCostPct shouldBe null
            }
        }

        Given("a daily series") {
            val series =
                mapOf(
                    1 to
                        listOf(
                            point(1, "2026-10-28", "36"),
                            point(1, "2026-10-30", "38"),
                            point(1, "2026-11-01", "39"),
                        ),
                    2 to listOf(point(2, "2026-11-01", "20")),
                )

            Then("when the series has today the day change uses the current price against the close before it") {
                val rows = PortfolioAnalyticsService.buildRows(all, emptyList(), series, today)

                rows.first { it.ticker.id == 1 }.dayChangePct shouldBe BigDecimal("5.26")
            }

            Then("on a day without session the day change is the one of the last stored session") {
                val weekend = mapOf(1 to listOf(point(1, "2026-10-29", "37"), point(1, "2026-10-30", "38")))
                val rows = PortfolioAnalyticsService.buildRows(all, emptyList(), weekend, today)

                rows.first { it.ticker.id == 1 }.dayChangePct shouldBe BigDecimal("2.70")
            }

            Then("closes too old or too far apart are not a day change") {
                val stale = mapOf(1 to listOf(point(1, "2026-10-10", "30"), point(1, "2026-10-12", "31")))
                val gap = mapOf(1 to listOf(point(1, "2026-10-10", "30"), point(1, "2026-10-30", "38")))

                PortfolioAnalyticsService
                    .buildRows(all, emptyList(), stale, today)
                    .first { it.ticker.id == 1 }
                    .dayChangePct shouldBe null
                PortfolioAnalyticsService
                    .buildRows(all, emptyList(), gap, today)
                    .first { it.ticker.id == 1 }
                    .dayChangePct shouldBe null
            }

            Then("a ticker with fewer than two closes has no day change") {
                val rows = PortfolioAnalyticsService.buildRows(all, emptyList(), series, today)

                rows.first { it.ticker.id == 2 }.dayChangePct shouldBe null
                rows.first { it.ticker.id == 3 }.dayChangePct shouldBe null
            }

            Then("the sparkline carries the closes oldest first") {
                val rows = PortfolioAnalyticsService.buildRows(all, emptyList(), series, today)

                rows.first { it.ticker.id == 1 }.sparkline.map { it.toInt() } shouldContainExactly listOf(36, 38, 39)
                rows.first { it.ticker.id == 4 }.sparkline shouldBe emptyList()
            }
        }

        Given("rows to filter and order") {
            val series =
                mapOf(
                    1 to listOf(point(1, "2026-10-29", "36"), point(1, "2026-10-30", "38")),
                    2 to listOf(point(2, "2026-10-29", "24"), point(2, "2026-10-30", "22")),
                )
            val rows = PortfolioAnalyticsService.buildRows(all, emptyList(), series, today)

            Then("the type filter keeps only that type and null keeps everything") {
                PortfolioAnalyticsService
                    .filterAndSort(rows, AssetType.CRYPTOCURRENCY, TickerSort.NAME)
                    .map { it.ticker.id } shouldContainExactly listOf(3)
                PortfolioAnalyticsService.filterAndSort(rows, null, TickerSort.NAME).size shouldBe 4
            }

            Then("by name it is alphabetical") {
                PortfolioAnalyticsService
                    .filterAndSort(rows, null, TickerSort.NAME)
                    .map { it.ticker.name } shouldContainExactly listOf("Bitcoin", "Closed", "Itau", "Petrobras")
            }

            Then("by return the best comes first and a missing return goes last") {
                PortfolioAnalyticsService
                    .filterAndSort(rows, null, TickerSort.RETURN)
                    .map { it.ticker.id } shouldContainExactly listOf(3, 1, 2, 4)
            }

            Then("by weight the heaviest comes first") {
                PortfolioAnalyticsService
                    .filterAndSort(rows, null, TickerSort.WEIGHT)
                    .map { it.ticker.id } shouldContainExactly listOf(1, 2, 3, 4)
            }

            Then("by day change the biggest rise comes first and missing values go last") {
                val reversed = rows.map { it.copy() }.sortedByDescending { it.ticker.id }

                PortfolioAnalyticsService
                    .filterAndSort(reversed, null, TickerSort.DAY_CHANGE)
                    .map { it.ticker.id } shouldContainExactly listOf(1, 2, 4, 3)
            }
        }

        Given("a ticker with trades inside and outside the window") {
            val points =
                listOf(
                    point(1, "2026-04-15", "30"),
                    point(1, "2026-07-01", "35"),
                    point(1, "2026-10-29", "37"),
                    point(1, "2026-10-30", "38"),
                )
            val purchases =
                listOf(
                    TickerPurchaseFactory.create(
                        ticker = petr,
                        quantity = BigDecimal("60"),
                        unitPrice = BigDecimal("29"),
                        walletTransaction =
                            WalletTransactionFactory.create(
                                date = LocalDateTime.of(2026, 4, 10, 10, 0),
                            ),
                    ),
                    TickerPurchaseFactory.create(
                        ticker = petr,
                        quantity = BigDecimal("50"),
                        unitPrice = BigDecimal("33"),
                        walletTransaction = WalletTransactionFactory.create(date = LocalDateTime.of(2026, 7, 2, 10, 0)),
                    ),
                )
            val sales =
                listOf(
                    TickerSaleFactory.create(
                        ticker = petr,
                        quantity = BigDecimal("10"),
                        unitPrice = BigDecimal("37"),
                        walletTransaction =
                            WalletTransactionFactory.create(
                                date = LocalDateTime.of(2026, 10, 20, 10, 0),
                            ),
                    ),
                )

            Then("only prices and trades of the window are kept, trades oldest first") {
                val detail = PortfolioAnalyticsService.buildDetail(petr, points, purchases, sales, 6, today)

                detail.prices.map { it.date.toString() } shouldContainExactly
                    listOf("2026-07-01", "2026-10-29", "2026-10-30")
                detail.trades.map { it.side to it.date.toString() } shouldContainExactly
                    listOf(TradeSide.BUY to "2026-07-02", TradeSide.SELL to "2026-10-20")
            }

            Then("a longer window brings the older purchase in") {
                val detail = PortfolioAnalyticsService.buildDetail(petr, points, purchases, sales, 12, today)

                detail.trades.size shouldBe 3
                detail.prices.size shouldBe 4
            }

            Then("the position numbers describe the whole position, whatever the window") {
                val detail = PortfolioAnalyticsService.buildDetail(petr, points, purchases, sales, 3, today)

                detail.averagePrice.compareTo(BigDecimal("30")) shouldBe 0
                detail.quantity.compareTo(BigDecimal("100")) shouldBe 0
                detail.returnPct shouldBe BigDecimal("33.33")
                detail.profitLoss.compareTo(BigDecimal("1000")) shouldBe 0
                detail.dayChangePct shouldBe BigDecimal("2.70")
            }

            Then("a purchase without a wallet transaction date is ignored") {
                val broken = TickerPurchaseFactory.create(ticker = petr)
                broken.walletTransaction = null

                PortfolioAnalyticsService
                    .buildDetail(
                        petr,
                        points,
                        listOf(broken),
                        emptyList(),
                        12,
                        today,
                    ).trades shouldBe
                    emptyList()
            }
        }

        Given("a position with dividends and a year of prices") {
            val rich = ticker(31, "Rich", quantity = "10", price = "50", average = "40")
            val other = ticker(32, "Other", quantity = "10", price = "50", average = "40")
            val prices =
                listOf(
                    point(31, "2026-01-10", "30"),
                    point(31, "2026-06-10", "80"),
                    point(31, "2026-10-30", "50"),
                )
            val paid =
                listOf(
                    dividend(rich, "20", LocalDate.of(2026, 10, 5)),
                    dividend(rich, "30", LocalDate.of(2026, 8, 5)),
                    dividend(rich, "100", LocalDate.of(2024, 3, 5)),
                    dividend(other, "999", LocalDate.of(2026, 10, 5)),
                )
            val detail =
                PortfolioAnalyticsService.buildDetail(
                    rich,
                    prices,
                    emptyList(),
                    emptyList(),
                    12,
                    today,
                    paid,
                    BigDecimal("2000"),
                )

            Then("the position tells its value, cost and share of the portfolio") {
                detail.position.value.compareTo(BigDecimal("500")) shouldBe 0
                detail.position.cost.compareTo(BigDecimal("400")) shouldBe 0
                detail.position.weightPct shouldBe BigDecimal("25.00")
            }

            Then("the twelve month range comes from the daily prices, whatever the chart window") {
                val short = PortfolioAnalyticsService.buildDetail(rich, prices, emptyList(), emptyList(), 3, today)

                short.position.highest12m?.compareTo(BigDecimal("80")) shouldBe 0
                short.position.lowest12m?.compareTo(BigDecimal("30")) shouldBe 0
                detail.position.distanceFromHighestPct shouldBe BigDecimal("-37.50")
            }

            Then("without prices there is no range") {
                val none = PortfolioAnalyticsService.buildDetail(rich, emptyList(), emptyList(), emptyList(), 12, today)

                none.position.highest12m shouldBe null
                none.position.distanceFromHighestPct shouldBe null
            }

            Then("only the dividends of this ticker count: twelve months, all time and the last one") {
                detail.income.total12m.compareTo(BigDecimal("50")) shouldBe 0
                detail.income.totalReceived.compareTo(BigDecimal("150")) shouldBe 0
                detail.income.lastAmount?.compareTo(BigDecimal("20")) shouldBe 0
                detail.income.lastDate shouldBe LocalDate.of(2026, 10, 5)
                detail.income.hasDividends() shouldBe true
            }

            Then("the yield on cost is the twelve months over the cost") {
                detail.income.dyOnCostPct shouldBe BigDecimal("12.50")
            }

            Then("a ticker that never paid has no income") {
                val none =
                    PortfolioAnalyticsService.buildDetail(
                        other,
                        emptyList(),
                        emptyList(),
                        emptyList(),
                        12,
                        today,
                        emptyList(),
                    )

                none.income.hasDividends() shouldBe false
                none.income.lastAmount shouldBe null
                none.income.lastDate shouldBe null
                none.income.panel.series shouldBe emptyList()
            }
        }

        Given("a ticker id that does not exist") {
            val tickerRepository = mockk<TickerRepository>()
            val service =
                PortfolioAnalyticsService(
                    tickerRepository,
                    mockk<DividendRepository>(),
                    mockk<TickerPurchaseRepository>(),
                    mockk<TickerSaleRepository>(),
                    mockk<TickerPriceHistoryService>(),
                    ClockProvider(),
                )

            Then("there is no detail") {
                every { tickerRepository.findById(99) } returns java.util.Optional.empty()

                service.tickerDetail(99, 12) shouldBe null
            }
        }

        Given("dividends received over the last twelve months") {
            val a = ticker(11, "Alpha", quantity = "1", price = "1", average = "1")
            val b = ticker(12, "Beta", quantity = "1", price = "1", average = "1")
            val c = ticker(13, "Gamma", quantity = "1", price = "1", average = "1")
            val dividends =
                listOf(
                    dividend(a, "100", LocalDate.of(2026, 10, 5)),
                    dividend(a, "50", LocalDate.of(2026, 10, 20)),
                    dividend(a, "70", LocalDate.of(2026, 9, 10)),
                    dividend(b, "30", LocalDate.of(2026, 10, 15)),
                    dividend(c, "10", LocalDate.of(2026, 6, 15)),
                    dividend(a, "999", LocalDate.of(2025, 10, 31)),
                )

            Then("the months run oldest first and end in the current one") {
                val panel = PortfolioAnalyticsService.buildDividendPanel(dividends, today)

                panel.months.size shouldBe 12
                panel.months.first() shouldBe java.time.YearMonth.of(2025, 12)
                panel.months.last() shouldBe java.time.YearMonth.of(2026, 11)
            }

            Then("payments of the same month are added up and a payment older than the window is left out") {
                val panel = PortfolioAnalyticsService.buildDividendPanel(dividends, today)
                val alpha = panel.series.first { it.symbol == "T11" }

                alpha.values[
                    panel.months.indexOf(
                        java.time.YearMonth.of(2026, 10),
                    ),
                ].compareTo(BigDecimal("150")) shouldBe
                    0
                alpha.values[panel.months.indexOf(java.time.YearMonth.of(2026, 9))].compareTo(BigDecimal("70")) shouldBe
                    0
                alpha.values.fold(BigDecimal.ZERO, BigDecimal::add).compareTo(BigDecimal("220")) shouldBe 0
            }

            Then("the series are ordered by what each ticker paid, and the total, average and projection follow") {
                val panel = PortfolioAnalyticsService.buildDividendPanel(dividends, today)

                panel.series.map { it.symbol } shouldContainExactly listOf("T11", "T12", "T13")
                panel.total12m.compareTo(BigDecimal("260")) shouldBe 0
                panel.monthlyAverage shouldBe BigDecimal("21.67")
                panel.projection12m.compareTo(BigDecimal("260")) shouldBe 0
            }

            Then("the tickers beyond the limit are added up in a series without symbol") {
                val panel = PortfolioAnalyticsService.buildDividendPanel(dividends, today, maxSeries = 1)

                panel.series.map { it.symbol } shouldContainExactly listOf("T11", null)
                panel.series
                    .last()
                    .values
                    .fold(BigDecimal.ZERO, BigDecimal::add)
                    .compareTo(BigDecimal("40")) shouldBe 0
                panel.total12m.compareTo(BigDecimal("260")) shouldBe 0
            }

            Then("without dividends there are no series and everything is zero") {
                val panel = PortfolioAnalyticsService.buildDividendPanel(emptyList(), today)

                panel.series shouldBe emptyList()
                panel.total12m.compareTo(BigDecimal.ZERO) shouldBe 0
                panel.projection12m.compareTo(BigDecimal.ZERO) shouldBe 0
            }
        }

        Given("allocations against their targets") {
            fun allocation(
                type: AssetType,
                current: String,
                currentPct: String,
                target: String,
            ) = AllocationDTO(
                type,
                type.name,
                BigDecimal(currentPct),
                BigDecimal(target),
                BigDecimal(current),
                BigDecimal(currentPct).subtract(BigDecimal(target)),
            )

            val stocksHeavy =
                listOf(allocation(AssetType.STOCK, "80", "80", "50"), allocation(AssetType.REIT, "20", "20", "50"))

            Then("without a contribution the excess is sold and the shortfall bought, in the same amount") {
                val rows = PortfolioAnalyticsService.rebalance(stocksHeavy, null)

                rows.map { it.action } shouldContainExactly listOf(RebalanceAction.SELL, RebalanceAction.BUY)
                rows.map { it.amount } shouldContainExactly listOf(BigDecimal("30.00"), BigDecimal("30.00"))
            }

            Then("with a contribution only purchases are suggested, covering the shortfalls") {
                val rows = PortfolioAnalyticsService.rebalance(stocksHeavy, BigDecimal("100"))

                rows.map { it.action } shouldContainExactly listOf(RebalanceAction.BUY, RebalanceAction.BUY)
                rows.map { it.amount } shouldContainExactly listOf(BigDecimal("20.00"), BigDecimal("80.00"))
            }

            Then("a small contribution all goes to the type that is short") {
                val rows = PortfolioAnalyticsService.rebalance(stocksHeavy, BigDecimal("40"))

                rows.map { it.action } shouldContainExactly listOf(RebalanceAction.NONE, RebalanceAction.BUY)
                rows.map { it.amount } shouldContainExactly listOf(BigDecimal("0.00"), BigDecimal("40.00"))
            }

            Then("a portfolio already on target needs nothing") {
                val onTarget =
                    listOf(allocation(AssetType.STOCK, "50", "50", "50"), allocation(AssetType.REIT, "50", "50", "50"))

                PortfolioAnalyticsService.rebalance(onTarget, null).map { it.action }.toSet() shouldBe
                    setOf(RebalanceAction.NONE)
            }

            Then("types without a target are left out, and with no targets there is nothing to suggest") {
                val withUntargeted = stocksHeavy + allocation(AssetType.CRYPTOCURRENCY, "500", "10", "0")

                PortfolioAnalyticsService.rebalance(withUntargeted, null).map { it.assetType } shouldContainExactly
                    listOf(AssetType.STOCK, AssetType.REIT)
                PortfolioAnalyticsService.rebalance(emptyList(), null) shouldBe emptyList()
            }

            Then("rows keep the percentages the screen shows") {
                val row = PortfolioAnalyticsService.rebalance(stocksHeavy, null).first()

                row.currentPct shouldBe BigDecimal("80")
                row.targetPct shouldBe BigDecimal("50")
                row.deviationPp.compareTo(BigDecimal("30")) shouldBe 0
            }

            Then("targets that do not add up to 100 are normalised") {
                val partial =
                    listOf(allocation(AssetType.STOCK, "80", "80", "30"), allocation(AssetType.REIT, "20", "20", "30"))

                PortfolioAnalyticsService.rebalance(partial, null).map { it.amount } shouldContainExactly
                    listOf(BigDecimal("30.00"), BigDecimal("30.00"))
            }
        }

        Given("positions with gains and losses") {
            val gain = ticker(21, "Gain", quantity = "10", price = "15", average = "10")
            val smallGain = ticker(22, "Small", quantity = "10", price = "11", average = "10")
            val loss = ticker(23, "Loss", quantity = "10", price = "8", average = "10")
            val soldOut = ticker(24, "Sold", quantity = "0", price = "1", average = "10")
            val rows =
                PortfolioAnalyticsService.buildRows(
                    listOf(gain, smallGain, loss, soldOut),
                    emptyList(),
                    emptyMap(),
                    today,
                )

            Then("the best are only the ones that gained, biggest first") {
                PortfolioAnalyticsService
                    .topPerformers(
                        rows,
                        best = true,
                        limit = 5,
                    ).map { it.name } shouldContainExactly
                    listOf("Gain", "Small")
            }

            Then("the worst are only the ones that lost, and the limit applies") {
                PortfolioAnalyticsService
                    .topPerformers(
                        rows,
                        best = false,
                        limit = 5,
                    ).map { it.name } shouldContainExactly
                    listOf("Loss")
                PortfolioAnalyticsService
                    .topPerformers(
                        rows,
                        best = true,
                        limit = 1,
                    ).map { it.name } shouldContainExactly
                    listOf("Gain")
            }

            Then("a portfolio with no losses has no worst performers") {
                PortfolioAnalyticsService.topPerformers(
                    rows.filter { it.ticker.name != "Loss" },
                    best = false,
                    limit = 5,
                ) shouldBe
                    emptyList()
            }
        }

        Given("the repositories") {
            val tickerRepository = mockk<TickerRepository>()
            val dividendRepository = mockk<DividendRepository>()
            val priceHistoryService = mockk<TickerPriceHistoryService>()
            val service =
                PortfolioAnalyticsService(
                    tickerRepository,
                    dividendRepository,
                    mockk<TickerPurchaseRepository>(),
                    mockk<TickerSaleRepository>(),
                    priceHistoryService,
                    ClockProvider(),
                )

            Then("the service loads tickers, dividends and one batch of prices") {
                every { tickerRepository.findAllByIsArchivedFalseOrderBySymbolAsc() } returns all
                every { dividendRepository.findAll() } returns emptyList()
                every { priceHistoryService.getPricePoints(listOf(1, 2, 3, 4), any(), any()) } returns emptyMap()

                val rows = service.tickerRows(AssetType.STOCK, TickerSort.WEIGHT)

                rows.map { it.ticker.id } shouldContainExactly listOf(1, 2, 4)
            }
        }
    })
