package org.moinex.ai.ingest

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import org.apache.pdfbox.pdmodel.PDDocument
import org.apache.pdfbox.pdmodel.PDPage
import org.apache.pdfbox.pdmodel.PDPageContentStream
import org.apache.pdfbox.pdmodel.font.PDType1Font
import org.apache.pdfbox.pdmodel.font.Standard14Fonts
import java.nio.file.Files

class PdfStatementParserTest :
    BehaviorSpec({
        val parser = PdfStatementParser()

        Given("a file with a .pdf extension") {
            When("checking support") {
                Then("it is supported, case-insensitively") {
                    parser.supports("fatura.pdf") shouldBe true
                    parser.supports("FATURA.PDF") shouldBe true
                    parser.supports("fatura.csv") shouldBe false
                }
            }
        }

        Given("a PDF with a text layer") {
            val file = Files.createTempFile("statement", ".pdf")
            PDDocument().use { document ->
                val page = PDPage()
                document.addPage(page)
                PDPageContentStream(document, page).use { stream ->
                    stream.beginText()
                    stream.setFont(PDType1Font(Standard14Fonts.FontName.HELVETICA), 12f)
                    stream.newLineAtOffset(50f, 700f)
                    stream.showText("Statement total: R$ 150.30")
                    stream.endText()
                }
                document.save(file.toFile())
            }

            When("extracting text") {
                val text = parser.extractText(file)

                Then("the embedded text is extracted") {
                    text shouldContain "Statement total: R$ 150.30"
                }
            }
        }
    })
