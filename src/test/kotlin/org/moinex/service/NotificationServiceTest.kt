/*
 * Filename: NotificationServiceTest.kt
 * Created on: November 2, 2026
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 */

package org.moinex.service

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.slot
import io.mockk.unmockkObject
import io.mockk.verify
import kotlinx.coroutines.Job
import org.moinex.common.ClockProvider
import org.moinex.common.util.FxUtils
import org.moinex.model.Notification
import org.moinex.model.enums.NotificationType
import org.moinex.repository.NotificationRepository
import java.time.LocalDateTime

class NotificationServiceTest :
    BehaviorSpec({
        val repository = mockk<NotificationRepository>()
        val clockProvider = mockk<ClockProvider>()
        val service = NotificationService(repository, clockProvider)

        every { clockProvider.now() } returns LocalDateTime.of(2026, 11, 2, 10, 0)

        beforeSpec {
            mockkObject(FxUtils)
            every { FxUtils.launchOnFxThread(any()) } returns mockk<Job>(relaxed = true)
        }

        afterSpec { unmockkObject(FxUtils) }

        Given("a notification sent with an action url") {
            When("send is called") {
                val saved = slot<Notification>()
                every { repository.save(capture(saved)) } answers { saved.captured }

                service.send(
                    NotificationType.INFO,
                    "Title",
                    "Message",
                    actionUrl = "https://github.com/luk3rr/MOINEX/releases/tag/v2.22.0",
                )

                Then("the url is persisted with the notification") {
                    saved.captured.actionUrl shouldBe "https://github.com/luk3rr/MOINEX/releases/tag/v2.22.0"
                    verify(exactly = 1) { repository.save(any<Notification>()) }
                }
            }
        }

        Given("a notification sent without an action url") {
            When("send is called") {
                val saved = slot<Notification>()
                every { repository.save(capture(saved)) } answers { saved.captured }

                service.send(NotificationType.INFO, "Title", "Message")

                Then("the url stays null") {
                    saved.captured.actionUrl shouldBe null
                }
            }
        }
    })
