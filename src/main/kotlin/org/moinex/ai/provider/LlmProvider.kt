/*
 * Filename: LlmProvider.kt
 * Created on: September 20, 2026
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 */

package org.moinex.ai.provider

import org.moinex.ai.chat.ChatMessage
import org.moinex.ai.chat.LlmResponse
import org.moinex.ai.tools.LlmTool
import org.moinex.model.enums.LlmProviderType

/**
 * Common surface over the chat-completion + tool-calling APIs of the supported BYOK providers.
 *
 * Deliberately minimal: this app does not need streaming or the full breadth of each provider's
 * SDK, so a hand-rolled abstraction over java.net.http.HttpClient is enough and avoids pulling in
 * a heavyweight agent framework for a small surface area.
 */
interface LlmProvider {
    val type: LlmProviderType

    suspend fun chat(
        messages: List<ChatMessage>,
        tools: List<LlmTool>,
    ): LlmResponse

    /** Cheap call used by the "Testar conexão" button in Settings - must not consume chat tokens. */
    suspend fun testConnection(): Result<Unit>
}

class LlmProviderException(
    message: String,
) : RuntimeException(message)
