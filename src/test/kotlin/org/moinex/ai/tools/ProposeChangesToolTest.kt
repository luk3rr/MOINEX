package org.moinex.ai.tools

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.types.shouldBeInstanceOf
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.json.JSONObject
import org.moinex.ai.operation.CategoryResolver
import org.moinex.ai.operation.ChangeBadge
import org.moinex.ai.operation.EntityCatalogProvider
import org.moinex.ai.operation.OperationRegistry
import org.moinex.ai.operation.creditcard.CreateCreditCardDebtOperation
import org.moinex.ai.operation.creditcard.CreditCardDebtPayload
import org.moinex.ai.operation.creditcard.CreditCardInvoicePaymentPayload
import org.moinex.ai.operation.creditcard.PayCreditCardInvoiceOperation
import org.moinex.ai.operation.investments.ApplyToBondOperation
import org.moinex.ai.operation.investments.InvestmentPayload
import org.moinex.ai.operation.investments.RecordDividendOperation
import org.moinex.ai.operation.investments.RecordStockPurchaseOperation
import org.moinex.ai.operation.investments.RecordStockSaleOperation
import org.moinex.ai.operation.investments.RedeemBondOperation
import org.moinex.ai.operation.transactions.CreateWalletTransactionOperation
import org.moinex.ai.operation.transactions.WalletTransactionPayload
import org.moinex.factory.CategoryFactory
import org.moinex.factory.creditcard.CreditCardFactory
import org.moinex.factory.investment.BondFactory
import org.moinex.factory.investment.TickerFactory
import org.moinex.factory.wallet.WalletFactory
import org.moinex.factory.wallet.WalletTransactionFactory
import org.moinex.model.enums.WalletTransactionStatus
import org.moinex.model.enums.WalletTransactionType
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
import java.time.YearMonth

