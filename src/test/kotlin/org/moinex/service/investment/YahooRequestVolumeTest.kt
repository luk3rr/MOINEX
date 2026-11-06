/*
 * Filename: YahooRequestVolumeTest.kt
 * Created on: November 6, 2026
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 */

package org.moinex.service.investment

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.mockk.clearAllMocks
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkAll
import org.json.JSONArray
import org.json.JSONObject
import org.moinex.common.ClockProvider
import org.moinex.common.util.APIUtils
import org.moinex.factory.investment.MarketQuotesAndCommoditiesFactory
import org.moinex.factory.investment.TickerFactory
import org.moinex.model.dto.StockHistoryRequest
import org.moinex.model.dto.TickerMetadata
import org.moinex.model.investment.Ticker
import org.moinex.model.investment.TickerPriceHistory
import org.moinex.repository.investment.BrazilianMarketIndicatorsRepository
import org.moinex.repository.investment.CryptoExchangeRepository
import org.moinex.repository.investment.DividendRepository
import org.moinex.repository.investment.MarketQuotesAndCommoditiesRepository
import org.moinex.repository.investment.TickerPriceHistoryRepository
import org.moinex.repository.investment.TickerPurchaseRepository
import org.moinex.repository.investment.TickerRepository
import org.moinex.repository.investment.TickerSaleRepository
import org.moinex.service.NotificationService
import org.moinex.service.PreferencesService
import org.moinex.service.wallet.WalletService
import java.time.LocalDateTime
import kotlin.math.ceil

/**
 * NFR-1: how many requests the startup refresh sends to Yahoo. A script run costs one download per
 * [DOWNLOAD_CHUNK] symbols, plus one `info` lookup for every symbol whose currency it was not given.
 */
