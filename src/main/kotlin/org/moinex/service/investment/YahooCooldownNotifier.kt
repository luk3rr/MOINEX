/*
 * Filename: YahooCooldownNotifier.kt
 * Created on: November 6, 2026
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 */

package org.moinex.service.investment

import jakarta.annotation.PostConstruct
import org.moinex.common.constant.TranslationKeys
import org.moinex.common.util.APIUtils
import org.moinex.model.enums.NotificationType
import org.moinex.service.NotificationService
import org.moinex.service.PreferencesService
import org.springframework.stereotype.Component
import java.time.LocalDateTime

/**
 * Tells the user, once per cooldown, that Yahoo limited the requests and when the app will try again.
 */
@Component
class YahooCooldownNotifier(
    private val notificationService: NotificationService,
    private val preferencesService: PreferencesService,
) {
    @PostConstruct
    fun register() {
        APIUtils.yahooGateway.onCooldownStarted = ::notifyCooldown
    }

    fun notifyCooldown(until: LocalDateTime) {
        notificationService.send(
            type = NotificationType.WARNING,
            title = preferencesService.translate(TranslationKeys.YAHOO_COOLDOWN_TITLE),
            message =
                YahooCooldownMessage.text(preferencesService.translate(TranslationKeys.YAHOO_COOLDOWN_MESSAGE), until),
        )
    }
}
