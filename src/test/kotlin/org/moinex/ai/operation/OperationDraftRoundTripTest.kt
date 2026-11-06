/*
 * Filename: OperationDraftRoundTripTest.kt
 * Created on: November 5, 2026
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 */

package org.moinex.ai.operation

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.collections.shouldContainAll
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldNotBeBlank
import io.mockk.every
import io.mockk.mockk
import org.json.JSONObject
import org.moinex.ai.operation.creditcard.AddCreditCardCreditOperation
import org.moinex.ai.operation.creditcard.ArchiveCreditCardOperation
import org.moinex.ai.operation.creditcard.CreateCreditCardOperation
import org.moinex.ai.operation.creditcard.CreateRecurringCardDebtOperation
import org.moinex.ai.operation.creditcard.UnarchiveCreditCardOperation
import org.moinex.ai.operation.creditcard.UpdateCreditCardOperation
import org.moinex.ai.operation.creditcard.UpdateRecurringCardDebtOperation
import org.moinex.ai.operation.investments.CreateBondOperation
import org.moinex.ai.operation.investments.CreateTickerOperation
import org.moinex.ai.operation.investments.RefreshTickerDataOperation
import org.moinex.ai.operation.investments.UpdateBondOperation
import org.moinex.ai.operation.investments.UpdateTickerOperation
import org.moinex.ai.operation.planning.ArchiveGoalOperation
import org.moinex.ai.operation.planning.CreateGoalOperation
import org.moinex.ai.operation.planning.CreateWishlistItemOperation
import org.moinex.ai.operation.planning.UpdateGoalOperation
import org.moinex.ai.operation.planning.UpdateWishlistItemOperation
import org.moinex.ai.operation.transactions.ArchiveCategoryOperation
import org.moinex.ai.operation.transactions.ArchiveWalletOperation
import org.moinex.ai.operation.transactions.CreateCategoryOperation
import org.moinex.ai.operation.transactions.CreateRecurringTransactionOperation
import org.moinex.ai.operation.transactions.CreateTransferOperation
import org.moinex.ai.operation.transactions.CreateWalletOperation
import org.moinex.ai.operation.transactions.CreateWalletTransactionOperation
import org.moinex.ai.operation.transactions.UnarchiveWalletOperation
import org.moinex.ai.operation.transactions.UpdateCategoryOperation
import org.moinex.ai.operation.transactions.UpdateRecurringTransactionOperation
import org.moinex.ai.operation.transactions.UpdateTransferOperation
import org.moinex.ai.operation.transactions.UpdateWalletOperation
import org.moinex.ai.operation.transactions.UpdateWalletTransactionOperation
import org.moinex.factory.CategoryFactory
import org.moinex.factory.creditcard.CreditCardFactory
import org.moinex.factory.creditcard.RecurringCreditCardDebtFactory
import org.moinex.factory.goal.GoalFactory
import org.moinex.factory.investment.BondFactory
import org.moinex.factory.investment.TickerFactory
import org.moinex.factory.wallet.RecurringTransactionFactory
import org.moinex.factory.wallet.TransferFactory
import org.moinex.factory.wallet.WalletFactory
import org.moinex.factory.wallet.WalletTransactionFactory
import org.moinex.factory.wallet.WalletTypeFactory
import org.moinex.factory.wishlist.WishlistItemFactory
import org.moinex.model.Category
import org.moinex.model.creditcard.CreditCardOperator
import org.moinex.service.CategoryService
import org.moinex.service.PreferencesService
import org.moinex.service.creditcard.CreditCardService
import org.moinex.service.creditcard.RecurringCreditCardDebtService
import org.moinex.service.goal.GoalService
import org.moinex.service.investment.BondService
import org.moinex.service.investment.TickerPriceHistoryService
import org.moinex.service.investment.TickerService
import org.moinex.service.wallet.RecurringTransactionService
import org.moinex.service.wallet.WalletService
import org.moinex.service.wishlist.WishlistService
import java.time.LocalDate

/**
 * Every operation of the domains added after the first release must describe itself to the model, summarize its
 * payload, and, when the user can edit it, give the same payload back after a trip through the draft editor.
 */
