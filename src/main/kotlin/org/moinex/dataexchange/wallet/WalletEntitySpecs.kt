/*
 * Filename: WalletEntitySpecs.kt
 * Created on: November 12, 2026
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 */

package org.moinex.dataexchange.wallet

import org.moinex.common.extension.toRounded
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
import org.moinex.dataexchange.spec.Severity
import org.moinex.model.Category
import org.moinex.model.enums.RecurringTransactionFrequency
import org.moinex.model.enums.RecurringTransactionStatus
import org.moinex.model.enums.WalletTransactionStatus
import org.moinex.model.enums.WalletTransactionType
import org.moinex.model.goal.Goal
import org.moinex.model.wallettransaction.RecurringTransaction
import org.moinex.model.wallettransaction.Transfer
import org.moinex.model.wallettransaction.Wallet
import org.moinex.model.wallettransaction.WalletTransaction
import org.moinex.model.wallettransaction.WalletType
import org.moinex.repository.CategoryRepository
import org.moinex.repository.wallettransaction.RecurringTransactionRepository
import org.moinex.repository.wallettransaction.TransferRepository
import org.moinex.repository.wallettransaction.WalletRepository
import org.moinex.repository.wallettransaction.WalletTransactionRepository
import org.moinex.repository.wallettransaction.WalletTypeRepository
import org.moinex.service.CategoryService
import org.moinex.service.wallet.RecurringTransactionService
import org.moinex.service.wallet.WalletService
import org.springframework.stereotype.Component
import java.math.BigDecimal

object WalletEntities {
    const val GROUP = "wallets"
    const val WALLET_TYPES = "wallet_types"
    const val CATEGORIES = "categories"
    const val WALLETS = "wallets"
    const val TRANSACTIONS = "wallet_transactions"
    const val TRANSFERS = "transfers"
    const val RECURRING = "recurring_transactions"
    const val MAX_NAME_LENGTH = 50

    fun transactionKey(
        wallet: String?,
        date: java.time.LocalDateTime?,
        type: WalletTransactionType?,
        amount: BigDecimal?,
        description: String?,
    ) = NaturalKeys.of(wallet, date, type, amount, description)

    fun error(
        entity: String,
        row: ParsedRow,
        column: String,
        code: String,
        message: String = "",
    ) = Problem(entity, row.number, column, code, Severity.ERROR, message)

    fun checkReference(
        entity: String,
        row: ParsedRow,
        column: String,
        target: String,
        ctx: ResolveContext,
    ): List<Problem> {
        val value = row.text(column) ?: return emptyList()
        return if (ctx.exists(target, value)) {
            emptyList()
        } else {
            listOf(error(entity, row, column, ProblemCodes.UNRESOLVED_REFERENCE, value))
        }
    }

    fun checkPositive(
        entity: String,
        row: ParsedRow,
        column: String,
    ): List<Problem> {
        val value = row.decimal(column) ?: return emptyList()
        return if (value.toRounded() > BigDecimal.ZERO) {
            emptyList()
        } else {
            listOf(error(entity, row, column, ProblemCodes.DOMAIN_RULE, "positive"))
        }
    }
}

@Component
class WalletTypeSpec(
    private val repository: WalletTypeRepository,
) : EntitySpec {
    override val id = WalletEntities.WALLET_TYPES
    override val group = WalletEntities.GROUP
    override val columns =
        listOf(
            Column(Cols.NAME, CellType.TEXT, required = true),
            Column(Cols.ICON, CellType.TEXT),
        )

    override fun count() = repository.count()

    override fun export() =
        repository.findAllByOrderByNameAsc().asSequence().map {
            mapOf(
                Cols.NAME to it.name,
                Cols.ICON to it.icon,
            )
        }

    override fun naturalKey(row: ParsedRow) = row.text(Cols.NAME)!!

    override fun existingKeys(): Map<String, Int> = repository.findAll().groupingBy { it.name }.eachCount()

    override fun validate(
        row: ParsedRow,
        ctx: ResolveContext,
    ): List<Problem> =
        if (row.text(Cols.NAME)!!.length > WalletEntities.MAX_NAME_LENGTH) {
            listOf(WalletEntities.error(id, row, Cols.NAME, ProblemCodes.DOMAIN_RULE, "length"))
        } else {
            emptyList()
        }

    override fun apply(
        row: ParsedRow,
        ctx: ApplyContext,
    ) {
        repository.save(WalletType(name = row.text(Cols.NAME)!!, icon = row.text(Cols.ICON)))
    }
}

