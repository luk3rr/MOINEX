/*
 * Filename: PlanningRoundTripTest.kt
 * Created on: November 13, 2026
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 */

package org.moinex.dataexchange.planning

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
import org.moinex.dataexchange.wallet.WalletTestDatabase
import org.moinex.model.Category
import org.moinex.model.enums.BudgetGroupTransactionFilter
import org.moinex.model.enums.WalletTransactionStatus
import org.moinex.model.enums.WalletTransactionType
import org.moinex.model.enums.WishlistItemPriority
import org.moinex.model.enums.WishlistItemStatus
import org.moinex.model.financialplanning.BudgetGroup
import org.moinex.model.financialplanning.FinancialPlan
import org.moinex.model.goal.Goal
import org.moinex.model.wallettransaction.Wallet
import org.moinex.model.wallettransaction.WalletTransaction
import org.moinex.model.wallettransaction.WalletType
import org.moinex.model.wishlist.WishlistItem
import org.moinex.model.wishlist.WishlistItemLink
import org.moinex.repository.CategoryRepository
import org.moinex.repository.financialplanning.FinancialPlanRepository
import org.moinex.repository.wallettransaction.WalletRepository
import org.moinex.repository.wallettransaction.WalletTransactionRepository
import org.moinex.repository.wallettransaction.WalletTypeRepository
import org.moinex.repository.wishlist.WishlistItemLinkRepository
import org.moinex.repository.wishlist.WishlistItemRepository
import org.moinex.service.CategoryService
import org.moinex.service.wallet.WalletService
import org.moinex.service.wishlist.WishlistService
import java.math.BigDecimal
import java.nio.file.Files
import java.time.LocalDate
import java.time.LocalDateTime

