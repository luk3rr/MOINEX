/*
 * Filename: StatementParser.kt
 * Created on: September 20, 2026
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 */

package org.moinex.ai.ingest

import java.nio.file.Path

interface StatementParser {
    fun supports(fileName: String): Boolean

    /**
     * Extracts the raw text of a statement file.
     *
     * @param password password used to open the file, when it is protected
     * @throws org.moinex.exception.MoinexException.PasswordProtectedFileException
     * when the file is protected and the password is missing or wrong
     */
    fun extractText(
        file: Path,
        password: String? = null,
    ): String
}
