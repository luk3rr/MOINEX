/*
 * Filename: YahooGatewayTest.kt
 * Created on: November 6, 2026
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 */

package org.moinex.common.util

import ch.qos.logback.classic.Logger
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.read.ListAppender
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import org.moinex.common.ClockProvider
import org.moinex.exception.MoinexException
import org.slf4j.LoggerFactory
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.util.concurrent.atomic.AtomicInteger

private class MutableClock(
    private var instant: Instant,
) : Clock() {
    override fun getZone(): ZoneId = ZoneOffset.UTC

    override fun withZone(zone: ZoneId?): Clock = this

    override fun instant(): Instant = instant

    fun advance(duration: Duration) {
        instant = instant.plus(duration)
    }
}

class YahooGatewayTest :
    BehaviorSpec({
        val rateLimited = JSONObject("""{"error":"too many requests","error_kind":"rate_limit"}""")
        val partiallyLimited = JSONObject("""{"AAA":{"price":1.0},"BBB":{"error":"x","error_kind":"rate_limit"}}""")
        val ok = JSONObject("""{"AAA":{"price":1.0}}""")

        fun newGateway(
            clock: MutableClock,
            maxConcurrentCalls: Int = 2,
        ) = YahooGateway(
            clockProvider = ClockProvider(clock),
            maxConcurrentCalls = maxConcurrentCalls,
            baseCooldown = Duration.ofMinutes(5),
            maxCooldown = Duration.ofMinutes(60),
        )

        Given("a gateway with no cooldown") {
            val clock = MutableClock(Instant.parse("2026-11-06T12:00:00Z"))
            val gateway = newGateway(clock)

            When("a call succeeds") {
                val result = runBlocking { gateway.call("get_stock_price.py", 3) { ok } }

                Then("the result is returned untouched and no cooldown starts") {
                    result.toString() shouldBe ok.toString()
                    gateway.cooldownUntil() shouldBe null
                }
            }
        }

        Given("a call that reports a rate limit") {
            val clock = MutableClock(Instant.parse("2026-11-06T12:00:00Z"))
            val gateway = newGateway(clock)
            val started = mutableListOf<LocalDateTime>()
            gateway.onCooldownStarted = { started.add(it) }
            val executed = AtomicInteger(0)

            val first =
                runBlocking {
                    gateway.call("get_stock_price.py", 2) {
                        executed.incrementAndGet()
                        rateLimited
                    }
                }

            When("the result is read") {
                Then("the caller still receives it and a 5 minute cooldown opens") {
                    first.getString("error_kind") shouldBe "rate_limit"
                    gateway.cooldownUntil() shouldBe LocalDateTime.of(2026, 11, 6, 12, 5)
                    started shouldBe listOf(LocalDateTime.of(2026, 11, 6, 12, 5))
                }
            }

            When("another call arrives during the cooldown") {
                clock.advance(Duration.ofMinutes(2))

                Then("it is refused without running the script") {
                    val error =
                        shouldThrow<MoinexException.YahooCooldownException> {
                            runBlocking {
                                gateway.call(
                                    "get_stock_price_history.py",
                                    1,
                                ) { executed.incrementAndGet().let { ok } }
                            }
                        }
                    error.until shouldBe LocalDateTime.of(2026, 11, 6, 12, 5)
                    executed.get() shouldBe 1
                }
            }

            When("the cooldown has ended") {
                clock.advance(Duration.ofMinutes(4))

                Then("calls run again and a success clears the state") {
                    gateway.cooldownUntil() shouldBe null
                    runBlocking { gateway.call("get_stock_price.py", 1) { ok } }
                    gateway.cooldownUntil() shouldBe null
                }
            }
        }

        Given("rate limits that repeat") {
            val clock = MutableClock(Instant.parse("2026-11-06T12:00:00Z"))
            val gateway = newGateway(clock)

            When("each one comes right after the previous cooldown ends") {
                val lengths = mutableListOf<Long>()
                repeat(6) {
                    val before = LocalDateTime.ofInstant(clock.instant(), ZoneOffset.UTC)
                    runBlocking { gateway.call("get_stock_price.py", 1) { rateLimited } }
                    val until = gateway.cooldownUntil()!!
                    lengths.add(Duration.between(before, until).toMinutes())
                    clock.advance(Duration.between(before, until).plusSeconds(1))
                }

                Then("the cooldown doubles up to the maximum") {
                    lengths shouldBe listOf(5L, 10L, 20L, 40L, 60L, 60L)
                }
            }

            When("a call succeeds after a rate limit") {
                runBlocking { gateway.call("get_stock_price.py", 1) { ok } }
                val before = LocalDateTime.ofInstant(clock.instant(), ZoneOffset.UTC)
                runBlocking { gateway.call("get_stock_price.py", 1) { rateLimited } }

                Then("the next cooldown starts again at the base length") {
                    Duration.between(before, gateway.cooldownUntil()!!).toMinutes() shouldBe 5L
                }
            }
        }

        Given("results and failures that do not mean a rate limit") {
            val clock = MutableClock(Instant.parse("2026-11-06T12:00:00Z"))
            val gateway = newGateway(clock)

            When("one symbol of a batch is limited") {
                runBlocking { gateway.call("get_stock_price.py", 2) { partiallyLimited } }

                Then("the whole gateway cools down") {
                    gateway.cooldownUntil() shouldBe LocalDateTime.of(2026, 11, 6, 12, 5)
                }
            }
        }

        Given("a script that fails") {
            val clock = MutableClock(Instant.parse("2026-11-06T12:00:00Z"))
            val gateway = newGateway(clock)

            When("the error is an invalid symbol") {
                Then("the error propagates and no cooldown starts") {
                    shouldThrow<MoinexException.APIFetchException> {
                        runBlocking {
                            gateway.call("get_stock_price.py", 1) {
                                throw MoinexException.APIFetchException("No data found for XYZ")
                            }
                        }
                    }
                    gateway.cooldownUntil() shouldBe null
                }
            }

            When("the error message names yfinance's rate limit error") {
                val other = newGateway(clock)
                runCatching {
                    runBlocking {
                        other.call("get_stock_price.py", 1) {
                            throw MoinexException.APIFetchException("[ERROR] error_kind=rate_limit: YFRateLimitError")
                        }
                    }
                }

                Then("it counts as a rate limit") {
                    other.cooldownUntil() shouldBe LocalDateTime.of(2026, 11, 6, 12, 5)
                }
            }

            When("the error message says 429") {
                Then("the error propagates and a cooldown starts") {
                    shouldThrow<MoinexException.APIFetchException> {
                        runBlocking {
                            gateway.call("get_stock_price.py", 1) {
                                throw MoinexException.APIFetchException("Script failed: HTTP Error 429")
                            }
                        }
                    }
                    gateway.cooldownUntil() shouldBe LocalDateTime.of(2026, 11, 6, 12, 5)
                }
            }
        }

        Given("two calls in flight that both report a rate limit") {
            val clock = MutableClock(Instant.parse("2026-11-06T12:00:00Z"))
            val gateway = newGateway(clock, maxConcurrentCalls = 2)
            val started = mutableListOf<LocalDateTime>()
            gateway.onCooldownStarted = { started.add(it) }

            When("both finish") {
                runBlocking {
                    val release = CompletableDeferred<Unit>()
                    val calls =
                        (1..2).map {
                            async {
                                gateway.call("get_stock_price.py", 1) {
                                    release.await()
                                    rateLimited
                                }
                            }
                        }
                    delay(100)
                    release.complete(Unit)
                    calls.forEach { it.await() }
                }

                Then("the cooldown opens once, is announced once and is not doubled") {
                    started shouldBe listOf(LocalDateTime.of(2026, 11, 6, 12, 5))
                    gateway.cooldownUntil() shouldBe LocalDateTime.of(2026, 11, 6, 12, 5)
                }
            }
        }

        Given("the log of the gateway") {
            val clock = MutableClock(Instant.parse("2026-11-06T12:00:00Z"))
            val gateway = newGateway(clock)
            val logger = LoggerFactory.getLogger(YahooGateway::class.java) as Logger
            val appender = ListAppender<ILoggingEvent>().also { it.start() }
            logger.addAppender(appender)

            When("a successful and a limited call run") {
                runBlocking { gateway.call("get_stock_price.py", 7) { ok } }
                runBlocking { gateway.call("get_stock_price_history.py", 1) { rateLimited } }

                Then("each call is logged with script, symbols, duration and result") {
                    val lines = appender.list.map { it.formattedMessage }
                    lines.any {
                        it.startsWith("[yahoo] script=get_stock_price.py symbols=7 duration=") &&
                            it.endsWith("result=ok")
                    } shouldBe true
                    lines.any {
                        it.startsWith("[yahoo] script=get_stock_price_history.py symbols=1 duration=") &&
                            it.endsWith("result=rate_limit")
                    } shouldBe true
                    logger.detachAppender(appender)
                }
            }
        }

        Given("a gateway that allows two calls at a time") {
            val clock = MutableClock(Instant.parse("2026-11-06T12:00:00Z"))
            val gateway = newGateway(clock, maxConcurrentCalls = 2)

            When("five calls start together") {
                val running = AtomicInteger(0)
                val peak = AtomicInteger(0)
                val release = CompletableDeferred<Unit>()

                runBlocking {
                    val calls =
                        (1..5).map {
                            async {
                                gateway.call("get_stock_price.py", 1) {
                                    val now = running.incrementAndGet()
                                    peak.updateAndGet { maxOf(it, now) }
                                    release.await()
                                    running.decrementAndGet()
                                    ok
                                }
                            }
                        }
                    delay(200)
                    peak.get() shouldBe 2
                    release.complete(Unit)
                    calls.forEach { it.await() }
                }

                Then("never more than two run at once and all five finish") {
                    peak.get() shouldBe 2
                    running.get() shouldBe 0
                }
            }
        }
    })
