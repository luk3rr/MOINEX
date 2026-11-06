/*
 * Filename: LlmHttpClientTest.kt
 * Created on: November 5, 2026
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 */

package org.moinex.ai.provider

import com.sun.net.httpserver.HttpServer
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import java.net.InetSocketAddress

class LlmHttpClientTest :
    BehaviorSpec({
        val seen = mutableListOf<String>()
        val server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
        server.createContext("/ok") { exchange ->
            seen += exchange.requestMethod + ":" + exchange.requestHeaders.getFirst("X-Test") + ":" +
                exchange.requestBody.readBytes().decodeToString()
            val body = """{"answer":42}""".toByteArray()
            exchange.sendResponseHeaders(200, body.size.toLong())
            exchange.responseBody.use { it.write(body) }
        }
        server.createContext("/fail") { exchange ->
            val body = "boom".toByteArray()
            exchange.sendResponseHeaders(500, body.size.toLong())
            exchange.responseBody.use { it.write(body) }
        }
        server.start()
        val base = "http://127.0.0.1:${server.address.port}"
        val client = LlmHttpClient("Test")

        afterSpec { server.stop(0) }

        Given("a provider endpoint that answers") {
            Then("post sends the body and headers and returns the answer") {
                client.post("m", "$base/ok?key=secret", mapOf("X-Test" to "1"), """{"q":1}""") shouldBe
                    """{"answer":42}"""
                seen.last() shouldBe """POST:1:{"q":1}"""
            }

            Then("get returns the answer") {
                client.get("m", "$base/ok", mapOf("X-Test" to "2")) shouldBe """{"answer":42}"""
                seen.last().startsWith("GET:2:") shouldBe true
            }
        }

        Given("a provider endpoint that fails") {
            Then("the status and the body are in the error") {
                val error = shouldThrow<LlmProviderException> { client.post("m", "$base/fail", emptyMap(), "{}") }
                error.message!! shouldContain "500"
                error.message!! shouldContain "boom"
            }
        }

        Given("an endpoint with a query string") {
            Then("the path used in logs never carries it") {
                LlmHttpClient.sanitizePath("https://x.test/v1/models?key=secret") shouldBe "https://x.test/v1/models"
            }
        }
    })
