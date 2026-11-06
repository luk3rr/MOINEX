/*
 * Filename: YahooCooldownNotifierTest.kt
 * Created on: November 6, 2026
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 */

package org.moinex.service.investment

import io.kotest.core.spec.style.BehaviorSpec
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import org.moinex.common.ClockProvider
import org.moinex.common.constant.TranslationKeys
import org.moinex.common.util.APIUtils
import org.moinex.common.util.YahooGateway
import org.moinex.model.enums.NotificationType
import org.moinex.service.NotificationService
import org.moinex.service.PreferencesService
import java.time.LocalDateTime

class YahooCooldownNotifierTest :
    BehaviorSpec({
        val notificationService = mockk<NotificationService>(relaxed = true)
        val preferencesService = mockk<PreferencesService>()
        val original = APIUtils.yahooGateway

        afterSpec { APIUtils.yahooGateway = original }
        afterContainer { clearAllMocks(answers = true) }

        fun stubTranslations() {
            every { preferencesService.translate(TranslationKeys.YAHOO_COOLDOWN_TITLE) } returns "Limited"
            every { preferencesService.translate(TranslationKeys.YAHOO_COOLDOWN_MESSAGE) } returns "Try again after %s."
        }

        Given("a notifier") {
            val notifier = YahooCooldownNotifier(notificationService, preferencesService)

            When("a cooldown that ends at 14:35 starts") {
                stubTranslations()

                notifier.notifyCooldown(LocalDateTime.of(2026, 11, 6, 14, 35))

                Then("a warning tells the user when the app will try again") {
                    verify(exactly = 1) {
                        notificationService.send(
                            type = NotificationType.WARNING,
                            title = "Limited",
                            message = "Try again after 14:35.",
                        )
                    }
                }
            }

            When("it is registered and the gateway meets a rate limit") {
                stubTranslations()
                APIUtils.yahooGateway = YahooGateway(ClockProvider())
                notifier.register()

                runBlocking {
                    APIUtils.yahooGateway.call("get_stock_price.py", 1) {
                        JSONObject("""{"error":"x","error_kind":"rate_limit"}""")
                    }
                }

                Then("the user is warned exactly once") {
                    verify(exactly = 1) {
                        notificationService.send(
                            type = NotificationType.WARNING,
                            title = "Limited",
                            message = any(),
                            relatedEntityId = any(),
                            actionUrl = any(),
                        )
                    }
                }
            }
        }
    })
