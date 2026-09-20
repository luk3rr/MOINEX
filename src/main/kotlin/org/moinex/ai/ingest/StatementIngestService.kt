/*
 * Filename: StatementIngestService.kt
 * Created on: September 20, 2026
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 */

package org.moinex.ai.ingest

import org.springframework.stereotype.Service
import java.nio.file.Path

@Service
class StatementIngestService(
    private val parsers: List<StatementParser>,
) {
    fun extractText(file: Path): String {
        val fileName = file.fileName.toString()
        val parser =
            parsers.firstOrNull { it.supports(fileName) }
                ?: throw IllegalArgumentException("Unsupported file type: $fileName")

        return parser.extractText(file)
    }
}
