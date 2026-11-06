/*
 * Filename: ReadToolsTest.kt
 * Created on: November 5, 2026
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 */

package org.moinex.ai.tools

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import org.json.JSONObject
import org.moinex.ai.chat.ToolLabels
import org.moinex.factory.CategoryFactory
import org.moinex.factory.creditcard.CreditCardDebtFactory
import org.moinex.factory.creditcard.CreditCardFactory
import org.moinex.factory.creditcard.RecurringCreditCardDebtFactory
import org.moinex.factory.goal.GoalFactory
import org.moinex.factory.investment.BondFactory
import org.moinex.factory.investment.TickerFactory
import org.moinex.factory.wallet.RecurringTransactionFactory
import org.moinex.factory.wallet.WalletFactory
import org.moinex.factory.wallet.WalletTransactionFactory
import org.moinex.model.enums.WalletTransactionStatus
import org.moinex.service.CategoryService
import org.moinex.service.PreferencesService
import org.moinex.service.creditcard.CreditCardService
import org.moinex.service.goal.GoalService
import org.moinex.service.investment.TickerService
import org.moinex.service.wallet.RecurringTransactionService
import org.moinex.service.wallet.WalletService
import java.math.BigDecimal
import java.time.LocalDateTime

class ReadToolsTest :
    BehaviorSpec({
        val walletService = mockk<WalletService>()
        val creditCardService = mockk<CreditCardService>()
        val categoryService = mockk<CategoryService>()
        val tickerService = mockk<TickerService>()
        val recurringService = mockk<RecurringTransactionService>()
        val goalService = mockk<GoalService>()
        val bondService = mockk<org.moinex.service.investment.BondService>()
        val recurringCardService = mockk<org.moinex.service.creditcard.RecurringCreditCardDebtService>()
        val tools =
            ReadTools(
                walletService,
                creditCardService,
                categoryService,
                tickerService,
                recurringService,
                goalService,
                bondService,
                recurringCardService,
            )

        val bank = WalletFactory.create(id = 1, name = "Bank")
        val food = CategoryFactory.create(id = 2, name = "Food")
        val transactions =
            (1..45).map {
                WalletTransactionFactory.create(
                    id = it,
                    date = LocalDateTime.of(2026, 10, 1, 12, 0).plusDays(it.toLong() % 28),
                    description = if (it % 2 == 0) "Market $it" else "Gym $it",
                    amount = BigDecimal(it),
                    wallet = bank,
                    category = food,
                    status = WalletTransactionStatus.CONFIRMED,
                )
            }
        every { walletService.getAllNonArchivedWalletTransactionsBetweenDates(any(), any()) } returns transactions
        every { creditCardService.getDebtsBetweenForAnalysis(any(), any()) } returns emptyList()

        val search = tools.tools().first { it.name == ReadTools.SEARCH_TRANSACTIONS }
        val list = tools.tools().first { it.name == ReadTools.LIST_RECORDS }

        Given("45 transactions and a search with no filter") {
            val first = JSONObject(search.execute("{}"))

            Then("the first page has 20 items, the total and a next page") {
                first.getInt("total") shouldBe 45
                first.getJSONArray("items").length() shouldBe 20
                first.getBoolean("hasMore") shouldBe true
            }

            Then("the last page is partial and has no next page") {
                val last = JSONObject(search.execute("""{"page":3}"""))
                last.getJSONArray("items").length() shouldBe 5
                last.getBoolean("hasMore") shouldBe false
            }

            Then("the page size is capped") {
                JSONObject(search.execute("""{"pageSize":500}""")).getInt("pageSize") shouldBe ReadTools.MAX_PAGE_SIZE
            }
        }

        Given("a search with text and an amount range") {
            val result = JSONObject(search.execute("""{"text":"market","minAmount":10,"maxAmount":20}"""))

            Then("only the matching entries come back, each with its id") {
                result.getInt("total") shouldBe 6
                result.getJSONArray("items").getJSONObject(0).has("id") shouldBe true
            }
        }

        Given("a listing request") {
            every { walletService.getAllNonArchivedWalletsOrderedByName() } returns listOf(bank)

            Then("it lists the kind asked and refuses an unknown kind") {
                JSONObject(list.execute("""{"kind":"wallets"}""")).getInt("total") shouldBe 1
                runCatching { list.execute("""{"kind":"nope"}""") }.isFailure shouldBe true
            }
        }

        Given("every kind of listing") {
            val card = CreditCardFactory.create(id = 20, name = "Card")
            every { walletService.getAllWalletsOrderedByName() } returns listOf(bank)
            every { categoryService.getCategories() } returns
                listOf(food, CategoryFactory.create(id = 3, name = "Old", isArchived = true))
            every { creditCardService.getAllNonArchivedCreditCardsOrderedByName() } returns listOf(card)
            every { creditCardService.getAllArchivedCreditCards() } returns
                listOf(CreditCardFactory.create(id = 21, name = "Gone", isArchived = true))
            every { tickerService.getAllNonArchivedTickers() } returns listOf(TickerFactory.create(id = 30))
            every { recurringService.getAllRecurringTransactions() } returns
                listOf(RecurringTransactionFactory.create(id = 4, wallet = bank, category = food))
            every { goalService.getAllGoals() } returns
                listOf(GoalFactory.create(id = 6), GoalFactory.create(id = 7, name = "Done", isArchived = true))
            every { bondService.getAllNonArchivedBonds() } returns listOf(BondFactory.create(id = 40))
            every { recurringCardService.getAllRecurringDebts() } returns
                listOf(RecurringCreditCardDebtFactory.create(id = 3, creditCard = card, category = food))

            fun total(args: String) = JSONObject(list.execute(args)).getInt("total")

            Then("each kind lists its records and archived ones only when asked") {
                total("""{"kind":"wallets","includeArchived":true}""") shouldBe 1
                total("""{"kind":"categories"}""") shouldBe 1
                total("""{"kind":"categories","includeArchived":true}""") shouldBe 2
                total("""{"kind":"credit_cards"}""") shouldBe 1
                total("""{"kind":"credit_cards","includeArchived":true}""") shouldBe 2
                total("""{"kind":"tickers"}""") shouldBe 1
                total("""{"kind":"recurring"}""") shouldBe 1
                total("""{"kind":"goals"}""") shouldBe 1
                total("""{"kind":"goals","includeArchived":true}""") shouldBe 2
                total("""{"kind":"bonds"}""") shouldBe 1
                total("""{"kind":"card_recurring"}""") shouldBe 1
            }
        }

        Given("a search over wallet transactions and card purchases") {
            every { creditCardService.getDebtsBetweenForAnalysis(any(), any()) } returns
                listOf(
                    CreditCardDebtFactory.create(
                        id = 5,
                        category = food,
                        creditCard = CreditCardFactory.create(id = 20, name = "Card"),
                        description = "Shoes",
                        amount = BigDecimal("300"),
                        date = LocalDateTime.of(2026, 10, 30, 10, 0),
                    ),
                )

            Then("card purchases are found with their kind and the filters narrow them") {
                val shoes = JSONObject(search.execute("""{"text":"shoes","walletOrCard":"card","category":"food"}"""))
                shoes.getInt("total") shouldBe 1
                shoes.getJSONArray("items").getJSONObject(0).getString("kind") shouldBe "credit_card_debt"
                JSONObject(
                    search.execute("""{"startDate":"2026-10-01","endDate":"2026-12-31","walletOrCard":"nobody"}"""),
                ).getInt("total") shouldBe
                    0
            }
        }

        Given("the activity labels") {
            val preferences = mockk<PreferencesService>()
            every { preferences.translate("aitool.label.list_records") } returns "listing records"
            every { preferences.translate("aitool.label.mystery") } returns "aitool.label.mystery"

            Then("a known tool shows its label and an unknown one its name") {
                ToolLabels.of("list_records", preferences) shouldBe "listing records"
                ToolLabels.of("mystery", preferences) shouldBe "mystery"
            }
        }
    })
