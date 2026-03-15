/*
 * Filename: BondInterestCalculationService.kt (original filename: BondInterestCalculationService.java)
 * Created on: February 20, 2026
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 *
 * Migrate to Kotlin on 12/03/2026
 */

package org.moinex.service.investment

import org.moinex.common.extension.buyOperationsUntil
import org.moinex.common.extension.calculateOperationStateUntil
import org.moinex.common.extension.calculateQuantityUntil
import org.moinex.common.extension.findByIdOrThrow
import org.moinex.common.extension.getLastBuyPrice
import org.moinex.common.extension.isAfterOrEqual
import org.moinex.common.extension.isBeforeOrEqual
import org.moinex.common.extension.toRounded
import org.moinex.model.dto.BondInterestCalculationContextDTO
import org.moinex.model.dto.BondOperationStateDTO
import org.moinex.model.dto.BondPeriodInterestResultDTO
import org.moinex.model.dto.BondPeriodStateDTO
import org.moinex.model.enums.InterestIndex
import org.moinex.model.enums.InterestType
import org.moinex.model.investment.Bond
import org.moinex.model.investment.BondInterestCalculation
import org.moinex.model.investment.BondOperation
import org.moinex.model.investment.MarketIndicatorHistory
import org.moinex.repository.investment.BondInterestCalculationRepository
import org.moinex.repository.investment.BondOperationRepository
import org.moinex.repository.investment.BondRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth
import kotlin.math.pow

