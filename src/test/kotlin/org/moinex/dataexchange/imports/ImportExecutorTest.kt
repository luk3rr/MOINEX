/*
 * Filename: ImportExecutorTest.kt
 * Created on: November 12, 2026
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 */

package org.moinex.dataexchange.imports

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.runBlocking
import org.moinex.dataexchange.FakeTransactionSpec
import org.moinex.dataexchange.FakeWalletSpec
import org.moinex.dataexchange.spec.EntityCatalog
import org.moinex.dataexchange.tabular.ExportFormat
import org.moinex.service.NotificationService
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.TransactionStatus
import java.math.BigDecimal

class ImportExecutorTest :
    BehaviorSpec({
        val walletCols = FakeWalletSpec().columns
        val txCols = FakeTransactionSpec(FakeWalletSpec()).columns

        fun wallet(name: String) = mapOf("name" to name, "balance" to BigDecimal("1.00"), "archived" to false)

        val notifications = NotificationService(mockk(relaxed = true), mockk(relaxed = true))

        fun manager(): PlatformTransactionManager {
            val manager = mockk<PlatformTransactionManager>(relaxed = true)
            io.mockk.every { manager.getTransaction(any()) } returns mockk<TransactionStatus>(relaxed = true)
            return manager
        }

        fun file() =
            ImportTestSupport.write(
                ExportFormat.XLSX,
                listOf(
                    ImportTestSupport.Sheet("wallets", walletCols, listOf(wallet("Main"), wallet("Savings"))),
                    ImportTestSupport.Sheet(
                        "transactions",
                        txCols,
                        listOf(
                            FakeTransactionSpec.sample("Main", "1"),
                            FakeTransactionSpec.sample("Main", "2"),
                            FakeTransactionSpec.sample("Savings", "3"),
                        ),
                    ),
                ),
            )

        Given("a valid plan") {
            val wallets = FakeWalletSpec()
            val transactions = FakeTransactionSpec(wallets)
            val catalog = EntityCatalog(listOf(wallets, transactions))
            val manager = manager()
            val executor = ImportExecutor(catalog, manager, notifications)
            val plan = runBlocking { ImportPlanner(catalog).plan(file()) }

            When("executed") {
                val result = executor.execute(plan)

                Then("the rows are applied in dependency order inside one committed transaction") {
                    result.added shouldBe mapOf("wallets" to 2, "transactions" to 3)
                    wallets.stored.size shouldBe 2
                    transactions.stored.size shouldBe 3
                    verify(exactly = 1) { manager.commit(any()) }
                    verify(exactly = 0) { manager.rollback(any()) }
                }

                Then("planning the same file again finds only duplicates") {
                    val second = runBlocking { ImportPlanner(catalog).plan(file()) }
                    second.entities.map { it.toAdd } shouldBe listOf(0, 0)
                    second.entities.map { it.duplicates } shouldBe listOf(2, 3)
                    executor.execute(second).added shouldBe mapOf("wallets" to 0, "transactions" to 0)
                    wallets.stored.size shouldBe 2
                }
            }
        }

        Given("a row that fails to apply in the third position") {
            val wallets = FakeWalletSpec()
            val transactions = FakeTransactionSpec(wallets, failAt = 2)
            val catalog = EntityCatalog(listOf(wallets, transactions))
            val manager = manager()
            val executor = ImportExecutor(catalog, manager, notifications)
            val plan = runBlocking { ImportPlanner(catalog).plan(file()) }

            When("executed") {
                Then("the transaction is rolled back, never committed, and the error is reported") {
                    shouldThrow<ImportExecutionException> { executor.execute(plan) }
                    verify(exactly = 1) { manager.rollback(any()) }
                    verify(exactly = 0) { manager.commit(any()) }
                }
            }
        }

        Given("a plan with errors") {
            val wallets = FakeWalletSpec()
            val catalog = EntityCatalog(listOf(wallets, FakeTransactionSpec(wallets)))
            val plan =
                runBlocking {
                    ImportPlanner(catalog).plan(
                        ImportTestSupport.write(
                            ExportFormat.CSV_ZIP,
                            listOf(
                                ImportTestSupport.Sheet(
                                    "transactions",
                                    txCols,
                                    listOf(FakeTransactionSpec.sample("Ghost")),
                                ),
                            ),
                        ),
                    )
                }

            Then("the executor refuses it") {
                shouldThrow<ImportExecutionException> {
                    ImportExecutor(
                        catalog,
                        manager(),
                        notifications,
                    ).execute(plan)
                }
            }
        }

        Given("a database that changed after the preview") {
            val wallets = FakeWalletSpec()
            val catalog = EntityCatalog(listOf(wallets, FakeTransactionSpec(wallets)))
            val plan = runBlocking { ImportPlanner(catalog).plan(file()) }
            wallets.stored.add(wallet("Other"))

            Then("the executor aborts without writing") {
                shouldThrow<ImportExecutionException> {
                    ImportExecutor(
                        catalog,
                        manager(),
                        notifications,
                    ).execute(plan)
                }
                wallets.stored.size shouldBe 1
            }
        }
    })
