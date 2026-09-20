/*
 * Filename: PdfStatementParser.kt
 * Created on: September 20, 2026
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 */

package org.moinex.ai.ingest

import org.apache.pdfbox.Loader
import org.apache.pdfbox.text.PDFTextStripper
import org.springframework.stereotype.Component
import java.nio.file.Path

@Component
class PdfStatementParser : StatementParser {
    override fun supports(fileName: String): Boolean = fileName.endsWith(".pdf", ignoreCase = true)

    override fun extractText(file: Path): String =
        Loader.loadPDF(file.toFile()).use { document -> PDFTextStripper().getText(document) }
}
