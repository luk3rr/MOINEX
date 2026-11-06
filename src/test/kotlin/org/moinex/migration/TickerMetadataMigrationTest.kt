/*
 * Filename: TickerMetadataMigrationTest.kt
 * Created on: November 6, 2026
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 */

package org.moinex.migration

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import java.sql.Connection
import java.sql.DriverManager

class TickerMetadataMigrationTest :
    BehaviorSpec({
        val script =
            requireNotNull(
                TickerMetadataMigrationTest::class.java.getResource("/db/migration/V037__add_ticker_metadata.sql"),
            ).readText()
                .split(";")
                .map { it.trim() }
                .filter { it.isNotEmpty() }

        fun Connection.columns(): List<String> =
            createStatement().use { statement ->
                statement.executeQuery("PRAGMA table_info(ticker)").use { rs ->
                    buildList { while (rs.next()) add(rs.getString("name")) }
                }
            }

        Given("a database with tickers created before the migration") {
            DriverManager.getConnection("jdbc:sqlite::memory:").use { connection ->
                connection.createStatement().use {
                    it.execute(
                        """
                        CREATE TABLE ticker (
                            id INTEGER PRIMARY KEY,
                            symbol VARCHAR(255) NOT NULL,
                            current_unit_value NUMERIC NOT NULL,
                            last_update VARCHAR(255) NOT NULL,
                            domain VARCHAR(255)
                        )
                        """.trimIndent(),
                    )
                    it.execute(
                        "INSERT INTO ticker VALUES (1, 'AAA', 12.5, '2026-11-01T09:00:00', 'https://a.example')",
                    )
                }

                When("the migration runs") {
                    connection.createStatement().use { statement -> script.forEach { statement.execute(it) } }

                    Then("the ticker table has a metadata column") {
                        connection.columns().contains("metadata") shouldBe true
                    }

                    Then("existing tickers have no metadata yet") {
                        connection.createStatement().use { statement ->
                            statement.executeQuery("SELECT metadata FROM ticker WHERE id = 1").use { rs ->
                                rs.next() shouldBe true
                                rs.getString(1) shouldBe null
                            }
                        }
                    }

                    Then("the price, its moment and the site stay in their columns, untouched") {
                        connection.createStatement().use { statement ->
                            statement
                                .executeQuery("SELECT current_unit_value, last_update, domain FROM ticker WHERE id = 1")
                                .use { rs ->
                                    rs.next() shouldBe true
                                    rs.getDouble(1) shouldBe 12.5
                                    rs.getString(2) shouldBe "2026-11-01T09:00:00"
                                    rs.getString(3) shouldBe "https://a.example"
                                }
                        }
                    }
                }
            }
        }
    })
