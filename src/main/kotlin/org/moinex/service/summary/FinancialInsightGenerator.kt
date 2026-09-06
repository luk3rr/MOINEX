/*
 * Filename: FinancialInsightGenerator.kt
 * Created on: September 5, 2026
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 */

package org.moinex.service.summary

import org.moinex.common.constant.TranslationKeys
import org.moinex.model.dto.AnnualSummaryDTO
import org.moinex.model.dto.FinancialInsightDTO
import org.moinex.model.enums.FinancialInsightSeverity
import org.moinex.model.enums.FinancialInsightType
import org.springframework.stereotype.Component
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.YearMonth

@Component
class FinancialInsightGenerator {
    fun generate(
        summary: AnnualSummaryDTO,
        savingsRateTarget: BigDecimal = SAVINGS_RATE_HEALTHY,
    ): List<FinancialInsightDTO> =
        listOfNotNull(
            savingsRateInsight(summary, savingsRateTarget),
            currentMonthProjectionInsight(summary, savingsRateTarget),
            lifestyleInflationInsight(summary),
            spendingConcentrationInsight(summary),
            incomeConcentrationInsight(summary),
            spendingSpikeInsight(summary),
        )

    private fun savingsRateInsight(
        summary: AnnualSummaryDTO,
        target: BigDecimal,
    ): FinancialInsightDTO {
        val rate = summary.savingsRatePercentage
        val (severity, key) =
            when {
                rate >= target ->
                    FinancialInsightSeverity.POSITIVE to TranslationKeys.ANNUAL_SUMMARY_INSIGHT_SAVINGS_RATE_HEALTHY
                rate <= SAVINGS_RATE_LOW ->
                    FinancialInsightSeverity.WARNING to TranslationKeys.ANNUAL_SUMMARY_INSIGHT_SAVINGS_RATE_LOW
                else ->
                    FinancialInsightSeverity.NEUTRAL to TranslationKeys.ANNUAL_SUMMARY_INSIGHT_SAVINGS_RATE_MODERATE
            }
        return FinancialInsightDTO(FinancialInsightType.SAVINGS_RATE, severity, key, listOf(rate))
    }

    private fun currentMonthProjectionInsight(
        summary: AnnualSummaryDTO,
        target: BigDecimal,
    ): FinancialInsightDTO? {
        val currentMonth = summary.monthlyFlows.firstOrNull { it.isCurrentMonth } ?: return null
        val projectedRate = currentMonth.projectedSavingsRatePercentage ?: return null

        return if (projectedRate >= target) {
            FinancialInsightDTO(
                FinancialInsightType.CURRENT_MONTH_PROJECTION,
                FinancialInsightSeverity.NEUTRAL,
                TranslationKeys.ANNUAL_SUMMARY_INSIGHT_CURRENT_MONTH_ON_TRACK,
                listOf(currentMonth.period, projectedRate),
            )
        } else {
            FinancialInsightDTO(
                FinancialInsightType.CURRENT_MONTH_PROJECTION,
                FinancialInsightSeverity.WARNING,
                TranslationKeys.ANNUAL_SUMMARY_INSIGHT_CURRENT_MONTH_ALERT,
                listOf(currentMonth.period, projectedRate, target),
            )
        }
    }

    private fun lifestyleInflationInsight(summary: AnnualSummaryDTO): FinancialInsightDTO? {
        val flows = summary.monthlyFlows
        if (flows.size < MIN_MONTHS_FOR_INFLATION) return null

        val half = flows.size / 2
        val firstHalf = flows.take(half)
        val secondHalf = flows.drop(flows.size - half)

        val firstIncome = firstHalf.sumOfAmount { it.income }
        val secondIncome = secondHalf.sumOfAmount { it.income }
        if (firstIncome.signum() == 0) return null

        val incomeGrowth = secondIncome.divide(firstIncome, 6, RoundingMode.HALF_UP)
        if (incomeGrowth < INCOME_GROWTH_FACTOR) return null

        val firstSavingsRatio = ratio(firstHalf.sumOfAmount { it.net }, firstIncome)
        val secondSavingsRatio = ratio(secondHalf.sumOfAmount { it.net }, secondIncome)
        if (secondSavingsRatio > firstSavingsRatio) return null

        val growthPercentage =
            incomeGrowth.subtract(BigDecimal.ONE).multiply(BigDecimal(100)).setScale(0, RoundingMode.HALF_UP)
        return FinancialInsightDTO(
            FinancialInsightType.LIFESTYLE_INFLATION,
            FinancialInsightSeverity.WARNING,
            TranslationKeys.ANNUAL_SUMMARY_INSIGHT_LIFESTYLE_INFLATION,
            listOf(growthPercentage),
        )
    }