@Component
class CategorySpec(
    private val repository: CategoryRepository,
    private val service: CategoryService,
) : EntitySpec {
    override val id = WalletEntities.CATEGORIES
    override val group = WalletEntities.GROUP
    override val columns =
        listOf(
            Column(Cols.NAME, CellType.TEXT, required = true),
            Column(Cols.ARCHIVED, CellType.BOOLEAN),
        )

    override fun count() = repository.count()

    override fun export() =
        repository.findAll().sortedBy { it.name }.asSequence().map {
            mapOf(
                Cols.NAME to it.name,
                Cols.ARCHIVED to it.isArchived,
            )
        }

    override fun naturalKey(row: ParsedRow) = row.text(Cols.NAME)!!

    override fun existingKeys(): Map<String, Int> = repository.findAll().groupingBy { it.name }.eachCount()

    override fun validate(
        row: ParsedRow,
        ctx: ResolveContext,
    ): List<Problem> =
        if (row.text(Cols.NAME)!!.trim().isEmpty() || row.text(Cols.NAME)!!.length > WalletEntities.MAX_NAME_LENGTH) {
            listOf(WalletEntities.error(id, row, Cols.NAME, ProblemCodes.DOMAIN_RULE, "length"))
        } else {
            emptyList()
        }

    override fun apply(
        row: ParsedRow,
        ctx: ApplyContext,
    ) {
        service.createCategory(Category(name = row.text(Cols.NAME)!!, isArchived = row.bool(Cols.ARCHIVED) ?: false))
    }
}

