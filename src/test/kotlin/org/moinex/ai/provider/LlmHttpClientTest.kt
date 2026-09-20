package org.moinex.ai.provider

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe

class LlmHttpClientTest :
    BehaviorSpec({
        Given("an endpoint with a query string carrying an API key (e.g. Gemini)") {
            val basePath = "https://generativelanguage.googleapis.com/v1beta/models/gemini-3.6-flash:generateContent"
            val endpoint = "$basePath?key=super-secret-key"

            When("sanitizing it for logging") {
                val sanitized = LlmHttpClient.sanitizePath(endpoint)

                Then("the query string, and the key with it, is stripped") {
                    sanitized shouldBe basePath
                    sanitized.contains("super-secret-key") shouldBe false
                }
            }
        }

        Given("an endpoint with no query string") {
            val endpoint = "https://api.openai.com/v1/chat/completions"

            When("sanitizing it for logging") {
                val sanitized = LlmHttpClient.sanitizePath(endpoint)

                Then("it is returned unchanged") {
                    sanitized shouldBe endpoint
                }
            }
        }
    })