class OperationDraftRoundTripTest :
    BehaviorSpec({
        val walletService = mockk<WalletService>()
        val categoryService = mockk<CategoryService>()
        val creditCardService = mockk<CreditCardService>()
        val recurringCardService = mockk<RecurringCreditCardDebtService>()
        val recurringService = mockk<RecurringTransactionService>()
        val tickerService = mockk<TickerService>()
        val history = mockk<TickerPriceHistoryService>()
        val bondService = mockk<BondService>()
        val goalService = mockk<GoalService>()
        val wishlistService = mockk<WishlistService>()
        val preferences = mockk<PreferencesService>()
        val resolver = mockk<CategoryResolver>()

        val food = CategoryFactory.create(id = 1, name = "Food")
        val bank = WalletFactory.create(id = 10, name = "Bank")
        val other = WalletFactory.create(id = 11, name = "Other")
        val card = CreditCardFactory.create(id = 20, name = "Card")
        val ticker = TickerFactory.create(id = 30, symbol = "PETR4.SA", name = "Petrobras")
        val bond = BondFactory.create(id = 40, name = "CDB Example", symbol = "CDBX")
        val goal = GoalFactory.create(id = 6, name = "Trip")
        val item = WishlistItemFactory.create(id = 8, title = "Laptop", category = food)
        val catalog =
            EntityCatalog(
                wallets = listOf(bank, other),
                creditCards = listOf(card),
                tickers = listOf(ticker),
                bonds = listOf(bond),
                categories = listOf(food),
            )
        val future = LocalDate.now().plusMonths(6)

        every { preferences.translate(any()) } answers { firstArg() }
        every { resolver.resolveOrCreate(any()) } answers { Category(id = 99, name = firstArg()) }
        every { walletService.getWalletById(10) } returns bank
        every { walletService.getWalletById(11) } returns other
        every { walletService.getAllWalletTypes() } returns listOf(WalletTypeFactory.create(5, "Checking"))
        every { walletService.getAllArchivedWallets() } returns
            listOf(WalletFactory.create(id = 12, name = "Old", isArchived = true))
        every { walletService.existsByName(any()) } returns false
        every { walletService.getWalletTransactionById(7) } returns
            WalletTransactionFactory.create(id = 7, wallet = bank, category = food, description = "Market")
        every { walletService.getAllTransfers() } returns
            listOf(TransferFactory.create(id = 3, senderWallet = bank, receiverWallet = other, category = food))
        every { recurringService.getAllRecurringTransactions() } returns
            listOf(RecurringTransactionFactory.create(id = 4, wallet = bank, category = food))
        every { categoryService.getCategories() } returns listOf(food)
        every { creditCardService.getAllCreditCardOperatorsOrderedByName() } returns
            listOf(CreditCardOperator(id = 1, name = "Visa"))
        every { creditCardService.getAllNonArchivedCreditCardsOrderedByName() } returns listOf(card)
        every { creditCardService.getAllArchivedCreditCards() } returns emptyList()
        every { recurringCardService.getAllRecurringDebts() } returns
            listOf(RecurringCreditCardDebtFactory.create(id = 3, creditCard = card, category = food))
        every { tickerService.getAllTickers() } returns listOf(ticker)
        every { tickerService.getAllNonArchivedTickers() } returns listOf(ticker)
        every { bondService.getAllNonArchivedBonds() } returns listOf(bond)
        every { bondService.getAllArchivedBonds() } returns emptyList()
        every { goalService.getAllGoals() } returns listOf(goal)
        every { wishlistService.getAllItems() } returns listOf(item)

        val date = "2026-11-01"
        val cases: List<Triple<AppOperation<*>, String, Boolean>> =
            listOf(
                Triple(
                    CreateWalletTransactionOperation(walletService, resolver, preferences),
                    """{"date":"$date","amount":10,"walletName":"Bank","categoryName":"Food","movementType":"income"}""",
                    true,
                ),
                Triple(
                    UpdateWalletTransactionOperation(walletService, resolver),
                    """{"transactionId":7,"amount":12}""",
                    true,
                ),
                Triple(
                    CreateTransferOperation(walletService, resolver),
                    """{"date":"$date","amount":5,"fromWalletName":"Bank","toWalletName":"Other","categoryName":"Food"}""",
                    true,
                ),
                Triple(UpdateTransferOperation(walletService, resolver), """{"transactionId":3,"amount":9}""", true),
                Triple(
                    CreateRecurringTransactionOperation(walletService, recurringService, resolver),
                    """{"date":"$date","amount":5,"walletName":"Bank","categoryName":"Food","movementType":"expense",
                        "frequency":"weekly","endDate":"2030-01-01"}""",
                    true,
                ),
                Triple(
                    UpdateRecurringTransactionOperation(walletService, recurringService, resolver),
                    """{"recurringId":4,"amount":9}""",
                    true,
                ),
                Triple(
                    CreateWalletOperation(walletService),
                    """{"name":"Cash","walletType":"Checking","amount":3}""",
                    true,
                ),
                Triple(
                    UpdateWalletOperation(walletService),
                    """{"walletName":"Bank","name":"Bank2","walletType":"Checking"}""",
                    true,
                ),
                Triple(ArchiveWalletOperation(walletService), """{"walletName":"Bank"}""", false),
                Triple(UnarchiveWalletOperation(walletService), """{"walletName":"Old"}""", false),
                Triple(CreateCategoryOperation(categoryService), """{"name":"Pets"}""", true),
                Triple(UpdateCategoryOperation(categoryService), """{"categoryName":"Food","name":"Meals"}""", true),
                Triple(ArchiveCategoryOperation(categoryService), """{"categoryName":"Food"}""", false),
                Triple(
                    CreateCreditCardOperation(creditCardService, walletService),
                    """{"name":"Travel","operator":"Visa","closingDay":5,"dueDay":12,"maxDebt":900,"billingWalletName":"Bank",
                        "lastFourDigits":"1234"}""",
                    true,
                ),
                Triple(
                    UpdateCreditCardOperation(creditCardService, walletService),
                    """{"creditCardName":"Card","maxDebt":950}""",
                    true,
                ),
                Triple(ArchiveCreditCardOperation(creditCardService), """{"creditCardName":"Card"}""", false),
                Triple(UnarchiveCreditCardOperation(creditCardService), """{"creditCardName":"Card"}""", false),
                Triple(
                    AddCreditCardCreditOperation(creditCardService),
                    """{"creditCardName":"Card","date":"$date","amount":4,"creditType":"reward"}""",
                    true,
                ),
                Triple(
                    CreateRecurringCardDebtOperation(creditCardService, recurringCardService, resolver),
                    """{"creditCardName":"Card","categoryName":"Food","amount":20,"date":"$date","cardFrequency":"yearly"}""",
                    true,
                ),
                Triple(
                    UpdateRecurringCardDebtOperation(creditCardService, recurringCardService, resolver),
                    """{"cardRecurringId":3,"amount":21}""",
                    true,
                ),
                Triple(
                    CreateTickerOperation(tickerService),
                    """{"symbol":"NEW3.SA","name":"New","assetType":"etf","amount":5}""",
                    true,
                ),
                Triple(UpdateTickerOperation(tickerService), """{"assetName":"PETR4","name":"Petro"}""", true),
                Triple(
                    RefreshTickerDataOperation(tickerService, history),
                    """{"assetName":"PETR4","rebuild":true}""",
                    false,
                ),
                Triple(
                    CreateBondOperation(bondService),
                    """{"name":"CDB Z","bondType":"lca","symbol":"Z1","issuer":"Bank","maturityDate":"2030-01-01",
                        "interestType":"fixed","interestIndex":"cdi","interestRate":105}""",
                    true,
                ),
                Triple(UpdateBondOperation(bondService), """{"assetName":"CDBX","issuer":"Other bank"}""", true),
                Triple(
                    CreateGoalOperation(goalService, walletService),
                    """{"name":"House","targetAmount":900,"targetDate":"$future","motivation":"home"}""",
                    true,
                ),
                Triple(UpdateGoalOperation(goalService), """{"goalName":"Trip","motivation":"new"}""", true),
                Triple(ArchiveGoalOperation(goalService), """{"goalName":"Trip"}""", false),
                Triple(
                    CreateWishlistItemOperation(wishlistService, resolver),
                    """{"title":"Camera","amount":900,"categoryName":"Food","priority":"low","targetDate":"$future","notes":"n"}""",
                    true,
                ),
                Triple(
                    UpdateWishlistItemOperation(wishlistService, resolver),
                    """{"itemTitle":"Laptop","notes":"x"}""",
                    true,
                ),
            )

        cases.forEach { (operation, json, editable) ->
            @Suppress("UNCHECKED_CAST")
            val op = operation as AppOperation<Any>

            Given("the operation ${op.name}") {
                val payload = op.fromModelArguments(JSONObject(json), catalog)

                Then("it describes itself, names its required fields and summarizes the payload") {
                    op.modelDescription().shouldNotBeBlank()
                    op.summary(payload).shouldNotBeBlank()
                    op.modelRequired().isNotEmpty() shouldBe true
                    op.preview(payload)
                    op.categoryNeeded(payload)
                    op.validate(payload)
                }

                if (editable) {
                    Then("its draft editor fields are all filled and the draft comes back as the same payload") {
                        val draft = op.toDraft(payload)!!
                        op.draftSpec().map { it.key } shouldContainAll
                            op.draftSpec().map { it.key }.filter { it in draft.fields }
                        op.draftSpec().forEach { field -> (field.key in draft.fields) shouldBe true }

                        val back = op.fromDraft(draft)!!
                        op.summary(back) shouldBe op.summary(payload)
                        op.toDraft(back) shouldBe draft
                    }

                    Then("a draft with nothing in it makes no payload") {
                        op.fromDraft(DraftValues(op.name, emptyMap())) shouldBe null
                    }
                } else {
                    Then("it has no draft editor") {
                        op.toDraft(payload) shouldBe null
                        op.draftSpec() shouldBe emptyList()
                    }
                }
            }
        }
    })
