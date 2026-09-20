package org.moinex.ai.provider

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import org.json.JSONObject
import org.moinex.ai.chat.ChatMessage
import org.moinex.ai.chat.ChatRole
import org.moinex.ai.chat.ToolCall

class AnthropicCodecTest :
    BehaviorSpec({
        Given("a system message followed by a user message") {
            val messages =
                listOf(
                    ChatMessage(role = ChatRole.SYSTEM, content = "Você é um assistente financeiro."),
                    ChatMessage(role = ChatRole.USER, content = "Qual meu patrimônio?"),
                )

            When("building the request body") {
                val body = JSONObject(AnthropicCodec.buildRequestBody("claude-sonnet-5", messages, emptyList()))

                Then("the system text is hoisted to the top-level 'system' field, not the messages array") {
                    body.getString("system") shouldBe "Você é um assistente financeiro."
                    body.getJSONArray("messages").length() shouldBe 1
                    body.getJSONArray("messages").getJSONObject(0).getString("role") shouldBe "user"
                }
            }
        }

        Given("an assistant turn with two tool calls, followed by two TOOL result messages") {
            val assistantMessage =
                ChatMessage(
                    role = ChatRole.ASSISTANT,
                    content = "",
                    toolCalls =
                        listOf(
                            ToolCall(id = "call_1", toolName = "get_net_worth", argumentsJson = "{}"),
                            ToolCall(id = "call_2", toolName = "get_spending_by_category", argumentsJson = "{}"),
                        ),
                )
            val toolResults =
                listOf(
                    ChatMessage(role = ChatRole.TOOL, content = """{"netWorth":1000}""", toolCallId = "call_1"),
                    ChatMessage(role = ChatRole.TOOL, content = """{"byCategory":{}}""", toolCallId = "call_2"),
                )

            When("building the request body") {
                val body =
                    JSONObject(
                        AnthropicCodec.buildRequestBody(
                            "claude-sonnet-5",
                            listOf(assistantMessage) + toolResults,
                            emptyList(),
                        ),
                    )

                Then("both tool results are grouped into a single following 'user' message") {
                    val messages = body.getJSONArray("messages")
                    messages.length() shouldBe 2

                    val assistantJson = messages.getJSONObject(0)
                    assistantJson.getString("role") shouldBe "assistant"
                    assistantJson.getJSONArray("content").length() shouldBe 2

                    val userJson = messages.getJSONObject(1)
                    userJson.getString("role") shouldBe "user"
                    val resultBlocks = userJson.getJSONArray("content")
                    resultBlocks.length() shouldBe 2
                    resultBlocks.getJSONObject(0).getString("type") shouldBe "tool_result"
                    resultBlocks.getJSONObject(0).getString("tool_use_id") shouldBe "call_1"
                    resultBlocks.getJSONObject(1).getString("tool_use_id") shouldBe "call_2"
                }
            }
        }

        Given("a Messages API response with a text block and a tool_use block") {
            val responseBody =
                """
                {
                  "content": [
                    {"type": "text", "text": "Deixa eu consultar seus dados."},
                    {"type": "tool_use", "id": "toolu_1", "name": "get_net_worth", "input": {"month": "2026-09"}}
                  ]
                }
                """.trimIndent()

            When("parsing the response") {
                val result = AnthropicCodec.parseResponse(responseBody)

                Then("both the text and the tool call are extracted") {
                    result.text shouldBe "Deixa eu consultar seus dados."
                    result.toolCalls shouldHaveSize 1
                    result.toolCalls[0].id shouldBe "toolu_1"
                    result.toolCalls[0].toolName shouldBe "get_net_worth"
                    JSONObject(result.toolCalls[0].argumentsJson).getString("month") shouldBe "2026-09"
                }
            }
        }
    })
