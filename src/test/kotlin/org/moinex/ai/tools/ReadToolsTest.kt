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
