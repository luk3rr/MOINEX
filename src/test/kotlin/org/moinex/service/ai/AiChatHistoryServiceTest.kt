/*
 * Filename: AiChatHistoryServiceTest.kt
 * Created on: November 6, 2026
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 */

package org.moinex.service.ai

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.IsolationMode
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldNotContain
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import io.mockk.verifyOrder
import org.moinex.ai.chat.ChatMessage
import org.moinex.ai.chat.ChatRole
import org.moinex.ai.chat.ToolCall
import org.moinex.ai.history.ChatPayloadCodec
import org.moinex.ai.history.RawPayload
import org.moinex.ai.operation.ChangeBadge
import org.moinex.ai.operation.ChangeSet
import org.moinex.ai.operation.ChangeState
import org.moinex.ai.operation.OperationRegistry
import org.moinex.ai.operation.ProposedChange
import org.moinex.ai.operation.transactions.CreateWalletTransactionOperation
import org.moinex.ai.operation.transactions.WalletTransactionPayload
import org.moinex.common.ClockProvider
import org.moinex.common.constant.TranslationKeys
import org.moinex.exception.MoinexException
import org.moinex.model.ai.AiChat
import org.moinex.model.ai.AiChatChange
import org.moinex.model.ai.AiChatChangeSet
import org.moinex.model.ai.AiChatMessage
import org.moinex.model.enums.WalletTransactionType
import org.moinex.repository.ai.AiChatChangeRepository
import org.moinex.repository.ai.AiChatChangeSetRepository
import org.moinex.repository.ai.AiChatMessageRepository
import org.moinex.repository.ai.AiChatRepository
import org.moinex.service.PreferencesService
import org.springframework.dao.DataAccessResourceFailureException
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.Optional

