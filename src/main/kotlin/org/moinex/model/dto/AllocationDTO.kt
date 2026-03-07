/*
 * Filename: AllocationDTO.kt
 * Created on: January  2, 2026
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 *
 * Migrate to Kotlin on 07/03/2026
 */

package org.moinex.model.dto

import org.moinex.model.enums.AssetType
import org.moinex.util.Constants
import java.math.BigDecimal
import java.math.RoundingMode

data class AllocationDTO(
    val assetType: AssetType,
    val typeName: String,
    val currentPercentage: BigDecimal,
    val targetPercentage: BigDecimal,
    val currentValue: BigDecimal,
    val difference: BigDecimal,
) {
    fun isAboveTarget(): Boolean = currentPercentage > targetPercentage

    fun isBelowTarget(): Boolean = currentPercentage < targetPercentage

    fun isOnTarget(): Boolean = currentPercentage.compareTo(targetPercentage) == 0

    fun getDifferenceSign(): String = if (difference >= BigDecimal.ZERO) "+ " else "- "

    fun getAchievementPercentage(): BigDecimal {
        if (targetPercentage.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO
        }
        return currentPercentage
            .divide(targetPercentage, 4, RoundingMode.HALF_UP)
            .multiply(BigDecimal.valueOf(100))
    }

    fun isCriticalLow(): Boolean {
        val achievement = getAchievementPercentage()
        return achievement < BigDecimal.valueOf(Constants.ALLOCATION_WARNING_LOW_THRESHOLD.toDouble())
    }

    fun isWarningLow(): Boolean {
        val achievement = getAchievementPercentage()
        return achievement >= BigDecimal.valueOf(Constants.ALLOCATION_WARNING_LOW_THRESHOLD.toDouble()) &&
            achievement < BigDecimal.valueOf(Constants.ALLOCATION_ON_TARGET_LOW_THRESHOLD.toDouble())
    }

    fun isOnTargetRange(): Boolean {
        val achievement = getAchievementPercentage()
        return achievement >= BigDecimal.valueOf(Constants.ALLOCATION_ON_TARGET_LOW_THRESHOLD.toDouble()) &&
            achievement < BigDecimal.valueOf(Constants.ALLOCATION_ON_TARGET_HIGH_THRESHOLD.toDouble())
    }

    fun isWarningHigh(): Boolean {
        val achievement = getAchievementPercentage()
        return achievement >= BigDecimal.valueOf(Constants.ALLOCATION_ON_TARGET_HIGH_THRESHOLD.toDouble()) &&
            achievement < BigDecimal.valueOf(Constants.ALLOCATION_WARNING_HIGH_THRESHOLD.toDouble())
    }

    fun isCriticalHigh(): Boolean {
        val achievement = getAchievementPercentage()
        return achievement >= BigDecimal.valueOf(Constants.ALLOCATION_WARNING_HIGH_THRESHOLD.toDouble())
    }

    fun isNotInStrategy(): Boolean = targetPercentage.compareTo(BigDecimal.ZERO) == 0
}
