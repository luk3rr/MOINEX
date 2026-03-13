package org.moinex.factory.investment

import org.moinex.model.investment.Bond
import org.moinex.model.investment.BondInterestCalculation
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth

object BondInterestCalculationFactory {
    fun create(
        id: Int? = null,
        bond: Bond = BondFactory.create(id = 1),
        referenceMonth: YearMonth = YearMonth.now(),
        calculationDate: LocalDate = LocalDate.now(),
        calculatedUntilDate: LocalDate = LocalDate.now(),
        quantity: BigDecimal = BigDecimal("100"),
        investedAmount: BigDecimal = BigDecimal("1000.00"),
        monthlyInterest: BigDecimal = BigDecimal("10.00"),
        accumulatedInterest: BigDecimal = BigDecimal("10.00"),
        finalValue: BigDecimal = BigDecimal("1010.00"),
        calculationMethod: String = "FIXED: 10.00% a.a.",
        manuallyAdjusted: Boolean = false,
        createdAt: LocalDateTime = LocalDateTime.now(),
    ): BondInterestCalculation =
        BondInterestCalculation(
            id = id,
            bond = bond,
            referenceMonth = referenceMonth,
            calculationDate = calculationDate,
            calculatedUntilDate = calculatedUntilDate,
            quantity = quantity,
            investedAmount = investedAmount,
            monthlyInterest = monthlyInterest,
            accumulatedInterest = accumulatedInterest,
            finalValue = finalValue,
            calculationMethod = calculationMethod,
            createdAt = createdAt,
            manuallyAdjusted = manuallyAdjusted,
        )
}
