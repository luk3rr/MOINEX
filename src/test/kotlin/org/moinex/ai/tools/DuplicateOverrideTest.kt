/*
 * Filename: DuplicateOverrideTest.kt
 * Created on: November 5, 2026
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 */

package org.moinex.ai.tools

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.moinex.ai.audit.ChangeSetAuditLogger
import org.moinex.ai.operation.CategoryResolver
import org.moinex.ai.operation.ChangeBadge
import org.moinex.ai.operation.EntityCatalogProvider
import org.moinex.ai.operation.OperationRegistry
import org.moinex.ai.operation.transactions.CreateWalletTransactionOperation
import org.moinex.ai.operation.transactions.WalletTransactionPayload
import org.moinex.ai.review.ReviewSession
import org.moinex.factory.CategoryFactory
import org.moinex.factory.wallet.WalletFactory
import org.moinex.model.wallettransaction.WalletTransaction
import org.moinex.service.CategoryService
import org.moinex.service.PreferencesService
import org.moinex.service.creditcard.CreditCardService
import org.moinex.service.investment.BondService
import org.moinex.service.investment.TickerService
import org.moinex.service.matching.Match
import org.moinex.service.matching.MatchQuery
import org.moinex.service.matching.MatchStatus
import org.moinex.service.matching.MatchTargetType
import org.moinex.service.matching.TransactionMatchingService
import org.moinex.service.wallet.WalletService
import java.math.BigDecimal
import java.time.LocalDate

class DuplicateOverrideTest :
    BehaviorSpec({
        val walletService = mockk<WalletService>()
        val categoryService = mockk<CategoryService>()
        val creditCardService = mockk<CreditCardService>()
        val tickerService = mockk<TickerService>()
        val bondService = mockk<BondService>()
        val matchingService = mockk<TransactionMatchingService>()
        val preferencesService = mockk<PreferencesService>()
        val resolver = mockk<CategoryResolver>()
        val audit = mockk<ChangeSetAuditLogger>(relaxed = true)

        val groceries = CategoryFactory.create(id = 1, name = "Groceries")
        val nubank = WalletFactory.create(id = 10, name = "Nubank")

        every { preferencesService.translate(any()) } answers { firstArg() }
        every { categoryService.getNonArchivedCategoriesOrderedByName() } returns listOf(groceries)
        every { walletService.getAllNonArchivedWalletsOrderedByName() } returns listOf(nubank)
        every { walletService.getWalletById(10) } returns nubank
        every { walletService.createWalletTransaction(any(), any()) } returns 77
        every { creditCardService.getAllNonArchivedCreditCardsOrderedByName() } returns emptyList()
        every { tickerService.getAllNonArchivedTickers() } returns emptyList()
        every { bondService.getAllNonArchivedBonds() } returns emptyList()
        every { resolver.resolveOrCreate(any()) } returns groceries
        every { matchingService.findMatches(any<List<MatchQuery>>()) } returns
            listOf(
                listOf(
                    Match(
                        MatchTargetType.WALLET_TRANSACTION,
                        198,
                        MatchStatus.CONFIRMED,
                        0.95,
                        listOf("same amount"),
                        "Gym",
                        LocalDate.of(2026, 10, 10),
                        BigDecimal("99.90"),
                    ),
                ),
            )

        val operation = CreateWalletTransactionOperation(walletService, resolver, preferencesService)
        val registry = OperationRegistry(listOf(operation))
        val catalog =
            EntityCatalogProvider(walletService, creditCardService, tickerService, bondService, categoryService)
        val tool = ProposeChangesTool(registry, catalog, matchingService, preferencesService)

        val line =
            """{"changes":[{"operation":"create_wallet_transaction","date":"2026-10-10","description":"Gym again",
                "amount":99.90,"walletName":"Nubank","movementType":"expense","categoryName":"Groceries",
                "confidence":0.9%s}]}"""

        Given("a line already recorded and confirmed") {
            When("the user asks to record it anyway") {
                val outcome = tool.propose(line.format(",\"allowDuplicate\":true"))
                val session = ReviewSession(registry, audit, preferencesService).also { it.add(outcome.changeSet) }
                val item = session.items.single()

                Then("it is a possible duplicate that does not start selected and carries the reasons") {
                    item.change.badge shouldBe ChangeBadge.POSSIBLE_DUPLICATE
                    item.change.reasons shouldBe listOf("same amount")
                    item.selectedProperty.get() shouldBe false
                }

                Then("approving it creates a new transaction instead of touching the existing one") {
                    session.approve(listOf(item))

                    (item.payloadProperty.get() as WalletTransactionPayload).existingTransactionId shouldBe null
                    verify(exactly = 1) { walletService.createWalletTransaction(any<WalletTransaction>(), any()) }
                    verify(exactly = 0) { walletService.updateWalletTransaction(any()) }
                }
            }

            When("the user did not ask for it") {
                val outcome = tool.propose(line.format(""))

                Then("it is ignored with the existing record id and creates no card") {
                    outcome.changeSet.changes.size shouldBe 0
                    outcome.changeSet.ignored
                        .single()
                        .targetId shouldBe 198
                }
            }
        }
    })
