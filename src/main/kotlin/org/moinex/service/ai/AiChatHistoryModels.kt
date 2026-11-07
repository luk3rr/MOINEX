/*
 * Filename: AiChatHistoryModels.kt
 * Created on: November 6, 2026
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 */

package org.moinex.service.ai

import org.moinex.ai.chat.ChatMessage
import org.moinex.ai.operation.ChangeSet
import org.moinex.ai.operation.ChangeState
import java.time.LocalDateTime

/** A stored conversation as listed beside the chat. [messageCount] counts the messages the chat shows. */
data class ChatSummary(
    val id: Int,
    val title: String,
    val updatedAt: LocalDateTime,
    val messageCount: Int,
    val pendingProposals: Int = 0,
)

/** A stored conversation opened for reading, with every message in the order it was sent to the model. */
data class StoredConversation(
    val chat: ChatSummary,
    val messages: List<ChatMessage>,
    val changeSets: List<StoredChangeSet> = emptyList(),
)

/**
 * What the assistant proposed in one answer. [messageIndex] is the index, in [StoredConversation.messages], of the
 * answer that carried it. [changeSet] holds each proposal as the assistant made it; [states] holds what the user did
 * with it since, by change id.
 */
data class StoredChangeSet(
    val messageIndex: Int,
    val changeSet: ChangeSet,
    val states: Map<String, StoredChangeState>,
)

/** The current state of a stored proposal. [payload] is a RawPayload when it can no longer be rebuilt. */
data class StoredChangeState(
    val state: ChangeState,
    val payload: Any,
    val message: String?,
    val decidedAt: LocalDateTime?,
)

/**
 * A search result. [messageId] is null when only the title matched. [position] is the index of the matching message
 * in the conversation, or 0 for a title match.
 */
data class SearchHit(
    val chatId: Int,
    val chatTitle: String,
    val messageId: Int?,
    val position: Int,
    val snippet: String,
)
