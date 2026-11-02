/*
 * Filename: ReleaseUpdateServiceTest.kt
 * Created on: November 2, 2026
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 */

package org.moinex.service.release

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.moinex.model.SemanticVersion
import org.moinex.model.enums.NotificationType
import org.moinex.service.NotificationService
import org.moinex.service.PreferencesService

class ReleaseUpdateServiceTest :
    BehaviorSpec({
        val client = mockk<GitHubReleaseClient>()
        val notificationService = mockk<NotificationService>(relaxed = true)
        val preferences = mockk<PreferencesService>(relaxed = true)
        val installed = SemanticVersion(2, 21, 0)
        val service = ReleaseUpdateService(client, notificationService, preferences) { installed }

        fun release(tag: String) = LatestRelease(tag, "https://github.com/luk3rr/MOINEX/releases/tag/$tag")

        fun resetMocks() {
            clearAllMocks()
            every { preferences.updateCheckEnabled } returns true
            every { preferences.lastNotifiedVersion } returns null
            every { preferences.translate(any()) } answers { firstArg<String>() + " {0}/{1}" }
        }

        Given("a newer release and notifications not yet sent") {
            When("checking for updates") {
                resetMocks()
                every { client.latest() } returns release("v2.22.0")
                val notified = service.checkForUpdate()

                Then("the user is notified with the release link and the version is remembered") {
                    notified shouldBe true
                    verify(exactly = 1) {
                        notificationService.send(
                            NotificationType.INFO,
                            any(),
                            match { it.contains("2.22.0") && it.contains("2.21.0") },
                            null,
                            "https://github.com/luk3rr/MOINEX/releases/tag/v2.22.0",
                        )
                    }
                    verify { preferences.lastNotifiedVersion = "2.22.0" }
                }
            }
        }

        Given("a version that was already notified") {
            When("the same release is still the latest") {
                resetMocks()
                every { preferences.lastNotifiedVersion } returns "2.22.0"
                every { client.latest() } returns release("v2.22.0")

                Then("nothing is sent") {
                    service.checkForUpdate() shouldBe false
                    verify(exactly = 0) { notificationService.send(any(), any(), any(), any(), any()) }
                }
            }
            When("an even newer release appears") {
                resetMocks()
                every { preferences.lastNotifiedVersion } returns "2.22.0"
                every { client.latest() } returns release("v2.23.0")

                Then("the user is notified again") {
                    service.checkForUpdate() shouldBe true
                }
            }
        }

        Given("a release that is not newer") {
            When("the remote version is equal or older") {
                resetMocks()
                Then("nothing is sent") {
                    every { client.latest() } returns release("v2.21.0")
                    service.checkForUpdate() shouldBe false
                    every { client.latest() } returns release("v2.20.0")
                    service.checkForUpdate() shouldBe false
                    verify(exactly = 0) { notificationService.send(any(), any(), any(), any(), any()) }
                }
            }
        }

        Given("the check is disabled") {
            When("checking for updates") {
                resetMocks()
                every { preferences.updateCheckEnabled } returns false

                Then("GitHub is not called and nothing is sent") {
                    service.checkForUpdate() shouldBe false
                    verify(exactly = 0) { client.latest() }
                    verify(exactly = 0) { notificationService.send(any(), any(), any(), any(), any()) }
                }
            }
        }

        Given("GitHub cannot be reached or the tag is unreadable") {
            When("the client returns null") {
                resetMocks()
                every { client.latest() } returns null

                Then("nothing is sent") {
                    service.checkForUpdate() shouldBe false
                    verify(exactly = 0) { notificationService.send(any(), any(), any(), any(), any()) }
                }
            }
            When("the tag is not a version") {
                resetMocks()
                every { client.latest() } returns release("nightly")

                Then("nothing is sent") {
                    service.checkForUpdate() shouldBe false
                }
            }
        }
    })
