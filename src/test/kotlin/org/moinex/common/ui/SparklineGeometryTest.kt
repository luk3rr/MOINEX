package org.moinex.common.ui

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.doubles.plusOrMinus
import io.kotest.matchers.shouldBe
import java.math.BigDecimal

class SparklineGeometryTest :
    BehaviorSpec({
        fun values(vararg numbers: Int) = numbers.map { BigDecimal(it) }

        Given("a series of prices") {
            val points = SparklineGeometry.points(values(10, 20, 15), width = 104.0, height = 24.0, padding = 2.0)

            Then("the points are spread evenly from the left padding to the right padding") {
                points.map { it.first } shouldBe listOf(2.0, 52.0, 102.0)
            }

            Then("the highest price is at the top and the lowest at the bottom") {
                points[1].second shouldBe (2.0 plusOrMinus 1e-9)
                points[0].second shouldBe (22.0 plusOrMinus 1e-9)
                points[2].second shouldBe (12.0 plusOrMinus 1e-9)
            }
        }

        Given("a series that never changes") {
            Then("it is drawn flat in the middle") {
                SparklineGeometry.points(values(5, 5, 5), 100.0, 24.0).map { it.second } shouldBe
                    listOf(12.0, 12.0, 12.0)
            }
        }

        Given("fewer than two prices") {
            Then("there is no line") {
                SparklineGeometry.points(values(5), 100.0, 24.0).shouldBeEmpty()
                SparklineGeometry.points(emptyList(), 100.0, 24.0).shouldBeEmpty()
            }
        }

        Given("the period ends higher, lower or equal") {
            Then("the trend follows the first and last prices, not the highs and lows") {
                SparklineGeometry.trend(values(10, 30, 12)) shouldBe SparklineTrend.UP
                SparklineGeometry.trend(values(10, 3, 9)) shouldBe SparklineTrend.DOWN
                SparklineGeometry.trend(values(10, 30, 10)) shouldBe SparklineTrend.FLAT
                SparklineGeometry.trend(values(10)) shouldBe SparklineTrend.FLAT
            }
        }
    })
