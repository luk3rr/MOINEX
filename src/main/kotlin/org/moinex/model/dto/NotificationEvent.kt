/*
 * Filename: NotificationEvent.kt
 * Created on: April 26, 2026
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 */

package org.moinex.model.dto

import org.moinex.model.enums.NotificationType

data class NotificationEvent(
    val type: NotificationType,
    val title: String,
    val message: String,
    val relatedEntityId: Int? = null,
)