class PlanningRoundTripTest :
    BehaviorSpec({
        beforeSpec {
            mockkObject(FxUtils)
            every { FxUtils.launchOnFxThread(any()) } returns mockk<Job>(relaxed = true)
        }

        afterSpec { unmockkObject(FxUtils) }

        fun seed(db: WalletTestDatabase) {
            val types = db.get<WalletTypeRepository>()
            val checking = types.save(WalletType(name = "Checking"))
            val goalType = types.save(WalletType(name = "Goal"))
            val categories = db.get<CategoryService>()
            listOf("Food", "Rent", "Savings", "Gadgets").forEach { categories.createCategory(Category(name = it)) }
            val wallets = db.get<WalletService>()
            wallets.createWallet(Wallet(type = checking, name = "Main", balance = BigDecimal("2000.00")))
            val main = db.get<WalletRepository>().findByName("Main").get()
            wallets.createWallet(
                Goal(
                    initialBalance = BigDecimal("100.00"),
                    targetBalance = BigDecimal("1000.00"),
                    targetDate = LocalDate.of(2027, 6, 1),
                    motivation = "trip",
                    name = "Trip",
                    type = goalType,
                    masterWallet = main,
                ),
            )
            val categoryRepository = db.get<CategoryRepository>()
            val food = categoryRepository.findByName("Food")!!
            val rent = categoryRepository.findByName("Rent")!!
            val savings = categoryRepository.findByName("Savings")!!
            val gadgets = categoryRepository.findByName("Gadgets")!!
            val trip = db.get<WalletRepository>().findByName("Trip").get()
            wallets.createWalletTransaction(
                WalletTransaction(
                    date = LocalDateTime.of(2026, 5, 1, 8, 0, 0),
                    status = WalletTransactionStatus.CONFIRMED,
                    description = "deposit",
                    includeInAnalysis = true,
                    wallet = trip,
                    category = savings,
                    type = WalletTransactionType.INCOME,
                    amount = BigDecimal("150.00"),
                ),
            )
            val purchase =
                WalletTransaction(
                    date = LocalDateTime.of(2026, 5, 3, 8, 0, 0),
                    status = WalletTransactionStatus.CONFIRMED,
                    description = "headphones",
                    includeInAnalysis = true,
                    wallet = main,
                    category = gadgets,
                    type = WalletTransactionType.EXPENSE,
                    amount = BigDecimal("80.00"),
                )
            val purchaseId = wallets.createWalletTransaction(purchase)
            val plans = db.get<FinancialPlanRepository>()
            val plan =
                FinancialPlan(
                    name = "2026 plan",
                    baseIncome = BigDecimal("5000.00"),
                    budgetGroups =
                        mutableListOf(
                            BudgetGroup(
                                name = "Needs",
                                targetPercentage = BigDecimal("60"),
                                categories = mutableSetOf(food, rent),
                                transactionTypeFilter = BudgetGroupTransactionFilter.EXPENSE,
                            ),
                            BudgetGroup(
                                name = "Future",
                                targetPercentage = BigDecimal("40"),
                                categories = mutableSetOf(savings),
                                transactionTypeFilter = BudgetGroupTransactionFilter.BOTH,
                            ),
                        ),
                    startDate = LocalDate.of(2026, 1, 1),
                )
            plan.budgetGroups.forEach { it.plan = plan }
            plans.save(plan)
            val wishlist = db.get<WishlistService>()
            wishlist.createItem(
                WishlistItem(
                    title = "Headphones",
                    estimatedPrice = BigDecimal("80.00"),
                    status = WishlistItemStatus.PURCHASED,
                    priority = WishlistItemPriority.HIGH,
                    category = gadgets,
                    createdAt = LocalDateTime.of(2026, 4, 1, 12, 0, 0),
                    purchasedAt = LocalDateTime.of(2026, 5, 3, 8, 0, 0),
                    walletTransaction = db.get<WalletTransactionRepository>().findById(purchaseId).get(),
                ),
                emptyList(),
            )
            wishlist.createItem(
                WishlistItem(
                    title = "Laptop",
                    estimatedPrice = BigDecimal("3500.00"),
                    targetDate = LocalDate.of(2027, 1, 1),
                    priority = WishlistItemPriority.LOW,
                    notes = "wait for sale",
                    category = gadgets,
                    createdAt = LocalDateTime.of(2026, 4, 2, 12, 0, 0),
                ),
                listOf(
                    WishlistItemLink(url = "https://example.com/a", label = "shop"),
                    WishlistItemLink(url = "https://example.com/b"),
                ),
            )
        }

        fun snapshot(db: WalletTestDatabase): Map<String, Any> =
            mapOf(
                "wallets" to
                    db.get<WalletRepository>().findAll().sortedBy { it.name }.map {
                        val goal = it as? Goal
                        listOf(
                            it.name,
                            it.type.name,
                            it.balance.toPlainString(),
                            it.masterWallet?.name,
                            goal?.initialBalance,
                            goal?.targetBalance,
                            goal?.targetDate,
                            goal?.motivation,
                            goal != null,
                        )
                    },
                "plans" to
                    db.get<FinancialPlanRepository>().findAll().map { plan ->
                        listOf(
                            plan.name,
                            plan.baseIncome.toPlainString(),
                            plan.startDate,
                            plan.endDate,
                            plan.archived,
                            plan.budgetGroups
                                .map {
                                    listOf(
                                        it.name,
                                        it.targetPercentage.toPlainString(),
                                        it.transactionTypeFilter,
                                        it.categories
                                            .map { c ->
                                                c.name
                                            }.sorted(),
                                    )
                                }.sortedBy { it[0] as String },
                        )
                    },
                "items" to
                    db
                        .get<WishlistItemRepository>()
                        .findAll()
                        .map {
                            listOf(
                                it.title,
                                it.estimatedPrice.toPlainString(),
                                it.targetDate,
                                it.status,
                                it.priority,
                                it.notes,
                                it.category.name,
                                it.createdAt,
                                it.purchasedAt,
                                it.walletTransaction?.description,
                                it.walletTransaction?.amount?.toPlainString(),
                            )
                        }.sortedBy { it.joinToString("|") },
                "links" to
                    db
                        .get<WishlistItemLinkRepository>()
                        .findAll()
                        .map {
                            listOf(it.wishlistItem!!.title, it.url, it.label)
                        }.sortedBy { it.joinToString("|") },
            )

        listOf(ExportFormat.XLSX, ExportFormat.CSV_ZIP).forEach { format ->
            Given("a database with a plan, a goal wallet and a wishlist exported as $format") {
                WalletTestDatabase().use { a ->
                    seed(a)
                    val expected = snapshot(a)
                    val file = Files.createTempFile("planning", if (format == ExportFormat.XLSX) ".xlsx" else ".zip")
                    runBlocking { a.get<ExportService>().export(a.get<ExportService>().counts().keys, format, file) }

                    When("importing into an empty database") {
                        WalletTestDatabase().use { b ->
                            val plan = runBlocking { b.get<ImportPlanner>().plan(file) }
                            (plan.fileProblems + plan.entities.flatMap { it.problems }) shouldBe emptyList()
                            b.get<ImportExecutor>().execute(plan)

                            Then(
                                "plans with their groups, the goal wallet with its balance, and the wishlist with links are identical",
                            ) {
                                snapshot(b) shouldBe expected
                            }

                            Then("importing again adds nothing") {
                                val second = runBlocking { b.get<ImportPlanner>().plan(file) }
                                second.hasErrors shouldBe false
                                second.entities.sumOf { it.toAdd } shouldBe 0
                            }
                        }
                    }
                }
            }
        }
    })
