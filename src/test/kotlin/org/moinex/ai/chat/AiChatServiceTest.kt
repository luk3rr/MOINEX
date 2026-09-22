package org.moinex.ai.chat

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.mockk.clearAllMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import org.moinex.ai.provider.LlmProvider
import org.moinex.ai.provider.LlmProviderFactory
import org.moinex.ai.tools.LlmTool
import org.moinex.ai.tools.ProposeTransactionTool
import org.moinex.ai.tools.QueryFinanceTool
import org.moinex.config.AiPromptProperties
import org.moinex.service.PreferencesService
import java.util.Locale

class AiChatServiceTest :
    BehaviorSpec({
        val llmProviderFactory = mockk<LlmProviderFactory>()
        val queryFinanceTool = mockk<QueryFinanceTool>()
        val proposeTransactionTool = mockk<ProposeTransactionTool>()
        val provider = mockk<LlmProvider>()
        val preferencesService = mockk<PreferencesService>(relaxed = true)
        val aiPromptProperties =
            AiPromptProperties(
                systemPrompt = mapOf("en" to "You are a test assistant."),
                attachmentInstructionTemplate = "File \"{0}\" | context: {1} | content: {2}",
            )

        val fakeTool =
            object : LlmTool {
                override val name = "get_net_worth"
                override val description = "Returns net worth"
                override val jsonSchema = """{"type":"object","properties":{}}"""

                override fun execute(argumentsJson: String): String = """{"netWorth":1000}"""
            }

        every { proposeTransactionTool.name } returns ProposeTransactionTool.NAME

        val multiLocalePrompts =
            AiPromptProperties(
                systemPrompt =
                    mapOf(
                        "en" to "ENGLISH_PROMPT",
                        "pt-br" to "PORTUGUESE_PROMPT",
                    ),
            )
        val serviceWithMultiLocalePrompts =
            AiChatService(
                llmProviderFactory,
                queryFinanceTool,
                proposeTransactionTool,
                multiLocalePrompts,
                preferencesService,
            )

        val service =
            AiChatService(
                llmProviderFactory,
                queryFinanceTool,
                proposeTransactionTool,
                aiPromptProperties,
                preferencesService,
            )

        afterContainer { clearAllMocks() }

        Given("the UI language is set to pt-BR") {
            every { llmProviderFactory.currentProvider() } returns provider
            every { preferencesService.locale } returns Locale.forLanguageTag("pt-BR")
            every { queryFinanceTool.availableTools() } returns listOf(fakeTool)
            coEvery { provider.chat(any(), any()) } returns LlmResponse(text = "ok")

            When("sending a message") {
                serviceWithMultiLocalePrompts.sendMessage(listOf(ChatMessage(role = ChatRole.USER, content = "Oi")))

                Then("the Portuguese system prompt is sent to the provider") {
                    coVerify {
                        provider.chat(
                            match { messages -> messages.first().content == "PORTUGUESE_PROMPT" },
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
            coEvery { provider.chat(any(), any()) } returns LlmResponse(text = "ok")

            When("sending a message") {
                serviceWithMultiLocalePrompts.sendMessage(listOf(ChatMessage(role = ChatRole.USER, content = "Hi")))

                Then("it falls back to the English prompt") {
                    coVerify {
                        provider.chat(
                            match { messages -> messages.first().content == "ENGLISH_PROMPT" },
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
            }
        }

        Given("a configured provider that requests a tool call before answering") {
            every { llmProviderFactory.currentProvider() } returns provider
            every { preferencesService.locale } returns Locale.ENGLISH
            every { queryFinanceTool.availableTools() } returns listOf(fakeTool)

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
            }
        }

        Given("a provider call that throws") {
            every { llmProviderFactory.currentProvider() } returns provider
            every { preferencesService.locale } returns Locale.ENGLISH
            every { queryFinanceTool.availableTools() } returns listOf(fakeTool)
            coEvery { provider.chat(any(), any()) } throws RuntimeException("network unreachable")

            When("sending a message") {
                val result = service.sendMessage(listOf(ChatMessage(role = ChatRole.USER, content = "Oi")))

                Then("it returns an Error result carrying the failure message, instead of throwing") {
                    result.shouldBeInstanceOf<ChatTurnResult.Error>()
                    (result as ChatTurnResult.Error).message shouldBe "network unreachable"
                }
            }
        }

        Given("a configured provider that calls propose_transactions before answering") {
            every { llmProviderFactory.currentProvider() } returns provider
            every { preferencesService.locale } returns Locale.ENGLISH
            every { queryFinanceTool.availableTools() } returns listOf(fakeTool)
            every { proposeTransactionTool.name } returns ProposeTransactionTool.NAME

            val toolCall = ToolCall(id = "call_1", toolName = ProposeTransactionTool.NAME, argumentsJson = "{}")
            val proposedJson =
                """
                [{"date":"2026-09-05","description":"Market","amount":150.30,"kind":"wallet",
                  "suggestedWalletId":null,"suggestedCreditCardId":null,"suggestedCategoryId":null,
                  "suggestedCategoryName":"Groceries","confidence":0.8}]
                """.trimIndent()
            every { proposeTransactionTool.execute(any()) } returns proposedJson

            coEvery { provider.chat(any(), any()) } returnsMany
                listOf(
                    LlmResponse(text = null, toolCalls = listOf(toolCall)),
                    LlmResponse(text = "Found 1 transaction, please review."),
                )

            When("sending a message") {
                val result =
                    service.sendMessage(listOf(ChatMessage(role = ChatRole.USER, content = "Here's my statement")))

                Then("it returns TransactionsProposed with the parsed list instead of Success") {
                    result.shouldBeInstanceOf<ChatTurnResult.TransactionsProposed>()
                    val proposed = result as ChatTurnResult.TransactionsProposed
                    proposed.transactions.size shouldBe 1
                    proposed.transactions[0].suggestedCategoryName shouldBe "Groceries"
                    proposed.reply shouldBe "Found 1 transaction, please review."
                }
            }
        }

        Given("a single attached file's extracted text") {
            When("building the attachment message with no user note") {
                every { proposeTransactionTool.describeAvailableContext() } returns "Existing categories: Groceries"
                val message =
                    service.buildAttachmentMessage(
                        listOf(ChatAttachment("extrato.csv", "date,amount\n2026-09-05,150.30")),
                    )

                Then("the configured template is filled in with the file name, context and content") {
                    message shouldBe
                        "File \"extrato.csv\" | context: Existing categories: Groceries | " +
                        "content: File \"extrato.csv\":\n\ndate,amount\n2026-09-05,150.30"
                }
            }

            When("building the attachment message with a user note") {
                every { proposeTransactionTool.describeAvailableContext() } returns "Existing categories: Groceries"
                val message =
                    service.buildAttachmentMessage(
                        listOf(ChatAttachment("extrato.csv", "date,amount\n2026-09-05,150.30")),
                        userNote = "These are my September expenses",
                    )

                Then("the note is prepended before the instruction template") {
                    message shouldBe
                        "These are my September expenses\n\n" +
                        "File \"extrato.csv\" | context: Existing categories: Groceries | " +
                        "content: File \"extrato.csv\":\n\ndate,amount\n2026-09-05,150.30"
                }
            }
        }

        Given("multiple attached files' extracted text") {
            When("building the attachment message") {
                every { proposeTransactionTool.describeAvailableContext() } returns "Existing categories: Groceries"
                val message =
                    service.buildAttachmentMessage(
                        listOf(
                            ChatAttachment("extrato.csv", "date,amount\n2026-09-05,150.30"),
                            ChatAttachment("fatura.pdf", "Nubank invoice - September"),
                        ),
                    )

                Then("all file names are listed and each file's content keeps its own header") {
                    message shouldBe
                        "File \"extrato.csv, fatura.pdf\" | context: Existing categories: Groceries | " +
                        "content: File \"extrato.csv\":\n\ndate,amount\n2026-09-05,150.30\n\n\n" +
                        "File \"fatura.pdf\":\n\nNubank invoice - September"
                }
            }
        }

        Given("a provider that keeps requesting tool calls forever") {
            every { llmProviderFactory.currentProvider() } returns provider
            every { preferencesService.locale } returns Locale.ENGLISH
            every { queryFinanceTool.availableTools() } returns listOf(fakeTool)

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
    })
