package org.moinex.ai.tools

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import org.json.JSONObject
import org.moinex.factory.CategoryFactory
import org.moinex.factory.NetWorthSnapshotFactory
import org.moinex.factory.creditcard.CreditCardDebtFactory
import org.moinex.factory.creditcard.CreditCardFactory
import org.moinex.factory.wallet.WalletTransactionFactory
import org.moinex.model.enums.WalletTransactionType
import org.moinex.service.PreferencesService
import org.moinex.service.creditcard.CreditCardService
import org.moinex.service.networth.NetWorthService
import org.moinex.service.wallet.WalletService
import java.math.BigDecimal
import java.time.YearMonth

class QueryFinanceToolTest :
    BehaviorSpec({
        val walletService = mockk<WalletService>()
        val creditCardService = mockk<CreditCardService>()
        val netWorthService = mockk<NetWorthService>()
        val preferencesService = mockk<PreferencesService>(relaxed = true)

        val queryFinanceTool = QueryFinanceTool(walletService, creditCardService, netWorthService, preferencesService)

        afterContainer { clearAllMocks() }

        Given("availableTools") {
            When("listing the exposed tools") {
                val tools = queryFinanceTool.availableTools()

                Then("it exposes the read-only finance query tools") {
                    tools.map { it.name }.toSet() shouldBe
                        setOf(
                            "get_net_worth",
                            "get_spending_by_category",
                            "get_transactions_in_period",
                            "get_credit_card_invoice_payment_status",
                        )
                }
            }
        }

        Given("a month with a saved net worth snapshot") {
            val month = YearMonth.of(2026, 9)
            every { netWorthService.getSnapshot(month) } returns
                NetWorthSnapshotFactory.create(referenceMonth = month, netWorth = BigDecimal("3500.00"))

            When("calling get_net_worth for that month") {
                val tool = queryFinanceTool.availableTools().first { it.name == "get_net_worth" }
                val result = JSONObject(tool.execute("""{"month":"2026-09"}"""))

                Then("it returns the stored net worth") {
                    result.getBoolean("available") shouldBe true
                    result.getBigDecimal("netWorth").compareTo(BigDecimal("3500.00")) shouldBe 0
                }
            }
        }

        Given("a month with no saved net worth snapshot") {
            val month = YearMonth.of(2026, 8)
            every { netWorthService.getSnapshot(month) } returns null

            When("calling get_net_worth for that month") {
                val tool = queryFinanceTool.availableTools().first { it.name == "get_net_worth" }
                val result = JSONObject(tool.execute("""{"month":"2026-08"}"""))

                Then("it reports the data as unavailable instead of failing") {
                    result.getBoolean("available") shouldBe false
                }
            }
        }

        Given("wallet expenses and a credit card purchase in the same category, in the same month") {
            val month = YearMonth.of(2026, 9)
            val foodCategory = CategoryFactory.create(id = 1, name = "Alimentação")

            every { walletService.getAllNonArchivedConfirmedWalletTransactionsByMonthForAnalysis(month) } returns
                listOf(
                    WalletTransactionFactory.create(
                        type = WalletTransactionType.EXPENSE,
                        category = foodCategory,
                        amount = BigDecimal("50.00"),
                    ),
                )
            every { creditCardService.getDebtsBetweenForAnalysis(any(), any()) } returns
                listOf(CreditCardDebtFactory.create(category = foodCategory, amount = BigDecimal("30.00")))

            When("calling get_spending_by_category for that month") {
                val tool = queryFinanceTool.availableTools().first { it.name == "get_spending_by_category" }
                val result = JSONObject(tool.execute("""{"month":"2026-09"}"""))

                Then("wallet expenses and credit card debts for the category are summed together") {
                    result
                        .getJSONObject(
                            "byCategory",
                        ).getBigDecimal("Alimentação")
                        .compareTo(BigDecimal("80.00")) shouldBe
                        0
                    result.getBigDecimal("total").compareTo(BigDecimal("80.00")) shouldBe 0
                }
            }
        }

        Given("wallet income in the month (not an expense)") {
            val month = YearMonth.of(2026, 9)
            val salaryCategory = CategoryFactory.create(id = 2, name = "Salário")

            every { walletService.getAllNonArchivedConfirmedWalletTransactionsByMonthForAnalysis(month) } returns
                listOf(
                    WalletTransactionFactory.create(
                        type = WalletTransactionType.INCOME,
                        category = salaryCategory,
                        amount = BigDecimal("5000.00"),
                    ),
                )
            every { creditCardService.getDebtsBetweenForAnalysis(any(), any()) } returns emptyList()

            When("calling get_spending_by_category for that month") {
                val tool = queryFinanceTool.availableTools().first { it.name == "get_spending_by_category" }
                val result = JSONObject(tool.execute("""{"month":"2026-09"}"""))

                Then("income is excluded from the spending breakdown") {
                    result.getJSONObject("byCategory").keySet().contains("Salário") shouldBe false
                    result.getBigDecimal("total") shouldBe BigDecimal.ZERO
                }
            }
        }

        Given("existing wallet transactions and credit card debts within a date range") {
            val foodCategory = CategoryFactory.create(id = 1, name = "Alimentação")

            every { walletService.getAllNonArchivedWalletTransactionsBetweenDates(any(), any()) } returns
                listOf(
                    WalletTransactionFactory.create(
                        type = WalletTransactionType.EXPENSE,
                        category = foodCategory,
                        amount = BigDecimal("42.00"),
                    ),
                )
            every { creditCardService.getDebtsBetweenForAnalysis(any(), any()) } returns
                listOf(CreditCardDebtFactory.create(category = foodCategory, amount = BigDecimal("99.00")))

            When("calling get_transactions_in_period") {
                val tool = queryFinanceTool.availableTools().first { it.name == "get_transactions_in_period" }
                val result = JSONObject(tool.execute("""{"startDate":"2026-09-01","endDate":"2026-09-30"}"""))

                Then("it lists both wallet transactions and credit card debts") {
                    result.getJSONArray("walletTransactions").length() shouldBe 1
                    result.getJSONArray("creditCardDebts").length() shouldBe 1
                }
            }
        }

        Given("a credit card whose invoice for a month is fully paid") {
            val card = CreditCardFactory.create(id = 5, name = "Nubank Mastercard")
            every { creditCardService.getAllNonArchivedCreditCardsOrderedByName() } returns listOf(card)
            every { creditCardService.isInvoicePaid(5, YearMonth.of(2026, 9)) } returns true

            When("calling get_credit_card_invoice_payment_status, matching the card name case-insensitively") {
                val tool =
                    queryFinanceTool.availableTools().first {
                        it.name == "get_credit_card_invoice_payment_status"
                    }
                val result =
                    JSONObject(tool.execute("""{"creditCardName":"nubank mastercard","month":"2026-09"}"""))

                Then("it reports the invoice as paid") {
                    result.getBoolean("paid") shouldBe true
                }
            }
        }

        Given("a credit card name that doesn't match any registered card") {
            every { creditCardService.getAllNonArchivedCreditCardsOrderedByName() } returns emptyList()

            When("calling get_credit_card_invoice_payment_status") {
                val tool =
                    queryFinanceTool.availableTools().first {
                        it.name == "get_credit_card_invoice_payment_status"
                    }
                val result = JSONObject(tool.execute("""{"creditCardName":"Unknown Card","month":"2026-09"}"""))

                Then("it returns an error instead of throwing") {
                    result.has("error") shouldBe true
                }
            }
        }

        Given("an invalid month argument") {
            When("calling get_net_worth with a malformed month") {
                val tool = queryFinanceTool.availableTools().first { it.name == "get_net_worth" }
                val result = JSONObject(tool.execute("""{"month":"not-a-month"}"""))

                Then("it returns an error instead of throwing") {
                    result.has("error") shouldBe true
                }
            }
        }
    })