@Component
class WalletSpec(
    private val walletRepository: WalletRepository,
    private val walletTypeRepository: WalletTypeRepository,
    private val transactionRepository: WalletTransactionRepository,
    private val transferRepository: TransferRepository,
    private val walletService: WalletService,
) : EntitySpec {
    override val id = WalletEntities.WALLETS
    override val group = WalletEntities.GROUP
    override val dependsOn = listOf(WalletEntities.WALLET_TYPES)
    override val columns =
        listOf(
            Column(Cols.NAME, CellType.TEXT, required = true),
            Column(Cols.TYPE, CellType.TEXT, required = true),
            Column(Cols.INITIAL_BALANCE, CellType.DECIMAL, required = true),
            Column(Cols.BALANCE, CellType.DECIMAL),
            Column(Cols.ARCHIVED, CellType.BOOLEAN),
            Column(Cols.MASTER_WALLET, CellType.TEXT),
            Column(Cols.KIND, CellType.TEXT),
            Column(Cols.GOAL_INITIAL_BALANCE, CellType.DECIMAL),
            Column(Cols.TARGET_BALANCE, CellType.DECIMAL),
            Column(Cols.TARGET_DATE, CellType.DATE),
            Column(Cols.COMPLETION_DATE, CellType.DATE),
            Column(Cols.MOTIVATION, CellType.TEXT),
        )

    override fun count() = walletRepository.count()

    override fun export(): Sequence<Map<String, Any?>> {
        val wallets = walletRepository.findAll()
        val initial =
            WalletBalanceCalculator.initialBalances(
                wallets,
                transactionRepository.findAll(),
                transferRepository.findAll(),
            )
        return wallets
            .sortedWith(compareBy({ it.masterWallet != null }, { it.name }))
            .asSequence()
            .map {
                mapOf(
                    Cols.NAME to it.name,
                    Cols.TYPE to it.type.name,
                    Cols.INITIAL_BALANCE to initial.getValue(it.id!!),
                    Cols.BALANCE to it.balance,
                    Cols.ARCHIVED to it.isArchived,
                    Cols.MASTER_WALLET to it.masterWallet?.name,
                    Cols.KIND to if (it is Goal) KIND_GOAL else null,
                    Cols.GOAL_INITIAL_BALANCE to (it as? Goal)?.initialBalance,
                    Cols.TARGET_BALANCE to (it as? Goal)?.targetBalance,
                    Cols.TARGET_DATE to (it as? Goal)?.targetDate,
                    Cols.COMPLETION_DATE to (it as? Goal)?.completionDate,
                    Cols.MOTIVATION to (it as? Goal)?.motivation,
                )
            }
    }

    override fun naturalKey(row: ParsedRow) = row.text(Cols.NAME)!!

    override fun existingKeys(): Map<String, Int> = walletRepository.findAll().groupingBy { it.name }.eachCount()

    override fun validate(
        row: ParsedRow,
        ctx: ResolveContext,
    ): List<Problem> {
        val problems = mutableListOf<Problem>()
        val name = row.text(Cols.NAME)!!
        if (name.trim().isEmpty() || name.length > WalletEntities.MAX_NAME_LENGTH) {
            problems.add(WalletEntities.error(id, row, Cols.NAME, ProblemCodes.DOMAIN_RULE, "length"))
        }
        problems.addAll(WalletEntities.checkReference(id, row, Cols.TYPE, WalletEntities.WALLET_TYPES, ctx))
        problems.addAll(WalletEntities.checkReference(id, row, Cols.MASTER_WALLET, WalletEntities.WALLETS, ctx))
        if (row.text(Cols.MASTER_WALLET) == name) {
            problems.add(WalletEntities.error(id, row, Cols.MASTER_WALLET, ProblemCodes.DOMAIN_RULE, "self"))
        }
        if (row.text(Cols.KIND) == KIND_GOAL) {
            listOf(Cols.GOAL_INITIAL_BALANCE, Cols.TARGET_BALANCE, Cols.TARGET_DATE).forEach { column ->
                if (row.values[column] == null) {
                    problems.add(
                        WalletEntities.error(id, row, column, ProblemCodes.INVALID_VALUE, "required for a goal"),
                    )
                }
            }
            val initial = row.decimal(Cols.GOAL_INITIAL_BALANCE)
            val target = row.decimal(Cols.TARGET_BALANCE)
            if (initial != null && target != null && (initial.signum() < 0 || initial > target)) {
                problems.add(
                    WalletEntities.error(
                        id,
                        row,
                        Cols.GOAL_INITIAL_BALANCE,
                        ProblemCodes.DOMAIN_RULE,
                        "initial above target",
                    ),
                )
            }
        }
        return problems
    }

    override fun applyOrder(rows: List<ParsedRow>): List<ParsedRow> =
        rows.sortedBy {
            it.text(Cols.MASTER_WALLET) !=
                null
        }

    private companion object {
        const val KIND_GOAL = "GOAL"
    }

    override fun apply(
        row: ParsedRow,
        ctx: ApplyContext,
    ) {
        val type = walletTypeRepository.findByName(row.text(Cols.TYPE)!!).get()
        val master = row.text(Cols.MASTER_WALLET)?.let { walletRepository.findByName(it).get() }
        val initialBalance = row.decimal(Cols.INITIAL_BALANCE)!!
        val wallet =
            if (row.text(Cols.KIND) == KIND_GOAL) {
                Goal(
                    initialBalance = row.decimal(Cols.GOAL_INITIAL_BALANCE)!!,
                    targetBalance = row.decimal(Cols.TARGET_BALANCE)!!,
                    targetDate = row.date(Cols.TARGET_DATE)!!,
                    completionDate = row.date(Cols.COMPLETION_DATE),
                    motivation = row.text(Cols.MOTIVATION),
                    name = row.text(Cols.NAME)!!,
                    type = type,
                    isArchived = row.bool(Cols.ARCHIVED) ?: false,
                    masterWallet = master,
                ).also { it.balance = initialBalance.toRounded() }
            } else {
                Wallet(
                    type = type,
                    name = row.text(Cols.NAME)!!,
                    balance = initialBalance,
                    isArchived = row.bool(Cols.ARCHIVED) ?: false,
                    masterWallet = master,
                )
            }
        walletService.createWallet(wallet)
    }
}

