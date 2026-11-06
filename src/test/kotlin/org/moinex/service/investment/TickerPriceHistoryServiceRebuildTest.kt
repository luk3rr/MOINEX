package org.moinex.service.investment

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.clearAllMocks
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.slot
import io.mockk.unmockkAll
import io.mockk.verify
import io.mockk.verifyOrder
import org.json.JSONArray
import org.json.JSONObject
import org.moinex.common.ClockProvider
import org.moinex.common.util.APIUtils
import org.moinex.factory.investment.TickerFactory
import org.moinex.factory.investment.TickerPriceHistoryFactory
import org.moinex.model.investment.Ticker
import org.moinex.model.investment.TickerPriceHistory
import org.moinex.repository.investment.TickerPriceHistoryRepository
import org.moinex.repository.investment.TickerPurchaseRepository
import org.moinex.repository.investment.TickerRepository
import org.moinex.repository.investment.TickerSaleRepository
import java.math.BigDecimal

class TickerPriceHistoryServiceRebuildTest :
    BehaviorSpec({
        val priceHistoryRepository = mockk<TickerPriceHistoryRepository>()
        val purchaseRepository = mockk<TickerPurchaseRepository>()
        val saleRepository = mockk<TickerSaleRepository>()
        val clockProvider = ClockProvider()
        val service =
            TickerPriceHistoryService(
                priceHistoryRepository,
                mockk<TickerRepository>(),
                purchaseRepository,
                saleRepository,
                clockProvider,
                TickerMarketDataService(
                    mockk<TickerRepository> {
                        every { save(any<Ticker>()) } answers { firstArg() }
                    },
                ),
            )

        val today = clockProvider.today()
        val ticker = TickerFactory.create(id = 1, symbol = "PETR4.SA", currentQuantity = BigDecimal.ZERO)
        val stored =
            listOf(
                TickerPriceHistoryFactory.create(
                    ticker = ticker,
                    priceDate = today.minusYears(2),
                    closingPrice = BigDecimal("7.50"),
                    isMonthEnd = true,
                ),
                TickerPriceHistoryFactory.create(ticker = ticker, priceDate = today.minusDays(3)),
            )

        beforeSpec { mockkObject(APIUtils) }
        afterSpec { unmockkAll() }
        afterContainer { clearAllMocks(answers = true) }

        fun prepareStoredHistory() {
            every { priceHistoryRepository.findAllByTickerId(1) } returns stored
            every { priceHistoryRepository.deleteAllByTickerId(1) } returns Unit
            every { purchaseRepository.findAllByTickerId(1) } returns emptyList()
            every { saleRepository.findAllByTickerId(1) } returns emptyList()
            every { priceHistoryRepository.findLatestPriceByTicker(1) } returns null
            every { priceHistoryRepository.findByTickerIdAndDateRange(1, any(), any()) } returns emptyList()
        }

        Given("a ticker with stored price history") {
            When("the API answers while rebuilding") {
                prepareStoredHistory()
                coEvery { APIUtils.fetchStockPriceHistory("PETR4.SA", any(), any(), any()) } returns
                    JSONObject().put(
                        "prices",
                        JSONArray().put(
                            JSONObject().put("date", today.toString()).put("price", 12.0).put("is_month_end", false),
                        ),
                    )
                every { priceHistoryRepository.saveAll(any<Iterable<TickerPriceHistory>>()) } answers
                    { firstArg<Iterable<TickerPriceHistory>>().toList() }

                kotlinx.coroutines.runBlocking { service.rebuildPriceHistory(ticker) }

                Then("it deletes the stored rows before saving the fetched ones, once") {
                    verifyOrder {
                        priceHistoryRepository.deleteAllByTickerId(1)
                        priceHistoryRepository.saveAll(any<Iterable<TickerPriceHistory>>())
                    }
                    verify(exactly = 1) { priceHistoryRepository.deleteAllByTickerId(1) }
                }
            }

            When("the API fails while rebuilding") {
                prepareStoredHistory()
                coEvery { APIUtils.fetchStockPriceHistory("PETR4.SA", any(), any(), any()) } returns
                    JSONObject().put("error", "boom")
                val restored = slot<Iterable<TickerPriceHistory>>()
                every { priceHistoryRepository.saveAll(capture(restored)) } answers { restored.captured.toList() }

                shouldThrow<Exception> { kotlinx.coroutines.runBlocking { service.rebuildPriceHistory(ticker) } }

                Then("the previous rows are put back and the error is rethrown") {
                    verify(exactly = 2) { priceHistoryRepository.deleteAllByTickerId(1) }
                    restored.captured.map { it.priceDate to it.closingPrice.toPlainString() to it.isMonthEnd } shouldBe
                        stored.map { it.priceDate to it.closingPrice.toPlainString() to it.isMonthEnd }
                }
            }
        }
    })
