/*
 * Filename: NoNetworkTest.kt
 * Created on: November 12, 2026
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 */

package org.moinex.dataexchange

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import java.nio.file.Files
import java.nio.file.Path

class NoNetworkTest :
    BehaviorSpec({
        Given("the data exchange sources") {
            val root = Path.of("src/main/kotlin/org/moinex/dataexchange")
            val forbidden =
                listOf(
                    "java.net",
                    "okhttp",
                    "HttpClient",
                    "RestTemplate",
                    "WebClient",
                    "YahooGateway",
                    "APIUtils",
                    "ProcessBuilder",
                )

            Then("none of them references a network, HTTP or process API") {
                val offenders =
                    Files
                        .walk(root)
                        .filter { it.toString().endsWith(".kt") }
                        .toList()
                        .filter { file -> Files.readString(file).let { text -> forbidden.any { it in text } } }
                offenders shouldBe emptyList()
            }
        }
    })
