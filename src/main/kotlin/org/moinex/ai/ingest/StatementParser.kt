/*
 * Filename: StatementParser.kt
 * Created on: September 20, 2026
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 */

package org.moinex.ai.ingest

import java.nio.file.Path

interface StatementParser {
    fun supports(fileName: String): Boolean

    fun extractText(file: Path): String
}
