/*
 * Filename: AiChatRepository.kt
 * Created on: November 6, 2026
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 */

package org.moinex.repository.ai

import org.moinex.model.ai.AiChat
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository

/**
 * The SQLite connection of the app does not enforce foreign keys, so the children of a conversation are removed
 * explicitly, deepest first, by the three delete queries below.
 */
@Repository
interface AiChatRepository : JpaRepository<AiChat, Int> {
    fun findAllByOrderByUpdatedAtDescIdDesc(): List<AiChat>

    fun findFirstByOrderByUpdatedAtDescIdDesc(): AiChat?

    @Modifying
    @Query(
        value =
            "DELETE FROM ai_chat_change WHERE change_set_id IN (" +
                "SELECT cs.id FROM ai_chat_change_set cs JOIN ai_chat_message m ON m.id = cs.message_id " +
                "WHERE m.chat_id IN (:chatIds))",
        nativeQuery = true,
    )
    fun deleteChangesOfChats(
        @Param("chatIds") chatIds: Collection<Int>,
    ): Int

    @Modifying
    @Query(
        value =
            "DELETE FROM ai_chat_change_set WHERE message_id IN (" +
                "SELECT id FROM ai_chat_message WHERE chat_id IN (:chatIds))",
        nativeQuery = true,
    )
    fun deleteChangeSetsOfChats(
        @Param("chatIds") chatIds: Collection<Int>,
    ): Int

    @Modifying
    @Query(value = "DELETE FROM ai_chat_message WHERE chat_id IN (:chatIds)", nativeQuery = true)
    fun deleteMessagesOfChats(
        @Param("chatIds") chatIds: Collection<Int>,
    ): Int
}
