package org.moinex.factory

import org.moinex.model.enums.AssetType
import org.moinex.model.investment.InvestmentTarget
import java.math.BigDecimal

object InvestmentTargetFactory {
    fun create(
        id: Int? = null,
        assetType: AssetType = AssetType.STOCK,
        targetPercentage: BigDecimal = BigDecimal("50"),
        isActive: Boolean = true,
    ): InvestmentTarget =
        InvestmentTarget(
            id,
            assetType,
            targetPercentage,
            isActive,
        )
}
