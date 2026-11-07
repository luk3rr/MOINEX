package org.moinex.service.investment

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import org.moinex.factory.investment.BondFactory
import org.moinex.factory.investment.BondOperationFactory
import org.moinex.factory.investment.TickerFactory
import org.moinex.factory.investment.TickerPurchaseFactory
import org.moinex.factory.investment.TickerSaleFactory
import org.moinex.model.dto.QuantityIssueKind
import org.moinex.model.enums.OperationType
import java.math.BigDecimal

class QuantityIntegrityServiceTest :
    BehaviorSpec({
        Given("a ticker with 11 units held and purchases of 10") {
            val ticker = TickerFactory.create(id = 1, symbol = "AAA", currentQuantity = BigDecimal("11"))
            val purchases = listOf(TickerPurchaseFactory.create(id = 1, ticker = ticker, quantity = BigDecimal("10")))

            When("checking the ticker") {
                val issues = QuantityIntegrityService.tickerIssues(listOf(ticker), purchases, emptyList())

                Then("the difference of 1 is reported") {
                    issues shouldHaveSize 1
                    issues[0].kind shouldBe QuantityIssueKind.TICKER_QUANTITY_DIFFERS
                    issues[0].difference.compareTo(BigDecimal.ONE) shouldBe 0
                    issues[0].registeredQuantity.compareTo(BigDecimal("10")) shouldBe 0
                }
            }
        }

        Given("a ticker whose held quantity equals purchases minus sales") {
            val ticker = TickerFactory.create(id = 1, symbol = "AAA", currentQuantity = BigDecimal("6"))
            val purchases = listOf(TickerPurchaseFactory.create(id = 1, ticker = ticker, quantity = BigDecimal("10")))
            val sales = listOf(TickerSaleFactory.create(id = 1, ticker = ticker, quantity = BigDecimal("4")))

            When("checking the ticker") {
                Then("nothing is reported") {
                    QuantityIntegrityService.tickerIssues(listOf(ticker), purchases, sales).shouldBeEmpty()
                }
            }
        }

        Given("a ticker with no trades registered but units held") {
            val ticker = TickerFactory.create(id = 1, symbol = "AAA", currentQuantity = BigDecimal("10"))

            When("checking the ticker") {
                val issues = QuantityIntegrityService.tickerIssues(listOf(ticker), emptyList(), emptyList())

                Then("the whole quantity is reported as unexplained") {
                    issues shouldHaveSize 1
                    issues[0].difference.compareTo(BigDecimal("10")) shouldBe 0
                }
            }
        }

        Given("a bond with only sales and another with a purchase") {
            val soldOnly = BondFactory.create(id = 1)
            val normal = BondFactory.create(id = 2)
            val operations =
                mapOf(
                    soldOnly to
                        listOf(
                            BondOperationFactory.create(
                                id = 1,
                                bond = soldOnly,
                                operationType = OperationType.SELL,
                                quantity = BigDecimal("50"),
                            ),
                        ),
                    normal to
                        listOf(
                            BondOperationFactory.create(
                                id = 2,
                                bond = normal,
                                operationType = OperationType.BUY,
                                quantity = BigDecimal("50"),
                            ),
                        ),
                )

            When("checking the bonds") {
                val issues = QuantityIntegrityService.bondIssues(operations)

                Then("only the bond without a purchase is reported") {
                    issues shouldHaveSize 1
                    issues[0].kind shouldBe QuantityIssueKind.BOND_WITHOUT_PURCHASE
                }
            }
        }
    })
