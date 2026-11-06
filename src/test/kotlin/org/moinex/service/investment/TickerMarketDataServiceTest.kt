/*
 * Filename: TickerMarketDataServiceTest.kt
 * Created on: November 6, 2026
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 */

package org.moinex.service.investment

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.moinex.factory.investment.TickerFactory
import org.moinex.model.dto.TickerMetadata
import org.moinex.model.investment.Ticker
import org.moinex.repository.investment.TickerRepository
import java.math.BigDecimal
import java.time.LocalDateTime

class TickerMarketDataServiceTest :
    BehaviorSpec({
        val tickerRepository = mockk<TickerRepository>()
        val service = TickerMarketDataService(tickerRepository)

        val oldMoment = LocalDateTime.of(2026, 11, 1, 9, 0)
        val newMoment = LocalDateTime.of(2026, 11, 6, 10, 30)

        beforeSpec {
            every { tickerRepository.save(any<Ticker>()) } answers { firstArg() }
        }

        afterContainer {
            clearAllMocks(answers = false)
        }

        Given("a ticker without metadata") {
            val ticker =
                TickerFactory.create(
                    currentUnitValue = BigDecimal("10.00"),
                    domain = "https://old.example",
                    lastUpdate = oldMoment,
                )

            When("it is read") {
                val metadata = service.read(ticker)

                Then("nothing is set") {
                    metadata.currency shouldBe null
                    metadata.historyUpdatedAt shouldBe null
                }
            }

            When("a price is recorded with a currency") {
                service.record(ticker, BigDecimal("12.50"), newMoment, currency = "BRL")

                Then("the price and its moment go to the columns and the currency to the metadata") {
                    ticker.currentUnitValue.compareTo(BigDecimal("12.50")) shouldBe 0
                    ticker.lastUpdate shouldBe newMoment
                    ticker.domain shouldBe "https://old.example"
                    TickerMetadata.fromJson(ticker.metadata).currency shouldBe "BRL"
                    verify(exactly = 1) { tickerRepository.save(ticker) }
                }

                Then("the metadata does not repeat the price") {
                    (ticker.metadata?.contains("price") ?: false) shouldBe false
                }
            }
        }

        Given("a ticker whose metadata already knows the currency") {
            val ticker = TickerFactory.create(lastUpdate = oldMoment)
            service.record(ticker, BigDecimal("10.00"), oldMoment, currency = "USD", website = "https://a.example")

            When("a later update brings only the price") {
                service.record(ticker, BigDecimal("11.00"), newMoment)

                Then("the currency and the site are kept") {
                    TickerMetadata.fromJson(ticker.metadata).currency shouldBe "USD"
                    ticker.domain shouldBe "https://a.example"
                    ticker.lastUpdate shouldBe newMoment
                }
            }
        }

        Given("an invalid price") {
            val ticker = TickerFactory.create(currentUnitValue = BigDecimal("10.00"), lastUpdate = oldMoment)

            When("a negative price is recorded") {
                Then("it is rejected and nothing is saved") {
                    shouldThrow<IllegalArgumentException> {
                        service.record(ticker, BigDecimal("-1"), newMoment)
                    }
                    ticker.currentUnitValue.compareTo(BigDecimal("10.00")) shouldBe 0
                    ticker.lastUpdate shouldBe oldMoment
                    ticker.metadata shouldBe null
                    verify(exactly = 0) { tickerRepository.save(any<Ticker>()) }
                }
            }
        }

        Given("a ticker with a price record") {
            val ticker = TickerFactory.create(lastUpdate = oldMoment, currentUnitValue = BigDecimal("10.00"))
            service.record(ticker, BigDecimal("12.00"), oldMoment, currency = "USD")

            When("the history sync is recorded with a newly reported currency") {
                service.recordHistorySync(ticker, newMoment, currency = "BRL")

                Then("only the history moment and the currency change; price and last update stay") {
                    val stored = TickerMetadata.fromJson(ticker.metadata)
                    stored.historyUpdatedAt shouldBe newMoment
                    stored.currency shouldBe "BRL"
                    ticker.lastUpdate shouldBe oldMoment
                    ticker.currentUnitValue.compareTo(BigDecimal("12.00")) shouldBe 0
                }
            }

            When("the history sync is recorded without a currency") {
                service.recordHistorySync(ticker, newMoment.plusMinutes(5))

                Then("the known currency is kept") {
                    TickerMetadata.fromJson(ticker.metadata).currency shouldBe "BRL"
                }
            }
        }
    })
