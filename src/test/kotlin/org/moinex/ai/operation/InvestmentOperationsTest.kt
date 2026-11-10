package org.moinex.ai.operation

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.json.JSONObject
import org.moinex.ai.operation.investments.ApplyToBondOperation
import org.moinex.ai.operation.investments.InvestmentContext
import org.moinex.ai.operation.investments.InvestmentPayload
import org.moinex.ai.operation.investments.RecordDividendOperation
import org.moinex.ai.operation.investments.RecordStockPurchaseOperation
import org.moinex.ai.operation.investments.RecordStockSaleOperation
import org.moinex.ai.operation.investments.RedeemBondOperation
import org.moinex.factory.CategoryFactory
import org.moinex.factory.investment.BondFactory
import org.moinex.factory.investment.TickerFactory
import org.moinex.factory.wallet.WalletFactory
import org.moinex.model.dto.BondRedemptionEstimateDTO
import org.moinex.model.dto.WalletTransactionContextDTO
import org.moinex.model.enums.BondType
import org.moinex.model.enums.OperationType
import org.moinex.model.enums.WalletTransactionStatus
import org.moinex.model.investment.BondOperation
import org.moinex.model.investment.Dividend
import org.moinex.model.investment.TickerPurchase
import org.moinex.model.investment.TickerSale
import org.moinex.service.PreferencesService
import org.moinex.service.investment.BondService
import org.moinex.service.investment.TickerService
import org.moinex.service.wallet.WalletService
import java.math.BigDecimal
import java.time.LocalDate

