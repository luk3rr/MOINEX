/*
 * Filename: PdfStatementParser.kt
 * Created on: September 20, 2026
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 */

package org.moinex.ai.ingest

import org.apache.pdfbox.Loader
import org.apache.pdfbox.pdmodel.encryption.InvalidPasswordException
import org.apache.pdfbox.text.PDFTextStripper
import org.moinex.exception.MoinexException
import org.springframework.stereotype.Component
import java.nio.file.Path

@Component
class PdfStatementParser : StatementParser {
    override fun supports(fileName: String): Boolean = fileName.endsWith(".pdf", ignoreCase = true)

    override fun extractText(
        file: Path,
        password: String?,
    ): String =
        try {
            Loader.loadPDF(file.toFile(), password.orEmpty()).use { document -> PDFTextStripper().getText(document) }
        } catch (e: InvalidPasswordException) {
            throw MoinexException.PasswordProtectedFileException("Password protected file: ${file.fileName}")
        }
}
