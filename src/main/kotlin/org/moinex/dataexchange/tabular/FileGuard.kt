/*
 * Filename: FileGuard.kt
 * Created on: November 12, 2026
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 */

package org.moinex.dataexchange.tabular

import org.moinex.dataexchange.spec.Problem
import org.moinex.dataexchange.spec.ProblemCodes
import org.moinex.dataexchange.spec.Severity
import java.nio.file.Files
import java.nio.file.Path
import java.util.zip.ZipException
import java.util.zip.ZipFile

class FileGuard(
    private val maxFileBytes: Long = MAX_FILE_BYTES,
    private val maxEntries: Int = MAX_ENTRIES,
    private val maxUncompressedBytes: Long = MAX_UNCOMPRESSED_BYTES,
    private val maxRatio: Long = MAX_RATIO,
) {
    fun check(path: Path): Problem? {
        val name = path.fileName.toString().lowercase()
        if (!Files.isRegularFile(path)) return fileProblem(ProblemCodes.NOT_MOINEX_FILE, "not a file")
        if (!name.endsWith(".xlsx") && !name.endsWith(".zip")) {
            return fileProblem(ProblemCodes.NOT_MOINEX_FILE, "extension")
        }
        if (Files.size(path) > maxFileBytes) return fileProblem(ProblemCodes.FILE_TOO_LARGE, "file size")
        return try {
            ZipFile(path.toFile()).use { zip -> checkEntries(zip) }
        } catch (_: ZipException) {
            fileProblem(ProblemCodes.NOT_MOINEX_FILE, "not a zip")
        }
    }

    private fun checkEntries(zip: ZipFile): Problem? {
        var count = 0
        var total = 0L
        for (entry in zip.entries()) {
            count++
            if (count > maxEntries) return fileProblem(ProblemCodes.FILE_TOO_LARGE, "entries")
            val entryName = entry.name
            if (entryName.startsWith("/") || entryName.contains("..") || entryName.contains("\\")) {
                return fileProblem(ProblemCodes.FILE_TOO_LARGE, "unsafe entry name")
            }
            val size = entry.size
            val compressed = entry.compressedSize
            if (size > 0) {
                total += size
                if (total > maxUncompressedBytes) return fileProblem(ProblemCodes.FILE_TOO_LARGE, "uncompressed size")
                if (size > RATIO_MIN_BYTES && compressed > 0 && size / compressed > maxRatio) {
                    return fileProblem(ProblemCodes.FILE_TOO_LARGE, "compression ratio")
                }
            }
        }
        return null
    }

    private fun fileProblem(
        code: String,
        detail: String,
    ) = Problem("file", 0, null, code, Severity.ERROR, detail)

    companion object {
        const val MAX_FILE_BYTES = 50L * 1024 * 1024
        const val MAX_ENTRIES = 200
        const val MAX_UNCOMPRESSED_BYTES = 512L * 1024 * 1024
        const val MAX_RATIO = 200L
        const val RATIO_MIN_BYTES = 1024L * 1024
        const val MAX_ENTRY_BYTES = 256L * 1024 * 1024
    }
}
