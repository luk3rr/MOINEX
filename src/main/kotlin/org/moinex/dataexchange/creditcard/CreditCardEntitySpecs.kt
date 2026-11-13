/*
 * Filename: CreditCardEntitySpecs.kt
 * Created on: November 13, 2026
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 */

package org.moinex.dataexchange.creditcard

import org.moinex.common.constant.Constants
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
import org.moinex.dataexchange.wallet.WalletEntities
import org.moinex.model.creditcard.CreditCard
import org.moinex.model.creditcard.CreditCardCredit
import org.moinex.model.creditcard.CreditCardDebt
import org.moinex.model.creditcard.CreditCardOperator
import org.moinex.model.creditcard.RecurringCreditCardDebt
import org.moinex.model.enums.CreditCardCreditType
import org.moinex.model.enums.CreditCardRecurringFrequency
import org.moinex.model.enums.RecurringTransactionStatus
import org.moinex.repository.CategoryRepository
import org.moinex.repository.creditcard.CreditCardCreditRepository
import org.moinex.repository.creditcard.CreditCardDebtRepository
import org.moinex.repository.creditcard.CreditCardOperatorRepository
import org.moinex.repository.creditcard.CreditCardPaymentRepository
import org.moinex.repository.creditcard.CreditCardRepository
import org.moinex.repository.creditcard.RecurringCreditCardDebtRepository
import org.moinex.repository.wallettransaction.WalletRepository
import org.moinex.service.creditcard.CreditCardService
import org.springframework.stereotype.Component
import java.math.BigDecimal
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeParseException

object CreditCardEntities {
    const val GROUP = "credit_cards"
    const val OPERATORS = "credit_card_operators"
    const val CARDS = "credit_cards"
    const val RECURRING = "recurring_credit_card_debts"
    const val DEBTS = "credit_card_debts"
    const val PAYMENTS = "credit_card_payments"
    const val CREDITS = "credit_card_credits"

    fun monthProblem(
        entity: String,
        row: ParsedRow,
        column: String,
    ): List<Problem> {
        val value = row.text(column) ?: return emptyList()
        return try {
            YearMonth.parse(value)
            emptyList()
        } catch (_: DateTimeParseException) {
            listOf(WalletEntities.error(entity, row, column, ProblemCodes.INVALID_VALUE, value))
        }
    }

    fun inRange(
        entity: String,
        row: ParsedRow,
        column: String,
        range: IntRange,
    ): List<Problem> {
        val value = row.int(column) ?: return emptyList()
        return if (value in range) {
            emptyList()
        } else {
            listOf(WalletEntities.error(entity, row, column, ProblemCodes.DOMAIN_RULE, "$range"))
        }
    }

    fun debtKey(
        card: String?,
        date: Any?,
        amount: BigDecimal?,
        description: String?,
    ) = NaturalKeys.of(card, date, amount, description)

    fun recurringKey(
        card: String?,
        start: LocalDate?,
        frequency: Any?,
        amount: BigDecimal?,
        description: String?,
    ) = NaturalKeys.of(card, start, frequency, amount, description)
}

@Component
class CreditCardOperatorSpec(
    private val repository: CreditCardOperatorRepository,
) : EntitySpec {
    override val id = CreditCardEntities.OPERATORS
    override val group = CreditCardEntities.GROUP
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
        if (row.text(Cols.NAME)!!.trim().isEmpty() || row.text(Cols.NAME)!!.length > WalletEntities.MAX_NAME_LENGTH) {
            listOf(WalletEntities.error(id, row, Cols.NAME, ProblemCodes.DOMAIN_RULE, "length"))
        } else {
            emptyList()
        }

    override fun apply(
        row: ParsedRow,
        ctx: ApplyContext,
    ) {
        repository.save(CreditCardOperator(name = row.text(Cols.NAME)!!, icon = row.text(Cols.ICON)))
    }
}

