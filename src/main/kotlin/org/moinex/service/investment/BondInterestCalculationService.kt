/*
 * Filename: BondInterestCalculationService.kt (original filename: BondInterestCalculationService.java)
 * Created on: February 20, 2026
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 *
 * Migrate to Kotlin on 12/03/2026
 */

package org.moinex.service.investment

import org.moinex.common.ClockProvider
import org.moinex.common.constant.TranslationKeys
import org.moinex.common.extension.calculateInvestedAmountUntil
import org.moinex.common.extension.calculateQuantityUntil
import org.moinex.common.extension.findByIdOrThrow
import org.moinex.common.extension.isBeforeOrEqual
import org.moinex.common.extension.realizedInterest
import org.moinex.common.extension.toRounded
import org.moinex.model.dto.BondInterestCalculationContextDTO
import org.moinex.model.dto.BondPeriodInterestResultDTO
import org.moinex.model.dto.InterestRecalculationRowDTO
import org.moinex.model.enums.InterestIndex
import org.moinex.model.enums.InterestType
import org.moinex.model.enums.NotificationType
import org.moinex.model.investment.Bond
import org.moinex.model.investment.BondInterestCalculation
import org.moinex.model.investment.BondOperation
import org.moinex.repository.investment.BondInterestCalculationRepository
import org.moinex.repository.investment.BondOperationRepository
import org.moinex.repository.investment.BondRepository
import org.moinex.service.NotificationService
import org.moinex.service.PreferencesService
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.transaction.interceptor.TransactionAspectSupport
import org.springframework.transaction.support.TransactionSynchronizationManager
import java.math.BigDecimal
import java.math.MathContext
import java.time.LocalDate
import java.time.YearMonth

