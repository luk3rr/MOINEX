/*
 * Filename: XlsxRoundTripTest.kt
 * Created on: November 12, 2026
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 */

package org.moinex.dataexchange.tabular

import io.kotest.core.spec.style.BehaviorSpec

class XlsxRoundTripTest :
    BehaviorSpec({
        Given("rows with accents, quotes, line breaks, formulas and nulls") {
            When("written as XLSX and read back") {
                val parsed = TabularRoundTripSupport.roundTrip(ExportFormat.XLSX)

                Then("every value is equal, with typed dates and exact decimals") {
                    TabularRoundTripSupport.assertSameAsRows(parsed)
                }
            }
        }
    })
