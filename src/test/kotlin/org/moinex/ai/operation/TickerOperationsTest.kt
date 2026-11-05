/*
 * Filename: TickerOperationsTest.kt
 * Created on: November 5, 2026
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 */

package org.moinex.ai.operation

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.json.JSONObject
import org.moinex.ai.operation.investments.CreateTickerOperation
import org.moinex.ai.operation.investments.RefreshTickerDataOperation
import org.moinex.ai.operation.investments.UpdateTickerOperation
import org.moinex.factory.investment.TickerFactory
import org.moinex.model.enums.AssetType
import org.moinex.model.investment.Ticker
import org.moinex.service.investment.TickerPriceHistoryService
import org.moinex.service.investment.TickerService

class TickerOperationsTest :
    BehaviorSpec({
        val tickerService = mockk<TickerService>()
        val history = mockk<TickerPriceHistoryService>()
        val petr = TickerFactory.create(id = 30, symbol = "PETR4.SA", name = "Petrobras")
        val catalog = EntityCatalog(tickers = listOf(petr))

        every { tickerService.getAllTickers() } returns listOf(petr)
        every { tickerService.getAllNonArchivedTickers() } returns listOf(petr)

        Given("a request to register a new asset") {
            val operation = CreateTickerOperation(tickerService)
            val payload =
                operation.fromModelArguments(
                    JSONObject("""{"symbol":"VALE3.SA","name":"Vale","assetType":"stock","amount":60.5}"""),
                    catalog,
                )

            Then("it is created with no quantity and the given price") {
                operation.validate(payload).isValid shouldBe true
                val saved = slot<Ticker>()
                every { tickerService.createTicker(capture(saved)) } returns 31
                operation.apply(payload).shouldBeInstanceOf<ApplyResult.Success>()
                saved.captured.currentQuantity.signum() shouldBe 0
                saved.captured.type shouldBe AssetType.STOCK
            }

            Then("an unknown type, a bond type or a symbol already stored are refused") {
                operation.validate(payload.copy(assetType = null)).isValid shouldBe false
                val duplicate =
                    operation.fromModelArguments(
                        JSONObject("""{"symbol":"petr4.sa","name":"x","assetType":"stock"}"""),
                        catalog,
                    )
                operation.validate(duplicate).isValid shouldBe false
                operation
                    .fromModelArguments(JSONObject("""{"symbol":"A","name":"x","assetType":"bond"}"""), catalog)
                    .assetType shouldBe null
            }
        }

        Given("a request that renames an asset") {
            val operation = UpdateTickerOperation(tickerService)
            val payload =
                operation.fromModelArguments(JSONObject("""{"assetName":"PETR4","name":"Petrobras PN"}"""), catalog)

            Then("the symbol and type are kept and the quantity and prices are untouched on save") {
                payload.symbol shouldBe "PETR4.SA"
                val saved = slot<Ticker>()
                every { tickerService.updateTicker(capture(saved)) } returns Unit
                operation.apply(payload).shouldBeInstanceOf<ApplyResult.Success>()
                saved.captured.name shouldBe "Petrobras PN"
                saved.captured.currentQuantity shouldBe petr.currentQuantity
                saved.captured.averageUnitValue shouldBe petr.averageUnitValue
            }
        }

        Given("a request to refresh one asset") {
            val operation = RefreshTickerDataOperation(tickerService, history)
            coEvery { tickerService.updateTickersPriceFromApi(any()) } returns emptyList()
            coEvery { history.syncDaily(any()) } returns 0
            coEvery { history.rebuildPriceHistory(any()) } returns Unit

            Then("a plain refresh syncs the daily history and a rebuild throws it away first") {
                operation.apply(operation.fromModelArguments(JSONObject("""{"assetName":"PETR4"}"""), catalog))
                coVerify(exactly = 1) { history.syncDaily(petr) }
                operation.apply(
                    operation.fromModelArguments(JSONObject("""{"assetName":"PETR4","rebuild":true}"""), catalog),
                )
                coVerify(exactly = 1) { history.rebuildPriceHistory(petr) }
            }

            Then("an unknown asset is refused and a failing price fetch is reported without touching the history") {
                operation
                    .validate(operation.fromModelArguments(JSONObject("""{"assetName":"NOPE"}"""), catalog))
                    .isValid shouldBe false

                coEvery { tickerService.updateTickersPriceFromApi(any()) } returns listOf(petr)
                val failed =
                    operation.apply(operation.fromModelArguments(JSONObject("""{"assetName":"PETR4"}"""), catalog))
                failed.shouldBeInstanceOf<ApplyResult.Failure>()
                verify(exactly = 0) { tickerService.createTicker(match { it.symbol == "x" }) }
                coVerify(exactly = 1) { history.syncDaily(petr) }
            }
        }
    })
