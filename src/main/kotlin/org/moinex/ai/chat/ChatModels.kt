/*
 * Filename: ChatModels.kt
 * Created on: September 20, 2026
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 */

package org.moinex.ai.chat

import org.moinex.model.dto.ai.ExtractedTransactionDTO

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
 * [displayText], when set, is shown in the chat bubble instead of [content] - used when [content]
 * carries extra instructions/context (e.g. an attached file's full text) that shouldn't be
 * re-rendered verbatim when the history is replayed.
 */
data class ChatMessage(
    val role: ChatRole,
    val content: String,
    val displayText: String? = null,
    val toolCalls: List<ToolCall> = emptyList(),
    val toolCallId: String? = null,
    val toolName: String? = null,
    val responseTimeMillis: Long? = null,
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

    data class TransactionsProposed(
        val transactions: List<ExtractedTransactionDTO>,
        val reply: String,
    ) : ChatTurnResult()

    data object NoProviderConfigured : ChatTurnResult()

    data class Error(
        val message: String,
    ) : ChatTurnResult()
}