@Component
class WalletTransactionSpec(
    private val transactionRepository: WalletTransactionRepository,
    private val walletRepository: WalletRepository,
    private val categoryRepository: CategoryRepository,
    private val walletService: WalletService,
) : EntitySpec {
    override val id = WalletEntities.TRANSACTIONS
    override val group = WalletEntities.GROUP
    override val dependsOn = listOf(WalletEntities.WALLETS, WalletEntities.CATEGORIES)
    override val refColumn = Cols.REF
    override val columns =
        listOf(
            Column(Cols.REF, CellType.TEXT),
            Column(Cols.WALLET, CellType.TEXT, required = true),
            Column(Cols.CATEGORY, CellType.TEXT, required = true),
            Column(Cols.TYPE, CellType.ENUM, required = true, enumClass = WalletTransactionType::class.java),
            Column(Cols.STATUS, CellType.ENUM, required = true, enumClass = WalletTransactionStatus::class.java),
            Column(Cols.DATE, CellType.DATETIME, required = true),
            Column(Cols.AMOUNT, CellType.DECIMAL, required = true),
            Column(Cols.DESCRIPTION, CellType.TEXT),
            Column(Cols.INCLUDE_IN_ANALYSIS, CellType.BOOLEAN),
        )

    override fun count() = transactionRepository.count()

    override fun export() =
        transactionRepository
            .findAll()
            .sortedWith(compareBy({ it.date }, { it.id }))
            .asSequence()
            .mapIndexed { index, it ->
                mapOf(
                    Cols.REF to "wt-${index + 1}",
                    Cols.WALLET to it.wallet.name,
                    Cols.CATEGORY to it.category.name,
                    Cols.TYPE to it.type,
                    Cols.STATUS to it.status,
                    Cols.DATE to it.date,
                    Cols.AMOUNT to it.amount,
                    Cols.DESCRIPTION to it.description,
                    Cols.INCLUDE_IN_ANALYSIS to it.includeInAnalysis,
                )
            }

    override fun naturalKey(row: ParsedRow) =
        WalletEntities.transactionKey(
            row.text(Cols.WALLET),
            row.dateTime(Cols.DATE),
            row.enum<WalletTransactionType>(Cols.TYPE),
            row.decimal(Cols.AMOUNT),
            row.text(Cols.DESCRIPTION),
        )

    override fun existingKeys(): Map<String, Int> =
        transactionRepository
            .findAll()
            .groupingBy { WalletEntities.transactionKey(it.wallet.name, it.date, it.type, it.amount, it.description) }
            .eachCount()

    override fun validate(
        row: ParsedRow,
        ctx: ResolveContext,
    ): List<Problem> =
        WalletEntities.checkReference(id, row, Cols.WALLET, WalletEntities.WALLETS, ctx) +
            WalletEntities.checkReference(id, row, Cols.CATEGORY, WalletEntities.CATEGORIES, ctx) +
            WalletEntities.checkPositive(id, row, Cols.AMOUNT)

    override fun applyOrder(rows: List<ParsedRow>): List<ParsedRow> = rows.sortedBy { it.dateTime(Cols.DATE) }

    override fun apply(
        row: ParsedRow,
        ctx: ApplyContext,
    ) {
        val savedId =
            walletService.createWalletTransaction(
                WalletTransaction(
                    date = row.dateTime(Cols.DATE)!!,
                    status = row.enum<WalletTransactionStatus>(Cols.STATUS)!!,
                    description = row.text(Cols.DESCRIPTION),
                    includeInAnalysis = row.bool(Cols.INCLUDE_IN_ANALYSIS) ?: true,
                    wallet = walletRepository.findByName(row.text(Cols.WALLET)!!).get(),
                    category = categoryRepository.findByName(row.text(Cols.CATEGORY)!!)!!,
                    type = row.enum<WalletTransactionType>(Cols.TYPE)!!,
                    amount = row.decimal(Cols.AMOUNT)!!,
                ),
                publishNotification = false,
                enforceBalanceRules = false,
            )
        row.text(Cols.REF)?.let { ctx.remember(id, it, savedId) }
    }
}

