/*
 * Filename: CsvZipRoundTripTest.kt
 * Created on: November 12, 2026
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 */

package org.moinex.dataexchange.tabular

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import java.io.ByteArrayOutputStream
import java.math.BigDecimal
import java.util.zip.ZipInputStream

class CsvZipRoundTripTest :
    BehaviorSpec({
        Given("rows with accents, quotes, line breaks, formulas and nulls") {
            When("written as a CSV zip and read back") {
                val parsed = TabularRoundTripSupport.roundTrip(ExportFormat.CSV_ZIP)

                Then("every field is identical, including the decimal scale") {
                    TabularRoundTripSupport.assertSameAsRows(parsed)
                    (parsed[0].values["amount"] as BigDecimal).scale() shouldBe 2
                }
            }

            When("the zip is inspected") {
                val out = ByteArrayOutputStream()
                TabularFiles.writer(ExportFormat.CSV_ZIP, out).use { w ->
                    w.writeMeta(mapOf("app" to "moinex"))
                    w.writeSheet(
                        "transactions",
                        TabularRoundTripSupport.columns,
                        TabularRoundTripSupport.rows.asSequence(),
                    )
                }
                val text =
                    ZipInputStream(out.toByteArray().inputStream()).use { zip ->
                        generateSequence { zip.nextEntry }
                            .associate { it.name to zip.readBytes().toString(Charsets.UTF_8) }
                    }

                Then("it has a BOM, UTF-8 text and neutralised formulas") {
                    text.keys shouldBe setOf("_meta.csv", "transactions.csv")
                    text.getValue("transactions.csv").startsWith("") shouldBe true
                    text.getValue("transactions.csv").contains("'=SUM(A1)") shouldBe true
                    text.getValue("transactions.csv").contains("ação") shouldBe true
                }
            }
        }
    })
