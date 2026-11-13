/*
 * Filename: CellCodecFormulaTest.kt
 * Created on: November 12, 2026
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 */

package org.moinex.dataexchange.codec

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe

class CellCodecFormulaTest :
    BehaviorSpec({
        Given("text that could be read as a formula") {
            Then("it is escaped and unescaped without loss") {
                listOf("=1+1", "+cmd", "-5", "@SUM(A1)", "\tx", "'=x", "''=x", "'abc", "plain", "", "a=b").forEach {
                    CellCodec.unescapeFormula(CellCodec.escapeFormula(it)) shouldBe it
                }
            }
            Then("dangerous prefixes get a leading apostrophe") {
                CellCodec.escapeFormula("=1+1") shouldBe "'=1+1"
                CellCodec.escapeFormula("@x") shouldBe "'@x"
                CellCodec.escapeFormula("-5") shouldBe "'-5"
                CellCodec.escapeFormula("plain") shouldBe "plain"
                CellCodec.escapeFormula("'abc") shouldBe "'abc"
            }
        }
    })
