/*
 * Filename: TabularFiles.kt
 * Created on: November 12, 2026
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 */

package org.moinex.dataexchange.tabular

import org.moinex.dataexchange.spec.DataExchangeException
import org.moinex.dataexchange.spec.Problem
import org.moinex.dataexchange.spec.ProblemCodes
import org.moinex.dataexchange.spec.Severity
import java.io.IOException
import java.io.OutputStream
import java.nio.file.Path
import java.util.zip.ZipFile

enum class ExportFormat { XLSX, CSV_ZIP }

object TabularFiles {
    fun writer(
        format: ExportFormat,
        output: OutputStream,
    ): TabularWriter =
        when (format) {
            ExportFormat.XLSX -> XlsxTabularWriter(output)
            ExportFormat.CSV_ZIP -> CsvZipTabularWriter(output)
        }

    fun open(
        path: Path,
        guard: FileGuard = FileGuard(),
    ): TabularReader {
        guard.check(path)?.let { throw DataExchangeException(it) }
        return try {
            if (isXlsx(path)) XlsxTabularReader(path) else CsvZipTabularReader(path)
        } catch (e: IOException) {
            throw DataExchangeException(
                Problem("file", 0, null, ProblemCodes.NOT_MOINEX_FILE, Severity.ERROR, "unreadable"),
                e,
            )
        }
    }

    private fun isXlsx(path: Path): Boolean = ZipFile(path.toFile()).use { it.getEntry("[Content_Types].xml") != null }
}
