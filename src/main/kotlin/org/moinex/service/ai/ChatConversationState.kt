/*
 * Filename: ChatConversationState.kt
 * Created on: November 7, 2026
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 */

package org.moinex.service.ai

import org.moinex.ai.chat.ChatMessage
import org.moinex.ai.chat.ChatRole
import org.moinex.ai.chat.ChatTurnResult
import org.moinex.ai.operation.ChangeSet
import org.moinex.exception.MoinexException
import org.springframework.stereotype.Component

/** Whether what the conversation holds in memory reached the database. */
sealed interface PersistOutcome {
    data object Saved : PersistOutcome

    /** [message] is already translated and fit to show to the user. */
    data class Failed(
        val message: String,
    ) : PersistOutcome
}

/**
 * The conversation on screen: its messages, as the model needs them, and the stored conversation they belong to.
 * It has no JavaFX in it. A conversation is stored from its first message on; a failure to store never loses what is
 * on screen, and the next turn stores again whatever is still missing.
 */
@Component
class ChatConversationState(
    private val historyService: AiChatHistoryService,
) {
    private val messages = mutableListOf<ChatMessage>()
    private val attachmentNames = mutableMapOf<Int, List<String>>()
    private var persistedCount = 0
    private var chatId: Int? = null

    @Synchronized
    fun currentChatId(): Int? = chatId

    @Synchronized
    fun messages(): List<ChatMessage> = messages.toList()

    /** Starts an empty conversation that is stored when its first message is sent. */
    @Synchronized
    fun reset() {
        chatId = null
        messages.clear()
        attachmentNames.clear()
        persistedCount = 0
    }

    @Synchronized
    fun load(stored: StoredConversation) {
        reset()
        chatId = stored.chat.id
        messages += stored.messages
        persistedCount = messages.size
    }

    /** Adds the user's [message] to the conversation and stores it. [attachmentNames] are the files it carries. */
    @Synchronized
    fun startTurn(
        message: ChatMessage,
        attachmentNames: List<String> = emptyList(),
    ): PersistOutcome {
        messages += message
        if (attachmentNames.isNotEmpty()) this.attachmentNames[messages.lastIndex] = attachmentNames
        return persist(null)
    }

    /** Adds what the turn produced, with the time it took on the final answer, and stores it with its proposals. */
    @Synchronized
    fun finishTurn(
        result: ChatTurnResult.Success,
        elapsedMillis: Long,
    ): PersistOutcome {
        val turn =
            result.turnMessages.ifEmpty {
                listOfNotNull(
                    ChatMessage(role = ChatRole.ASSISTANT, content = result.reply).takeIf { result.notice == null },
                )
            }
        messages +=
            turn.mapIndexed { index, message ->
                if (index == turn.lastIndex && message.role == ChatRole.ASSISTANT) {
                    message.copy(responseTimeMillis = elapsedMillis)
                } else {
                    message
                }
            }
        result.notice?.let { messages += ChatMessage(role = ChatRole.NOTICE, content = it) }
        return persist(result.changeSet)
    }

    /** Stores [message] as an app notice for a turn that failed, so the conversation shows it when reopened. */
    @Synchronized
    fun failTurn(message: String): PersistOutcome {
        messages += ChatMessage(role = ChatRole.NOTICE, content = message)
        return persist(null)
    }

    private fun createChat(): Int {
        val id = historyService.createChat()
        chatId = id
        runCatching { historyService.applyConfiguredRetention() }
        return id
    }

    private fun persist(changeSet: ChangeSet?): PersistOutcome =
        try {
            val id = chatId ?: createChat()
            val batch = messages.drop(persistedCount)
            if (batch.isNotEmpty()) {
                val names =
                    attachmentNames
                        .filterKeys { it >= persistedCount }
                        .minByOrNull { it.key }
                        ?.value
                        .orEmpty()
                val ids = historyService.appendMessages(id, batch, names)
                persistedCount = messages.size
                attachmentNames.clear()
                if (changeSet != null && !changeSet.isEmpty) historyService.saveChangeSet(id, ids.last(), changeSet)
            }
            PersistOutcome.Saved
        } catch (e: MoinexException.AiChatHistoryException) {
            PersistOutcome.Failed(e.message.orEmpty())
        }
}
