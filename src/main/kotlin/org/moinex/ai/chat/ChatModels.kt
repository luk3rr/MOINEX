/*
 * Filename: ChatModels.kt
 * Created on: September 20, 2026
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 */

package org.moinex.ai.chat

enum class ChatRole {
    SYSTEM,
    USER,
    ASSISTANT,
    TOOL,
}

/**
 * A single turn in the conversation sent to/received from a [org.moinex.ai.provider.LlmProvider].
 *
 * [toolCalls] is only populated on ASSISTANT messages that requested tool execution.
 * [toolCallId]/[toolName] are only populated on TOOL messages, linking the result back to the
 * call that produced it (required to build a valid follow-up request for OpenAI/Anthropic).
 */
data class ChatMessage(
    val role: ChatRole,
    val content: String,
    val toolCalls: List<ToolCall> = emptyList(),
    val toolCallId: String? = null,
    val toolName: String? = null,
)

data class ToolCall(
    val id: String,
    val toolName: String,
    val argumentsJson: String,
)

data class LlmResponse(
    val text: String?,
    val toolCalls: List<ToolCall> = emptyList(),
)

sealed class ChatTurnResult {
    data class Success(
        val reply: String,
    ) : ChatTurnResult()

    data object NoProviderConfigured : ChatTurnResult()

    data class Error(
        val message: String,
    ) : ChatTurnResult()
}
