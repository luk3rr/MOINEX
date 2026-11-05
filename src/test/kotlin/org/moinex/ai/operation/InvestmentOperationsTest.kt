package org.moinex.ai.operation

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
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
import org.moinex.model.dto.WalletTransactionContextDTO
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
