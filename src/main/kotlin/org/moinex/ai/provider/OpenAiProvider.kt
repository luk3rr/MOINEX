/*
 * Filename: OpenAiProvider.kt
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

class OpenAiProvider(
    private val apiKey: String,
    val model: String = DEFAULT_MODEL,
    httpClient: HttpClient = HttpClient.newHttpClient(),
) : LlmProvider {
    override val type = LlmProviderType.OPENAI

    private val httpClient = LlmHttpClient("OpenAI", httpClient)

    companion object {
        const val DEFAULT_MODEL = "gpt-4o-mini"
        private const val CHAT_ENDPOINT = "https://api.openai.com/v1/chat/completions"
        private const val MODELS_ENDPOINT = "https://api.openai.com/v1/models"
    }

    override suspend fun chat(
        messages: List<ChatMessage>,
        tools: List<LlmTool>,
    ): LlmResponse =
        withContext(AppDispatchers.IO) {
            val requestBody = OpenAiCodec.buildRequestBody(model, messages, tools)
            val responseBody = httpClient.post(model, CHAT_ENDPOINT, jsonHeaders(), requestBody)
            OpenAiCodec.parseResponse(responseBody)
        }

    override suspend fun testConnection(): Result<Unit> =
        withContext(AppDispatchers.IO) {
            runCatching { httpClient.get(model, MODELS_ENDPOINT, authHeader()) }.map { }
        }

    private fun jsonHeaders() =
        mapOf(
            "Content-Type" to "application/json",
            "Authorization" to "Bearer $apiKey",
        )

    private fun authHeader() = mapOf("Authorization" to "Bearer $apiKey")
}

/**
 * Pure request/response JSON mapping for the OpenAI Chat Completions API (with tool calling),
 * kept separate from [OpenAiProvider] so it can be unit tested without any network I/O.
 */
internal object OpenAiCodec {
    fun buildRequestBody(
        model: String,
        messages: List<ChatMessage>,
        tools: List<LlmTool>,
    ): String {
        val json =
            JSONObject()
                .put("model", model)
                .put("messages", buildMessages(messages))
        if (tools.isNotEmpty()) {
            json.put("tools", buildTools(tools))
        }
        return json.toString()
    }

    private fun buildMessages(messages: List<ChatMessage>): JSONArray {
        val array = JSONArray()
        messages.forEach { message ->
            when (message.role) {
                ChatRole.TOOL ->
                    array.put(
                        JSONObject()
                            .put("role", "tool")
                            .put("tool_call_id", message.toolCallId)
                            .put("content", message.content),
                    )
                ChatRole.ASSISTANT -> array.put(buildAssistantMessage(message))
                else ->
                    array.put(
                        JSONObject()
                            .put("role", message.role.name.lowercase())
                            .put("content", message.content),
                    )
            }
        }
        return array
    }

    private fun buildAssistantMessage(message: ChatMessage): JSONObject {
        val obj = JSONObject().put("role", "assistant")
        obj.put("content", if (message.content.isBlank()) JSONObject.NULL else message.content)

        if (message.toolCalls.isNotEmpty()) {
            obj.put(
                "tool_calls",
                JSONArray(
                    message.toolCalls.map { call ->
                        JSONObject()
                            .put("id", call.id)
                            .put("type", "function")
                            .put(
                                "function",
                                JSONObject()
                                    .put("name", call.toolName)
                                    .put("arguments", call.argumentsJson),
                            )
                    },
                ),
            )
        }
        return obj
    }

    private fun buildTools(tools: List<LlmTool>): JSONArray =
        JSONArray(
            tools.map { tool ->
                JSONObject()
                    .put("type", "function")
                    .put(
                        "function",
                        JSONObject()
                            .put("name", tool.name)
                            .put("description", tool.description)
                            .put("parameters", JSONObject(tool.jsonSchema)),
                    )
            },
        )

    fun parseResponse(responseBody: String): LlmResponse {
        val message =
            JSONObject(responseBody)
                .getJSONArray("choices")
                .getJSONObject(0)
                .getJSONObject("message")

        val text =
            if (message.has("content") && !message.isNull("content")) {
                message.optString("content", "").takeIf { it.isNotBlank() }
            } else {
                null
            }

        val toolCalls =
            if (message.has("tool_calls") && !message.isNull("tool_calls")) {
                val array = message.getJSONArray("tool_calls")
                (0 until array.length()).map { index ->
                    val call = array.getJSONObject(index)
                    val function = call.getJSONObject("function")
                    ToolCall(
                        id = call.getString("id"),
                        toolName = function.getString("name"),
                        argumentsJson = function.getString("arguments"),
                    )
                }
            } else {
                emptyList()
            }

        return LlmResponse(text = text, toolCalls = toolCalls)
    }
}