class YahooRequestVolumeTest :
    BehaviorSpec({
        val downloadChunk = 8

        val tickerRepository = mockk<TickerRepository>()
        val priceHistoryRepository = mockk<TickerPriceHistoryRepository>()
        val quotesRepository = mockk<MarketQuotesAndCommoditiesRepository>()
        val indicatorsRepository = mockk<BrazilianMarketIndicatorsRepository>()
        val clockProvider = ClockProvider()

        val marketData = TickerMarketDataService(tickerRepository)
        val tickerService =
            TickerService(
                tickerRepository,
                mockk<TickerPurchaseRepository>(),
                mockk<TickerSaleRepository>(),
                mockk<DividendRepository>(),
                mockk<CryptoExchangeRepository>(),
                mockk<WalletService>(),
                mockk<NotificationService>(relaxed = true),
                mockk<PreferencesService>(relaxed = true),
                clockProvider,
                marketData,
            )
        val historyService =
            TickerPriceHistoryService(
                priceHistoryRepository,
                tickerRepository,
                mockk<TickerPurchaseRepository>(),
                mockk<TickerSaleRepository>(),
                clockProvider,
                marketData,
            )
        val marketService = MarketService(indicatorsRepository, quotesRepository, clockProvider)

        var requests = 0

        fun requestsFor(
            symbols: Int,
            withoutCurrency: Int,
        ) = ceil(symbols / downloadChunk.toDouble()).toInt() + withoutCurrency

        beforeSpec { mockkObject(APIUtils) }
        afterSpec { unmockkAll() }
        afterContainer { clearAllMocks(answers = true) }

        fun portfolio(
            size: Int,
            updatedAt: LocalDateTime,
        ): List<Ticker> =
            (1..size).map { index ->
                TickerFactory.create(id = index, symbol = "T$index.SA", lastUpdate = updatedAt).also {
                    it.metadata =
                        TickerMetadata(currency = "BRL", historyUpdatedAt = updatedAt).toJson()
                }
            }

        fun stubApi() {
            requests = 0
            every { tickerRepository.save(any<Ticker>()) } answers { firstArg() }
            every { priceHistoryRepository.findLatestPriceByTicker(any()) } returns null
            every { priceHistoryRepository.findByTickerIdAndDateRange(any(), any(), any()) } returns emptyList()
            every { priceHistoryRepository.saveAll(any<Iterable<TickerPriceHistory>>()) } answers { firstArg() }
            every { quotesRepository.save(any()) } answers { firstArg() }
            coEvery { APIUtils.fetchStockLogos(any()) } returns JSONObject()

            coEvery { APIUtils.fetchStockPrices(any()) } answers {
                val arguments = firstArg<List<String>>()
                requests += requestsFor(arguments.size, arguments.count { ":" !in it })
                JSONObject().also { json ->
                    arguments.forEach {
                        json.put(
                            it.substringBefore(":"),
                            JSONObject().put("price", 10.0).put("currency", "BRL"),
                        )
                    }
                }
            }
            coEvery { APIUtils.fetchStockPriceHistoryBatch(any()) } answers {
                val batch = firstArg<List<StockHistoryRequest>>()
                requests += requestsFor(batch.size, batch.count { it.currency == null })
                JSONObject().also { json ->
                    batch.forEach {
                        json.put(it.symbol, JSONObject().put("currency", "BRL").put("prices", JSONArray()))
                    }
                }
            }
        }

        Given("a portfolio of 30 tickers whose data is three hours old, and stale market quotes") {
            val tickers = portfolio(30, LocalDateTime.now().minusHours(3))

            When("the startup refresh runs") {
                stubApi()
                every { tickerRepository.findAllByIsArchivedFalseOrderBySymbolAsc() } returns tickers
                every { quotesRepository.findAll() } returns
                    listOf(
                        MarketQuotesAndCommoditiesFactory.create(
                            id = 1,
                            lastUpdate = LocalDateTime.now().minusHours(3),
                        ),
                    )

                runCatching {
                    tickerService.updateAllNonArchivedTickersPrices()
                    historyService.syncDailyPriceHistory()
                    marketService.updateMarketQuotesAndCommoditiesIfStale()
                }

                Then("it sends at most 15 requests to Yahoo, against about 100 to 150 before") {
                    requests shouldNotBe 0
                    (requests <= 15) shouldBe true
                }
            }
        }

        Given("the same portfolio updated ten minutes ago") {
            val tickers = portfolio(30, LocalDateTime.now().minusMinutes(10))

            When("the startup refresh runs") {
                stubApi()
                every { tickerRepository.findAllByIsArchivedFalseOrderBySymbolAsc() } returns tickers
                every { quotesRepository.findAll() } returns
                    listOf(
                        MarketQuotesAndCommoditiesFactory.create(
                            id = 1,
                            lastUpdate = LocalDateTime.now().minusMinutes(10),
                        ),
                    )

                tickerService.updateAllNonArchivedTickersPrices()
                historyService.syncDailyPriceHistory()
                marketService.updateMarketQuotesAndCommoditiesIfStale()

                Then("it sends no request at all") {
                    requests shouldBe 0
                }
            }
        }

        Given("30 tickers whose currency was never stored") {
            val tickers = portfolio(30, LocalDateTime.now().minusHours(3))
            tickers.forEach { it.metadata = null }

            When("the first refresh runs") {
                stubApi()
                every { tickerRepository.findAllByIsArchivedFalseOrderBySymbolAsc() } returns tickers

                tickerService.updateAllNonArchivedTickersPrices()
                val afterFirst = requests

                Then("it pays one info lookup per ticker once, and learns the currency of every one") {
                    afterFirst shouldBe requestsFor(30, 30)
                    tickers.all { TickerMetadata.fromJson(it.metadata).currency == "BRL" } shouldBe true
                }

                Then("the next refresh, once the data is stale again, needs no lookup") {
                    requests = 0
                    tickers.forEach { it.lastUpdate = LocalDateTime.now().minusHours(3) }
                    tickerService.updateAllNonArchivedTickersPrices()
                    requests shouldBe requestsFor(30, 0)
                }
            }
        }
    })
