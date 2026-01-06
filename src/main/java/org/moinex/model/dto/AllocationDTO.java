/*
 * Filename: AllocationDTO.java
 * Created on: January  2, 2026
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 */

package org.moinex.model.dto;

import java.math.BigDecimal;
import java.math.RoundingMode;
import org.moinex.model.enums.AssetType;
import org.moinex.util.Constants;

public record AllocationDTO(
        AssetType assetType,
        String typeName,
        BigDecimal currentPercentage,
        BigDecimal targetPercentage,
        BigDecimal currentValue,
        BigDecimal difference) {

    public boolean isAboveTarget() {
        return currentPercentage.compareTo(targetPercentage) > 0;
    }

    public boolean isBelowTarget() {
        return currentPercentage.compareTo(targetPercentage) < 0;
    }

    public boolean isOnTarget() {
        return currentPercentage.compareTo(targetPercentage) == 0;
    }

    public String getDifferenceSign() {
        return difference.compareTo(BigDecimal.ZERO) >= 0 ? "+ " : "- ";
    }

    public BigDecimal getAchievementPercentage() {
        if (targetPercentage.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }
        return currentPercentage
                .divide(targetPercentage, 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100));
    }

    public boolean isCriticalLow() {
        BigDecimal achievement = getAchievementPercentage();
        return achievement.compareTo(BigDecimal.valueOf(Constants.ALLOCATION_WARNING_LOW_THRESHOLD))
                < 0;
    }

    public boolean isWarningLow() {
        BigDecimal achievement = getAchievementPercentage();
        return achievement.compareTo(BigDecimal.valueOf(Constants.ALLOCATION_WARNING_LOW_THRESHOLD))
                        >= 0
                && achievement.compareTo(
                                BigDecimal.valueOf(Constants.ALLOCATION_ON_TARGET_LOW_THRESHOLD))
                        < 0;
    }

    public boolean isOnTargetRange() {
        BigDecimal achievement = getAchievementPercentage();
        return achievement.compareTo(
                                BigDecimal.valueOf(Constants.ALLOCATION_ON_TARGET_LOW_THRESHOLD))
                        >= 0
                && achievement.compareTo(
                                BigDecimal.valueOf(Constants.ALLOCATION_ON_TARGET_HIGH_THRESHOLD))
                        < 0;
    }

    public boolean isWarningHigh() {
        BigDecimal achievement = getAchievementPercentage();
        return achievement.compareTo(
                                BigDecimal.valueOf(Constants.ALLOCATION_ON_TARGET_HIGH_THRESHOLD))
                        >= 0
                && achievement.compareTo(
                                BigDecimal.valueOf(Constants.ALLOCATION_WARNING_HIGH_THRESHOLD))
                        < 0;
    }

    public boolean isCriticalHigh() {
        BigDecimal achievement = getAchievementPercentage();
        return achievement.compareTo(
                        BigDecimal.valueOf(Constants.ALLOCATION_WARNING_HIGH_THRESHOLD))
                >= 0;
    }

    public boolean isNotInStrategy() {
        return targetPercentage.compareTo(BigDecimal.ZERO) == 0;
    }
}