@Component
class TransferSpec(
    private val transferRepository: TransferRepository,
    private val walletRepository: WalletRepository,
    private val categoryRepository: CategoryRepository,
    private val walletService: WalletService,
) : EntitySpec {
    override val id = WalletEntities.TRANSFERS
    override val group = WalletEntities.GROUP
    override val dependsOn = listOf(WalletEntities.WALLETS, WalletEntities.CATEGORIES)
    override val columns =
        listOf(
            Column(Cols.SENDER_WALLET, CellType.TEXT, required = true),
            Column(Cols.RECEIVER_WALLET, CellType.TEXT, required = true),
            Column(Cols.DATE, CellType.DATETIME, required = true),
            Column(Cols.AMOUNT, CellType.DECIMAL, required = true),
            Column(Cols.DESCRIPTION, CellType.TEXT),
            Column(Cols.CATEGORY, CellType.TEXT),
        )

    override fun count() = transferRepository.count()

    override fun export() =
        transferRepository
            .findAll()
            .sortedWith(compareBy({ it.date }, { it.id }))
            .asSequence()
            .map {
                mapOf(
                    Cols.SENDER_WALLET to it.senderWallet.name,
                    Cols.RECEIVER_WALLET to it.receiverWallet.name,
                    Cols.DATE to it.date,
                    Cols.AMOUNT to it.amount,
                    Cols.DESCRIPTION to it.description,
                    Cols.CATEGORY to it.category?.name,
                )
            }

    override fun naturalKey(row: ParsedRow) =
        NaturalKeys.of(
            row.text(Cols.SENDER_WALLET),
            row.text(Cols.RECEIVER_WALLET),
            row.dateTime(Cols.DATE),
            row.decimal(Cols.AMOUNT),
            row.text(Cols.DESCRIPTION),
        )

    override fun existingKeys(): Map<String, Int> =
        transferRepository
            .findAll()
            .groupingBy {
                NaturalKeys.of(
                    it.senderWallet.name,
                    it.receiverWallet.name,
                    it.date,
                    it.amount,
                    it.description,
                )
            }.eachCount()

    override fun validate(
        row: ParsedRow,
        ctx: ResolveContext,
    ): List<Problem> {
        val problems = mutableListOf<Problem>()
        problems.addAll(WalletEntities.checkReference(id, row, Cols.SENDER_WALLET, WalletEntities.WALLETS, ctx))
        problems.addAll(WalletEntities.checkReference(id, row, Cols.RECEIVER_WALLET, WalletEntities.WALLETS, ctx))
        problems.addAll(WalletEntities.checkReference(id, row, Cols.CATEGORY, WalletEntities.CATEGORIES, ctx))
        problems.addAll(WalletEntities.checkPositive(id, row, Cols.AMOUNT))
        if (row.text(Cols.SENDER_WALLET) == row.text(Cols.RECEIVER_WALLET)) {
            problems.add(WalletEntities.error(id, row, Cols.RECEIVER_WALLET, ProblemCodes.DOMAIN_RULE, "same wallet"))
        }
        return problems
    }

    override fun applyOrder(rows: List<ParsedRow>): List<ParsedRow> = rows.sortedBy { it.dateTime(Cols.DATE) }

    override fun apply(
        row: ParsedRow,
        ctx: ApplyContext,
    ) {
        walletService.createTransfer(
            Transfer(
                senderWallet = walletRepository.findByName(row.text(Cols.SENDER_WALLET)!!).get(),
                receiverWallet = walletRepository.findByName(row.text(Cols.RECEIVER_WALLET)!!).get(),
                date = row.dateTime(Cols.DATE)!!,
                amount = row.decimal(Cols.AMOUNT)!!,
                description = row.text(Cols.DESCRIPTION),
                category = row.text(Cols.CATEGORY)?.let { categoryRepository.findByName(it) },
            ),
            enforceBalanceRules = false,
        )
    }
}

