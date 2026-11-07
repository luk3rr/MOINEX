/*
 * Filename: AiChatChange.kt
 * Created on: November 6, 2026
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 */

package org.moinex.model.ai

import jakarta.persistence.Column
import jakarta.persistence.Convert
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import org.moinex.common.converter.LocalDateTimeStringConverter
import java.time.LocalDateTime

/**
 * One stored proposal. [payloadJson] is the current payload (with the user's edits) and [originalPayloadJson]
 * what the assistant proposed. [state] holds the name of an `org.moinex.ai.operation.ChangeState`.
 */
@Entity
@Table(name = "ai_chat_change")
class AiChatChange(
    @Id
    @Column(name = "id", length = 64)
    var id: String,
    @Column(name = "change_set_id", nullable = false, length = 64)
    var changeSetId: String,
    @Column(name = "position", nullable = false)
    var position: Int,
    @Column(name = "operation_name", nullable = false, length = 100)
    var operationName: String,
    @Column(name = "payload_json", nullable = false, columnDefinition = "TEXT")
    var payloadJson: String,
    @Column(name = "original_payload_json", nullable = false, columnDefinition = "TEXT")
    var originalPayloadJson: String,
    @Column(name = "summary", length = 500)
    var summary: String? = null,
    @Column(name = "badge", nullable = false, length = 30)
    var badge: String,
    @Column(name = "reasons_json", columnDefinition = "TEXT")
    var reasonsJson: String? = null,
    @Column(name = "confidence", nullable = false)
    var confidence: Double,
    @Column(name = "depends_on_json", columnDefinition = "TEXT")
    var dependsOnJson: String? = null,
    @Column(name = "match_candidates_json", columnDefinition = "TEXT")
    var matchCandidatesJson: String? = null,
    @Column(name = "target_id")
    var targetId: Int? = null,
    @Column(name = "state", nullable = false, length = 20)
    var state: String,
    @Column(name = "message", columnDefinition = "TEXT")
    var message: String? = null,
    @Convert(converter = LocalDateTimeStringConverter::class)
    @Column(name = "decided_at")
    var decidedAt: LocalDateTime? = null,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || javaClass != other.javaClass) return false
        return id == (other as AiChatChange).id
    }

    override fun hashCode(): Int = id.hashCode()

    override fun toString(): String = "AiChatChange [id=$id, operation=$operationName, state=$state]"
}
