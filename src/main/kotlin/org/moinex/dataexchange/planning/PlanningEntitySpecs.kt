/*
 * Filename: PlanningEntitySpecs.kt
 * Created on: November 13, 2026
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 */

package org.moinex.dataexchange.planning

import org.moinex.common.extension.toRounded
import org.moinex.dataexchange.creditcard.CreditCardEntities
import org.moinex.dataexchange.investment.TransactionLinker
import org.moinex.dataexchange.spec.ApplyContext
import org.moinex.dataexchange.spec.CellType
import org.moinex.dataexchange.spec.Cols
import org.moinex.dataexchange.spec.Column
import org.moinex.dataexchange.spec.EntitySpec
import org.moinex.dataexchange.spec.NaturalKeys
import org.moinex.dataexchange.spec.ParsedRow
import org.moinex.dataexchange.spec.Problem
import org.moinex.dataexchange.spec.ProblemCodes
import org.moinex.dataexchange.spec.ResolveContext
import org.moinex.dataexchange.wallet.WalletEntities
import org.moinex.model.enums.BudgetGroupTransactionFilter
import org.moinex.model.enums.WalletTransactionType
import org.moinex.model.enums.WishlistItemPriority
import org.moinex.model.enums.WishlistItemStatus
import org.moinex.model.financialplanning.BudgetGroup
import org.moinex.model.financialplanning.FinancialPlan
import org.moinex.model.wishlist.WishlistItem
import org.moinex.model.wishlist.WishlistItemLink
import org.moinex.repository.CategoryRepository
import org.moinex.repository.creditcard.CreditCardDebtRepository
import org.moinex.repository.financialplanning.FinancialPlanRepository
import org.moinex.repository.wishlist.WishlistItemLinkRepository
import org.moinex.repository.wishlist.WishlistItemRepository
import org.moinex.service.wishlist.WishlistService
import org.springframework.stereotype.Component
import java.math.BigDecimal
import java.time.LocalDateTime

object PlanningEntities {
    const val GROUP = "planning"
    const val PLANS = "financial_plans"
    const val BUDGET_GROUPS = "budget_groups"
    const val WISHLIST = "wishlist_items"
    const val WISHLIST_LINKS = "wishlist_item_links"
    const val CATEGORY_SEPARATOR = ";"

    fun categoryNames(value: String?): List<String> =
        value
            ?.split(CATEGORY_SEPARATOR)
            ?.map { it.trim() }
            ?.filter { it.isNotEmpty() }
            .orEmpty()

    fun itemKey(
        title: String?,
        createdAt: LocalDateTime?,
    ) = NaturalKeys.of(title?.trim(), createdAt)
}

