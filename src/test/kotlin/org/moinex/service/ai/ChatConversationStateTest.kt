/*
 * Filename: ChatConversationStateTest.kt
 * Created on: November 7, 2026
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 */

package org.moinex.service.ai

import io.kotest.core.spec.IsolationMode
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.moinex.ai.chat.ChatMessage
import org.moinex.ai.chat.ChatRole
import org.moinex.ai.chat.ChatTurnResult
import org.moinex.ai.chat.ToolCall
import org.moinex.ai.operation.ChangeBadge
import org.moinex.ai.operation.ChangeSet
import org.moinex.ai.operation.ProposedChange
import org.moinex.exception.MoinexException
import java.time.LocalDateTime

class ChatConversationStateTest :
    BehaviorSpec({
        isolationMode = IsolationMode.InstancePerLeaf

        val history = mockk<AiChatHistoryService>()
        val state = ChatConversationState(history)

        every { history.applyConfiguredRetention() } returns 0

        val user = ChatMessage(ChatRole.USER, "quanto gastei?")
        val answer = ChatMessage(ChatRole.ASSISTANT, "R$ 100")

        fun change() = ProposedChange("c1", "create_wallet_transaction", Any(), "x", ChangeBadge.NEW, emptyList(), 0.9)

        Given("a new conversation") {
            every { history.createChat() } returns 5
            val stored = slot<List<ChatMessage>>()
            every { history.appendMessages(5, capture(stored), any()) } answers
                { stored.captured.indices.map { 100 + it } }

            When("the user sends the first message") {
                val outcome = state.startTurn(user, listOf("a.csv"))

                Then("the conversation is created and the message stored with its attachment names") {
                    outcome shouldBe PersistOutcome.Saved
                    state.currentChatId() shouldBe 5
                    verify { history.appendMessages(5, listOf(user), listOf("a.csv")) }
                }
            }

            When("a turn with a tool call finishes with proposals") {
                state.startTurn(user)
                val toolCall = ToolCall("t1", "get_net_worth", "{}")
                val turn =
                    listOf(
                        ChatMessage(ChatRole.ASSISTANT, "", toolCalls = listOf(toolCall)),
                        ChatMessage(ChatRole.TOOL, "{}", toolCallId = "t1", toolName = "get_net_worth"),
                        answer,
                    )
                val changeSet = ChangeSet("set-1", listOf(change()))
                every { history.saveChangeSet(any(), any(), any()) } returns Unit

                val outcome = state.finishTurn(ChatTurnResult.Success("R$ 100", changeSet, turn), 1500)

                Then("every message of the turn is stored in order, the final answer with its time") {
                    outcome shouldBe PersistOutcome.Saved
                    stored.captured.map { it.role } shouldBe
                        listOf(ChatRole.ASSISTANT, ChatRole.TOOL, ChatRole.ASSISTANT)
                    stored.captured.last().responseTimeMillis shouldBe 1500
                    stored.captured.first().responseTimeMillis shouldBe null
                }

                Then("the proposals are tied to the final answer") {
                    verify { history.saveChangeSet(5, 102, changeSet) }
                }

                Then("the model sees the whole turn on the next message") {
                    state.messages().map { it.role } shouldBe
                        listOf(ChatRole.USER, ChatRole.ASSISTANT, ChatRole.TOOL, ChatRole.ASSISTANT)
                }
            }

            When("a success comes without turn messages") {
                state.startTurn(user)
                state.finishTurn(ChatTurnResult.Success("R$ 100"), 10)

                Then("the reply becomes the assistant message") {
                    state.messages().last().content shouldBe "R$ 100"
                    state.messages().last().role shouldBe ChatRole.ASSISTANT
                }
            }

            When("a turn fails") {
                state.startTurn(user)
                state.failTurn("Erro ao consultar o assistente: limite")

                Then("the error is stored as an app notice, so it shows as one when the chat is reopened") {
                    stored.captured.map { it.role } shouldBe listOf(ChatRole.NOTICE)
                    stored.captured.single().content shouldBe "Erro ao consultar o assistente: limite"
                    state.messages().map { it.role } shouldBe listOf(ChatRole.USER, ChatRole.NOTICE)
                }
            }

            When("a turn ends with an app notice") {
                state.startTurn(user)
                state.finishTurn(
                    ChatTurnResult.Success(
                        reply = "",
                        turnMessages = listOf(ChatMessage(role = ChatRole.TOOL, content = "{}", toolCallId = "1")),
                        notice = "limit reached",
                    ),
                    10,
                )

                Then("the notice is stored as a NOTICE message and no empty assistant answer is added") {
                    state.messages().map { it.role } shouldBe listOf(ChatRole.USER, ChatRole.TOOL, ChatRole.NOTICE)
                    state.messages().last().content shouldBe "limit reached"
                }
            }

            When("the conversation is created") {
                state.startTurn(user)

                Then("the limits the user set are applied once, after it exists") {
                    verify(exactly = 1) { history.applyConfiguredRetention() }
                }
            }

            When("a second message goes to the same conversation") {
                state.startTurn(user)
                state.startTurn(user)

                Then("the limits are not applied again") {
                    verify(exactly = 1) { history.applyConfiguredRetention() }
                }
            }

            When("applying the limits fails") {
                every { history.applyConfiguredRetention() } throws IllegalStateException("boom")
                val outcome = state.startTurn(user)

                Then("the message is still stored") {
                    outcome shouldBe PersistOutcome.Saved
                    verify { history.appendMessages(5, any(), any()) }
                }
            }

            When("a turn finishes without proposals") {
                state.startTurn(user)
                state.finishTurn(ChatTurnResult.Success("ok", null, listOf(answer)), 10)

                Then("no change set is stored") {
                    verify(exactly = 0) { history.saveChangeSet(any(), any(), any()) }
                }
            }
        }

        Given("a database that fails on the first message and recovers later") {
            val error = MoinexException.AiChatHistoryException("could not save")
            every { history.createChat() } returns 9
            val batches = mutableListOf<List<ChatMessage>>()
            var failing = true
            every { history.appendMessages(9, any(), any()) } answers {
                if (failing) throw error
                batches += secondArg<List<ChatMessage>>()
                secondArg<List<ChatMessage>>().indices.toList()
            }

            When("the first message cannot be stored") {
                val outcome = state.startTurn(user)

                Then("the user is told and the message stays on screen") {
                    outcome.shouldBeInstanceOf<PersistOutcome.Failed>()
                    (outcome as PersistOutcome.Failed).message shouldBe "could not save"
                    state.messages() shouldBe listOf(user)
                }
            }

            When("the next turn is stored after the database recovered") {
                state.startTurn(user)
                failing = false
                val outcome = state.finishTurn(ChatTurnResult.Success("ok", null, listOf(answer)), 10)

                Then("everything that was missing is stored, in order") {
                    outcome shouldBe PersistOutcome.Saved
                    batches.single() shouldBe listOf(user, answer.copy(responseTimeMillis = 10))
                }
            }
        }

        Given("a stored conversation opened from the list") {
            val opened =
                StoredConversation(
                    ChatSummary(3, "Contas", LocalDateTime.of(2026, 11, 6, 10, 0), 2),
                    listOf(user, answer),
                )
            state.load(opened)
            val stored = slot<List<ChatMessage>>()
            every { history.appendMessages(3, capture(stored), any()) } answers { stored.captured.indices.toList() }

            When("the user sends another message") {
                state.startTurn(ChatMessage(ChatRole.USER, "e ontem?"))

                Then("it goes to the same conversation, without storing the old messages again") {
                    state.currentChatId() shouldBe 3
                    stored.captured.map { it.content } shouldBe listOf("e ontem?")
                    verify(exactly = 0) { history.createChat() }
                }
            }

            When("a new conversation is started") {
                state.reset()

                Then("nothing is left of the old one") {
                    state.currentChatId() shouldBe null
                    state.messages() shouldBe emptyList()
                }
            }
        }
    })
