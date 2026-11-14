/*
 * Filename: DialogStackTest.kt
 * Created on: November 13, 2026
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 */

package org.moinex.common.ui

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe

class DialogStackTest :
    BehaviorSpec({
        Given("a stack with three dialogs open") {
            val form = "form"
            val question = "question"
            val confirm = "confirm"
            val stack = DialogStack<String>()
            stack.push(form)
            stack.push(question)
            stack.push(confirm)

            Then("the last one opened is the top and the depth is three") {
                stack.depth shouldBe 3
                stack.top shouldBe confirm
                stack.isTop(confirm) shouldBe true
                stack.isTop(form) shouldBe false
            }

            When("closing the top one") {
                val removed = stack.closeFrom(confirm)

                Then("only it is removed and the one below becomes the top") {
                    removed shouldContainExactly listOf(confirm)
                    stack.depth shouldBe 2
                    stack.top shouldBe question
                }
            }

            When("closing the bottom one out of order") {
                val removed = stack.closeFrom(form)

                Then("everything above it goes too, from the top down") {
                    removed shouldContainExactly listOf(question, form)
                    stack.isEmpty() shouldBe true
                }
            }

            When("closing a dialog that is already closed") {
                val removed = stack.closeFrom(form)

                Then("nothing happens") {
                    removed.shouldBeEmpty()
                    stack.isEmpty() shouldBe true
                }
            }
        }

        Given("a dialog that is already open") {
            val stack = DialogStack<String>()
            val first = "first"
            stack.push(first)

            Then("opening it again is refused") {
                shouldThrow<IllegalStateException> { stack.push(first) }
            }
        }
    })