@Component
class FinancialPlanSpec(
    private val repository: FinancialPlanRepository,
    private val categoryRepository: CategoryRepository,
) : EntitySpec {
    override val id = PlanningEntities.PLANS
    override val group = PlanningEntities.GROUP
    override val dependsOn = listOf(WalletEntities.CATEGORIES)
    override val columns =
        listOf(
            Column(Cols.NAME, CellType.TEXT, required = true),
            Column(Cols.BASE_INCOME, CellType.DECIMAL, required = true),
            Column(Cols.START_DATE, CellType.DATE, required = true),
            Column(Cols.END_DATE, CellType.DATE),
            Column(Cols.ARCHIVED, CellType.BOOLEAN),
        )

    override fun count() = repository.count()

    override fun export() =
        repository.findAll().sortedWith(compareBy({ it.startDate }, { it.id })).asSequence().map {
            mapOf(
                Cols.NAME to it.name,
                Cols.BASE_INCOME to it.baseIncome,
                Cols.START_DATE to it.startDate,
                Cols.END_DATE to it.endDate,
                Cols.ARCHIVED to it.archived,
            )
        }

    override fun naturalKey(row: ParsedRow) = row.text(Cols.NAME)!!.trim()

    override fun existingKeys(): Map<String, Int> = repository.findAll().groupingBy { it.name }.eachCount()

    override fun validate(
        row: ParsedRow,
        ctx: ResolveContext,
    ): List<Problem> =
        WalletEntities.checkPositive(id, row, Cols.BASE_INCOME) +
            if (row.text(Cols.NAME)!!.trim().isEmpty() ||
                row.text(Cols.NAME)!!.length > WalletEntities.MAX_NAME_LENGTH
            ) {
                listOf(WalletEntities.error(id, row, Cols.NAME, ProblemCodes.DOMAIN_RULE, "length"))
            } else {
                emptyList()
            }

    override fun prepare(
        rowsByEntity: Map<String, List<ParsedRow>>,
        ctx: ApplyContext,
    ) {
        val groups = rowsByEntity[PlanningEntities.BUDGET_GROUPS].orEmpty().groupBy { it.text(Cols.PLAN)!!.trim() }
        ctx.memo("plan_groups") { groups }
    }

    override fun apply(
        row: ParsedRow,
        ctx: ApplyContext,
    ) {
        val name = row.text(Cols.NAME)!!.trim()
        val groupRows = ctx.memo<Map<String, List<ParsedRow>>>("plan_groups") { emptyMap() }[name].orEmpty()
        val plan =
            FinancialPlan(
                name = name,
                baseIncome = row.decimal(Cols.BASE_INCOME)!!,
                budgetGroups =
                    groupRows
                        .map { groupRow ->
                            BudgetGroup(
                                name = groupRow.text(Cols.NAME)!!,
                                targetPercentage = groupRow.decimal(Cols.TARGET_PERCENTAGE)!!,
                                categories =
                                    PlanningEntities
                                        .categoryNames(groupRow.text(Cols.CATEGORIES))
                                        .map { categoryRepository.findByName(it)!! }
                                        .toMutableSet(),
                                transactionTypeFilter =
                                    groupRow.enum<BudgetGroupTransactionFilter>(
                                        Cols.TRANSACTION_TYPE_FILTER,
                                    )!!,
                            )
                        }.toMutableList(),
                startDate = row.date(Cols.START_DATE)!!,
                endDate = row.date(Cols.END_DATE),
                archived = row.bool(Cols.ARCHIVED) ?: false,
            )
        plan.budgetGroups.forEach { it.plan = plan }
        repository.save(plan)
    }
}

@Component
class BudgetGroupSpec(
    private val repository: FinancialPlanRepository,
) : EntitySpec {
    override val id = PlanningEntities.BUDGET_GROUPS
    override val group = PlanningEntities.GROUP
    override val dependsOn = listOf(PlanningEntities.PLANS, WalletEntities.CATEGORIES)
    override val columns =
        listOf(
            Column(Cols.PLAN, CellType.TEXT, required = true),
            Column(Cols.NAME, CellType.TEXT, required = true),
            Column(Cols.TARGET_PERCENTAGE, CellType.DECIMAL, required = true),
            Column(
                Cols.TRANSACTION_TYPE_FILTER,
                CellType.ENUM,
                required = true,
                enumClass = BudgetGroupTransactionFilter::class.java,
            ),
            Column(Cols.CATEGORIES, CellType.TEXT),
        )

    override fun count() = repository.findAll().sumOf { it.budgetGroups.size }.toLong()

    override fun export() =
        repository
            .findAll()
            .sortedWith(compareBy({ it.startDate }, { it.id }))
            .asSequence()
            .flatMap { plan ->
                plan.budgetGroups.sortedBy { it.id }.map {
                    mapOf(
                        Cols.PLAN to plan.name,
                        Cols.NAME to it.name,
                        Cols.TARGET_PERCENTAGE to it.targetPercentage,
                        Cols.TRANSACTION_TYPE_FILTER to it.transactionTypeFilter,
                        Cols.CATEGORIES to
                            it.categories
                                .map { c ->
                                    c.name
                                }.sorted()
                                .joinToString(PlanningEntities.CATEGORY_SEPARATOR),
                    )
                }
            }

    override fun naturalKey(row: ParsedRow) = NaturalKeys.of(row.text(Cols.PLAN)!!.trim(), row.text(Cols.NAME)!!.trim())

    override fun existingKeys(): Map<String, Int> =
        repository
            .findAll()
            .flatMap { plan ->
                plan.budgetGroups.map { NaturalKeys.of(plan.name, it.name) }
            }.groupingBy { it }
            .eachCount()

    override fun validate(
        row: ParsedRow,
        ctx: ResolveContext,
    ): List<Problem> {
        val problems = mutableListOf<Problem>()
        problems.addAll(WalletEntities.checkReference(id, row, Cols.PLAN, PlanningEntities.PLANS, ctx))
        val percentage = row.decimal(Cols.TARGET_PERCENTAGE)!!
        if (percentage < BigDecimal.ZERO || percentage > BigDecimal(100)) {
            problems.add(WalletEntities.error(id, row, Cols.TARGET_PERCENTAGE, ProblemCodes.DOMAIN_RULE, "0..100"))
        }
        PlanningEntities.categoryNames(row.text(Cols.CATEGORIES)).forEach {
            if (!ctx.exists(WalletEntities.CATEGORIES, it)) {
                problems.add(WalletEntities.error(id, row, Cols.CATEGORIES, ProblemCodes.UNRESOLVED_REFERENCE, it))
            }
        }
        val planName = row.text(Cols.PLAN)!!.trim()
        if (ctx.existsInDatabase(PlanningEntities.PLANS, planName) && !ctx.existsInDatabase(id, naturalKey(row))) {
            problems.add(
                WalletEntities.error(
                    id,
                    row,
                    Cols.PLAN,
                    ProblemCodes.DOMAIN_RULE,
                    "the plan already exists and its groups cannot change",
                ),
            )
        }
        return problems
    }

    override fun apply(
        row: ParsedRow,
        ctx: ApplyContext,
    ) = Unit
}

