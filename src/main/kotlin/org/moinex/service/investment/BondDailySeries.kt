/*
 * Filename: BondDailySeries.kt
 * Created on: November 7, 2026
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 */

package org.moinex.service.investment

import org.moinex.common.extension.realizedInterest
import org.moinex.model.enums.InterestIndex
import org.moinex.model.investment.Bond
import org.springframework.stereotype.Service
import java.time.LocalDate

/**
 * A bond valued day by day by the same rules that give its monthly interest, with the money put in and taken out.
 */
@Service
class BondDailySeries(
    private val bondOperationRepository: org.moinex.repository.investment.BondOperationRepository,
    private val marketIndicatorService: MarketIndicatorService,
) {
    /**
     * The position of [bond] until [today], or null when it has no rate, type, purchase or index history.
     */
    fun position(
        bond: Bond,
        today: LocalDate,
    ): PortfolioPerformanceCalculator.FixedIncomePosition? {
        val type = bond.interestType ?: return null
        val rate = bond.interestRate ?: return null
        val operations =
            bondOperationRepository.findByBondOrderByOperationDateAsc(bond).filter { it.walletTransaction != null }
        val first = operations.firstOrNull { it.isPurchase() }?.localDate ?: return null
        val index = bond.interestIndex ?: InterestIndex.CDI

        val points =
            marketIndicatorService
                .getIndicatorHistoryBetween(index, first, today)
                .filter { it.referenceDate in first..today }
                .map { BondAccrualCalculator.IndexPoint(it.referenceDate, it.rateValue) }
        if (points.isEmpty()) return null

        val flows =
            operations.map {
                val base = it.quantity.multiply(it.unitPrice)
                BondAccrualCalculator.Flow(
                    date = it.localDate,
                    amount = if (it.isPurchase()) base else base.add(it.realizedInterest()).negate(),
                    rate = it.spread?.takeIf { spread -> spread.signum() > 0 },
                )
            }

        return PortfolioPerformanceCalculator.FixedIncomePosition(
            BondAccrualCalculator.dailyValues(type, index, rate, flows, points).map { it.date to it.value },
            flows.map { it.date to it.amount },
        )
    }
}
