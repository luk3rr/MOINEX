/*
 * Filename: LargeFileBenchmarkTest.kt
 * Created on: November 13, 2026
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 */

package org.moinex.dataexchange

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.longs.shouldBeLessThan
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.moinex.dataexchange.export.ExportService
import org.moinex.dataexchange.imports.ImportExecutor
import org.moinex.dataexchange.imports.ImportPlanner
import org.moinex.dataexchange.spec.EntityCatalog
import org.moinex.dataexchange.tabular.ExportFormat
import org.moinex.service.NotificationService
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.TransactionStatus
import java.math.BigDecimal
import java.nio.file.Files
import java.time.LocalDateTime

class LargeFileBenchmarkTest :
    BehaviorSpec({
        val rows = 50_000
        val wallets =
            FakeWalletSpec(
                mutableListOf(
                    mapOf(
                        "name" to "Main",
                        "balance" to BigDecimal("1.00"),
                        "archived" to false,
                    ),
                ),
            )
        val transactions =
            FakeTransactionSpec(
                wallets,
                (0 until rows)
                    .map { i ->
                        FakeTransactionSpec.sample(amount = "${i % 500}.${(i % 90) + 10}", note = "row $i") +
                            mapOf("date" to LocalDateTime.of(2026, 1, 1, 0, 0, 0).plusMinutes(i.toLong()))
                    }.toMutableList(),
            )
        val catalog = EntityCatalog(listOf(wallets, transactions))
        val manager = mockk<PlatformTransactionManager>(relaxed = true)
        every { manager.getTransaction(any()) } returns mockk<TransactionStatus>(relaxed = true)
        val executor =
            ImportExecutor(catalog, manager, NotificationService(mockk(relaxed = true), mockk(relaxed = true)))

        listOf(ExportFormat.XLSX, ExportFormat.CSV_ZIP).forEach { format ->
            Given("$rows rows exported as $format") {
                When("exporting, planning and importing into empty storage") {
                    val file = Files.createTempFile("big", if (format == ExportFormat.XLSX) ".xlsx" else ".zip")
                    val started = System.nanoTime()
                    runBlocking { ExportService(catalog).export(setOf("wallets", "transactions"), format, file) }
                    val emptyWallets = FakeWalletSpec()
                    val emptyTransactions = FakeTransactionSpec(emptyWallets)
                    val emptyCatalog = EntityCatalog(listOf(emptyWallets, emptyTransactions))
                    val plan = runBlocking { ImportPlanner(emptyCatalog).plan(file) }
                    ImportExecutor(
                        emptyCatalog,
                        manager,
                        NotificationService(mockk(relaxed = true), mockk(relaxed = true)),
                    ).execute(plan)
                    val seconds = (System.nanoTime() - started) / 1_000_000_000

                    Then("it finishes within 30 seconds with every row present") {
                        seconds shouldBeLessThan 30
                        plan.hasErrors shouldBe false
                        emptyTransactions.stored.size shouldBe rows
                        plan.entities.first { it.id == "transactions" }.toAdd shouldBe rows
                    }
                }
            }
        }
    })