@Component
class CreditCardSpec(
    private val repository: CreditCardRepository,
    private val operatorRepository: CreditCardOperatorRepository,
    private val walletRepository: WalletRepository,
    private val service: CreditCardService,
) : EntitySpec {
    override val id = CreditCardEntities.CARDS
    override val group = CreditCardEntities.GROUP
    override val dependsOn = listOf(CreditCardEntities.OPERATORS, WalletEntities.WALLETS)
    override val columns =
        listOf(
            Column(Cols.NAME, CellType.TEXT, required = true),
            Column(Cols.OPERATOR, CellType.TEXT, required = true),
            Column(Cols.DEFAULT_BILLING_WALLET, CellType.TEXT),
            Column(Cols.BILLING_DUE_DAY, CellType.INT, required = true),
            Column(Cols.CLOSING_DAY, CellType.INT, required = true),
            Column(Cols.MAX_DEBT, CellType.DECIMAL, required = true),
            Column(Cols.AVAILABLE_REBATE, CellType.DECIMAL),
            Column(Cols.LAST_FOUR_DIGITS, CellType.TEXT),
            Column(Cols.ARCHIVED, CellType.BOOLEAN),
        )

    override fun count() = repository.count()

    override fun export() =
        repository.findAllByOrderByNameAsc().asSequence().map {
            mapOf(
                Cols.NAME to it.name,
                Cols.OPERATOR to it.operator.name,
                Cols.DEFAULT_BILLING_WALLET to it.defaultBillingWallet?.name,
                Cols.BILLING_DUE_DAY to it.billingDueDay,
                Cols.CLOSING_DAY to it.closingDay,
                Cols.MAX_DEBT to it.maxDebt,
                Cols.AVAILABLE_REBATE to it.availableRebate,
                Cols.LAST_FOUR_DIGITS to it.lastFourDigits,
                Cols.ARCHIVED to it.isArchived,
            )
        }

    override fun naturalKey(row: ParsedRow) = row.text(Cols.NAME)!!

    override fun existingKeys(): Map<String, Int> = repository.findAll().groupingBy { it.name }.eachCount()

    override fun validate(
        row: ParsedRow,
        ctx: ResolveContext,
    ): List<Problem> {
        val problems = mutableListOf<Problem>()
        if (row.text(Cols.NAME)!!.trim().isEmpty() || row.text(Cols.NAME)!!.length > WalletEntities.MAX_NAME_LENGTH) {
            problems.add(WalletEntities.error(id, row, Cols.NAME, ProblemCodes.DOMAIN_RULE, "length"))
        }
        problems.addAll(WalletEntities.checkReference(id, row, Cols.OPERATOR, CreditCardEntities.OPERATORS, ctx))
        problems.addAll(
            WalletEntities.checkReference(id, row, Cols.DEFAULT_BILLING_WALLET, WalletEntities.WALLETS, ctx),
        )
        problems.addAll(CreditCardEntities.inRange(id, row, Cols.BILLING_DUE_DAY, 1..Constants.MAX_BILLING_DUE_DAY))
        problems.addAll(CreditCardEntities.inRange(id, row, Cols.CLOSING_DAY, 1..Constants.MAX_BILLING_DUE_DAY))
        problems.addAll(WalletEntities.checkPositive(id, row, Cols.MAX_DEBT))
        row.text(Cols.LAST_FOUR_DIGITS)?.let {
            if (!it.matches(Regex("\\d{4}"))) {
                problems.add(WalletEntities.error(id, row, Cols.LAST_FOUR_DIGITS, ProblemCodes.DOMAIN_RULE, "4 digits"))
            }
        }
        return problems
    }

    override fun apply(
        row: ParsedRow,
        ctx: ApplyContext,
    ) {
        val operator = operatorRepository.findAll().first { it.name == row.text(Cols.OPERATOR) }
        val wallet = row.text(Cols.DEFAULT_BILLING_WALLET)?.let { walletRepository.findByName(it).get() }
        val id =
            service.createCreditCard(
                CreditCard(
                    operator = operator,
                    defaultBillingWallet = wallet,
                    name = row.text(Cols.NAME)!!,
                    billingDueDay = row.int(Cols.BILLING_DUE_DAY)!!,
                    closingDay = row.int(Cols.CLOSING_DAY)!!,
                    maxDebt = row.decimal(Cols.MAX_DEBT)!!,
                    lastFourDigits = row.text(Cols.LAST_FOUR_DIGITS),
                ),
            )
        repository.findById(id).get().apply {
            availableRebate = (row.decimal(Cols.AVAILABLE_REBATE) ?: BigDecimal.ZERO).toRounded()
            isArchived = row.bool(Cols.ARCHIVED) ?: false
        }
    }
}

