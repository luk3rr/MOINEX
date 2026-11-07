package org.moinex.common.ui

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import java.time.LocalDate

class CrosshairMathTest :
    BehaviorSpec({
        fun day(text: String) = LocalDate.parse(text)

        val days = listOf(day("2026-10-01"), day("2026-10-02"), day("2026-10-05"), day("2026-10-06"))

        Given("the days of a series") {
            Then("the mouse snaps to the closest day") {
                CrosshairMath.nearestDay(days, day("2026-10-02").toEpochDay() + 0.4) shouldBe day("2026-10-02")
                CrosshairMath.nearestDay(days, day("2026-10-02").toEpochDay() + 1.6) shouldBe day("2026-10-05")
            }

            Then("a tie goes to the earlier day, and positions beyond the ends go to the first and last day") {
                CrosshairMath.nearestDay(days, day("2026-10-02").toEpochDay() + 1.5) shouldBe day("2026-10-02")
                CrosshairMath.nearestDay(days, day("2026-01-01").toEpochDay().toDouble()) shouldBe day("2026-10-01")
                CrosshairMath.nearestDay(days, day("2027-01-01").toEpochDay().toDouble()) shouldBe day("2026-10-06")
            }

            Then("there is no closest day when there are no days") {
                CrosshairMath.nearestDay(emptyList(), 1.0) shouldBe null
            }
        }

        Given("a series of points") {
            val points = listOf(day("2026-10-02") to 10.0, day("2026-10-05") to 12.0, day("2026-10-06") to 11.0)

            Then("a day with a point gives its value") {
                CrosshairMath.valueOn(points, day("2026-10-05")) shouldBe 12.0
            }

            Then("a day without a point gives the last one before it, and nothing before the start") {
                CrosshairMath.valueOn(points, day("2026-10-04")) shouldBe 10.0
                CrosshairMath.valueOn(points, day("2026-10-20")) shouldBe 11.0
                CrosshairMath.valueOn(points, day("2026-10-01")) shouldBe null
                CrosshairMath.valueOn(emptyList(), day("2026-10-01")) shouldBe null
            }
        }

        Given("notes dated on days with and without data") {
            val notes =
                listOf(
                    day("2026-10-03") to "buy on a Saturday",
                    day("2026-10-05") to "sell on Monday",
                    day("2026-10-01") to "buy on the first day",
                )

            Then("a note shows on the first day with data on or after its date") {
                CrosshairMath.notesFor(notes, day("2026-10-02"), day("2026-10-05")) shouldContainExactly
                    listOf("buy on a Saturday", "sell on Monday")
                CrosshairMath.notesFor(notes, day("2026-10-05"), day("2026-10-06")) shouldContainExactly emptyList()
            }

            Then("the first day takes everything up to itself") {
                CrosshairMath.notesFor(notes, null, day("2026-10-01")) shouldContainExactly
                    listOf("buy on the first day")
            }

            Then("the day before is the last day of the series that comes earlier") {
                CrosshairMath.previousDay(days, day("2026-10-05")) shouldBe day("2026-10-02")
                CrosshairMath.previousDay(days, day("2026-10-01")) shouldBe null
            }
        }

        Given("the display positions of the columns of a category axis") {
            val positions = listOf(30.0, 80.0, 130.0, 180.0)

            Then("the mouse snaps to the closest column") {
                CrosshairMath.nearestIndex(positions, 40.0) shouldBe 0
                CrosshairMath.nearestIndex(positions, 60.0) shouldBe 1
                CrosshairMath.nearestIndex(positions, 150.0) shouldBe 2
            }

            Then("a tie goes to the earlier column, and positions beyond the ends go to the first and last") {
                CrosshairMath.nearestIndex(positions, 55.0) shouldBe 0
                CrosshairMath.nearestIndex(positions, -500.0) shouldBe 0
                CrosshairMath.nearestIndex(positions, 900.0) shouldBe 3
            }

            Then("columns without a position are ignored") {
                CrosshairMath.nearestIndex(listOf(Double.NaN, 80.0, Double.NaN), 5.0) shouldBe 1
                CrosshairMath.nearestIndex(listOf(Double.NaN), 5.0) shouldBe null
            }

            Then("there is no column when there are no positions") {
                CrosshairMath.nearestIndex(emptyList(), 1.0) shouldBe null
            }
        }
    })
