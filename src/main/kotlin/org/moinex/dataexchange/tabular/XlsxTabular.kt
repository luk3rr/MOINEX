/*
 * Filename: XlsxTabular.kt
 * Created on: November 12, 2026
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 */

package org.moinex.dataexchange.tabular

import org.dhatim.fastexcel.Workbook
import org.dhatim.fastexcel.Worksheet
import org.dhatim.fastexcel.reader.Cell
import org.dhatim.fastexcel.reader.ReadableWorkbook
import org.moinex.dataexchange.codec.CellCodec
import org.moinex.dataexchange.spec.CellType
import org.moinex.dataexchange.spec.Column
import org.moinex.dataexchange.spec.RawRow
import java.io.OutputStream
import java.math.BigDecimal
import java.nio.file.Path
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import org.dhatim.fastexcel.reader.CellType as XlsxCellType

class XlsxTabularWriter(
    output: OutputStream,
) : TabularWriter {
    private val workbook = Workbook(output, "MOINEX", OOXML_APP_VERSION)

    override fun writeMeta(meta: Map<String, String>) {
        val sheet = workbook.newWorksheet(TabularLayout.META_SHEET)
        sheet.value(0, 0, "key")
        sheet.value(0, 1, "value")
        meta.entries.forEachIndexed { index, (k, v) ->
            sheet.value(index + 1, 0, k)
            sheet.value(index + 1, 1, v)
        }
        sheet.finish()
    }

    override fun writeSheet(
        name: String,
        columns: List<Column>,
        rows: Sequence<Map<String, Any?>>,
        onRow: () -> Unit,
    ) {
        val sheet = workbook.newWorksheet(name)
        columns.forEachIndexed { index, column ->
            sheet.value(0, index, column.name)
            sheet.style(0, index).bold().set()
        }
        var rowIndex = 1
        for (row in rows) {
            columns.forEachIndexed { index, column -> writeCell(sheet, rowIndex, index, column, row[column.name]) }
            rowIndex++
            onRow()
        }
        sheet.finish()
    }

    private fun writeCell(
        sheet: Worksheet,
        row: Int,
        col: Int,
        column: Column,
        value: Any?,
    ) {
        if (value == null) return
        when (column.type) {
            CellType.DECIMAL -> {
                val text = CellCodec.encode(value, column)!!
                sheet.value(row, col, BigDecimal(text))
            }
            CellType.INT -> sheet.value(row, col, (value as Number).toLong())
            CellType.DATE -> {
                sheet.value(row, col, value as LocalDate)
                sheet.style(row, col).format(DATE_FORMAT).set()
            }
            CellType.DATETIME -> {
                sheet.value(row, col, (value as LocalDateTime).truncatedTo(ChronoUnit.SECONDS))
                sheet.style(row, col).format(DATETIME_FORMAT).set()
            }
            else -> sheet.value(row, col, CellCodec.encode(value, column)!!)
        }
    }

    override fun close() {
        workbook.close()
    }

    private companion object {
        const val OOXML_APP_VERSION = "16.0300"
        const val DATE_FORMAT = "yyyy-mm-dd"
        const val DATETIME_FORMAT = "yyyy-mm-dd hh:mm:ss"
    }
}

class XlsxTabularReader(
    path: Path,
) : TabularReader {
    private val workbook = ReadableWorkbook(path.toFile())
    private val sheets = workbook.sheets.toList().associateBy { it.name }

    override fun meta(): Map<String, String>? {
        val sheet = sheets[TabularLayout.META_SHEET] ?: return null
        return sheet
            .read()
            .drop(1)
            .mapNotNull { row ->
                val key = row.getCellText(0)
                if (key.isNullOrEmpty()) null else key to (row.getCellText(1) ?: "")
            }.toMap()
    }

    override fun sheetNames(): Set<String> = sheets.keys - TabularLayout.META_SHEET

    override fun header(sheet: String): List<String> {
        val target = sheets[sheet] ?: return emptyList()
        return target.openStream().use { stream ->
            stream
                .findFirst()
                .map { row -> (0 until row.cellCount).map { (row.getCellText(it) ?: "").trim() } }
                .orElse(emptyList())
        }
    }

    override fun rows(
        sheet: String,
        columns: List<Column>,
    ): Sequence<RawRow> {
        val target = sheets[sheet] ?: return emptySequence()
        val types = columns.associate { it.name to it.type }
        return sequence {
            target.openStream().use { stream ->
                var header: List<String>? = null
                for (row in stream) {
                    if (header == null) {
                        header = (0 until row.cellCount).map { (row.getCellText(it) ?: "").trim() }
                        continue
                    }
                    val cells = mutableMapOf<String, String?>()
                    val formulas = mutableSetOf<String>()
                    header.forEachIndexed { index, name ->
                        val cell = row.getOptionalCell(index).orElse(null)
                        if (cell != null && cell.type == XlsxCellType.FORMULA) {
                            formulas.add(name)
                            cells[name] = null
                        } else {
                            cells[name] = cell?.let { textOf(it, types[name]) }?.takeIf { it.isNotEmpty() }
                        }
                    }
                    if (cells.values.all { it == null } && formulas.isEmpty()) continue
                    yield(RawRow(row.rowNum, cells, formulas))
                }
            }
        }
    }

    private fun textOf(
        cell: Cell,
        type: CellType?,
    ): String? =
        when (cell.type) {
            XlsxCellType.EMPTY -> null
            XlsxCellType.BOOLEAN -> cell.asBoolean().toString()
            XlsxCellType.NUMBER ->
                when (type) {
                    CellType.DATE -> cell.asDate().toLocalDate().toString()
                    CellType.DATETIME ->
                        cell
                            .asDate()
                            .truncatedTo(ChronoUnit.SECONDS)
                            .toString()
                            .let(::withSeconds)
                    else -> cell.asNumber().toPlainString()
                }
            XlsxCellType.ERROR -> cell.rawValue ?: "#ERROR"
            else -> cell.asString()
        }

    private fun withSeconds(iso: String): String = if (iso.length == 16) "$iso:00" else iso

    override fun close() {
        workbook.close()
    }
}