    private fun spendingConcentrationInsight(summary: AnnualSummaryDTO): FinancialInsightDTO? {
        val top = summary.expenseByCategory.take(TOP_CATEGORIES_COUNT)
        if (top.isEmpty()) return null

        val topPercentage = top.fold(BigDecimal.ZERO) { acc, it -> acc + it.percentage }
        if (topPercentage <= SPENDING_CONCENTRATION_THRESHOLD) return null

        val names = top.joinToString(", ") { it.categoryName }
        return FinancialInsightDTO(
            FinancialInsightType.SPENDING_CONCENTRATION,
            FinancialInsightSeverity.WARNING,
            TranslationKeys.ANNUAL_SUMMARY_INSIGHT_SPENDING_CONCENTRATION,
            listOf(topPercentage.setScale(0, RoundingMode.HALF_UP), names),
        )
    }

    private fun incomeConcentrationInsight(summary: AnnualSummaryDTO): FinancialInsightDTO? {
        val largest = summary.incomeByCategory.firstOrNull() ?: return null
        if (largest.percentage <= INCOME_CONCENTRATION_THRESHOLD) return null

        return FinancialInsightDTO(
            FinancialInsightType.INCOME_CONCENTRATION,
            FinancialInsightSeverity.WARNING,
            TranslationKeys.ANNUAL_SUMMARY_INSIGHT_INCOME_CONCENTRATION,
            listOf(largest.percentage.setScale(0, RoundingMode.HALF_UP), largest.categoryName),
        )
    }

    private fun spendingSpikeInsight(summary: AnnualSummaryDTO): FinancialInsightDTO? {
        val worstMonth = summary.monthlyFlows.filter { it.net.signum() < 0 }.minByOrNull { it.net } ?: return null
        val largestItem =
            summary.topExpenses
                .filter { YearMonth.from(it.date) == worstMonth.period }
                .maxByOrNull { it.amount } ?: return null

        return FinancialInsightDTO(
            FinancialInsightType.SPENDING_SPIKE,
            FinancialInsightSeverity.WARNING,
            TranslationKeys.ANNUAL_SUMMARY_INSIGHT_SPENDING_SPIKE,
            listOf(worstMonth.period, largestItem.description, largestItem.amount),
        )
    }

    private fun <T> List<T>.sumOfAmount(selector: (T) -> BigDecimal): BigDecimal =
        fold(BigDecimal.ZERO) { acc, item -> acc + selector(item) }

    private fun ratio(
        part: BigDecimal,
        whole: BigDecimal,
    ): BigDecimal = if (whole.signum() == 0) BigDecimal.ZERO else part.divide(whole, 6, RoundingMode.HALF_UP)

    companion object {
        val SAVINGS_RATE_HEALTHY: BigDecimal = BigDecimal(20)
        val SAVINGS_RATE_LOW: BigDecimal = BigDecimal(5)
        val INCOME_GROWTH_FACTOR: BigDecimal = BigDecimal("1.30")
        val SPENDING_CONCENTRATION_THRESHOLD: BigDecimal = BigDecimal(50)
        val INCOME_CONCENTRATION_THRESHOLD: BigDecimal = BigDecimal(85)
        const val MIN_MONTHS_FOR_INFLATION = 4
        const val TOP_CATEGORIES_COUNT = 3
    }
}
