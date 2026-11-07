/*
 * Filename: AiChatChangeSetRepository.kt
 * Created on: November 6, 2026
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 */

package org.moinex.repository.ai

import org.moinex.model.ai.AiChatChangeSet
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface AiChatChangeSetRepository : JpaRepository<AiChatChangeSet, String> {
    fun findByMessageIdIn(messageIds: Collection<Int>): List<AiChatChangeSet>
}
