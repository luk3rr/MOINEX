/*
 * Filename: GeminiProvider.kt
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
import java.net.URLEncoder
import java.net.http.HttpClient
import java.nio.charset.StandardCharsets

class GeminiProvider(
    private val apiKey: String,
    val model: String = DEFAULT_MODEL,
    httpClient: HttpClient = HttpClient.newHttpClient(),
) : LlmProvider {
    override val type = LlmProviderType.GEMINI

    private val httpClient = LlmHttpClient("Gemini", httpClient)

    companion object {
        const val DEFAULT_MODEL = "gemini-3.6-flash"
        private const val API_BASE = "https://generativelanguage.googleapis.com/v1beta"
    }

    override suspend fun chat(
        messages: List<ChatMessage>,
        tools: List<LlmTool>,
    ): LlmResponse =
        withContext(AppDispatchers.IO) {
            val requestBody = GeminiCodec.buildRequestBody(messages, tools)
            val endpoint = "$API_BASE/models/$model:generateContent?key=${encodeKey()}"
            val responseBody = httpClient.post(model, endpoint, jsonHeaders(), requestBody)
            GeminiCodec.parseResponse(responseBody)
        }

    override suspend fun testConnection(): Result<Unit> =
        withContext(AppDispatchers.IO) {
            runCatching { httpClient.get(model, "$API_BASE/models?key=${encodeKey()}", emptyMap()) }.map { }
        }

    private fun encodeKey(): String = URLEncoder.encode(apiKey, StandardCharsets.UTF_8)

    private fun jsonHeaders() = mapOf("Content-Type" to "application/json")
}

/**
 * Pure request/response JSON mapping for the Gemini `generateContent` API (with function
 * calling), kept separate from [GeminiProvider] so it can be unit tested without any network I/O.
 *
 * Unlike OpenAI/Anthropic, Gemini's `functionCall` parts carry no call id - responses are matched
 * back to calls by function name via a `functionResponse` part, so [ToolCall.id] here is a
 * synthetic value used only internally by [org.moinex.ai.chat.AiChatService].
 *
 * The Content holding that `functionResponse` part uses role "user" (confirmed against a live
 * "Role 'function' is not supported" error from the API and Google's own function-calling
 * examples) - Gemini has no distinct "function"/"tool" role; tool results are just another user
 * turn.
 */
internal object GeminiCodec {
    fun buildRequestBody(
        messages: List<ChatMessage>,
        tools: List<LlmTool>,
    ): String {
        val systemText =
            messages
                .filter { it.role == ChatRole.SYSTEM }
                .joinToString("\n") { it.content }
                .takeIf { it.isNotBlank() }

        val json = JSONObject()
        if (systemText != null) {
            json.put(
                "systemInstruction",
                JSONObject().put("parts", JSONArray().put(JSONObject().put("text", systemText))),
            )
        }
        json.put("contents", buildContents(messages.filter { it.role != ChatRole.SYSTEM }))
        if (tools.isNotEmpty()) json.put("tools", buildTools(tools))
        return json.toString()
    }

    private fun buildContents(messages: List<ChatMessage>): JSONArray {
        val array = JSONArray()
        messages.forEach { message ->
            when (message.role) {
                ChatRole.ASSISTANT -> array.put(contentOf("model", buildAssistantParts(message)))
                ChatRole.TOOL -> array.put(contentOf("user", buildToolResponseParts(message)))
                else -> array.put(contentOf("user", JSONArray().put(JSONObject().put("text", message.content))))
            }
        }
        return array
    }

    private fun buildAssistantParts(message: ChatMessage): JSONArray {
        val parts = JSONArray()
        if (message.content.isNotBlank()) parts.put(JSONObject().put("text", message.content))
        message.toolCalls.forEach { call ->
            parts.put(
                JSONObject().put(
                    "functionCall",
                    JSONObject()
                        .put("name", call.toolName)
                        .put("args", JSONObject(call.argumentsJson.ifBlank { "{}" })),
                ),
            )
        }
        return parts
    }

    private fun buildToolResponseParts(message: ChatMessage): JSONArray =
        JSONArray().put(
            JSONObject().put(
                "functionResponse",
                JSONObject()
                    .put("name", message.toolName.orEmpty())
                    .put(
                        "response",
                        JSONObject().put("content", JSONObject(message.content.ifBlank { "{}" })),
                    ),
            ),
        )

    private fun contentOf(
        role: String,
        parts: JSONArray,
    ): JSONObject = JSONObject().put("role", role).put("parts", parts)

    private fun buildTools(tools: List<LlmTool>): JSONArray =
        JSONArray().put(
            JSONObject().put(
                "functionDeclarations",
                JSONArray(
                    tools.map { tool ->
                        JSONObject()
                            .put("name", tool.name)
                            .put("description", tool.description)
                            .put("parameters", JSONObject(tool.jsonSchema))
                    },
                ),
            ),
        )

    fun parseResponse(responseBody: String): LlmResponse {
        val candidates = JSONObject(responseBody).optJSONArray("candidates")
        if (candidates == null || candidates.length() == 0) return LlmResponse(text = null)

        val parts = candidates.getJSONObject(0).getJSONObject("content").optJSONArray("parts") ?: JSONArray()

        var text: String? = null
        val toolCalls = mutableListOf<ToolCall>()
        var callIndex = 0

        for (i in 0 until parts.length()) {
            val part = parts.getJSONObject(i)

            if (part.has("text")) {
                text = ((text ?: "") + part.optString("text", "")).ifBlank { null }
            }

            if (part.has("functionCall")) {
                val functionCall = part.getJSONObject("functionCall")
                toolCalls.add(
                    ToolCall(
                        id = "gemini-call-${callIndex++}",
                        toolName = functionCall.getString("name"),
                        argumentsJson = functionCall.optJSONObject("args")?.toString() ?: "{}",
                    ),
                )
            }
        }

        return LlmResponse(text = text, toolCalls = toolCalls)
    }
}
