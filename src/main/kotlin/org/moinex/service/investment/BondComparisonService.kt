/*
 * Filename: BondComparisonService.kt
 * Created on: November 7, 2026
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 */

package org.moinex.service.investment

import org.moinex.common.ClockProvider
import org.moinex.model.dto.BondComparisonDTO
import org.moinex.model.dto.BondComparisonRequest
import org.moinex.model.dto.BondLineDTO
import org.moinex.model.dto.BondMonthlyDTO
import org.moinex.model.dto.BondMonthlySeriesDTO
import org.moinex.model.dto.ComparisonPeriod
import org.moinex.model.dto.SeriesPointDTO
import org.moinex.model.enums.InterestIndex
import org.moinex.model.investment.Bond
import org.moinex.repository.investment.BondOperationRepository
import org.moinex.repository.investment.BondRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate
import java.time.YearMonth

/**
 * Compares the bonds with each other and with the CDI: the accumulated return of each one over a period and the interest
 * each one earned per month, gross or net of the estimated income tax.
 */
@Service
class BondComparisonService(
    private val bondRepository: BondRepository,
    private val bondOperationRepository: BondOperationRepository,
    private val bondDailySeries: BondDailySeries,
    private val bondInterestCalculationService: BondInterestCalculationService,
    private val marketIndicatorService: MarketIndicatorService,
    private val clockProvider: ClockProvider,
) {
    @Transactional(readOnly = true)
    fun comparison(request: BondComparisonRequest): BondComparisonDTO {
        val today = clockProvider.today()
        val bonds = bondsOf(request)
        val positions = bonds.associateWith { bondDailySeries.position(it, today) }
        val start = periodStart(request.period, bonds, today)

        val lines = mutableListOf<BondLineDTO>()
        val excluded = mutableListOf<Bond>()

        bonds.forEach { bond ->
            val position = positions[bond]
            val line = position?.let { line(bond, it, request.net, start, today) }

            if (line == null) excluded.add(bond) else lines.add(line)
        }

        val days = lines.flatMap { line -> line.points.map { it.date } }.distinct().sorted()

        return BondComparisonDTO(
            lines = lines.sortedByDescending { it.resultPct },
            cdi = cdi(start, today, days),
            excluded = excluded,
        )
    }

    @Transactional(readOnly = true)
    fun monthly(request: BondComparisonRequest): BondMonthlyDTO {
        val today = clockProvider.today()
        val bonds = bondsOf(request)
        val currentMonth = YearMonth.from(today)

        val histories = bonds.associateWith { bondInterestCalculationService.getMonthlyInterestHistory(it) }
        val first =
            when (val months = request.period.months) {
                null -> histories.values.flatten().minOfOrNull { it.referenceMonth } ?: currentMonth
                else -> currentMonth.minusMonths(months.toLong() - 1)
            }
        val months = generateSequence(first) { it.plusMonths(1) }.takeWhile { !it.isAfter(currentMonth) }.toList()

        val series = mutableListOf<BondMonthlySeriesDTO>()
        val excluded = mutableListOf<Bond>()

        bonds.forEach { bond ->
            val rows = histories.getValue(bond).filter { it.referenceMonth in months }

            if (rows.isEmpty()) {
                excluded.add(bond)
                return@forEach
            }

            val operations = bondOperationRepository.findByBondOrderByOperationDateAsc(bond)
            val values =
                rows.associate { row ->
                    val gross = row.monthlyInterest
                    val value =
                        if (request.net) {
                            val lots = BondTaxEstimator.lotsAt(operations, row.referenceMonth.atEndOfMonth())
                            val rate = BondTaxEstimator.effectiveRate(bond.type, lots, row.accumulatedInterest)
                            gross.multiply(BigDecimal.ONE.subtract(rate))
                        } else {
                            gross
                        }

                    row.referenceMonth to value.setScale(2, RoundingMode.HALF_UP)
                }

            series.add(BondMonthlySeriesDTO(bond, values))
        }

        return BondMonthlyDTO(months, series, excluded)
    }

    private fun bondsOf(request: BondComparisonRequest): List<Bond> =
        bondRepository.findByArchivedFalseOrderByNameAsc() +
            if (request.includeArchived) bondRepository.findByArchivedTrueOrderByNameAsc() else emptyList()

    private fun periodStart(
        period: ComparisonPeriod,
        bonds: List<Bond>,
        today: LocalDate,
    ): LocalDate =
        period.months?.let { today.minusMonths(it.toLong()) }
            ?: bonds
                .mapNotNull { bond ->
                    bondOperationRepository
                        .findByBondOrderByOperationDateAsc(bond)
                        .firstOrNull { it.isPurchase() && it.walletTransaction != null }
                        ?.localDate
                }.minOrNull()
            ?: today

    /**
     * The accumulated return of [bond] in [start]..[today], starting at zero on the first day it has a balance; null
     * when it has fewer than two days with a balance in the period.
     */
    private fun line(
        bond: Bond,
        position: PortfolioPerformanceCalculator.FixedIncomePosition,
        net: Boolean,
        start: LocalDate,
        today: LocalDate,
    ): BondLineDTO? {
        val valued = if (net) netOfTax(bond, position, today) else position

        val points =
            PortfolioPerformanceCalculator
                .portfolioReturn(emptyList(), start, today, fixedIncome = listOf(valued))
                .let { all ->
                    val firstDayWithBalance =
                        valued.values
                            .firstOrNull { it.first >= start && it.second.signum() > 0 }
                            ?.first
                    all.filter { firstDayWithBalance != null && !it.date.isBefore(firstDayWithBalance) }
                }

        if (points.size < 2) return null

        return BondLineDTO(bond, points, points.last().pct)
    }

    /**
     * The same position with each day's value less the estimated income tax on the interest held that day.
     */
    private fun netOfTax(
        bond: Bond,
        position: PortfolioPerformanceCalculator.FixedIncomePosition,
        today: LocalDate,
    ): PortfolioPerformanceCalculator.FixedIncomePosition {
        val operations = bondOperationRepository.findByBondOrderByOperationDateAsc(bond)

        return position.copy(
            values =
                position.values.map { (date, value) ->
                    val lots = BondTaxEstimator.lotsAt(operations, minOf(date, today))
                    val principal = lots.fold(BigDecimal.ZERO) { acc, lot -> acc.add(lot.principal) }
                    val interest = value.subtract(principal).max(BigDecimal.ZERO)

                    date to value.subtract(BondTaxEstimator.estimate(bond.type, lots, interest).tax)
                },
        )
    }

    private fun cdi(
        start: LocalDate,
        today: LocalDate,
        days: List<LocalDate>,
    ): List<SeriesPointDTO> {
        if (days.isEmpty()) return emptyList()

        val rates =
            marketIndicatorService
                .getIndicatorHistoryBetween(InterestIndex.CDI, start, today)
                .map { it.referenceDate to it.rateValue }

        return PortfolioPerformanceCalculator.cumulativeFromDailyRates(rates, days)
    }
}
