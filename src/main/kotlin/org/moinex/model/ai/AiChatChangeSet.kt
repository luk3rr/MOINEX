/*
 * Filename: AiChatChangeSet.kt
 * Created on: November 6, 2026
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 */

package org.moinex.model.ai

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table

/** The proposals of one assistant answer. [id] is the id of the in-memory change set. */
@Entity
@Table(name = "ai_chat_change_set")
class AiChatChangeSet(
    @Id
    @Column(name = "id", length = 64)
    var id: String,
    @Column(name = "message_id", nullable = false)
    var messageId: Int,
    @Column(name = "ignored_json", columnDefinition = "TEXT")
    var ignoredJson: String? = null,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || javaClass != other.javaClass) return false
        return id == (other as AiChatChangeSet).id
    }

    override fun hashCode(): Int = id.hashCode()

    override fun toString(): String = "AiChatChangeSet [id=$id, messageId=$messageId]"
}
