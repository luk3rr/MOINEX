/*
 * Filename: FakeSpecs.kt
 * Created on: November 12, 2026
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 */

package org.moinex.dataexchange

import org.moinex.dataexchange.spec.ApplyContext
import org.moinex.dataexchange.spec.CellType
import org.moinex.dataexchange.spec.Column
import org.moinex.dataexchange.spec.EntitySpec
import org.moinex.dataexchange.spec.ParsedRow
import org.moinex.dataexchange.spec.Problem
import org.moinex.dataexchange.spec.ProblemCodes
import org.moinex.dataexchange.spec.ResolveContext
import org.moinex.dataexchange.spec.Severity
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime

enum class Kind { INCOME, EXPENSE }

class FakeWalletSpec(
    val stored: MutableList<Map<String, Any?>> = mutableListOf(),
) : EntitySpec {
    override val id = "wallets"
    override val group = "wallets"
    override val columns =
        listOf(
            Column("name", CellType.TEXT, required = true),
            Column("balance", CellType.DECIMAL, required = true),
            Column("archived", CellType.BOOLEAN),
        )

    override fun count() = stored.size.toLong()

    override fun export() = stored.asSequence()

    override fun naturalKey(row: ParsedRow) = row.text("name")!!

    override fun existingKeys(): Map<String, Int> = stored.groupingBy { it["name"] as String }.eachCount()

    override fun apply(
        row: ParsedRow,
        ctx: ApplyContext,
    ) {
        stored.add(row.values)
        ctx.remember(id, row.text("name")!!, row.values)
    }
}

class FakeTransactionSpec(
    private val wallets: FakeWalletSpec,
    val stored: MutableList<Map<String, Any?>> = mutableListOf(),
    private val failAt: Int = -1,
) : EntitySpec {
    override val id = "transactions"
    override val group = "wallets"
    override val dependsOn = listOf("wallets")
    override val refColumn = "ref"
    override val columns =
        listOf(
            Column("ref", CellType.TEXT),
            Column("wallet", CellType.TEXT, required = true),
            Column("type", CellType.ENUM, required = true, enumClass = Kind::class.java),
            Column("date", CellType.DATETIME, required = true),
            Column("day", CellType.DATE),
            Column("amount", CellType.DECIMAL, required = true),
            Column("note", CellType.TEXT),
            Column("count", CellType.INT),
        )

    override fun count() = stored.size.toLong()

    override fun export() = stored.asSequence()

    override fun naturalKey(row: ParsedRow) =
        listOf(
            row.text("wallet"),
            row.dateTime("date"),
            row.decimal("amount")?.toPlainString(),
            row.text("note"),
        ).joinToString("|")

    override fun existingKeys(): Map<String, Int> =
        stored
            .map {
                listOf(
                    it["wallet"],
                    it["date"],
                    (it["amount"] as BigDecimal).toPlainString(),
                    it["note"],
                ).joinToString("|")
            }.groupingBy { it }
            .eachCount()

    override fun validate(
        row: ParsedRow,
        ctx: ResolveContext,
    ): List<Problem> {
        val wallet = row.text("wallet")!!
        return if (ctx.exists("wallets", wallet)) {
            emptyList()
        } else {
            listOf(Problem(id, row.number, "wallet", ProblemCodes.UNRESOLVED_REFERENCE, Severity.ERROR, wallet))
        }
    }

    override fun apply(
        row: ParsedRow,
        ctx: ApplyContext,
    ) {
        if (failAt >= 0 && stored.size == failAt) error("boom")
        stored.add(row.values)
    }

    companion object {
        fun sample(
            wallet: String = "Main",
            amount: String = "10.50",
            note: String? = "lunch",
        ): Map<String, Any?> =
            mapOf(
                "ref" to null,
                "wallet" to wallet,
                "type" to Kind.EXPENSE,
                "date" to LocalDateTime.of(2026, 11, 12, 14, 30, 5),
                "day" to LocalDate.of(2026, 11, 12),
                "amount" to BigDecimal(amount),
                "note" to note,
                "count" to 3,
            )
    }
}
