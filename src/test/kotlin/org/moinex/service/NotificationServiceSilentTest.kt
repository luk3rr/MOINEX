/*
 * Filename: NotificationServiceSilentTest.kt
 * Created on: November 12, 2026
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 */

package org.moinex.service

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkObject
import io.mockk.verify
import kotlinx.coroutines.Job
import org.moinex.common.ClockProvider
import org.moinex.common.util.FxUtils
import org.moinex.model.enums.NotificationType
import org.moinex.repository.NotificationRepository

class NotificationServiceSilentTest :
    BehaviorSpec({
        val repository = mockk<NotificationRepository>()
        every { repository.save(any()) } answers { firstArg() }
        val service = NotificationService(repository, ClockProvider())

        beforeSpec {
            mockkObject(FxUtils)
            every { FxUtils.launchOnFxThread(any()) } returns mockk<Job>(relaxed = true)
        }

        afterSpec { unmockkObject(FxUtils) }

        Given("a block run without notifications") {
            When("notifications are sent inside it") {
                val result =
                    service.withoutNotifications {
                        service.send(NotificationType.SUCCESS, "t", "m")
                        42
                    }

                Then("nothing is saved and the block result is returned") {
                    result shouldBe 42
                    verify(exactly = 0) { repository.save(any()) }
                }
            }

            When("a notification is sent after the block, even when the block threw") {
                runCatching { service.withoutNotifications<Unit> { error("boom") } }
                service.send(NotificationType.SUCCESS, "t", "m")

                Then("it is saved again") {
                    verify(exactly = 1) { repository.save(any()) }
                }
            }
        }
    })
