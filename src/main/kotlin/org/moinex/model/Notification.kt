package org.moinex.model

import jakarta.persistence.Column
import jakarta.persistence.Convert
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import org.moinex.common.converter.LocalDateTimeStringConverter
import org.moinex.model.enums.NotificationStatus
import org.moinex.model.enums.NotificationType
import java.time.LocalDateTime

@Entity
@Table(name = "notification")
class Notification(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    var id: Int? = null,
    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false)
    var type: NotificationType,
    @Column(name = "title", nullable = false, length = 100)
    var title: String,
    @Column(name = "message", nullable = false, length = 500)
    var message: String,
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    var status: NotificationStatus = NotificationStatus.UNREAD,
    @Convert(converter = LocalDateTimeStringConverter::class)
    @Column(name = "created_at", nullable = false)
    var createdAt: LocalDateTime,
    @Column(name = "related_entity_id")
    var relatedEntityId: Int? = null,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || javaClass != other.javaClass) return false
        val notification = other as Notification
        return id != null && id == notification.id
    }

    override fun hashCode(): Int = id ?: javaClass.hashCode()

    override fun toString(): String = "Notification [id=$id, type=$type, status=$status]"
}
