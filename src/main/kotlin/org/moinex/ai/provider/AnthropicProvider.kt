/*
 * Filename: AnthropicProvider.kt
 * Created on: September 20, 2026
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 */

package org.moinex.ai.provider

import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import org.moinex.ai.chat.ChatMessage
import org.moinex.ai.chat.ChatRole
import org.moinex.ai.chat.LlmResponse
import org.moinex.ai.chat.ToolCall
import org.moinex.ai.tools.LlmTool
import org.moinex.config.AppDispatchers
import org.moinex.model.enums.LlmProviderType
import java.net.http.HttpClient

class AnthropicProvider(
    private val apiKey: String,
    val model: String = DEFAULT_MODEL,
    httpClient: HttpClient = HttpClient.newHttpClient(),
) : LlmProvider {
    override val type = LlmProviderType.ANTHROPIC

    private val httpClient = LlmHttpClient("Anthropic", httpClient)

    companion object {
        const val DEFAULT_MODEL = "claude-sonnet-5"
        private const val API_VERSION = "2023-06-01"
        private const val MESSAGES_ENDPOINT = "https://api.anthropic.com/v1/messages"
        private const val MODELS_ENDPOINT = "https://api.anthropic.com/v1/models"
    }

    override suspend fun chat(
        messages: List<ChatMessage>,
        tools: List<LlmTool>,
    ): LlmResponse =
        withContext(AppDispatchers.IO) {
            val requestBody = AnthropicCodec.buildRequestBody(model, messages, tools)
            val responseBody = httpClient.post(model, MESSAGES_ENDPOINT, jsonHeaders(), requestBody)
            AnthropicCodec.parseResponse(responseBody)
        }

    override suspend fun testConnection(): Result<Unit> =
        withContext(AppDispatchers.IO) {
            runCatching { httpClient.get(model, MODELS_ENDPOINT, authHeaders()) }.map { }
        }

    private fun jsonHeaders() =
        mapOf(
            "Content-Type" to "application/json",
            "x-api-key" to apiKey,
            "anthropic-version" to API_VERSION,
        )

    private fun authHeaders() =
        mapOf(
            "x-api-key" to apiKey,
            "anthropic-version" to API_VERSION,
        )
}

/**
 * Pure request/response JSON mapping for the Anthropic Messages API (with tool use), kept
 * separate from [AnthropicProvider] so it can be unit tested without any network I/O.
 *
 * Anthropic has no "tool" role: a tool result is sent back as a "user" message containing a
 * `tool_result` content block. When our internal message list has several consecutive TOOL
 * messages (one assistant turn requested more than one tool call), they must be grouped into a
 * single "user" message with multiple `tool_result` blocks, not one message per result.
 */
internal object AnthropicCodec {
    private const val DEFAULT_MAX_TOKENS = 4096

    fun buildRequestBody(
        model: String,
        messages: List<ChatMessage>,
        tools: List<LlmTool>,
    ): String {
        val systemText =
            messages
                .filter { it.role == ChatRole.SYSTEM }
                .joinToString("\n") { it.content }
                .takeIf { it.isNotBlank() }

        val json =
            JSONObject()
                .put("model", model)
                .put("max_tokens", DEFAULT_MAX_TOKENS)
                .put("messages", buildMessages(messages.filter { it.role != ChatRole.SYSTEM }))
        if (systemText != null) json.put("system", systemText)
        if (tools.isNotEmpty()) json.put("tools", buildTools(tools))
        return json.toString()
    }

    private fun buildMessages(messages: List<ChatMessage>): JSONArray {
        val array = JSONArray()
        var index = 0
        while (index < messages.size) {
            val message = messages[index]
            when (message.role) {
                ChatRole.ASSISTANT -> {
                    array.put(JSONObject().put("role", "assistant").put("content", buildAssistantBlocks(message)))
                    index++
                }
                ChatRole.TOOL -> {
                    val blocks = JSONArray()
                    while (index < messages.size && messages[index].role == ChatRole.TOOL) {
                        val toolMessage = messages[index]
                        blocks.put(
                            JSONObject()
                                .put("type", "tool_result")
                                .put("tool_use_id", toolMessage.toolCallId)
                                .put("content", toolMessage.content),
                        )
                        index++
                    }
                    array.put(JSONObject().put("role", "user").put("content", blocks))
                }
                else -> {
                    array.put(
                        JSONObject().put("role", "user").put("content", JSONArray().put(textBlock(message.content))),
                    )
                    index++
                }
            }
        }
        return array
    }

    private fun buildAssistantBlocks(message: ChatMessage): JSONArray {
        val blocks = JSONArray()
        if (message.content.isNotBlank()) blocks.put(textBlock(message.content))
        message.toolCalls.forEach { call ->
            blocks.put(
                JSONObject()
                    .put("type", "tool_use")
                    .put("id", call.id)
                    .put("name", call.toolName)
                    .put("input", JSONObject(call.argumentsJson.ifBlank { "{}" })),
            )
        }
        return blocks
    }

    private fun textBlock(text: String): JSONObject = JSONObject().put("type", "text").put("text", text)

    private fun buildTools(tools: List<LlmTool>): JSONArray =
        JSONArray(
            tools.map { tool ->
                JSONObject()
                    .put("name", tool.name)
                    .put("description", tool.description)
                    .put("input_schema", JSONObject(tool.jsonSchema))
            },
        )

    fun parseResponse(responseBody: String): LlmResponse {
        val blocks = JSONObject(responseBody).getJSONArray("content")

        var text: String? = null
        val toolCalls = mutableListOf<ToolCall>()

        for (i in 0 until blocks.length()) {
            val block = blocks.getJSONObject(i)
            when (block.optString("type")) {
                "text" -> text = ((text ?: "") + block.optString("text", "")).ifBlank { null }
                "tool_use" ->
                    toolCalls.add(
                        ToolCall(
                            id = block.getString("id"),
                            toolName = block.getString("name"),
                            argumentsJson = block.optJSONObject("input")?.toString() ?: "{}",
                        ),
                    )
            }
        }

        return LlmResponse(text = text, toolCalls = toolCalls)
    }
}
