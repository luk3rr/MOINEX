package org.moinex.ai.chat

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.types.shouldBeInstanceOf
import io.mockk.clearAllMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import org.moinex.ai.operation.ChangeBadge
import org.moinex.ai.operation.ChangeSet
import org.moinex.ai.operation.ProposedChange
import org.moinex.ai.prompt.PromptComposer
import org.moinex.ai.provider.LlmProvider
import org.moinex.ai.provider.LlmProviderFactory
import org.moinex.ai.tools.FindMatchingTransactionsTool
import org.moinex.ai.tools.LlmTool
import org.moinex.ai.tools.ProposeChangesTool
import org.moinex.ai.tools.QueryFinanceTool
import org.moinex.ai.tools.ReadTools
import org.moinex.common.ClockProvider
import org.moinex.config.AiPromptProperties
import org.moinex.model.dto.ai.IgnoredLine
import org.moinex.service.PreferencesService
import org.moinex.service.matching.MatchTargetType
import java.time.LocalDate
import java.util.Locale

class AiChatServiceTest :
    BehaviorSpec({
        val llmProviderFactory = mockk<LlmProviderFactory>()
        val queryFinanceTool = mockk<QueryFinanceTool>()
        val findMatchingTransactionsTool = mockk<FindMatchingTransactionsTool>()
        val proposeChangesTool = mockk<ProposeChangesTool>()
        val readTools = mockk<ReadTools>()
        val clockProvider = mockk<ClockProvider>()
        val provider = mockk<LlmProvider>()
        val preferencesService = mockk<PreferencesService>(relaxed = true)
        val aiPromptProperties =
            AiPromptProperties(
                attachmentInstructionTemplate = "File \"{0}\" | content: {1}",
            )

        val fakeTool =
            object : LlmTool {
                override val name = "get_net_worth"
                override val description = "Returns net worth"
                override val jsonSchema = """{"type":"object","properties":{}}"""

                override fun execute(argumentsJson: String): String = """{"netWorth":1000}"""
            }

        every { proposeChangesTool.name } returns ProposeChangesTool.NAME

        /** Stubs the calls [AiChatService] makes on every turn to build the system message. */
        fun stubTurnContext() {
            every { readTools.tools() } returns emptyList()
            every { proposeChangesTool.describeAvailableContext() } returns
                "Existing categories: Groceries\nExisting tickers: PETR4"
            every { clockProvider.today() } returns LocalDate.of(2026, 9, 21)
            every { findMatchingTransactionsTool.name } returns FindMatchingTransactionsTool.NAME
        }

        val service =
            AiChatService(
                llmProviderFactory,
                queryFinanceTool,
                findMatchingTransactionsTool,
                readTools,
                proposeChangesTool,
                aiPromptProperties,
                PromptComposer(),
                preferencesService,
                clockProvider,
            )

        afterContainer { clearAllMocks() }

        Given("the UI language is set to pt-BR") {
            every { llmProviderFactory.currentProvider() } returns provider
            every { preferencesService.locale } returns Locale.forLanguageTag("pt-BR")
            every { queryFinanceTool.availableTools() } returns listOf(fakeTool)
            stubTurnContext()
            coEvery { provider.chat(any(), any()) } returns LlmResponse(text = "ok")

            When("sending a message") {
                service.sendMessage(listOf(ChatMessage(role = ChatRole.USER, content = "Oi")))

                Then("the Portuguese system prompt is sent to the provider") {
                    coVerify {
                        provider.chat(
                            match { messages ->
                                messages.first().content.startsWith("Você é o assistente financeiro do MOINEX")
                            },
                            any(),
                        )
                    }
                }
            }
        }

        Given("the UI language has no matching prompt entry") {
            every { llmProviderFactory.currentProvider() } returns provider
            every { preferencesService.locale } returns Locale.forLanguageTag("fr")
            every { queryFinanceTool.availableTools() } returns listOf(fakeTool)
            stubTurnContext()
            coEvery { provider.chat(any(), any()) } returns LlmResponse(text = "ok")

            When("sending a message") {
                service.sendMessage(listOf(ChatMessage(role = ChatRole.USER, content = "Hi")))

                Then("it falls back to the English prompt") {
                    coVerify {
                        provider.chat(
                            match { messages ->
                                messages.first().content.startsWith("You are the MOINEX financial assistant")
                            },
                            any(),
                        )
                    }
                }
            }
        }

        Given("no AI provider is configured") {
            every { llmProviderFactory.currentProvider() } returns null

            When("sending a message") {
                val result = service.sendMessage(listOf(ChatMessage(role = ChatRole.USER, content = "Oi")))

                Then("it returns NoProviderConfigured without contacting any provider") {
                    result.shouldBeInstanceOf<ChatTurnResult.NoProviderConfigured>()
                }
            }
        }

        Given("a configured provider that answers directly, with no tool calls") {
            every { llmProviderFactory.currentProvider() } returns provider
            every { preferencesService.locale } returns Locale.ENGLISH
            every { queryFinanceTool.availableTools() } returns listOf(fakeTool)
            stubTurnContext()
            coEvery { provider.chat(any(), any()) } returns LlmResponse(text = "Você gastou R$ 100 esse mês.")

            When("sending a message") {
                val result = service.sendMessage(listOf(ChatMessage(role = ChatRole.USER, content = "Quanto gastei?")))

                Then("it returns the model's answer") {
                    result.shouldBeInstanceOf<ChatTurnResult.Success>()
                    (result as ChatTurnResult.Success).reply shouldBe "Você gastou R$ 100 esse mês."
                }

                Then("the provider is called exactly once") {
                    coVerify(exactly = 1) { provider.chat(any(), any()) }
                }

                Then("the turn messages hold only the final answer") {
                    val success = result as ChatTurnResult.Success
                    success.turnMessages.map { it.role } shouldBe listOf(ChatRole.ASSISTANT)
                    success.turnMessages.single().content shouldBe "Você gastou R$ 100 esse mês."
                }

                Then("the system message carries today's date and the existing categories/tickers context") {
                    coVerify {
                        provider.chat(
                            match { messages ->
                                messages.first().content.let {
                                    it.contains("2026-09-21") &&
                                        it.contains("Existing categories: Groceries") &&
                                        it.contains("Existing tickers: PETR4")
                                }
                            },
                            any(),
                        )
                    }
                }
            }
        }

        Given("a configured provider that requests a tool call before answering") {
            every { llmProviderFactory.currentProvider() } returns provider
            every { preferencesService.locale } returns Locale.ENGLISH
            every { queryFinanceTool.availableTools() } returns listOf(fakeTool)
            stubTurnContext()

            val toolCall = ToolCall(id = "call_1", toolName = "get_net_worth", argumentsJson = "{}")
            coEvery { provider.chat(any(), any()) } returnsMany
                listOf(
                    LlmResponse(text = null, toolCalls = listOf(toolCall)),
                    LlmResponse(text = "Seu patrimônio é R$ 1.000."),
                )

            When("sending a message") {
                val result =
                    service.sendMessage(
                        listOf(ChatMessage(role = ChatRole.USER, content = "Qual meu patrimônio?")),
                    )

                Then("it executes the tool locally and returns the model's final answer") {
                    result.shouldBeInstanceOf<ChatTurnResult.Success>()
                    (result as ChatTurnResult.Success).reply shouldBe "Seu patrimônio é R$ 1.000."
                }

                Then("the provider is called twice: once to request the tool, once with the tool result") {
                    coVerify(exactly = 2) { provider.chat(any(), any()) }
                }

                Then("the turn messages carry the tool request, the tool result and the final answer, in order") {
                    val turn = (result as ChatTurnResult.Success).turnMessages
                    turn.map { it.role } shouldBe listOf(ChatRole.ASSISTANT, ChatRole.TOOL, ChatRole.ASSISTANT)
                    turn[0].toolCalls shouldBe listOf(toolCall)
                    turn[1].toolCallId shouldBe "call_1"
                    turn[1].content shouldBe """{"netWorth":1000}"""
                    turn[2].content shouldBe "Seu patrimônio é R$ 1.000."
                }
            }
        }

        Given("a provider call that throws") {
            every { llmProviderFactory.currentProvider() } returns provider
            every { preferencesService.locale } returns Locale.ENGLISH
            every { queryFinanceTool.availableTools() } returns listOf(fakeTool)
            stubTurnContext()
            coEvery { provider.chat(any(), any()) } throws RuntimeException("network unreachable")

            When("sending a message") {
                val result = service.sendMessage(listOf(ChatMessage(role = ChatRole.USER, content = "Oi")))

                Then("it returns an Error result carrying the failure message, instead of throwing") {
                    result.shouldBeInstanceOf<ChatTurnResult.Error>()
                    (result as ChatTurnResult.Error).message shouldBe "network unreachable"
                }
            }
        }

        fun change(summary: String) =
            ProposedChange(
                id = summary,
                operationName = "create_wallet_transaction",
                payload = summary,
                summary = summary,
                badge = ChangeBadge.NEW,
                reasons = emptyList(),
                confidence = 0.9,
            )

        fun outcome(
            changes: List<ProposedChange> = emptyList(),
            ignored: List<IgnoredLine> = emptyList(),
        ) = ProposeChangesTool.Outcome(ChangeSet("set", changes, ignored), emptyList())

        Given("a provider that calls propose_changes before answering") {
            every { llmProviderFactory.currentProvider() } returns provider
            every { preferencesService.locale } returns Locale.ENGLISH
            every { queryFinanceTool.availableTools() } returns listOf(fakeTool)
            stubTurnContext()

            val toolCall = ToolCall(id = "call_1", toolName = ProposeChangesTool.NAME, argumentsJson = "{}")
            val proposed = outcome(listOf(change("Market")))
            every { proposeChangesTool.propose(any()) } returns proposed
            every { proposeChangesTool.toModelJson(proposed) } returns """{"accepted":[{"summary":"Market"}]}"""

            coEvery { provider.chat(any(), any()) } returnsMany
                listOf(
                    LlmResponse(text = null, toolCalls = listOf(toolCall)),
                    LlmResponse(text = "Found 1 change, please review."),
                )

            When("sending a message") {
                val result =
                    service.sendMessage(listOf(ChatMessage(role = ChatRole.USER, content = "Here's my statement")))

                Then("the result carries the change set and the tool result goes back to the model") {
                    val success = result.shouldBeInstanceOf<ChatTurnResult.Success>()
                    success.changeSet!!.changes.map { it.summary } shouldBe listOf("Market")
                    success.reply shouldBe "Found 1 change, please review."
                    coVerify {
                        provider.chat(
                            match { messages -> messages.last().content.contains("\"accepted\"") },
                            any(),
                        )
                    }
                }
            }
        }

        Given("a propose_changes result that skipped a line already recorded") {
            every { llmProviderFactory.currentProvider() } returns provider
            every { preferencesService.locale } returns Locale.ENGLISH
            every { queryFinanceTool.availableTools() } returns listOf(fakeTool)
            stubTurnContext()

            val toolCall = ToolCall(id = "call_1", toolName = ProposeChangesTool.NAME, argumentsJson = "{}")
            val proposed =
                outcome(
                    ignored =
                        listOf(
                            IgnoredLine(
                                "Taxi, 23.50, 2026-10-09 -> already recorded",
                                MatchTargetType.WALLET_TRANSACTION,
                                198,
                                listOf("same amount", "same date"),
                            ),
                        ),
                )
            every { proposeChangesTool.propose(any()) } returns proposed
            every { proposeChangesTool.toModelJson(proposed) } returns """{"ignored":[{"targetId":198}]}"""

            coEvery { provider.chat(any(), any()) } returnsMany
                listOf(
                    LlmResponse(text = null, toolCalls = listOf(toolCall)),
                    LlmResponse(text = "Nothing new to review."),
                )

            When("sending a message") {
                val result =
                    service.sendMessage(listOf(ChatMessage(role = ChatRole.USER, content = "Here's my statement")))

                Then("the change set carries the ignored line with its record id and reasons") {
                    val success = result.shouldBeInstanceOf<ChatTurnResult.Success>()
                    success.changeSet!!.changes.size shouldBe 0
                    success.changeSet!!
                        .ignored
                        .single()
                        .targetId shouldBe 198
                    success.changeSet!!
                        .ignored
                        .single()
                        .reasons shouldBe listOf("same amount", "same date")
                }
            }
        }

        Given("a provider that calls propose_changes twice before answering") {
            every { llmProviderFactory.currentProvider() } returns provider
            every { preferencesService.locale } returns Locale.ENGLISH
            every { queryFinanceTool.availableTools() } returns listOf(fakeTool)
            stubTurnContext()

            val first = outcome(listOf(change("Market")))
            val second = outcome(listOf(change("Pharmacy")))
            every { proposeChangesTool.propose("1") } returns first
            every { proposeChangesTool.propose("2") } returns second
            every { proposeChangesTool.toModelJson(any()) } returns "{}"

            coEvery { provider.chat(any(), any()) } returnsMany
                listOf(
                    LlmResponse(text = null, toolCalls = listOf(ToolCall("c1", ProposeChangesTool.NAME, "1"))),
                    LlmResponse(text = null, toolCalls = listOf(ToolCall("c2", ProposeChangesTool.NAME, "2"))),
                    LlmResponse(text = "Two batches."),
                )

            When("sending a message") {
                val result =
                    service.sendMessage(listOf(ChatMessage(role = ChatRole.USER, content = "Here's my statement")))

                Then("both batches are accumulated instead of the second overwriting the first") {
                    val success = result.shouldBeInstanceOf<ChatTurnResult.Success>()
                    success.changeSet!!.changes.map { it.summary } shouldBe listOf("Market", "Pharmacy")
                }
            }
        }

        Given("a propose_changes call that throws") {
            every { llmProviderFactory.currentProvider() } returns provider
            every { preferencesService.locale } returns Locale.ENGLISH
            every { queryFinanceTool.availableTools() } returns listOf(fakeTool)
            stubTurnContext()
            every { proposeChangesTool.propose(any()) } throws IllegalStateException("boom")

            coEvery { provider.chat(any(), any()) } returnsMany
                listOf(
                    LlmResponse(text = null, toolCalls = listOf(ToolCall("c1", ProposeChangesTool.NAME, "{}"))),
                    LlmResponse(text = "Sorry."),
                )

            When("sending a message") {
                val result =
                    service.sendMessage(listOf(ChatMessage(role = ChatRole.USER, content = "Here's my statement")))

                Then("the model receives the error and the turn still ends with its answer") {
                    val success = result.shouldBeInstanceOf<ChatTurnResult.Success>()
                    success.changeSet shouldBe null
                    success.reply shouldBe "Sorry."
                }
            }
        }

        Given("a single attached file's extracted text") {
            When("building the attachment message with no user note") {
                val message =
                    service.buildAttachmentMessage(
                        listOf(ChatAttachment("extrato.csv", "date,amount\n2026-09-05,150.30")),
                    )

                Then("the configured template is filled in with the file name and content") {
                    message shouldBe
                        "File \"extrato.csv\" | content: File \"extrato.csv\":\n\ndate,amount\n2026-09-05,150.30"
                }
            }

            When("building the attachment message with a user note") {
                val message =
                    service.buildAttachmentMessage(
                        listOf(ChatAttachment("extrato.csv", "date,amount\n2026-09-05,150.30")),
                        userNote = "These are my September expenses",
                    )

                Then("the note is prepended before the instruction template") {
                    message shouldBe
                        "These are my September expenses\n\n" +
                        "File \"extrato.csv\" | content: File \"extrato.csv\":\n\ndate,amount\n2026-09-05,150.30"
                }
            }
        }

        Given("multiple attached files' extracted text") {
            When("building the attachment message") {
                val message =
                    service.buildAttachmentMessage(
                        listOf(
                            ChatAttachment("extrato.csv", "date,amount\n2026-09-05,150.30"),
                            ChatAttachment("fatura.pdf", "Nubank invoice - September"),
                        ),
                    )

                Then("all file names are listed and each file's content keeps its own header") {
                    message shouldContain "File \"extrato.csv, fatura.pdf\""
                    message shouldContain "File \"extrato.csv\":\n\ndate,amount\n2026-09-05,150.30"
                    message shouldContain "File \"fatura.pdf\":\n\nNubank invoice - September"
                }
            }
        }

        Given("a provider that keeps requesting tool calls forever") {
            every { llmProviderFactory.currentProvider() } returns provider
            every { preferencesService.locale } returns Locale.ENGLISH
            every { queryFinanceTool.availableTools() } returns listOf(fakeTool)
            stubTurnContext()

            val toolCall = ToolCall(id = "call_1", toolName = "get_net_worth", argumentsJson = "{}")
            coEvery { provider.chat(any(), any()) } returns LlmResponse(text = null, toolCalls = listOf(toolCall))

            When("sending a message") {
                val result =
                    service.sendMessage(
                        listOf(ChatMessage(role = ChatRole.USER, content = "Qual meu patrimônio?")),
                    )

                Then("it gives up with an Error instead of looping forever") {
                    result.shouldBeInstanceOf<ChatTurnResult.Error>()
                }
            }
        }

        Given("a provider that keeps calling tools after it already proposed changes") {
            every { llmProviderFactory.currentProvider() } returns provider
            every { preferencesService.locale } returns Locale.ENGLISH
            every { queryFinanceTool.availableTools() } returns listOf(fakeTool)
            stubTurnContext()

            val proposed = outcome(listOf(change("Market")))
            every { proposeChangesTool.propose(any()) } returns proposed
            every { proposeChangesTool.toModelJson(any()) } returns "{}"
            coEvery { provider.chat(any(), any()) } returns
                LlmResponse(text = null, toolCalls = listOf(ToolCall("c", ProposeChangesTool.NAME, "{}")))

            When("sending a message") {
                val result =
                    service.sendMessage(listOf(ChatMessage(role = ChatRole.USER, content = "Here's my statement")))

                Then("the limit does not throw away what was already proposed") {
                    val success = result.shouldBeInstanceOf<ChatTurnResult.Success>()
                    success.changeSet!!.changes.map { it.summary } shouldBe listOf("Market")
                }
            }
        }
    })
