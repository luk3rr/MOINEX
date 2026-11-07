package org.moinex.model.goal

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import org.moinex.factory.goal.GoalFactory
import java.math.BigDecimal

class GoalProgressTest :
    BehaviorSpec({
        fun BigDecimal.eq(text: String) = compareTo(BigDecimal(text)) == 0

        Given("a goal with R$ 125 saved of R$ 1.000") {
            val goal =
                GoalFactory.create(
                    initialBalance = BigDecimal("125.00"),
                    targetBalance = BigDecimal("1000.00"),
                )

            When("asking for the progress") {
                Then("it is 12.5%, not rounded to a whole point") {
                    goal.progressPercentage().eq("12.5") shouldBe true
                }
            }
        }

        Given("a goal with R$ 1 saved of R$ 3") {
            val goal = GoalFactory.create(initialBalance = BigDecimal("1.00"), targetBalance = BigDecimal("3.00"))

            When("asking for the progress") {
                Then("it keeps the precision of the division: 33.333333%") {
                    goal.progressPercentage().eq("33.333333") shouldBe true
                }
            }
        }

        Given("a goal with nothing saved") {
            val goal = GoalFactory.create(initialBalance = BigDecimal.ZERO, targetBalance = BigDecimal("500.00"))

            When("asking for the progress") {
                Then("it is zero") {
                    goal.progressPercentage().signum() shouldBe 0
                }
            }
        }
    })
