package org.moinex.ai.evaluation

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.types.shouldBeInstanceOf
import io.mockk.every
import io.mockk.mockk
import org.moinex.ai.chat.AiChatService
import org.moinex.ai.chat.ChatMessage
import org.moinex.ai.chat.ChatRole
import org.moinex.ai.chat.ChatTurnResult
import org.moinex.ai.chat.LlmResponse
import org.moinex.ai.chat.ToolCall
import org.moinex.ai.operation.CategoryResolver
import org.moinex.ai.operation.ChangeBadge
import org.moinex.ai.operation.EntityCatalogProvider
import org.moinex.ai.operation.OperationRegistry
import org.moinex.ai.operation.creditcard.CreateCreditCardDebtOperation
import org.moinex.ai.operation.creditcard.CreditCardDebtPayload
import org.moinex.ai.operation.creditcard.PayCreditCardInvoiceOperation
import org.moinex.ai.operation.investments.RecordStockPurchaseOperation
import org.moinex.ai.operation.transactions.CreateWalletTransactionOperation
import org.moinex.ai.operation.transactions.WalletTransactionPayload
import org.moinex.ai.prompt.PromptComposer
import org.moinex.ai.provider.LlmProviderFactory
import org.moinex.ai.tools.FindMatchingTransactionsTool
import org.moinex.ai.tools.ProposeChangesTool
import org.moinex.ai.tools.QueryFinanceTool
import org.moinex.common.ClockProvider
import org.moinex.config.AiPromptProperties
import org.moinex.factory.CategoryFactory
import org.moinex.factory.creditcard.CreditCardDebtFactory
import org.moinex.factory.creditcard.CreditCardFactory
import org.moinex.factory.wallet.WalletFactory
import org.moinex.factory.wallet.WalletTransactionFactory
import org.moinex.model.creditcard.CreditCardDebt
import org.moinex.model.enums.WalletTransactionStatus
import org.moinex.model.wallettransaction.WalletTransaction
import org.moinex.repository.creditcard.CreditCardDebtRepository
import org.moinex.repository.wallettransaction.TransferRepository
import org.moinex.repository.wallettransaction.WalletTransactionRepository
import org.moinex.service.CategoryService
import org.moinex.service.PreferencesService
import org.moinex.service.creditcard.CreditCardService
import org.moinex.service.investment.BondService
import org.moinex.service.investment.TickerService
import org.moinex.service.matching.MatchTargetType
import org.moinex.service.matching.TransactionMatchingService
import org.moinex.service.wallet.WalletService
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.Locale

/**
 * Golden cases for the assistant. The model is scripted, so they check what the app itself guarantees: how it matches
 * proposals against stored records, what it reports back, what the model is offered and what the prompt tells it.
 * All data is made up.
 */
