package org.moinex.ai.provider

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import org.json.JSONObject
import org.moinex.ai.chat.ChatMessage
import org.moinex.ai.chat.ChatRole
import org.moinex.ai.chat.ToolCall

class GeminiCodecTest :
    BehaviorSpec({
        Given("a system message followed by a user message") {
            val messages =
                listOf(
                    ChatMessage(role = ChatRole.SYSTEM, content = "Você é um assistente financeiro."),
                    ChatMessage(role = ChatRole.USER, content = "Qual meu patrimônio?"),
                )

            When("building the request body") {
                val body = JSONObject(GeminiCodec.buildRequestBody(messages, emptyList()))

                Then("the system text goes into systemInstruction, not contents") {
                    body
                        .getJSONObject("systemInstruction")
                        .getJSONArray("parts")
                        .getJSONObject(0)
                        .getString("text") shouldBe "Você é um assistente financeiro."

                    val contents = body.getJSONArray("contents")
                    contents.length() shouldBe 1
                    contents.getJSONObject(0).getString("role") shouldBe "user"
                }
            }
        }

        Given("an assistant turn with a tool call, followed by its TOOL result message") {
            val assistantMessage =
                ChatMessage(
                    role = ChatRole.ASSISTANT,
                    content = "",
                    toolCalls =
                        listOf(
                            ToolCall(
                                id = "gemini-call-0",
                                toolName = "get_net_worth",
                                argumentsJson = """{"month":"2026-09"}""",
                            ),
                        ),
                )
            val toolResultMessage =
                ChatMessage(role = ChatRole.TOOL, content = """{"netWorth":1000}""", toolName = "get_net_worth")

            When("building the request body") {
                val body =
                    JSONObject(GeminiCodec.buildRequestBody(listOf(assistantMessage, toolResultMessage), emptyList()))

                Then("the model turn has a functionCall part and the result is sent as a 'user' role content") {
                    val contents = body.getJSONArray("contents")
                    contents.length() shouldBe 2

                    val modelContent = contents.getJSONObject(0)
                    modelContent.getString("role") shouldBe "model"
                    val functionCall = modelContent.getJSONArray("parts").getJSONObject(0).getJSONObject("functionCall")
                    functionCall.getString("name") shouldBe "get_net_worth"

                    val functionContent = contents.getJSONObject(1)
                    functionContent.getString("role") shouldBe "user"
                    val functionResponse =
                        functionContent
                            .getJSONArray(
                                "parts",
                            ).getJSONObject(0)
                            .getJSONObject("functionResponse")
                    functionResponse.getString("name") shouldBe "get_net_worth"
                }
            }
        }

        Given("a generateContent response with a text part and a functionCall part") {
            val responseBody =
                """
                {
                  "candidates": [
                    {
                      "content": {
                        "role": "model",
                        "parts": [
                          {"text": "Vou consultar seus dados."},
                          {"functionCall": {"name": "get_net_worth", "args": {"month": "2026-09"}}}
                        ]
                      }
                    }
                  ]
                }
                """.trimIndent()

            When("parsing the response") {
                val result = GeminiCodec.parseResponse(responseBody)

                Then("both the text and the synthetic tool call are extracted") {
                    result.text shouldBe "Vou consultar seus dados."
                    result.toolCalls shouldHaveSize 1
                    result.toolCalls[0].toolName shouldBe "get_net_worth"
                    JSONObject(result.toolCalls[0].argumentsJson).getString("month") shouldBe "2026-09"
                }
            }
        }

        Given("a response with no candidates") {
            val responseBody = """{"candidates": []}"""

            When("parsing the response") {
                val result = GeminiCodec.parseResponse(responseBody)

                Then("it returns no text and no tool calls instead of throwing") {
                    result.text shouldBe null
                    result.toolCalls shouldHaveSize 0
                }
            }
        }
    })
