/*
 * Filename: XlsxLibSmokeTest.kt
 * Created on: November 12, 2026
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 */

package org.moinex.dataexchange.tabular

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import org.dhatim.fastexcel.Workbook
import org.dhatim.fastexcel.reader.ReadableWorkbook
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream

class XlsxLibSmokeTest :
    BehaviorSpec({
        Given("a workbook written with fastexcel") {
            val out = ByteArrayOutputStream()
            Workbook(out, "moinex", "1.0").use { wb ->
                val ws = wb.newWorksheet("sheet")
                ws.value(0, 0, "name")
                ws.value(1, 0, "ação")
                ws.value(1, 1, java.math.BigDecimal("12.50"))
            }

            When("it is read back with fastexcel-reader") {
                val rows =
                    ReadableWorkbook(ByteArrayInputStream(out.toByteArray())).use { wb ->
                        wb.firstSheet.read().map { r -> r.getCellText(0) }
                    }

                Then("the cells survive") {
                    rows shouldBe listOf("name", "ação")
                }
            }
        }
    })