@Service
class BondInterestCalculationService(
    private val bondRepository: BondRepository,
    private val bondOperationRepository: BondOperationRepository,
    private val bondInterestCalculationRepository: BondInterestCalculationRepository,
    private val marketIndicatorService: MarketIndicatorService,
) {
    private val logger = LoggerFactory.getLogger(BondInterestCalculationService::class.java)

    companion object {
        private const val DAYS_PER_YEAR = 252
        private val HUNDRED = BigDecimal("100")
        private const val FIXED_FORMAT = "FIXED: %s%% a.a."
        private const val FLOATING_FORMAT = "FLOATING: %s + %s%%"
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
    fun calculateAndStoreMonthlyInterest(bond: Bond) {
        requireBondConfiguration(bond)

        val operations = bondOperationRepository.findByBondOrderByOperationDateAsc(bond)

        val firstBuy =
            operations.firstOrNull { it.isPurchase() }
                ?: run {
                    logger.warn("$bond has no buy operations, skipping monthly interest calculation")
                    return
                }

        val startMonth = YearMonth.from(firstBuy.walletTransaction!!.date.toLocalDate())
        val currentMonth = YearMonth.now()

        val context = resolveCalculationContext(bond, startMonth, currentMonth)

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

        bondInterestCalculationRepository.deleteByBond(bond)

        calculateAndStoreMonthlyInterest(bond)
        logger.info("Completed full recalculation of interest history for $bond")
    }

    @Transactional
    fun adjustMonthlyInterest(
        bondId: Int,
        month: YearMonth,
        newMonthlyInterest: BigDecimal,
    ) {
        val bond = bondRepository.findByIdOrThrow(bondId)
        requireBondConfiguration(bond)

        val calculation =
            requireNotNull(
                bondInterestCalculationRepository.findByBondAndReferenceMonth(bond, month),
            ) {
                "No interest calculation found for $bond in month $month"
            }

        val calculatedUntil = resolveCalculatedUntilDate(bond, month)

        val delta =
            updateMonthCalculation(
                calculation = calculation,
                newMonthlyInterest = newMonthlyInterest,
                calculatedUntil = calculatedUntil,
                manuallyAdjusted = true,
            )

        logger.info(
            "Adjusted monthly interest for {} month {}: delta={}",
            bond,
            month,
            delta,
        )

        propagateDeltaToFutureMonths(bond, month, delta)

        logger.info(
            "Completed adjustment propagation for {} starting from month {}",
            bond,
            month,
        )
    }

    @Transactional
    fun resetToAutomaticCalculation(
        bondId: Int,
        month: YearMonth,
    ) {
        val bond = bondRepository.findByIdOrThrow(bondId)

        val calculation =
            requireNotNull(
                bondInterestCalculationRepository.findByBondAndReferenceMonth(bond, month),
            ) {
                "No interest calculation found for $bond in month $month"
            }

        if (!calculation.manuallyAdjusted) {
            logger.warn(
                "Month {} for {} was not manually adjusted, skipping reset",
                month,
                bond,
            )
            return
        }

        val operations = bondOperationRepository.findByBondOrderByOperationDateAsc(bond)
        val recalculatedInterest = calculateMonthlyInterest(bond, month, operations)
        val calculatedUntil = resolveCalculatedUntilDate(bond, month)

        val delta =
            updateMonthCalculation(
                calculation = calculation,
                newMonthlyInterest = recalculatedInterest,
                calculatedUntil = calculatedUntil,
                manuallyAdjusted = false,
            )

        logger.info(
            "Reset monthly interest for {} month {} to automatic: delta={}",
            bond,
            month,
            delta,
        )

        propagateDeltaToFutureMonths(bond, month, delta)
    }

    @Transactional(readOnly = true)
    fun getCurrentMonthInterest(bondId: Int): BigDecimal? {
        val bond = bondRepository.findByIdOrThrow(bondId)

        return bondInterestCalculationRepository
            .findByBondAndReferenceMonth(bond, YearMonth.now())
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

    private fun calculateMonthlyInterest(
        bond: Bond,
        month: YearMonth,
        operations: List<BondOperation>,
    ): BigDecimal {
        if (operations.isEmpty()) return BigDecimal.ZERO

        val monthStart = month.atDay(1)
        val monthEnd = month.atEndOfMonth()

        val initialOperationState = operations.calculateOperationStateUntil(monthStart.minusDays(1))
        val initialState =
            BondOperationStateDTO(
                quantity = initialOperationState.quantity,
                lastSpread = initialOperationState.lastSpread,
                lastBuyPrice = initialOperationState.lastBuyPrice,
                interest = BigDecimal.ZERO,
                lastDate = monthStart,
            )

        val state =
            operations
                .asSequence()
                .filter { it.localDate.isAfterOrEqual(monthStart) }
                .takeWhile { it.localDate.isBeforeOrEqual(monthEnd) }
                .fold(initialState) { state, operation ->
                    val operationDate = operation.localDate
                    val newInterest = calculateInterestUntilOperation(bond, state, operationDate)
                    updateStateWithOperation(state, operation, newInterest, operationDate)
                }

        if (shouldSkipFinalInterestCalculation(state, monthEnd)) {
            return state.interest ?: BigDecimal.ZERO
        }

        return calculateFinalMonthInterest(bond, state, monthEnd, operations)
    }

    private fun calculateInterestUntilOperation(
        bond: Bond,
        state: BondOperationStateDTO,
        operationDate: LocalDate,
    ): BigDecimal {
        if (!shouldCalculateInterestBetweenOperations(state, operationDate)) {
            return state.interest ?: BigDecimal.ZERO
        }

        val spread = getSpreadToUse(state.lastSpread, bond.interestRate)
        val buyPrice = state.lastBuyPrice ?: BigDecimal.ZERO

        val interest =
            calculatePeriodInterest(
                bond,
                state.lastDate!!,
                operationDate,
                state.quantity,
                buyPrice,
                spread,
            )

        return (state.interest ?: BigDecimal.ZERO).add(interest)
    }

    private fun calculateFinalMonthInterest(
        bond: Bond,
        state: BondOperationStateDTO,
        monthEnd: LocalDate,
        operations: List<BondOperation>,
    ): BigDecimal {
        val lastBuyPrice = state.lastBuyPrice ?: operations.getLastBuyPrice() ?: BigDecimal.ZERO
        val spread = getSpreadToUse(state.lastSpread, bond.interestRate)

        val finalInterest =
            calculatePeriodInterest(
                bond,
                state.lastDate!!,
                monthEnd,
                state.quantity,
                lastBuyPrice,
                spread,
            )

        return (state.interest ?: BigDecimal.ZERO).add(finalInterest)
    }

    private fun shouldCalculateInterestBetweenOperations(
        state: BondOperationStateDTO,
        operationDate: LocalDate,
    ): Boolean =
        state.quantity > BigDecimal.ZERO &&
            state.lastDate != null &&
            operationDate > state.lastDate

    private fun shouldSkipFinalInterestCalculation(
        state: BondOperationStateDTO,
        monthEnd: LocalDate,
    ): Boolean =
        state.quantity <= BigDecimal.ZERO ||
            state.lastDate == null ||
            state.lastDate >= monthEnd

    private fun updateStateWithOperation(
        state: BondOperationStateDTO,
        operation: BondOperation,
        newInterest: BigDecimal,
        operationDate: LocalDate = operation.localDate,
    ): BondOperationStateDTO {
        val newQuantity =
            when {
                operation.isPurchase() -> state.quantity.add(operation.quantity)
                else -> state.quantity.minus(operation.quantity)
            }

        val newSpread = operation.spread ?: state.lastSpread

        val newLastBuyPrice =
            when {
                operation.isPurchase() -> operation.unitPrice
                else -> state.lastBuyPrice ?: BigDecimal.ZERO
            }

        return state.copy(
            interest = newInterest,
            quantity = newQuantity,
            lastSpread = newSpread,
            lastDate = operationDate,
            lastBuyPrice = newLastBuyPrice,
        )
    }

    private fun updatePeriodStateWithOperation(
        bond: Bond,
        state: BondPeriodStateDTO,
        operation: BondOperation,
    ): BondPeriodStateDTO {
        val operationDate = operation.localDate

        var newInterest = state.interest
        if (state.quantity > BigDecimal.ZERO && operationDate.isAfter(state.lastCalculationDate)) {
            val spreadToUse = getSpreadToUse(state.lastSpread, bond.interestRate)
            val buyPrice = state.lastBuyPrice ?: BigDecimal.ZERO

            val interest =
                calculatePeriodInterest(
                    bond,
                    state.lastCalculationDate,
                    operationDate,
                    state.quantity,
                    buyPrice,
                    spreadToUse,
                )

            newInterest = newInterest.add(interest)
        }

        val newQuantity =
            when {
                operation.isPurchase() -> state.quantity.add(operation.quantity)
                else -> state.quantity.subtract(operation.quantity)
            }

        val newSpread = operation.spread ?: state.lastSpread
        val newLastBuyPrice =
            when {
                operation.isPurchase() -> operation.unitPrice
                else -> state.lastBuyPrice
            }

        return state.copy(
            quantity = newQuantity,
            lastSpread = newSpread,
            lastBuyPrice = newLastBuyPrice,
            interest = newInterest,
            lastCalculationDate = operationDate,
        )
    }

    private fun calculatePeriodInterest(
        bond: Bond,
        startDate: LocalDate,
        endDate: LocalDate,
        quantity: BigDecimal,
        unitPrice: BigDecimal,
        spread: BigDecimal,
    ): BigDecimal =
        when (bond.interestType) {
            InterestType.FIXED -> calculateFixedInterest(bond, startDate, endDate, quantity, unitPrice)
            InterestType.FLOATING ->
                calculateFloatingInterest(
                    bond,
                    startDate,
                    endDate,
                    quantity,
                    unitPrice,
                    bond.interestIndex!!,
                    spread,
                )
            else -> BigDecimal.ZERO
        }

    private fun calculateFinalPeriodInterest(
        bond: Bond,
        state: BondPeriodStateDTO,
        adjustedEnd: LocalDate,
        operations: List<BondOperation>,
    ): BondPeriodStateDTO {
        if (state.quantity <= BigDecimal.ZERO || state.lastCalculationDate >= adjustedEnd) {
            return state
        }

        val lastBuyPrice =
            state.lastBuyPrice
                ?: operations.getLastBuyPrice()
                ?: throw IllegalStateException("Cannot calculate interest without buy operations for bond ${bond.name}")

        val spreadToUse = getSpreadToUse(state.lastSpread, bond.interestRate)
        val indicatorIndex =
            bond.interestIndex
                ?: throw IllegalStateException("Bond ${bond.name} must have an interest index configured")

        val availableData =
            marketIndicatorService.getIndicatorHistoryBetween(
                indicatorIndex,
                state.lastCalculationDate,
                adjustedEnd,
            )

        if (availableData.isEmpty()) {
            return state
        }

        val actualLastDate = availableData.maxOf { it.referenceDate }
        val interest =
            calculatePeriodInterest(
                bond,
                state.lastCalculationDate,
                actualLastDate,
                state.quantity,
                lastBuyPrice,
                spreadToUse,
            )

        return state.copy(
            interest = state.interest.add(interest),
            actualLastDate = actualLastDate,
        )
    }

    private fun calculateFloatingInterest(
        bond: Bond,
        startDate: LocalDate,
        endDate: LocalDate,
        quantity: BigDecimal,
        unitPrice: BigDecimal,
        indicatorType: InterestIndex,
        spread: BigDecimal,
    ): BigDecimal {
        val investedAmount = quantity.multiply(unitPrice)

        return runCatching {
            val accumulatedRate =
                getAccumulatedIndicatorRate(
                    indicatorType,
                    startDate,
                    endDate,
                    spread,
                )

            investedAmount.multiply(accumulatedRate.subtract(BigDecimal.ONE)).also { interest ->
                logger.debug(
                    "Floating interest: bond={}, indicator={}, spread={}, accumulated={}, invested={}, interest={}",
                    bond,
                    indicatorType,
                    spread,
                    accumulatedRate.toRounded(6),
                    investedAmount.toRounded(),
                    interest.toRounded(),
                )
            }
        }.getOrElse { e ->
            logger.warn(
                "Could not calculate floating interest for {}: {}",
                bond,
                e.message,
            )
            BigDecimal.ZERO
        }
    }

    private fun calculateFixedInterest(
        bond: Bond,
        startDate: LocalDate,
        endDate: LocalDate,
        quantity: BigDecimal,
        unitPrice: BigDecimal,
    ): BigDecimal {
        val investedAmount = quantity.multiply(unitPrice)

        val index =
            requireNotNull(bond.interestIndex) {
                "Bond must have an interest index"
            }

        val indexData =
            marketIndicatorService.getIndicatorHistoryBetween(
                index,
                startDate,
                endDate,
            )

        val businessDays = indexData.size

        if (businessDays == 0) {
            logger.warn(
                "No business days found between {} and {} using {}",
                startDate,
                endDate,
                index,
            )
            return BigDecimal.ZERO
        }

        val interestRate =
            requireNotNull(bond.interestRate) {
                "Bond must have an interest rate"
            }

        val annualFactor = BigDecimal.ONE.add(interestRate.divide(HUNDRED))

        val exponent = businessDays / DAYS_PER_YEAR.toDouble()

        val accumulatedFactor = BigDecimal(annualFactor.toDouble().pow(exponent))

        val interest = investedAmount.multiply(accumulatedFactor.subtract(BigDecimal.ONE))

        logger.debug(
            "Fixed interest: bond={}, days={}, rate={}, invested={}, factor={}, interest={}",
            bond,
            businessDays,
            bond.interestRate,
            investedAmount.toRounded(),
            accumulatedFactor.toRounded(6),
            interest.toRounded(),
        )

        return interest
    }

    private fun getAccumulatedIndicatorRate(
        indicatorType: InterestIndex,
        startDate: LocalDate,
        endDate: LocalDate,
        percentageOfIndicator: BigDecimal,
    ): BigDecimal {
        val historicalData =
            marketIndicatorService.getIndicatorHistoryBetween(
                indicatorType,
                startDate,
                endDate,
            )

        if (historicalData.isEmpty()) {
            logger.warn(
                "No historical data found for {} between {} and {}",
                indicatorType,
                startDate,
                endDate,
            )
            return BigDecimal.ONE
        }

        val factor = getAccumulatedFactor(percentageOfIndicator, historicalData)

        logger.debug(
            "Accumulated rate {} from {} to {}: {} days factor={}",
            indicatorType,
            startDate,
            endDate,
            historicalData.size,
            factor.toRounded(6),
        )

        return factor
    }

    private fun getAccumulatedFactor(
        percentageOfIndicator: BigDecimal,
        historicalData: List<MarketIndicatorHistory>,
    ): BigDecimal =
        historicalData.fold(BigDecimal.ONE) { acc, data ->
            val adjustedRate = data.rateValue.add(percentageOfIndicator)

            val dailyFactor = BigDecimal.ONE.add(adjustedRate.divide(HUNDRED))

            acc.multiply(dailyFactor)
        }

    private fun getCalculationMethod(bond: Bond): String =
        when (bond.interestType) {
            InterestType.FIXED -> String.format(FIXED_FORMAT, bond.interestRate)
            InterestType.FLOATING -> String.format(FLOATING_FORMAT, bond.interestIndex, bond.interestRate)
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

    private fun calculatePeriodInterestForMonth(
        bond: Bond,
        month: YearMonth,
        periodStart: LocalDate,
        periodEnd: LocalDate,
        previousCalculatedUntil: LocalDate,
        operations: List<BondOperation>,
    ): BondPeriodInterestResultDTO {
        if (operations.isEmpty()) {
            return BondPeriodInterestResultDTO(BigDecimal.ZERO, previousCalculatedUntil)
        }

        val period =
            clampPeriodToMonth(month, periodStart, periodEnd)
                ?: return BondPeriodInterestResultDTO(BigDecimal.ZERO, previousCalculatedUntil)

        val (adjustedStart, adjustedEnd) = period

        val initialState = operations.calculateOperationStateUntil(adjustedStart.minusDays(1))
        val initialPeriodState =
            BondPeriodStateDTO(
                quantity = initialState.quantity,
                lastSpread = initialState.lastSpread,
                lastBuyPrice = initialState.lastBuyPrice,
                interest = BigDecimal.ZERO,
                lastCalculationDate = adjustedStart,
                actualLastDate = previousCalculatedUntil,
            )

        val state =
            operations
                .asSequence()
                .filter { it.localDate >= adjustedStart }
                .takeWhile { it.localDate <= adjustedEnd }
                .fold(initialPeriodState) { state, operation ->
                    updatePeriodStateWithOperation(bond, state, operation)
                }

        val finalState = calculateFinalPeriodInterest(bond, state, adjustedEnd, operations)

        logger.info(
            "Period interest calculated for bond {} from {} to {}",
            bond.name,
            adjustedStart,
            adjustedEnd,
        )
        logger.info("Period interest total: {}", finalState.interest)
        logger.info("Actual last date with data: {}", finalState.actualLastDate)

        return BondPeriodInterestResultDTO(finalState.interest, finalState.actualLastDate)
    }

    private fun calculateInvestedAmountForMonth(
        month: YearMonth,
        operations: List<BondOperation>,
    ): BigDecimal {
        val monthEnd = month.atEndOfMonth()

        return operations
            .buyOperationsUntil(monthEnd)
            .fold(BigDecimal.ZERO) { acc, operation ->
                acc.add(operation.totalValue)
            }
    }

    private fun processIncrementalCalculation(
        bond: Bond,
        month: YearMonth,
        calculation: BondInterestCalculation,
        context: BondInterestCalculationContextDTO,
        operations: List<BondOperation>,
    ) {
        val calculatedUntil = calculation.calculatedUntilDate ?: return

        val today = LocalDate.now()
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

        val previousAccumulated =
            calculation.accumulatedInterest.subtract(calculation.monthlyInterest)

        val newAccumulated = previousAccumulated.add(newMonthlyInterest)

        calculation.apply {
            calculationDate = LocalDate.now()
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

        val accumulated = context.accumulatedInterest.add(monthlyInterest)

        val calculation =
            BondInterestCalculation(
                bond = bond,
                referenceMonth = month,
                calculationDate = LocalDate.now(),
                calculatedUntilDate = resolveCalculatedUntilDate(bond, month),
                quantity = quantity,
                investedAmount = investedAmount,
                monthlyInterest = monthlyInterest,
                accumulatedInterest = accumulated,
                finalValue = investedAmount.add(accumulated),
                calculationMethod = getCalculationMethod(bond),
                createdAt = LocalDateTime.now(),
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
        val lastDate = getLastDateWithData(bond, month.atDay(1), LocalDate.now())

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
    ): BondInterestCalculationContextDTO {
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
        val accumulated = lastCalculation.accumulatedInterest

        val start =
            when {
                lastMonth == currentMonth -> currentMonth
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

        val newAccumulated = context.accumulatedInterest.add(monthlyInterest)

        calculation.apply {
            calculationDate = LocalDate.now()
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

    private fun getSpreadToUse(
        lastSpread: BigDecimal?,
        defaultSpread: BigDecimal?,
    ): BigDecimal = lastSpread ?: defaultSpread ?: BigDecimal.ZERO

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
