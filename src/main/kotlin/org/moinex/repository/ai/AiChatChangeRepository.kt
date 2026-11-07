/*
 * Filename: AiChatChangeRepository.kt
 * Created on: November 6, 2026
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 */

package org.moinex.repository.ai

import org.moinex.model.ai.AiChatChange
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository

@Repository
interface AiChatChangeRepository : JpaRepository<AiChatChange, String> {
    fun findByChangeSetIdInOrderByPosition(changeSetIds: Collection<String>): List<AiChatChange>

    @Query(
        value =
            "SELECT m.chat_id, COUNT(*) FROM ai_chat_change c " +
                "JOIN ai_chat_change_set cs ON cs.id = c.change_set_id " +
                "JOIN ai_chat_message m ON m.id = cs.message_id " +
                "WHERE c.state IN ('PENDING', 'EDITED', 'ERROR') GROUP BY m.chat_id",
        nativeQuery = true,
    )
    fun countOpenByChat(): List<Array<Any>>
}