@Component
class RecurringCreditCardDebtSpec(
    private val repository: RecurringCreditCardDebtRepository,
    private val cardRepository: CreditCardRepository,
    private val categoryRepository: CategoryRepository,
) : EntitySpec {
    override val id = CreditCardEntities.RECURRING
    override val group = CreditCardEntities.GROUP
    override val dependsOn = listOf(CreditCardEntities.CARDS, WalletEntities.CATEGORIES)
    override val columns =
        listOf(
            Column(Cols.CREDIT_CARD, CellType.TEXT, required = true),
            Column(Cols.CATEGORY, CellType.TEXT, required = true),
            Column(Cols.AMOUNT, CellType.DECIMAL, required = true),
            Column(Cols.DESCRIPTION, CellType.TEXT),
            Column(Cols.DAY_OF_MONTH, CellType.INT, required = true),
            Column(
                Cols.FREQUENCY,
                CellType.ENUM,
                required = true,
                enumClass = CreditCardRecurringFrequency::class.java,
            ),
            Column(Cols.STATUS, CellType.ENUM, required = true, enumClass = RecurringTransactionStatus::class.java),
            Column(Cols.START_DATE, CellType.DATE, required = true),
            Column(Cols.END_DATE, CellType.DATE),
            Column(Cols.NEXT_INVOICE_MONTH, CellType.TEXT, required = true),
        )

    override fun count() = repository.count()

    override fun export() =
        repository
            .findAll()
            .sortedWith(compareBy({ it.startDate }, { it.id }))
            .asSequence()
            .map {
                mapOf(
                    Cols.CREDIT_CARD to it.creditCard.name,
                    Cols.CATEGORY to it.category.name,
                    Cols.AMOUNT to it.amount,
                    Cols.DESCRIPTION to it.description,
                    Cols.DAY_OF_MONTH to it.dayOfMonth,
                    Cols.FREQUENCY to it.frequency,
                    Cols.STATUS to it.status,
                    Cols.START_DATE to it.startDate,
                    Cols.END_DATE to it.endDate,
                    Cols.NEXT_INVOICE_MONTH to it.nextInvoiceMonth.toString(),
                )
            }

    override fun naturalKey(row: ParsedRow) =
        CreditCardEntities.recurringKey(
            row.text(Cols.CREDIT_CARD),
            row.date(Cols.START_DATE),
            row.values[Cols.FREQUENCY],
            row.decimal(Cols.AMOUNT),
            row.text(Cols.DESCRIPTION),
        )

    override fun existingKeys(): Map<String, Int> =
        repository
            .findAll()
            .groupingBy {
                CreditCardEntities.recurringKey(
                    it.creditCard.name,
                    it.startDate,
                    it.frequency,
                    it.amount,
                    it.description,
                )
            }.eachCount()

    override fun validate(
        row: ParsedRow,
        ctx: ResolveContext,
    ): List<Problem> {
        val problems = mutableListOf<Problem>()
        problems.addAll(WalletEntities.checkReference(id, row, Cols.CREDIT_CARD, CreditCardEntities.CARDS, ctx))
        problems.addAll(WalletEntities.checkReference(id, row, Cols.CATEGORY, WalletEntities.CATEGORIES, ctx))
        problems.addAll(WalletEntities.checkPositive(id, row, Cols.AMOUNT))
        problems.addAll(CreditCardEntities.inRange(id, row, Cols.DAY_OF_MONTH, 1..Constants.MAX_BILLING_DUE_DAY))
        problems.addAll(CreditCardEntities.monthProblem(id, row, Cols.NEXT_INVOICE_MONTH))
        val end = row.date(Cols.END_DATE)
        val frequency = row.enum<CreditCardRecurringFrequency>(Cols.FREQUENCY)!!
        if (end != null && row.date(Cols.START_DATE)!!.plus(1, frequency.chronoUnit).isAfter(end)) {
            problems.add(WalletEntities.error(id, row, Cols.END_DATE, ProblemCodes.DOMAIN_RULE, "end before start"))
        }
        return problems
    }

    override fun apply(
        row: ParsedRow,
        ctx: ApplyContext,
    ) {
        repository.save(
            RecurringCreditCardDebt(
                creditCard = cardRepository.findAll().first { it.name == row.text(Cols.CREDIT_CARD) },
                category = categoryRepository.findByName(row.text(Cols.CATEGORY)!!)!!,
                amount = row.decimal(Cols.AMOUNT)!!,
                description = row.text(Cols.DESCRIPTION),
                dayOfMonth = row.int(Cols.DAY_OF_MONTH)!!,
                frequency = row.enum<CreditCardRecurringFrequency>(Cols.FREQUENCY)!!,
                status = row.enum<RecurringTransactionStatus>(Cols.STATUS)!!,
                startDate = row.date(Cols.START_DATE)!!,
                endDate = row.date(Cols.END_DATE) ?: Constants.RECURRING_TRANSACTION_DEFAULT_END_DATE,
                nextInvoiceMonth = YearMonth.parse(row.text(Cols.NEXT_INVOICE_MONTH)!!),
            ),
        )
    }
}

