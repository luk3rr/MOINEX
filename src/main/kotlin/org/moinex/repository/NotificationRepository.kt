package org.moinex.repository

import org.moinex.model.Notification
import org.moinex.model.enums.NotificationStatus
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface NotificationRepository : JpaRepository<Notification, Int> {
    fun findAllByOrderByCreatedAtDesc(): List<Notification>

    fun findByStatusOrderByCreatedAtDesc(status: NotificationStatus): List<Notification>

    fun countByStatus(status: NotificationStatus): Long
}
