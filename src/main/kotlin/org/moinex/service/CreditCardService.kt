/*
 * Filename: CreditCardService.kt (original filename: CreditCardService.java)
 * Created on: September  4, 2024
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 *
 * Migrate to Kotlin on 28/02/2026
 */

package org.moinex.service

import org.moinex.common.ClockProvider
import org.moinex.common.findByIdOrThrow
import org.moinex.model.creditcard.CreditCard
import org.moinex.model.creditcard.CreditCardCredit
import org.moinex.model.creditcard.CreditCardDebt
import org.moinex.model.creditcard.CreditCardPayment
import org.moinex.model.enums.CreditCardCreditType
import org.moinex.model.enums.CreditCardInvoiceStatus
import org.moinex.repository.creditcard.CreditCardCreditRepository
import org.moinex.repository.creditcard.CreditCardDebtRepository
import org.moinex.repository.creditcard.CreditCardOperatorRepository
import org.moinex.repository.creditcard.CreditCardPaymentRepository
import org.moinex.repository.creditcard.CreditCardRepository
import org.moinex.repository.wallettransaction.WalletRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDateTime
import java.time.Year
import java.time.YearMonth

@Service
class CreditCardService(
    private val creditCardDebtRepository: CreditCardDebtRepository,
    private val creditCardPaymentRepository: CreditCardPaymentRepository,
    private val creditCardRepository: CreditCardRepository,
    private val creditCardOperatorRepository: CreditCardOperatorRepository,
    private val walletRepository: WalletRepository,
    private val creditCardCreditRepository: CreditCardCreditRepository,
    private val clockProvider: ClockProvider = ClockProvider(),
) {
    companion object {
        private val logger = LoggerFactory.getLogger(CreditCardService::class.java)

        private data class InstallmentCalculation(
            val installmentValue: BigDecimal,
            val remainder: BigDecimal,
        )
    }

    /**
     * Creates a new credit card
     *
     * @param creditCard The credit card to be created
     * @return The id of the created credit card
     */
    @Transactional
    fun createCreditCard(creditCard: CreditCard): Int {
        check(!creditCardRepository.existsByName(creditCard.name)) {
            "Credit card with name '${creditCard.name}' already exists"
        }

        check(creditCardOperatorRepository.existsById(creditCard.operator.id!!)) {
            "Credit card operator ${creditCard.operator} does not exist"
        }

        creditCard.defaultBillingWallet?.let { wallet ->
            check(walletRepository.existsById(wallet.id)) {
                "Wallet $wallet does not exist"
            }
        }

        val newCreditCard =
            creditCardRepository.save(
                CreditCard(
                    name = creditCard.name,
                    billingDueDay = creditCard.billingDueDay,
                    closingDay = creditCard.closingDay,
                    maxDebt = creditCard.maxDebt,
                    lastFourDigits = creditCard.lastFourDigits,
                    operator = creditCard.operator,
                    defaultBillingWallet = creditCard.defaultBillingWallet,
                ),
            )

        logger.info("Credit card {} has created successfully", creditCard)

        return newCreditCard.id!!
    }

    /**
     * Updates an existing credit card
     *
     * @param updatedCreditCard The credit card with updated information
     */
    @Transactional
    fun updateCreditCard(updatedCreditCard: CreditCard) {
        val creditCardFromDatabase = creditCardRepository.findByIdOrThrow(updatedCreditCard.id!!)

        check(creditCardOperatorRepository.existsById(updatedCreditCard.operator.id!!)) {
            "${updatedCreditCard.operator} does not exist"
        }

        check(!creditCardRepository.existsByNameAndIdNot(updatedCreditCard.name, updatedCreditCard.id!!)) {
            "Credit card with name '${updatedCreditCard.name}' already exists"
        }

        creditCardFromDatabase.apply {
            name = updatedCreditCard.name
            maxDebt = updatedCreditCard.maxDebt
            lastFourDigits = updatedCreditCard.lastFourDigits
            operator = updatedCreditCard.operator
            defaultBillingWallet = updatedCreditCard.defaultBillingWallet
            billingDueDay = updatedCreditCard.billingDueDay
            closingDay = updatedCreditCard.closingDay
        }

        val futurePendingPayments =
            creditCardPaymentRepository
                .getAllPendingCreditCardPayments(creditCardFromDatabase.id!!)
                .filter { it.date.isAfter(clockProvider.now()) }

        futurePendingPayments.forEach { payment ->
            payment.date = payment.date.withDayOfMonth(updatedCreditCard.billingDueDay)
        }

        logger.info("{} updated successfully", updatedCreditCard)
    }

    /**
     * Delete a credit card
     *
     * @param creditCardId The id of the credit card
     */
    @Transactional
    fun deleteCreditCard(creditCardId: Int) {
        val creditCardFromDatabase = creditCardRepository.findByIdOrThrow(creditCardId)

        check(creditCardDebtRepository.getDebtCountByCreditCard(creditCardId) == 0) {
            "Credit card with id $creditCardId has debts and cannot be deleted"
        }

        creditCardRepository.delete(creditCardFromDatabase)

        logger.info("Credit card with id {} was permanently deleted", creditCardId)
    }

    /**
     * Adds a new debt to a credit card with installments
     *
     * @param debt The credit card debt
     * @param invoiceMonth The month when the debt will be invoiced
     */
    @Transactional
    fun createDebt(
        debt: CreditCardDebt,
        invoiceMonth: YearMonth,
    ) {
        check(debt.amount <= getAvailableCredit(debt.creditCard.id!!)) {
            "${debt.creditCard} does not have enough credit"
        }

        creditCardDebtRepository.save(debt)

        val (installmentValue, remainder) = calculateInstallmentValues(debt.amount, debt.installments)

        val payments =
            (0 until debt.installments).map { installment ->
                val currentInstallmentValue =
                    when {
                        installment == 0 && remainder > BigDecimal.ZERO -> installmentValue + remainder
                        else -> installmentValue
                    }

                val paymentDate =
                    invoiceMonth
                        .plusMonths(installment.toLong())
                        .atDay(debt.creditCard.billingDueDay)
                        .atTime(23, 59)

                CreditCardPayment(
                    creditCardDebt = debt,
                    amount = currentInstallmentValue,
                    installment = installment + 1,
                    date = paymentDate,
                )
            }

        creditCardPaymentRepository.saveAll(payments)

        logger.info(
            "Debit registered on credit card with id {} with value {} and description '{}'",
            debt.creditCard.id,
            debt.amount,
            debt.description,
        )
    }

    @Transactional
    fun updateDebt(
        updatedDebt: CreditCardDebt,
        invoiceMonth: YearMonth,
    ) {
        val debtFromDatabase = creditCardDebtRepository.findByIdOrThrow(updatedDebt.id!!)

        val payments = creditCardPaymentRepository.getPaymentsByDebtOrderedByInstallment(updatedDebt.id!!).toMutableList()

        check(payments.none { it.isRefunded() }) {
            "$debtFromDatabase has already been refunded and cannot be updated"
        }

        check(creditCardRepository.existsById(updatedDebt.creditCard.id!!)) {
            "${updatedDebt.creditCard} does not exist"
        }

        updateDebtInvoice(debtFromDatabase, invoiceMonth, payments)
        updateDebtAmount(debtFromDatabase, updatedDebt.amount, payments)
        updateDebtInstallment(debtFromDatabase, updatedDebt.installments, payments)

        debtFromDatabase.apply {
            creditCard = updatedDebt.creditCard
            category = updatedDebt.category
            description = updatedDebt.description
        }

        logger.info("$debtFromDatabase updated successfully")
    }

    /**
     * Delete a debt from a credit card
     *
     * @param debtId The id of the debt
     */
    @Transactional
    fun deleteDebt(debtId: Int) {
        val debtFromDatabase = creditCardDebtRepository.findByIdOrThrow(debtId)

        creditCardPaymentRepository.getPaymentsByDebtOrderedByInstallment(debtId).forEach {
            deletePayment(it.id!!)
        }

        creditCardDebtRepository.delete(debtFromDatabase)

        logger.info("$debtFromDatabase was permanently deleted")
    }

    @Transactional
    fun refundDebt(
        debtId: Int,
        description: String? = null,
    ) {
        val debtFromDatabase = creditCardDebtRepository.findByIdOrThrow(debtId)

        val payments = creditCardPaymentRepository.getPaymentsByDebtOrderedByInstallment(debtId)

        check(payments.none { it.isRefunded() }) {
            "$debtFromDatabase has already been refunded"
        }

        val nextInvoiceDate = getNextInvoiceDate(debtFromDatabase.creditCard)

        val totalToRefund =
            payments.fold(BigDecimal.ZERO) { acc, payment ->
                if (!payment.isPaid()) payment.date = nextInvoiceDate

                payment.refunded = true

                if (payment.isPaid()) acc.plus(payment.amount) else acc
            }

        if (totalToRefund > BigDecimal.ZERO) {
            addRebate(
                CreditCardCredit(
                    creditCard = debtFromDatabase.creditCard,
                    date = nextInvoiceDate,
                    amount = totalToRefund,
                    type = CreditCardCreditType.REFUND,
                    description = description,
                ),
            )
        } else {
            logger.info("No payments to refund for $debtFromDatabase")
        }
    }

    @Transactional
    fun payInvoice(
        creditCardId: Int,
        billingWalletId: Int,
        invoiceDate: YearMonth,
        rebate: BigDecimal = BigDecimal.ZERO,
    ) {
        val creditCardFromDatabase = creditCardRepository.findByIdOrThrow(creditCardId)

        val walletFromDatabase = walletRepository.findByIdOrThrow(billingWalletId)

        check(rebate >= BigDecimal.ZERO) {
            "Rebate cannot be negative"
        }

        check(creditCardFromDatabase.availableRebate >= rebate) {
            "Insufficient available rebate"
        }

        val pendingPayments =
            creditCardPaymentRepository
                .getPendingCreditCardPayments(
                    creditCardId,
                    invoiceDate.monthValue,
                    invoiceDate.year,
                ).toList()

        val totalPendingPayments = pendingPayments.sumOf { it.amount }

        var totalToPay = totalPendingPayments.minus(rebate)

        var effectiveRebate = rebate

        if (totalToPay < BigDecimal.ZERO) {
            totalToPay = BigDecimal.ZERO
            effectiveRebate = totalPendingPayments
        }

        var totalRebateUsed = BigDecimal.ZERO
        var remainingRebate = effectiveRebate

        pendingPayments.forEach { payment ->
            val rebateForThisPayment =
                if (payment.id == pendingPayments.last().id) {
                    remainingRebate
                } else {
                    payment.amount.divide(totalPendingPayments, 2, RoundingMode.HALF_UP).multiply(effectiveRebate)
                }

            payment.rebateUsed = rebateForThisPayment
            payment.wallet = walletFromDatabase

            totalRebateUsed += rebateForThisPayment
            remainingRebate -= rebateForThisPayment
        }

        walletFromDatabase.balance -= totalToPay

        creditCardFromDatabase.availableRebate -= totalRebateUsed

        logger.info("Invoice {} from credit card {} paid successfully", invoiceDate, creditCardFromDatabase)
    }

    @Transactional
    fun archiveCreditCard(creditCardId: Int) {
        val creditCardFromDatabase = creditCardRepository.findByIdOrThrow(creditCardId)

        check(creditCardPaymentRepository.getTotalPendingPaymentsByCreditCard(creditCardId) == BigDecimal.ZERO) {
            "$creditCardFromDatabase has pending payments and cannot be archived"
        }

        creditCardFromDatabase.isArchived = true

        logger.info("$creditCardFromDatabase archived successfully")
    }

    @Transactional
    fun unarchiveCreditCard(creditCardId: Int) {
        val creditCardFromDatabase = creditCardRepository.findByIdOrThrow(creditCardId)

        creditCardFromDatabase.isArchived = false

        logger.info("$creditCardFromDatabase unarchived successfully")
    }

    @Transactional
    fun addRebate(creditCardCredit: CreditCardCredit) {
        val creditCardFromDatabase = creditCardRepository.findByIdOrThrow(creditCardCredit.creditCard.id!!)

        creditCardFromDatabase.availableRebate += creditCardCredit.amount

        creditCardCreditRepository.save(creditCardCredit)

        logger.info("$creditCardCredit added successfully")
    }

    fun getAvailableCredit(creditCardId: Int): BigDecimal {
        val creditCardFromDatabase = creditCardRepository.findByIdOrThrow(creditCardId)

        return creditCardFromDatabase.maxDebt.subtract(
            creditCardPaymentRepository.getTotalPendingPaymentsByCreditCard(creditCardId),
        )
    }

    fun getNextInvoiceDate(creditCard: CreditCard): LocalDateTime {
        val nextInvoiceDate = creditCardPaymentRepository.getNextInvoiceDate(creditCard.id!!)

        return nextInvoiceDate ?: calculateNextInvoiceDate(creditCard)
    }

    fun getDebtAtDate(date: LocalDateTime): BigDecimal {
        val now = clockProvider.now()

        val totalDebts =
            creditCardDebtRepository
                .findAllCreatedUpToDate(date)
                .sumOf { it.amount }

        val totalPaymentsUntilDate =
            creditCardPaymentRepository
                .findAllPaidUpToDate(date)
                .sumOf { it.amount }

        val projectedPayments =
            if (date.isAfter(now)) {
                val targetMonth = YearMonth.from(date)
                generateSequence(YearMonth.from(now)) { it.plusMonths(1) }
                    .takeWhile { !it.isAfter(targetMonth) }
                    .sumOf { month ->
                        creditCardPaymentRepository
                            .getPendingPaymentsByMonth(month.monthValue, month.year)
                    }
            } else {
                BigDecimal.ZERO
            }

        return totalDebts.minus(totalPaymentsUntilDate.plus(projectedPayments))
    }

    fun getAllNonArchivedCreditCardsOrderedByName() = creditCardRepository.findAllByIsArchivedFalseOrderByNameAsc()

    fun getAllNonArchivedCreditCardsOrderedByDebtCountDesc() =
        creditCardRepository
            .findAllByIsArchivedFalse()
            .sortedByDescending { creditCardDebtRepository.getDebtCountByCreditCard(it.id!!) }

    fun getAllArchivedCreditCards() = creditCardRepository.findAllByIsArchivedTrue()

    fun getAllCreditCardCredits() = creditCardCreditRepository.findAll()

    fun getAllPaidPaymentsByMonth(yearMonth: YearMonth) =
        creditCardPaymentRepository.getAllPaidPaymentsByMonth(yearMonth.monthValue, yearMonth.year)

    fun getAllCreditCardOperatorsOrderedByName() = creditCardOperatorRepository.findAllByOrderByNameAsc()

    fun getCreditCardCreditSuggestions() = creditCardCreditRepository.findSuggestions()

    fun getCreditCardDebtSuggestions() = creditCardDebtRepository.findSuggestions()

    fun getPaymentsByCreditCardAndMonth(
        creditCardId: Int,
        yearMonth: YearMonth,
    ) = creditCardPaymentRepository.getCreditCardPayments(creditCardId, yearMonth.monthValue, yearMonth.year)

    fun getPaymentsByMonth(yearMonth: YearMonth) = creditCardPaymentRepository.getCreditCardPayments(yearMonth.monthValue, yearMonth.year)

    fun getPaymentsByDebtOrderedByInstallment(debtId: Int) = creditCardPaymentRepository.getPaymentsByDebtOrderedByInstallment(debtId)

    fun getTotalEffectivePaidPaymentsByMonth(yearMonth: YearMonth) =
        creditCardPaymentRepository.getEffectivePaidPaymentsByMonth(yearMonth.monthValue, yearMonth.year)

    fun getTotalEffectivePaidPaymentsByWalletAndMonth(
        walletId: Int,
        yearMonth: YearMonth,
    ) = creditCardPaymentRepository.getEffectivePaidPaymentsByMonth(walletId, yearMonth.monthValue, yearMonth.year)

    fun getDebtCountByCreditCard(creditCardId: Int) = creditCardDebtRepository.getDebtCountByCreditCard(creditCardId)

    fun getTotalPendingPaymentsByCreditCard(creditCardId: Int) =
        creditCardPaymentRepository.getTotalPendingPaymentsByCreditCard(creditCardId)

    fun getTotalPendingPayments() = creditCardPaymentRepository.getTotalPendingPayments()

    fun getTotalDebtAmountByYear(year: Year) = creditCardPaymentRepository.getTotalDebtAmountByYear(year.value)

    fun getTotalDebtAmountByMonth(yearMonth: YearMonth) =
        creditCardPaymentRepository.getTotalDebtAmountByMonth(yearMonth.monthValue, yearMonth.year)

    fun getTotalPaidPaymentsByYear(year: Year) = creditCardPaymentRepository.getPaidPaymentsByYear(year.value)

    fun getTotalPendingPaymentsByYear(year: Year) = creditCardPaymentRepository.getPendingPaymentsByYear(year.value)

    fun getTotalPendingPaymentsByMonth(yearMonth: YearMonth) =
        creditCardPaymentRepository.getPendingPaymentsByMonth(yearMonth.monthValue, yearMonth.year)

    fun getTotalPendingPaymentsByCreditCardAndMonth(
        creditCardId: Int,
        yearMonth: YearMonth,
    ) = creditCardPaymentRepository.getPendingPaymentsByCreditCardAndMonth(creditCardId, yearMonth.monthValue, yearMonth.year)

    fun getTotalPaymentsByCategoriesAndDateTimeBetween(
        categoryIds: List<Int>,
        startDateTime: LocalDateTime,
        endDateTime: LocalDateTime,
    ) = creditCardPaymentRepository.getTotalPaymentsByCategoriesAndDateTimeBetween(categoryIds, startDateTime, endDateTime)

    fun getInvoiceAmount(
        creditCardId: Int,
        yearMonth: YearMonth,
    ) = creditCardPaymentRepository.getInvoiceAmount(creditCardId, yearMonth.monthValue, yearMonth.year)

    fun getInvoiceStatus(
        creditCardId: Int,
        yearMonth: YearMonth,
    ): CreditCardInvoiceStatus {
        val creditCardFromDatabase = creditCardRepository.findByIdOrThrow(creditCardId)

        val nextInvoiceDate = getNextInvoiceDate(creditCardFromDatabase).withHour(0).withMinute(0).withSecond(1)

        val invoiceClosingDate = LocalDateTime.of(yearMonth.year, yearMonth.monthValue, nextInvoiceDate.dayOfMonth, 23, 59)

        return if (invoiceClosingDate.isAfter(nextInvoiceDate) || invoiceClosingDate.isEqual(nextInvoiceDate)) {
            CreditCardInvoiceStatus.OPEN
        } else {
            CreditCardInvoiceStatus.CLOSED
        }
    }

    fun getEarliestPaymentDate(): LocalDateTime = creditCardPaymentRepository.findEarliestPaymentDate() ?: clockProvider.now()

    fun getLatestPaymentDate(): LocalDateTime = creditCardPaymentRepository.findLatestPaymentDate() ?: clockProvider.now()

    private fun calculateNextInvoiceDate(creditCard: CreditCard): LocalDateTime {
        val now = clockProvider.now()

        return if (now.dayOfMonth > creditCard.closingDay) {
            now.plusMonths(1).withDayOfMonth(creditCard.billingDueDay)
        } else {
            now.withDayOfMonth(creditCard.billingDueDay)
        }
    }

    private fun deletePayment(paymentId: Int) {
        val payment = creditCardPaymentRepository.findByIdOrThrow(paymentId)

        refundPayment(payment)

        creditCardPaymentRepository.delete(payment)

        logger.info("$payment was permanently deleted")
    }

    private fun refundPayment(payment: CreditCardPayment) {
        payment.wallet?.let { wallet ->
            wallet.balance += payment.amount
            logger.info("${payment.wallet} balance updated to ${wallet.balance} for payment $payment")
        } ?: run {
            logger.info("$payment is not paid")
        }
    }

    private fun calculateInstallmentValues(
        amount: BigDecimal,
        installments: Int,
    ): InstallmentCalculation {
        val installmentValue = amount.divide(installments.toBigDecimal(), 2, RoundingMode.HALF_UP)
        val remainder = amount.minus(installmentValue.multiply(installments.toBigDecimal()))
        return InstallmentCalculation(installmentValue, remainder)
    }

    private fun updateDebtInvoice(
        debt: CreditCardDebt,
        invoiceMonth: YearMonth,
        payments: List<CreditCardPayment>,
    ) {
        val firstPayment = payments.minBy { it.installment }

        if (YearMonth.from(firstPayment.date) == invoiceMonth) {
            return
        }

        payments.forEachIndexed { installment, payment ->
            val paymentDate =
                invoiceMonth
                    .plusMonths(installment.toLong())
                    .atDay(debt.creditCard.billingDueDay)
                    .atTime(23, 59)

            payment.date = paymentDate
        }

        logger.info("Invoice month of debt {} on credit card {} updated to {}", debt.id, debt.creditCard.id, invoiceMonth)
    }

    private fun updateDebtAmount(
        debt: CreditCardDebt,
        newAmount: BigDecimal,
        payments: List<CreditCardPayment>,
    ) {
        if (debt.amount == newAmount) return

        val (installmentValue, remainder) = calculateInstallmentValues(newAmount, debt.installments)

        payments.forEachIndexed { installment, payment ->
            val currentInstallmentValue =
                when {
                    installment == 0 && remainder > BigDecimal.ZERO -> installmentValue.plus(remainder)
                    else -> installmentValue
                }

            payment.wallet?.let { wallet ->
                val diff = currentInstallmentValue.minus(payment.amount)
                wallet.balance += diff

                logger.info(
                    "Payment number {} of debt {} on credit card {} updated and added to wallet {}",
                    payment.installment,
                    debt.id,
                    debt.creditCard.id,
                    wallet.id,
                )
            }

            payment.amount = currentInstallmentValue

            logger.info(
                "Payment number {} of debt {} on credit card {} updated with value {}",
                payment.installment,
                debt.id,
                debt.creditCard.id,
                currentInstallmentValue,
            )
        }

        debt.amount = newAmount
    }

    private fun updateDebtInstallment(
        debt: CreditCardDebt,
        newInstallments: Int,
        payments: MutableList<CreditCardPayment>,
    ) {
        if (debt.installments == newInstallments) return

        val installmentCalculation = calculateInstallmentValues(debt.amount, newInstallments)

        if (newInstallments < debt.installments) {
            handleReducedInstallments(payments, newInstallments, installmentCalculation)
        } else {
            handleIncreasedInstallments(debt, payments, newInstallments, installmentCalculation)
        }

        debt.installments = newInstallments
    }

    private fun handleReducedInstallments(
        payments: MutableList<CreditCardPayment>,
        newInstallments: Int,
        calculation: InstallmentCalculation,
    ) {
        payments.forEach { payment ->
            if (payment.installment > newInstallments) {
                deletePayment(payment.id!!)
            } else {
                payment.amount = calculatePaymentAmount(payment.installment, calculation)
            }
        }
    }

    private fun handleIncreasedInstallments(
        debt: CreditCardDebt,
        payments: MutableList<CreditCardPayment>,
        newInstallments: Int,
        calculation: InstallmentCalculation,
    ) {
        updateExistingPayments(payments, calculation)
        createNewPayments(debt, payments, newInstallments, calculation)
    }

    private fun updateExistingPayments(
        payments: MutableList<CreditCardPayment>,
        calculation: InstallmentCalculation,
    ) {
        payments.forEach { payment ->
            payment.amount = calculatePaymentAmount(payment.installment, calculation)
        }
    }

    private fun createNewPayments(
        debt: CreditCardDebt,
        payments: MutableList<CreditCardPayment>,
        newInstallments: Int,
        calculation: InstallmentCalculation,
    ) {
        var lastPaymentDate = payments.maxBy { it.installment }.date

        val newPayments =
            (debt.installments + 1..newInstallments).map { installment ->
                lastPaymentDate = lastPaymentDate.plusMonths(1)

                CreditCardPayment(
                    creditCardDebt = debt,
                    date = lastPaymentDate,
                    amount = calculation.installmentValue,
                    installment = installment,
                )
            }

        if (newPayments.isNotEmpty()) {
            creditCardPaymentRepository.saveAll(newPayments)
            payments.addAll(newPayments)
        }
    }

    private fun calculatePaymentAmount(
        installment: Int,
        calculation: InstallmentCalculation,
    ): BigDecimal =
        if (installment == 1) {
            calculation.installmentValue.plus(calculation.remainder)
        } else {
            calculation.installmentValue
        }
}