@Component
class CreditCardDebtSpec(
    private val repository: CreditCardDebtRepository,
    private val paymentRepository: CreditCardPaymentRepository,
    private val recurringRepository: RecurringCreditCardDebtRepository,
    private val cardRepository: CreditCardRepository,
    private val categoryRepository: CategoryRepository,
    private val service: CreditCardService,
) : EntitySpec {
    override val id = CreditCardEntities.DEBTS
    override val group = CreditCardEntities.GROUP
    override val dependsOn = listOf(CreditCardEntities.CARDS, WalletEntities.CATEGORIES, CreditCardEntities.RECURRING)
    override val columns =
        listOf(
            Column(Cols.CREDIT_CARD, CellType.TEXT, required = true),
            Column(Cols.CATEGORY, CellType.TEXT, required = true),
            Column(Cols.DATE, CellType.DATETIME, required = true),
            Column(Cols.AMOUNT, CellType.DECIMAL, required = true),
            Column(Cols.DESCRIPTION, CellType.TEXT),
            Column(Cols.INSTALLMENTS, CellType.INT, required = true),
            Column(Cols.INVOICE_MONTH, CellType.TEXT, required = true),
            Column(Cols.RECURRING_SOURCE, CellType.TEXT),
        )

    override fun count() = repository.count()

    override fun export(): Sequence<Map<String, Any?>> {
        val firstPayments =
            paymentRepository
                .findAll()
                .filter { it.installment == 1 }
                .associate { it.creditCardDebt.id to YearMonth.from(it.date) }
        return repository
            .findAll()
            .sortedWith(compareBy({ it.date }, { it.id }))
            .asSequence()
            .map {
                mapOf(
                    Cols.CREDIT_CARD to it.creditCard.name,
                    Cols.CATEGORY to it.category.name,
                    Cols.DATE to it.date,
                    Cols.AMOUNT to it.amount,
                    Cols.DESCRIPTION to it.description,
                    Cols.INSTALLMENTS to it.installments,
                    Cols.INVOICE_MONTH to (firstPayments[it.id] ?: YearMonth.from(it.date)).toString(),
                    Cols.RECURRING_SOURCE to
                        it.recurringSource?.let { r ->
                            NaturalKeys.toCell(
                                CreditCardEntities.recurringKey(
                                    r.creditCard.name,
                                    r.startDate,
                                    r.frequency,
                                    r.amount,
                                    r.description,
                                ),
                            )
                        },
                )
            }
    }

    override fun naturalKey(row: ParsedRow) =
        CreditCardEntities.debtKey(
            row.text(Cols.CREDIT_CARD),
            row.dateTime(Cols.DATE),
            row.decimal(Cols.AMOUNT),
            row.text(Cols.DESCRIPTION),
        )

    override fun existingKeys(): Map<String, Int> =
        repository
            .findAll()
            .groupingBy { CreditCardEntities.debtKey(it.creditCard.name, it.date, it.amount, it.description) }
            .eachCount()

    override fun validate(
        row: ParsedRow,
        ctx: ResolveContext,
    ): List<Problem> {
        val problems = mutableListOf<Problem>()
        problems.addAll(WalletEntities.checkReference(id, row, Cols.CREDIT_CARD, CreditCardEntities.CARDS, ctx))
        problems.addAll(WalletEntities.checkReference(id, row, Cols.CATEGORY, WalletEntities.CATEGORIES, ctx))
        problems.addAll(WalletEntities.checkPositive(id, row, Cols.AMOUNT))
        problems.addAll(CreditCardEntities.inRange(id, row, Cols.INSTALLMENTS, 1..Constants.MAX_INSTALLMENTS))
        problems.addAll(CreditCardEntities.monthProblem(id, row, Cols.INVOICE_MONTH))
        row.text(Cols.RECURRING_SOURCE)?.let { cell ->
            val key = NaturalKeys.fromCell(cell)
            if (!ctx.exists(CreditCardEntities.RECURRING, key)) {
                problems.add(WalletEntities.error(id, row, Cols.RECURRING_SOURCE, ProblemCodes.UNRESOLVED_REFERENCE))
            }
        }
        return problems
    }

    override fun applyOrder(rows: List<ParsedRow>): List<ParsedRow> = rows.sortedBy { it.dateTime(Cols.DATE) }

    override fun apply(
        row: ParsedRow,
        ctx: ApplyContext,
    ) {
        val source =
            row.text(Cols.RECURRING_SOURCE)?.let { cell ->
                val key = NaturalKeys.fromCell(cell)
                recurringRepository.findAll().first {
                    CreditCardEntities.recurringKey(
                        it.creditCard.name,
                        it.startDate,
                        it.frequency,
                        it.amount,
                        it.description,
                    ) ==
                        key
                }
            }
        service.createDebt(
            CreditCardDebt(
                category = categoryRepository.findByName(row.text(Cols.CATEGORY)!!)!!,
                installments = row.int(Cols.INSTALLMENTS)!!,
                recurringSource = source,
                creditCard = cardRepository.findAll().first { it.name == row.text(Cols.CREDIT_CARD) },
                date = row.dateTime(Cols.DATE)!!,
                amount = row.decimal(Cols.AMOUNT)!!,
                description = row.text(Cols.DESCRIPTION),
            ),
            YearMonth.parse(row.text(Cols.INVOICE_MONTH)!!),
            publishNotification = false,
            enforceCreditLimit = false,
        )
    }
}

