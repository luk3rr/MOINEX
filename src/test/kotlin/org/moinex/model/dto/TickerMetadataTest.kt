/*
 * Filename: TickerMetadataTest.kt
 * Created on: November 6, 2026
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 */

package org.moinex.model.dto

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import org.json.JSONObject
import java.time.LocalDateTime

class TickerMetadataTest :
    BehaviorSpec({
        val at = LocalDateTime.of(2026, 11, 6, 10, 30, 15)

        Given("a complete record") {
            val metadata = TickerMetadata(currency = "BRL", historyUpdatedAt = at)

            When("it is written and read back") {
                val read = TickerMetadata.fromJson(metadata.toJson())

                Then("every field survives") {
                    read.currency shouldBe "BRL"
                    read.historyUpdatedAt shouldBe at
                }
            }

            When("it is written") {
                val json = JSONObject(metadata.toJson())

                Then("it holds only what has no column of its own, never the price, its moment or the site") {
                    json.keySet() shouldBe setOf("schema_version", "currency", "history_updated_at")
                }
            }
        }

        Given("a legacy or incomplete record") {
            When("the JSON is null, blank or not JSON") {
                Then("every field is null and nothing is thrown") {
                    listOf(null, "", "   ", "not json", "[1,2]").forEach {
                        val read = TickerMetadata.fromJson(it)
                        read.currency shouldBe null
                        read.historyUpdatedAt shouldBe null
                        read.other shouldBe emptyMap()
                    }
                }
            }

            When("fields are null or malformed") {
                val read = TickerMetadata.fromJson("""{"currency":null,"history_updated_at":"yesterday"}""")

                Then("they are read as null") {
                    read.currency shouldBe null
                    read.historyUpdatedAt shouldBe null
                }
            }

            When("only the version is present") {
                val read = TickerMetadata.fromJson("""{"schema_version":1}""")

                Then("the other fields are null") {
                    read.currency shouldBe null
                    read.historyUpdatedAt shouldBe null
                }
            }
        }

        Given("a record with a key this version does not know") {
            val json = """{"schema_version":2,"currency":"USD","dividend_yield":0.07}"""

            When("it is read and written again") {
                val read = TickerMetadata.fromJson(json)
                val written = JSONObject(read.toJson())

                Then("the unknown key is kept in other and the version is the current one") {
                    read.other.keys shouldBe setOf("dividend_yield")
                    written.getDouble("dividend_yield") shouldBe 0.07
                    written.getInt("schema_version") shouldBe TickerMetadata.SCHEMA_VERSION
                    written.getString("currency") shouldBe "USD"
                }
            }
        }
    })
