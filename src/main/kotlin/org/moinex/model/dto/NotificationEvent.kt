package org.moinex.model.dto

import org.moinex.model.enums.NotificationType

data class NotificationEvent(
    val type: NotificationType,
    val title: String,
    val message: String,
    val relatedEntityId: Int? = null,
)
