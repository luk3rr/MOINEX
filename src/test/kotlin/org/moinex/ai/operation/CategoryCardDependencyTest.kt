/*
 * Filename: CategoryCardDependencyTest.kt
 * Created on: November 5, 2026
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 */

package org.moinex.ai.operation

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.moinex.ai.audit.ChangeSetAuditLogger
import org.moinex.ai.operation.transactions.CreateCategoryOperation
import org.moinex.ai.operation.transactions.CreateWalletTransactionOperation
import org.moinex.ai.operation.transactions.WalletTransactionPayload
import org.moinex.ai.review.ReviewSession
import org.moinex.ai.tools.ProposeChangesTool
import org.moinex.factory.CategoryFactory
import org.moinex.factory.creditcard.CreditCardFactory
import org.moinex.factory.wallet.WalletFactory
import org.moinex.model.Category
import org.moinex.model.wallettransaction.WalletTransaction
import org.moinex.service.CategoryService
import org.moinex.service.PreferencesService
import org.moinex.service.creditcard.CreditCardService
import org.moinex.service.investment.BondService
import org.moinex.service.investment.TickerService
import org.moinex.service.matching.Match
import org.moinex.service.matching.MatchQuery
import org.moinex.service.matching.TransactionMatchingService
import org.moinex.service.wallet.WalletService

class CategoryCardDependencyTest :
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
        every { categoryService.getCategories() } returns listOf(groceries)
        every { categoryService.createCategory(any()) } returns 50
        every { walletService.getAllNonArchivedWalletsOrderedByName() } returns listOf(nubank)
        every { walletService.getWalletById(10) } returns nubank
        every { walletService.createWalletTransaction(any(), any()) } returns 77
        every { creditCardService.getAllNonArchivedCreditCardsOrderedByName() } returns
            listOf(CreditCardFactory.create(id = 20, name = "Card"))
        every { tickerService.getAllNonArchivedTickers() } returns emptyList()
        every { bondService.getAllNonArchivedBonds() } returns emptyList()
        every { matchingService.findMatches(any<List<MatchQuery>>()) } answers
            { firstArg<List<MatchQuery>>().map { emptyList<Match>() } }
        every { resolver.resolveOrCreate(any()) } answers { Category(id = 99, name = firstArg()) }

        val registry =
            OperationRegistry(
                listOf(
                    CreateWalletTransactionOperation(walletService, resolver, preferencesService),
                    CreateCategoryOperation(categoryService),
                ),
            )
        val catalog =
            EntityCatalogProvider(walletService, creditCardService, tickerService, bondService, categoryService)
        val tool = ProposeChangesTool(registry, catalog, matchingService, preferencesService)

        fun line(
            category: String,
            description: String = "Pet shop",
        ) = """{"operation":"create_wallet_transaction","date":"2026-10-10","description":"$description",
            "amount":40,"walletName":"Nubank","movementType":"expense","categoryName":"$category","confidence":0.9}"""

        Given("two transactions that use the same category that does not exist yet") {
            val outcome = tool.propose("""{"changes":[${line("Pets")},${line("pets", "Vet")}]}""")
            val changes = outcome.changeSet.changes

            Then("one card creates the category, listed first, and both transactions depend on it") {
                changes.size shouldBe 3
                changes[0].operationName shouldBe "create_category"
                changes[1].dependsOn shouldBe listOf(changes[0].id)
                changes[2].dependsOn shouldBe listOf(changes[0].id)
            }

            Then("nothing is created until the user approves") {
                verify(exactly = 0) { categoryService.createCategory(any()) }
                verify(exactly = 0) { walletService.createWalletTransaction(any<WalletTransaction>(), any()) }
            }

            When("the transaction is approved without the category card") {
                val session = ReviewSession(registry, audit, preferencesService).also { it.add(outcome.changeSet) }
                session.approve(listOf(session.items[1]))

                Then("it fails with the dependency message and writes nothing") {
                    session.items[1].state shouldBe ChangeState.ERROR
                    session.items[1].messageProperty.get() shouldBe "aipanel.dependency.blocked"
                    verify(exactly = 0) { walletService.createWalletTransaction(any<WalletTransaction>(), any()) }
                }
            }

            When("everything is approved together") {
                val session = ReviewSession(registry, audit, preferencesService).also { it.add(outcome.changeSet) }
                val order = session.approve(session.items.toList())

                Then("the category is created first and then both transactions") {
                    order.map { it.change.operationName } shouldBe
                        listOf("create_category", "create_wallet_transaction", "create_wallet_transaction")
                    session.items.map { it.state } shouldBe List(3) { ChangeState.APPROVED }
                }
            }

            When("the category card is rejected and the transaction approved") {
                val session = ReviewSession(registry, audit, preferencesService).also { it.add(outcome.changeSet) }
                session.reject(session.items[0])
                session.approve(listOf(session.items[1]))

                Then("the transaction is blocked") {
                    session.items[1].state shouldBe ChangeState.ERROR
                }
            }
        }

        Given("a transaction whose category already exists") {
            val outcome = tool.propose("""{"changes":[${line("groceries")}]}""")

            Then("no category card is added and there is no dependency") {
                outcome.changeSet.changes.size shouldBe 1
                outcome.changeSet.changes[0].dependsOn shouldBe emptyList()
                (outcome.changeSet.changes[0].payload as WalletTransactionPayload).categoryName shouldBe "Groceries"
            }
        }

        Given("the model also sent a create_category for the same name") {
            val outcome =
                tool.propose(
                    """{"changes":[{"operation":"create_category","name":"Pets"},${line("Pets")}]}""",
                )

            Then("that card is reused instead of duplicated") {
                outcome.changeSet.changes.count { it.operationName == "create_category" } shouldBe 1
                val category = outcome.changeSet.changes.first { it.operationName == "create_category" }
                outcome.changeSet.changes
                    .first { it.operationName == "create_wallet_transaction" }
                    .dependsOn shouldBe listOf(category.id)
            }
        }
    })