class AgentEvaluationSuite :
    BehaviorSpec({
        val bank = WalletFactory.create(id = 1, name = "Bank")
        val groceries = CategoryFactory.create(id = 3, name = "Groceries")
        val card = CreditCardFactory.create(id = 7, name = "Card")

        val walletTransactionRepository = mockk<WalletTransactionRepository>()
        val transferRepository = mockk<TransferRepository>()
        val creditCardDebtRepository = mockk<CreditCardDebtRepository>()
        val walletService = mockk<WalletService>()
        val creditCardService = mockk<CreditCardService>()
        val categoryService = mockk<CategoryService>()
        val queryFinanceTool = mockk<QueryFinanceTool>()
        val tickerService = mockk<TickerService>()
        val bondService = mockk<BondService>()
        val categoryResolver = mockk<CategoryResolver>()
        val llmProviderFactory = mockk<LlmProviderFactory>()
        val clockProvider = mockk<ClockProvider>()
        val preferencesService = mockk<PreferencesService>()

        every { preferencesService.translate(any()) } answers { firstArg() }
        every { preferencesService.locale } returns Locale.ENGLISH
        every { clockProvider.today() } returns LocalDate.of(2026, 10, 20)
        every { categoryService.getNonArchivedCategoriesOrderedByName() } returns listOf(groceries)
        every { walletService.getAllNonArchivedWalletsOrderedByName() } returns listOf(bank)
        every { creditCardService.getAllNonArchivedCreditCardsOrderedByName() } returns listOf(card)
        every { queryFinanceTool.availableTools() } returns emptyList()
        every { tickerService.getAllNonArchivedTickers() } returns emptyList()
        every { bondService.getAllNonArchivedBonds() } returns emptyList()
        every { transferRepository.findBetweenDates(any(), any()) } returns emptyList()

        val matching =
            TransactionMatchingService(
                walletTransactionRepository,
                transferRepository,
                creditCardDebtRepository,
                preferencesService,
            )
        val registry =
            OperationRegistry(
                listOf(
                    CreateWalletTransactionOperation(walletService, categoryResolver, preferencesService),
                    CreateCreditCardDebtOperation(creditCardService, categoryResolver, preferencesService),
                    PayCreditCardInvoiceOperation(creditCardService, preferencesService),
                    RecordStockPurchaseOperation(tickerService, walletService, categoryResolver, preferencesService),
                ),
            )
        val catalog =
            EntityCatalogProvider(walletService, creditCardService, tickerService, bondService, categoryService)
        val proposeTool = ProposeChangesTool(registry, catalog, matching, preferencesService)
        val findTool = FindMatchingTransactionsTool(matching, walletService, creditCardService, preferencesService)

        fun stored(
            transactions: List<WalletTransaction> = emptyList(),
            debts: List<CreditCardDebt> = emptyList(),
        ) {
            every { walletTransactionRepository.findTransactionsBetweenDates(any(), any()) } returns transactions
            every { creditCardDebtRepository.findByDateBetween(any(), any()) } returns debts
        }

        fun gym(
            id: Int,
            status: WalletTransactionStatus,
            date: LocalDate = LocalDate.of(2026, 10, 11),
        ) = WalletTransactionFactory.create(
            id = id,
            amount = BigDecimal("99.90"),
            date = date.atTime(12, 0),
            description = "Academia",
            status = status,
            wallet = bank,
        )

        fun run(
            toolName: String,
            arguments: String,
        ): Pair<ChatTurnResult, ScriptedLlmProvider> {
            val provider =
                ScriptedLlmProvider(
                    listOf(
                        LlmResponse(text = null, toolCalls = listOf(ToolCall("call_1", toolName, arguments))),
                        LlmResponse(text = "done"),
                    ),
                )
            every { llmProviderFactory.currentProvider() } returns provider
            val service =
                AiChatService(
                    llmProviderFactory,
                    queryFinanceTool,
                    findTool,
                    proposeTool,
                    AiPromptProperties(),
                    PromptComposer(),
                    preferencesService,
                    clockProvider,
                )
            val result =
                kotlinx.coroutines.runBlocking {
                    service.sendMessage(listOf(ChatMessage(ChatRole.USER, "extrato")))
                }
            return result to provider
        }

        val gymLine =
            """{"changes":[{"operation":"create_wallet_transaction","date":"2026-10-10","description":"PIX ACADEMIA FIT",
                "amount":99.90,"walletName":"Bank","movementType":"expense","categoryName":"Groceries","confidence":0.9}]}"""

        Given("case 1: the user already recorded the gym under another name and it is confirmed") {
            stored(transactions = listOf(gym(198, WalletTransactionStatus.CONFIRMED)))

            When("the model proposes the statement line") {
                val (result, provider) = run(ProposeChangesTool.NAME, gymLine)

                Then("nothing is proposed, the line is reported as ignored with the record id") {
                    val success = result.shouldBeInstanceOf<ChatTurnResult.Success>()
                    success.changeSet!!.changes.size shouldBe 0
                    success.changeSet!!.ignored.map { it.targetId } shouldBe listOf(198)
                }

                Then("the model also learns which line was ignored") {
                    provider.receivedMessages
                        .last()
                        .last()
                        .content shouldContain "\"ignored\""
                }
            }
        }

        Given("case 2: the same gym is still pending") {
            stored(transactions = listOf(gym(214, WalletTransactionStatus.PENDING)))
            every { walletService.getWalletTransactionById(214) } returns gym(214, WalletTransactionStatus.PENDING)

            When("the model proposes the statement line") {
                val (result, _) = run(ProposeChangesTool.NAME, gymLine)

                Then("the pending one is confirmed with its own name, no duplicate is created") {
                    val success = result.shouldBeInstanceOf<ChatTurnResult.Success>()
                    val payload =
                        success.changeSet!!
                            .changes
                            .single()
                            .payload as WalletTransactionPayload
                    payload.existingTransactionId shouldBe 214
                    payload.description shouldBe "Academia"
                    success.changeSet!!.ignored.size shouldBe 0
                }
            }
        }

        Given("case 3: an installment line whose purchase is already on the card") {
            stored(
                debts =
                    listOf(
                        CreditCardDebtFactory.create(
                            id = 31,
                            creditCard = card,
                            installments = 10,
                            amount = BigDecimal("1000.00"),
                            date = LocalDateTime.of(2026, 9, 5, 15, 0),
                            description = "Notebook",
                        ),
                    ),
            )
            val line =
                """{"changes":[{"operation":"create_credit_card_debt","date":"2026-09-05",
                    "description":"NOTEBOOK LOJA 3/10","amount":100.00,"creditCardName":"Card","installments":10,
                    "installmentNumber":3,"categoryName":"Groceries"}]}"""

            When("the model proposes it") {
                val (result, _) = run(ProposeChangesTool.NAME, line)

                Then("it is ignored as an installment of the recorded purchase") {
                    val success = result.shouldBeInstanceOf<ChatTurnResult.Success>()
                    success.changeSet!!.changes.size shouldBe 0
                    success.changeSet!!
                        .ignored
                        .single()
                        .targetType shouldBe MatchTargetType.CREDIT_CARD_DEBT
                }
            }
        }

        Given("case 4: a card purchase that is not recorded yet") {
            stored()
            val line =
                """{"changes":[{"operation":"create_credit_card_debt","date":"2026-10-12","description":"Shoes",
                    "amount":120.00,"creditCardName":"Card","installments":3,"installmentNumber":1,
                    "categoryName":"Groceries"}]}"""

            When("the model proposes it") {
                val (result, _) = run(ProposeChangesTool.NAME, line)

                Then("it becomes a proposal for the whole purchase") {
                    val payload =
                        (result as ChatTurnResult.Success)
                            .changeSet!!
                            .changes
                            .single()
                            .payload
                    payload.shouldBeInstanceOf<CreditCardDebtPayload>()
                    payload.amount.compareTo(BigDecimal("360.00")) shouldBe 0
                    payload.installments shouldBe 3
                }
            }
        }

        Given("case 5: the user explicitly asks to record the ignored line anyway") {
            stored(transactions = listOf(gym(198, WalletTransactionStatus.CONFIRMED)))
            val forced = gymLine.replace("\"confidence\":0.9", "\"confidence\":0.9,\"allowDuplicate\":true")

            When("the model resends it with allowDuplicate") {
                val (result, _) = run(ProposeChangesTool.NAME, forced)

                Then("it is proposed as a possible duplicate, never silently") {
                    val success = result.shouldBeInstanceOf<ChatTurnResult.Success>()
                    success.changeSet!!
                        .changes
                        .single()
                        .badge shouldBe ChangeBadge.POSSIBLE_DUPLICATE
                    success.changeSet!!.ignored.size shouldBe 0
                }
            }
        }

        Given("case 6: a card invoice payment line") {
            stored(transactions = listOf(gym(198, WalletTransactionStatus.CONFIRMED)))
            val line =
                """{"changes":[{"operation":"pay_credit_card_invoice","date":"2026-10-10","description":"Fatura",
                    "amount":99.90,"creditCardName":"Card","categoryName":"Groceries"}]}"""

            When("the model proposes it") {
                val (result, _) = run(ProposeChangesTool.NAME, line)

                Then("it is never mistaken for a stored transaction") {
                    val success = result.shouldBeInstanceOf<ChatTurnResult.Success>()
                    success.changeSet!!
                        .changes
                        .single()
                        .operationName shouldBe "pay_credit_card_invoice"
                    success.changeSet!!.ignored.size shouldBe 0
                }
            }
        }

        Given("case 7: the user asks whether something was already recorded") {
            stored(transactions = listOf(gym(198, WalletTransactionStatus.CONFIRMED)))

            When("the model calls the lookup tool") {
                val (_, provider) =
                    run(
                        FindMatchingTransactionsTool.NAME,
                        """{"amount":99.90,"date":"2026-10-10","walletName":"Bank","description":"PIX ACADEMIA FIT"}""",
                    )

                Then("the tool answer carries the stored record with its reasons") {
                    val answer =
                        provider.receivedMessages
                            .last()
                            .last()
                            .content
                    answer shouldContain "\"targetId\":198"
                    answer shouldContain "aimatch.reason.exactAmount"
                }
            }
        }

        Given("case 8: the user asks to delete something") {
            stored()

            When("any turn runs") {
                val (_, provider) = run(ProposeChangesTool.NAME, gymLine)

                Then("the model is not offered any tool that deletes or removes") {
                    val names = provider.offeredTools.flatten().map { it.name }
                    names shouldContain FindMatchingTransactionsTool.NAME
                    names.none { name ->
                        listOf("delete", "remove", "drop", "purge").any { name.contains(it) }
                    } shouldBe
                        true
                }

                Then("the system prompt says the assistant cannot write on its own") {
                    provider.receivedMessages
                        .first()
                        .first()
                        .content shouldContain "no tool that writes directly"
                }
            }
        }

        Given("case 9: the model is offered the matching tool in every turn") {
            stored()

            When("a turn runs") {
                val (_, provider) = run(ProposeChangesTool.NAME, gymLine)

                Then("the lookup and the proposal tools are both present") {
                    val names = provider.offeredTools.first().map { it.name }
                    names shouldContain ProposeChangesTool.NAME
                    names shouldNotContain "create_wallet_transaction"
                }
            }
        }
    })
