/*
 * Filename: ToastBoardTest.kt
 * Created on: November 13, 2026
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 */

package org.moinex.ui.common

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import org.moinex.model.enums.NotificationType

class ToastBoardTest :
    BehaviorSpec({
        fun warning(text: String) = ToastSpec(NotificationType.WARNING, "Title", text, sticky = false)

        fun error(text: String) = ToastSpec(NotificationType.ERROR, "Title", text, sticky = true)

        Given("an empty board of three toasts") {
            val board = ToastBoard(maxVisible = 3)

            When("the same warning is shown five times in a row") {
                val first = board.offer(warning("Select an item"))
                repeat(4) { board.offer(warning("Select an item")) }

                Then("there is one toast, counted five times") {
                    first.shouldBeInstanceOf<ToastBoard.Offer.Added>()
                    board.visibleCount shouldBe 1
                    (first as ToastBoard.Offer.Added).entry.count shouldBe 5
                }
            }

            When("a warning and an error share the same text") {
                val other = ToastBoard(maxVisible = 3)
                other.offer(warning("Same"))
                val second = other.offer(error("Same"))

                Then("they are different toasts") {
                    second.shouldBeInstanceOf<ToastBoard.Offer.Added>()
                    other.visibleCount shouldBe 2
                }
            }
        }

        Given("a full board with a warning and two errors") {
            val board = ToastBoard(maxVisible = 3)
            val warningOffer = board.offer(warning("A")) as ToastBoard.Offer.Added
            board.offer(error("B"))
            board.offer(error("C"))

            When("a new warning arrives") {
                val offer = board.offer(warning("D"))

                Then("the old warning makes room and no error is removed") {
                    offer.shouldBeInstanceOf<ToastBoard.Offer.Added>()
                    (offer as ToastBoard.Offer.Added).evicted?.id shouldBe warningOffer.entry.id
                    board.visibleCount shouldBe 3
                }
            }
        }

        Given("a board full of errors") {
            val board = ToastBoard(maxVisible = 2)
            val first = board.offer(error("A")) as ToastBoard.Offer.Added
            board.offer(error("B"))

            When("another error arrives") {
                val offer = board.offer(error("C"))

                Then("it waits and no error is removed") {
                    offer shouldBe ToastBoard.Offer.Queued
                    board.visibleCount shouldBe 2
                    board.waitingCount shouldBe 1
                }

                And("when one error is closed") {
                    val promoted = board.remove(first.entry.id)

                    Then("the waiting one takes its place") {
                        promoted?.spec?.message shouldBe "C"
                        board.visibleCount shouldBe 2
                        board.waitingCount shouldBe 0
                    }
                }
            }
        }
    })