@Component
class CreditCardPaymentSpec(
    private val repository: CreditCardPaymentRepository,
    private val debtRepository: CreditCardDebtRepository,
    private val walletRepository: WalletRepository,
) : EntitySpec {
    override val id = CreditCardEntities.PAYMENTS
    override val group = CreditCardEntities.GROUP
    override val dependsOn = listOf(CreditCardEntities.DEBTS, WalletEntities.WALLETS)
    override val columns =
        listOf(
            Column(Cols.CREDIT_CARD, CellType.TEXT, required = true),
            Column(Cols.DEBT_DATE, CellType.DATETIME, required = true),
            Column(Cols.DEBT_AMOUNT, CellType.DECIMAL, required = true),
            Column(Cols.DEBT_DESCRIPTION, CellType.TEXT),
            Column(Cols.INSTALLMENT, CellType.INT, required = true),
            Column(Cols.AMOUNT, CellType.DECIMAL, required = true),
            Column(Cols.PAID_AMOUNT, CellType.DECIMAL),
            Column(Cols.REBATE_USED, CellType.DECIMAL),
            Column(Cols.WALLET, CellType.TEXT),
            Column(Cols.REFUNDED, CellType.BOOLEAN),
            Column(Cols.DATE, CellType.DATETIME, required = true),
        )

    override fun count() = repository.count()

    override fun export() =
        repository
            .findAll()
            .sortedWith(compareBy({ it.creditCardDebt.date }, { it.creditCardDebt.id }, { it.installment }))
            .asSequence()
            .map {
                mapOf(
                    Cols.CREDIT_CARD to it.creditCardDebt.creditCard.name,
                    Cols.DEBT_DATE to it.creditCardDebt.date,
                    Cols.DEBT_AMOUNT to it.creditCardDebt.amount,
                    Cols.DEBT_DESCRIPTION to it.creditCardDebt.description,
                    Cols.INSTALLMENT to it.installment,
                    Cols.AMOUNT to it.amount,
                    Cols.PAID_AMOUNT to it.paidAmount,
                    Cols.REBATE_USED to it.rebateUsed,
                    Cols.WALLET to it.wallet?.name,
                    Cols.REFUNDED to it.refunded,
                    Cols.DATE to it.date,
                )
            }

    private fun debtKey(row: ParsedRow) =
        CreditCardEntities.debtKey(
            row.text(Cols.CREDIT_CARD),
            row.dateTime(Cols.DEBT_DATE),
            row.decimal(Cols.DEBT_AMOUNT),
            row.text(Cols.DEBT_DESCRIPTION),
        )

    override fun naturalKey(row: ParsedRow) = NaturalKeys.of(debtKey(row), row.int(Cols.INSTALLMENT))

    override fun existingKeys(): Map<String, Int> =
        repository
            .findAll()
            .groupingBy {
                val debt = it.creditCardDebt
                NaturalKeys.of(
                    CreditCardEntities.debtKey(debt.creditCard.name, debt.date, debt.amount, debt.description),
                    it.installment,
                )
            }.eachCount()

    override fun validate(
        row: ParsedRow,
        ctx: ResolveContext,
    ): List<Problem> {
        val problems = mutableListOf<Problem>()
        if (!ctx.exists(CreditCardEntities.DEBTS, debtKey(row))) {
            problems.add(WalletEntities.error(id, row, Cols.DEBT_DATE, ProblemCodes.UNRESOLVED_REFERENCE))
        }
        problems.addAll(WalletEntities.checkReference(id, row, Cols.WALLET, WalletEntities.WALLETS, ctx))
        problems.addAll(WalletEntities.checkPositive(id, row, Cols.AMOUNT))
        problems.addAll(CreditCardEntities.inRange(id, row, Cols.INSTALLMENT, 1..Constants.MAX_INSTALLMENTS))
        listOf(Cols.PAID_AMOUNT, Cols.REBATE_USED).forEach { column ->
            val value = row.decimal(column)
            if (value != null && value < BigDecimal.ZERO) {
                problems.add(
                    Problem(
                        id,
                        row.number,
                        column,
                        ProblemCodes.ADJUSTED_VALUE,
                        Severity.WARNING,
                        "${value.toPlainString()} -> 0.00",
                    ),
                )
            }
        }
        return problems
    }

    override fun apply(
        row: ParsedRow,
        ctx: ApplyContext,
    ) {
        val debtIds =
            ctx.memo("credit_card_debt_ids") {
                val map = mutableMapOf<String, Int>()
                debtRepository.findAll().forEach {
                    map.putIfAbsent(
                        CreditCardEntities.debtKey(it.creditCard.name, it.date, it.amount, it.description),
                        it.id!!,
                    )
                }
                map
            }
        val debtId = debtIds.getValue(debtKey(row))
        val payment =
            repository
                .getPaymentsByDebtOrderedByInstallment(
                    debtId,
                ).first { it.installment == row.int(Cols.INSTALLMENT) }
        payment.amount = row.decimal(Cols.AMOUNT)!!.toRounded()
        payment.paidAmount = (row.decimal(Cols.PAID_AMOUNT) ?: BigDecimal.ZERO).max(BigDecimal.ZERO).toRounded()
        payment.rebateUsed = (row.decimal(Cols.REBATE_USED) ?: BigDecimal.ZERO).max(BigDecimal.ZERO).toRounded()
        payment.wallet = row.text(Cols.WALLET)?.let { walletRepository.findByName(it).get() }
        payment.refunded = row.bool(Cols.REFUNDED) ?: false
        payment.date = row.dateTime(Cols.DATE)!!
    }
}

