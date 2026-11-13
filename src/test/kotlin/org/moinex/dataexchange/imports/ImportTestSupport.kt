/*
 * Filename: ImportTestSupport.kt
 * Created on: November 12, 2026
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 */

package org.moinex.dataexchange.imports

import org.dhatim.fastexcel.Workbook
import org.moinex.dataexchange.spec.Column
import org.moinex.dataexchange.tabular.ExportFormat
import org.moinex.dataexchange.tabular.TabularFiles
import java.nio.file.Files
import java.nio.file.Path

object ImportTestSupport {
    data class Sheet(
        val name: String,
        val columns: List<Column>,
        val rows: List<Map<String, Any?>>,
    )

    fun write(
        format: ExportFormat,
        sheets: List<Sheet>,
        meta: Map<String, String>? = null,
    ): Path {
        val file = Files.createTempFile("import", if (format == ExportFormat.XLSX) ".xlsx" else ".zip")
        val effectiveMeta =
            meta
                ?: mapOf(
                    "app" to "moinex",
                    "layout_version" to "1",
                    "entities" to sheets.joinToString(",") { it.name },
                )
        Files.newOutputStream(file).use { out ->
            TabularFiles.writer(format, out).use { writer ->
                writer.writeMeta(effectiveMeta)
                sheets.forEach { writer.writeSheet(it.name, it.columns, it.rows.asSequence()) }
            }
        }
        return file
    }

    fun writeXlsxWithFormula(): Path {
        val file = Files.createTempFile("formula", ".xlsx")
        Files.newOutputStream(file).use { out ->
            Workbook(out, "test", "1.0").use { wb ->
                val meta = wb.newWorksheet("_meta")
                meta.value(0, 0, "key")
                meta.value(0, 1, "value")
                meta.value(1, 0, "app")
                meta.value(1, 1, "moinex")
                meta.value(2, 0, "layout_version")
                meta.value(2, 1, "1")
                val ws = wb.newWorksheet("wallets")
                ws.value(0, 0, "name")
                ws.value(0, 1, "balance")
                ws.value(1, 0, "Main")
                ws.formula(1, 1, "1+1")
            }
        }
        return file
    }
}
