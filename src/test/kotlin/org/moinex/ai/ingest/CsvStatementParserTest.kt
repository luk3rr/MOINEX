package org.moinex.ai.ingest

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import java.nio.file.Files

class CsvStatementParserTest :
    BehaviorSpec({
        val parser = CsvStatementParser()

        Given("a file with a .csv extension") {
            When("checking support") {
                Then("it is supported, case-insensitively") {
                    parser.supports("extrato.csv") shouldBe true
                    parser.supports("EXTRATO.CSV") shouldBe true
                    parser.supports("extrato.pdf") shouldBe false
                }
            }
        }

        Given("a CSV file with a header and quoted fields") {
            val file = Files.createTempFile("statement", ".csv")
            Files.writeString(file, "date,description,amount\n2026-09-01,\"Supermarket, downtown\",150.30\n")

            When("extracting text") {
                val text = parser.extractText(file)

                Then("rows are preserved with fields tab-separated, quoting resolved") {
                    text shouldBe "date\tdescription\tamount\n2026-09-01\tSupermarket, downtown\t150.30"
                }
            }
        }
    })