@Component
class WishlistItemSpec(
    private val repository: WishlistItemRepository,
    private val categoryRepository: CategoryRepository,
    private val debtRepository: CreditCardDebtRepository,
    private val linker: TransactionLinker,
    private val service: WishlistService,
) : EntitySpec {
    override val id = PlanningEntities.WISHLIST
    override val group = PlanningEntities.GROUP
    override val dependsOn = listOf(WalletEntities.CATEGORIES, WalletEntities.TRANSACTIONS, CreditCardEntities.DEBTS)
    override val columns =
        listOf(
            Column(Cols.TITLE, CellType.TEXT, required = true),
            Column(Cols.ESTIMATED_PRICE, CellType.DECIMAL, required = true),
            Column(Cols.TARGET_DATE, CellType.DATE),
            Column(Cols.STATUS, CellType.ENUM, required = true, enumClass = WishlistItemStatus::class.java),
            Column(Cols.PRIORITY, CellType.ENUM, required = true, enumClass = WishlistItemPriority::class.java),
            Column(Cols.NOTES, CellType.TEXT),
            Column(Cols.CATEGORY, CellType.TEXT, required = true),
            Column(Cols.CREATED_AT, CellType.DATETIME, required = true),
            Column(Cols.PURCHASED_AT, CellType.DATETIME),
            Column(Cols.TX_WALLET, CellType.TEXT),
            Column(Cols.TX_DATE, CellType.DATETIME),
            Column(Cols.TX_AMOUNT, CellType.DECIMAL),
            Column(Cols.TX_DESCRIPTION, CellType.TEXT),
            Column(Cols.DEBT_CARD, CellType.TEXT),
            Column(Cols.DEBT_DATE, CellType.DATETIME),
            Column(Cols.DEBT_AMOUNT, CellType.DECIMAL),
            Column(Cols.DEBT_DESCRIPTION, CellType.TEXT),
        )

    override fun count() = repository.count()

    override fun export() =
        repository.findAll().sortedWith(compareBy({ it.createdAt }, { it.id })).asSequence().map {
            val tx = it.walletTransaction
            val debt = it.creditCardDebt
            mapOf(
                Cols.TITLE to it.title,
                Cols.ESTIMATED_PRICE to it.estimatedPrice,
                Cols.TARGET_DATE to it.targetDate,
                Cols.STATUS to it.status,
                Cols.PRIORITY to it.priority,
                Cols.NOTES to it.notes,
                Cols.CATEGORY to it.category.name,
                Cols.CREATED_AT to it.createdAt,
                Cols.PURCHASED_AT to it.purchasedAt,
                Cols.TX_WALLET to tx?.wallet?.name,
                Cols.TX_DATE to tx?.date,
                Cols.TX_AMOUNT to tx?.amount,
                Cols.TX_DESCRIPTION to tx?.description,
                Cols.DEBT_CARD to debt?.creditCard?.name,
                Cols.DEBT_DATE to debt?.date,
                Cols.DEBT_AMOUNT to debt?.amount,
                Cols.DEBT_DESCRIPTION to debt?.description,
            )
        }

    override fun naturalKey(row: ParsedRow) =
        PlanningEntities.itemKey(row.text(Cols.TITLE), row.dateTime(Cols.CREATED_AT))

    override fun existingKeys(): Map<String, Int> =
        repository.findAll().groupingBy { PlanningEntities.itemKey(it.title, it.createdAt) }.eachCount()

    private fun txKey(row: ParsedRow) =
        WalletEntities.transactionKey(
            row.text(Cols.TX_WALLET),
            row.dateTime(Cols.TX_DATE),
            WalletTransactionType.EXPENSE,
            row.decimal(Cols.TX_AMOUNT),
            row.text(Cols.TX_DESCRIPTION),
        )

    private fun debtKey(row: ParsedRow) =
        CreditCardEntities.debtKey(
            row.text(Cols.DEBT_CARD),
            row.dateTime(Cols.DEBT_DATE),
            row.decimal(Cols.DEBT_AMOUNT),
            row.text(Cols.DEBT_DESCRIPTION),
        )

    override fun validate(
        row: ParsedRow,
        ctx: ResolveContext,
    ): List<Problem> {
        val problems = mutableListOf<Problem>()
        problems.addAll(WalletEntities.checkReference(id, row, Cols.CATEGORY, WalletEntities.CATEGORIES, ctx))
        val price = row.decimal(Cols.ESTIMATED_PRICE)!!
        if (price <
            BigDecimal.ZERO
        ) {
            problems.add(WalletEntities.error(id, row, Cols.ESTIMATED_PRICE, ProblemCodes.DOMAIN_RULE, "negative"))
        }
        if (row.text(Cols.TX_WALLET) != null) {
            if (row.dateTime(Cols.TX_DATE) == null || row.decimal(Cols.TX_AMOUNT) == null) {
                problems.add(
                    WalletEntities.error(id, row, Cols.TX_DATE, ProblemCodes.INVALID_VALUE, "required with tx_wallet"),
                )
            } else if (!ctx.exists(WalletEntities.TRANSACTIONS, txKey(row))) {
                problems.add(
                    WalletEntities.error(id, row, Cols.TX_WALLET, ProblemCodes.UNRESOLVED_REFERENCE, "transaction"),
                )
            }
        }
        if (row.text(Cols.DEBT_CARD) != null) {
            if (row.dateTime(Cols.DEBT_DATE) == null || row.decimal(Cols.DEBT_AMOUNT) == null) {
                problems.add(
                    WalletEntities.error(
                        id,
                        row,
                        Cols.DEBT_DATE,
                        ProblemCodes.INVALID_VALUE,
                        "required with debt_card",
                    ),
                )
            } else if (!ctx.exists(CreditCardEntities.DEBTS, debtKey(row))) {
                problems.add(WalletEntities.error(id, row, Cols.DEBT_CARD, ProblemCodes.UNRESOLVED_REFERENCE, "debt"))
            }
        }
        return problems
    }

    override fun applyOrder(rows: List<ParsedRow>): List<ParsedRow> = rows.sortedBy { it.dateTime(Cols.CREATED_AT) }

    override fun apply(
        row: ParsedRow,
        ctx: ApplyContext,
    ) {
        val transaction = row.text(Cols.TX_WALLET)?.let { linker.take(ctx, txKey(row)) }
        val debt =
            row.text(Cols.DEBT_CARD)?.let {
                val index =
                    ctx.memo("credit_card_debt_by_key") {
                        val map = mutableMapOf<String, org.moinex.model.creditcard.CreditCardDebt>()
                        debtRepository.findAll().forEach { d ->
                            map.putIfAbsent(
                                CreditCardEntities.debtKey(d.creditCard.name, d.date, d.amount, d.description),
                                d,
                            )
                        }
                        map
                    }
                index.getValue(debtKey(row))
            }
        service.createItem(
            WishlistItem(
                title = row.text(Cols.TITLE)!!,
                estimatedPrice = row.decimal(Cols.ESTIMATED_PRICE)!!.toRounded(),
                targetDate = row.date(Cols.TARGET_DATE),
                status = row.enum<WishlistItemStatus>(Cols.STATUS)!!,
                priority = row.enum<WishlistItemPriority>(Cols.PRIORITY)!!,
                notes = row.text(Cols.NOTES),
                category = categoryRepository.findByName(row.text(Cols.CATEGORY)!!)!!,
                createdAt = row.dateTime(Cols.CREATED_AT)!!,
                purchasedAt = row.dateTime(Cols.PURCHASED_AT),
                walletTransaction = transaction,
                creditCardDebt = debt,
            ),
            emptyList(),
        )
    }
}