@Component
class CreditCardCreditSpec(
    private val repository: CreditCardCreditRepository,
    private val cardRepository: CreditCardRepository,
) : EntitySpec {
    override val id = CreditCardEntities.CREDITS
    override val group = CreditCardEntities.GROUP
    override val dependsOn = listOf(CreditCardEntities.CARDS)
    override val columns =
        listOf(
            Column(Cols.CREDIT_CARD, CellType.TEXT, required = true),
            Column(Cols.TYPE, CellType.ENUM, required = true, enumClass = CreditCardCreditType::class.java),
            Column(Cols.DATE, CellType.DATETIME, required = true),
            Column(Cols.AMOUNT, CellType.DECIMAL, required = true),
            Column(Cols.DESCRIPTION, CellType.TEXT),
        )

    override fun count() = repository.count()

    override fun export() =
        repository
            .findAll()
            .sortedWith(compareBy({ it.date }, { it.id }))
            .asSequence()
            .map {
                mapOf(
                    Cols.CREDIT_CARD to it.creditCard.name,
                    Cols.TYPE to it.type,
                    Cols.DATE to it.date,
                    Cols.AMOUNT to it.amount,
                    Cols.DESCRIPTION to it.description,
                )
            }

    override fun naturalKey(row: ParsedRow) =
        NaturalKeys.of(
            row.text(Cols.CREDIT_CARD),
            row.dateTime(Cols.DATE),
            row.values[Cols.TYPE],
            row.decimal(Cols.AMOUNT),
            row.text(Cols.DESCRIPTION),
        )

    override fun existingKeys(): Map<String, Int> =
        repository
            .findAll()
            .groupingBy { NaturalKeys.of(it.creditCard.name, it.date, it.type, it.amount, it.description) }
            .eachCount()

    override fun validate(
        row: ParsedRow,
        ctx: ResolveContext,
    ): List<Problem> =
        WalletEntities.checkReference(id, row, Cols.CREDIT_CARD, CreditCardEntities.CARDS, ctx) +
            WalletEntities.checkPositive(id, row, Cols.AMOUNT)

    override fun apply(
        row: ParsedRow,
        ctx: ApplyContext,
    ) {
        repository.save(
            CreditCardCredit(
                type = row.enum<CreditCardCreditType>(Cols.TYPE)!!,
                creditCard = cardRepository.findAll().first { it.name == row.text(Cols.CREDIT_CARD) },
                date = row.dateTime(Cols.DATE)!!,
                amount = row.decimal(Cols.AMOUNT)!!,
                description = row.text(Cols.DESCRIPTION),
            ),
        )
    }
}
