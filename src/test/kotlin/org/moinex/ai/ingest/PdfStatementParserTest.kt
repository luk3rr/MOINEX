package org.moinex.ai.ingest

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import org.apache.pdfbox.pdmodel.PDDocument
import org.apache.pdfbox.pdmodel.PDPage
import org.apache.pdfbox.pdmodel.PDPageContentStream
import org.apache.pdfbox.pdmodel.encryption.AccessPermission
import org.apache.pdfbox.pdmodel.encryption.StandardProtectionPolicy
import org.apache.pdfbox.pdmodel.font.PDType1Font
import org.apache.pdfbox.pdmodel.font.Standard14Fonts
import org.moinex.exception.MoinexException
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

        Given("a password protected PDF") {
            val file = Files.createTempFile("protected-statement", ".pdf")
            PDDocument().use { document ->
                val page = PDPage()
                document.addPage(page)
                PDPageContentStream(document, page).use { stream ->
                    stream.beginText()
                    stream.setFont(PDType1Font(Standard14Fonts.FontName.HELVETICA), 12f)
                    stream.newLineAtOffset(50f, 700f)
                    stream.showText("Protected total: R$ 99.90")
                    stream.endText()
                }
                document.protect(
                    StandardProtectionPolicy("owner-secret", "user-secret", AccessPermission()).apply {
                        encryptionKeyLength = 256
                    },
                )
                document.save(file.toFile())
            }

            When("extracting text without a password") {
                Then("it reports the file as password protected") {
                    shouldThrow<MoinexException.PasswordProtectedFileException> {
                        parser.extractText(file)
                    }
                }
            }

            When("extracting text with a wrong password") {
                Then("it reports the file as password protected") {
                    shouldThrow<MoinexException.PasswordProtectedFileException> {
                        parser.extractText(file, "wrong")
                    }
                }
            }

            When("extracting text with the right password") {
                val text = parser.extractText(file, "user-secret")

                Then("the embedded text is extracted") {
                    text shouldContain "Protected total: R$ 99.90"
                }
            }
        }
    })
