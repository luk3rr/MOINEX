package org.moinex.service

import org.moinex.common.util.FxUtils
import org.moinex.model.Notification
import org.moinex.model.enums.NotificationStatus
import org.moinex.model.enums.NotificationType
import org.moinex.repository.NotificationRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.transaction.support.TransactionSynchronization
import org.springframework.transaction.support.TransactionSynchronizationManager

@Service
class NotificationService(
    private val notificationRepository: NotificationRepository,
) {
    private val logger = LoggerFactory.getLogger(NotificationService::class.java)

    private val uiListeners = mutableListOf<(Notification) -> Unit>()
    private val pendingToasts = mutableListOf<Notification>()

    fun registerUiListener(listener: (Notification) -> Unit) {
        uiListeners.add(listener)
        if (pendingToasts.isNotEmpty()) {
            val pending = pendingToasts.toList()
            pendingToasts.clear()
            FxUtils.launchOnFxThread { pending.forEach { listener(it) } }
        }
    }

    @Transactional
    fun createNotification(
        type: NotificationType,
        title: String,
        message: String,
        relatedEntityId: Int? = null,
    ) {
        val notification =
            Notification(
                type = type,
                title = title,
                message = message,
                relatedEntityId = relatedEntityId,
            )

        notificationRepository.save(notification)
        logger.debug("Notification saved: [{}] {}", notification.type, notification.title)

        val notifyUi = {
            FxUtils.launchOnFxThread {
                if (uiListeners.isEmpty()) {
                    pendingToasts.add(notification)
                } else {
                    uiListeners.forEach { it(notification) }
                }
            }
        }

        if (TransactionSynchronizationManager.isActualTransactionActive()) {
            TransactionSynchronizationManager.registerSynchronization(
                object : TransactionSynchronization {
                    override fun afterCommit() {
                        notifyUi()
                    }
                },
            )
        } else {
            notifyUi()
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