class ProposeChangesToolTest :
    BehaviorSpec({
        val categoryService = mockk<CategoryService>()
        val walletService = mockk<WalletService>()
        val creditCardService = mockk<CreditCardService>()
        val tickerService = mockk<TickerService>()
        val bondService = mockk<BondService>()
        val matchingService = mockk<TransactionMatchingService>()
        val preferencesService = mockk<PreferencesService>()

        val groceries = CategoryFactory.create(id = 1, name = "Groceries")
        val nubank = WalletFactory.create(id = 10, name = "Nubank")
        val card = CreditCardFactory.create(id = 20, name = "Nubank Mastercard")
        val petr = TickerFactory.create(id = 30, symbol = "PETR4.SA", name = "Petrobras")
        val cdb = BondFactory.create(id = 40, name = "CDB Example", symbol = "CDBX")

        every { preferencesService.translate(any()) } answers { firstArg() }
        every { categoryService.getNonArchivedCategoriesOrderedByName() } returns listOf(groceries)
        every { walletService.getAllNonArchivedWalletsOrderedByName() } returns listOf(nubank)
        every { creditCardService.getAllNonArchivedCreditCardsOrderedByName() } returns listOf(card)
        every { tickerService.getAllNonArchivedTickers() } returns listOf(petr)
        every { bondService.getAllNonArchivedBonds() } returns listOf(cdb)
        every { matchingService.findMatches(any<List<MatchQuery>>()) } answers
            { firstArg<List<MatchQuery>>().map { emptyList<Match>() } }

        val resolver = mockk<CategoryResolver>()
        val registry =
            OperationRegistry(
                listOf(
                    CreateWalletTransactionOperation(walletService, resolver, preferencesService),
                    CreateCreditCardDebtOperation(creditCardService, resolver, preferencesService),
                    PayCreditCardInvoiceOperation(creditCardService, preferencesService),
                    RecordStockPurchaseOperation(tickerService, walletService, resolver, preferencesService),
                    RecordStockSaleOperation(tickerService, walletService, resolver, preferencesService),
                    RecordDividendOperation(tickerService, walletService, resolver, preferencesService),
                    ApplyToBondOperation(bondService, walletService, resolver, preferencesService),
                    RedeemBondOperation(bondService, walletService, resolver, preferencesService),
                ),
            )
        val catalog =
            EntityCatalogProvider(walletService, creditCardService, tickerService, bondService, categoryService)
        val tool = ProposeChangesTool(registry, catalog, matchingService, preferencesService)

        fun payloadOf(outcome: ProposeChangesTool.Outcome) =
            outcome.changeSet.changes
                .single()
                .payload

        val expense =
            """{"changes":[{"operation":"create_wallet_transaction","date":"2026-09-05","description":"Market",
                "amount":150.30,"walletName":"nubank","categoryName":"groceries","movementType":"expense",
                "confidence":0.9}]}"""

        Given("a wallet change whose names match existing records") {
            val outcome = tool.propose(expense)

            Then("names are resolved to ids case-insensitively and the change is new") {
                val payload = payloadOf(outcome).shouldBeInstanceOf<WalletTransactionPayload>()
                payload.walletId shouldBe 10
                payload.categoryName shouldBe "Groceries"
                payload.movementType shouldBe WalletTransactionType.EXPENSE
                outcome.changeSet.changes
                    .single()
                    .badge shouldBe ChangeBadge.NEW
                outcome.errors.size shouldBe 0
            }
        }

        Given("a change with an unknown wallet and a category that doesn't exist yet") {
            val outcome =
                tool.propose(
                    """{"changes":[{"operation":"create_wallet_transaction","date":"2026-09-05","description":"x",
                        "amount":50,"walletName":"nowhere","categoryName":"Brand New","movementType":"expense"}]}""",
                )

            Then("the wallet stays unresolved, the proposed category name is kept and nothing is created") {
                val payload = payloadOf(outcome).shouldBeInstanceOf<WalletTransactionPayload>()
                payload.walletId shouldBe null
                payload.categoryName shouldBe "Brand New"
                verify(exactly = 0) { categoryService.createCategory(any()) }
            }
        }

        Given("a change missing a required field, next to a valid one") {
            val outcome =
                tool.propose(
                    """{"changes":[
                        {"operation":"create_wallet_transaction","description":"No date","amount":10,"categoryName":"Groceries"},
                        {"operation":"create_wallet_transaction","date":"2026-09-05","description":"ok","amount":10,
                         "walletName":"Nubank","categoryName":"Groceries"}]}""",
                )

            Then("the malformed one is reported as an error and the batch goes on") {
                outcome.changeSet.changes.size shouldBe 1
                outcome.errors.single().index shouldBe 0
            }
        }

        Given("an unknown operation name") {
            val outcome = tool.propose("""{"changes":[{"operation":"delete_wallet","date":"2026-09-05"}]}""")

            Then("it is an error, never a change") {
                outcome.changeSet.changes.size shouldBe 0
                outcome.errors.single().message shouldContain "unknown operation"
            }
        }

        Given("an income sent with a negative amount") {
            val outcome =
                tool.propose(
                    """{"changes":[{"operation":"create_wallet_transaction","date":"2026-09-05","description":"Salary",
                        "amount":-3000,"walletName":"Nubank","categoryName":"Groceries","movementType":"income"}]}""",
                )

            Then("the amount is positive and the direction is income") {
                val payload = payloadOf(outcome).shouldBeInstanceOf<WalletTransactionPayload>()
                payload.amount.signum() shouldBe 1
                payload.movementType shouldBe WalletTransactionType.INCOME
            }
        }

        Given("a change with low confidence") {
            val outcome = tool.propose(expense.replace("\"confidence\":0.9", "\"confidence\":0.3"))

            Then("it carries the low confidence badge") {
                outcome.changeSet.changes
                    .single()
                    .badge shouldBe ChangeBadge.LOW_CONFIDENCE
            }
        }

        Given("an existingTransactionId pointing to a pending transaction") {
            every { walletService.getWalletTransactionById(214) } returns
                WalletTransactionFactory.create(
                    id = 214,
                    status = WalletTransactionStatus.PENDING,
                    description = "Academia",
                )
            val outcome =
                tool.propose(
                    expense.replace("\"confidence\":0.9", "\"confidence\":0.9,\"existingTransactionId\":214"),
                )

            Then("it confirms that one, keeping its own description") {
                val payload = payloadOf(outcome).shouldBeInstanceOf<WalletTransactionPayload>()
                payload.existingTransactionId shouldBe 214
                payload.description shouldBe "Academia"
                outcome.changeSet.changes
                    .single()
                    .badge shouldBe ChangeBadge.MATCHES_EXISTING
            }
        }

        Given("an existingTransactionId pointing to an already confirmed or missing transaction") {
            every { walletService.getWalletTransactionById(215) } returns
                WalletTransactionFactory.create(id = 215, status = WalletTransactionStatus.CONFIRMED)
            every { walletService.getWalletTransactionById(999) } throws NoSuchElementException("missing")

            Then("it is ignored and the change is created as new") {
                listOf(215, 999).forEach { id ->
                    val outcome =
                        tool.propose(
                            expense.replace("\"confidence\":0.9", "\"confidence\":0.9,\"existingTransactionId\":$id"),
                        )
                    payloadOf(outcome).shouldBeInstanceOf<WalletTransactionPayload>().existingTransactionId shouldBe
                        null
                }
            }
        }

        Given("a card purchase seen for the first time, in installments") {
            val outcome =
                tool.propose(
                    """{"changes":[{"operation":"create_credit_card_debt","date":"2026-09-05","description":"Notebook",
                        "amount":100.00,"creditCardName":"nubank mastercard","categoryName":"Groceries",
                        "installments":10,"installmentNumber":1}]}""",
                )

            Then("the whole purchase is the installment value times the installments") {
                val payload = payloadOf(outcome).shouldBeInstanceOf<CreditCardDebtPayload>()
                payload.creditCardId shouldBe 20
                payload.installments shouldBe 10
                payload.amount.compareTo(BigDecimal("1000.00")) shouldBe 0
            }
        }

        Given("a statement line continuing an installment plan from a prior invoice") {
            val outcome =
                tool.propose(
                    """{"changes":[{"operation":"create_credit_card_debt","date":"2026-05-05","description":"Notebook",
                        "amount":100.00,"creditCardName":"Nubank Mastercard","categoryName":"Groceries",
                        "installments":10,"installmentNumber":3}]}""",
                )

            Then("only the remaining installments are scheduled and the original position is kept") {
                val payload = payloadOf(outcome).shouldBeInstanceOf<CreditCardDebtPayload>()
                payload.installments shouldBe 8
                payload.installmentNumber shouldBe 3
                payload.amount.compareTo(BigDecimal("800.00")) shouldBe 0
            }
        }

        Given("a card purchase with no installment fields") {
            val outcome =
                tool.propose(
                    """{"changes":[{"operation":"create_credit_card_debt","date":"2026-09-05","description":"Shoes",
                        "amount":120.00,"creditCardName":"Nubank Mastercard","categoryName":"Groceries"}]}""",
                )

            Then("it is a single payment") {
                val payload = payloadOf(outcome).shouldBeInstanceOf<CreditCardDebtPayload>()
                payload.installments shouldBe 1
                payload.amount.compareTo(BigDecimal("120.00")) shouldBe 0
            }
        }

        Given("an invoice payment without an invoice month") {
            val outcome =
                tool.propose(
                    """{"changes":[{"operation":"pay_credit_card_invoice","date":"2026-10-05","description":"Fatura",
                        "amount":500,"creditCardName":"Nubank Mastercard","categoryName":"Groceries"}]}""",
                )

            Then("the month defaults to the payment date's month") {
                val payload = payloadOf(outcome).shouldBeInstanceOf<CreditCardInvoicePaymentPayload>()
                payload.invoiceMonth shouldBe YearMonth.of(2026, 10)
            }
        }

        Given("a stock purchase whose symbol matches a ticker stored with an exchange suffix") {
            val outcome =
                tool.propose(
                    """{"changes":[{"operation":"record_stock_purchase","date":"2026-09-05","description":"Buy",
                        "assetName":"PETR4","quantity":10,"unitPrice":30.5,"walletName":"Nubank",
                        "categoryName":"Groceries"}]}""",
                )

            Then("the base symbol matches and the label names the asset") {
                val payload = payloadOf(outcome).shouldBeInstanceOf<InvestmentPayload>()
                payload.assetId shouldBe 30
                payload.assetLabel shouldBe "PETR4.SA - Petrobras"
                payload.context.walletId shouldBe 10
            }
        }

        Given("an investment whose asset matches nothing") {
            val outcome =
                tool.propose(
                    """{"changes":[{"operation":"record_stock_purchase","date":"2026-09-05","description":"Buy",
                        "assetName":"UNKNOWN","quantity":10,"unitPrice":30.5,"walletName":"Nubank",
                        "categoryName":"Groceries"}]}""",
                )

            Then("the asset stays unresolved for the user to choose") {
                val payload = payloadOf(outcome).shouldBeInstanceOf<InvestmentPayload>()
                payload.assetId shouldBe null
                payload.assetLabel shouldBe "UNKNOWN"
            }
        }

        Given("a CDB application and a dividend") {
            val outcome =
                tool.propose(
                    """{"changes":[
                        {"operation":"apply_to_bond","date":"2026-09-05","description":"CDB","assetName":"CDBX",
                         "quantity":1,"unitPrice":1000,"fees":2,"taxes":1,"walletName":"Nubank","categoryName":"Groceries"},
                        {"operation":"record_dividend","date":"2026-09-06","description":"Div","assetName":"PETR4",
                         "dividendAmount":42.5,"walletName":"Nubank","categoryName":"Groceries"}]}""",
                )

            Then("the bond is matched among bonds and the dividend amount is read") {
                val payloads = outcome.changeSet.changes.map { it.payload as InvestmentPayload }
                payloads[0].assetId shouldBe 40
                payloads[0].fees.compareTo(BigDecimal("2")) shouldBe 0
                payloads[1].assetId shouldBe 30
                payloads[1].amount.compareTo(BigDecimal("42.5")) shouldBe 0
            }
        }

        fun match(
            id: Int,
            status: MatchStatus,
            type: MatchTargetType = MatchTargetType.WALLET_TRANSACTION,
        ) = Match(
            targetType = type,
            targetId = id,
            status = status,
            score = 0.95,
            reasons = listOf("same amount", "same date"),
            existingDescription = "Gym",
            date = LocalDate.of(2026, 10, 10),
            amount = BigDecimal("99.90"),
        )

        val gymLine =
            """{"changes":[{"operation":"create_wallet_transaction","date":"2026-10-10","description":"PIX ACADEMIA FIT",
                "amount":99.90,"walletName":"Nubank","movementType":"expense","categoryName":"Groceries",
                "confidence":0.9}]}"""

        Given("a line that matches a pending wallet transaction under another name") {
            every { matchingService.findMatches(any<List<MatchQuery>>()) } returns
                listOf(listOf(match(214, MatchStatus.PENDING)))
            every { walletService.getWalletTransactionById(214) } returns
                WalletTransactionFactory.create(
                    id = 214,
                    status = WalletTransactionStatus.PENDING,
                    description = "Academia",
                )

            val outcome = tool.propose(gymLine)

            Then("the pending one is confirmed with its own description and nothing is ignored") {
                val payload = payloadOf(outcome).shouldBeInstanceOf<WalletTransactionPayload>()
                payload.existingTransactionId shouldBe 214
                payload.description shouldBe "Academia"
                outcome.changeSet.changes
                    .single()
                    .badge shouldBe ChangeBadge.MATCHES_EXISTING
                outcome.changeSet.ignored.size shouldBe 0
            }
        }

        Given("a line that matches an already confirmed transaction") {
            every { matchingService.findMatches(any<List<MatchQuery>>()) } returns
                listOf(listOf(match(198, MatchStatus.CONFIRMED)))

            When("proposed normally") {
                val outcome = tool.propose(gymLine)

                Then("no change is made and the line is ignored with the record id and reasons") {
                    outcome.changeSet.changes.size shouldBe 0
                    outcome.changeSet.ignored
                        .single()
                        .targetId shouldBe 198
                    outcome.changeSet.ignored
                        .single()
                        .reasons shouldBe listOf("same amount", "same date")
                }

                Then("the model is told which line was ignored") {
                    JSONObject(tool.toModelJson(outcome)).getJSONArray("ignored").length() shouldBe 1
                }
            }

            When("the user explicitly asked to record it anyway") {
                val outcome =
                    tool.propose(
                        gymLine.replace("\"confidence\":0.9", "\"confidence\":0.9,\"allowDuplicate\":true"),
                    )

                Then("it becomes a possible duplicate change") {
                    outcome.changeSet.changes
                        .single()
                        .badge shouldBe ChangeBadge.POSSIBLE_DUPLICATE
                    outcome.changeSet.ignored.size shouldBe 0
                }
            }
        }

        Given("a card installment whose purchase is already stored") {
            every { matchingService.findMatches(any<List<MatchQuery>>()) } returns
                listOf(listOf(match(31, MatchStatus.CONFIRMED, MatchTargetType.CREDIT_CARD_DEBT)))
            val outcome =
                tool.propose(
                    """{"changes":[{"operation":"create_credit_card_debt","date":"2026-09-05","description":"NOTEBOOK 3/10",
                        "amount":100.00,"creditCardName":"Nubank Mastercard","categoryName":"Groceries",
                        "installments":10,"installmentNumber":3}]}""",
                )

            Then("it is ignored instead of proposed again") {
                outcome.changeSet.changes.size shouldBe 0
                outcome.changeSet.ignored
                    .single()
                    .targetType shouldBe MatchTargetType.CREDIT_CARD_DEBT
            }
        }

        Given("an invoice payment and an investment") {
            val queries = slot<List<MatchQuery>>()
            every { matchingService.findMatches(capture(queries)) } answers
                { firstArg<List<MatchQuery>>().map { emptyList<Match>() } }
            tool.propose(
                """{"changes":[
                    {"operation":"pay_credit_card_invoice","date":"2026-10-05","description":"Fatura","amount":500,
                     "creditCardName":"Nubank Mastercard","categoryName":"Groceries"},
                    {"operation":"record_dividend","date":"2026-09-06","description":"Div","assetName":"PETR4",
                     "dividendAmount":42.5,"walletName":"Nubank","categoryName":"Groceries"}]}""",
            )

            Then("they are never looked up among stored records") {
                queries.captured.size shouldBe 0
            }
        }

        Given("a wallet line with its direction and wallet resolved") {
            val queries = slot<List<MatchQuery>>()
            every { matchingService.findMatches(capture(queries)) } answers
                { firstArg<List<MatchQuery>>().map { emptyList<Match>() } }
            tool.propose(gymLine)

            Then("the lookup uses the amount, date, wallet, direction and description") {
                val query = queries.captured.single()
                query.amount.compareTo(BigDecimal("99.90")) shouldBe 0
                query.date shouldBe LocalDate.of(2026, 10, 10)
                query.walletId shouldBe 10
                query.isIncome shouldBe false
                query.description shouldBe "PIX ACADEMIA FIT"
            }
        }

        Given("the tool definition sent to the providers") {
            Then("it lists every operation, requires the changes array and fits the tool budget") {
                val schema = JSONObject(tool.jsonSchema)
                val item = schema.getJSONObject("properties").getJSONObject("changes").getJSONObject("items")
                val operations =
                    item
                        .getJSONObject(
                            "properties",
                        ).getJSONObject("operation")
                        .getJSONArray("enum")
                        .toList()
                operations shouldContain "create_wallet_transaction"
                operations shouldContain "redeem_bond"
                operations.none { name ->
                    listOf("delete", "remove", "drop", "purge").any { name.toString().startsWith(it) }
                } shouldBe
                    true
                schema.getJSONArray("required").toList() shouldBe listOf("changes")
                tool.name shouldBe "propose_changes"
            }
        }

        Given("the context description") {
            Then("it lists categories, wallets, cards, tickers and bonds") {
                val context = tool.describeAvailableContext()
                listOf("Groceries", "Nubank", "Nubank Mastercard", "PETR4.SA", "CDB Example").forEach {
                    context shouldContain it
                }
            }
        }

        Given("any set of changes, valid or not") {
            Then("the tool never writes through a service") {
                tool.propose(expense)
                verify(exactly = 0) { categoryService.createCategory(any()) }
                verify(exactly = 0) { walletService.createWalletTransaction(any(), any()) }
            }
        }

        Given("an investment sent without quantity and unit price") {
            val outcome =
                tool.propose(
                    """{"changes":[{"operation":"record_stock_purchase","date":"2026-09-05","description":"Buy",
                        "assetName":"PETR4","walletName":"Nubank","categoryName":"Groceries"}]}""",
                )

            Then("it is not accepted: the model is told what is missing so it can resend") {
                outcome.changeSet.changes.size shouldBe 0
                outcome.errors.single().message shouldContain "quantity must be greater than 0"
                outcome.errors.single().message shouldContain "unitPrice must be greater than 0"
                JSONObject(tool.toModelJson(outcome)).getJSONArray("errors").length() shouldBe 1
            }
        }

        Given("a dividend without amount and an invoice payment without amount") {
            val outcome =
                tool.propose(
                    """{"changes":[
                        {"operation":"record_dividend","date":"2026-09-06","description":"Div","assetName":"PETR4",
                         "walletName":"Nubank","categoryName":"Groceries"},
                        {"operation":"pay_credit_card_invoice","date":"2026-10-05","description":"Fatura",
                         "creditCardName":"Nubank Mastercard","categoryName":"Groceries"}]}""",
                )

            Then("both are errors for the model to fix") {
                outcome.changeSet.changes.size shouldBe 0
                outcome.errors.size shouldBe 2
            }
        }

        Given("a statement from an institution whose name is longer than the wallet name") {
            val outcome =
                tool.propose(
                    """{"changes":[{"operation":"record_stock_purchase","date":"2026-09-05","description":"Buy",
                        "assetName":"PETR4","quantity":10,"unitPrice":30.5,"walletName":"Banco Nubank",
                        "categoryName":"Groceries"}]}""",
                )

            Then("the wallet is still found") {
                (payloadOf(outcome) as InvestmentPayload).context.walletId shouldBe 10
            }
        }
    })
