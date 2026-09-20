/*
 * Filename: OpenRouterProvider.kt
 * Created on: September 20, 2026
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 */

package org.moinex.ai.provider

import kotlinx.coroutines.withContext
import org.moinex.ai.chat.ChatMessage
import org.moinex.ai.chat.LlmResponse
import org.moinex.ai.tools.LlmTool
import org.moinex.config.AppDispatchers
import org.moinex.model.enums.LlmProviderType
import java.net.http.HttpClient

/**
 * OpenRouter exposes an OpenAI-compatible Chat Completions API (including tool calling) in front
 * of many underlying models, so this reuses [OpenAiCodec] as-is - only the endpoint, auth header
 * and default model id differ from [OpenAiProvider].
 */
class OpenRouterProvider(
    private val apiKey: String,
    val model: String = DEFAULT_MODEL,
    httpClient: HttpClient = HttpClient.newHttpClient(),
) : LlmProvider {
    override val type = LlmProviderType.OPENROUTER

    private val httpClient = LlmHttpClient("OpenRouter", httpClient)

    companion object {
        const val DEFAULT_MODEL = "openai/gpt-4o-mini"
        private const val CHAT_ENDPOINT = "https://openrouter.ai/api/v1/chat/completions"
        private const val MODELS_ENDPOINT = "https://openrouter.ai/api/v1/models"
        private const val APP_TITLE = "MOINEX"
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
            "X-Title" to APP_TITLE,
        )

    private fun authHeader() = mapOf("Authorization" to "Bearer $apiKey")
}
