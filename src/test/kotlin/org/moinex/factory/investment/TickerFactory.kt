package org.moinex.factory.investment

import org.moinex.model.enums.AssetType
import org.moinex.model.investment.Ticker
import java.math.BigDecimal
import java.time.LocalDateTime

object TickerFactory {
    fun create(
        id: Int? = null,
        type: AssetType = AssetType.STOCK,
        name: String = "Test Company",
        symbol: String = "TEST",
        currentQuantity: BigDecimal = BigDecimal("100"),
        currentUnitValue: BigDecimal = BigDecimal("10.00"),
        averageUnitValue: BigDecimal = BigDecimal("10.00"),
        isArchived: Boolean = false,
        domain: String? = null,
        lastUpdate: LocalDateTime = LocalDateTime.now(),
        createdAt: LocalDateTime = LocalDateTime.now(),
    ): Ticker =
        Ticker(
            id = id,
            type = type,
            name = name,
            symbol = symbol,
            currentQuantity = currentQuantity,
            currentUnitValue = currentUnitValue,
            averageUnitValue = averageUnitValue,
            isArchived = isArchived,
            domain = domain,
            lastUpdate = lastUpdate,
            createdAt = createdAt,
        )
}
