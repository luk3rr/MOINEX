/*
 * Filename: AiChatMessageRepository.kt
 * Created on: November 6, 2026
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 */

package org.moinex.repository.ai

import org.moinex.model.ai.AiChatMessage
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository

/** Columns: chat id, chat title, message id, position, display text, content. TOOL messages are never searched. */
const val AI_CHAT_SEARCH_MESSAGES_SQL: String =
    "SELECT c.id, c.title, m.id, m.position, m.display_text, m.content " +
        "FROM ai_chat_message m JOIN ai_chat c ON c.id = m.chat_id " +
        "WHERE m.role <> 'TOOL' AND (m.content LIKE :pattern ESCAPE '\\' " +
        "OR m.display_text LIKE :pattern ESCAPE '\\') " +
        "ORDER BY c.updated_at DESC, m.position LIMIT :limit"

/** Columns: chat id, chat title. */
const val AI_CHAT_SEARCH_TITLES_SQL: String =
    "SELECT c.id, c.title FROM ai_chat c WHERE c.title LIKE :pattern ESCAPE '\\' " +
        "ORDER BY c.updated_at DESC LIMIT :limit"

@Repository
interface AiChatMessageRepository : JpaRepository<AiChatMessage, Int> {
    fun findByChatIdOrderByPosition(chatId: Int): List<AiChatMessage>

    @Query(
        value = "SELECT COALESCE(MAX(position), -1) FROM ai_chat_message WHERE chat_id = :chatId",
        nativeQuery = true,
    )
    fun findLastPosition(
        @Param("chatId") chatId: Int,
    ): Int

    @Query(
        value = "SELECT chat_id, COUNT(*) FROM ai_chat_message WHERE role <> 'TOOL' GROUP BY chat_id",
        nativeQuery = true,
    )
    fun countVisibleMessagesByChat(): List<Array<Any>>

    @Query(value = AI_CHAT_SEARCH_MESSAGES_SQL, nativeQuery = true)
    fun searchMessages(
        @Param("pattern") pattern: String,
        @Param("limit") limit: Int,
    ): List<Array<Any?>>

    @Query(value = AI_CHAT_SEARCH_TITLES_SQL, nativeQuery = true)
    fun searchTitles(
        @Param("pattern") pattern: String,
        @Param("limit") limit: Int,
    ): List<Array<Any?>>
}
