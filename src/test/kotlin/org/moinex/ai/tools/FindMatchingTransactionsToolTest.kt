package org.moinex.ai.tools

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import org.json.JSONObject
import org.moinex.factory.creditcard.CreditCardFactory
import org.moinex.factory.wallet.WalletFactory
import org.moinex.service.PreferencesService
import org.moinex.service.creditcard.CreditCardService
import org.moinex.service.matching.Match
import org.moinex.service.matching.MatchQuery
import org.moinex.service.matching.MatchStatus
import org.moinex.service.matching.MatchTargetType
import org.moinex.service.matching.TransactionMatchingService
import org.moinex.service.wallet.WalletService
import java.math.BigDecimal
import java.time.LocalDate

class FindMatchingTransactionsToolTest :
    BehaviorSpec({
        val matchingService = mockk<TransactionMatchingService>()
        val walletService = mockk<WalletService>()
        val creditCardService = mockk<CreditCardService>()
        val preferencesService = mockk<PreferencesService>()
        every { preferencesService.translate(any()) } answers { firstArg() }
        every { walletService.getAllNonArchivedWalletsOrderedByName() } returns
            listOf(WalletFactory.create(id = 10, name = "Nubank"))
        every { creditCardService.getAllNonArchivedCreditCardsOrderedByName() } returns
            listOf(CreditCardFactory.create(id = 20, name = "Nubank Mastercard"))

        val tool = FindMatchingTransactionsTool(matchingService, walletService, creditCardService, preferencesService)

        Given("a lookup by wallet name, amount, date and direction") {
            val query = slot<MatchQuery>()
            every { matchingService.findMatches(capture(query)) } returns
                listOf(
                    Match(
                        targetType = MatchTargetType.WALLET_TRANSACTION,
                        targetId = 198,
                        status = MatchStatus.CONFIRMED,
                        score = 0.93,
                        reasons = listOf("same amount"),
                        existingDescription = "Gym",
                        date = LocalDate.of(2026, 10, 10),
                        amount = BigDecimal("99.90"),
                    ),
                )

            When("executing the tool") {
                val output =
                    JSONObject(
                        tool.execute(
                            """{"amount":-99.90,"date":"2026-10-09","walletName":"nubank",
                                "movementType":"expense","description":"PIX GYM"}""",
                        ),
                    )

                Then("names are resolved to ids and the amount is made positive") {
                    query.captured.walletId shouldBe 10
                    query.captured.creditCardId shouldBe null
                    query.captured.isIncome shouldBe false
                    query.captured.amount.compareTo(BigDecimal("99.90")) shouldBe 0
                    query.captured.date shouldBe LocalDate.of(2026, 10, 9)
                }

                Then("the candidates come back with id, status, score and reasons") {
                    val match = output.getJSONArray("matches").getJSONObject(0)
                    match.getInt("targetId") shouldBe 198
                    match.getString("status") shouldBe "confirmed"
                    match.getString("targetType") shouldBe "wallet_transaction"
                    match.getJSONArray("reasons").getString(0) shouldBe "same amount"
                }
            }
        }

        Given("nothing similar is recorded") {
            every { matchingService.findMatches(any<MatchQuery>()) } returns emptyList()

            When("executing the tool for a card line") {
                val output =
                    JSONObject(
                        tool.execute("""{"amount":10,"date":"2026-10-09","creditCardName":"Nubank Mastercard"}"""),
                    )

                Then("the list is empty") {
                    output.getJSONArray("matches").length() shouldBe 0
                }
            }
        }

        Given("the tool definition") {
            Then("amount and date are required and the schema is valid JSON") {
                val schema = JSONObject(tool.jsonSchema)
                schema.getJSONArray("required").toList() shouldBe listOf("amount", "date")
                tool.name shouldBe "find_matching_transactions"
            }
        }
    })
