/*
 * Filename: GitHubReleaseClientTest.kt
 * Created on: November 2, 2026
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 */

package org.moinex.service.release

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import java.io.IOException
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse

class GitHubReleaseClientTest :
    BehaviorSpec({
        fun clientReturning(
            status: Int,
            body: String,
            captured: io.mockk.CapturingSlot<HttpRequest>? = null,
        ): GitHubReleaseClient {
            val response = mockk<HttpResponse<String>>()
            every { response.statusCode() } returns status
            every { response.body() } returns body
            val http = mockk<HttpClient>()
            every {
                http.send(captured?.let { capture(it) } ?: any<HttpRequest>(), any<HttpResponse.BodyHandler<String>>())
            } returns
                response
            return GitHubReleaseClient(http)
        }

        val validBody =
            """{"tag_name":"v2.22.0","html_url":"https://github.com/luk3rr/MOINEX/releases/tag/v2.22.0"}"""

        Given("a successful GitHub response") {
            When("fetching the latest release") {
                val request = slot<HttpRequest>()
                val result = clientReturning(200, validBody, request).latest()

                Then("tag and url are returned") {
                    result shouldBe
                        LatestRelease("v2.22.0", "https://github.com/luk3rr/MOINEX/releases/tag/v2.22.0")
                }
                Then("the request targets the releases/latest endpoint with a 10 s timeout") {
                    request.captured.uri().toString() shouldBe GitHubReleaseClient.LATEST_RELEASE_URL
                    request.captured.timeout().get() shouldBe GitHubReleaseClient.TIMEOUT
                }
            }
        }

        Given("unusable responses") {
            When("the status is not 200") {
                Then("null is returned") {
                    clientReturning(403, validBody).latest() shouldBe null
                }
            }
            When("the body is not valid JSON") {
                Then("null is returned") {
                    clientReturning(200, "<html>").latest() shouldBe null
                }
            }
            When("the fields are missing") {
                Then("null is returned") {
                    clientReturning(200, "{}").latest() shouldBe null
                }
            }
            When("the url belongs to another host") {
                Then("null is returned") {
                    val body = """{"tag_name":"v9.9.9","html_url":"https://evil.example.com/releases/v9.9.9"}"""
                    clientReturning(200, body).latest() shouldBe null
                }
            }
            When("the network fails") {
                Then("null is returned") {
                    val http = mockk<HttpClient>()
                    every { http.send(any<HttpRequest>(), any<HttpResponse.BodyHandler<String>>()) } throws
                        IOException("offline")
                    GitHubReleaseClient(http).latest() shouldBe null
                }
            }
        }
    })
