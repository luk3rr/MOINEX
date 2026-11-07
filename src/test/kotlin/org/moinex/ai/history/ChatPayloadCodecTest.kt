/*
 * Filename: ChatPayloadCodecTest.kt
 * Created on: November 7, 2026
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 */

package org.moinex.ai.history

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.mockk.mockkClass
import org.moinex.ai.chat.ToolCall
import org.moinex.ai.operation.AppOperation
import org.moinex.ai.operation.OperationRegistry
import org.moinex.model.dto.ai.IgnoredLine
import org.moinex.service.matching.Match
import org.moinex.service.matching.MatchStatus
import org.moinex.service.matching.MatchTargetType
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider
import org.springframework.core.type.filter.AssignableTypeFilter
import org.springframework.stereotype.Component
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth
import kotlin.reflect.KClass
import kotlin.reflect.KType
import kotlin.reflect.full.primaryConstructor
import kotlin.reflect.full.starProjectedType

class ChatPayloadCodecTest :
    BehaviorSpec({
        fun operations(): List<AppOperation<*>> {
            val scanner = ClassPathScanningCandidateComponentProvider(false)
            scanner.addIncludeFilter(AssignableTypeFilter(AppOperation::class.java))
            return scanner
                .findCandidateComponents("org.moinex.ai.operation")
                .map { Class.forName(it.beanClassName).kotlin }
                .filter { !it.isAbstract && it.java.isAnnotationPresent(Component::class.java) }
                .map { type ->
                    val constructor = type.primaryConstructor ?: error("No primary constructor: $type")
                    val arguments =
                        constructor.parameters.map {
                            mockkClass(
                                it.type.classifier as KClass<*>,
                                relaxed = true,
                            )
                        }
                    constructor.call(*arguments.toTypedArray()) as AppOperation<*>
                }
        }

        fun sample(type: KType): Any? {
            val klass = type.classifier as KClass<*>
            return when {
                klass == String::class -> "sample"
                klass == Int::class -> 7
                klass == Long::class -> 7L
                klass == Double::class -> 1.5
                klass == Boolean::class -> true
                klass == BigDecimal::class -> BigDecimal("12.34")
                klass == LocalDate::class -> LocalDate.of(2026, 11, 6)
                klass == LocalDateTime::class -> LocalDateTime.of(2026, 11, 6, 10, 30)
                klass == YearMonth::class -> YearMonth.of(2026, 11)
                klass == List::class -> listOf(sample(type.arguments.first().type!!))
                klass == Set::class -> setOf(sample(type.arguments.first().type!!))
                klass.java.isEnum -> klass.java.enumConstants.first()
                else -> {
                    val constructor = klass.primaryConstructor ?: error("No sample for $klass")
                    constructor.call(*constructor.parameters.map { sample(it.type) }.toTypedArray())
                }
            }
        }

        val all = operations()
        val codec = ChatPayloadCodec(OperationRegistry(all))

        Given("every operation the assistant can propose") {
            When("a sample payload of each one is written and read back") {
                Then("there is at least the set of operations known today") {
                    (all.size >= 30) shouldBe true
                }

                all.forEach { operation ->
                    Then("${operation.name} comes back equal") {
                        val payloadClass = codec.payloadClassOf(operation.name)!!
                        val payload = sample(payloadClass.kotlin.starProjectedType)!!
                        val json = codec.payloadToJson(payload)

                        codec.payloadFromJson(operation.name, json) shouldBe payload
                    }
                }
            }
        }

        Given("a payload with dates and money") {
            val payload = sample(codec.payloadClassOf("create_wallet_transaction")!!.kotlin.starProjectedType)!!

            When("it is written") {
                val json = codec.payloadToJson(payload)

                Then("dates are ISO-8601 text and money is a plain number") {
                    json.contains("\"2026-11-06\"") shouldBe true
                    json.contains("12.34") shouldBe true
                    Regex("[0-9][eE][+-]?[0-9]").containsMatchIn(json) shouldBe false
                }
            }
        }

        Given("a stored payload with a field that no longer exists") {
            val payload = sample(codec.payloadClassOf("create_wallet_transaction")!!.kotlin.starProjectedType)!!
            val json = codec.payloadToJson(payload).replaceFirst("{", "{\"removedField\":1,")

            When("it is read") {
                val read = codec.payloadFromJson("create_wallet_transaction", json)

                Then("the unknown field is ignored and the payload is rebuilt") { read shouldBe payload }
            }
        }

        Given("a stored payload of an operation that no longer exists") {
            When("it is read") {
                val read = codec.payloadFromJson("operation_from_the_future", """{"a":1}""")

                Then("it comes back as raw text, to be shown but never applied") {
                    read.shouldBeInstanceOf<RawPayload>()
                    read.json shouldBe """{"a":1}"""
                    codec.payloadToJson(read) shouldBe """{"a":1}"""
                }
            }
        }

        Given("a stored payload that no longer fits its operation") {
            When("it is read") {
                val read = codec.payloadFromJson("create_wallet_transaction", """{"date":"not a date"}""")

                Then("it comes back as raw text instead of failing") {
                    read.shouldBeInstanceOf<RawPayload>()
                }
            }
        }

        Given("tool calls, matches, ignored lines and plain lists") {
            val calls = listOf(ToolCall("c1", "get_net_worth", "{\"a\":1}"), ToolCall("c2", "x", "{}"))
            val match =
                Match(
                    targetType = MatchTargetType.WALLET_TRANSACTION,
                    targetId = 3,
                    status = MatchStatus.values().first(),
                    score = 0.8,
                    reasons = listOf("same amount"),
                    existingDescription = "Mercado",
                    date = LocalDate.of(2026, 11, 2),
                    amount = BigDecimal("50.10"),
                )
            val ignored = IgnoredLine("Mercado 50,10", MatchTargetType.WALLET_TRANSACTION, 3, listOf("same"))

            Then("each one is read back equal") {
                codec.toolCallsFromJson(codec.toolCallsToJson(calls)) shouldBe calls
                codec.matchesFromJson(codec.matchesToJson(listOf(match))) shouldBe listOf(match)
                codec.ignoredLinesFromJson(codec.ignoredLinesToJson(listOf(ignored))) shouldBe listOf(ignored)
                codec.stringsFromJson(codec.stringsToJson(listOf("a", "b\"c"))) shouldBe listOf("a", "b\"c")
                codec.matchesFromJson(codec.matchesToJson(emptyList())).shouldBeEmpty()
            }

            Then("the tool call text keeps the format the first version of the history wrote") {
                codec.toolCallsFromJson("""[{"id":"c1","toolName":"t","argumentsJson":"{}"}]""") shouldBe
                    listOf(ToolCall("c1", "t", "{}"))
            }
        }
    })
