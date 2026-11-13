/*
 * Filename: CatalogCompletenessTest.kt
 * Created on: November 13, 2026
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 */

package org.moinex.dataexchange

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.collections.shouldContainAll
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkObject
import kotlinx.coroutines.Job
import kotlinx.coroutines.runBlocking
import org.moinex.common.util.FxUtils
import org.moinex.dataexchange.export.ExportService
import org.moinex.dataexchange.imports.ImportPlanner
import org.moinex.dataexchange.spec.EntityCatalog
import org.moinex.dataexchange.tabular.ExportFormat
import org.moinex.dataexchange.tabular.TabularFiles
import org.moinex.dataexchange.wallet.WalletTestDatabase
import java.nio.file.Files

class CatalogCompletenessTest :
    BehaviorSpec({
        beforeSpec {
            mockkObject(FxUtils)
            every { FxUtils.launchOnFxThread(any()) } returns mockk<Job>(relaxed = true)
        }

        afterSpec { unmockkObject(FxUtils) }

        val expected =
            setOf(
                "wallet_types",
                "categories",
                "wallets",
                "wallet_transactions",
                "transfers",
                "recurring_transactions",
                "credit_card_operators",
                "credit_cards",
                "recurring_credit_card_debts",
                "credit_card_debts",
                "credit_card_payments",
                "credit_card_credits",
                "tickers",
                "ticker_purchases",
                "ticker_sales",
                "dividends",
                "crypto_exchanges",
                "bonds",
                "bond_operations",
                "investment_targets",
                "financial_plans",
                "budget_groups",
                "wishlist_items",
                "wishlist_item_links",
            )

        Given("the catalog of the real specs") {
            WalletTestDatabase().use { db ->
                val catalog = db.get<EntityCatalog>()

                Then("every entity of the design is registered, in the four groups, with consistent metadata") {
                    catalog.ordered().map { it.id }.toSet() shouldBe expected
                    catalog.groups().keys shouldBe setOf("wallets", "credit_cards", "investments", "planning")
                    catalog.ordered().forEach { spec ->
                        spec.columns
                            .map { it.name }
                            .toSet()
                            .size shouldBe spec.columns.size
                        spec.id.length shouldBe spec.id.length.coerceAtMost(31)
                        catalog.ordered().map { it.id } shouldContainAll spec.dependsOn
                    }
                    val position = catalog.ordered().mapIndexed { index, spec -> spec.id to index }.toMap()
                    catalog.ordered().forEach { spec ->
                        spec.dependsOn.forEach {
                            position.getValue(it) shouldBe
                                position.getValue(it).coerceAtMost(position.getValue(spec.id) - 1)
                        }
                    }
                }

                Then("the empty template has every sheet with headers and imports as nothing to add") {
                    listOf(ExportFormat.XLSX, ExportFormat.CSV_ZIP).forEach { format ->
                        val file =
                            Files.createTempFile(
                                "template",
                                if (format ==
                                    ExportFormat.XLSX
                                ) {
                                    ".xlsx"
                                } else {
                                    ".zip"
                                },
                            )
                        db.get<ExportService>().exportTemplate(format, file)
                        TabularFiles.open(file).use { reader ->
                            reader.sheetNames() shouldBe expected
                            catalog.ordered().forEach { reader.header(it.id) shouldBe it.columns.map { c -> c.name } }
                        }
                        val plan = runBlocking { db.get<ImportPlanner>().plan(file) }
                        plan.hasErrors shouldBe false
                        plan.totalToAdd shouldBe 0
                    }
                }
            }
        }
    })