@Component
class RecurringTransactionSpec(
    private val repository: RecurringTransactionRepository,
    private val walletRepository: WalletRepository,
    private val categoryRepository: CategoryRepository,
    private val service: RecurringTransactionService,
) : EntitySpec {
    override val id = WalletEntities.RECURRING
    override val group = WalletEntities.GROUP
    override val dependsOn = listOf(WalletEntities.WALLETS, WalletEntities.CATEGORIES)
    override val columns =
        listOf(
            Column(Cols.WALLET, CellType.TEXT, required = true),
            Column(Cols.CATEGORY, CellType.TEXT, required = true),
            Column(Cols.TYPE, CellType.ENUM, required = true, enumClass = WalletTransactionType::class.java),
            Column(Cols.AMOUNT, CellType.DECIMAL, required = true),
            Column(Cols.DESCRIPTION, CellType.TEXT),
            Column(Cols.INCLUDE_IN_ANALYSIS, CellType.BOOLEAN),
            Column(Cols.START_DATE, CellType.DATE, required = true),
            Column(Cols.END_DATE, CellType.DATE),
            Column(Cols.NEXT_DUE_DATE, CellType.DATE, required = true),
            Column(
                Cols.FREQUENCY,
                CellType.ENUM,
                required = true,
                enumClass = RecurringTransactionFrequency::class.java,
            ),
            Column(Cols.STATUS, CellType.ENUM, required = true, enumClass = RecurringTransactionStatus::class.java),
            Column(Cols.INCLUDE_IN_NET_WORTH, CellType.BOOLEAN),
        )

    override fun count() = repository.count()

    override fun export() =
        repository
            .findAll()
            .sortedWith(compareBy({ it.startDate }, { it.id }))
            .asSequence()
            .map {
                mapOf(
                    Cols.WALLET to it.wallet.name,
                    Cols.CATEGORY to it.category.name,
                    Cols.TYPE to it.type,
                    Cols.AMOUNT to it.amount,
                    Cols.DESCRIPTION to it.description,
                    Cols.INCLUDE_IN_ANALYSIS to it.includeInAnalysis,
                    Cols.START_DATE to it.startDate,
                    Cols.END_DATE to it.endDate,
                    Cols.NEXT_DUE_DATE to it.nextDueDate,
                    Cols.FREQUENCY to it.frequency,
                    Cols.STATUS to it.status,
                    Cols.INCLUDE_IN_NET_WORTH to it.includeInNetWorth,
                )
            }

    override fun naturalKey(row: ParsedRow) =
        NaturalKeys.of(
            row.text(Cols.WALLET),
            row.date(Cols.START_DATE),
            row.values[Cols.FREQUENCY],
            row.decimal(Cols.AMOUNT),
            row.text(Cols.DESCRIPTION),
        )

    override fun existingKeys(): Map<String, Int> =
        repository
            .findAll()
            .groupingBy { NaturalKeys.of(it.wallet.name, it.startDate, it.frequency, it.amount, it.description) }
            .eachCount()

    override fun validate(
        row: ParsedRow,
        ctx: ResolveContext,
    ): List<Problem> {
        val problems = mutableListOf<Problem>()
        problems.addAll(WalletEntities.checkReference(id, row, Cols.WALLET, WalletEntities.WALLETS, ctx))
        problems.addAll(WalletEntities.checkReference(id, row, Cols.CATEGORY, WalletEntities.CATEGORIES, ctx))
        problems.addAll(WalletEntities.checkPositive(id, row, Cols.AMOUNT))
        val start = row.date(Cols.START_DATE)!!
        val frequency = row.enum<RecurringTransactionFrequency>(Cols.FREQUENCY)!!
        val end = row.date(Cols.END_DATE)
        if (end != null && start.plus(1, frequency.chronoUnit).isAfter(end)) {
            problems.add(WalletEntities.error(id, row, Cols.END_DATE, ProblemCodes.DOMAIN_RULE, "end before start"))
        }
        if (row.date(Cols.NEXT_DUE_DATE)!!.isBefore(start)) {
            problems.add(
                WalletEntities.error(id, row, Cols.NEXT_DUE_DATE, ProblemCodes.DOMAIN_RULE, "next due before start"),
            )
        }
        return problems
    }

    override fun apply(
        row: ParsedRow,
        ctx: ApplyContext,
    ) {
        val start = row.date(Cols.START_DATE)!!
        val end = row.date(Cols.END_DATE)
        service.createRecurringTransaction(
            RecurringTransaction(
                startDate = start,
                endDate = end ?: org.moinex.common.constant.Constants.RECURRING_TRANSACTION_DEFAULT_END_DATE,
                nextDueDate = row.date(Cols.NEXT_DUE_DATE)!!,
                frequency = row.enum<RecurringTransactionFrequency>(Cols.FREQUENCY)!!,
                status = row.enum<RecurringTransactionStatus>(Cols.STATUS)!!,
                includeInNetWorth = row.bool(Cols.INCLUDE_IN_NET_WORTH) ?: false,
                description = row.text(Cols.DESCRIPTION),
                wallet = walletRepository.findByName(row.text(Cols.WALLET)!!).get(),
                category = categoryRepository.findByName(row.text(Cols.CATEGORY)!!)!!,
                type = row.enum<WalletTransactionType>(Cols.TYPE)!!,
                amount = row.decimal(Cols.AMOUNT)!!,
                includeInAnalysis = row.bool(Cols.INCLUDE_IN_ANALYSIS) ?: true,
            ),
        )
    }
}
