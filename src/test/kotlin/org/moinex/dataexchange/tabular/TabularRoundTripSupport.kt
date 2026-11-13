/*
 * Filename: TabularRoundTripSupport.kt
 * Created on: November 12, 2026
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 */

package org.moinex.dataexchange.tabular

import io.kotest.matchers.shouldBe
import org.moinex.dataexchange.FakeTransactionSpec
import org.moinex.dataexchange.Kind
import org.moinex.dataexchange.codec.CellCodec
import org.moinex.dataexchange.spec.Column
import org.moinex.dataexchange.spec.ParsedRow
import java.math.BigDecimal
import java.nio.file.Files
import java.nio.file.Path

object TabularRoundTripSupport {
    val columns: List<Column> = FakeTransactionSpec(org.moinex.dataexchange.FakeWalletSpec()).columns

    val rows: List<Map<String, Any?>> =
        listOf(
            FakeTransactionSpec.sample(),
            FakeTransactionSpec.sample(
                wallet = "Cartão, \"Principal\"",
                amount = "1234567890123.45",
                note = "ação\nlinha 2",
            ),
            FakeTransactionSpec.sample(amount = "-0.10", note = "=SUM(A1)"),
            FakeTransactionSpec.sample(amount = "7", note = null) + mapOf("type" to Kind.INCOME, "count" to null),
            FakeTransactionSpec.sample(note = "-5"),
        )

    fun roundTrip(format: ExportFormat): List<ParsedRow> {
        val file: Path = Files.createTempFile("moinex", if (format == ExportFormat.XLSX) ".xlsx" else ".zip")
        Files.newOutputStream(file).use { out ->
            TabularFiles.writer(format, out).use { writer ->
                writer.writeMeta(mapOf("app" to "moinex", "layout_version" to "1"))
                writer.writeSheet("transactions", columns, rows.asSequence())
            }
        }
        TabularFiles.open(file).use { reader ->
            reader.meta() shouldBe mapOf("app" to "moinex", "layout_version" to "1")
            reader.sheetNames() shouldBe setOf("transactions")
            reader.header("transactions") shouldBe columns.map { it.name }
            return reader
                .rows("transactions", columns)
                .map { raw ->
                    ParsedRow(
                        raw.number,
                        columns.associate { c ->
                            c.name to (CellCodec.decode(raw.cells[c.name], c) as CellCodec.Decoded.Value).value
                        },
                    )
                }.toList()
        }
    }

    fun assertSameAsRows(parsed: List<ParsedRow>) {
        parsed.size shouldBe rows.size
        parsed.forEachIndexed { index, row ->
            val expected = rows[index]
            columns.forEach { c ->
                val actual = row.values[c.name]
                val wanted = expected[c.name]
                if (wanted is BigDecimal) {
                    (actual as BigDecimal).compareTo(wanted) shouldBe 0
                } else {
                    actual shouldBe wanted
                }
            }
            row.number shouldBe index + 2
        }
    }
}