class AiChatHistoryServiceTest :
    BehaviorSpec({
        isolationMode = IsolationMode.InstancePerLeaf

        val chatRepository = mockk<AiChatRepository>()
        val messageRepository = mockk<AiChatMessageRepository>()
        val changeSetRepository = mockk<AiChatChangeSetRepository>()
        val changeRepository = mockk<AiChatChangeRepository>()
        val registry = mockk<OperationRegistry>()
        val preferencesService = mockk<PreferencesService>()
        val clockProvider = mockk<ClockProvider>()
        val codec = ChatPayloadCodec(registry)
        val service =
            AiChatHistoryService(
                chatRepository,
                messageRepository,
                changeSetRepository,
                changeRepository,
                codec,
                preferencesService,
                clockProvider,
            )

        val now = LocalDateTime.of(2026, 11, 6, 12, 0)

        fun chat(
            id: Int,
            title: String = "Nova conversa",
            updatedAt: LocalDateTime = now,
        ) = AiChat(id = id, title = title, createdAt = updatedAt, updatedAt = updatedAt)

        fun stubSaveAll() {
            every { messageRepository.saveAll(any<Iterable<AiChatMessage>>()) } answers {
                firstArg<Iterable<AiChatMessage>>().toList().onEachIndexed { index, row -> row.id = 100 + index }
            }
            every { chatRepository.save(any<AiChat>()) } answers { firstArg() }
        }

        every { clockProvider.now() } returns now
        every { registry.find("create_wallet_transaction") } returns mockk<CreateWalletTransactionOperation>()
        every { registry.find("operation_from_the_future") } returns null
        every { preferencesService.translate(any()) } answers { "t:${firstArg<String>()}" }

        Given("a new conversation") {
            When("createChat is called") {
                val saved = slot<AiChat>()
                every { chatRepository.save(capture(saved)) } answers { saved.captured.also { it.id = 7 } }

                val id = service.createChat()

                Then("it is stored with the provisional title and the current time") {
                    id shouldBe 7
                    saved.captured.title shouldBe "t:${TranslationKeys.AI_HISTORY_DEFAULT_TITLE}"
                    saved.captured.createdAt shouldBe now
                    saved.captured.updatedAt shouldBe now
                }
            }
        }

        Given("an empty conversation") {
            val target = chat(1)
            every { chatRepository.findById(1) } returns Optional.of(target)
            every { messageRepository.findLastPosition(1) } returns -1
            stubSaveAll()

            When("a long first user message with attachments is appended with its answer") {
                val longText =
                    "Lança esse extrato do cartão de outubro, por favor, com todas as parcelas e categorias certas"
                val rows = slot<Iterable<AiChatMessage>>()
                every { messageRepository.saveAll(capture(rows)) } answers {
                    rows.captured.toList().onEachIndexed { index, row -> row.id = 100 + index }
                }

                val ids =
                    service.appendMessages(
                        1,
                        listOf(
                            ChatMessage(ChatRole.USER, "$longText\n\nFile \"a.csv\": ...", displayText = longText),
                            ChatMessage(
                                ChatRole.ASSISTANT,
                                "",
                                toolCalls = listOf(ToolCall("c1", "get_net_worth", "{}")),
                            ),
                            ChatMessage(ChatRole.TOOL, "{}", toolCallId = "c1", toolName = "get_net_worth"),
                            ChatMessage(ChatRole.ASSISTANT, "Pronto", responseTimeMillis = 1200),
                        ),
                        attachmentNames = listOf("a.csv", "b.pdf"),
                    )

                Then("the ids come back in order") { ids shouldBe listOf(100, 101, 102, 103) }

                Then("positions start at 0 with no gaps") {
                    rows.captured.map { it.position } shouldBe listOf(0, 1, 2, 3)
                }

                Then("the conversation is named after the first line, cut at 60 characters") {
                    target.title.length shouldBe 61
                    target.title.endsWith("…") shouldBe true
                    longText.startsWith(target.title.dropLast(1)) shouldBe true
                }

                Then("the attachment names go only on the first user message") {
                    rows.captured.map { it.attachmentNames } shouldBe listOf("[\"a.csv\",\"b.pdf\"]", null, null, null)
                }

                Then("tool calls, tool ids and response time are kept") {
                    val list = rows.captured.toList()
                    list[1].toolCallsJson!!.contains("get_net_worth") shouldBe true
                    list[2].toolCallId shouldBe "c1"
                    list[2].toolName shouldBe "get_net_worth"
                    list[3].responseTimeMillis shouldBe 1200
                }

                Then("the conversation is touched") { target.updatedAt shouldBe now }
            }
        }

        Given("a conversation that already has messages") {
            val target = chat(2, title = "Meu título", updatedAt = now.minusDays(3))
            every { chatRepository.findById(2) } returns Optional.of(target)
            every { messageRepository.findLastPosition(2) } returns 5
            stubSaveAll()

            When("another user message is appended") {
                val rows = slot<Iterable<AiChatMessage>>()
                every { messageRepository.saveAll(capture(rows)) } answers {
                    rows.captured.toList().onEachIndexed { index, row -> row.id = 200 + index }
                }
                service.appendMessages(2, listOf(ChatMessage(ChatRole.USER, "outra pergunta")))

                Then("the position continues, the title stays and the last activity moves") {
                    rows.captured.single().position shouldBe 6
                    target.title shouldBe "Meu título"
                    target.updatedAt shouldBe now
                }
            }

            When("an empty list is appended") {
                val ids = service.appendMessages(2, emptyList())

                Then("nothing is stored") {
                    ids.shouldBeEmpty()
                    verify(exactly = 0) { messageRepository.saveAll(any<Iterable<AiChatMessage>>()) }
                }
            }
        }

        Given("a conversation that does not exist") {
            every { chatRepository.findById(99) } returns Optional.empty()

            When("it is opened") {
                Then("an AiChatHistoryException tells it was not found") {
                    val error = shouldThrow<MoinexException.AiChatHistoryException> { service.openChat(99) }
                    error.message shouldBe "t:${TranslationKeys.AI_HISTORY_ERROR_NOT_FOUND}"
                }
            }
        }

        Given("a database that fails while saving") {
            every { chatRepository.findById(3) } returns Optional.of(chat(3))
            every { messageRepository.findLastPosition(3) } returns -1
            every { messageRepository.saveAll(any<Iterable<AiChatMessage>>()) } throws
                DataAccessResourceFailureException("disk I/O error with secret statement text")

            When("messages are appended") {
                Then("the user gets a translated error that does not carry the conversation or the cause text") {
                    val error =
                        shouldThrow<MoinexException.AiChatHistoryException> {
                            service.appendMessages(3, listOf(ChatMessage(ChatRole.USER, "texto sigiloso")))
                        }
                    error.message shouldBe "t:${TranslationKeys.AI_HISTORY_ERROR_SAVE}"
                    error.message!! shouldNotContain "sigiloso"
                    error.message!! shouldNotContain "secret"
                }
            }
        }

        Given("a stored conversation with tool messages") {
            every { chatRepository.findById(4) } returns Optional.of(chat(4, "Contas"))
            every { messageRepository.findByChatIdOrderByPosition(4) } returns
                listOf(
                    AiChatMessage(
                        id = 1,
                        chatId = 4,
                        position = 0,
                        role = "USER",
                        content = "full text",
                        displayText = "shown",
                        createdAt = now,
                    ),
                    AiChatMessage(
                        id = 2,
                        chatId = 4,
                        position = 1,
                        role = "ASSISTANT",
                        content = "",
                        toolCallsJson = """[{"id":"c1","toolName":"get_net_worth","argumentsJson":"{}"}]""",
                        createdAt = now,
                    ),
                    AiChatMessage(
                        id = 3,
                        chatId = 4,
                        position = 2,
                        role = "TOOL",
                        content = "{}",
                        toolCallId = "c1",
                        toolName = "get_net_worth",
                        createdAt = now,
                    ),
                    AiChatMessage(
                        id = 4,
                        chatId = 4,
                        position = 3,
                        role = "ASSISTANT",
                        content = "ok",
                        responseTimeMillis = 900,
                        createdAt = now,
                    ),
                )

            every { changeSetRepository.findByMessageIdIn(any()) } returns emptyList()

            When("it is opened") {
                val opened = service.openChat(4)

                Then("every message comes back in order with what the model needs to carry on") {
                    opened.messages.map { it.role } shouldBe
                        listOf(ChatRole.USER, ChatRole.ASSISTANT, ChatRole.TOOL, ChatRole.ASSISTANT)
                    opened.messages[0].displayText shouldBe "shown"
                    opened.messages[1].toolCalls shouldBe listOf(ToolCall("c1", "get_net_worth", "{}"))
                    opened.messages[2].toolCallId shouldBe "c1"
                    opened.messages[3].responseTimeMillis shouldBe 900
                }

                Then("the summary counts only the messages the chat shows") {
                    opened.chat.messageCount shouldBe 3
                    opened.chat.title shouldBe "Contas"
                }
            }
        }

        Given("the list of conversations") {
            every { chatRepository.findAllByOrderByUpdatedAtDescIdDesc() } returns
                listOf(chat(1, "Nova"), chat(2, "Antiga", now.minusDays(1)))
            every { messageRepository.countVisibleMessagesByChat() } returns listOf(arrayOf<Any>(1, 4L))
            every { changeRepository.countOpenByChat() } returns listOf(arrayOf<Any>(2, 3))

            When("listChats is called") {
                val list = service.listChats()

                Then("it keeps the order and counts messages per conversation, zero when none") {
                    list.map { it.id } shouldBe listOf(1, 2)
                    list.map { it.messageCount } shouldBe listOf(4, 0)
                }

                Then("it counts the proposals still waiting for a decision") {
                    list.map { it.pendingProposals } shouldBe listOf(0, 3)
                }
            }
        }

        Given("a conversation to rename") {
            val target = chat(5, "Velho")
            every { chatRepository.findById(5) } returns Optional.of(target)
            every { chatRepository.save(any<AiChat>()) } answers { firstArg() }

            When("the new title has spaces around it") {
                service.renameChat(5, "  Novo título  ")

                Then("it is trimmed") { target.title shouldBe "Novo título" }
            }

            When("the new title is blank") {
                Then("it is refused and the old title stays") {
                    shouldThrow<MoinexException.AiChatHistoryException> { service.renameChat(5, "   ") }
                    target.title shouldBe "Velho"
                }
            }

            When("the new title is longer than 200 characters") {
                service.renameChat(5, "x".repeat(300))

                Then("it is cut") { target.title.length shouldBe 200 }
            }
        }

        Given("a conversation to delete") {
            every { chatRepository.deleteChangesOfChats(any()) } returns 0
            every { chatRepository.deleteChangeSetsOfChats(any()) } returns 0
            every { chatRepository.deleteMessagesOfChats(any()) } returns 0
            every { chatRepository.deleteAllById(any<Iterable<Int>>()) } returns Unit

            When("deleteChat is called") {
                service.deleteChat(8)

                Then("children go before parents, so no row is left orphaned") {
                    verifyOrder {
                        chatRepository.deleteChangesOfChats(listOf(8))
                        chatRepository.deleteChangeSetsOfChats(listOf(8))
                        chatRepository.deleteMessagesOfChats(listOf(8))
                        chatRepository.deleteAllById(listOf(8))
                    }
                }
            }

            When("deleteAllChats is called") {
                every { chatRepository.findAll() } returns listOf(chat(1), chat(2), chat(3))
                service.deleteAllChats()

                Then("every conversation is removed the same way") {
                    verify { chatRepository.deleteMessagesOfChats(listOf(1, 2, 3)) }
                    verify { chatRepository.deleteAllById(listOf(1, 2, 3)) }
                }
            }
        }

        Given("30 conversations, one per day, newest first") {
            val chats = (0 until 30).map { chat(it + 1, "c${it + 1}", now.minusDays(it.toLong())) }
            every { chatRepository.findAllByOrderByUpdatedAtDescIdDesc() } returns chats
            every { chatRepository.deleteChangesOfChats(any()) } returns 0
            every { chatRepository.deleteChangeSetsOfChats(any()) } returns 0
            every { chatRepository.deleteMessagesOfChats(any()) } returns 0
            every { chatRepository.deleteAllById(any<Iterable<Int>>()) } returns Unit

            When("no limit is set") {
                val removed = service.applyRetention(null, null)

                Then("nothing is deleted") {
                    removed shouldBe 0
                    verify(exactly = 0) { chatRepository.deleteAllById(any<Iterable<Int>>()) }
                }
            }

            When("a non positive limit is set") {
                val removed = service.applyRetention(0, -3)

                Then("it means no limit") { removed shouldBe 0 }
            }

            When("the limit is 20 conversations") {
                val removed = service.applyRetention(20, null)

                Then("the 10 oldest are deleted") {
                    removed shouldBe 10
                    verify { chatRepository.deleteAllById((21..30).toList()) }
                }
            }

            When("the limit is 7 days") {
                val removed = service.applyRetention(null, 7)

                Then("conversations with the last activity more than 7 days ago are deleted") {
                    removed shouldBe 22
                    verify { chatRepository.deleteAllById((9..30).toList()) }
                }
            }

            When("both limits are set") {
                val removed = service.applyRetention(maxChats = 25, maxDays = 10)

                Then("the union is deleted, each conversation once") {
                    removed shouldBe 19
                    verify { chatRepository.deleteAllById((12..30).toList()) }
                }
            }
        }

        Given("limits set in the preferences") {
            val chats = (0 until 5).map { chat(it + 1, "c${it + 1}", now.minusDays(it.toLong())) }
            every { chatRepository.findAllByOrderByUpdatedAtDescIdDesc() } returns chats
            every { chatRepository.deleteChangesOfChats(any()) } returns 0
            every { chatRepository.deleteChangeSetsOfChats(any()) } returns 0
            every { chatRepository.deleteMessagesOfChats(any()) } returns 0
            every { chatRepository.deleteAllById(any<Iterable<Int>>()) } returns Unit

            When("only a maximum of 3 conversations is set") {
                every { preferencesService.aiHistoryMaxChats } returns 3
                every { preferencesService.aiHistoryMaxDays } returns null

                val removed = service.applyConfiguredRetention()

                Then("the two oldest are deleted") {
                    removed shouldBe 2
                    verify { chatRepository.deleteAllById(listOf(4, 5)) }
                }
            }

            When("no limit is set") {
                every { preferencesService.aiHistoryMaxChats } returns null
                every { preferencesService.aiHistoryMaxDays } returns null

                Then("nothing is deleted") {
                    service.applyConfiguredRetention() shouldBe 0
                    verify(exactly = 0) { chatRepository.deleteAllById(any<Iterable<Int>>()) }
                }
            }
        }

        Given("a search") {
            every { messageRepository.searchTitles(any(), any()) } returns emptyList()
            every { messageRepository.searchMessages(any(), any()) } returns emptyList()

            When("the text is blank") {
                val hits = service.search("   ")

                Then("nothing is searched") {
                    hits.shouldBeEmpty()
                    verify(exactly = 0) { messageRepository.searchMessages(any(), any()) }
                }
            }

            When("the text has LIKE wildcards") {
                service.search("50%_a\\b")

                Then("they are escaped so they match themselves") {
                    verify { messageRepository.searchMessages("%50\\%\\_a\\\\b%", 50) }
                }
            }

            When("a message and a title match") {
                every { messageRepository.searchTitles(any(), any()) } returns
                    listOf(arrayOf<Any?>(1, "Fatura de outubro"))
                every { messageRepository.searchMessages(any(), any()) } returns
                    listOf(
                        arrayOf<Any?>(
                            2,
                            "Contas",
                            55,
                            3,
                            null,
                            "x ".repeat(40) + "a FATURA venceu ontem " + "y ".repeat(40),
                        ),
                    )
                val hits = service.search("fatura")

                Then("the title comes first, without a message") {
                    hits[0].chatId shouldBe 1
                    hits[0].messageId shouldBe null
                }

                Then("the message hit has the position and a snippet around the term") {
                    hits[1].messageId shouldBe 55
                    hits[1].position shouldBe 3
                    hits[1].snippet.contains("a FATURA venceu") shouldBe true
                    (hits[1].snippet.length <= 82) shouldBe true
                }
            }
        }

        val payload =
            WalletTransactionPayload(
                date = LocalDate.of(2026, 11, 6),
                description = "Mercado",
                amount = BigDecimal("50.10"),
                movementType = WalletTransactionType.EXPENSE,
                walletId = 1,
                categoryName = "Food",
            )

        fun proposed(id: String) =
            ProposedChange(
                id = id,
                operationName = "create_wallet_transaction",
                payload = payload,
                summary = "Mercado 50,10",
                badge = ChangeBadge.NEW,
                reasons = listOf("new record"),
                confidence = 0.9,
            )

        Given("an answer of the assistant with two proposals") {
            val answer =
                AiChatMessage(id = 31, chatId = 6, position = 1, role = "ASSISTANT", content = "ok", createdAt = now)
            every { messageRepository.findById(31) } returns Optional.of(answer)
            val sets = mutableListOf<AiChatChangeSet>()
            val rows = slot<Iterable<AiChatChange>>()
            every { changeSetRepository.save(capture(sets)) } answers { firstArg() }
            every { changeRepository.saveAll(capture(rows)) } answers { rows.captured.toList() }

            When("the change set is saved") {
                service.saveChangeSet(6, 31, ChangeSet("set-1", listOf(proposed("a"), proposed("b"))))

                Then("it is linked to the answer") {
                    sets.single().id shouldBe "set-1"
                    sets.single().messageId shouldBe 31
                }

                Then("each proposal waits for a decision, in order, with the payload as proposed") {
                    val saved = rows.captured.toList()
                    saved.map { it.id } shouldBe listOf("a", "b")
                    saved.map { it.position } shouldBe listOf(0, 1)
                    saved.map { it.state }.toSet() shouldBe setOf("PENDING")
                    saved[0].payloadJson shouldBe saved[0].originalPayloadJson
                    saved[0].payloadJson.contains("\"2026-11-06\"") shouldBe true
                    saved[0].badge shouldBe "NEW"
                    saved[0].decidedAt shouldBe null
                }
            }

            When("the change set has nothing in it") {
                service.saveChangeSet(6, 31, ChangeSet("set-2", emptyList()))

                Then("nothing is stored") {
                    verify(exactly = 0) { changeSetRepository.save(any<AiChatChangeSet>()) }
                }
            }

            When("the answer belongs to another conversation") {
                Then("it is refused") {
                    shouldThrow<MoinexException.AiChatHistoryException> {
                        service.saveChangeSet(99, 31, ChangeSet("set-3", listOf(proposed("c"))))
                    }
                    verify(exactly = 0) { changeSetRepository.save(any<AiChatChangeSet>()) }
                }
            }
        }

        Given("a stored proposal") {
            val change =
                AiChatChange(
                    id = "a",
                    changeSetId = "set-1",
                    position = 0,
                    operationName = "create_wallet_transaction",
                    payloadJson = "{}",
                    originalPayloadJson = "{}",
                    badge = "NEW",
                    confidence = 0.9,
                    state = "PENDING",
                )
            every { changeRepository.findById("a") } returns Optional.of(change)
            every { changeRepository.save(any<AiChatChange>()) } answers { firstArg() }

            When("the user approves it") {
                service.updateChange("a", ChangeState.APPROVED, payload, null)

                Then("the state, the payload in use and the moment are kept") {
                    change.state shouldBe "APPROVED"
                    change.decidedAt shouldBe now
                    change.payloadJson.contains("Mercado") shouldBe true
                    change.originalPayloadJson shouldBe "{}"
                }
            }

            When("the user rejects it") {
                service.updateChange("a", ChangeState.REJECTED, payload, null)

                Then("it counts as decided") { change.decidedAt shouldBe now }
            }

            When("the user edits it") {
                service.updateChange("a", ChangeState.EDITED, payload, null)

                Then("it is still open, with no moment of decision") {
                    change.state shouldBe "EDITED"
                    change.decidedAt shouldBe null
                }
            }

            When("applying it failed") {
                service.updateChange("a", ChangeState.ERROR, payload, "wallet is archived")

                Then("the error and its text are kept") {
                    change.state shouldBe "ERROR"
                    change.message shouldBe "wallet is archived"
                    change.decidedAt shouldBe now
                }
            }

            When("the id is unknown") {
                every { changeRepository.findById("zzz") } returns Optional.empty()

                Then("it is refused") {
                    shouldThrow<MoinexException.AiChatHistoryException> {
                        service.updateChange("zzz", ChangeState.APPROVED, payload, null)
                    }
                }
            }
        }

        Given("a stored conversation whose answer proposed three things") {
            every { chatRepository.findById(8) } returns Optional.of(chat(8, "Extrato"))
            every { messageRepository.findByChatIdOrderByPosition(8) } returns
                listOf(
                    AiChatMessage(id = 80, chatId = 8, position = 0, role = "USER", content = "lança", createdAt = now),
                    AiChatMessage(
                        id = 81,
                        chatId = 8,
                        position = 1,
                        role = "ASSISTANT",
                        content = "pronto",
                        createdAt = now,
                    ),
                )
            val json = codec.payloadToJson(payload)

            fun row(
                id: String,
                state: String,
                operation: String = "create_wallet_transaction",
                current: String = json,
            ) = AiChatChange(
                id = id,
                changeSetId = "set-8",
                position = id.toInt(),
                operationName = operation,
                payloadJson = current,
                originalPayloadJson = json,
                summary = "Mercado",
                badge = "NEW",
                reasonsJson = "[\"new\"]",
                confidence = 0.9,
                dependsOnJson = "[]",
                matchCandidatesJson = "[]",
                state = state,
                decidedAt = if (state == "APPROVED") now else null,
            )
            every { changeSetRepository.findByMessageIdIn(any()) } returns
                listOf(AiChatChangeSet(id = "set-8", messageId = 81, ignoredJson = null))
            every { changeRepository.findByChangeSetIdInOrderByPosition(any()) } returns
                listOf(
                    row("1", "APPROVED"),
                    row("2", "REJECTED"),
                    row("3", "EDITED", current = json.replace("Mercado", "Padaria")),
                )

            When("it is opened") {
                val opened = service.openChat(8)
                val stored = opened.changeSets.single()

                Then("the proposals are tied to the answer that carried them") {
                    stored.messageIndex shouldBe 1
                    stored.changeSet.id shouldBe "set-8"
                    stored.changeSet.changes.map { it.id } shouldBe listOf("1", "2", "3")
                    stored.changeSet.changes
                        .first()
                        .payload shouldBe payload
                }

                Then("each one comes back with its state, and edits are kept") {
                    stored.states.getValue("1").state shouldBe ChangeState.APPROVED
                    stored.states.getValue("1").decidedAt shouldBe now
                    stored.states.getValue("2").state shouldBe ChangeState.REJECTED
                    (stored.states.getValue("3").payload as WalletTransactionPayload).description shouldBe "Padaria"
                    stored.changeSet.changes[2].payload shouldBe payload
                }

                Then("only the ones that can still be decided count as pending") {
                    opened.chat.pendingProposals shouldBe 1
                }
            }
        }

        Given("a stored proposal whose operation no longer exists") {
            every { chatRepository.findById(9) } returns Optional.of(chat(9))
            every { messageRepository.findByChatIdOrderByPosition(9) } returns
                listOf(
                    AiChatMessage(
                        id = 90,
                        chatId = 9,
                        position = 0,
                        role = "ASSISTANT",
                        content = "x",
                        createdAt = now,
                    ),
                )
            every { changeSetRepository.findByMessageIdIn(any()) } returns
                listOf(AiChatChangeSet(id = "set-9", messageId = 90))

            fun gone(state: String) =
                AiChatChange(
                    id = "g",
                    changeSetId = "set-9",
                    position = 0,
                    operationName = "operation_from_the_future",
                    payloadJson = "{\"a\":1}",
                    originalPayloadJson = "{\"a\":1}",
                    badge = "NEW",
                    confidence = 0.5,
                    state = state,
                )

            When("it was still pending") {
                every { changeRepository.findByChangeSetIdInOrderByPosition(any()) } returns listOf(gone("PENDING"))
                val state =
                    service
                        .openChat(9)
                        .changeSets
                        .single()
                        .states
                        .getValue("g")

                Then("it opens as a read only error with the raw text, and the conversation still opens") {
                    state.state shouldBe ChangeState.ERROR
                    state.payload shouldBe RawPayload("{\"a\":1}")
                }
            }

            When("it had already been approved") {
                every { changeRepository.findByChangeSetIdInOrderByPosition(any()) } returns listOf(gone("APPROVED"))
                val state =
                    service
                        .openChat(9)
                        .changeSets
                        .single()
                        .states
                        .getValue("g")

                Then("it keeps its history as approved") { state.state shouldBe ChangeState.APPROVED }
            }
        }
    })
