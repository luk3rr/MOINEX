/*
 * Filename: ChatMessageVisibilityTest.kt
 * Created on: November 7, 2026
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 */

package org.moinex.ai.chat

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe

class ChatMessageVisibilityTest :
    BehaviorSpec({
        val call = ToolCall("c1", "get_net_worth", "{}")

        Given("the messages of a restored conversation") {
            Then("the user's messages and the final answers are shown") {
                ChatMessage(ChatRole.USER, "oi").isShownInChat shouldBe true
                ChatMessage(ChatRole.USER, "full text", displayText = "shown").isShownInChat shouldBe true
                ChatMessage(ChatRole.ASSISTANT, "resposta").isShownInChat shouldBe true
            }

            Then("what the model says while it asks for tools is not shown, as when the chat was live") {
                ChatMessage(
                    ChatRole.ASSISTANT,
                    "Vou consultar os dados...",
                    toolCalls = listOf(call),
                ).isShownInChat shouldBe
                    false
            }

            Then("tool results, system messages and blank messages are not shown") {
                ChatMessage(ChatRole.TOOL, "{}", toolCallId = "c1", toolName = "get_net_worth").isShownInChat shouldBe
                    false
                ChatMessage(ChatRole.SYSTEM, "prompt").isShownInChat shouldBe false
                ChatMessage(ChatRole.ASSISTANT, "   ").isShownInChat shouldBe false
                ChatMessage(ChatRole.USER, "", displayText = "").isShownInChat shouldBe false
            }
        }
    })
