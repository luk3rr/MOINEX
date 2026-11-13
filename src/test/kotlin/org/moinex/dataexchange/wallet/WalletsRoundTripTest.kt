/*
 * Filename: WalletsRoundTripTest.kt
 * Created on: November 12, 2026
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 */

package org.moinex.dataexchange.wallet

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkObject
import kotlinx.coroutines.Job
import kotlinx.coroutines.runBlocking
import org.moinex.common.util.FxUtils
import org.moinex.dataexchange.export.ExportService
import org.moinex.dataexchange.imports.ImportExecutor
import org.moinex.dataexchange.imports.ImportPlanner
import org.moinex.dataexchange.tabular.ExportFormat
import org.moinex.model.Category
import org.moinex.model.enums.RecurringTransactionFrequency
import org.moinex.model.enums.RecurringTransactionStatus
import org.moinex.model.enums.WalletTransactionStatus
import org.moinex.model.enums.WalletTransactionType
import org.moinex.model.wallettransaction.RecurringTransaction
import org.moinex.model.wallettransaction.Transfer
import org.moinex.model.wallettransaction.Wallet
import org.moinex.model.wallettransaction.WalletTransaction
import org.moinex.model.wallettransaction.WalletType
import org.moinex.repository.CategoryRepository
import org.moinex.repository.NotificationRepository
import org.moinex.repository.wallettransaction.RecurringTransactionRepository
import org.moinex.repository.wallettransaction.TransferRepository
import org.moinex.repository.wallettransaction.WalletRepository
import org.moinex.repository.wallettransaction.WalletTransactionRepository
import org.moinex.repository.wallettransaction.WalletTypeRepository
import org.moinex.service.CategoryService
import org.moinex.service.wallet.RecurringTransactionService
import org.moinex.service.wallet.WalletService
import java.math.BigDecimal
import java.nio.file.Files
import java.time.LocalDate
import java.time.LocalDateTime

