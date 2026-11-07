/*
 * Filename: AiChat.kt
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

/** One stored conversation with the assistant. */
@Entity
@Table(name = "ai_chat")
class AiChat(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    var id: Int? = null,
    @Column(name = "title", nullable = false, length = 200)
    var title: String,
    @Convert(converter = LocalDateTimeStringConverter::class)
    @Column(name = "created_at", nullable = false)
    var createdAt: LocalDateTime,
    @Convert(converter = LocalDateTimeStringConverter::class)
    @Column(name = "updated_at", nullable = false)
    var updatedAt: LocalDateTime,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || javaClass != other.javaClass) return false
        return id != null && id == (other as AiChat).id
    }

    override fun hashCode(): Int = id ?: javaClass.hashCode()

    override fun toString(): String = "AiChat [id=$id, title=$title]"
}
