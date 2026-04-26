package org.moinex.service.financialplanning

import org.moinex.common.constant.Constants
import org.moinex.common.extension.toRounded
import org.moinex.model.dto.FIREProjectionResultDTO
import org.moinex.model.financialplanning.FIRECalculatorSettings
import org.moinex.repository.financialplanning.FIRECalculatorSettingsRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.math.MathContext
import java.math.RoundingMode
import java.time.LocalDate
import kotlin.math.pow

@Service
class FIRECalculatorService(
    private val fireCalculatorSettingsRepository: FIRECalculatorSettingsRepository,
) {
    companion object {
        private val logger = LoggerFactory.getLogger(FIRECalculatorService::class.java)
        private const val SINGLETON_ID = 1
        private const val MAX_MONTHS = 600
        private val MC = MathContext(20, RoundingMode.HALF_UP)
        private val HUNDRED = BigDecimal("100")
        private val TWELVE = BigDecimal("12")
        private val ONE = BigDecimal.ONE
        private const val MIN_AGE = 0
        private const val MAX_AGE = 120
    }

    fun getSettings(): FIRECalculatorSettings? = fireCalculatorSettingsRepository.findById(SINGLETON_ID).orElse(null)

    @Transactional
    fun saveSettings(settings: FIRECalculatorSettings): FIRECalculatorSettings {
        validate(settings)
        val saved = fireCalculatorSettingsRepository.save(settings)
        logger.info("FIRE calculator settings saved")
        return saved
    }

    fun calculate(settings: FIRECalculatorSettings): FIREProjectionResultDTO {
        validate(settings)

        val fireTarget =
            settings.monthlyExpense
                .multiply(TWELVE, MC)
                .divide(settings.withdrawalRate.divide(HUNDRED, MC), MC)

        val dataPoints = mutableListOf<Pair<Int, BigDecimal>>()

        if (settings.currentNetWorth >= fireTarget) {
            dataPoints.add(0 to settings.currentNetWorth)
            return FIREProjectionResultDTO(
                monthsToFire = 0,
                fireDate = LocalDate.now(),
                ageAtFire = settings.currentAge,
                fireTarget = fireTarget,
                projectedNetWorthAtFire = settings.currentNetWorth,
                dataPoints = dataPoints,
            )
        }

        val annualRate = settings.annualReturnRate.divide(HUNDRED, MC)
        val monthlyRate = computeMonthlyRate(annualRate)
        val useLinear = monthlyRate.compareTo(BigDecimal.ZERO) == 0

        var patrimony = settings.currentNetWorth
        var monthsToFire: Int? = null
        var netWorthAtFire: BigDecimal? = null

        for (month in 1..MAX_MONTHS) {
            patrimony =
                if (useLinear) {
                    patrimony.add(settings.monthlyContribution, MC)
                } else {
                    val factor = ONE.add(monthlyRate, MC).pow(month, MC)
                    val growthOfPrincipal = settings.currentNetWorth.multiply(factor, MC)
                    val growthOfContributions =
                        settings.monthlyContribution
                            .multiply(factor.subtract(ONE, MC), MC)
                            .divide(monthlyRate, MC)
                    growthOfPrincipal.add(growthOfContributions, MC)
                }

            dataPoints.add(month to patrimony.toRounded())

            if (monthsToFire == null && patrimony >= fireTarget) {
                monthsToFire = month
                netWorthAtFire = patrimony
            }
        }

        return if (monthsToFire != null) {
            val fireDate = LocalDate.now().plusMonths(monthsToFire.toLong())
            FIREProjectionResultDTO(
                monthsToFire = monthsToFire,
                fireDate = fireDate,
                ageAtFire = settings.currentAge + (monthsToFire / Constants.YEAR_MONTHS),
                fireTarget = fireTarget,
                projectedNetWorthAtFire = netWorthAtFire!!.toRounded(),
                dataPoints = dataPoints,
            )
        } else {
            FIREProjectionResultDTO(
                monthsToFire = null,
                fireDate = null,
                ageAtFire = null,
                fireTarget = fireTarget,
                projectedNetWorthAtFire = null,
                dataPoints = dataPoints,
            )
        }
    }

    private fun computeMonthlyRate(annualRate: BigDecimal): BigDecimal {
        if (annualRate.compareTo(BigDecimal.ZERO) == 0) return BigDecimal.ZERO
        // (1 + annualRate)^(1/12) - 1
        val base = ONE.add(annualRate, MC)
        val monthly = base.toDouble().pow(1.0 / 12)
        return BigDecimal(monthly, MC).subtract(ONE, MC)
    }

    private fun validate(settings: FIRECalculatorSettings) {
        require(settings.currentNetWorth >= BigDecimal.ZERO) {
            "Current net worth must be >= 0"
        }
        require(settings.monthlyContribution >= BigDecimal.ZERO) {
            "Monthly contribution must be >= 0"
        }
        require(settings.annualReturnRate >= BigDecimal.ZERO) {
            "Annual return rate must be >= 0"
        }
        require(settings.monthlyExpense > BigDecimal.ZERO) {
            "Monthly expense must be > 0"
        }
        require(settings.withdrawalRate > BigDecimal.ZERO) {
            "Withdrawal rate must be > 0"
        }
        require(settings.withdrawalRate <= HUNDRED) {
            "Withdrawal rate must be <= 100"
        }
        require(settings.currentAge > MIN_AGE) {
            "Current age must be > $MIN_AGE"
        }
        require(settings.currentAge <= MAX_AGE) {
            "Current age must be <= $MAX_AGE"
        }
    }
}
