package org.moinex.ai.tools

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.json.JSONObject
import org.moinex.factory.CategoryFactory
import org.moinex.factory.investment.BondFactory
import org.moinex.factory.investment.TickerFactory
import org.moinex.factory.wallet.WalletFactory
import org.moinex.model.dto.ai.InvestmentOperationKind
import org.moinex.service.CategoryService
import org.moinex.service.PreferencesService
import org.moinex.service.investment.BondService
import org.moinex.service.investment.TickerService
import org.moinex.service.wallet.WalletService

class ProposeInvestmentOperationToolTest :
    BehaviorSpec({
        val categoryService = mockk<CategoryService>()
        val walletService = mockk<WalletService>()
        val tickerService = mockk<TickerService>()
        val bondService = mockk<BondService>()
        val preferencesService = mockk<PreferencesService>(relaxed = true)

        val investments = CategoryFactory.create(id = 1, name = "Investimentos")
        val nubank = WalletFactory.create(id = 10, name = "Nubank")
        val petr4 =
            TickerFactory.create(
                id = 20,
                symbol = "PETR4",
                name = "Petrobras",
                averageUnitValue = "30.00".toBigDecimal(),
            )
        val cdb = BondFactory.create(id = 30, name = "CDB Banco Inter", symbol = "CDBINTER")

        every { categoryService.getNonArchivedCategoriesOrderedByName() } returns listOf(investments)
        every { walletService.getAllNonArchivedWalletsOrderedByName() } returns listOf(nubank)
        every { tickerService.getAllNonArchivedTickers() } returns listOf(petr4)
        every { bondService.getAllNonArchivedBonds() } returns listOf(cdb)

        val tool =
            ProposeInvestmentOperationTool(
                categoryService,
                walletService,
                tickerService,
                bondService,
                preferencesService,
            )

        Given("a stock purchase whose ticker symbol matches an existing one") {
            val arguments =
                """
                {"operations":[
                    {"date":"2026-09-05","kind":"stock_purchase","assetName":"petr4","description":"Compra PETR4",
                     "quantity":10,"unitPrice":30.50,"walletName":"nubank","categoryName":"investimentos",
                     "confidence":0.9}
                ]}
                """.trimIndent()

            When("executing the tool") {
                val result = ProposeInvestmentOperationTool.parseResultJson(tool.execute(arguments))

                Then("it resolves the ticker, wallet and category ids case-insensitively") {
                    result.size shouldBe 1
                    result[0].suggestedTickerId shouldBe petr4.id
                    result[0].suggestedBondId shouldBe null
                    result[0].suggestedWalletId shouldBe nubank.id
                    result[0].suggestedCategoryId shouldBe investments.id
                    result[0].kind shouldBe InvestmentOperationKind.STOCK_PURCHASE
                }
            }
        }

        Given("a ticker stored with a yfinance exchange suffix (e.g. .SA for B3-listed stocks)") {
            val vale3 =
                TickerFactory.create(
                    id = 21,
                    symbol = "VALE3.SA",
                    name = "Vale",
                    averageUnitValue = "60.00".toBigDecimal(),
                )
            every { tickerService.getAllNonArchivedTickers() } returns listOf(petr4, vale3)

            val arguments =
                """
                {"operations":[
                    {"date":"2026-09-05","kind":"stock_purchase","assetName":"VALE3","description":"Compra VALE3",
                     "quantity":10,"unitPrice":60.00,"walletName":"nubank","categoryName":"investimentos"}
                ]}
                """.trimIndent()

            When("executing the tool with assetName reported without the suffix, as printed on a statement") {
                val result = ProposeInvestmentOperationTool.parseResultJson(tool.execute(arguments))

                Then("it still matches the ticker by its base symbol") {
                    result[0].suggestedTickerId shouldBe vale3.id
                }
            }
        }

        Given("a stock purchase whose asset name doesn't match any existing ticker") {
            val arguments =
                """
                {"operations":[
                    {"date":"2026-09-05","kind":"stock_purchase","assetName":"UNKNOWN4","description":"Compra",
                     "quantity":5,"unitPrice":10.00,"walletName":"nubank","categoryName":"investimentos"}
                ]}
                """.trimIndent()

            When("executing the tool") {
                val result = ProposeInvestmentOperationTool.parseResultJson(tool.execute(arguments))

                Then("suggestedTickerId is null and the original name is kept as the label") {
                    result[0].suggestedTickerId shouldBe null
                    result[0].suggestedAssetLabel shouldBe "UNKNOWN4"
                }
            }
        }

        Given("a CDB application whose asset name matches an existing bond") {
            val arguments =
                """
                {"operations":[
                    {"date":"2026-09-05","kind":"bond_application","assetName":"CDB Banco Inter","description":"Aplicação",
                     "quantity":1,"unitPrice":1000.00,"fees":5.00,"taxes":0,"walletName":"nubank",
                     "categoryName":"investimentos"}
                ]}
                """.trimIndent()

            When("executing the tool") {
                val result = ProposeInvestmentOperationTool.parseResultJson(tool.execute(arguments))

                Then("it resolves against bonds, never tickers, even if a ticker symbol coincidentally matches") {
                    result[0].suggestedBondId shouldBe cdb.id
                    result[0].suggestedTickerId shouldBe null
                    result[0].fees.compareTo("5.00".toBigDecimal()) shouldBe 0
                }
            }
        }

        Given("a dividend candidate") {
            val arguments =
                """
                {"operations":[
                    {"date":"2026-09-05","kind":"dividend","assetName":"PETR4","description":"Dividendo",
                     "dividendAmount":42.50,"walletName":"nubank","categoryName":"investimentos"}
                ]}
                """.trimIndent()

            When("executing the tool") {
                val result = ProposeInvestmentOperationTool.parseResultJson(tool.execute(arguments))

                Then("dividendAmount is preserved and quantity/unitPrice default to zero") {
                    result[0].kind shouldBe InvestmentOperationKind.DIVIDEND
                    result[0].dividendAmount.compareTo("42.50".toBigDecimal()) shouldBe 0
                    result[0].quantity.compareTo(java.math.BigDecimal.ZERO) shouldBe 0
                    result[0].unitPrice.compareTo(java.math.BigDecimal.ZERO) shouldBe 0
                }
            }
        }

        Given("a candidate with a category name that doesn't exist yet") {
            val arguments =
                """
                {"operations":[
                    {"date":"2026-09-05","kind":"stock_sale","assetName":"PETR4","description":"Venda",
                     "quantity":2,"unitPrice":35.00,"walletName":"nubank","categoryName":"Nova Categoria"}
                ]}
                """.trimIndent()

            When("executing the tool") {
                val result = ProposeInvestmentOperationTool.parseResultJson(tool.execute(arguments))

                Then("suggestedCategoryId is null and the proposed name is preserved") {
                    result[0].suggestedCategoryId shouldBe null
                    result[0].suggestedCategoryName shouldBe "Nova Categoria"
                }
            }
        }

        Given("a candidate missing a required field") {
            val arguments =
                """
                {"operations":[
                    {"kind":"dividend","description":"Missing date","dividendAmount":10,"categoryName":"Investimentos"}
                ]}
                """.trimIndent()

            When("executing the tool") {
                val result = ProposeInvestmentOperationTool.parseResultJson(tool.execute(arguments))

                Then("the malformed entry is skipped instead of failing the whole batch") {
                    result.size shouldBe 0
                }
            }
        }

        Given("the tool's JSON schema sent to the provider") {
            When("inspecting the required fields for an operation candidate") {
                val schema = JSONObject(tool.jsonSchema)
                val requiredFields =
                    schema
                        .getJSONObject("properties")
                        .getJSONObject("operations")
                        .getJSONObject("items")
                        .getJSONArray("required")
                        .let { array -> (0 until array.length()).map { array.getString(it) } }

                Then("quantity and unitPrice are required, so the model can't omit them for a CDB/stock operation") {
                    requiredFields shouldContain "quantity"
                    requiredFields shouldContain "unitPrice"
                }
            }
        }

        Given("the tool is asked to describe the available context") {
            When("calling describeAvailableContext") {
                val context = tool.describeAvailableContext()

                Then("it lists the existing tickers and bonds, never wallets/categories (covered by the other tool)") {
                    context.contains("PETR4") shouldBe true
                    context.contains("CDB Banco Inter") shouldBe true
                }
            }
        }

        Given("any candidate list, valid or not") {
            val arguments =
                """
                {"operations":[
                    {"date":"2026-09-05","kind":"dividend","assetName":"PETR4","description":"Dividendo",
                     "dividendAmount":1,"walletName":"nubank","categoryName":"Investimentos"}
                ]}
                """.trimIndent()

            When("executing the tool") {
                tool.execute(arguments)

                Then("it never calls a write method on any service") {
                    verify(exactly = 0) { categoryService.createCategory(any()) }
                    verify(exactly = 0) { tickerService.createTickerPurchase(any(), any()) }
                    verify(exactly = 0) { tickerService.createTickerSale(any(), any()) }
                    verify(exactly = 0) { tickerService.createDividend(any(), any(), any()) }
                    verify(exactly = 0) { bondService.createBondOperation(any(), any()) }
                }
            }
        }
    })
