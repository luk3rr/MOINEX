package org.moinex.ai.evaluation

import org.moinex.ai.chat.ChatMessage
import org.moinex.ai.chat.LlmResponse
import org.moinex.ai.provider.LlmProvider
import org.moinex.ai.tools.LlmTool
import org.moinex.model.enums.LlmProviderType

/**
 * A model replaced by a fixed script, so evaluation cases do not depend on any real LLM. Each call to [chat] returns
 * the next scripted response and records what the app offered: the messages and the tools.
 */
class ScriptedLlmProvider(
    private val script: List<LlmResponse>,
) : LlmProvider {
    override val type: LlmProviderType = LlmProviderType.OPENROUTER

    val receivedMessages = mutableListOf<List<ChatMessage>>()
    val offeredTools = mutableListOf<List<LlmTool>>()

    override suspend fun chat(
        messages: List<ChatMessage>,
        tools: List<LlmTool>,
    ): LlmResponse {
        receivedMessages += messages
        offeredTools += tools
        return script[receivedMessages.size - 1]
    }

    override suspend fun testConnection(): Result<Unit> = Result.success(Unit)
}
