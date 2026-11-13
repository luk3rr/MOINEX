/*
 * Filename: CsvZipTabular.kt
 * Created on: November 12, 2026
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 */

package org.moinex.dataexchange.tabular

import com.opencsv.CSVReader
import com.opencsv.CSVWriter
import org.moinex.dataexchange.codec.CellCodec
import org.moinex.dataexchange.spec.CellType
import org.moinex.dataexchange.spec.Column
import org.moinex.dataexchange.spec.RawRow
import java.io.BufferedWriter
import java.io.InputStream
import java.io.InputStreamReader
import java.io.OutputStream
import java.io.OutputStreamWriter
import java.nio.charset.StandardCharsets
import java.nio.file.Path
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import java.util.zip.ZipOutputStream

private const val BOM = 0xFEFF.toChar()

class CsvZipTabularWriter(
    output: OutputStream,
) : TabularWriter {
    private val zip = ZipOutputStream(output, StandardCharsets.UTF_8)

    override fun writeMeta(meta: Map<String, String>) {
        writeEntry(TabularLayout.META_SHEET) { csv ->
            csv.writeNext(arrayOf("key", "value"), false)
            meta.forEach { (k, v) -> csv.writeNext(arrayOf(k, CellCodec.escapeFormula(v)), false) }
        }
    }

    override fun writeSheet(
        name: String,
        columns: List<Column>,
        rows: Sequence<Map<String, Any?>>,
        onRow: () -> Unit,
    ) {
        writeEntry(name) { csv ->
            csv.writeNext(columns.map { it.name }.toTypedArray(), false)
            for (row in rows) {
                val cells =
                    columns
                        .map { column ->
                            val text = CellCodec.encode(row[column.name], column)
                            if (text != null && column.type == CellType.TEXT) CellCodec.escapeFormula(text) else text
                        }.toTypedArray()
                csv.writeNext(cells, false)
                onRow()
            }
        }
    }

    private fun writeEntry(
        name: String,
        block: (CSVWriter) -> Unit,
    ) {
        zip.putNextEntry(ZipEntry("$name.csv"))
        val writer = BufferedWriter(OutputStreamWriter(zip, StandardCharsets.UTF_8))
        writer.write(BOM.code)
        val csv = CSVWriter(writer, ',', '"', '"', "\n")
        block(csv)
        csv.flush()
        zip.closeEntry()
    }

    override fun close() {
        zip.close()
    }
}

class CsvZipTabularReader(
    path: Path,
) : TabularReader {
    private val zip = ZipFile(path.toFile())
    private val entries: Map<String, ZipEntry> =
        zip
            .entries()
            .asSequence()
            .filter { it.name.endsWith(".csv") }
            .associateBy { it.name.removeSuffix(".csv") }

    override fun meta(): Map<String, String>? {
        val entry = entries[TabularLayout.META_SHEET] ?: return null
        return readAll(entry)
            .drop(1)
            .filter { it.size >= 2 }
            .associate { it[0] to CellCodec.unescapeFormula(it[1]) }
    }

    override fun sheetNames(): Set<String> = entries.keys - TabularLayout.META_SHEET

    override fun header(sheet: String): List<String> {
        val entry = entries[sheet] ?: return emptyList()
        return open(entry).use { reader ->
            reader.readNext()?.map { it.removePrefix(BOM.toString()).trim() }
                ?: emptyList()
        }
    }

    override fun rows(
        sheet: String,
        columns: List<Column>,
    ): Sequence<RawRow> {
        val entry = entries[sheet] ?: return emptySequence()
        val textColumns = columns.filter { it.type == CellType.TEXT }.map { it.name }.toSet()
        return sequence {
            open(entry).use { reader ->
                val header = reader.readNext()?.map { it.removePrefix(BOM.toString()).trim() } ?: return@sequence
                var line = 1
                while (true) {
                    val values = reader.readNext() ?: break
                    line++
                    if (values.all { it.isEmpty() }) continue
                    val cells =
                        header
                            .mapIndexed { index, name ->
                                val raw = values.getOrNull(index)?.takeIf { it.isNotEmpty() }
                                name to if (raw != null && name in textColumns) CellCodec.unescapeFormula(raw) else raw
                            }.toMap()
                    yield(RawRow(line, cells))
                }
            }
        }
    }

    private fun readAll(entry: ZipEntry): List<Array<String>> = open(entry).use { it.readAll() }

    private fun open(entry: ZipEntry): CSVReader {
        val bounded = BoundedInputStream(zip.getInputStream(entry), FileGuard.MAX_ENTRY_BYTES)
        return CSVReader(InputStreamReader(bounded, StandardCharsets.UTF_8))
    }

    override fun close() {
        zip.close()
    }
}

internal class BoundedInputStream(
    private val delegate: InputStream,
    private val limit: Long,
) : InputStream() {
    private var read = 0L

    override fun read(): Int {
        val value = delegate.read()
        if (value >= 0) count(1)
        return value
    }

    override fun read(
        b: ByteArray,
        off: Int,
        len: Int,
    ): Int {
        val n = delegate.read(b, off, len)
        if (n > 0) count(n)
        return n
    }

    private fun count(n: Int) {
        read += n
        if (read > limit) throw java.io.IOException("Entry exceeds the size limit")
    }

    override fun close() {
        delegate.close()
    }
}
