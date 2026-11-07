/*
 * Filename: QuantityIntegrityService.kt
 * Created on: November 7, 2026
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 */

package org.moinex.service.investment

import org.moinex.model.dto.QuantityIntegrityIssueDTO
import org.moinex.model.dto.QuantityIssueKind
import org.moinex.model.enums.OperationType
import org.moinex.model.investment.Bond
import org.moinex.model.investment.BondOperation
import org.moinex.model.investment.Ticker
import org.moinex.model.investment.TickerPurchase
import org.moinex.model.investment.TickerSale
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal

/**
 * Compares what is held with what the registered trades explain. It only reports: nothing is corrected.
 */
@Service
class QuantityIntegrityService(
    private val tickerService: TickerService,
    private val bondService: BondService,
) {
    @Transactional(readOnly = true)
    fun findIssues(): List<QuantityIntegrityIssueDTO> {
        val tickers = tickerService.getAllNonArchivedTickers()
        val purchases = tickerService.getAllPurchases()
        val sales = tickerService.getAllSales()
        val bonds = bondService.getAllNonArchivedBonds()
        val operations = bonds.associateWith { bondService.getOperationsByBond(it) }

        return tickerIssues(tickers, purchases, sales) + bondIssues(operations)
    }

    companion object {
        fun tickerIssues(
            tickers: List<Ticker>,
            purchases: List<TickerPurchase>,
            sales: List<TickerSale>,
        ): List<QuantityIntegrityIssueDTO> {
            val bought = purchases.groupBy { it.ticker.id }.mapValues { (_, list) -> list.sumOf { it.quantity } }
            val sold = sales.groupBy { it.ticker.id }.mapValues { (_, list) -> list.sumOf { it.quantity } }

            return tickers.mapNotNull { ticker ->
                val registered = (bought[ticker.id] ?: BigDecimal.ZERO).subtract(sold[ticker.id] ?: BigDecimal.ZERO)
                val difference = ticker.currentQuantity.subtract(registered)

                if (difference.signum() == 0) {
                    null
                } else {
                    QuantityIntegrityIssueDTO(
                        kind = QuantityIssueKind.TICKER_QUANTITY_DIFFERS,
                        name = ticker.symbol,
                        heldQuantity = ticker.currentQuantity,
                        registeredQuantity = registered,
                        difference = difference,
                    )
                }
            }
        }

        fun bondIssues(operations: Map<Bond, List<BondOperation>>): List<QuantityIntegrityIssueDTO> =
            operations.mapNotNull { (bond, list) ->
                val hasPurchase = list.any { it.operationType == OperationType.BUY }

                if (list.isEmpty() || hasPurchase) {
                    null
                } else {
                    val sold = list.sumOf { it.quantity }
                    QuantityIntegrityIssueDTO(
                        kind = QuantityIssueKind.BOND_WITHOUT_PURCHASE,
                        name = bond.name,
                        heldQuantity = sold.negate(),
                        registeredQuantity = BigDecimal.ZERO,
                        difference = sold.negate(),
                    )
                }
            }
    }
}
