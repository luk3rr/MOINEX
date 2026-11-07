/*
 * Filename: AiChatMessage.kt
 * Created on: November 6, 2026
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 */

package org.moinex.model.ai

import jakarta.persistence.Column
import jakarta.persistence.Convert
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import org.moinex.common.converter.LocalDateTimeStringConverter
import java.time.LocalDateTime

/**
 * One turn of a stored conversation. [content] is what was sent to the model (attachment text included) and
 * [displayText] what the chat bubble shows. [attachmentNames] and [toolCallsJson] are JSON arrays.
 */
@Entity
@Table(name = "ai_chat_message")
class AiChatMessage(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    var id: Int? = null,
    @Column(name = "chat_id", nullable = false)
    var chatId: Int,
    @Column(name = "position", nullable = false)
    var position: Int,
    @Column(name = "role", nullable = false, length = 20)
    var role: String,
    @Column(name = "content", nullable = false, columnDefinition = "TEXT")
    var content: String,
    @Column(name = "display_text", columnDefinition = "TEXT")
    var displayText: String? = null,
    @Column(name = "attachment_names", columnDefinition = "TEXT")
    var attachmentNames: String? = null,
    @Column(name = "tool_calls_json", columnDefinition = "TEXT")
    var toolCallsJson: String? = null,
    @Column(name = "tool_call_id", length = 100)
    var toolCallId: String? = null,
    @Column(name = "tool_name", length = 100)
    var toolName: String? = null,
    @Column(name = "response_time_millis")
    var responseTimeMillis: Long? = null,
    @Convert(converter = LocalDateTimeStringConverter::class)
    @Column(name = "created_at", nullable = false)
    var createdAt: LocalDateTime,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || javaClass != other.javaClass) return false
        return id != null && id == (other as AiChatMessage).id
    }

    override fun hashCode(): Int = id ?: javaClass.hashCode()

    override fun toString(): String = "AiChatMessage [id=$id, chatId=$chatId, position=$position, role=$role]"
}
