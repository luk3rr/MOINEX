package org.moinex.ai.provider

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import org.json.JSONObject
import org.moinex.ai.chat.ChatMessage
import org.moinex.ai.chat.ChatRole
import org.moinex.ai.chat.ToolCall
import org.moinex.ai.tools.LlmTool

private class FakeTool(
    override val name: String = "get_net_worth",
) : LlmTool {
    override val description = "Returns net worth"
    override val jsonSchema = """{"type":"object","properties":{}}"""

    override fun execute(argumentsJson: String): String = "{}"
}

class OpenAiCodecTest :
    BehaviorSpec({
        Given("a simple user message and one available tool") {
            val messages = listOf(ChatMessage(role = ChatRole.USER, content = "Quanto gastei em maio?"))
            val tools = listOf(FakeTool())

            When("building the request body") {
                val body = JSONObject(OpenAiCodec.buildRequestBody("gpt-4o-mini", messages, tools))

                Then("it contains the model, the user message and the tool declaration") {
                    body.getString("model") shouldBe "gpt-4o-mini"

                    val messageJson = body.getJSONArray("messages").getJSONObject(0)
                    messageJson.getString("role") shouldBe "user"
                    messageJson.getString("content") shouldBe "Quanto gastei em maio?"

                    val toolJson = body.getJSONArray("tools").getJSONObject(0)
                    toolJson.getString("type") shouldBe "function"
                    toolJson.getJSONObject("function").getString("name") shouldBe "get_net_worth"
                }
            }
        }

        Given("an assistant turn that requested a tool call, followed by its result") {
            val assistantMessage =
                ChatMessage(
                    role = ChatRole.ASSISTANT,
                    content = "",
                    toolCalls = listOf(ToolCall(id = "call_1", toolName = "get_net_worth", argumentsJson = "{}")),
                )
            val toolResultMessage =
                ChatMessage(
                    role = ChatRole.TOOL,
                    content = """{"netWorth":1000}""",
                    toolCallId = "call_1",
                    toolName = "get_net_worth",
                )

            When("building the request body") {
                val body =
                    JSONObject(
                        OpenAiCodec.buildRequestBody(
                            "gpt-4o-mini",
                            listOf(assistantMessage, toolResultMessage),
                            emptyList(),
                        ),
                    )

                Then("the assistant message carries tool_calls and the tool message links back via tool_call_id") {
                    val assistantJson = body.getJSONArray("messages").getJSONObject(0)
                    assistantJson.getString("role") shouldBe "assistant"
                    val toolCallJson = assistantJson.getJSONArray("tool_calls").getJSONObject(0)
                    toolCallJson.getString("id") shouldBe "call_1"
                    toolCallJson.getJSONObject("function").getString("name") shouldBe "get_net_worth"

                    val toolJson = body.getJSONArray("messages").getJSONObject(1)
                    toolJson.getString("role") shouldBe "tool"
                    toolJson.getString("tool_call_id") shouldBe "call_1"
                    toolJson.getString("content") shouldBe """{"netWorth":1000}"""
                }
            }
        }

        Given("a chat completion response with a plain text answer") {
            val responseBody =
                """
                {
                  "choices": [
                    {"message": {"role": "assistant", "content": "Você gastou R$ 100."}}
                  ]
                }
                """.trimIndent()

            When("parsing the response") {
                val result = OpenAiCodec.parseResponse(responseBody)

                Then("the text is extracted and there are no tool calls") {
                    result.text shouldBe "Você gastou R$ 100."
                    result.toolCalls shouldHaveSize 0
                }
            }
        }

        Given("a chat completion response requesting a tool call") {
            val responseBody =
                """
                {
                  "choices": [
                    {
                      "message": {
                        "role": "assistant",
                        "content": null,
                        "tool_calls": [
                          {
                            "id": "call_abc",
                            "type": "function",
                            "function": {"name": "get_net_worth", "arguments": "{\"month\":\"2026-09\"}"}
                          }
                        ]
                      }
                    }
                  ]
                }
                """.trimIndent()

            When("parsing the response") {
                val result = OpenAiCodec.parseResponse(responseBody)

                Then("the tool call is extracted with its id, name and arguments") {
                    result.text shouldBe null
                    result.toolCalls shouldHaveSize 1
                    result.toolCalls[0].id shouldBe "call_abc"
                    result.toolCalls[0].toolName shouldBe "get_net_worth"
                    result.toolCalls[0].argumentsJson shouldBe """{"month":"2026-09"}"""
                }
            }
        }
    })