@Service
class BondInterestCalculationService(
    private val bondRepository: BondRepository,
    private val bondOperationRepository: BondOperationRepository,
    private val bondInterestCalculationRepository: BondInterestCalculationRepository,
    private val marketIndicatorService: MarketIndicatorService,
    private val notificationService: NotificationService,
    private val preferencesService: PreferencesService,
    private val clockProvider: ClockProvider = ClockProvider(),
) {
    private val logger = LoggerFactory.getLogger(BondInterestCalculationService::class.java)

    companion object {
        private val HUNDRED = BigDecimal("100")
        private const val FIXED_FORMAT = "FIXED: %s%% a.a."
        private const val FLOATING_FORMAT = "FLOATING: %s%% of %s"
        private const val FLOATING_SPREAD_FORMAT = "FLOATING: %s + %s%% a.a."
        private const val ZERO_COUPON_METHOD = "ZERO_COUPON"
    }

    @Transactional
    fun calculateInterestForAllBondsIfNeeded() {
        logger.info("Starting monthly interest synchronization for all bonds")

        val operationsByBond =
            bondOperationRepository
                .findAllByNonArchivedBondsOrderByBondAndDate()
                .groupBy { it.bond }

        val syncedCount =
            operationsByBond
                .filter { (_, operations) -> operations.isNotEmpty() }
                .count { (bond, _) ->
                    runCatching {
                        calculateAndStoreMonthlyInterest(bond)
                        logger.debug("Synchronized monthly interest for {}", bond)
                    }.onFailure {
                        logger.warn(
                            "Failed to synchronize monthly interest for {}: {}",
                            bond,
                            it.message,
                        )
                    }.isSuccess
                }

        logger.info("Monthly interest synchronization completed: {} bonds synced", syncedCount)
    }

    @Transactional
    fun calculateAndStoreMonthlyInterest(
        bond: Bond,
        fromStart: Boolean = false,
    ) {
        requireBondConfiguration(bond)

        val operations = bondOperationRepository.findByBondOrderByOperationDateAsc(bond)

        val firstBuy =
            operations.firstOrNull { it.isPurchase() }
                ?: run {
                    logger.warn("$bond has no buy operations, skipping monthly interest calculation")
                    return
                }

        val startMonth = YearMonth.from(firstBuy.walletTransaction!!.date.toLocalDate())
        val currentMonth = YearMonth.from(clockProvider.now())

        val context =
            resolveCalculationContext(bond, startMonth, currentMonth, fromStart) ?: run {
                logger.debug("No recalculation needed for {}", bond)
                return
            }

        generateMonthSequence(context.startMonth, currentMonth)
            .forEach { month ->
                processMonthCalculation(bond, month, context, operations)
            }

        logger.info(
            "Monthly interest calculation completed for {} with total accumulated: {}",
            bond,
            context.accumulatedInterest.toRounded(),
        )
    }

    @Transactional
    fun recalculateAllMonthlyInterest(bondId: Int) {
        val bond = bondRepository.findByIdOrThrow(bondId)

        val operations = bondOperationRepository.findByBondOrderByOperationDateAsc(bond)

        if (operations.isEmpty()) {
            logger.warn("$bond has no operations, cannot recalculate interest history")
            return
        }

        calculateAndStoreMonthlyInterest(bond, fromStart = true)
        logger.info("Completed full recalculation of interest history for $bond")
    }

    /**
     * What recalculating the history of [bondId] would change, without changing anything: the recalculation runs and
     * its transaction is rolled back. Months adjusted by hand keep their interest.
     */
    @Transactional
    fun previewRecalculation(bondId: Int): List<InterestRecalculationRowDTO> {
        val bond = bondRepository.findByIdOrThrow(bondId)

        val before =
            bondInterestCalculationRepository
                .findByBondOrderByReferenceMonthAsc(bond)
                .associate { it.referenceMonth to (it.monthlyInterest to it.accumulatedInterest) }

        calculateAndStoreMonthlyInterest(bond, fromStart = true)

        val rows =
            bondInterestCalculationRepository
                .findByBondOrderByReferenceMonthAsc(bond)
                .map { after ->
                    InterestRecalculationRowDTO(
                        month = after.referenceMonth,
                        manuallyAdjusted = after.manuallyAdjusted,
                        monthlyBefore = before[after.referenceMonth]?.first,
                        monthlyAfter = after.monthlyInterest,
                        accumulatedBefore = before[after.referenceMonth]?.second,
                        accumulatedAfter = after.accumulatedInterest,
                    )
                }

        if (TransactionSynchronizationManager.isActualTransactionActive()) {
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly()
        }

        return rows
    }

    @Transactional
    fun adjustMonthlyInterest(
        bondId: Int,
        month: YearMonth,
        newMonthlyInterest: BigDecimal,
    ) {
        val bondFromDatabase = bondRepository.findByIdOrThrow(bondId)
        requireBondConfiguration(bondFromDatabase)

        val calculation =
            requireNotNull(
                bondInterestCalculationRepository.findByBondAndReferenceMonth(bondFromDatabase, month),
            ) {
                "No interest calculation found for $bondFromDatabase in month $month"
            }

        val calculatedUntil = resolveCalculatedUntilDate(bondFromDatabase, month)

        val delta =
            updateMonthCalculation(
                calculation = calculation,
                newMonthlyInterest = newMonthlyInterest,
                calculatedUntil = calculatedUntil,
                manuallyAdjusted = true,
            )

        logger.info(
            "Adjusted monthly interest for {} month {}: delta={}",
            bondFromDatabase,
            month,
            delta,
        )

        propagateDeltaToFutureMonths(bondFromDatabase, month, delta)

        logger.info(
            "Completed adjustment propagation for {} starting from month {}",
            bondFromDatabase,
            month,
        )

        notificationService.send(
            type = NotificationType.SUCCESS,
            title =
                preferencesService.translate(TranslationKeys.BOND_INTEREST_HISTORY_DIALOG_INTEREST_ADJUSTED_TITLE),
            message =
                preferencesService.translate(TranslationKeys.BOND_INTEREST_HISTORY_DIALOG_INTEREST_ADJUSTED_MESSAGE),
            relatedEntityId = bondFromDatabase.id!!,
        )
    }

    @Transactional
    fun resetToAutomaticCalculation(
        bondId: Int,
        month: YearMonth,
    ) {
        val bondFromDatabase = bondRepository.findByIdOrThrow(bondId)

        val calculation =
            requireNotNull(
                bondInterestCalculationRepository.findByBondAndReferenceMonth(bondFromDatabase, month),
            ) {
                "No interest calculation found for $bondFromDatabase in month $month"
            }

        if (!calculation.manuallyAdjusted) {
            logger.warn(
                "Month {} for {} was not manually adjusted, skipping reset",
                month,
                bondFromDatabase,
            )
            return
        }

        val operations = bondOperationRepository.findByBondOrderByOperationDateAsc(bondFromDatabase)
        val recalculatedInterest = calculateMonthlyInterest(bondFromDatabase, month, operations)
        val calculatedUntil = resolveCalculatedUntilDate(bondFromDatabase, month)

        val delta =
            updateMonthCalculation(
                calculation = calculation,
                newMonthlyInterest = recalculatedInterest,
                calculatedUntil = calculatedUntil,
                manuallyAdjusted = false,
            )

        logger.info(
            "Reset monthly interest for {} month {} to automatic: delta={}",
            bondFromDatabase,
            month,
            delta,
        )

        propagateDeltaToFutureMonths(bondFromDatabase, month, delta)

        notificationService.send(
            type = NotificationType.SUCCESS,
            title =
                preferencesService.translate(TranslationKeys.BOND_INTEREST_HISTORY_DIALOG_INTEREST_RESET_TITLE),
            message =
                preferencesService.translate(TranslationKeys.BOND_INTEREST_HISTORY_DIALOG_INTEREST_RESET_MESSAGE),
            relatedEntityId = bondFromDatabase.id!!,
        )
    }

    @Transactional(readOnly = true)
    fun getCurrentMonthInterest(bondId: Int): BigDecimal? {
        val bond = bondRepository.findByIdOrThrow(bondId)

        return bondInterestCalculationRepository
            .findByBondAndReferenceMonth(bond, YearMonth.from(clockProvider.now()))
            ?.monthlyInterest
    }

    @Transactional(readOnly = true)
    fun getLatestCalculation(bondId: Int): BondInterestCalculation? {
        val bond = bondRepository.findByIdOrThrow(bondId)
        return bondInterestCalculationRepository.findLastCalculatedMonth(bond)
    }

    @Transactional(readOnly = true)
    fun getMonthlyInterestHistory(bond: Bond): List<BondInterestCalculation> =
        bondInterestCalculationRepository.findByBondOrderByReferenceMonthAsc(bond)

    private fun getCalculationMethod(bond: Bond): String =
        when (bond.interestType) {
            InterestType.FIXED -> String.format(FIXED_FORMAT, bond.interestRate)
            InterestType.FLOATING -> String.format(FLOATING_FORMAT, bond.interestRate, bond.interestIndex)
            InterestType.FLOATING_SPREAD ->
                String.format(FLOATING_SPREAD_FORMAT, bond.interestIndex, bond.interestRate)
            InterestType.ZERO_COUPON -> ZERO_COUPON_METHOD
            else -> ""
        }

    private fun getLastDateWithData(
        bond: Bond,
        startDate: LocalDate,
        endDate: LocalDate,
    ): LocalDate {
        val data =
            marketIndicatorService.getIndicatorHistoryBetween(
                bond.interestIndex!!,
                startDate,
                endDate,
            )

        if (data.isEmpty()) {
            logger.info(
                "No market data available from {} to {}",
                startDate,
                endDate,
            )
        }

        return data.maxOfOrNull { it.referenceDate } ?: startDate
    }

    private fun indexFor(bond: Bond): InterestIndex = bond.interestIndex ?: InterestIndex.CDI

    private fun indexPoints(
        bond: Bond,
        start: LocalDate,
        end: LocalDate,
    ): List<BondAccrualCalculator.IndexPoint> =
        marketIndicatorService
            .getIndicatorHistoryBetween(indexFor(bond), start, end)
            .filter { it.referenceDate in start..end }
            .map { BondAccrualCalculator.IndexPoint(it.referenceDate, it.rateValue) }

    private fun flowsBetween(
        operations: List<BondOperation>,
        start: LocalDate,
        end: LocalDate,
    ): List<BondAccrualCalculator.Flow> =
        operations
            .filter { it.localDate in start..end }
            .map {
                val amount = it.quantity.multiply(it.unitPrice)
                BondAccrualCalculator.Flow(
                    date = it.localDate,
                    amount = if (it.isPurchase()) amount else amount.add(it.realizedInterest()).negate(),
                    rate = it.spread?.takeIf { rate -> rate.signum() > 0 },
                )
            }

    private fun openingLotsAt(
        operations: List<BondOperation>,
        date: LocalDate,
    ): List<BondAccrualCalculator.OpeningLot> {
        val lots = mutableListOf<Pair<BigDecimal?, BigDecimal>>()

        operations
            .filter { it.localDate <= date }
            .sortedBy { it.localDate }
            .forEach { operation ->
                val value = operation.quantity.multiply(operation.unitPrice)

                if (operation.isPurchase()) {
                    lots.add(operation.spread?.takeIf { it.signum() > 0 } to value)
                } else {
                    val total = lots.fold(BigDecimal.ZERO) { acc, lot -> acc.add(lot.second) }

                    if (total.signum() > 0) {
                        val kept =
                            BigDecimal.ONE
                                .subtract(
                                    value.divide(total, MathContext.DECIMAL64),
                                ).max(BigDecimal.ZERO)
                        lots.replaceAll { it.first to it.second.multiply(kept) }
                    }
                }
            }

        return lots.map { BondAccrualCalculator.OpeningLot(it.second, it.first) }
    }

    private fun openingBalance(
        bond: Bond,
        month: YearMonth,
        operations: List<BondOperation>,
    ): BigDecimal =
        bondInterestCalculationRepository
            .findByBondAndReferenceMonth(bond, month.minusMonths(1))
            ?.finalValue
            ?: operations.calculateInvestedAmountUntil(month.atDay(1).minusDays(1))

    private fun accrue(
        bond: Bond,
        opening: BigDecimal,
        start: LocalDate,
        end: LocalDate,
        operations: List<BondOperation>,
    ): BigDecimal {
        val type = bond.interestType ?: return BigDecimal.ZERO
        val rate = requireNotNull(bond.interestRate) { "Bond ${bond.name} must have an interest rate configured" }

        return BondAccrualCalculator.periodInterest(
            type = type,
            index = indexFor(bond),
            bondRate = rate,
            opening = opening,
            openingLots = openingLotsAt(operations, start.minusDays(1)),
            flows = flowsBetween(operations, start, end),
            points = indexPoints(bond, start, end),
        )
    }

    private fun calculateMonthlyInterest(
        bond: Bond,
        month: YearMonth,
        operations: List<BondOperation>,
    ): BigDecimal {
        if (operations.isEmpty()) return BigDecimal.ZERO

        return accrue(bond, openingBalance(bond, month, operations), month.atDay(1), month.atEndOfMonth(), operations)
    }

    private fun calculatePeriodInterestForMonth(
        bond: Bond,
        month: YearMonth,
        periodStart: LocalDate,
        periodEnd: LocalDate,
        previousCalculatedUntil: LocalDate,
        accumulatedSoFar: BigDecimal,
        operations: List<BondOperation>,
    ): BondPeriodInterestResultDTO {
        if (operations.isEmpty()) {
            return BondPeriodInterestResultDTO(BigDecimal.ZERO, previousCalculatedUntil)
        }

        val (adjustedStart, adjustedEnd) =
            clampPeriodToMonth(month, periodStart, periodEnd)
                ?: return BondPeriodInterestResultDTO(BigDecimal.ZERO, previousCalculatedUntil)

        val points = indexPoints(bond, adjustedStart, adjustedEnd)
        if (points.isEmpty()) return BondPeriodInterestResultDTO(BigDecimal.ZERO, previousCalculatedUntil)

        val opening =
            operations
                .calculateInvestedAmountUntil(adjustedStart.minusDays(1))
                .add(accumulatedSoFar)

        return BondPeriodInterestResultDTO(
            accrue(bond, opening, adjustedStart, adjustedEnd, operations),
            points.maxOf { it.date },
        )
    }

    private fun rechain(
        calculation: BondInterestCalculation,
        context: BondInterestCalculationContextDTO,
        month: YearMonth,
        operations: List<BondOperation>,
    ) {
        val chained =
            context.accumulatedInterest
                .add(
                    calculation.monthlyInterest,
                ).subtract(realizedInterestInMonth(operations, month))
        val invested = calculateInvestedAmountForMonth(month, operations)
        val final = invested.add(chained)

        if (chained.compareTo(calculation.accumulatedInterest) == 0 &&
            invested.compareTo(calculation.investedAmount) == 0 &&
            final.compareTo(calculation.finalValue) == 0
        ) {
            return
        }

        calculation.apply {
            accumulatedInterest = chained
            investedAmount = invested
            quantity = operations.calculateQuantityUntil(month.atEndOfMonth())
            finalValue = final
        }

        bondInterestCalculationRepository.save(calculation)
    }

    private fun realizedInterestInMonth(
        operations: List<BondOperation>,
        month: YearMonth,
    ): BigDecimal =
        operations
            .filter { YearMonth.from(it.localDate) == month }
            .fold(BigDecimal.ZERO) { acc, operation -> acc.add(operation.realizedInterest()) }

    private fun calculateInvestedAmountForMonth(
        month: YearMonth,
        operations: List<BondOperation>,
    ): BigDecimal = operations.calculateInvestedAmountUntil(month.atEndOfMonth())

    private fun processIncrementalCalculation(
        bond: Bond,
        month: YearMonth,
        calculation: BondInterestCalculation,
        context: BondInterestCalculationContextDTO,
        operations: List<BondOperation>,
    ) {
        val calculatedUntil = calculation.calculatedUntilDate ?: return

        rechain(calculation, context, month, operations)

        val today = clockProvider.now().toLocalDate()
        val monthEnd = month.atEndOfMonth()

        if (today.isBeforeOrEqual(calculatedUntil) || calculatedUntil.isAfter(monthEnd)) {
            context.accumulatedInterest = calculation.accumulatedInterest
            logger.info(
                "Skipping recalculation for manually adjusted {} month {} (already up to date)",
                bond,
                month,
            )
            return
        }

        val periodStart = calculatedUntil.plusDays(1)
        val periodEnd = minOf(today, monthEnd)

        val result =
            calculatePeriodInterestForMonth(
                bond,
                month,
                periodStart,
                periodEnd,
                calculatedUntil,
                calculation.accumulatedInterest,
                operations,
            )

        if (result.interest <= BigDecimal.ZERO && result.lastDateWithData.isBeforeOrEqual(calculatedUntil)) {
            context.accumulatedInterest = calculation.accumulatedInterest

            logger.info(
                "No new data available for manually adjusted {} month {} (last calculated: {})",
                bond,
                month,
                calculatedUntil,
            )
            return
        }

        val investedAmount = calculateInvestedAmountForMonth(month, operations)
        val quantity = operations.calculateQuantityUntil(month.atEndOfMonth())

        val newMonthlyInterest = calculation.monthlyInterest.add(result.interest)

        val newAccumulated =
            context.accumulatedInterest.add(newMonthlyInterest).subtract(realizedInterestInMonth(operations, month))

        calculation.apply {
            calculationDate = clockProvider.now().toLocalDate()
            calculatedUntilDate = result.lastDateWithData
            this.quantity = quantity
            this.investedAmount = investedAmount
            monthlyInterest = newMonthlyInterest
            accumulatedInterest = newAccumulated
            finalValue = investedAmount.add(newAccumulated)
        }

        bondInterestCalculationRepository.save(calculation)

        context.accumulatedInterest = newAccumulated

        logger.info(
            "Incremental update for manually adjusted {} month {}: added={}, new monthly={}, accumulated={}, calculated until={}",
            bond,
            month,
            result.interest,
            newMonthlyInterest,
            newAccumulated,
            result.lastDateWithData,
        )
    }

    private fun createNewCalculation(
        bond: Bond,
        month: YearMonth,
        context: BondInterestCalculationContextDTO,
        operations: List<BondOperation>,
    ) {
        val monthlyInterest = calculateMonthlyInterest(bond, month, operations)
        val investedAmount = calculateInvestedAmountForMonth(month, operations)
        val quantity = operations.calculateQuantityUntil(month.atEndOfMonth())

        val accumulated =
            context.accumulatedInterest
                .add(
                    monthlyInterest,
                ).subtract(realizedInterestInMonth(operations, month))

        val calculation =
            BondInterestCalculation(
                bond = bond,
                referenceMonth = month,
                calculationDate = clockProvider.now().toLocalDate(),
                calculatedUntilDate = resolveCalculatedUntilDate(bond, month),
                quantity = quantity,
                investedAmount = investedAmount,
                monthlyInterest = monthlyInterest,
                accumulatedInterest = accumulated,
                finalValue = investedAmount.add(accumulated),
                calculationMethod = getCalculationMethod(bond),
                createdAt = clockProvider.now(),
                manuallyAdjusted = false,
            )

        bondInterestCalculationRepository.save(calculation)

        context.accumulatedInterest = accumulated
    }

    private fun resolveCalculatedUntilDate(
        bond: Bond,
        month: YearMonth,
    ): LocalDate {
        val monthEnd = month.atEndOfMonth()
        val lastDate = getLastDateWithData(bond, month.atDay(1), clockProvider.now().toLocalDate())

        return minOf(lastDate, monthEnd)
    }

    private fun updateMonthCalculation(
        calculation: BondInterestCalculation,
        newMonthlyInterest: BigDecimal,
        calculatedUntil: LocalDate,
        manuallyAdjusted: Boolean,
    ): BigDecimal {
        val delta = newMonthlyInterest.subtract(calculation.monthlyInterest)

        calculation.apply {
            monthlyInterest = newMonthlyInterest
            this.manuallyAdjusted = manuallyAdjusted
            calculatedUntilDate = calculatedUntil
            val newAccumulated = accumulatedInterest.add(delta)
            accumulatedInterest = newAccumulated
            finalValue = investedAmount.add(newAccumulated)
        }

        bondInterestCalculationRepository.save(calculation)

        return delta
    }

    private fun propagateDeltaToFutureMonths(
        bond: Bond,
        startMonth: YearMonth,
        delta: BigDecimal,
    ) {
        val futureCalculations =
            bondInterestCalculationRepository
                .findByBondAndReferenceMonthAfter(bond, startMonth)
                .map { future ->
                    val updatedAccumulated = future.accumulatedInterest.add(delta)

                    future.apply {
                        accumulatedInterest = updatedAccumulated
                        finalValue = investedAmount.add(updatedAccumulated)
                    }

                    logger.debug(
                        "Propagated delta to {} month {}: accumulated={}",
                        bond,
                        future.referenceMonth,
                        updatedAccumulated,
                    )

                    future
                }

        bondInterestCalculationRepository.saveAll(futureCalculations)
    }

    private fun clampPeriodToMonth(
        month: YearMonth,
        start: LocalDate,
        end: LocalDate,
    ): Pair<LocalDate, LocalDate>? {
        val monthStart = month.atDay(1)
        val monthEnd = month.atEndOfMonth()

        val adjustedStart = maxOf(start, monthStart)
        val adjustedEnd = minOf(end, monthEnd)

        return when (adjustedStart.isAfter(adjustedEnd)) {
            true -> null
            false -> adjustedStart to adjustedEnd
        }
    }

    private fun resolveCalculationContext(
        bond: Bond,
        startMonth: YearMonth,
        currentMonth: YearMonth,
        fromStart: Boolean = false,
    ): BondInterestCalculationContextDTO? {
        if (fromStart) return BondInterestCalculationContextDTO(startMonth, BigDecimal.ZERO)

        val lastCalculation =
            bondInterestCalculationRepository
                .findLastCalculatedMonth(bond) ?: run {
                logger.info(
                    "No previous calculations found for {}, calculating from {} to {}",
                    bond,
                    startMonth,
                    currentMonth,
                )

                return BondInterestCalculationContextDTO(startMonth, BigDecimal.ZERO)
            }

        val lastMonth = lastCalculation.referenceMonth
        val accumulated =
            if (lastMonth == currentMonth) {
                bondInterestCalculationRepository
                    .findByBondAndReferenceMonth(bond, currentMonth.minusMonths(1))
                    ?.accumulatedInterest
                    ?: BigDecimal.ZERO
            } else {
                lastCalculation.accumulatedInterest
            }

        val start =
            when {
                lastMonth == currentMonth -> {
                    // Check if there's new data available since last calculation
                    val lastDateWithData =
                        getLastDateWithData(bond, currentMonth.atDay(1), clockProvider.now().toLocalDate())
                    val alreadyCalculatedUntil = lastCalculation.calculatedUntilDate

                    alreadyCalculatedUntil?.let {
                        if (alreadyCalculatedUntil >= lastDateWithData) {
                            logger.debug(
                                "Skipping recalculation for {} month {}: already calculated until {} (latest data: {})",
                                bond,
                                currentMonth,
                                alreadyCalculatedUntil,
                                lastDateWithData,
                            )
                            return null
                        }
                    }

                    logger.debug(
                        "Recalculating {} month {}: calculated until {} but new data available until {}",
                        bond,
                        currentMonth,
                        alreadyCalculatedUntil,
                        lastDateWithData,
                    )
                    currentMonth
                }
                else -> lastMonth.plusMonths(1)
            }

        return BondInterestCalculationContextDTO(start, accumulated)
    }

    private fun generateMonthSequence(
        start: YearMonth,
        end: YearMonth,
    ): Sequence<YearMonth> =
        generateSequence(start) { it.plusMonths(1) }
            .takeWhile { !it.isAfter(end) }

    private fun processMonthCalculation(
        bond: Bond,
        month: YearMonth,
        context: BondInterestCalculationContextDTO,
        operations: List<BondOperation>,
    ) = bondInterestCalculationRepository
        .findByBondAndReferenceMonth(bond, month)
        .also { existing ->
            when (existing) {
                null -> createNewCalculation(bond, month, context, operations)
                else -> updateExistingCalculation(bond, month, existing, context, operations)
            }
        }

    private fun updateExistingCalculation(
        bond: Bond,
        month: YearMonth,
        calculation: BondInterestCalculation,
        context: BondInterestCalculationContextDTO,
        operations: List<BondOperation>,
    ) {
        if (calculation.manuallyAdjusted) {
            processIncrementalCalculation(bond, month, calculation, context, operations)
            return
        }

        val monthlyInterest = calculateMonthlyInterest(bond, month, operations)
        val investedAmount = calculateInvestedAmountForMonth(month, operations)
        val quantity = operations.calculateQuantityUntil(month.atEndOfMonth())

        val newAccumulated =
            context.accumulatedInterest.add(monthlyInterest).subtract(realizedInterestInMonth(operations, month))

        calculation.apply {
            calculationDate = clockProvider.now().toLocalDate()
            calculatedUntilDate = resolveCalculatedUntilDate(bond, month)
            this.quantity = quantity
            this.investedAmount = investedAmount
            this.monthlyInterest = monthlyInterest
            this.accumulatedInterest = newAccumulated
            finalValue = investedAmount.add(newAccumulated)
            calculationMethod = getCalculationMethod(bond)
        }

        bondInterestCalculationRepository.save(calculation)

        context.accumulatedInterest = newAccumulated
    }

    private fun requireBondConfiguration(bond: Bond) {
        requireNotNull(bond.interestRate) {
            "Bond ${bond.name} must have an interest rate configured"
        }

        if (bond.interestType != InterestType.ZERO_COUPON) {
            requireNotNull(bond.interestIndex) {
                "Bond ${bond.name} must have an interest index configured"
            }
        }
    }
}
