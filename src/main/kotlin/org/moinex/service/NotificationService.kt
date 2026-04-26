package org.moinex.service

import org.moinex.common.util.FxUtils
import org.moinex.model.Notification
import org.moinex.model.dto.NotificationEvent
import org.moinex.model.enums.NotificationStatus
import org.moinex.repository.NotificationRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional
import org.springframework.transaction.event.TransactionPhase
import org.springframework.transaction.event.TransactionalEventListener

@Service
class NotificationService(
    private val notificationRepository: NotificationRepository,
) {
    private val logger = LoggerFactory.getLogger(NotificationService::class.java)

    private val uiListeners = mutableListOf<(Notification) -> Unit>()

    fun registerUiListener(listener: (Notification) -> Unit) {
        uiListeners.add(listener)
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    fun onNotificationEvent(event: NotificationEvent) {
        val notification =
            Notification(
                type = event.type,
                title = event.title,
                message = event.message,
                relatedEntityId = event.relatedEntityId,
            )

        notificationRepository.save(notification)
        logger.debug("Notification saved: [{}] {}", notification.type, notification.title)

        FxUtils.launchOnFxThread {
            uiListeners.forEach { it(notification) }
        }
    }

    fun getAll(): List<Notification> = notificationRepository.findAllByOrderByCreatedAtDesc()

    fun getUnread(): List<Notification> =
        notificationRepository.findByStatusOrderByCreatedAtDesc(NotificationStatus.UNREAD)

    fun countUnread(): Long = notificationRepository.countByStatus(NotificationStatus.UNREAD)

    @Transactional
    fun markAllAsRead() {
        notificationRepository
            .findByStatusOrderByCreatedAtDesc(NotificationStatus.UNREAD)
            .forEach { it.status = NotificationStatus.READ }
    }

    @Transactional
    fun markAsRead(id: Int) {
        notificationRepository.findById(id).ifPresent { it.status = NotificationStatus.READ }
    }

    @Transactional
    fun deleteAll() = notificationRepository.deleteAll()
}