class WalletsRoundTripTest :
    BehaviorSpec({
        beforeSpec {
            mockkObject(FxUtils)
            every { FxUtils.launchOnFxThread(any()) } returns mockk<Job>(relaxed = true)
        }

        afterSpec { unmockkObject(FxUtils) }

        fun seed(db: WalletTestDatabase) {
            val types = db.get<WalletTypeRepository>()
            val checking = types.save(WalletType(name = "Checking", icon = "bank"))
            val savings = types.save(WalletType(name = "Savings"))
            val categories = db.get<CategoryService>()
            categories.createCategory(Category(name = "Food"))
            categories.createCategory(Category(name = "Salary"))
            categories.createCategory(Category(name = "Old", isArchived = true))
            val wallets = db.get<WalletService>()
            wallets.createWallet(Wallet(type = checking, name = "Main", balance = BigDecimal("1000.00")))
            val main = db.get<WalletRepository>().findByName("Main").get()
            wallets.createWallet(
                Wallet(type = savings, name = "Reserve", balance = BigDecimal("200.00"), masterWallet = main),
            )
            wallets.createWallet(
                Wallet(type = savings, name = "Piggy", balance = BigDecimal("40.00"), isArchived = true),
            )
            val repo = db.get<WalletRepository>()
            val food = db.get<CategoryRepository>().findByName("Food")!!
            val salary = db.get<CategoryRepository>().findByName("Salary")!!

            fun tx(
                wallet: String,
                kind: WalletTransactionType,
                amount: String,
                category: Category,
                day: Int,
                status: WalletTransactionStatus = WalletTransactionStatus.CONFIRMED,
                description: String? = null,
            ) = wallets.createWalletTransaction(
                WalletTransaction(
                    date = LocalDateTime.of(2026, 10, day, 9, 30, 15),
                    status = status,
                    description = description,
                    includeInAnalysis = true,
                    wallet = repo.findByName(wallet).get(),
                    category = category,
                    type = kind,
                    amount = BigDecimal(amount),
                ),
            )
            tx("Main", WalletTransactionType.EXPENSE, "30.00", food, 2, description = "=lunch")
            tx("Reserve", WalletTransactionType.INCOME, "50.00", salary, 3)
            tx("Piggy", WalletTransactionType.INCOME, "12.50", salary, 4, WalletTransactionStatus.PENDING)
            tx("Main", WalletTransactionType.INCOME, "100.25", salary, 5, description = "Pay, \"Oct\"\nline 2")
            wallets.createTransfer(
                Transfer(
                    senderWallet = repo.findByName("Main").get(),
                    receiverWallet = repo.findByName("Piggy").get(),
                    date = LocalDateTime.of(2026, 10, 6, 8, 0, 0),
                    amount = BigDecimal("10.00"),
                    description = "to piggy",
                    category = food,
                ),
            )
            db.get<RecurringTransactionService>().createRecurringTransaction(
                RecurringTransaction(
                    startDate = LocalDate.of(2026, 1, 1),
                    endDate = LocalDate.of(2027, 1, 1),
                    nextDueDate = LocalDate.of(2026, 12, 1),
                    frequency = RecurringTransactionFrequency.MONTHLY,
                    status = RecurringTransactionStatus.ACTIVE,
                    description = "rent",
                    wallet = repo.findByName("Main").get(),
                    category = food,
                    type = WalletTransactionType.EXPENSE,
                    amount = BigDecimal("20.00"),
                ),
            )
        }

        fun snapshot(db: WalletTestDatabase): Map<String, Any> {
            val wallets = db.get<WalletRepository>().findAll().sortedBy { it.name }
            return mapOf(
                "wallets" to
                    wallets.map {
                        listOf(
                            it.name,
                            it.type.name,
                            it.balance.toPlainString(),
                            it.isArchived,
                            it.masterWallet?.name,
                        )
                    },
                "types" to
                    db
                        .get<WalletTypeRepository>()
                        .findAll()
                        .map {
                            listOf(
                                it.name,
                                it.icon,
                            )
                        }.sortedBy { it[0] as String },
                "categories" to
                    db
                        .get<CategoryRepository>()
                        .findAll()
                        .map {
                            listOf(
                                it.name,
                                it.isArchived,
                            )
                        }.sortedBy { it[0] as String },
                "transactions" to
                    db
                        .get<WalletTransactionRepository>()
                        .findAll()
                        .map {
                            listOf(
                                it.wallet.name,
                                it.category.name,
                                it.type,
                                it.status,
                                it.date,
                                it.amount.toPlainString(),
                                it.description,
                            )
                        }.sortedBy { it.joinToString("|") },
                "transfers" to
                    db
                        .get<TransferRepository>()
                        .findAll()
                        .map {
                            listOf(
                                it.senderWallet.name,
                                it.receiverWallet.name,
                                it.date,
                                it.amount.toPlainString(),
                                it.description,
                                it.category?.name,
                            )
                        },
                "recurring" to
                    db
                        .get<RecurringTransactionRepository>()
                        .findAll()
                        .map {
                            listOf(
                                it.wallet.name,
                                it.startDate,
                                it.endDate,
                                it.nextDueDate,
                                it.frequency,
                                it.status,
                                it.amount.toPlainString(),
                                it.description,
                            )
                        },
            )
        }

        listOf(ExportFormat.XLSX, ExportFormat.CSV_ZIP).forEach { format ->
            Given("a populated database A exported as $format") {
                WalletTestDatabase().use { a ->
                    seed(a)
                    val expected = snapshot(a)
                    val file = Files.createTempFile("wallets", if (format == ExportFormat.XLSX) ".xlsx" else ".zip")
                    val selection = a.get<ExportService>().counts().keys
                    runBlocking { a.get<ExportService>().export(selection, format, file) }

                    When("importing into an empty database B") {
                        WalletTestDatabase().use { b ->
                            val plan = runBlocking { b.get<ImportPlanner>().plan(file) }
                            val result = b.get<ImportExecutor>().execute(plan)

                            Then("the plan has no errors and counts every row as new") {
                                plan.hasErrors shouldBe false
                                plan.entities.sumOf { it.duplicates } shouldBe 0
                                result.added.getValue("wallet_transactions") shouldBe 4
                            }

                            Then("all rows and the final balances of every wallet are identical to A") {
                                snapshot(b) shouldBe expected
                            }

                            Then("no notification is created by the import") {
                                b.get<NotificationRepository>().count() shouldBe 0L
                            }

                            Then(
                                "planning the same file again finds only duplicates and importing it changes nothing",
                            ) {
                                val second = runBlocking { b.get<ImportPlanner>().plan(file) }
                                second.hasErrors shouldBe false
                                second.entities.sumOf { it.toAdd } shouldBe 0
                                b.get<ImportExecutor>().execute(second)
                                snapshot(b) shouldBe expected
                            }
                        }
                    }
                }
            }
        }
    })
