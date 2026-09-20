/*
 * Filename: CsvStatementParser.kt
 * Created on: September 20, 2026
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 */

package org.moinex.ai.ingest

import com.opencsv.CSVReader
import org.springframework.stereotype.Component
import java.io.FileReader
import java.nio.file.Path

@Component
class CsvStatementParser : StatementParser {
    override fun supports(fileName: String): Boolean = fileName.endsWith(".csv", ignoreCase = true)

    override fun extractText(file: Path): String =
        CSVReader(FileReader(file.toFile())).use { reader ->
            reader.readAll().joinToString("\n") { row -> row.joinToString("\t") }
        }
}
