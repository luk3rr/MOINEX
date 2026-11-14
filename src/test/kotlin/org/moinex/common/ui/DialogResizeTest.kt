/*
 * Filename: DialogResizeTest.kt
 * Created on: November 13, 2026
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 */

package org.moinex.common.ui

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import org.moinex.common.ui.DialogResize.Size
import org.moinex.common.ui.DialogResize.Zone

class DialogResizeTest :
    BehaviorSpec({
        Given("a card of 400 by 300") {
            When("looking at points of its border") {
                Then("each part of the border is recognised") {
                    DialogResize.zoneAt(200.0, 2.0, 400.0, 300.0) shouldBe Zone.N
                    DialogResize.zoneAt(200.0, 298.0, 400.0, 300.0) shouldBe Zone.S
                    DialogResize.zoneAt(2.0, 150.0, 400.0, 300.0) shouldBe Zone.W
                    DialogResize.zoneAt(398.0, 150.0, 400.0, 300.0) shouldBe Zone.E
                    DialogResize.zoneAt(3.0, 3.0, 400.0, 300.0) shouldBe Zone.NW
                    DialogResize.zoneAt(397.0, 3.0, 400.0, 300.0) shouldBe Zone.NE
                    DialogResize.zoneAt(3.0, 297.0, 400.0, 300.0) shouldBe Zone.SW
                    DialogResize.zoneAt(397.0, 297.0, 400.0, 300.0) shouldBe Zone.SE
                }

                Then("the inside and the outside are not the border") {
                    DialogResize.zoneAt(200.0, 150.0, 400.0, 300.0) shouldBe Zone.NONE
                    DialogResize.zoneAt(-1.0, 150.0, 400.0, 300.0) shouldBe Zone.NONE
                    DialogResize.zoneAt(401.0, 150.0, 400.0, 300.0) shouldBe Zone.NONE
                }
            }

            When("dragging edges and corners") {
                val start = Size(400.0, 300.0)
                val max = Size(1000.0, 700.0)

                Then("the centered card changes by twice the drag") {
                    DialogResize.resize(start, Zone.E, 50.0, 0.0, max = max) shouldBe Size(500.0, 300.0)
                    DialogResize.resize(start, Zone.W, -50.0, 0.0, max = max) shouldBe Size(500.0, 300.0)
                    DialogResize.resize(start, Zone.S, 0.0, 20.0, max = max) shouldBe Size(400.0, 340.0)
                    DialogResize.resize(start, Zone.N, 0.0, -20.0, max = max) shouldBe Size(400.0, 340.0)
                    DialogResize.resize(start, Zone.SE, 10.0, 10.0, max = max) shouldBe Size(420.0, 320.0)
                }

                Then("the size stays between the minimum and the area") {
                    DialogResize.resize(start, Zone.E, -500.0, 0.0, max = max) shouldBe
                        Size(DialogResize.MIN_WIDTH, 300.0)
                    DialogResize.resize(start, Zone.E, 900.0, 0.0, max = max) shouldBe Size(1000.0, 300.0)
                    DialogResize.resize(start, Zone.S, 0.0, 900.0, max = max) shouldBe Size(400.0, 700.0)
                    DialogResize.resize(start, Zone.S, 0.0, -900.0, max = max) shouldBe
                        Size(400.0, DialogResize.MIN_HEIGHT)
                }

                Then("an area smaller than the minimum does not break the limits") {
                    DialogResize.resize(start, Zone.E, 100.0, 0.0, max = Size(200.0, 100.0)) shouldBe
                        Size(DialogResize.MIN_WIDTH, DialogResize.MIN_HEIGHT)
                }
            }
        }
    })
