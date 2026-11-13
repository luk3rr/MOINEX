/*
 * Filename: CellCodecTest.kt
 * Created on: November 12, 2026
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 */

package org.moinex.dataexchange.codec

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import org.moinex.dataexchange.Kind
import org.moinex.dataexchange.spec.CellType
import org.moinex.dataexchange.spec.Column
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime

class CellCodecTest :
    BehaviorSpec({
        val decimal = Column("amount", CellType.DECIMAL)
        val enumColumn = Column("type", CellType.ENUM, enumClass = Kind::class.java)

        fun value(
            raw: String?,
            column: Column,
        ) = (CellCodec.decode(raw, column) as CellCodec.Decoded.Value).value

        Given("decimal values") {
            Then("the scale is preserved and the text is plain") {
                CellCodec.encode(BigDecimal("1234.50"), decimal) shouldBe "1234.50"
                CellCodec.encode(BigDecimal("1E+3"), decimal) shouldBe "1000"
                value("1234.50", decimal) shouldBe BigDecimal("1234.50")
                value("-0.10", decimal) shouldBe BigDecimal("-0.10")
            }
            Then("comma, thousands and scientific notation are invalid") {
                listOf("12,5", "1.234,50", "1e3", "12x", "1.2.3").forEach {
                    CellCodec.decode(it, decimal).shouldBeInstanceOf<CellCodec.Decoded.Invalid>()
                }
            }
            Then("a Double is refused on encode") {
                shouldThrow<IllegalArgumentException> { CellCodec.encode(1.5, decimal) }
            }
        }

        Given("dates, enums, booleans and integers") {
            Then("they use the fixed ISO format") {
                CellCodec.encode(LocalDate.of(2026, 11, 12), Column("d", CellType.DATE)) shouldBe "2026-11-12"
                CellCodec.encode(
                    LocalDateTime.of(2026, 11, 12, 14, 30, 0, 999),
                    Column("d", CellType.DATETIME),
                ) shouldBe
                    "2026-11-12T14:30:00"
                value("2026-11-12T14:30:05", Column("d", CellType.DATETIME)) shouldBe
                    LocalDateTime.of(2026, 11, 12, 14, 30, 5)
                CellCodec
                    .decode(
                        "12/11/2026",
                        Column("d", CellType.DATE),
                    ).shouldBeInstanceOf<CellCodec.Decoded.Invalid>()
                CellCodec.encode(Kind.INCOME, enumColumn) shouldBe "INCOME"
                value("income", enumColumn) shouldBe Kind.INCOME
                CellCodec.decode("OTHER", enumColumn).shouldBeInstanceOf<CellCodec.Decoded.Invalid>()
                value("1", Column("b", CellType.BOOLEAN)) shouldBe true
                value("FALSE", Column("b", CellType.BOOLEAN)) shouldBe false
                value("12", Column("i", CellType.INT)) shouldBe 12
                CellCodec.decode("1.5", Column("i", CellType.INT)).shouldBeInstanceOf<CellCodec.Decoded.Invalid>()
            }
            Then("empty and null decode to null") {
                value("", decimal) shouldBe null
                value(null, decimal) shouldBe null
                CellCodec.encode(null, decimal) shouldBe null
            }
        }
    })