class InvestmentOperationsTest :
    BehaviorSpec({
        val tickerService = mockk<TickerService>()
        val bondService = mockk<BondService>()
        val walletService = mockk<WalletService>()
        val categoryResolver = mockk<CategoryResolver>()
        val preferencesService = mockk<PreferencesService>()
        every { preferencesService.translate(any()) } answers { firstArg() }

        val wallet = WalletFactory.create(id = 1, name = "Broker")
        val category = CategoryFactory.create(id = 4, name = "Investments")
        val ticker = TickerFactory.create(id = 10, symbol = "EXMP3", averageUnitValue = BigDecimal("11.00"))
        val bond = BondFactory.create(id = 20, name = "CDB Example")
        val context = InvestmentContext(LocalDate.of(2026, 10, 13), "Operation", 1, "Investments")

        every { walletService.getWalletById(1) } returns wallet
        every { categoryResolver.resolveOrCreate("Investments") } returns category
        every { tickerService.getAllNonArchivedTickers() } returns listOf(ticker)
        every { bondService.getBondById(20) } returns bond

        Given("a stock purchase") {
            val purchase = slot<TickerPurchase>()
            val walletContext = slot<WalletTransactionContextDTO>()
            every { tickerService.createTickerPurchase(capture(purchase), capture(walletContext)) } returns Unit

            When("applied") {
                val result =
                    RecordStockPurchaseOperation(tickerService, walletService, categoryResolver, preferencesService)
                        .apply(InvestmentPayload(context, 10, BigDecimal("100"), BigDecimal("12.30")))

                Then("the ticker domain service records it with a confirmed wallet transaction") {
                    result.shouldBeInstanceOf<ApplyResult.Success>()
                    purchase.captured.quantity shouldBe BigDecimal("100")
                    purchase.captured.unitPrice shouldBe BigDecimal("12.30")
                    walletContext.captured.status shouldBe WalletTransactionStatus.CONFIRMED
                    walletContext.captured.wallet shouldBe wallet
                    walletContext.captured.category shouldBe category
                }
            }
        }

        Given("a stock sale") {
            val sale = slot<TickerSale>()
            every { tickerService.createTickerSale(capture(sale), any()) } returns Unit

            When("applied") {
                RecordStockSaleOperation(tickerService, walletService, categoryResolver, preferencesService)
                    .apply(InvestmentPayload(context, 10, BigDecimal("50"), BigDecimal("13.00")))

                Then("the average cost of the ticker is used") {
                    sale.captured.averageCost shouldBe BigDecimal("11.00")
                    sale.captured.quantity shouldBe BigDecimal("50")
                }
            }
        }

        Given("a dividend") {
            val amount = slot<BigDecimal>()
            every { tickerService.createDividend(any<Dividend>(), capture(amount), any()) } returns Unit

            When("applied with an amount and no quantity") {
                val result =
                    RecordDividendOperation(tickerService, walletService, categoryResolver, preferencesService)
                        .apply(InvestmentPayload(context, 10, amount = BigDecimal("42.50")))

                Then("the total received is recorded") {
                    result.shouldBeInstanceOf<ApplyResult.Success>()
                    amount.captured shouldBe BigDecimal("42.50")
                }
            }

            When("applied without an amount") {
                val result =
                    RecordDividendOperation(tickerService, walletService, categoryResolver, preferencesService)
                        .apply(InvestmentPayload(context, 10))

                Then("it fails validation") {
                    result.shouldBeInstanceOf<ApplyResult.Failure>()
                }
            }
        }

        Given("a bond application and a redemption") {
            val operations = mutableListOf<BondOperation>()
            every { bondService.createBondOperation(capture(operations), any()) } returns Unit
            every { bondService.getAverageUnitPrice(bond) } returns BigDecimal("10.00")

            When("applying") {
                ApplyToBondOperation(bondService, walletService, categoryResolver, preferencesService)
                    .apply(InvestmentPayload(context, 20, BigDecimal("5"), BigDecimal("10.00"), fees = BigDecimal("1")))

                Then("it records a BUY with no profit") {
                    operations.last().operationType shouldBe OperationType.BUY
                    operations.last().netProfit.signum() shouldBe 0
                    operations.last().fees.compareTo(BigDecimal("1")) shouldBe 0
                }
            }

            When("redeeming above the average price") {
                RedeemBondOperation(bondService, walletService, categoryResolver, preferencesService)
                    .apply(
                        InvestmentPayload(
                            context,
                            20,
                            BigDecimal("5"),
                            BigDecimal("12.00"),
                            fees = BigDecimal("1"),
                            taxes = BigDecimal("1"),
                        ),
                    )

                Then("the net profit is (price - average) x quantity - fees - taxes") {
                    operations.last().operationType shouldBe OperationType.SELL
                    operations.last().netProfit.compareTo(BigDecimal("8")) shouldBe 0
                }
            }
        }

        Given("a CDB held in reais") {
            val cdb = BondFactory.create(id = 21, name = "CDB Inter", type = BondType.CDB)
            val operations = mutableListOf<BondOperation>()
            every { bondService.getBondById(21) } returns cdb
            every { bondService.createBondOperation(capture(operations), any()) } returns Unit
            every { bondService.getAverageUnitPrice(cdb) } returns BigDecimal.ONE
            every { bondService.estimateRedemptionByGross(cdb, BigDecimal("100"), any()) } returns
                BondRedemptionEstimateDTO(BigDecimal("10.00"), BigDecimal("1.50"), true)

            val redeem = RedeemBondOperation(bondService, walletService, categoryResolver, preferencesService)
            val apply = ApplyToBondOperation(bondService, walletService, categoryResolver, preferencesService)
            val catalog = mockk<EntityCatalog>()
            every { catalog.bond("CDB Inter") } returns cdb
            every { catalog.wallet(any()) } returns wallet
            every { catalog.category(any()) } returns category

            fun arguments(vararg extra: Pair<String, Any>) =
                JSONObject(
                    mapOf(
                        "description" to "Aplicação CDB Inter",
                        "date" to "2026-10-13",
                        "categoryName" to "Investments",
                        "assetName" to "CDB Inter",
                        "walletName" to "Broker",
                    ) + extra,
                )

            When("the model sends an amount") {
                val payload = redeem.fromModelArguments(arguments("amount" to 100), catalog)

                Then("it is kept as the quantity at a unit price of one and nothing else is missing") {
                    payload.amountOnly shouldBe true
                    payload.quantity.compareTo(BigDecimal("100")) shouldBe 0
                    payload.unitPrice.compareTo(BigDecimal.ONE) shouldBe 0
                    redeem.missingFromModel(payload) shouldBe emptyList()
                    redeem.modelRequired().contains("quantity") shouldBe false
                }

                Then("the editor shows a single amount field instead of quantity and price") {
                    val keys = redeem.draftSpecFor(payload).map { it.key }
                    keys.contains("amount") shouldBe true
                    keys.contains("quantity") shouldBe false
                    keys.contains("unitPrice") shouldBe false
                }

                Then("an edited draft is read back the same way") {
                    val back = redeem.fromDraft(redeem.toDraft(payload))!!
                    back shouldBe payload
                }
            }

            When("the card of a redemption is built") {
                val payload = redeem.fromModelArguments(arguments("amount" to 100), catalog)

                Then("the bond name is the main line and the amount and date are facts") {
                    val card = redeem.card(payload)
                    card.title shouldBe "CDB Inter"
                    card.facts.map { it.value } shouldBe
                        listOf(CardValue.Money(BigDecimal("100")), CardValue.Day(LocalDate.of(2026, 10, 13)))
                }
            }

            When("the model sends no description") {
                val payload =
                    redeem.fromModelArguments(arguments("amount" to 100, "description" to ""), catalog)

                Then("it is sent back to the model instead of being filled in by the app") {
                    redeem.missingFromModel(payload) shouldBe listOf(DESCRIPTION_MISSING)
                    redeem.modelRequired().contains("description") shouldBe true
                }
            }

            When("the model still sends quantity and price") {
                val payload = redeem.fromModelArguments(arguments("quantity" to 1, "unitPrice" to 100), catalog)

                Then("the amount is their product") {
                    payload.quantity.compareTo(BigDecimal("100")) shouldBe 0
                    payload.unitPrice.compareTo(BigDecimal.ONE) shouldBe 0
                }
            }

            When("the model sends no value at all") {
                val payload = apply.fromModelArguments(arguments(), catalog)

                Then("it is asked for the amount") {
                    apply.missingFromModel(payload) shouldBe listOf("amount must be greater than 0")
                }
            }

            When("applying an investment") {
                apply.apply(apply.fromModelArguments(arguments("amount" to 250), catalog))

                Then("a BUY of 250 units at one is recorded") {
                    operations.last().operationType shouldBe OperationType.BUY
                    operations.last().quantity.compareTo(BigDecimal("250")) shouldBe 0
                    operations.last().unitPrice.compareTo(BigDecimal.ONE) shouldBe 0
                }
            }

            When("redeeming with nothing else given") {
                redeem.apply(redeem.fromModelArguments(arguments("amount" to 100), catalog))

                Then("the whole value is the principal, with no interest, tax or profit") {
                    val saved = operations.last()
                    saved.operationType shouldBe OperationType.SELL
                    saved.quantity.compareTo(BigDecimal("100")) shouldBe 0
                    saved.unitPrice.compareTo(BigDecimal.ONE) shouldBe 0
                    saved.taxes.signum() shouldBe 0
                    saved.netProfit.signum() shouldBe 0
                    verify(exactly = 0) { bondService.estimateRedemptionByGross(any(), any(), any()) }
                }
            }

            When("the user types the interest and the taxes in the review") {
                val payload =
                    redeem
                        .fromModelArguments(arguments("amount" to 100), catalog)
                        .copy(interest = BigDecimal("10"), taxes = BigDecimal("2"))
                redeem.apply(payload)

                Then("they are split like the redemption form does") {
                    val saved = operations.last()
                    saved.quantity.compareTo(BigDecimal("90")) shouldBe 0
                    saved.taxes.compareTo(BigDecimal("2")) shouldBe 0
                    saved.netProfit.compareTo(BigDecimal("8")) shouldBe 0
                }

                Then("the card shows the principal, the yield, the taxes and what reaches the wallet") {
                    redeem
                        .preview(
                            payload,
                        )!!
                        .rows
                        .single()
                        .map { (it.value as CardValue.Money).amount.setScale(2).toPlainString() } shouldBe
                        listOf("90.00", "10.00", "2.00", "98.00")
                }
            }

            When("the user turns the automatic calculation on") {
                val payload =
                    redeem.fromModelArguments(arguments("amount" to 100), catalog).copy(autoCalculate = true)
                redeem.apply(payload)

                Then("the interest and the estimated tax come from the app") {
                    val saved = operations.last()
                    saved.quantity.compareTo(BigDecimal("90.00")) shouldBe 0
                    saved.taxes.compareTo(BigDecimal("1.50")) shouldBe 0
                    saved.netProfit.compareTo(BigDecimal("8.50")) shouldBe 0
                }

                Then("the checkbox and the interest survive an edit") {
                    redeem
                        .draftSpecFor(
                            payload,
                        ).map { it.key }
                        .containsAll(listOf("interest", "autoCalculate")) shouldBe
                        true
                    redeem.fromDraft(redeem.toDraft(payload))!! shouldBe payload
                }
            }

            When("the editor asks what to fill in while the box is on") {
                val draft =
                    redeem.toDraft(
                        redeem.fromModelArguments(arguments("amount" to 100), catalog).copy(autoCalculate = true),
                    )

                Then("the interest and the estimated tax are suggested from the amount") {
                    val suggestion = redeem.suggest(draft)
                    (suggestion["interest"] as BigDecimal).compareTo(BigDecimal("10.00")) shouldBe 0
                    (suggestion["taxes"] as BigDecimal).compareTo(BigDecimal("1.50")) shouldBe 0
                }

                Then("nothing is suggested when the box is off") {
                    redeem.suggest(DraftValues(draft.operationName, draft.fields + ("autoCalculate" to false))) shouldBe
                        emptyMap()
                }
            }

            When("the interest is not less than the amount") {
                val payload =
                    redeem.fromModelArguments(arguments("amount" to 100), catalog).copy(interest = BigDecimal("100"))

                Then("it does not validate") {
                    redeem.validate(payload).isValid shouldBe false
                }
            }
        }

        Given("an unknown ticker id") {
            When("applying a purchase") {
                val result =
                    RecordStockPurchaseOperation(tickerService, walletService, categoryResolver, preferencesService)
                        .apply(InvestmentPayload(context, 999, BigDecimal("1"), BigDecimal("1")))

                Then("it fails without writing") {
                    result.shouldBeInstanceOf<ApplyResult.Failure>()
                    verify(exactly = 0) { tickerService.createTickerPurchase(match { it.ticker.id == 999 }, any()) }
                }
            }
        }
    })