@Component
class WishlistItemLinkSpec(
    private val repository: WishlistItemLinkRepository,
    private val itemRepository: WishlistItemRepository,
) : EntitySpec {
    private fun validLinks() = repository.findAllWithExistingItem()

    override val id = PlanningEntities.WISHLIST_LINKS
    override val group = PlanningEntities.GROUP
    override val dependsOn = listOf(PlanningEntities.WISHLIST)
    override val columns =
        listOf(
            Column(Cols.ITEM_TITLE, CellType.TEXT, required = true),
            Column(Cols.ITEM_CREATED_AT, CellType.DATETIME, required = true),
            Column(Cols.URL, CellType.TEXT, required = true),
            Column(Cols.LABEL, CellType.TEXT),
        )

    override fun count() = validLinks().size.toLong()

    override fun export() =
        validLinks().sortedWith(compareBy({ it.wishlistItem?.createdAt }, { it.id })).asSequence().map {
            mapOf(
                Cols.ITEM_TITLE to it.wishlistItem!!.title,
                Cols.ITEM_CREATED_AT to it.wishlistItem!!.createdAt,
                Cols.URL to it.url,
                Cols.LABEL to it.label,
            )
        }

    override fun naturalKey(row: ParsedRow) =
        NaturalKeys.of(
            PlanningEntities.itemKey(row.text(Cols.ITEM_TITLE), row.dateTime(Cols.ITEM_CREATED_AT)),
            row.text(Cols.URL)?.trim(),
        )

    override fun existingKeys(): Map<String, Int> =
        validLinks()
            .groupingBy {
                NaturalKeys.of(
                    PlanningEntities.itemKey(it.wishlistItem!!.title, it.wishlistItem!!.createdAt),
                    it.url,
                )
            }.eachCount()

    override fun validate(
        row: ParsedRow,
        ctx: ResolveContext,
    ): List<Problem> =
        if (ctx.exists(
                PlanningEntities.WISHLIST,
                PlanningEntities.itemKey(row.text(Cols.ITEM_TITLE), row.dateTime(Cols.ITEM_CREATED_AT)),
            )
        ) {
            emptyList()
        } else {
            listOf(WalletEntities.error(id, row, Cols.ITEM_TITLE, ProblemCodes.UNRESOLVED_REFERENCE))
        }

    override fun apply(
        row: ParsedRow,
        ctx: ApplyContext,
    ) {
        val key = PlanningEntities.itemKey(row.text(Cols.ITEM_TITLE), row.dateTime(Cols.ITEM_CREATED_AT))
        val items =
            ctx.memo("wishlist_item_by_key") {
                val map = mutableMapOf<String, WishlistItem>()
                itemRepository.findAll().forEach {
                    map.putIfAbsent(
                        PlanningEntities.itemKey(it.title, it.createdAt),
                        it,
                    )
                }
                map
            }
        repository.save(
            WishlistItemLink(
                url = row.text(Cols.URL)!!,
                label = row.text(Cols.LABEL),
                wishlistItem = items.getValue(key),
            ),
        )
    }
}
