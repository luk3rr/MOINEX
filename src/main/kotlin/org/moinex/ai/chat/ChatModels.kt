/*
 * Filename: ChatModels.kt
 * Created on: September 20, 2026
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 */

package org.moinex.ai.chat

import org.moinex.ai.operation.ChangeSet

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

/**
 * Whether the chat window shows this message. The user's messages and the assistant's answers are shown; the text
 * an assistant message carries while it asks for tools is the model thinking aloud, and the tool results are data for
 * the model, so neither is shown, as when the conversation was live.
 */
val ChatMessage.isShownInChat: Boolean
    get() =
        when (role) {
            ChatRole.USER -> true
            ChatRole.ASSISTANT -> toolCalls.isEmpty()
            else -> false
        } &&
            (displayText ?: content).isNotBlank()

data class ToolCall(
    val id: String,
    val toolName: String,
    val argumentsJson: String,
)

/** A single file attached to the chat, with its text already extracted, pending a user message. */
data class ChatAttachment(
    val fileName: String,
    val extractedText: String,
)

data class LlmResponse(
    val text: String?,
    val toolCalls: List<ToolCall> = emptyList(),
)

/**
 * [Success.turnMessages] are the messages the turn added to the conversation, in order: the assistant messages that
 * asked for tools, the tool results, and the final answer. They are what the model needs to carry on the conversation.
 */
sealed class ChatTurnResult {
    data class Success(
        val reply: String,
        val changeSet: ChangeSet? = null,
        val turnMessages: List<ChatMessage> = emptyList(),
    ) : ChatTurnResult()

    data object NoProviderConfigured : ChatTurnResult()

    data class Error(
        val message: String,
    ) : ChatTurnResult()
}
